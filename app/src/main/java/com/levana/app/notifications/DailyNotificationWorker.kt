package com.levana.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.levana.app.data.CalendarRepository
import com.levana.app.data.ContactBirthdayRepository
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.PreferencesRepository
import com.levana.app.data.ZmanimRepository
import com.levana.app.domain.model.HolidayCategory
import com.levana.app.domain.model.Location
import com.levana.app.domain.model.UserPreferences
import com.levana.app.domain.model.activeLocation
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DailyNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()
    private val zmanimRepository: ZmanimRepository by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val personalEventRepository: PersonalEventRepository by inject()
    private val contactBirthdayRepository: ContactBirthdayRepository by inject()
    private val alarmScheduler: NotificationAlarmScheduler by inject()

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.preferences.first()
        val location = prefs.activeLocation ?: return Result.success()
        val today = prefs.devDateOverride ?: LocalDate.now()

        if (prefs.notifyCandleLighting) {
            handleCandleLighting(today, location, prefs)
        }

        if (prefs.notifyHolidays) {
            handleHolidays(today, prefs)
        }

        if (prefs.notifyFasts) {
            handleFasts(today, location, prefs)
        }

        if (prefs.notifyPersonalEvents) {
            handlePersonalEvents(today)
        }

        if (prefs.notifyOmer) {
            handleOmer(today, location, prefs)
        }

        return Result.success()
    }

    private fun handleCandleLighting(today: LocalDate, location: Location, prefs: UserPreferences) {
        if (!zmanimRepository.hasCandles(today)) return

        val shabbatInfo = zmanimRepository.getShabbatInfo(
            today,
            location,
            prefs.candleLightingOffset
        )
        val candleLightingTime = shabbatInfo.candleLightingTime ?: return

        // Get havdalah/end time from the next day
        val nextDay = today.plusDays(1)
        val nextDayInfo = zmanimRepository.getShabbatInfo(
            nextDay,
            location,
            prefs.candleLightingOffset
        )
        val endTime = nextDayInfo.havdalahTime

        when (prefs.candleLightingNotifyMode) {
            "morning" -> {
                val morningTime = LocalTime.of(
                    prefs.candleLightingMorningTime / 60,
                    prefs.candleLightingMorningTime % 60
                )
                if (LocalTime.now().isBefore(morningTime)) {
                    // Schedule for this morning
                    alarmScheduler.scheduleCandleLighting(
                        today,
                        morningTime,
                        location,
                        candleLightingTime,
                        endTime
                    )
                } else {
                    // Already past morning time, post immediately
                    NotificationPoster.postCandleLighting(
                        applicationContext,
                        today,
                        candleLightingTime,
                        endTime
                    )
                }
            }
            "hours_before" -> {
                val triggerTime = candleLightingTime.minusHours(
                    prefs.candleLightingHoursBefore.toLong()
                )
                alarmScheduler.scheduleCandleLighting(
                    today,
                    triggerTime,
                    location,
                    candleLightingTime,
                    endTime
                )
            }
        }
    }

    private fun handleHolidays(today: LocalDate, prefs: UserPreferences) {
        val daysBefore = prefs.holidayNotifyDaysBefore
        for (offset in 0..daysBefore) {
            val checkDate = today.plusDays(offset.toLong())
            val dayInfo = calendarRepository.getDayInfo(
                checkDate,
                prefs.isInIsrael,
                showModernIsraeli = false
            )
            for (holiday in dayInfo.holidays) {
                if (holiday.category == HolidayCategory.TORAH ||
                    holiday.category == HolidayCategory.RABBINIC
                ) {
                    NotificationPoster.postHoliday(
                        applicationContext,
                        checkDate,
                        holiday.name,
                        offset
                    )
                }
            }
        }
    }

    private fun handleFasts(today: LocalDate, location: Location, prefs: UserPreferences) {
        // All fasts: notify the day before
        val tomorrow = today.plusDays(1)
        val tomorrowInfo = calendarRepository.getDayInfo(tomorrow, inIsrael = prefs.isInIsrael)

        for (holiday in tomorrowInfo.holidays) {
            if (holiday.category != HolidayCategory.FAST) continue

            val fastTimes = zmanimRepository.getFastTimes(tomorrow, location)
            NotificationPoster.postFast(
                applicationContext,
                tomorrow,
                holiday.name,
                fastTimes?.first,
                fastTimes?.second
            )
        }
    }

    private suspend fun handlePersonalEvents(today: LocalDate) {
        val events = personalEventRepository.getEventsForDate(today)
        for (event in events) {
            NotificationPoster.postPersonalEvent(
                applicationContext,
                today,
                event.title
            )
        }

        val birthdays = contactBirthdayRepository.getBirthdaysForDate(today)
        for (birthday in birthdays) {
            NotificationPoster.postPersonalEvent(
                applicationContext,
                today,
                "${birthday.contactName}'s Hebrew Birthday"
            )
        }
    }

    private fun handleOmer(today: LocalDate, location: Location, prefs: UserPreferences) {
        val dayInfo = calendarRepository.getDayInfo(today, inIsrael = false)
        val omerDay = dayInfo.omerDay ?: return

        if (prefs.notifyOmerTzait) {
            val tzaitTime = zmanimRepository.getTzaitTime(today, location) ?: return
            alarmScheduler.scheduleOmerReminder(today, tzaitTime, location, omerDay)
        }

        if (prefs.notifyOmerMorning) {
            val morningTime = LocalTime.of(
                prefs.notifyOmerMorningTime / 60,
                prefs.notifyOmerMorningTime % 60
            )
            alarmScheduler.scheduleOmerMorningReminder(today, morningTime, omerDay)
        }
    }

    companion object {
        private const val DAILY_WORK_NAME = "daily_notification_worker"
        private const val IMMEDIATE_WORK_NAME = "immediate_notification_worker"

        fun enqueueDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
                1,
                TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

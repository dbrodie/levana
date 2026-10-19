package ninja.doskey.app.levana.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.activeLocation
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
        val location = prefs.activeLocation
        val today = prefs.devDateOverride ?: LocalDate.now()
        // Notification text follows Settings → App language, not just the device locale
        val localized = applicationContext.withAppLocale()
        // Keep channel names in the app language even if a language change was missed
        NotificationChannels.createAll(applicationContext)

        if (prefs.notifyCandleLighting) {
            handleCandleLighting(localized, today, location, prefs)
        }

        if (prefs.notifyHolidays) {
            handleHolidays(localized, today, prefs)
        }

        if (prefs.notifyFasts) {
            handleFasts(localized, today, location, prefs)
        }

        if (prefs.notifyPersonalEvents) {
            handlePersonalEvents(localized, today)
        }

        if (prefs.notifyOmer) {
            handleOmer(today, location, prefs)
        }

        return Result.success()
    }

    private fun handleCandleLighting(
        localized: Context,
        today: LocalDate, location: Location, prefs: UserPreferences) {
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
                        localized,
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

    private fun handleHolidays(localized: Context, today: LocalDate, prefs: UserPreferences) {
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
                        localized,
                        checkDate,
                        holiday,
                        offset
                    )
                }
            }
        }
    }

    private fun handleFasts(
        localized: Context,
        today: LocalDate, location: Location, prefs: UserPreferences) {
        // All fasts: notify the day before
        val tomorrow = today.plusDays(1)
        val tomorrowInfo = calendarRepository.getDayInfo(tomorrow, inIsrael = prefs.isInIsrael)

        for (holiday in tomorrowInfo.holidays) {
            if (holiday.category != HolidayCategory.FAST) continue

            val fastTimes = zmanimRepository.getFastTimes(tomorrow, location)
            NotificationPoster.postFast(
                localized,
                tomorrow,
                holiday,
                fastTimes?.first,
                fastTimes?.second
            )
        }
    }

    private suspend fun handlePersonalEvents(localized: Context, today: LocalDate) {
        val events = personalEventRepository.getEventsForDate(today)
        for (occurrence in events) {
            NotificationPoster.postPersonalEvent(
                localized,
                today,
                occurrence.event.title,
                occurrence.label
            )
        }

        val birthdays = contactBirthdayRepository.getBirthdaysForDate(today)
        for (occurrence in birthdays) {
            NotificationPoster.postBirthday(
                localized,
                today,
                occurrence.birthday.contactName,
                occurrence.label
            )
        }
    }

    private fun handleOmer(today: LocalDate, location: Location, prefs: UserPreferences) {
        val plan = planOmerReminders(
            today,
            prefs.notifyOmerTzait,
            prefs.notifyOmerMorning,
            calendarRepository
        ) { zmanimRepository.getTzaitTime(today, location) }

        plan.evening?.let { evening ->
            alarmScheduler.scheduleOmerReminder(today, evening.tzaitTime, location, evening.omerDay)
        }

        plan.morningOmerDay?.let { omerDay ->
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

internal data class EveningOmerReminder(val tzaitTime: LocalTime, val omerDay: Int)

internal data class OmerReminderPlan(
    val evening: EveningOmerReminder?,
    val morningOmerDay: Int?
)

/**
 * Omer reminders for the civil date [today]. The Hebrew day starts at nightfall, so the tzait
 * reminder announces the count of the upcoming Hebrew day ([today] + 1), while the morning
 * reminder announces the current day's count. A missing tzait only drops the evening reminder.
 */
internal fun planOmerReminders(
    today: LocalDate,
    notifyTzait: Boolean,
    notifyMorning: Boolean,
    calendarRepository: CalendarRepository,
    tzaitTime: () -> LocalTime?
): OmerReminderPlan {
    val evening = if (notifyTzait) {
        calendarRepository.getDayInfo(today.plusDays(1), inIsrael = false).omerDay
            ?.let { omerDay -> tzaitTime()?.let { EveningOmerReminder(it, omerDay) } }
    } else {
        null
    }
    val morningOmerDay = if (notifyMorning) {
        calendarRepository.getDayInfo(today, inIsrael = false).omerDay
    } else {
        null
    }
    return OmerReminderPlan(evening, morningOmerDay)
}

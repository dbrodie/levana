package ninja.doskey.app.levana.widget

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.ui.calendar.monthGridWeeks
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import ninja.doskey.app.levana.ui.theme.activeHolidayTheme
import java.time.Instant
import java.time.LocalDate

/** Everything the month widget shows, computed once per update. */
data class MonthWidgetSnapshot(
    val today: LocalDate,
    val month: HebrewYearMonth,
    /** The month's days, as the calendar screen's Hebrew grid lays them out. */
    val days: List<HebrewDay>,
    /** Sunday-first weeks of seven cells; null outside the month. */
    val weeks: List<List<HebrewDay?>>,
    val holidayTheme: HolidayTheme?,
    /** When the snapshot goes stale, on the real clock: the device's next midnight. */
    val refreshAt: Instant,
)

class MonthWidgetSnapshots(private val calendarRepository: CalendarRepository) {

    /**
     * The Hebrew month of the [device]'s date (developer override included), with the
     * holidays of the resolved Israel / Diaspora setting, as the calendar screen shows it.
     */
    fun build(prefs: UserPreferences, device: DeviceNow): MonthWidgetSnapshot {
        val today = device.today
        val month = HebrewYearMonth.from(today)
        val days = calendarRepository.getHebrewMonthDays(month, prefs.isInIsrael, prefs.showModernIsraeliHolidays)
        return MonthWidgetSnapshot(
            today = today,
            month = month,
            days = days,
            weeks = monthGridWeeks(days),
            holidayTheme = activeHolidayTheme(prefs, today),
            refreshAt = device.toSystemInstant(device.nextMidnight),
        )
    }
}

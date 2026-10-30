package ninja.doskey.app.levana.data

import ninja.doskey.app.levana.domain.agenda.AgendaAssembler
import ninja.doskey.app.levana.domain.agenda.AgendaDay
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.displayCity
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The agenda days from [start] to [end]; [calendarPermission] is false when device events could not be read. */
data class AgendaRange(
    val start: LocalDate,
    val end: LocalDate,
    val days: List<AgendaDay>,
    val calendarPermission: Boolean
)

/**
 * Gathers the agenda from the sources the calendar and day panel already use: holidays and
 * parashot ([CalendarRepository]), candle lighting and havdalah at the selected city
 * ([ZmanimRepository], as the day panel), personal events and contact birthdays (Hebrew
 * recurrence through [HebrewDateMatcher]) and the selected device calendars.
 */
class AgendaRepository(
    private val calendarRepository: CalendarRepository,
    private val zmanimRepository: ZmanimRepository,
    private val personalEventRepository: PersonalEventRepository,
    private val contactBirthdayRepository: ContactBirthdayRepository,
    private val systemCalendarRepository: SystemCalendarRepository,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    suspend fun load(start: LocalDate, end: LocalDate, prefs: UserPreferences): AgendaRange {
        val events = personalEventRepository.getEventsBetween(start, end)
        val birthdays = try {
            contactBirthdayRepository.getBirthdaysBetween(start, end)
        } catch (_: SecurityException) {
            emptyMap()
        }
        val calendarPermission = systemCalendarRepository.hasCalendarPermission()
        val deviceEvents = try {
            systemCalendarRepository.getEventsForDateRange(start, end, prefs.selectedCalendarIds)
        } catch (_: SecurityException) {
            emptyMap()
        }
        // Times are the selected city's, like the day panel; dates and holidays are the device's
        val city = prefs.displayCity
        val days = withContext(computeDispatcher) {
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.mapNotNull { date ->
                AgendaAssembler.day(
                    dayInfo = calendarRepository.getDayInfo(date, prefs.isInIsrael, prefs.showModernIsraeliHolidays),
                    shabbatInfo = zmanimRepository.getShabbatInfo(date, city, prefs.candleLightingOffset, prefs.isInIsrael),
                    events = events[date].orEmpty(),
                    birthdays = birthdays[date].orEmpty(),
                    deviceEvents = deviceEvents[date].orEmpty()
                )
            }.toList()
        }
        return AgendaRange(start, end, days, calendarPermission)
    }
}

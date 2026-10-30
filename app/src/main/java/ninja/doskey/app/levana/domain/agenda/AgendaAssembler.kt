package ninja.doskey.app.levana.domain.agenda

import ninja.doskey.app.levana.domain.model.BirthdayOccurrence
import ninja.doskey.app.levana.domain.model.DayInfo
import ninja.doskey.app.levana.domain.model.EventOccurrence
import ninja.doskey.app.levana.domain.model.ShabbatInfo
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent

/**
 * Builds one agenda day from what the day panel already reads for that day: its [DayInfo],
 * its [ShabbatInfo] at the selected city, and its event, birthday and device-event
 * occurrences. No calendar rule is computed here.
 */
object AgendaAssembler {

    /** The day's entries in display order, or null when it has none. */
    fun day(
        dayInfo: DayInfo,
        shabbatInfo: ShabbatInfo,
        events: List<EventOccurrence> = emptyList(),
        birthdays: List<BirthdayOccurrence> = emptyList(),
        deviceEvents: List<SystemCalendarEvent> = emptyList()
    ): AgendaDay? {
        val date = dayInfo.gregorianDate
        val items = buildList {
            dayInfo.holidays.forEach { add(AgendaItem.HolidayItem(it)) }
            shabbatInfo.candleLightingTime?.let { add(AgendaItem.CandleLighting(it)) }
            val havdalah = shabbatInfo.havdalahTime
            val parsha = dayInfo.parsha
            if (parsha != null) {
                add(AgendaItem.Shabbat(parsha, dayInfo.parshaHebrew, dayInfo.specialShabbat, havdalah))
            } else if (havdalah != null) {
                add(AgendaItem.Havdalah(havdalah))
            }
            events.forEach { add(AgendaItem.PersonalEventItem(it.event, it.label)) }
            birthdays.forEach { add(AgendaItem.BirthdayItem(it.birthday, it.label)) }
            deviceEvents.forEach { event ->
                val startsToday = event.startTime.toLocalDate() == date
                add(AgendaItem.DeviceEventItem(event, event.startTime.toLocalTime().takeIf { !event.allDay && startsToday }))
            }
        }
        if (items.isEmpty()) return null
        return AgendaDay(date, dayInfo.hebrewDay, order(items))
    }

    /**
     * All-day entries first (holidays, Shabbat, yahrzeits, birthdays, events, all-day device
     * events), then timed entries by time; ties by kind, then title. The holiday order of the
     * day (Yom Tov before Rosh Chodesh) is kept.
     */
    fun order(items: List<AgendaItem>): List<AgendaItem> =
        items.withIndex().sortedWith(
            compareBy<IndexedValue<AgendaItem>> { it.value.time != null }
                .thenBy { it.value.time }
                .thenBy { rank(it.value) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { title(it.value) }
                .thenBy { it.index }
        ).map { it.value }

    private fun rank(item: AgendaItem): Int = when (item) {
        is AgendaItem.HolidayItem -> 0
        is AgendaItem.Shabbat -> 1
        is AgendaItem.PersonalEventItem -> if (item.isYahrzeit) 2 else 4
        is AgendaItem.BirthdayItem -> 3
        is AgendaItem.CandleLighting -> 5
        is AgendaItem.Havdalah -> 6
        is AgendaItem.DeviceEventItem -> 7
    }

    private fun title(item: AgendaItem): String = when (item) {
        is AgendaItem.PersonalEventItem -> item.event.title
        is AgendaItem.BirthdayItem -> item.birthday.contactName
        is AgendaItem.DeviceEventItem -> item.event.title
        else -> ""
    }
}

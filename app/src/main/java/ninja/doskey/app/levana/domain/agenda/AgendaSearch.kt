package ninja.doskey.app.levana.domain.agenda

import java.time.LocalDate

/**
 * Agenda search (owner decision 2, TASK-8): everything the agenda lists, by its titles —
 * event, birthday and yahrzeit titles and notes, holidays and parashot in English and
 * Hebrew, and device events with their locations — over [range] around today.
 */
object AgendaSearch {

    /** Twelve months back and ahead of [today], inclusive. */
    fun range(today: LocalDate): ClosedRange<LocalDate> = today.minusMonths(12)..today.plusMonths(12)

    /** The days with an entry matching [query], each with only its matching entries. */
    fun search(days: List<AgendaDay>, query: String): List<AgendaDay> {
        val needle = normalize(query)
        if (needle.isEmpty()) return emptyList()
        return days.mapNotNull { day ->
            val matches = day.items.filter { item -> texts(item).any { normalize(it).contains(needle) } }
            if (matches.isEmpty()) null else day.copy(items = matches)
        }
    }

    /** The searchable texts of [item]. */
    fun texts(item: AgendaItem): List<String> = when (item) {
        is AgendaItem.HolidayItem -> item.roshChodeshOf?.let { month ->
            listOf("${item.holiday.name} ${month.englishName}", "${item.holiday.hebrewName} ${month.hebrewName}")
        } ?: listOf(item.holiday.name, item.holiday.hebrewName)
        is AgendaItem.Shabbat -> listOfNotNull(item.parsha, item.parshaHebrew, item.specialShabbat)
        is AgendaItem.PersonalEventItem -> listOf(item.event.title, item.event.notes)
        is AgendaItem.BirthdayItem -> listOf(item.birthday.contactName)
        is AgendaItem.DeviceEventItem -> listOf(item.event.title, item.event.location)
        is AgendaItem.CandleLighting, is AgendaItem.Havdalah -> emptyList()
    }

    /** Lower case, without apostrophes, geresh/gershayim, quotes or Hebrew points; spaces collapsed. */
    internal fun normalize(text: String): String =
        text.lowercase()
            .replace(IGNORED, "")
            .replace(SPACES, " ")
            .trim()

    private val IGNORED = Regex("['\"`‘’“”׳״֑-ׇ]")
    private val SPACES = Regex("\\s+")
}

package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.domain.model.EventOccurrence
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import java.time.LocalDate
import java.util.GregorianCalendar
import kotlinx.coroutines.flow.Flow

/** The current global yahrzeit Adar settings. */
fun interface YahrzeitRulesProvider {
    suspend fun rules(): YahrzeitRules
}

class PersonalEventRepository(
    private val dao: PersonalEventDao,
    private val yahrzeitRules: YahrzeitRulesProvider = YahrzeitRulesProvider { YahrzeitRules() }
) {

    fun getAll(): Flow<List<PersonalEvent>> = dao.getAll()

    suspend fun getAllOnce(): List<PersonalEvent> = dao.getAllOnce()

    suspend fun getById(id: Long): PersonalEvent? = dao.getById(id)

    suspend fun insert(event: PersonalEvent): Long = dao.insert(event)

    suspend fun update(event: PersonalEvent) = dao.update(event)

    suspend fun delete(event: PersonalEvent) = dao.delete(event)

    /** Events observed on [date], each with its occurrence label (day panel, notifications). */
    suspend fun getEventsForDate(date: LocalDate): List<EventOccurrence> {
        val jd = jewishDate(date)
        val rules = yahrzeitRules.rules()
        return dao.getAllOnce().mapNotNull { event ->
            HebrewDateMatcher.occurrenceOn(
                event.hebrewDay,
                event.hebrewMonth,
                event.hebrewYear,
                event.useYahrzeitRules,
                jd.jewishYear,
                jd.jewishMonth,
                jd.jewishDayOfMonth,
                rules
            )?.let { EventOccurrence(event, it.label) }
        }
    }

    /** Events observed from [start] to [end] (inclusive), by day, each with its occurrence label (agenda). */
    suspend fun getEventsBetween(start: LocalDate, end: LocalDate): Map<LocalDate, List<EventOccurrence>> {
        val rules = yahrzeitRules.rules()
        return dao.getAllOnce().flatMap { event ->
            HebrewDateMatcher.occurrencesBetween(
                event.hebrewDay,
                event.hebrewMonth,
                event.hebrewYear,
                event.useYahrzeitRules,
                start,
                end,
                rules
            ).map { (date, occurrence) -> date to EventOccurrence(event, occurrence.label) }
        }.groupBy({ it.first }, { it.second })
    }

    suspend fun getEventDaysForHebrewMonth(hebrewYear: Int, hebrewMonth: Int): Set<Int> {
        val rules = yahrzeitRules.rules()
        return dao.getAllOnce().flatMap { event ->
            HebrewDateMatcher.occurrences(
                event.hebrewDay,
                event.hebrewMonth,
                event.hebrewYear,
                event.useYahrzeitRules,
                hebrewYear,
                rules
            ).filter { it.month == hebrewMonth }.map { it.day }
        }.toSet()
    }

    suspend fun getEventDatesForGregorianMonth(year: Int, month: Int): Set<LocalDate> {
        val allEvents = dao.getAllOnce()
        if (allEvents.isEmpty()) return emptySet()
        val rules = yahrzeitRules.rules()
        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()

        return (1..daysInMonth).map { LocalDate.of(year, month, it) }.filter { date ->
            val jd = jewishDate(date)
            allEvents.any { event ->
                HebrewDateMatcher.matchesDate(
                    event.hebrewDay,
                    event.hebrewMonth,
                    event.hebrewYear,
                    event.useYahrzeitRules,
                    jd.jewishYear,
                    jd.jewishMonth,
                    jd.jewishDayOfMonth,
                    rules
                )
            }
        }.toSet()
    }

    private fun jewishDate(date: LocalDate) =
        JewishDate(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))
}

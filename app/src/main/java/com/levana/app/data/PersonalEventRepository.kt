package com.levana.app.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.levana.app.data.db.PersonalEvent
import com.levana.app.data.db.PersonalEventDao
import com.levana.app.domain.model.EventType
import java.time.LocalDate
import java.util.GregorianCalendar
import kotlinx.coroutines.flow.Flow

class PersonalEventRepository(private val dao: PersonalEventDao) {

    fun getAll(): Flow<List<PersonalEvent>> = dao.getAll()

    suspend fun getById(id: Long): PersonalEvent? = dao.getById(id)

    suspend fun insert(event: PersonalEvent): Long = dao.insert(event)

    suspend fun update(event: PersonalEvent) = dao.update(event)

    suspend fun delete(event: PersonalEvent) = dao.delete(event)

    /**
     * Get all personal events that occur on a given Gregorian date,
     * applying yahrzeit Adar rules for the target year.
     */
    suspend fun getEventsForDate(date: LocalDate): List<PersonalEvent> {
        val gc = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        val jd = JewishDate(gc)
        val targetMonth = jd.jewishMonth
        val targetDay = jd.jewishDayOfMonth
        val targetIsLeapYear = jd.isJewishLeapYear

        val allEvents = dao.getAllOnce()
        return allEvents.filter { event ->
            matchesDate(event, targetMonth, targetDay, targetIsLeapYear)
        }
    }

    /**
     * Get a set of Hebrew day-of-month values that have events for a given
     * Hebrew month in a given year. Used for calendar cell indicators.
     */
    suspend fun getEventDaysForHebrewMonth(hebrewYear: Int, hebrewMonth: Int): Set<Int> {
        val isLeapYear = JewishDate(hebrewYear, JewishDate.TISHREI, 1).isJewishLeapYear
        val allEvents = dao.getAllOnce()
        val days = mutableSetOf<Int>()

        for (event in allEvents) {
            val resolvedMonth = resolveMonth(event, isLeapYear)
            if (resolvedMonth == hebrewMonth) {
                days.add(event.hebrewDay)
            }
        }
        return days
    }

    /**
     * Get a set of Gregorian dates that have events for a given Gregorian month.
     * Used for calendar cell indicators in Gregorian mode.
     */
    suspend fun getEventDatesForGregorianMonth(year: Int, month: Int): Set<LocalDate> {
        val allEvents = dao.getAllOnce()
        val dates = mutableSetOf<LocalDate>()
        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()

        for (dayOfMonth in 1..daysInMonth) {
            val date = LocalDate.of(year, month, dayOfMonth)
            val gc = GregorianCalendar(year, month - 1, dayOfMonth)
            val jd = JewishDate(gc)
            val targetMonth = jd.jewishMonth
            val targetDay = jd.jewishDayOfMonth
            val targetIsLeapYear = jd.isJewishLeapYear

            if (allEvents.any { matchesDate(it, targetMonth, targetDay, targetIsLeapYear) }) {
                dates.add(date)
            }
        }
        return dates
    }

    /**
     * Determine which Hebrew month an event should be observed in for a given year,
     * applying yahrzeit Adar rules.
     */
    private fun resolveMonth(event: PersonalEvent, targetIsLeapYear: Boolean): Int {
        val eventMonth = event.hebrewMonth
        val eventIsLeapYear =
            JewishDate(event.hebrewYear, JewishDate.TISHREI, 1).isJewishLeapYear

        if (event.eventType == EventType.YAHRZEIT) {
            return resolveYahrzeitMonth(eventMonth, eventIsLeapYear, targetIsLeapYear)
        }

        // Birthday and Custom: simple mapping
        return resolveSimpleMonth(eventMonth, eventIsLeapYear, targetIsLeapYear)
    }

    /**
     * Yahrzeit Adar rules per Shulchan Aruch:
     * - Death in Adar of non-leap year -> observe in Adar II of leap year
     * - Death in Adar I of leap year -> observe in Adar I
     * - Death in Adar II of leap year -> observe in Adar II
     */
    private fun resolveYahrzeitMonth(
        eventMonth: Int,
        eventIsLeapYear: Boolean,
        targetIsLeapYear: Boolean
    ): Int {
        if (!targetIsLeapYear) {
            // Non-leap target: any Adar variant maps to Adar
            return if (eventMonth == JewishDate.ADAR || eventMonth == JewishDate.ADAR_II) {
                JewishDate.ADAR
            } else {
                eventMonth
            }
        }

        // Target IS leap year
        return when {
            // Death in Adar of non-leap year -> Adar II
            eventMonth == JewishDate.ADAR && !eventIsLeapYear -> JewishDate.ADAR_II
            // Death in Adar I (stored as ADAR in leap year) -> Adar I
            eventMonth == JewishDate.ADAR && eventIsLeapYear -> JewishDate.ADAR
            // Death in Adar II -> Adar II
            eventMonth == JewishDate.ADAR_II -> JewishDate.ADAR_II
            else -> eventMonth
        }
    }

    /**
     * Simple month resolution for birthdays and custom events.
     * In non-Adar months, no change needed.
     * For Adar: map to whichever Adar exists in the target year.
     */
    private fun resolveSimpleMonth(
        eventMonth: Int,
        eventIsLeapYear: Boolean,
        targetIsLeapYear: Boolean
    ): Int {
        if (!targetIsLeapYear) {
            return if (eventMonth == JewishDate.ADAR || eventMonth == JewishDate.ADAR_II) {
                JewishDate.ADAR
            } else {
                eventMonth
            }
        }

        // Target IS leap year
        return when {
            eventMonth == JewishDate.ADAR && !eventIsLeapYear -> JewishDate.ADAR
            eventMonth == JewishDate.ADAR && eventIsLeapYear -> JewishDate.ADAR
            eventMonth == JewishDate.ADAR_II -> JewishDate.ADAR_II
            else -> eventMonth
        }
    }

    private fun matchesDate(
        event: PersonalEvent,
        targetMonth: Int,
        targetDay: Int,
        targetIsLeapYear: Boolean
    ): Boolean {
        if (event.hebrewDay != targetDay) return false
        val eventIsLeapYear =
            JewishDate(event.hebrewYear, JewishDate.TISHREI, 1).isJewishLeapYear
        val resolvedMonth = if (event.eventType == EventType.YAHRZEIT) {
            resolveYahrzeitMonth(event.hebrewMonth, eventIsLeapYear, targetIsLeapYear)
        } else {
            resolveSimpleMonth(event.hebrewMonth, eventIsLeapYear, targetIsLeapYear)
        }
        return resolvedMonth == targetMonth
    }
}

package com.levana.app.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.levana.app.data.db.PersonalEvent
import com.levana.app.data.db.PersonalEventDao
import java.time.LocalDate
import java.util.GregorianCalendar
import kotlinx.coroutines.flow.Flow

class PersonalEventRepository(private val dao: PersonalEventDao) {

    fun getAll(): Flow<List<PersonalEvent>> = dao.getAll()

    suspend fun getById(id: Long): PersonalEvent? = dao.getById(id)

    suspend fun insert(event: PersonalEvent): Long = dao.insert(event)

    suspend fun update(event: PersonalEvent) = dao.update(event)

    suspend fun delete(event: PersonalEvent) = dao.delete(event)

    suspend fun getEventsForDate(date: LocalDate): List<PersonalEvent> {
        val gc = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        val jd = JewishDate(gc)
        val targetMonth = jd.jewishMonth
        val targetDay = jd.jewishDayOfMonth
        val targetIsLeapYear = jd.isJewishLeapYear

        val allEvents = dao.getAllOnce()
        return allEvents.filter { event ->
            HebrewDateMatcher.matchesDate(
                event.hebrewDay,
                event.hebrewMonth,
                event.hebrewYear,
                event.useYahrzeitRules,
                targetMonth,
                targetDay,
                targetIsLeapYear
            )
        }
    }

    suspend fun getEventDaysForHebrewMonth(hebrewYear: Int, hebrewMonth: Int): Set<Int> {
        val isLeapYear = JewishDate(
            hebrewYear,
            JewishDate.TISHREI,
            1
        ).isJewishLeapYear
        val allEvents = dao.getAllOnce()
        val days = mutableSetOf<Int>()

        for (event in allEvents) {
            val eventIsLeapYear = JewishDate(
                event.hebrewYear,
                JewishDate.TISHREI,
                1
            ).isJewishLeapYear
            val resolvedMonth = if (event.useYahrzeitRules) {
                HebrewDateMatcher.resolveYahrzeitMonth(
                    event.hebrewMonth,
                    eventIsLeapYear,
                    isLeapYear
                )
            } else {
                HebrewDateMatcher.resolveSimpleMonth(
                    event.hebrewMonth,
                    eventIsLeapYear,
                    isLeapYear
                )
            }
            if (resolvedMonth == hebrewMonth) {
                days.add(event.hebrewDay)
            }
        }
        return days
    }

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

            if (allEvents.any { event ->
                    HebrewDateMatcher.matchesDate(
                        event.hebrewDay,
                        event.hebrewMonth,
                        event.hebrewYear,
                        event.useYahrzeitRules,
                        targetMonth,
                        targetDay,
                        targetIsLeapYear
                    )
                }
            ) {
                dates.add(date)
            }
        }
        return dates
    }
}

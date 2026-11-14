package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.data.CalendarRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Which holiday reminders the daily run posts, with the real calendar (Diaspora). */
class HolidayRemindersTest {

    private val calendar = CalendarRepository()

    private fun reminders(today: String, daysBefore: Int, inIsrael: Boolean = false) =
        holidayReminders(LocalDate.parse(today), daysBefore) { date ->
            calendar.getDayInfo(date, inIsrael, showModernIsraeli = false).holidays
        }.map { "${it.date} ${it.holiday.name} +${it.daysUntil}" }

    @Test
    fun secondDayOfChanukahIsOnlyToday() {
        // Chanukah 5787 is 5–12 December 2026
        assertEquals(listOf("2026-12-06 Chanukah +0"), reminders("2026-12-06", 1))
        assertEquals(listOf("2026-12-06 Chanukah +0"), reminders("2026-12-06", 3))
    }

    @Test
    fun firstDayOfChanukahStillBeginsTomorrow() {
        assertEquals(listOf("2026-12-05 Chanukah +1"), reminders("2026-12-04", 1))
        // Further ahead, only the first day is announced, not "begins in 2 days" for day 2
        assertEquals(listOf("2026-12-05 Chanukah +1"), reminders("2026-12-04", 3))
        assertEquals(listOf("2026-12-05 Chanukah +2"), reminders("2026-12-03", 3))
    }

    @Test
    fun cholHamoedAndTheLastDaysContinuePesach() {
        // Pesach 5787 in the Diaspora: Yom Tov 22–23 April 2027, Chol HaMoed 24–27, Yom Tov 28–29
        assertEquals(
            listOf("2027-04-21 Erev Pesach +0", "2027-04-22 Pesach +1"),
            reminders("2027-04-21", 3)
        )
        assertEquals(listOf("2027-04-23 Pesach +0"), reminders("2027-04-23", 3))
        assertEquals(listOf("2027-04-27 Chol HaMoed Pesach +0"), reminders("2027-04-27", 3))
    }

    @Test
    fun distinctDaysAfterSukkotAreStillAnnounced() {
        // Hoshana Rabba 2 October 2026, Shemini Atzeret 3rd, Simchat Torah 4th
        assertEquals(
            listOf(
                "2026-10-01 Chol HaMoed Sukkot +0",
                "2026-10-02 Hoshana Rabba +1",
                "2026-10-03 Shemini Atzeret +2",
                "2026-10-04 Simchat Torah +3"
            ),
            reminders("2026-10-01", 3)
        )
    }

    @Test
    fun secondDayOfRoshHashanahIsOnlyToday() {
        // Rosh Hashanah 5788 is 2–3 October 2027
        assertEquals(
            listOf("2027-10-01 Erev Rosh Hashanah +0", "2027-10-02 Rosh Hashanah +1"),
            reminders("2027-10-01", 2)
        )
    }
}

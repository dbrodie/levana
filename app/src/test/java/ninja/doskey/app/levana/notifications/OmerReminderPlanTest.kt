package ninja.doskey.app.levana.notifications

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.data.CalendarRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class OmerReminderPlanTest {

    private val calendarRepository = CalendarRepository()
    private val tzait = LocalTime.of(20, 5)

    /** Civil date of [day] [month] 5786. */
    private fun date(month: Int, day: Int): LocalDate =
        JewishCalendar(5786, month, day).localDate

    private fun plan(
        today: LocalDate,
        notifyTzait: Boolean = true,
        notifyMorning: Boolean = true,
        tzaitTime: LocalTime? = tzait
    ) = planOmerReminders(today, notifyTzait, notifyMorning, calendarRepository) { tzaitTime }

    @Test
    fun `first night counts day 1 with no morning reminder`() {
        val result = plan(date(JewishCalendar.NISSAN, 15))
        assertEquals(EveningOmerReminder(tzait, 1), result.evening)
        assertNull(result.morningOmerDay)
    }

    @Test
    fun `ordinary evening counts the upcoming day and morning the current day`() {
        val result = plan(date(JewishCalendar.NISSAN, 16))
        assertEquals(EveningOmerReminder(tzait, 2), result.evening)
        assertEquals(1, result.morningOmerDay)

        val lagBaOmerEve = plan(date(JewishCalendar.IYAR, 17))
        assertEquals(EveningOmerReminder(tzait, 33), lagBaOmerEve.evening)
        assertEquals(32, lagBaOmerEve.morningOmerDay)
    }

    @Test
    fun `last night counts day 49`() {
        val result = plan(date(JewishCalendar.SIVAN, 4))
        assertEquals(EveningOmerReminder(tzait, 49), result.evening)
        assertEquals(48, result.morningOmerDay)
    }

    @Test
    fun `evening after the final count has no tzait reminder but keeps the morning one`() {
        val result = plan(date(JewishCalendar.SIVAN, 5))
        assertNull(result.evening)
        assertEquals(49, result.morningOmerDay)
    }

    @Test
    fun `no reminders outside the Omer`() {
        val result = plan(date(JewishCalendar.SIVAN, 6))
        assertNull(result.evening)
        assertNull(result.morningOmerDay)

        val beforePesach = plan(date(JewishCalendar.NISSAN, 14))
        assertNull(beforePesach.evening)
        assertNull(beforePesach.morningOmerDay)
    }

    @Test
    fun `missing tzait drops only the evening reminder`() {
        val result = plan(date(JewishCalendar.IYAR, 1), tzaitTime = null)
        assertNull(result.evening)
        assertEquals(16, result.morningOmerDay)
    }

    @Test
    fun `disabled preferences produce no reminder`() {
        val today = date(JewishCalendar.IYAR, 1)
        assertNull(plan(today, notifyTzait = false).evening)
        assertEquals(16, plan(today, notifyTzait = false).morningOmerDay)
        assertNull(plan(today, notifyMorning = false).morningOmerDay)
        assertEquals(EveningOmerReminder(tzait, 17), plan(today, notifyMorning = false).evening)
    }

    @Test
    fun `tzait is not computed when the evening reminder is not needed`() {
        var calls = 0
        planOmerReminders(date(JewishCalendar.SIVAN, 5), true, true, calendarRepository) {
            calls++
            tzait
        }
        planOmerReminders(date(JewishCalendar.IYAR, 1), false, true, calendarRepository) {
            calls++
            tzait
        }
        assertEquals(0, calls)
    }
}

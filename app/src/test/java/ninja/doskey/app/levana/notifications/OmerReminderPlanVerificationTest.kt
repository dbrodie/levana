package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.data.CalendarRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Independent verifier tests using hard-coded Gregorian dates rather than KosherJava-derived ones. */
class OmerReminderPlanVerificationTest {

    private val repo = CalendarRepository()
    private val tzait = Instant.parse("2026-04-20T16:45:00Z")

    private fun plan(today: LocalDate, tzaitTime: Instant? = tzait) =
        planOmerReminders(today, true, true, repo) { tzaitTime }

    /** first-night civil date (15 Nissan) and Shavuot (6 Sivan) per year. */
    private val years = listOf(
        LocalDate.of(2025, 4, 13) to LocalDate.of(2025, 6, 2),
        LocalDate.of(2026, 4, 2) to LocalDate.of(2026, 5, 22),
        LocalDate.of(2027, 4, 22) to LocalDate.of(2027, 6, 11),
    )

    @Test
    fun `first night, ordinary nights, last night and post-count evening across years`() {
        for ((firstNight, shavuot) in years) {
            // Erev Pesach (14 Nissan): nothing
            plan(firstNight.minusDays(1)).let {
                assertNull("$firstNight-1 evening", it.evening)
                assertNull("$firstNight-1 morning", it.morningOmerDay)
            }
            // First night: evening 1, no morning
            plan(firstNight).let {
                assertEquals("$firstNight evening", EveningOmerReminder(tzait, 1), it.evening)
                assertNull("$firstNight morning", it.morningOmerDay)
            }
            // Ordinary nights: evening of civil day k is count k+1, morning is k
            for (k in 1..47) {
                val p = plan(firstNight.plusDays(k.toLong()))
                assertEquals("day $k evening", EveningOmerReminder(tzait, k + 1), p.evening)
                assertEquals("day $k morning", k, p.morningOmerDay)
            }
            // Last night (evening of 4 Sivan): 49; morning 48
            val lastNight = shavuot.minusDays(2)
            assertEquals(firstNight.plusDays(48), lastNight)
            plan(lastNight).let {
                assertEquals(EveningOmerReminder(tzait, 49), it.evening)
                assertEquals(48, it.morningOmerDay)
            }
            // Erev Shavuot (5 Sivan): morning 49, no evening (no count 50)
            plan(shavuot.minusDays(1)).let {
                assertNull("erev Shavuot evening", it.evening)
                assertEquals(49, it.morningOmerDay)
            }
            // Shavuot: nothing
            plan(shavuot).let {
                assertNull(it.evening)
                assertNull(it.morningOmerDay)
            }
        }
    }

    @Test
    fun `missing tzait never suppresses morning, including first and final days`() {
        for ((firstNight, _) in years) {
            for (k in 0..50) {
                val day = firstNight.plusDays(k.toLong())
                val withTzait = plan(day)
                val without = plan(day, tzaitTime = null)
                assertNull(without.evening)
                assertEquals("morning on $day", withTzait.morningOmerDay, without.morningOmerDay)
            }
            // Erev Shavuot with tzait unavailable still gets morning 49
            assertEquals(49, plan(firstNight.plusDays(49), tzaitTime = null).morningOmerDay)
        }
    }

    @Test
    fun `evening count always equals next day's morning count over a full year`() {
        var d = LocalDate.of(2026, 1, 1)
        while (d.year == 2026) {
            val today = plan(d)
            val tomorrow = plan(d.plusDays(1))
            assertEquals("$d", tomorrow.morningOmerDay, today.evening?.omerDay)
            d = d.plusDays(1)
        }
    }
}

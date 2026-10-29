package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Omer evening reminders when the computed nightfall falls after civil midnight. */
class OmerNightfallRolloverTest {

    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()

    private val stPetersburg = Location(59.9343, 30.3351, timezoneId = "Europe/Moscow", name = "St. Petersburg")
    private val spbZone = ZoneId.of(stPetersburg.timezoneId)
    private val riga = Location(56.9496, 24.1052, timezoneId = "Europe/Riga", name = "Riga")
    private val helsinki = Location(60.1699, 24.9384, timezoneId = "Europe/Helsinki", name = "Helsinki")
    private val santiago = Location(-33.4489, -70.6693, timezoneId = "America/Santiago", name = "Santiago")

    private val omerPrefs = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 7 * 60
    )

    /** 15 Nissan 5787: the evening whose count is 1. */
    private val firstNight2027 = LocalDate.of(2027, 4, 22)

    private fun may(day: Int) = LocalDate.of(2027, 5, day)

    private fun plan(
        today: LocalDate,
        now: LocalTime,
        location: Location = stPetersburg,
        deviceZone: ZoneId = ZoneId.of(location.timezoneId),
        prefs: UserPreferences = omerPrefs,
        lookaheadDays: Int = REMINDER_LOOKAHEAD_DAYS
    ) = planReminderAlarms(
        today,
        ZonedDateTime.of(today, now, deviceZone).toInstant(),
        deviceZone,
        location,
        prefs,
        zmanim,
        calendar,
        lookaheadDays
    )

    private fun ReminderAlarmPlan.evenings() = alarms.filterIsInstance<ReminderAlarm.OmerEvening>()
    private fun ReminderAlarmPlan.mornings() = alarms.filterIsInstance<ReminderAlarm.OmerMorning>()
    private fun ReminderAlarmPlan.evening(date: LocalDate) = evenings().single { it.date == date }

    private fun ReminderAlarm.local(zone: ZoneId): LocalDateTime =
        triggerAt.atZone(zone).toLocalDateTime()

    @Test
    fun `post-midnight nightfall fires on the next civil date with the count beginning that night`() {
        val result = plan(may(24), LocalTime.NOON)
        val evening = result.evening(may(24))

        assertEquals(33, evening.omerDay)
        val local = evening.local(spbZone)
        assertEquals(may(25), local.toLocalDate())
        assertEquals(LocalTime.of(0, 2), local.toLocalTime().withSecond(0).withNano(0))
        assertEquals(zmanim.getTzaitInstant(may(24), stPetersburg), evening.triggerAt)
        // Not the same-date join of the clock time, almost a day earlier
        assertTrue(evening.triggerAt.isAfter(ZonedDateTime.of(may(24), LocalTime.of(23, 0), spbZone).toInstant()))
        // Only the previous evening, whose nightfall has passed, is overdue
        assertEquals(listOf(may(23)), result.overdue.filterIsInstance<ReminderAlarm.OmerEvening>().map { it.date })
        // The morning reminder of that date still carries the current count
        val morning = result.overdue.filterIsInstance<ReminderAlarm.OmerMorning>().single()
        assertEquals(may(24), morning.date)
        assertEquals(32, morning.omerDay)
    }

    @Test
    fun `ordinary nights fire on their own civil date at nightfall`() {
        val jerusalem = Location.JERUSALEM
        val zone = ZoneId.of(jerusalem.timezoneId)
        val result = plan(LocalDate.of(2027, 5, 2), LocalTime.NOON, location = jerusalem)
        val evenings = result.evenings()

        assertEquals((0..7L).map { LocalDate.of(2027, 5, 2).plusDays(it) }, evenings.map { it.date })
        evenings.forEach {
            assertEquals(it.date, it.local(zone).toLocalDate())
            assertEquals(ZonedDateTime.of(it.date, zmanim.getTzaitTime(it.date, jerusalem)!!, zone).toInstant(), it.triggerAt)
            assertEquals((it.date.toEpochDay() - firstNight2027.toEpochDay() + 1).toInt(), it.omerDay)
        }
    }

    @Test
    fun `successive post-midnight evenings stay distinct, ordered and one count apart`() {
        val result = plan(may(22), LocalTime.of(6, 0))
        val evenings = result.evenings()

        // Nightfall is computed through 28 May; from 29 May there is none
        assertEquals((22..28).map { may(it) }, evenings.map { it.date })
        assertEquals((31..37).toList(), evenings.map { it.omerDay })
        evenings.forEach {
            val expectedDate = if (it.date >= may(24)) it.date.plusDays(1) else it.date
            assertEquals(it.date.toString(), expectedDate, it.local(spbZone).toLocalDate())
            assertEquals(zmanim.getTzaitInstant(it.date, stPetersburg), it.triggerAt)
        }
        evenings.zipWithNext().forEach { (a, b) ->
            val gap = Duration.between(a.triggerAt, b.triggerAt)
            assertTrue("$a -> $b", gap > Duration.ofHours(23) && gap < Duration.ofHours(25))
        }
        // Mornings continue for every date, including those without nightfall
        assertEquals((22..29).map { may(it) }, result.mornings().map { it.date })
        assertEquals((30..37).toList(), result.mornings().map { it.omerDay })
    }

    @Test
    fun `run between midnight and a post-midnight nightfall still plans that night's reminder`() {
        val beforeNightfall = plan(may(25), LocalTime.of(0, 1))

        assertEquals(may(25), beforeNightfall.window.first())
        val lastNight = beforeNightfall.evening(may(24))
        assertEquals(33, lastNight.omerDay)
        assertEquals(zmanim.getTzaitInstant(may(24), stPetersburg), lastNight.triggerAt)
        assertEquals(34, beforeNightfall.evening(may(25)).omerDay)
        assertTrue(beforeNightfall.overdue.none { it.date == may(24) })
        // Today's morning is still ahead and counts the day that begins at that nightfall
        assertEquals(33, beforeNightfall.mornings().single { it.date == may(25) }.omerDay)

        assertEquals(may(24), beforeNightfall.previousEvening)

        // Once that nightfall has passed it is kept as overdue, not scheduled again
        val afterNightfall = plan(may(25), LocalTime.of(0, 5))
        assertTrue(afterNightfall.alarms.none { it.date == may(24) })
        assertEquals(33, afterNightfall.overdue.filterIsInstance<ReminderAlarm.OmerEvening>().single { it.date == may(24) }.omerDay)
        assertEquals(34, afterNightfall.evening(may(25)).omerDay)
    }

    @Test
    fun `previous evening is not planned when its reminder is off or the Omer is over`() {
        val noTzait = plan(may(25), LocalTime.of(0, 1), prefs = omerPrefs.copy(notifyOmerTzait = false))
        assertTrue(noTzait.evenings().isEmpty())
        assertTrue(noTzait.overdue.none { it is ReminderAlarm.OmerEvening })
        assertEquals(may(24), noTzait.previousEvening)

        val noOmer = plan(may(25), LocalTime.of(0, 1), prefs = omerPrefs.copy(notifyOmer = false))
        assertTrue(noOmer.alarms.isEmpty())
        assertTrue(noOmer.overdue.isEmpty())

        // Erev Shavuot 2027 (5 Sivan, 10 June) has no evening count; in Riga its nightfall is after midnight
        val afterLastEve = plan(LocalDate.of(2027, 6, 11), LocalTime.of(0, 1), location = riga)
        assertTrue(afterLastEve.evenings().isEmpty())
        assertTrue(afterLastEve.mornings().isEmpty())
        assertTrue(afterLastEve.overdue.isEmpty())
    }

    @Test
    fun `first Omer evening counts 1 and has no morning reminder that day`() {
        val result = plan(firstNight2027, LocalTime.NOON)
        val first = result.evening(firstNight2027)

        assertEquals(1, first.omerDay)
        assertEquals(firstNight2027, first.local(spbZone).toLocalDate())
        assertTrue(result.mornings().none { it.date == firstNight2027 })
        assertEquals(1, result.mornings().single { it.date == firstNight2027.plusDays(1) }.omerDay)
    }

    @Test
    fun `last Omer evening after midnight counts 49 and nothing follows it`() {
        val rigaZone = ZoneId.of(riga.timezoneId)
        val lastEve = LocalDate.of(2027, 6, 9)
        val result = plan(lastEve.minusDays(1), LocalTime.NOON, location = riga)

        val last = result.evening(lastEve)
        assertEquals(49, last.omerDay)
        assertEquals(LocalDate.of(2027, 6, 10), last.local(rigaZone).toLocalDate())
        assertEquals(zmanim.getTzaitInstant(lastEve, riga), last.triggerAt)
        assertEquals(48, result.evening(lastEve.minusDays(1)).omerDay)
        assertTrue(result.evenings().none { it.date > lastEve })
        assertEquals(49, result.mornings().single { it.date == LocalDate.of(2027, 6, 10) }.omerDay)
        assertTrue(result.mornings().none { it.date > LocalDate.of(2027, 6, 10) })

        val beforeLastNightfall = plan(LocalDate.of(2027, 6, 10), LocalTime.of(0, 1), location = riga)
        assertEquals(49, beforeLastNightfall.evening(lastEve).omerDay)
    }

    @Test
    fun `missing nightfall keeps morning reminders to the end of the Omer`() {
        val result = plan(LocalDate.of(2027, 6, 3), LocalTime.of(6, 0))

        assertNull(zmanim.getTzaitInstant(LocalDate.of(2027, 6, 9), stPetersburg))
        assertTrue(result.evenings().isEmpty())
        assertTrue(result.overdue.none { it is ReminderAlarm.OmerEvening })
        assertEquals((3..10).map { LocalDate.of(2027, 6, it) }, result.mornings().map { it.date })
        assertEquals((42..49).toList(), result.mornings().map { it.omerDay })
    }

    @Test
    fun `nightfall instant is kept across a DST change in the position's zone`() {
        // Chile leaves DST on 5 April 2026, during the first week of the Omer
        val zone = ZoneId.of(santiago.timezoneId)
        val result = plan(LocalDate.of(2026, 4, 1), LocalTime.NOON, location = santiago)
        val evenings = result.evenings()

        assertEquals((2..8).map { LocalDate.of(2026, 4, it) }, evenings.map { it.date })
        assertEquals((1..7).toList(), evenings.map { it.omerDay })
        evenings.forEach {
            val zoned = it.triggerAt.atZone(zone)
            assertEquals(it.date, zoned.toLocalDate())
            val offset = if (it.date < LocalDate.of(2026, 4, 5)) -3 else -4
            assertEquals(ZoneOffset.ofHours(offset), zoned.offset)
            assertEquals(zmanim.getTzaitInstant(it.date, santiago), it.triggerAt)
        }
    }

    @Test
    fun `nightfall instant does not depend on the device zone`() {
        val helsinkiZone = ZoneId.of(helsinki.timezoneId)
        val tokyo = ZoneId.of("Asia/Tokyo")
        val now = ZonedDateTime.of(may(24), LocalTime.NOON, helsinkiZone).toInstant()
        val result = planReminderAlarms(
            now.atZone(tokyo).toLocalDate(), now, tokyo, helsinki, omerPrefs, zmanim, calendar
        )

        val evening = result.evening(may(24))
        assertEquals(33, evening.omerDay)
        val zoned = evening.triggerAt.atZone(helsinkiZone)
        assertEquals(may(25), zoned.toLocalDate())
        assertEquals(ZoneOffset.ofHours(3), zoned.offset)
        assertEquals(zmanim.getTzaitInstant(may(24), helsinki), evening.triggerAt)
        // Mornings follow the device zone
        assertEquals(LocalTime.of(7, 0), result.mornings().first().triggerAt.atZone(tokyo).toLocalTime())
    }

    @Test
    fun `the last date of the lookahead plans its post-midnight evening`() {
        val result = plan(may(18), LocalTime.NOON)

        assertEquals(may(25), result.window.last())
        val last = result.evening(may(25))
        assertEquals(34, last.omerDay)
        assertEquals(may(26), last.local(spbZone).toLocalDate())
        assertTrue(result.evenings().none { it.date > may(25) })

        val oneDay = plan(may(24), LocalTime.NOON, lookaheadDays = 0)
        assertEquals(listOf(may(24)), oneDay.evenings().map { it.date })
        assertEquals(may(25), oneDay.evening(may(24)).local(spbZone).toLocalDate())
    }
}

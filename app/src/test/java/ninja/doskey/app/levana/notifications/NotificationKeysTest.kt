package ninja.doskey.app.levana.notifications

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.data.HolidayMapper
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class NotificationKeysTest {

    private val day = LocalDate.of(2026, 4, 20)

    private fun identity(key: NotificationKey) = key.tag to key.id

    @Test
    fun `omer morning differs from the same and the next evening`() {
        val morning = NotificationKeys.omerMorning(day)
        assertNotEquals(identity(morning), identity(NotificationKeys.omerTzait(day)))
        assertNotEquals(identity(morning), identity(NotificationKeys.omerTzait(day.plusDays(1))))
        assertNotEquals(
            identity(NotificationKeys.omerTzait(day)),
            identity(NotificationKeys.omerTzait(day.plusDays(1)))
        )
    }

    @Test
    fun `every reminder type on one date has its own identity`() {
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        val tevet = HolidayMapper.mapHoliday(JewishCalendar.TENTH_OF_TEVES)!!
        val keys = listOf(
            NotificationKeys.candleLighting(day),
            NotificationKeys.holiday(day, chanukah),
            NotificationKeys.fast(day, tevet),
            NotificationKeys.personalEvent(day, 1, "A", null),
            NotificationKeys.birthday(day, "lookup", "A", null),
            NotificationKeys.omerTzait(day),
            NotificationKeys.omerMorning(day)
        )
        assertEquals(keys.size, keys.map(::identity).toSet().size)
        assertEquals(keys.size, keys.map { it.id }.toSet().size)
    }

    @Test
    fun `full date distinguishes the same day of year across years and year ends`() {
        val dec31 = LocalDate.of(2026, 12, 31)
        val nextYearSameDayOfYear = LocalDate.of(2027, 1, 1).withDayOfYear(dec31.dayOfYear)
        for (key in listOf(
            NotificationKeys::candleLighting,
            NotificationKeys::omerTzait,
            NotificationKeys::omerMorning
        )) {
            assertNotEquals(identity(key(dec31)), identity(key(nextYearSameDayOfYear)))
            assertNotEquals(identity(key(dec31)), identity(key(dec31.plusDays(1))))
        }
        assertNotEquals(
            identity(NotificationKeys.personalEvent(LocalDate.of(2025, 3, 1), 7, "A", null)),
            identity(NotificationKeys.personalEvent(LocalDate.of(2026, 3, 1), 7, "A", null))
        )
    }

    @Test
    fun `simultaneous observances on one date are distinct`() {
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        val roshChodesh = HolidayMapper.mapHoliday(JewishCalendar.ROSH_CHODESH)!!
        assertNotEquals(
            identity(NotificationKeys.holiday(day, chanukah)),
            identity(NotificationKeys.holiday(day, roshChodesh))
        )
    }

    @Test
    fun `same title events and same name contacts are distinct`() {
        assertNotEquals(
            identity(NotificationKeys.personalEvent(day, 1, "Yahrzeit", null)),
            identity(NotificationKeys.personalEvent(day, 2, "Yahrzeit", null))
        )
        assertNotEquals(
            identity(NotificationKeys.birthday(day, "key-1", "David Cohen", null)),
            identity(NotificationKeys.birthday(day, "key-2", "David Cohen", null))
        )
    }

    @Test
    fun `the same reminder always gets the same identity`() {
        val delayed = OccurrenceLabel.DelayedFrom30(HebrewMonth.CHESHVAN)
        assertEquals(
            identity(NotificationKeys.personalEvent(day, 3, "Old title", null)),
            identity(NotificationKeys.personalEvent(day, 3, "Edited title", null))
        )
        assertEquals(
            identity(NotificationKeys.birthday(day, "key-1", "David", delayed)),
            identity(NotificationKeys.birthday(day, "key-1", "David", delayed))
        )
        val pesach = HolidayMapper.mapHoliday(JewishCalendar.PESACH)!!
        assertEquals(
            identity(NotificationKeys.holiday(day, pesach)),
            identity(NotificationKeys.holiday(day, pesach.copy()))
        )
    }

    @Test
    fun `contacts without a lookup key fall back to the name`() {
        assertNotEquals(
            identity(NotificationKeys.birthday(day, "", "Sarah", null)),
            identity(NotificationKeys.birthday(day, "", "Rivka", null))
        )
        // A lookup key equal to some name cannot alias the name fallback
        assertNotEquals(
            identity(NotificationKeys.birthday(day, "Sarah", "X", null)),
            identity(NotificationKeys.birthday(day, "", "Sarah", null))
        )
    }

    @Test
    fun `legacy ids match the previous scheme`() {
        // 2026-04-20 is day 110 of the year
        assertEquals(110, NotificationKeys.candleLighting(day).legacyId)
        assertEquals(40110, NotificationKeys.omerTzait(day).legacyId)
        assertEquals(40111, NotificationKeys.omerMorning(day).legacyId)
        assertEquals(
            30110 + "event|Yahrzeit|null".hashCode() % 1000,
            NotificationKeys.personalEvent(day, 9, "Yahrzeit", null).legacyId
        )
        assertEquals(
            30110 + "birthday|David|null".hashCode() % 1000,
            NotificationKeys.birthday(day, "k", "David", null).legacyId
        )
    }
}

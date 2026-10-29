package ninja.doskey.app.levana.widget

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.GregorianCalendar
import kotlinx.coroutines.flow.flowOf
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.ui.calendar.monthGridWeeks
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthWidgetSnapshotsTest {

    private val snapshots = MonthWidgetSnapshots(CalendarRepository())
    private val jerusalemZone = ZoneId.of("Asia/Jerusalem")
    private val israel = UserPreferences(currentLocation = Location.JERUSALEM, isInIsrael = true)
    private val diaspora = UserPreferences(
        currentLocation = Location(40.7128, -74.006, 10.0, "America/New_York", "New York"),
        isInIsrael = false,
    )

    private fun device(date: LocalDate, time: String = "15:00", zone: ZoneId = jerusalemZone) =
        DeviceNow(date.atTime(LocalTime.parse(time)).atZone(zone).toInstant(), zone, Location.JERUSALEM)

    private fun hebrewDate(year: Int, month: Int, day: Int): LocalDate {
        val gc = JewishDate(year, month, day).gregorianCalendar
        return LocalDate.of(gc.get(GregorianCalendar.YEAR), gc.get(GregorianCalendar.MONTH) + 1, gc.get(GregorianCalendar.DAY_OF_MONTH))
    }

    private fun MonthWidgetSnapshot.day(hebrewDay: Int): HebrewDay = days.single { it.day == hebrewDay }

    // --- Month and grid boundaries ---

    @Test
    fun `shows the Hebrew month of today`() {
        // 18 Cheshvan 5787
        val snapshot = snapshots.build(israel, device(LocalDate.of(2026, 10, 29)))
        assertEquals(5787, snapshot.month.year)
        assertEquals(HebrewMonth.CHESHVAN, snapshot.month.month)
        assertEquals(1, snapshot.days.first().day)
        assertEquals(snapshot.month.daysInMonth(), snapshot.days.size)
        assertTrue(snapshot.days.all { it.month == HebrewMonth.CHESHVAN && it.year == 5787 })
    }

    @Test
    fun `a month starting on Shabbat fills six weeks with leading and trailing blanks`() {
        // 1 Tishrei 5787 is Shabbat, 12 September 2026; Tishrei has 30 days
        val snapshot = snapshots.build(israel, device(LocalDate.of(2026, 9, 20)))
        assertEquals(DayOfWeek.SATURDAY, snapshot.days.first().gregorianDate.dayOfWeek)
        assertEquals(6, snapshot.weeks.size)
        assertTrue(snapshot.weeks.all { it.size == 7 })
        assertEquals(List(6) { null }, snapshot.weeks.first().take(6))
        assertEquals(1, snapshot.weeks.first()[6]?.day)
        // 36 cells used of 42
        assertEquals(30, snapshot.weeks.last().indexOfLast { it != null }.let { snapshot.weeks.last()[it]?.day })
        assertEquals(6, snapshot.weeks.last().count { it == null })
    }

    @Test
    fun `each grid column is one weekday, Sunday first`() {
        val snapshot = snapshots.build(israel, device(LocalDate.of(2026, 10, 29)))
        snapshot.weeks.forEach { week ->
            week.forEachIndexed { column, day ->
                if (day != null) assertEquals(column, day.gregorianDate.dayOfWeek.value % 7)
            }
        }
        assertEquals(snapshot.days, snapshot.weeks.flatten().filterNotNull())
    }

    @Test
    fun `month starting on Sunday has no leading blanks`() {
        val days = CalendarRepository().getMonthDays(java.time.YearMonth.of(2026, 11))
        // 1 November 2026 is a Sunday
        val weeks = monthGridWeeks(days)
        assertEquals(1, weeks.first().first()?.gregorianDate?.dayOfMonth)
        assertEquals(5, weeks.size)
        assertEquals(emptyList<List<HebrewDay?>>(), monthGridWeeks(emptyList()))
    }

    // --- Leap year ---

    @Test
    fun `leap year shows Adar I without Purim and Adar II with it`() {
        // 5787 is a leap year
        val adarI = snapshots.build(diaspora, device(hebrewDate(5787, JewishDate.ADAR, 14)))
        assertEquals(HebrewMonth.ADAR_I, adarI.month.month)
        assertEquals(30, adarI.days.size)
        assertNotEquals(HolidayCategory.RABBINIC, adarI.day(14).holidayCategory)
        assertEquals("Purim Katan", adarI.day(14).holidayLabel)

        val adarII = snapshots.build(diaspora, device(hebrewDate(5787, JewishDate.ADAR_II, 1)))
        assertEquals(HebrewMonth.ADAR_II, adarII.month.month)
        assertEquals(29, adarII.days.size)
        assertEquals("Purim", adarII.day(14).holidayLabel)
    }

    @Test
    fun `common year has a single Adar with Purim`() {
        // 5786 is a common year
        val adar = snapshots.build(diaspora, device(hebrewDate(5786, JewishDate.ADAR, 3)))
        assertEquals(HebrewMonth.ADAR, adar.month.month)
        assertEquals(29, adar.days.size)
        assertEquals("Purim", adar.day(14).holidayLabel)
    }

    // --- Today ---

    @Test
    fun `today is in the grid exactly once`() {
        val today = LocalDate.of(2026, 10, 29)
        val snapshot = snapshots.build(israel, device(today))
        assertEquals(today, snapshot.today)
        val todays = snapshot.weeks.flatten().filter { it?.gregorianDate == today }
        assertEquals(1, todays.size)
        assertEquals(18, todays.single()?.day)
    }

    @Test
    fun `today follows the device zone across midnight`() {
        // 23:30 in New York is already the next day in Jerusalem
        val newYork = ZoneId.of("America/New_York")
        val instant = LocalDate.of(2026, 10, 28).atTime(23, 30).atZone(newYork).toInstant()
        assertEquals(LocalDate.of(2026, 10, 28), snapshots.build(diaspora, DeviceNow(instant, newYork, diaspora.currentLocation)).today)
        assertEquals(LocalDate.of(2026, 10, 29), snapshots.build(israel, DeviceNow(instant, jerusalemZone, Location.JERUSALEM)).today)
    }

    // --- Israel vs Diaspora ---

    @Test
    fun `the eighth day of Pesach is a Yom Tov only in the Diaspora`() {
        val date = hebrewDate(5786, JewishDate.NISSAN, 1)
        val inIsrael = snapshots.build(israel, device(date)).day(22)
        val outside = snapshots.build(diaspora, device(date)).day(22)
        assertEquals(HolidayCategory.TORAH, outside.holidayCategory)
        assertNotEquals(HolidayCategory.TORAH, inIsrael.holidayCategory)
        // Both share the seventh day
        assertEquals(HolidayCategory.TORAH, snapshots.build(israel, device(date)).day(21).holidayCategory)
    }

    @Test
    fun `Simchat Torah falls on 22 Tishrei in Israel and 23 in the Diaspora`() {
        val date = LocalDate.of(2026, 9, 20)
        val inIsrael = snapshots.build(israel, device(date))
        val outside = snapshots.build(diaspora, device(date))
        assertEquals(HolidayCategory.TORAH, outside.day(23).holidayCategory)
        assertNotEquals(HolidayCategory.TORAH, inIsrael.day(23).holidayCategory)
    }

    @Test
    fun `modern Israeli holidays follow the setting`() {
        // Yom HaAtzmaut 5786 is moved to 4 Iyar (Tuesday 21 April 2026)
        val date = hebrewDate(5786, JewishDate.IYAR, 1)
        val shown = snapshots.build(israel, device(date)).days.filter { it.holidayCategory == HolidayCategory.MODERN_ISRAELI }
        val hidden = snapshots.build(israel.copy(showModernIsraeliHolidays = false), device(date))
            .days.filter { it.holidayCategory == HolidayCategory.MODERN_ISRAELI }
        assertTrue(shown.isNotEmpty())
        assertTrue(hidden.isEmpty())
    }

    // --- Developer override and refresh ---

    @Test
    fun `the developer override's fixed date picks the month and today`() {
        val override = hebrewDate(5787, JewishDate.ADAR_II, 14)
        val prefs = israel.copy(devDateOverride = override)
        val realNow = LocalDate.of(2026, 10, 29).atTime(15, 0).atZone(jerusalemZone)
        val device = DeviceContext(flowOf(prefs), systemClock = { Clock.fixed(realNow.toInstant(), jerusalemZone) }).now(prefs)
        val snapshot = snapshots.build(prefs, device)
        assertEquals(override, snapshot.today)
        assertEquals(HebrewMonth.ADAR_II, snapshot.month.month)
        // The forced theme setting is unset, so the date's own holiday theme applies
        assertEquals(HolidayTheme.PURIM, snapshot.holidayTheme)
        // The refresh is at the next real midnight
        assertEquals(LocalDate.of(2026, 10, 30).atStartOfDay(jerusalemZone).toInstant(), snapshot.refreshAt)
    }

    @Test
    fun `refreshes at the device's next midnight`() {
        val snapshot = snapshots.build(israel, device(LocalDate.of(2026, 10, 29), "23:59"))
        assertEquals(LocalDate.of(2026, 10, 30).atStartOfDay(jerusalemZone).toInstant(), snapshot.refreshAt)
        assertNull(snapshots.build(israel.copy(dynamicHolidayTheme = false), device(hebrewDate(5787, JewishDate.ADAR_II, 14))).holidayTheme)
    }
}

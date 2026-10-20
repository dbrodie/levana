package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.TimeZone
import ninja.doskey.app.levana.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Independent verifier sweep. The holy-day oracle is built from Hebrew month/day only
 * (not KosherJava's yomTovIndex / isAssurBemelacha flags), then every civil day in
 * 2024-2040 is checked for Diaspora (New York) and Israel (Jerusalem).
 */
class VerifyHolyDayTransitionsTest {

    private val repo = ZmanimRepository()
    private val newYork = Location(40.7128, -74.0060, 0.0, "America/New_York", "New York")
    private val jerusalem = Location.JERUSALEM

    /** Is the civil daytime of [date] Shabbat or a Yom Tov on which melacha is forbidden? */
    private fun oracleHoly(date: LocalDate, inIsrael: Boolean): Boolean {
        if (date.dayOfWeek == DayOfWeek.SATURDAY) return true
        val jc = JewishCalendar(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))
        val m = jc.jewishMonth
        val d = jc.jewishDayOfMonth
        val diaspora = !inIsrael
        return when (m) {
            JewishCalendar.TISHREI -> d == 1 || d == 2 || d == 10 || d == 15 || d == 22 ||
                (diaspora && (d == 16 || d == 23))
            JewishCalendar.NISSAN -> d == 15 || d == 21 || (diaspora && (d == 16 || d == 22))
            JewishCalendar.SIVAN -> d == 6 || (diaspora && d == 7)
            else -> false
        }
    }

    private data class Expected(val candles: LocalTime?, val havdalah: LocalTime?)

    private fun expected(date: LocalDate, loc: Location, inIsrael: Boolean): Expected {
        val today = oracleHoly(date, inIsrael)
        val tomorrow = oracleHoly(date.plusDays(1), inIsrael)
        val tomorrowIsShabbat = date.plusDays(1).dayOfWeek == DayOfWeek.SATURDAY
        val candles = when {
            !tomorrow -> null
            // Lighting may not happen on Shabbat/Yom Tov before it ends, unless lighting for Shabbat
            today && !tomorrowIsShabbat -> tzeit(date, loc)
            else -> beforeSunset(date, loc)
        }
        val havdalah = if (today && !tomorrow) tzeit(date, loc) else null
        return Expected(candles, havdalah)
    }

    @Test
    fun `sweep 2024-2040 matches independent oracle for Diaspora and Israel`() {
        val failures = mutableListOf<String>()
        var holyTransitions = 0
        for ((loc, inIsrael) in listOf(newYork to false, jerusalem to true)) {
            var date = LocalDate.of(2024, 1, 1)
            while (date.year <= 2040) {
                val exp = expected(date, loc, inIsrael)
                val info = repo.getShabbatInfo(date, loc, 18.0, inIsrael)
                val actual = Expected(info.candleLightingTime, info.havdalahTime)
                if (exp != actual) failures += "$date ${date.dayOfWeek} israel=$inIsrael exp=$exp act=$actual"
                if (info.showCandleLighting != (exp.candles != null) ||
                    info.showHavdalah != (exp.havdalah != null)
                ) failures += "$date show flags israel=$inIsrael"
                if (repo.hasCandles(date, inIsrael) != (exp.candles != null)) {
                    failures += "$date hasCandles israel=$inIsrael"
                }
                // Observance end: last consecutive holy day after the evening of [date]
                val end = repo.getObservanceEnd(date, loc, inIsrael)
                if (!oracleHoly(date.plusDays(1), inIsrael)) {
                    if (end != null) failures += "$date end should be null israel=$inIsrael"
                } else {
                    holyTransitions++
                    var last = date.plusDays(1)
                    while (oracleHoly(last.plusDays(1), inIsrael)) last = last.plusDays(1)
                    if (end?.date != last || end.time != tzeit(last, loc)) {
                        failures += "$date end exp=$last act=$end israel=$inIsrael"
                    }
                    // Notification "end" must coincide with the havdalah the day screen shows
                    val shown = repo.getShabbatInfo(last, loc, 18.0, inIsrael).havdalahTime
                    if (shown != end?.time) failures += "$date end/havdalah mismatch israel=$inIsrael"
                }
                date = date.plusDays(1)
            }
        }
        assertEquals(failures.take(30).joinToString("\n"), 0, failures.size)
        // Sanity: the sweep actually exercised holy days
        println("verify sweep holyTransitions=$holyTransitions")
        assertTrue("sweep exercised holy days: $holyTransitions", holyTransitions > 1500)
    }

    @Test
    fun `calendar grid candle marker agrees with ZmanimRepository hasCandles`() {
        val calendarRepo = CalendarRepository()
        val mismatches = mutableListOf<String>()
        for (inIsrael in listOf(false, true)) {
            var date = LocalDate.of(2024, 1, 1)
            while (date.year <= 2040) {
                val grid = calendarRepo.getHebrewDay(date, inIsrael).hasCandles
                if (grid != repo.hasCandles(date, inIsrael)) mismatches += "$date israel=$inIsrael grid=$grid"
                date = date.plusDays(1)
            }
        }
        assertEquals(mismatches.take(20).joinToString("\n"), 0, mismatches.size)
    }

    /** Old-API-compatible diaspora regressions (3-arg signature) to show baseline failure. */
    @Test
    fun `Shabbat into Pesach lights after nightfall and shows no havdalah - Diaspora`() {
        val shabbatErevPesach = LocalDate.of(2025, 4, 12) // Shabbat, 14 Nissan
        val info = repo.getShabbatInfo(shabbatErevPesach, newYork, 18.0)
        assertEquals(tzeit(shabbatErevPesach, newYork), info.candleLightingTime)
        assertEquals(null, info.havdalahTime)
    }

    @Test
    fun `first day Rosh Hashanah 2024 Thursday lights second-night candles after nightfall`() {
        val day1 = LocalDate.of(2024, 10, 3) // Thursday, 1 Tishrei 5785
        val day2 = day1.plusDays(1) // Friday
        val info1 = repo.getShabbatInfo(day1, newYork, 18.0)
        assertEquals(tzeit(day1, newYork), info1.candleLightingTime)
        assertEquals(null, info1.havdalahTime)
        // Yom Tov into Shabbat: before sunset, no havdalah on Friday night
        val info2 = repo.getShabbatInfo(day2, newYork, 18.0)
        assertEquals(beforeSunset(day2, newYork), info2.candleLightingTime)
        assertEquals(null, info2.havdalahTime)
    }

    @Test
    fun `Simchat Torah Friday into Shabbat 2024 - Friday has no havdalah`() {
        val simchatTorah = LocalDate.of(2024, 10, 25) // Friday, 23 Tishrei
        val info = repo.getShabbatInfo(simchatTorah, newYork, 18.0)
        assertEquals(beforeSunset(simchatTorah, newYork), info.candleLightingTime)
        assertEquals(null, info.havdalahTime)
    }

    private fun beforeSunset(date: LocalDate, loc: Location): LocalTime =
        cal(date, loc).apply { candleLightingOffset = 18.0 }.candleLighting.toLocal(loc)

    private fun tzeit(date: LocalDate, loc: Location): LocalTime = cal(date, loc).tzais.toLocal(loc)

    private fun cal(date: LocalDate, loc: Location): ComplexZmanimCalendar {
        val tz = TimeZone.getTimeZone(loc.timezoneId)
        return ComplexZmanimCalendar(GeoLocation(loc.name, loc.latitude, loc.longitude, loc.elevation, tz))
            .apply {
                calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
                    .also { it.timeZone = tz }
            }
    }

    private fun java.util.Date.toLocal(loc: Location): LocalTime =
        toInstant().atZone(ZoneId.of(loc.timezoneId)).toLocalTime()
}

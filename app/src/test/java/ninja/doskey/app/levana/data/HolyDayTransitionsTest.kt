package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.TimeZone
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.ShabbatInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Candle lighting and havdalah across Shabbat / Yom Tov transitions, 5786–5787 (2026).
 * Each scenario first checks the date against KosherJava's holiday index.
 */
class HolyDayTransitionsTest {

    private val repo = ZmanimRepository()
    private val offset = 18.0
    private val newYork = Location(
        latitude = 40.7128,
        longitude = -74.0060,
        timezoneId = "America/New_York",
        name = "New York",
        country = "USA"
    )
    private val jerusalem = Location.JERUSALEM

    @Test
    fun `ordinary Friday and Saturday are unchanged`() {
        val friday = LocalDate.of(2026, 7, 31)
        val saturday = friday.plusDays(1)
        assertYomTovIndex(-1, friday, inIsrael = false)
        assertYomTovIndex(-1, saturday, inIsrael = false)

        for ((location, inIsrael) in listOf(newYork to false, jerusalem to true)) {
            assertBeforeSunset(info(friday, location, inIsrael), friday, location)
            assertNull(info(friday, location, inIsrael).havdalahTime)
            assertHavdalahOnly(info(saturday, location, inIsrael), saturday, location)
            assertEquals(
                ObservanceEnd(saturday, tzeit(saturday, location), includesYomTov = false),
                repo.getObservanceEnd(friday, location, inIsrael)
            )
        }
    }

    @Test
    fun `Rosh Hashanah on Shabbat and Sunday - Shabbat into Yom Tov lights after nightfall`() {
        val erev = LocalDate.of(2026, 9, 11) // Friday
        val day1 = erev.plusDays(1) // Shabbat, 1 Tishrei
        val day2 = erev.plusDays(2) // Sunday, 2 Tishrei
        assertYomTovIndex(JewishCalendar.EREV_ROSH_HASHANA, erev, inIsrael = false)
        assertYomTovIndex(JewishCalendar.ROSH_HASHANA, day1, inIsrael = false)
        assertYomTovIndex(JewishCalendar.ROSH_HASHANA, day2, inIsrael = true)

        for ((location, inIsrael) in listOf(newYork to false, jerusalem to true)) {
            assertBeforeSunset(info(erev, location, inIsrael), erev, location)
            assertAfterNightfall(info(day1, location, inIsrael), day1, location)
            assertHavdalahOnly(info(day2, location, inIsrael), day2, location)
            assertEquals(
                ObservanceEnd(day2, tzeit(day2, location), includesYomTov = true),
                repo.getObservanceEnd(erev, location, inIsrael)
            )
        }
    }

    @Test
    fun `Pesach from Thursday - Diaspora Yom Tov into Yom Tov into Shabbat`() {
        val erev = LocalDate.of(2026, 4, 1) // Wednesday, 14 Nissan
        val thursday = erev.plusDays(1)
        val friday = erev.plusDays(2)
        val saturday = erev.plusDays(3)
        assertYomTovIndex(JewishCalendar.EREV_PESACH, erev, inIsrael = false)
        assertYomTovIndex(JewishCalendar.PESACH, thursday, inIsrael = false)
        assertYomTovIndex(JewishCalendar.PESACH, friday, inIsrael = false)

        assertBeforeSunset(info(erev, newYork, false), erev, newYork)
        // Second-night candles: only after the first day ends, no havdalah in between
        assertAfterNightfall(info(thursday, newYork, false), thursday, newYork)
        // Yom Tov into Shabbat: Shabbat candles before sunset, still no havdalah
        assertBeforeSunset(info(friday, newYork, false), friday, newYork)
        assertNull(info(friday, newYork, false).havdalahTime)
        assertHavdalahOnly(info(saturday, newYork, false), saturday, newYork)
        assertEquals(
            ObservanceEnd(saturday, tzeit(saturday, newYork), includesYomTov = true),
            repo.getObservanceEnd(erev, newYork, inIsrael = false)
        )
        assertEquals(
            ObservanceEnd(saturday, tzeit(saturday, newYork), includesYomTov = true),
            repo.getObservanceEnd(thursday, newYork, inIsrael = false)
        )
    }

    @Test
    fun `Pesach from Thursday - Israel ends after one day, Friday is plain Erev Shabbat`() {
        val erev = LocalDate.of(2026, 4, 1)
        val thursday = erev.plusDays(1)
        val friday = erev.plusDays(2)
        assertYomTovIndex(JewishCalendar.CHOL_HAMOED_PESACH, friday, inIsrael = true)

        assertBeforeSunset(info(erev, jerusalem, true), erev, jerusalem)
        assertHavdalahOnly(info(thursday, jerusalem, true), thursday, jerusalem)
        assertBeforeSunset(info(friday, jerusalem, true), friday, jerusalem)
        assertEquals(
            ObservanceEnd(thursday, tzeit(thursday, jerusalem), includesYomTov = true),
            repo.getObservanceEnd(erev, jerusalem, inIsrael = true)
        )
        assertFalse(repo.hasCandles(thursday, inIsrael = true))
        assertTrue(repo.hasCandles(thursday, inIsrael = false))
    }

    @Test
    fun `Sukkot on Shabbat - Diaspora lights after Shabbat, Israel makes havdalah`() {
        val erev = LocalDate.of(2026, 9, 25) // Friday
        val shabbat = erev.plusDays(1) // 15 Tishrei
        val sunday = erev.plusDays(2)
        assertYomTovIndex(JewishCalendar.EREV_SUCCOS, erev, inIsrael = false)
        assertYomTovIndex(JewishCalendar.SUCCOS, shabbat, inIsrael = false)
        assertYomTovIndex(JewishCalendar.SUCCOS, sunday, inIsrael = false)
        assertYomTovIndex(JewishCalendar.CHOL_HAMOED_SUCCOS, sunday, inIsrael = true)

        assertBeforeSunset(info(erev, newYork, false), erev, newYork)
        assertAfterNightfall(info(shabbat, newYork, false), shabbat, newYork)
        assertHavdalahOnly(info(sunday, newYork, false), sunday, newYork)

        assertBeforeSunset(info(erev, jerusalem, true), erev, jerusalem)
        assertHavdalahOnly(info(shabbat, jerusalem, true), shabbat, jerusalem)
        assertNoTimes(info(sunday, jerusalem, true))
    }

    @Test
    fun `Shemini Atzeret on Shabbat - Simchat Torah only in the Diaspora`() {
        val hoshanaRabba = LocalDate.of(2026, 10, 2) // Friday
        val shabbat = hoshanaRabba.plusDays(1)
        val sunday = hoshanaRabba.plusDays(2)
        assertYomTovIndex(JewishCalendar.HOSHANA_RABBA, hoshanaRabba, inIsrael = false)
        assertYomTovIndex(JewishCalendar.SHEMINI_ATZERES, shabbat, inIsrael = false)
        assertYomTovIndex(JewishCalendar.SIMCHAS_TORAH, sunday, inIsrael = false)

        assertBeforeSunset(info(hoshanaRabba, newYork, false), hoshanaRabba, newYork)
        assertAfterNightfall(info(shabbat, newYork, false), shabbat, newYork)
        assertHavdalahOnly(info(sunday, newYork, false), sunday, newYork)

        assertHavdalahOnly(info(shabbat, jerusalem, true), shabbat, jerusalem)
        assertNoTimes(info(sunday, jerusalem, true))
    }

    @Test
    fun `Yom Kippur lights before sunset and ends at nightfall`() {
        val erev = LocalDate.of(2026, 9, 20)
        val yomKippur = erev.plusDays(1)
        assertYomTovIndex(JewishCalendar.YOM_KIPPUR, yomKippur, inIsrael = false)

        assertBeforeSunset(info(erev, newYork, false), erev, newYork)
        assertHavdalahOnly(info(yomKippur, newYork, false), yomKippur, newYork)
    }

    @Test
    fun `minor festivals show neither candle lighting nor havdalah`() {
        val chanukah = LocalDate.of(2026, 12, 8) // Tuesday
        assertYomTovIndex(JewishCalendar.CHANUKAH, chanukah, inIsrael = false)
        val cholHamoed = LocalDate.of(2026, 9, 29) // Tuesday
        assertYomTovIndex(JewishCalendar.CHOL_HAMOED_SUCCOS, cholHamoed, inIsrael = false)

        assertNoTimes(info(chanukah, newYork, false))
        assertNoTimes(info(cholHamoed, newYork, false))
        assertNull(repo.getObservanceEnd(chanukah, newYork, inIsrael = false))
    }

    @Test
    fun `zmanim list follows the transition rules`() {
        val thursday = LocalDate.of(2026, 4, 2)
        val diaspora = repo.getZmanim(thursday, newYork, offset, inIsrael = false)
        assertEquals(
            tzeit(thursday, newYork),
            diaspora.single { it.name == ZmanimRepository.CANDLE_LIGHTING.english }.time
        )
        assertTrue(diaspora.none { it.name == ZmanimRepository.HAVDALAH.english })

        val israel = repo.getZmanim(thursday, jerusalem, offset, inIsrael = true)
        assertTrue(israel.none { it.name == ZmanimRepository.CANDLE_LIGHTING.english })
        assertEquals(
            tzeit(thursday, jerusalem),
            israel.single { it.name == ZmanimRepository.HAVDALAH.english }.time
        )
    }

    private fun info(date: LocalDate, location: Location, inIsrael: Boolean): ShabbatInfo =
        repo.getShabbatInfo(date, location, offset, inIsrael)

    private fun assertBeforeSunset(info: ShabbatInfo, date: LocalDate, location: Location) {
        assertEquals(candlesBeforeSunset(date, location), info.candleLightingTime)
        assertTrue(info.showCandleLighting)
    }

    private fun assertAfterNightfall(info: ShabbatInfo, date: LocalDate, location: Location) {
        assertEquals(tzeit(date, location), info.candleLightingTime)
        assertNull(info.havdalahTime)
        assertFalse(info.showHavdalah)
    }

    private fun assertHavdalahOnly(info: ShabbatInfo, date: LocalDate, location: Location) {
        assertNull(info.candleLightingTime)
        assertFalse(info.showCandleLighting)
        assertEquals(tzeit(date, location), info.havdalahTime)
        assertTrue(info.showHavdalah)
    }

    private fun assertNoTimes(info: ShabbatInfo) {
        assertNull(info.candleLightingTime)
        assertNull(info.havdalahTime)
    }

    private fun assertYomTovIndex(expected: Int, date: LocalDate, inIsrael: Boolean) {
        val jc = JewishCalendar(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))
        jc.inIsrael = inIsrael
        assertEquals("yomTovIndex on $date", expected, jc.yomTovIndex)
    }

    private fun candlesBeforeSunset(date: LocalDate, location: Location): LocalTime =
        calendarFor(location, date).apply { candleLightingOffset = offset }
            .candleLighting.toLocalTime(location)

    private fun tzeit(date: LocalDate, location: Location): LocalTime =
        calendarFor(location, date).tzais.toLocalTime(location)

    private fun calendarFor(location: Location, date: LocalDate): ComplexZmanimCalendar {
        val timezone = TimeZone.getTimeZone(location.timezoneId)
        return ComplexZmanimCalendar(
            GeoLocation(location.name, location.latitude, location.longitude, location.elevation, timezone)
        ).apply {
            calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth).also {
                it.timeZone = timezone
            }
        }
    }

    private fun java.util.Date.toLocalTime(location: Location): LocalTime = toInstant()
        .atZone(ZoneId.of(location.timezoneId))
        .toLocalTime()
}

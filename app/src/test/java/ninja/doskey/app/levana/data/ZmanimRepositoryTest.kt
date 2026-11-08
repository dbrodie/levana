package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.TimeZone
import ninja.doskey.app.levana.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZmanimRepositoryTest {

    private val date = LocalDate.of(2026, 7, 28)
    private val location = Location.JERUSALEM

    @Test
    fun `sunrise and sunset use sea-level times for an elevated location`() {
        val zmanim = ZmanimRepository().getZmanim(date, location)
        val calendar = calendarFor(location, date)

        assertEquals(
            calendar.seaLevelSunrise.toLocalTime(location.timezoneId),
            zmanim.single { it.name == "Netz hachama" }.time
        )
        assertEquals(
            calendar.seaLevelSunset.toLocalTime(location.timezoneId),
            zmanim.single { it.name == "Shkia" }.time
        )
        assertNotEquals(
            calendar.sunrise.toLocalTime(location.timezoneId),
            zmanim.single { it.name == "Netz hachama" }.time
        )
        assertNotEquals(
            calendar.sunset.toLocalTime(location.timezoneId),
            zmanim.single { it.name == "Shkia" }.time
        )
    }

    @Test
    fun `sunset helper uses sea-level sunset`() {
        val calendar = calendarFor(location, date)

        assertEquals(
            calendar.seaLevelSunset.toLocalTime(location.timezoneId),
            ZmanimRepository().getSunsetTime(date, location)
        )
    }

    @Test
    fun `zmanim are listed in chronological order in summer and winter`() {
        val newYork = Location(
            latitude = 40.7128,
            longitude = -74.0060,
            elevation = 10.0,
            timezoneId = "America/New_York",
            name = "New York",
            country = "USA"
        )
        // A Friday and a Shabbat in each season, so candle lighting and havdalah are included
        val dates = listOf(
            LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 25),
            LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 19)
        )
        val repo = ZmanimRepository()
        for (city in listOf(Location.JERUSALEM, newYork)) {
            for (day in dates) {
                val zmanim = repo.getZmanim(day, city)
                val minutes = zmanim.map { zman ->
                    val time = requireNotNull(zman.time) { "${city.name} $day ${zman.name} has no time" }
                    // Chatzot HaLaylah (and only it) may fall after civil midnight
                    val wraps = zman.name == ZmanimRepository.CHATZOT_HALAYLAH.english &&
                        time < LocalTime.NOON
                    time.toSecondOfDay() + if (wraps) 24 * 3600 else 0
                }
                assertTrue(
                    "${city.name} $day out of order: " +
                        zmanim.joinToString { "${it.name} ${it.time}" },
                    minutes.zipWithNext().all { (a, b) -> a <= b }
                )
            }
        }
    }

    @Test
    fun `a saved selection of the renamed Sof Zman Tefillah keeps the GRA opinion`() {
        assertEquals(
            setOf("Netz hachama", ZmanimRepository.SOF_ZMAN_TEFILLAH_GRA.english),
            parseSelectedZmanim(setOf("Netz hachama", "Sof Zman Tefillah"))
        )
        assertEquals(setOf("Netz hachama", "Shkia", "Tzeit hakochavim"), parseSelectedZmanim(null))
    }

    private fun calendarFor(location: Location, date: LocalDate): ComplexZmanimCalendar {
        val timezone = TimeZone.getTimeZone(location.timezoneId)
        return ComplexZmanimCalendar(
            GeoLocation(
                location.name,
                location.latitude,
                location.longitude,
                location.elevation,
                timezone
            )
        ).apply {
            calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth).also {
                it.timeZone = timezone
            }
        }
    }

    private fun java.util.Date.toLocalTime(timezoneId: String) = toInstant()
        .atZone(ZoneId.of(timezoneId))
        .toLocalTime()
}

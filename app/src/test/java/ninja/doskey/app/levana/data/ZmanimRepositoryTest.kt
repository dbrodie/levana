package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.TimeZone
import ninja.doskey.app.levana.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
            zmanim.single { it.name == "Sunrise" }.time
        )
        assertEquals(
            calendar.seaLevelSunset.toLocalTime(location.timezoneId),
            zmanim.single { it.name == "Sunset" }.time
        )
        assertNotEquals(
            calendar.sunrise.toLocalTime(location.timezoneId),
            zmanim.single { it.name == "Sunrise" }.time
        )
        assertNotEquals(
            calendar.sunset.toLocalTime(location.timezoneId),
            zmanim.single { it.name == "Sunset" }.time
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

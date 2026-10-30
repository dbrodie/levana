package ninja.doskey.app.levana.device

import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** TASK-46: when a location fix clearly doesn't fit the device's time zone. */
class ZoneMismatchTest {

    private val summer = Instant.parse("2026-07-01T12:00:00Z")
    private val winter = Instant.parse("2026-01-15T12:00:00Z")

    private val ukiah = place(39.1502, -123.2078)

    private fun place(latitude: Double, longitude: Double) =
        Location(latitude, longitude, 0.0, "UTC", "Here")

    private fun key(fix: Location, zone: String, at: Instant = summer, isFix: Boolean = true): String? {
        val prefs = UserPreferences(currentLocation = fix, currentLocationIsFix = isFix)
        return zoneMismatchKey(prefs, DeviceNow(at, ZoneId.of(zone), fix))
    }

    @Test
    fun aCaliforniaFixOnAJerusalemClockIsAMismatch() {
        // The TASK-48 emulator: GPS in Ukiah, zone from the host in Jerusalem
        assertNotNull(key(ukiah, "Asia/Jerusalem", summer))
        assertNotNull(key(ukiah, "Asia/Jerusalem", winter))
    }

    @Test
    fun aCaliforniaFixOnAKiritimatiClockIsAMismatch() {
        // Wrapped, the clocks are under 2 hours apart, but the date is a day ahead
        assertNotNull(key(ukiah, "Pacific/Kiritimati"))
    }

    @Test
    fun placesInTheirOwnZonesFitInSummerAndWinter() {
        val ownZones = listOf(
            "America/Los_Angeles" to ukiah,
            "Asia/Jerusalem" to place(31.7683, 35.2137),
            "America/New_York" to place(40.7128, -74.0060),
            "Europe/London" to place(51.5074, -0.1278),
            // Real zones far from solar time
            "Europe/Madrid" to place(40.4168, -3.7038),
            "Europe/Madrid" to place(42.2406, -8.7207), // Vigo, ~2.6 h in summer
            "Asia/Shanghai" to place(31.2304, 121.4737),
            "Asia/Shanghai" to place(39.4704, 75.9898), // Kashgar on Beijing time, ~2.9 h
            "Asia/Kolkata" to place(22.5726, 88.3639),
            "Asia/Kolkata" to place(23.0225, 72.5714), // Ahmedabad
            "Europe/Kaliningrad" to place(54.7104, 20.4522),
            "Europe/Moscow" to place(54.7826, 32.0453), // Smolensk
            "Europe/Samara" to place(53.1959, 50.1002),
            "America/Argentina/Mendoza" to place(-32.8895, -68.8458),
            "America/Nome" to place(64.5011, -165.4064), // ~3 h in summer
            "America/Adak" to place(51.8800, -176.6581),
            "America/St_Johns" to place(47.5615, -52.7126),
            "Asia/Kathmandu" to place(27.7172, 85.3240),
            // Either side of the date line
            "Pacific/Kiritimati" to place(1.8721, -157.4278),
            "Pacific/Tongatapu" to place(-21.1789, -175.1982),
            "Pacific/Apia" to place(-13.8333, -171.7500),
            "Pacific/Chatham" to place(-43.9535, -176.5597),
            "Pacific/Fiji" to place(-18.1416, 178.4419),
            "Asia/Anadyr" to place(64.7337, 177.4968),
        )
        for ((zone, fix) in ownZones) {
            assertNull("$zone at ${fix.longitude} in summer", key(fix, zone, summer))
            assertNull("$zone at ${fix.longitude} in winter", key(fix, zone, winter))
        }
        // Attu, at 173°E on the Aleutians' UTC-9/-10
        assertNull(key(place(52.9, 172.9), "America/Adak", summer))
        assertNull(key(place(52.9, 172.9), "America/Adak", winter))
    }

    @Test
    fun theFallbackCityNeverMismatches() {
        // Without a fix the position is a bundled stand-in, which may be far from the device
        assertNull(key(ukiah, "Asia/Jerusalem", isFix = false))
        assertNull(key(Location.JERUSALEM, "Pacific/Kiritimati", isFix = false))
    }

    @Test
    fun theKeyIsStableForOneMismatchAndChangesWithTheZoneOrPlace() {
        val nearby = place(38.4404, -122.7141) // Santa Rosa, a fresh fix nearby
        assertEquals(key(ukiah, "Asia/Jerusalem", summer), key(nearby, "Asia/Jerusalem", winter))
        assertNotEquals(key(ukiah, "Asia/Jerusalem"), key(ukiah, "Europe/Paris"))
        assertNotEquals(key(ukiah, "Asia/Jerusalem"), key(place(40.7128, -74.0060), "Asia/Jerusalem"))
    }

    @Test
    fun deviationsAreInHoursOfSolarTime() {
        assertEquals(0.0, solarDeviationHours(0.0, ZoneId.of("UTC"), summer), 1e-9)
        assertEquals(11.21, solarDeviationHours(-123.2078, ZoneId.of("Asia/Jerusalem"), summer), 0.01)
        assertEquals(0.49, solarDeviationHours(-157.4278, ZoneId.of("Pacific/Kiritimati"), summer), 0.01)
        assertEquals(22.21, solarDeviationHours(-123.2078, ZoneId.of("Pacific/Kiritimati"), summer), 0.01)
    }
}

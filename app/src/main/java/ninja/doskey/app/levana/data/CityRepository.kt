package ninja.doskey.app.levana.data

import android.content.Context
import ninja.doskey.app.levana.domain.model.Location
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CityRepository(private val context: Context) {

    private val cities: List<Location> by lazy { loadCities() }

    fun getAllCities(): List<Location> = cities

    fun search(query: String): List<Location> {
        if (query.isBlank()) return cities
        val lower = query.lowercase()
        return cities.filter { city ->
            city.name.lowercase().contains(lower) ||
                city.country.lowercase().contains(lower)
        }
    }

    /** The device position without a location fix; see [fallbackCity]. */
    fun defaultForTimeZone(zone: ZoneId, now: Instant): Location = fallbackCity(cities, zone, now)

    private fun loadCities(): List<Location> {
        val json = context.assets.open("cities.json")
            .bufferedReader().use { it.readText() }
        val cityDtos = Json.decodeFromString<List<CityDto>>(json)
        return cityDtos.map { it.toLocation() }
    }

    @Serializable
    private data class CityDto(
        val name: String,
        val country: String,
        val lat: Double,
        val lon: Double,
        val elev: Double,
        val tz: String
    ) {
        fun toLocation() = Location(
            latitude = lat,
            longitude = lon,
            elevation = elev,
            timezoneId = tz,
            name = name,
            country = country
        )
    }
}

/**
 * The best stand-in for the device position when there is no location fix: the first city in
 * the device's [zone] (or an alias of it, i.e. the same rules), otherwise the city whose UTC
 * offset at [now] is closest to the zone's. Ties go to a city outside Israel (a fallback must
 * not put a foreign device on Jerusalem's times), then to the city at or west of the device's
 * offset, then by name, so the choice is deterministic.
 */
internal fun fallbackCity(cities: List<Location>, zone: ZoneId, now: Instant): Location {
    val rules = zone.rules
    cities.firstOrNull { it.timezoneId == zone.id }?.let { return it }
    cities.firstOrNull { ZoneId.of(it.timezoneId).rules == rules }?.let { return it }
    val offset = rules.getOffset(now).totalSeconds
    fun Location.offset() = ZoneId.of(timezoneId).rules.getOffset(now).totalSeconds
    return cities.minWithOrNull(
        compareBy<Location> { Math.abs(it.offset() - offset) }
            .thenBy { it.timezoneId == ISRAEL_ZONE }
            .thenBy { it.offset() > offset }
            .thenBy { it.name }
    ) ?: Location.JERUSALEM
}

private const val ISRAEL_ZONE = "Asia/Jerusalem"


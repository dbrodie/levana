package ninja.doskey.app.levana.ui

import ninja.doskey.app.levana.domain.model.Location
import java.time.ZoneId

/**
 * The selected city's name when its clock differs from the device's, else null. The Zmanim
 * screen and day panel show the city's times in its own clock and then label them
 * ("Jerusalem time"). Zones with the same rules (aliases) count as the same clock.
 */
fun otherClockCityName(displayCity: Location, deviceZone: ZoneId): String? =
    displayCity.name.takeIf { ZoneId.of(displayCity.timezoneId).rules != deviceZone.rules }

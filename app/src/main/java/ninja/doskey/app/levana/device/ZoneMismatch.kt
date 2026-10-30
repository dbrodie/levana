package ninja.doskey.app.levana.device

import ninja.doskey.app.levana.domain.model.UserPreferences
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How far a real time zone may sit from solar time and still count as fitting a position.
 * Real zones reach about 3.5 hours (western China on Beijing time, Galicia in summer, the
 * Aleutians in summer), so only a clearly wrong zone goes past it.
 */
internal const val MAX_SOLAR_DEVIATION_HOURS = 4.0

/**
 * Only this close to the date line can a real zone be a day ahead of or behind solar time
 * (Kiribati's Line Islands at UTC+14, Tonga and Samoa at UTC+13, the western Aleutians).
 */
internal const val DATE_LINE_BAND_DEGREES = 145.0

/**
 * Hours between [zone]'s offset at [at] and solar time at [longitude] (15° an hour). Near the
 * date line it is wrapped to -12..12; elsewhere a day's difference is a mismatch, so a
 * California fix on a Kiritimati clock is caught although the clock times look close.
 */
internal fun solarDeviationHours(longitude: Double, zone: ZoneId, at: Instant): Double {
    val offsetHours = zone.rules.getOffset(at).totalSeconds / 3600.0
    val deviation = offsetHours - longitude / 15.0
    return if (abs(longitude) >= DATE_LINE_BAND_DEGREES) (deviation + 12.0).mod(24.0) - 12.0
    else deviation
}

/**
 * The device's location fix clearly doesn't fit its time zone (TASK-46): its times are still
 * shown on the device clock (TASK-5), so the user is asked to fix the date & time settings.
 * Null when it fits, or when the position is the zone's fallback city rather than a fix.
 *
 * The key names the mismatch (device zone and the fix's solar hour), so the notice shows once
 * per mismatch rather than on every open.
 */
fun zoneMismatchKey(prefs: UserPreferences, device: DeviceNow): String? {
    if (!prefs.currentLocationIsFix) return null
    val longitude = prefs.currentLocation.longitude
    if (abs(solarDeviationHours(longitude, device.zone, device.instant)) <= MAX_SOLAR_DEVIATION_HOURS) {
        return null
    }
    return "${device.zone.id}|${(longitude / 15.0).roundToInt()}"
}

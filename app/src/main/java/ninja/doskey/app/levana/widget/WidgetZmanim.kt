package ninja.doskey.app.levana.widget

import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.ZmanCategory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** One of the user's selected zmanim on a day, at its civil moment; null when it can't be computed. */
data class DatedZman(val name: String, val hebrewName: String, val at: ZonedDateTime?)

/**
 * [date]'s zmanim as the day details list them: the user's selection, in the same order, in
 * [location]'s zone. A night zman before noon (halachic midnight after civil midnight) belongs
 * to the next civil day.
 */
internal fun ZmanimRepository.selectedZmanimOn(
    prefs: UserPreferences,
    location: Location,
    date: LocalDate,
): List<DatedZman> {
    val zone = ZoneId.of(location.timezoneId)
    return getZmanim(date, location, prefs.candleLightingOffset, prefs.isInIsrael)
        .filter { it.name in prefs.selectedZmanim }
        .map { zman ->
            val at = zman.time?.let { time ->
                val day = if (zman.category == ZmanCategory.NIGHT && time < LocalTime.NOON) date.plusDays(1) else date
                day.atTime(time).atZone(zone)
            }
            DatedZman(zman.name, zman.hebrewName, at)
        }
}

/** The first selected zman after [now], from [today]'s list and then tomorrow's. */
internal fun ZmanimRepository.nextSelectedZman(
    prefs: UserPreferences,
    location: Location,
    today: LocalDate,
    now: Instant,
): NextZman? = listOf(today, today.plusDays(1)).firstNotNullOfOrNull { date ->
    selectedZmanimOn(prefs, location, date)
        .mapNotNull { zman -> zman.at?.let { NextZman(zman.name, zman.hebrewName, it) } }
        .filter { it.at.toInstant() > now }
        .minByOrNull { it.at }
}

/** When a widget showing [next] goes stale, on the real clock: [next] passing, or midnight. */
internal fun widgetRefreshAt(device: DeviceNow, next: NextZman?): Instant {
    val staleAt = next?.at?.toInstant()?.takeIf { it < device.nextMidnight } ?: device.nextMidnight
    return device.toSystemInstant(staleAt)
}

package ninja.doskey.app.levana.widget

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.ZmanCategory
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import ninja.doskey.app.levana.ui.theme.activeHolidayTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The next of the user's selected zmanim, in the location's zone. */
data class NextZman(val name: String, val hebrewName: String, val at: ZonedDateTime)

/**
 * Candle lighting and havdalah of one run of consecutive Shabbat / Yom Tov days. A time is
 * null when it can't be computed (no nightfall at high latitudes); the date is still known.
 */
data class ShabbatTimes(
    val candleLightingDate: LocalDate,
    val candleLighting: ZonedDateTime?,
    val havdalahDate: LocalDate,
    val havdalah: ZonedDateTime?,
)

/** Everything the Today widget shows, computed once per update. */
data class TodayWidgetSnapshot(
    val today: LocalDate,
    val hebrewDay: HebrewDay,
    val holiday: Holiday?,
    val locationName: String,
    val nextZman: NextZman?,
    val shabbat: ShabbatTimes?,
    val holidayTheme: HolidayTheme?,
    /** When the snapshot goes stale, on the real clock: the next zman passing, or midnight. */
    val refreshAt: Instant,
)

class TodayWidgetSnapshots(
    private val zmanimRepository: ZmanimRepository,
    private val calendarRepository: CalendarRepository,
) {

    /**
     * The snapshot for the [device]: its date and moment (developer override included), and
     * the zmanim and Shabbat times at its position (owner decision, TASK-5), never the
     * selected city's.
     */
    fun build(prefs: UserPreferences, device: DeviceNow): TodayWidgetSnapshot {
        val today = device.today
        val position = device.position
        val dayInfo = calendarRepository.getDayInfo(today, prefs.isInIsrael, prefs.showModernIsraeliHolidays)
        val nextZman = nextZman(prefs, position, today, device.instant)
        val nextMidnight = today.plusDays(1).atStartOfDay(device.zone).toInstant()
        val staleAt = nextZman?.at?.toInstant()?.takeIf { it < nextMidnight } ?: nextMidnight
        return TodayWidgetSnapshot(
            today = today,
            hebrewDay = dayInfo.hebrewDay,
            holiday = dayInfo.holidays.firstOrNull(),
            locationName = position.name,
            nextZman = nextZman,
            shabbat = shabbatTimes(prefs, position, today),
            holidayTheme = activeHolidayTheme(prefs, today),
            refreshAt = device.toSystemInstant(staleAt),
        )
    }

    /**
     * The first selected zman after [now], from [today]'s list and then tomorrow's. A night
     * zman before noon (halachic midnight after civil midnight) belongs to the next civil day.
     */
    internal fun nextZman(prefs: UserPreferences, location: Location, today: LocalDate, now: Instant): NextZman? {
        val zone = ZoneId.of(location.timezoneId)
        return listOf(today, today.plusDays(1)).firstNotNullOfOrNull { date ->
            zmanimRepository.getZmanim(date, location, prefs.candleLightingOffset, prefs.isInIsrael)
                .filter { it.name in prefs.selectedZmanim }
                .mapNotNull { zman ->
                    val time = zman.time ?: return@mapNotNull null
                    val day = if (zman.category == ZmanCategory.NIGHT && time < LocalTime.NOON) date.plusDays(1) else date
                    NextZman(zman.name, zman.hebrewName, day.atTime(time).atZone(zone))
                }
                .filter { it.at.toInstant() > now }
                .minByOrNull { it.at }
        }
    }

    /**
     * The run of Shabbat / Yom Tov days shown on [today]: the first whose havdalah falls on
     * [today] or later, so it stays through the civil day of havdalah and then rolls on.
     */
    internal fun shabbatTimes(prefs: UserPreferences, location: Location, today: LocalDate): ShabbatTimes? {
        val zone = ZoneId.of(location.timezoneId)
        val inIsrael = prefs.isInIsrael
        // A run starts on an evening with candles that isn't itself a holy day's evening
        // continuing an earlier run. Runs are at most a few days, so a week back suffices.
        var date = today.minusDays(SEARCH_BACK_DAYS)
        val last = today.plusDays(SEARCH_AHEAD_DAYS)
        while (date <= last) {
            if (zmanimRepository.hasCandles(date, inIsrael) &&
                !zmanimRepository.hasCandles(date.minusDays(1), inIsrael)
            ) {
                val end = zmanimRepository.getObservanceEnd(date, location, inIsrael)
                if (end != null && end.date >= today) {
                    val candles = zmanimRepository.getCandleLightingInstant(
                        date,
                        location,
                        prefs.candleLightingOffset,
                        inIsrael,
                    )
                    return ShabbatTimes(
                        candleLightingDate = date,
                        candleLighting = candles?.atZone(zone),
                        havdalahDate = end.date,
                        havdalah = end.time?.let { end.date.atTime(it).atZone(zone) },
                    )
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    private companion object {
        const val SEARCH_BACK_DAYS = 7L
        const val SEARCH_AHEAD_DAYS = 14L
    }
}

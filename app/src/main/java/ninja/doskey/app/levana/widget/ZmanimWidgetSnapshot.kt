package ninja.doskey.app.levana.widget

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import ninja.doskey.app.levana.ui.theme.activeHolidayTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** Where a zman stands against now: passed (dimmed), the next one (highlighted), or later. */
enum class ZmanState { PAST, NEXT, UPCOMING }

/** One row of the Zmanim widget; [at] is null when the time can't be computed (shown as —). */
data class ZmanimWidgetRow(
    val name: String,
    val hebrewName: String,
    val at: ZonedDateTime?,
    val state: ZmanState,
)

/** Everything the Zmanim widget shows, computed once per update. */
data class ZmanimWidgetSnapshot(
    val today: LocalDate,
    val hebrewDay: HebrewDay,
    val locationName: String,
    val rows: List<ZmanimWidgetRow>,
    val holidayTheme: HolidayTheme?,
    /** When the snapshot goes stale, on the real clock: the next zman passing, or midnight. */
    val refreshAt: Instant,
)

class ZmanimWidgetSnapshots(
    private val zmanimRepository: ZmanimRepository,
    private val calendarRepository: CalendarRepository,
) {

    /**
     * Today's selected zmanim for the [device]: its date and moment (developer override
     * included) at its position (owner decision, TASK-5), never the selected city's. The next
     * zman is the one the Today widget shows; it is highlighted when it is in today's list.
     */
    fun build(prefs: UserPreferences, device: DeviceNow): ZmanimWidgetSnapshot {
        val today = device.today
        val position = device.position
        val now = device.instant
        val next = zmanimRepository.nextSelectedZman(prefs, position, today, now)
        val rows = zmanimRepository.selectedZmanimOn(prefs, position, today).map { zman ->
            val state = when {
                zman.at == null -> ZmanState.UPCOMING
                next != null && zman.name == next.name && zman.at == next.at -> ZmanState.NEXT
                zman.at.toInstant() <= now -> ZmanState.PAST
                else -> ZmanState.UPCOMING
            }
            ZmanimWidgetRow(zman.name, zman.hebrewName, zman.at, state)
        }
        return ZmanimWidgetSnapshot(
            today = today,
            hebrewDay = calendarRepository.getDayInfo(today, prefs.isInIsrael, prefs.showModernIsraeliHolidays).hebrewDay,
            locationName = position.name,
            rows = rows,
            holidayTheme = activeHolidayTheme(prefs, today),
            refreshAt = widgetRefreshAt(device, next),
        )
    }
}

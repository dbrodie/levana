package ninja.doskey.app.levana.device

import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList

/** TASK-5 case 13: the device date is re-read at midnight and on clock/zone changes. */
class DeviceTodayUpdatesTest {

    private val nyZone = ZoneId.of("America/New_York")
    private val day = LocalDate.of(2026, 10, 29)
    private var clock: Clock = fixedClock(day, LocalTime.of(23, 59, 30), nyZone)
    private val prefs = MutableStateFlow(UserPreferences())
    private val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val device = DeviceContext(prefs, signals) { clock }

    private fun collect(block: suspend (List<LocalDate>) -> Unit) = runBlocking {
        val seen = CopyOnWriteArrayList<LocalDate>()
        val job = CoroutineScope(Dispatchers.Default).launch { device.todayUpdates.collect { seen += it } }
        try {
            await { seen.isNotEmpty() }
            block(seen)
        } finally {
            job.cancel()
        }
    }

    private suspend fun await(condition: () -> Boolean) = withTimeout(5.seconds) {
        while (!condition()) kotlinx.coroutines.delay(10)
    }

    @Test
    fun aDateChangeSignalAfterMidnightMovesToday() = collect { seen ->
        assertEquals(listOf(day), seen)
        clock = fixedClock(day.plusDays(1), LocalTime.of(0, 0, 5), nyZone)
        signals.emit(Unit)
        await { seen.size == 2 }
        assertEquals(listOf(day, day.plusDays(1)), seen)
    }

    @Test
    fun aTimeZoneChangeCanMoveTheDate() = collect { seen ->
        clock = clock.withZone(ZoneId.of("Asia/Tokyo"))
        signals.emit(Unit)
        await { seen.size == 2 }
        assertEquals(day.plusDays(1), seen.last())
    }

    @Test
    fun theOverrideMovesTodayAtOnceAndOffAgain() = collect { seen ->
        prefs.value = prefs.value.copy(devDayShift = 7)
        await { seen.size == 2 }
        prefs.value = prefs.value.copy(devDayShift = 0)
        await { seen.size == 3 }
        assertEquals(listOf(day, day.plusDays(7), day), seen)
    }

    @Test
    fun unchangedDatesAreNotRepeated() = collect { seen ->
        signals.emit(Unit)
        prefs.value = prefs.value.copy(selectedZmanim = setOf("Sunrise"))
        kotlinx.coroutines.delay(200)
        assertEquals(listOf(day), seen)
    }

    @Test
    fun theMidnightTimerWaitsUntilJustAfterTheNextDeviceMidnight() {
        val now = DeviceContext(prefs) { clock }.now(prefs.value)
        assertEquals(31.seconds, DeviceContext.untilNextDay(now))
        val morning = DeviceContext(prefs) { fixedClock(day, LocalTime.of(1, 0), nyZone) }.now(prefs.value)
        assertEquals(23.hours + 1.seconds, DeviceContext.untilNextDay(morning))
        // Sat 31 Oct 23:00 to Sun 1 Nov midnight; the DST change happens after that midnight
        val beforeDst = DeviceContext(prefs) { fixedClock(LocalDate.of(2026, 10, 31), LocalTime.of(23, 0), nyZone) }
            .now(prefs.value)
        assertEquals(60.minutes + 1.seconds, DeviceContext.untilNextDay(beforeDst))
        // Sun 1 Nov 01:30 (first, EDT) to Mon midnight is 23.5 real hours: 01:30 repeats
        val dstDay = DeviceContext(prefs) {
            Clock.fixed(java.time.ZonedDateTime.of(LocalDate.of(2026, 11, 1), LocalTime.of(1, 30), nyZone).toInstant(), nyZone)
        }.now(prefs.value)
        assertEquals(23.hours + 30.minutes + 1.seconds, DeviceContext.untilNextDay(dstDay))
    }
}

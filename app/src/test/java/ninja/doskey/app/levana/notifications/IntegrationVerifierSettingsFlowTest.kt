package ninja.doskey.app.levana.notifications

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.di.allModules
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import ninja.doskey.app.levana.ui.calendar.CalendarViewModel
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionViewModel
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
import ninja.doskey.app.levana.ui.events.EventsViewModel
import ninja.doskey.app.levana.ui.zmanim.ZmanimViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Collections

/** Independent checks (verify-bugfix-integration) of the settings observers and the Koin graph. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class IntegrationVerifierSettingsFlowTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()

    @Test
    fun `each preference change reaches only the affected rebuild or category refresh`() {
        val prefs = PreferencesRepository(app, CityRepository(app))
        val log = Collections.synchronizedList(mutableListOf<String>())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch { rebuildOnAlarmSettingsChange(prefs.preferences) { log += "rebuild" } }
        scope.launch {
            refreshOnDailySettingsChange(
                prefs.preferences,
                refresh = { log += "refresh:" + it.map { c -> c.name }.sorted() },
                disabled = { log += "off:" + it.map { c -> c.name }.sorted() }
            )
        }
        fun step(expected: List<String>, change: suspend () -> Unit) {
            Thread.sleep(300)
            log.clear()
            runBlocking { change() }
            val deadline = System.currentTimeMillis() + 3000
            while (log.size < expected.size && System.currentTimeMillis() < deadline) Thread.sleep(20)
            Thread.sleep(300)
            assertEquals(expected.sorted(), log.toList().sorted())
        }
        try {
            step(emptyList()) { prefs.saveDynamicHolidayTheme(false) }
            step(emptyList()) { prefs.saveSelectedZmanim(setOf("Sunrise")) }
            step(listOf("refresh:[HOLIDAYS]")) { prefs.saveNotifyHolidays(true) }
            step(listOf("rebuild")) { prefs.saveCandleLightingOffset(40.0) }
            step(listOf("rebuild")) { prefs.saveNotifyOmer(true) }
            step(listOf("rebuild")) { prefs.saveNotifyOmerMorningTime(500) }
            step(listOf("refresh:[HOLIDAYS]")) { prefs.saveHolidayNotifyDaysBefore(3) }
            step(listOf("refresh:[PERSONAL_EVENTS]")) { prefs.saveNotifyPersonalEvents(true) }
            // Default location is Jerusalem; a Diaspora override changes alarms and holiday dates only
            step(listOf("rebuild", "refresh:[HOLIDAYS]")) { prefs.saveIsInIsrael(false) }
            step(listOf("off:[HOLIDAYS]")) { prefs.saveNotifyHolidays(false) }
            step(listOf("rebuild")) { prefs.saveNotifyCandleLighting(true) }
            step(listOf("rebuild")) { prefs.saveNotifyCandleLighting(false) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `real Koin graph resolves every view model and notification dependency`() {
        val koin = startKoin { androidContext(app); modules(allModules) }.koin
        try {
            assertNotNull(koin.get<NotificationAlarmRebuilder>())
            assertNotNull(koin.get<NotificationDeliveryLog>())
            assertNotNull(koin.get<NotificationAlarmScheduler>())
            assertNotNull(koin.get<EventDataChanges>())
            assertNotNull(koin.get<EventFileRepository>())
            assertNotNull(koin.get<CalendarViewModel>())
            assertNotNull(koin.get<DayDetailViewModel>())
            assertNotNull(koin.get<EventsViewModel>())
            assertNotNull(koin.get<ContactBirthdayViewModel>())
            assertNotNull(koin.get<CalendarSelectionViewModel>())
            assertNotNull(koin.get<ZmanimViewModel>())
            // Application.onCreate path: starts the observers via injected dependencies
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            NotificationRescheduling.start(scope)
            Thread.sleep(200)
            scope.cancel()
        } finally {
            stopKoin()
        }
    }
}

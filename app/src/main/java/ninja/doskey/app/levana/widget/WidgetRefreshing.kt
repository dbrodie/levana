package ninja.doskey.app.levana.widget

import android.content.Context
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.activeLocation
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Redraws the widgets when a setting they show changes. */
object WidgetRefreshing : KoinComponent {
    private val preferencesRepository: PreferencesRepository by inject()

    fun start(context: Context, scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        scope.launch {
            widgetSettingsChanges(preferencesRepository.preferences).collect {
                WidgetUpdater.requestUpdate(context)
            }
        }
    }
}

/** The settings the widgets show, compared to skip unrelated preference changes. */
internal data class WidgetSettings(
    val location: Location,
    val isInIsrael: Boolean,
    val showModernIsraeliHolidays: Boolean,
    val candleLightingOffset: Double,
    val selectedZmanim: Set<String>,
    val dynamicHolidayTheme: Boolean,
    val devForceHolidayTheme: String?,
    val devDateOverride: LocalDate?,
)

internal fun UserPreferences.widgetSettings() = WidgetSettings(
    location = activeLocation,
    isInIsrael = isInIsrael,
    showModernIsraeliHolidays = showModernIsraeliHolidays,
    candleLightingOffset = candleLightingOffset,
    selectedZmanim = selectedZmanim,
    dynamicHolidayTheme = dynamicHolidayTheme,
    devForceHolidayTheme = devForceHolidayTheme,
    devDateOverride = devDateOverride,
)

/** Emits on each change to a widget setting after the first value. */
internal fun widgetSettingsChanges(preferences: Flow<UserPreferences>): Flow<WidgetSettings> =
    preferences.map { it.widgetSettings() }.distinctUntilChanged().drop(1)

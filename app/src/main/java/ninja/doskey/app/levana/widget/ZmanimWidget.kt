package ninja.doskey.app.levana.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.zmanDisplayName
import ninja.doskey.app.levana.ui.theme.levanaColorScheme
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Home-screen "Zmanim" widget: today's selected zmanim (the day details' selection) at the
 * device position, past ones dimmed and the next one highlighted.
 */
class ZmanimWidget : GlanceAppWidget(), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()
    private val snapshots: ZmanimWidgetSnapshots by inject()
    private val deviceContext: DeviceContext by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialPrefs = preferencesRepository.preferences.first()
        provideContent {
            // As in TodayWidget: recomputed in the live session on each settings change or refresh
            val prefs by preferencesRepository.preferences.collectAsState(initialPrefs)
            val refreshes by WidgetUpdater.refreshes.collectAsState()
            val snapshot = remember(prefs, refreshes) { snapshot(prefs) }
            LaunchedEffect(snapshot.refreshAt) {
                WidgetUpdater.scheduleRefresh(context, snapshot.refreshAt, ZmanimWidgetReceiver::class.java)
            }
            val colors = remember(snapshot.holidayTheme) {
                ColorProviders(
                    light = levanaColorScheme(darkTheme = false, holidayTheme = snapshot.holidayTheme),
                    dark = levanaColorScheme(darkTheme = true, holidayTheme = snapshot.holidayTheme),
                )
            }
            GlanceTheme(colors = colors) {
                ZmanimContent(snapshot)
            }
        }
    }

    // The device's date and moment, developer override included
    private fun snapshot(prefs: UserPreferences): ZmanimWidgetSnapshot =
        snapshots.build(prefs, deviceContext.now(prefs))
}

@Composable
private fun ZmanimContent(snapshot: ZmanimWidgetSnapshot) {
    val context = LocalContext.current
    val hebrewUi = context.widgetLocale().isHebrewUi()
    val mirror = context.widgetMirrored()
    val openZmanim = openZmanim(context, snapshot.today)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Header(snapshot, hebrewUi, mirror, openCalendar(context, snapshot.today))
        Spacer(GlanceModifier.height(6.dp))
        if (snapshot.rows.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_zmanim_empty),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp, textAlign = textAlign(mirror)),
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).clickable(openZmanim),
            )
        } else {
            val timeFormatter = timeFormatter(context)
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(snapshot.rows) { row ->
                    ZmanRow(
                        name = zmanDisplayName(row.name, row.hebrewName, hebrewUi).primary,
                        time = row.at?.format(timeFormatter) ?: NO_TIME,
                        state = row.state,
                        mirror = mirror,
                        action = openZmanim,
                    )
                }
            }
        }
    }
}

/** The Hebrew day and month and the device position; opens the calendar on today. */
@Composable
private fun Header(snapshot: ZmanimWidgetSnapshot, hebrewUi: Boolean, mirror: Boolean, action: Action) {
    val day = snapshot.hebrewDay
    val hebrewDate = if (hebrewUi) "${day.hebrewDayOfMonthFormatted} ${day.month.hebrewName}" else "${day.day} ${day.month.englishName}"
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp).clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val date = @Composable {
            Text(
                text = hebrewDate,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
        }
        // When space runs out, the location is cut short rather than the date
        val location = @Composable {
            Text(
                text = if (mirror) "${snapshot.locationName} · " else " · ${snapshot.locationName}",
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, textAlign = textAlign(mirror)),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        if (mirror) {
            location()
            date()
        } else {
            date()
            location()
        }
    }
}

@Composable
private fun ZmanRow(name: String, time: String, state: ZmanState, mirror: Boolean, action: Action) {
    val color = when (state) {
        ZmanState.PAST -> GlanceTheme.colors.onSurfaceVariant
        ZmanState.NEXT -> GlanceTheme.colors.onPrimaryContainer
        ZmanState.UPCOMING -> GlanceTheme.colors.onSurface
    }
    val weight = if (state == ZmanState.NEXT) FontWeight.Bold else FontWeight.Normal
    var modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
    if (state == ZmanState.NEXT) {
        modifier = modifier.cornerRadius(12.dp).background(GlanceTheme.colors.primaryContainer)
    }
    Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Row(modifier = modifier.clickable(action), verticalAlignment = Alignment.CenterVertically) {
            val label = @Composable {
                Text(
                    text = name,
                    maxLines = 1,
                    style = TextStyle(color = color, fontSize = 14.sp, fontWeight = weight, textAlign = textAlign(mirror)),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
            val value = @Composable {
                Text(
                    text = time,
                    maxLines = 1,
                    style = TextStyle(color = color, fontSize = 14.sp, fontWeight = weight),
                )
            }
            if (mirror) {
                value()
                Spacer(GlanceModifier.width(8.dp))
                label()
            } else {
                label()
                Spacer(GlanceModifier.width(8.dp))
                value()
            }
        }
    }
}

class ZmanimWidgetReceiver : RefreshingWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZmanimWidget()
}

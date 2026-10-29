package ninja.doskey.app.levana.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.ui.calendar.CalendarHeaderFormatter
import ninja.doskey.app.levana.ui.calendar.holidayCategoryColor
import ninja.doskey.app.levana.ui.theme.levanaColorScheme
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Home-screen month widget: the device's current Hebrew month as the calendar screen's Hebrew
 * grid shows it, with Gregorian day numbers, holiday markers and today highlighted.
 */
class MonthWidget : GlanceAppWidget(), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()
    private val snapshots: MonthWidgetSnapshots by inject()
    private val deviceContext: DeviceContext by inject()

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, FULL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialPrefs = preferencesRepository.preferences.first()
        provideContent {
            // Recomputed on each settings change or refresh request, as in the Today widget
            val prefs by preferencesRepository.preferences.collectAsState(initialPrefs)
            val refreshes by WidgetUpdater.refreshes.collectAsState()
            val snapshot = remember(prefs, refreshes) { snapshot(prefs) }
            LaunchedEffect(snapshot.refreshAt) {
                WidgetUpdater.scheduleRefresh(context, snapshot.refreshAt, MonthWidgetReceiver::class.java)
            }
            val light = remember(snapshot.holidayTheme) { levanaColorScheme(darkTheme = false, holidayTheme = snapshot.holidayTheme) }
            val dark = remember(snapshot.holidayTheme) { levanaColorScheme(darkTheme = true, holidayTheme = snapshot.holidayTheme) }
            val markers = remember(light, dark) {
                HolidayCategory.entries.associateWith {
                    ColorProvider(day = holidayCategoryColor(it, light), night = holidayCategoryColor(it, dark))
                }
            }
            GlanceTheme(colors = remember(light, dark) { ColorProviders(light = light, dark = dark) }) {
                MonthContent(snapshot, markers)
            }
        }
    }

    // The device's date, developer override included
    private fun snapshot(prefs: UserPreferences): MonthWidgetSnapshot =
        snapshots.build(prefs, deviceContext.now(prefs))

    companion object {
        val COMPACT = DpSize(180.dp, 180.dp)
        val FULL = DpSize(250.dp, 250.dp)
    }
}

class MonthWidgetReceiver : RefreshingWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

private val HEBREW_LOCALE = Locale.forLanguageTag("he")

// Sunday first, as in the calendar grid
private val DAYS_OF_WEEK = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)

@Composable
private fun MonthContent(snapshot: MonthWidgetSnapshot, markers: Map<HolidayCategory, ColorProvider>) {
    val context = LocalContext.current
    val locale = context.widgetLocale()
    val mirror = context.widgetMirrored()
    val full = LocalSize.current.height >= MonthWidget.FULL.height &&
        LocalSize.current.width >= MonthWidget.FULL.width
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    fun <T> inRowOrder(items: List<T>) = if (mirror) items.reversed() else items

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Header(snapshot, locale, full, mirror)
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
            inRowOrder(DAYS_OF_WEEK).forEach { dow ->
                Text(
                    // The calendar's Hebrew grid names the days in Hebrew in either language
                    text = dow.getDisplayName(java.time.format.TextStyle.NARROW, HEBREW_LOCALE),
                    maxLines = 1,
                    style = TextStyle(
                        color = if (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) {
                            GlanceTheme.colors.primary
                        } else {
                            GlanceTheme.colors.onSurfaceVariant
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        snapshot.weeks.forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                inRowOrder(week).forEach { day ->
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                isToday = day.gregorianDate == snapshot.today,
                                full = full,
                                marker = day.holidayCategory?.let { markers[it] },
                                description = dayDescription(
                                    day,
                                    locale,
                                    dateFormatter,
                                    today = if (day.gregorianDate == snapshot.today) context.getString(R.string.calendar_today) else null,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(snapshot: MonthWidgetSnapshot, locale: Locale, full: Boolean, mirror: Boolean) {
    val context = LocalContext.current
    val align = if (mirror) TextAlign.End else TextAlign.Start
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(openCalendar(context, snapshot.today)),
    ) {
        Text(
            text = CalendarHeaderFormatter.hebrewMonthTitle(snapshot.month, locale),
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = if (full) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = align,
            ),
            modifier = GlanceModifier.fillMaxWidth(),
        )
        if (full) {
            Text(
                text = CalendarHeaderFormatter.gregorianRange(snapshot.days, locale),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = align),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DayCell(day: HebrewDay, isToday: Boolean, full: Boolean, marker: ColorProvider?, description: String) {
    val context = LocalContext.current
    val cell = GlanceModifier
        .fillMaxSize()
        .padding(1.dp)
        .let { if (isToday) it.cornerRadius(8.dp).background(GlanceTheme.colors.primaryContainer) else it }
        .clickable(openCalendar(context, day.gregorianDate))
        .semantics { contentDescription = description }
    Column(
        modifier = cell,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = day.hebrewDayOfMonthFormatted,
            maxLines = 1,
            style = TextStyle(
                color = if (isToday) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurface,
                fontSize = if (full) 15.sp else 13.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
        if (full) {
            Text(
                text = day.gregorianDate.dayOfMonth.toString(),
                maxLines = 1,
                style = TextStyle(
                    color = if (isToday) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        // A holiday marker, colored by category as in the calendar; the space is kept
        // without one so the numbers line up across the row
        Spacer(GlanceModifier.height(2.dp))
        Box(
            modifier = GlanceModifier
                .width(14.dp)
                .height(3.dp)
                .let { if (marker != null) it.cornerRadius(2.dp).background(marker) else it },
        ) {}
    }
}

/** What a screen reader says for a cell; [today] marks the highlighted day. */
private fun dayDescription(day: HebrewDay, locale: Locale, dateFormatter: DateTimeFormatter, today: String?): String {
    val holiday = if (locale.isHebrewUi()) day.holidayLabelHebrew else day.holidayLabel
    return listOfNotNull(today, day.hebrewFormatted, day.gregorianDate.format(dateFormatter), holiday).joinToString(", ")
}

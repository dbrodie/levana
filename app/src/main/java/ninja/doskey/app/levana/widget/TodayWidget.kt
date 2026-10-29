package ninja.doskey.app.levana.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
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
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.zmanDisplayName
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.ui.theme.levanaColorScheme
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Home-screen "Today" widget: the Hebrew date first, the Gregorian date and location, and
 * at larger sizes the next selected zman and the coming Shabbat / Yom Tov times.
 */
class TodayWidget : GlanceAppWidget(), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()
    private val snapshots: TodayWidgetSnapshots by inject()
    private val deviceContext: DeviceContext by inject()

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, FULL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialPrefs = preferencesRepository.preferences.first()
        provideContent {
            // Glance keeps a session alive between updates and only recomposes it, so the
            // snapshot is computed here, again on each settings change or refresh request
            val prefs by preferencesRepository.preferences.collectAsState(initialPrefs)
            val refreshes by WidgetUpdater.refreshes.collectAsState()
            val snapshot = remember(prefs, refreshes) { snapshot(prefs) }
            LaunchedEffect(snapshot.refreshAt) {
                WidgetUpdater.scheduleRefresh(context, snapshot.refreshAt, TodayWidgetReceiver::class.java)
            }
            val colors = remember(snapshot.holidayTheme) {
                ColorProviders(
                    light = levanaColorScheme(darkTheme = false, holidayTheme = snapshot.holidayTheme),
                    dark = levanaColorScheme(darkTheme = true, holidayTheme = snapshot.holidayTheme),
                )
            }
            GlanceTheme(colors = colors) {
                TodayContent(snapshot)
            }
        }
    }

    // The device's date and moment, developer override included
    private fun snapshot(prefs: UserPreferences): TodayWidgetSnapshot =
        snapshots.build(prefs, deviceContext.now(prefs))

    companion object {
        val COMPACT = DpSize(110.dp, 40.dp)
        val FULL = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun TodayContent(snapshot: TodayWidgetSnapshot) {
    val context = LocalContext.current
    val locale = context.widgetLocale()
    val hebrewUi = locale.isHebrewUi()
    val mirror = context.widgetMirrored()
    val full = LocalSize.current.height >= TodayWidget.FULL.height &&
        LocalSize.current.width >= TodayWidget.FULL.width

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateBlock(snapshot, hebrewUi, locale, full, mirror, openCalendar(context, snapshot.today))
        if (full) {
            val openZmanim = openZmanim(context, snapshot.today)
            val timeFormatter = timeFormatter(context)
            val dayFormatter = DateTimeFormatter.ofPattern("EEE", locale)
            val next = snapshot.nextZman
            TimeRow(
                label = next?.let {
                    context.getString(
                        R.string.widget_next_zman,
                        zmanDisplayName(it.name, it.hebrewName, hebrewUi).primary,
                    )
                } ?: context.getString(R.string.widget_next_zman_none),
                value = next?.at?.let { relativeTime(it, snapshot.today, timeFormatter, dayFormatter) } ?: NO_TIME,
                action = openZmanim,
                mirror = mirror,
            )
            val shabbat = snapshot.shabbat
            if (shabbat != null) {
                TimeRow(
                    label = context.getString(R.string.candle_lighting),
                    value = timeOnDay(shabbat.candleLightingDate, shabbat.candleLighting, timeFormatter, dayFormatter),
                    secondaryLabel = context.getString(R.string.havdalah),
                    secondaryValue = timeOnDay(shabbat.havdalahDate, shabbat.havdalah, timeFormatter, dayFormatter),
                    action = openZmanim,
                    mirror = mirror,
                )
            }
        }
    }
}

@Composable
private fun DateBlock(
    snapshot: TodayWidgetSnapshot,
    hebrewUi: Boolean,
    locale: Locale,
    full: Boolean,
    mirror: Boolean,
    action: Action,
) {
    val day = snapshot.hebrewDay
    // The compact size has room for the day and month only; the year would be cut off
    val hebrewDate = when {
        full && hebrewUi -> day.hebrewFormatted
        full -> day.transliterated
        hebrewUi -> "${day.hebrewDayOfMonthFormatted} ${day.month.hebrewName}"
        else -> "${day.day} ${day.month.englishName}"
    }
    val gregorian = snapshot.today.format(
        if (full) DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
        else DateTimeFormatter.ofPattern("EEE d MMM", locale)
    )
    val holiday = snapshot.holiday?.let { if (hebrewUi) it.hebrewName else it.name }
    Column(modifier = GlanceModifier.fillMaxWidth().clickable(action)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val date = @Composable {
                Text(
                    text = hebrewDate,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = if (full) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlign(mirror),
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
            val holidayLabel = @Composable {
                if (full && holiday != null) {
                    Text(
                        text = holiday,
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
            if (mirror) {
                holidayLabel()
                date()
            } else {
                date()
                holidayLabel()
            }
        }
        // When space runs out, the location is cut short rather than the date
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            val secondaryStyle = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
            val date = @Composable { Text(text = gregorian, maxLines = 1, style = secondaryStyle) }
            val location = @Composable {
                Text(
                    text = " · ${snapshot.locationName}",
                    maxLines = 1,
                    style = secondaryStyle.copy(textAlign = textAlign(mirror)),
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
}

@Composable
private fun TimeRow(
    label: String,
    value: String,
    action: Action,
    mirror: Boolean,
    secondaryLabel: String? = null,
    secondaryValue: String? = null,
) {
    Spacer(GlanceModifier.height(8.dp))
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val items = listOfNotNull(
            label to value,
            secondaryLabel?.let { l -> secondaryValue?.let { l to it } },
        ).let { if (mirror) it.reversed() else it }
        items.forEachIndexed { index, (itemLabel, itemValue) ->
            if (index > 0) Spacer(GlanceModifier.width(12.dp))
            LabeledTime(itemLabel, itemValue, mirror, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun LabeledTime(label: String, value: String, mirror: Boolean, modifier: GlanceModifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = textAlign(mirror)),
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Text(
            text = value,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign(mirror),
            ),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

private const val NO_TIME = "—"

private fun textAlign(mirror: Boolean) = if (mirror) TextAlign.End else TextAlign.Start

private fun timeFormatter(context: Context): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a")

/** A time today as just the time; on another day prefixed with the weekday. */
private fun relativeTime(
    at: ZonedDateTime,
    today: LocalDate,
    timeFormatter: DateTimeFormatter,
    dayFormatter: DateTimeFormatter,
): String = if (at.toLocalDate() == today) at.format(timeFormatter) else timeOnDay(at.toLocalDate(), at, timeFormatter, dayFormatter)

private fun timeOnDay(
    date: LocalDate,
    at: ZonedDateTime?,
    timeFormatter: DateTimeFormatter,
    dayFormatter: DateTimeFormatter,
): String = "${date.format(dayFormatter)} ${at?.format(timeFormatter) ?: NO_TIME}"

private fun openZmanim(context: Context, today: LocalDate): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
            .putExtra(MainActivity.EXTRA_OPEN_ZMANIM, true)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )

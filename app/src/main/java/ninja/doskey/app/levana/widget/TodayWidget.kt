package ninja.doskey.app.levana.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.domain.model.zmanDisplayName
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.ui.theme.levanaColorScheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, FULL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = preferencesRepository.preferences.first()
        val override = prefs.devDateOverride
        val today = override ?: LocalDate.now()
        // With a Developer Settings date, "now" is that date at the current time of day
        val now = override?.atTime(LocalTime.now())?.atZone(java.time.ZoneId.systemDefault())?.toInstant()
            ?: Instant.now()
        val snapshot = snapshots.build(prefs, today, now)
        WidgetUpdater.scheduleRefresh(context, snapshot.refreshAt)

        val colors = ColorProviders(
            light = levanaColorScheme(darkTheme = false, holidayTheme = snapshot.holidayTheme),
            dark = levanaColorScheme(darkTheme = true, holidayTheme = snapshot.holidayTheme),
        )
        provideContent {
            GlanceTheme(colors = colors) {
                TodayContent(snapshot)
            }
        }
    }

    companion object {
        val COMPACT = DpSize(110.dp, 40.dp)
        val FULL = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun TodayContent(snapshot: TodayWidgetSnapshot) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val hebrewUi = locale.language in setOf("he", "iw")
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
        DateBlock(snapshot, hebrewUi, locale, full, openCalendar(context, snapshot.today))
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
            )
            val shabbat = snapshot.shabbat
            if (shabbat != null) {
                TimeRow(
                    label = context.getString(R.string.candle_lighting),
                    value = timeOnDay(shabbat.candleLightingDate, shabbat.candleLighting, timeFormatter, dayFormatter),
                    secondaryLabel = context.getString(R.string.havdalah),
                    secondaryValue = timeOnDay(shabbat.havdalahDate, shabbat.havdalah, timeFormatter, dayFormatter),
                    action = openZmanim,
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
    action: Action,
) {
    val hebrewDate = if (hebrewUi) snapshot.hebrewDay.hebrewFormatted else snapshot.hebrewDay.transliterated
    val gregorian = snapshot.today.format(
        if (full) DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
        else DateTimeFormatter.ofPattern("EEE d MMM", locale)
    )
    val holiday = snapshot.holiday?.let { if (hebrewUi) it.hebrewName else it.name }
    Column(modifier = GlanceModifier.fillMaxWidth().clickable(action)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = hebrewDate,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = if (full) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (full && holiday != null) {
                Text(
                    text = holiday,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
        Text(
            text = "$gregorian · ${snapshot.locationName}",
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
        )
    }
}

@Composable
private fun ColumnScope.TimeRow(
    label: String,
    value: String,
    action: Action,
    secondaryLabel: String? = null,
    secondaryValue: String? = null,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().defaultWeight().clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabeledTime(label, value, GlanceModifier.defaultWeight())
        if (secondaryLabel != null && secondaryValue != null) {
            Spacer(GlanceModifier.width(12.dp))
            LabeledTime(secondaryLabel, secondaryValue, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun LabeledTime(label: String, value: String, modifier: GlanceModifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        Text(
            text = value,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        )
    }
}

private const val NO_TIME = "—"

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

private fun openCalendar(context: Context, today: LocalDate): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )

private fun openZmanim(context: Context, today: LocalDate): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
            .putExtra(MainActivity.EXTRA_OPEN_ZMANIM, true)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )

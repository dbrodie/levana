package ninja.doskey.app.levana.ui.walkthrough

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.domain.agenda.AgendaDay
import ninja.doskey.app.levana.domain.agenda.AgendaItem
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import ninja.doskey.app.levana.ui.agenda.DayRow
import java.time.LocalTime
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.theme.DarkColorScheme
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import ninja.doskey.app.levana.ui.theme.LightColorScheme

// Live mini-UIs for the walkthrough's illustration cards. They use the app's theme and fixed
// sample data around Friday 30 October 2026 (19 Cheshvan 5787, erev Shabbat Vayera), so they
// look the same on every device and day.

private val SAMPLE_DAY: LocalDate = LocalDate.of(2026, 10, 30)
private const val SAMPLE_CANDLES = "16:23"
private const val SAMPLE_SUNRISE = "6:12"
private const val SAMPLE_SUNSET = "16:41"
private const val SAMPLE_HAVDALAH = "17:34"

private val hebrewNumbers = HebrewDateFormatter().apply { isHebrewFormat = true }

@Composable
private fun locale(): Locale = LocalConfiguration.current.locales[0]

private fun Locale.isHebrew() = language == "iw" || language == "he"

private fun jewishDate(date: LocalDate) = JewishCalendar(date)

/** "19 Cheshvan 5787", or "י״ט חשוון תשפ״ז" in Hebrew. */
private fun hebrewDate(date: LocalDate, hebrew: Boolean): String =
    HebrewDateFormatter().apply { isHebrewFormat = hebrew }.format(jewishDate(date))

/** "כ״א כסלו": a Hebrew date the way the event cards show it, whatever the app language. */
private fun hebrewDayMonth(day: Int, month: Int): String {
    // A common year, so Adar is plain Adar
    val date = JewishDate(5786, month, day)
    return "${hebrewNumbers.formatHebrewNumber(day)} ${hebrewNumbers.formatMonth(date)}"
}

/** The parasha read on [shabbat], in the app language. */
private fun parasha(shabbat: LocalDate, hebrew: Boolean): String =
    HebrewDateFormatter().apply { isHebrewFormat = hebrew }.formatParsha(JewishCalendar(shabbat))

private fun dayLine(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))

// ── 1 Welcome: a two-week Hebrew month strip and today's times ────────────────────────────

@Composable
internal fun WelcomeIllustration() {
    val locale = locale()
    val first = LocalDate.of(2026, 10, 25)
    val chips = mapOf(
        SAMPLE_DAY to stringResource(R.string.walkthrough_sample_candles),
        SAMPLE_DAY.plusDays(1) to parasha(SAMPLE_DAY.plusDays(1), locale.isHebrew()),
        LocalDate.of(2026, 11, 3) to stringResource(R.string.walkthrough_sample_yahrzeit),
        LocalDate.of(2026, 11, 7) to parasha(LocalDate.of(2026, 11, 7), locale.isHebrew()),
    )
    Decorative {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                hebrewNumbers.formatMonth(jewishDate(SAMPLE_DAY)) + " " +
                    hebrewNumbers.formatHebrewNumber(jewishDate(SAMPLE_DAY).jewishYear),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                first.format(DateTimeFormatter.ofPattern("MMM", locale)) + " – " +
                    first.plusDays(13).format(DateTimeFormatter.ofPattern("MMM yyyy", locale)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        for (week in 0..1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (weekday in 0..6) {
                    val date = first.plusDays((week * 7 + weekday).toLong())
                    DayCell(date, today = date == SAMPLE_DAY, chip = chips[date])
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The day panel's own labels
            TimeTile(stringResource(R.string.sheet_netz), SAMPLE_SUNRISE, highlighted = false)
            TimeTile(stringResource(R.string.sheet_shkia), SAMPLE_SUNSET, highlighted = false)
            TimeTile(stringResource(R.string.sheet_candles), SAMPLE_CANDLES, highlighted = true)
        }
    }
}

@Composable
private fun RowScope.DayCell(date: LocalDate, today: Boolean, chip: String?) {
    val colors = MaterialTheme.colorScheme
    val content = if (today) colors.onPrimary else colors.onSurface
    Column(
        Modifier
            .weight(1f)
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (today) colors.primary else colors.surfaceContainerLowest)
            .padding(horizontal = 3.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                hebrewNumbers.formatHebrewNumber(jewishDate(date).jewishDayOfMonth),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                date.dayOfMonth.toString(),
                fontSize = 9.sp,
                color = if (today) content else colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        if (chip != null) {
            Text(
                chip,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (today) colors.primary else colors.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (today) colors.onPrimary else colors.secondaryContainer)
                    .padding(horizontal = 2.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun RowScope.TimeTile(label: String, time: String, highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) colors.primary else colors.surfaceContainerLowest)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) colors.onPrimary else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            time,
            style = MaterialTheme.typography.titleLarge,
            color = if (highlighted) colors.onPrimary else colors.onSurface,
        )
    }
}

// ── 2 Zmanim for where you are: current location, saved cities, Add city ─────────────────

@Composable
internal fun LocationIllustration(onAddCity: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Decorative {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.secondaryContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = colors.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.walkthrough_sample_current_location),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSecondaryContainer,
                )
                Text(
                    stringResource(R.string.walkthrough_sample_jerusalem),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSecondaryContainer,
                )
            }
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onSecondaryContainer)
        }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerLowest)
        ) {
            CityRow(R.string.walkthrough_sample_tel_aviv, R.string.walkthrough_sample_tel_aviv_detail, "14:12")
            HorizontalDivider(color = colors.surfaceContainer)
            CityRow(R.string.walkthrough_sample_new_york, R.string.walkthrough_sample_new_york_detail, "08:12")
            HorizontalDivider(color = colors.surfaceContainer)
            CityRow(R.string.walkthrough_sample_london, R.string.walkthrough_sample_london_detail, "12:12")
        }
    }
    Spacer(Modifier.height(12.dp))
    // The one live control in the illustrations: adds a city in place
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        FilledTonalButton(onClick = onAddCity) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.walkthrough_add_city))
        }
    }
}

@Composable
private fun CityRow(name: Int, detail: Int, time: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(name), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(time, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ── 3 Every day, Shabbat and holiday: a day's panel and the chosen zmanim ─────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DaysIllustration() {
    val locale = locale()
    val hebrew = locale.isHebrew()
    val colors = MaterialTheme.colorScheme
    Decorative {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerLowest).padding(16.dp)
        ) {
            Text(dayLine(SAMPLE_DAY, locale), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(hebrewDate(SAMPLE_DAY, hebrew), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            DetailRow(
                Icons.Outlined.AutoStories,
                stringResource(R.string.walkthrough_sample_parasha),
                parasha(SAMPLE_DAY.plusDays(1), hebrew),
            )
            DetailRow(Icons.Outlined.WbTwilight, stringResource(R.string.candle_lighting), SAMPLE_CANDLES, emphasised = true)
            DetailRow(Icons.Outlined.WbTwilight, stringResource(R.string.havdalah), SAMPLE_HAVDALAH)
        }
        Spacer(Modifier.height(12.dp))
        // Sized to their labels and wrapped, so long Hebrew names are never cut
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ZmanChip(stringResource(R.string.walkthrough_sample_dawn), selected = false)
            ZmanChip(stringResource(R.string.walkthrough_sample_sunrise), selected = true)
            ZmanChip(stringResource(R.string.walkthrough_sample_sunset), selected = true)
            ZmanChip(stringResource(R.string.walkthrough_sample_nightfall), selected = true)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.clip(CircleShape).background(colors.tertiaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Public, contentDescription = null, tint = colors.onTertiaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.walkthrough_sample_auto_detect),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, emphasised: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasised) colors.primary else colors.onSurface,
        )
    }
}

@Composable
private fun ZmanChip(label: String, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else colors.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ── 4 Remember every date: recurring Hebrew-date events ───────────────────────────────────

@Composable
internal fun EventsIllustration() {
    val colors = MaterialTheme.colorScheme
    Decorative {
        EventCard(
            Icons.Outlined.FavoriteBorder,
            stringResource(R.string.walkthrough_sample_anniversary),
            hebrewDayMonth(21, JewishDate.KISLEV),
            stringResource(R.string.walkthrough_sample_every_year),
        )
        Spacer(Modifier.height(8.dp))
        EventCard(
            Icons.Outlined.WbTwilight,
            stringResource(R.string.walkthrough_sample_yahrzeit_event),
            hebrewDayMonth(3, JewishDate.ADAR),
            stringResource(R.string.walkthrough_sample_every_year),
        )
        Spacer(Modifier.height(8.dp))
        EventCard(
            Icons.Filled.Cake,
            stringResource(R.string.walkthrough_sample_birthday),
            hebrewDayMonth(15, JewishDate.SHEVAT),
            stringResource(R.string.walkthrough_sample_on_contact),
            badge = Icons.Outlined.Person,
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun EventCard(icon: ImageVector, title: String, hebrewDate: String, detail: String, badge: ImageVector? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerLowest).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(hebrewDate, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                Text("  ·  $detail", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
        Icon(
            badge ?: Icons.Outlined.Notifications,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── 5 Never miss candle lighting: a notification shade ────────────────────────────────────

@Composable
internal fun RemindersIllustration() {
    val hebrew = locale().isHebrew()
    Decorative {
        NotificationCard(
            stringResource(R.string.walkthrough_sample_notify_candles_title, SAMPLE_CANDLES),
            stringResource(R.string.walkthrough_sample_shabbat, parasha(SAMPLE_DAY.plusDays(1), hebrew)),
            "08:00",
        )
        Spacer(Modifier.height(8.dp))
        NotificationCard(
            stringResource(R.string.walkthrough_sample_notify_holiday_title),
            stringResource(R.string.walkthrough_sample_notify_holiday_text),
            "09:00",
        )
        Spacer(Modifier.height(8.dp))
        NotificationCard(
            stringResource(R.string.walkthrough_sample_anniversary),
            stringResource(R.string.walkthrough_sample_notify_event_text, hebrewDayMonth(21, JewishDate.KISLEV)),
            "07:30",
        )
        Spacer(Modifier.height(8.dp))
        NotificationCard(
            stringResource(R.string.walkthrough_sample_notify_omer_title),
            stringResource(R.string.walkthrough_sample_notify_omer_text),
            "18:05",
        )
    }
}

@Composable
private fun NotificationCard(title: String, text: String, time: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerLowest).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name) + " · " + time,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── 6 Your calendars together: chosen phone calendars beside the Jewish day ───────────────

private val WORK_COLOR = Color(0xFF3F7F5F)
private val FAMILY_COLOR = Color(0xFFB0573C)

@Composable
internal fun CalendarsIllustration() {
    val locale = locale()
    val colors = MaterialTheme.colorScheme
    Decorative {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerLowest).padding(vertical = 6.dp)) {
            CalendarChoice(stringResource(R.string.walkthrough_sample_calendar_family), FAMILY_COLOR, checked = true)
            CalendarChoice(stringResource(R.string.walkthrough_sample_calendar_work), WORK_COLOR, checked = true)
            CalendarChoice(stringResource(R.string.walkthrough_sample_calendar_birthdays), colors.outline, checked = false)
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerLowest).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dayLine(SAMPLE_DAY, locale),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(hebrewDate(SAMPLE_DAY, locale.isHebrew()), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            AgendaRow(colors.primary, stringResource(R.string.candle_lighting), SAMPLE_CANDLES)
            AgendaRow(WORK_COLOR, stringResource(R.string.walkthrough_sample_event_meeting), "10:00")
            AgendaRow(FAMILY_COLOR, stringResource(R.string.walkthrough_sample_event_dinner), "19:00")
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.walkthrough_sample_read_only),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun CalendarChoice(name: String, color: Color, checked: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
        Icon(
            if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked) colors.primary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun AgendaRow(color: Color, title: String, time: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(28.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── 7 Levana on your home screen: widgets, the Quick Settings tile and the dated icon ─────

@Composable
internal fun WidgetsIllustration() {
    val locale = locale()
    val hebrew = locale.isHebrew()
    val colors = MaterialTheme.colorScheme
    val today = jewishDate(SAMPLE_DAY)
    Decorative(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)))
            .padding(14.dp)
    ) {
        // Quick Settings tile
        Row(
            Modifier.clip(CircleShape).background(colors.primary).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.tile_name), style = MaterialTheme.typography.labelMedium, color = colors.onPrimary)
                Text(hebrewDate(SAMPLE_DAY, hebrew), style = MaterialTheme.typography.labelSmall, color = colors.onPrimary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Today widget
            Column(
                Modifier.weight(1.2f).clip(RoundedCornerShape(20.dp)).background(colors.surface).padding(12.dp)
            ) {
                Text(
                    hebrewNumbers.formatHebrewNumber(today.jewishDayOfMonth) + " " + hebrewNumbers.formatMonth(today),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    maxLines = 1,
                )
                Text(dayLine(SAMPLE_DAY, locale), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.walkthrough_sample_candles) + " " + SAMPLE_CANDLES,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
            // Month widget
            MiniMonth(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AppIcon(hebrewNumbers.formatHebrewNumber(today.jewishDayOfMonth), stringResource(R.string.app_name), colors.primary, colors.onPrimary)
            AppIcon("", "", colors.surfaceContainerHighest, colors.onSurface)
            AppIcon("", "", colors.surfaceContainerHighest, colors.onSurface)
            AppIcon("", "", colors.surfaceContainerHighest, colors.onSurface)
        }
    }
}

@Composable
private fun MiniMonth(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(colors.surface).padding(10.dp)) {
        Text(
            hebrewNumbers.formatMonth(jewishDate(SAMPLE_DAY)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        for (week in 0..3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (day in 0..6) {
                    val today = week == 2 && day == 5
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (today) colors.primary else colors.outlineVariant)
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun AppIcon(text: String, label: String, background: Color, content: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = content)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ── 8 Make it yours: light, dark, Material You and holiday colors; Hebrew or English ──────

/** A stand-in for wallpaper colors, so the card looks the same on every device. */
private val SampleWallpaperScheme = lightColorScheme(
    primary = Color(0xFF4D6537), onPrimary = Color.White,
    primaryContainer = Color(0xFFCEEBB1), onPrimaryContainer = Color(0xFF0D2000),
    secondaryContainer = Color(0xFFD9E7CA), background = Color(0xFFF9FAEF), surface = Color(0xFFF9FAEF),
    onSurface = Color(0xFF1A1C16), outlineVariant = Color(0xFFC4C8BB),
)

@Composable
internal fun AppearanceIllustration() {
    val colors = MaterialTheme.colorScheme
    Decorative {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniPhone(LightColorScheme, stringResource(R.string.walkthrough_sample_light))
            MiniPhone(DarkColorScheme, stringResource(R.string.walkthrough_sample_dark))
            MiniPhone(SampleWallpaperScheme, stringResource(R.string.walkthrough_sample_material_you))
            MiniPhone(remember { HolidayTheme.CHANUKAH.lightScheme() }, stringResource(R.string.walkthrough_sample_holiday))
        }
        Spacer(Modifier.height(16.dp))
        // Hebrew or English, each in its own language
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).border(1.dp, colors.outline, CircleShape),
        ) {
            LanguageSegment("עברית", selected = locale().isHebrew())
            Box(Modifier.width(1.dp).height(40.dp).background(colors.outline))
            LanguageSegment("English", selected = !locale().isHebrew())
        }
    }
}

@Composable
private fun RowScope.MiniPhone(scheme: ColorScheme, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.55f)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.background)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(6.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(scheme.primary))
            Spacer(Modifier.height(6.dp))
            repeat(3) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { col ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (row == 1 && col == 1) scheme.primary else scheme.secondaryContainer)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(scheme.primaryContainer).align(Alignment.End))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowScope.LanguageSegment(label: String, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.weight(1f).height(40.dp).background(if (selected) colors.secondaryContainer else Color.Transparent),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onSecondaryContainer else colors.onSurface)
    }
}

// ── Agenda: two days of the real agenda list, with fixed sample entries ───────────────────

@Composable
internal fun AgendaIllustration() {
    val hebrew = locale().isHebrew()
    val calendar = remember { CalendarRepository() }
    val shabbat = SAMPLE_DAY.plusDays(1)
    val dinnerStart = SAMPLE_DAY.atTime(19, 0)
    val dinner = SystemCalendarEvent(
        eventId = 0, title = stringResource(R.string.walkthrough_sample_agenda_event),
        startTime = dinnerStart, endTime = dinnerStart.plusHours(2), allDay = false,
        calendarId = 0, calendarColor = MaterialTheme.colorScheme.primary.toArgb(),
        location = stringResource(R.string.walkthrough_sample_agenda_place),
    )
    val days = listOf(
        AgendaDay(
            SAMPLE_DAY,
            calendar.getHebrewDay(SAMPLE_DAY),
            listOf(AgendaItem.CandleLighting(LocalTime.parse(SAMPLE_CANDLES)), AgendaItem.DeviceEventItem(dinner, dinnerStart.toLocalTime())),
        ),
        AgendaDay(
            shabbat,
            calendar.getHebrewDay(shabbat),
            listOf(AgendaItem.Shabbat(parasha(shabbat, hebrew = false), parasha(shabbat, hebrew = true), null, LocalTime.parse(SAMPLE_HAVDALAH))),
        ),
    )
    // The real list at a slightly smaller text size, so a phone-width row fits the frame;
    // times in the same 24-hour form as the other illustrations' samples
    val density = LocalDensity.current
    Decorative {
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * 0.85f)) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                days.forEach { day ->
                    DayRow(
                        day, isToday = day.date == SAMPLE_DAY, hebrewLeading = true, otherClockCityName = null,
                        onClickItem = {}, clickLabel = null, use24Hour = true,
                    )
                }
            }
        }
    }
}

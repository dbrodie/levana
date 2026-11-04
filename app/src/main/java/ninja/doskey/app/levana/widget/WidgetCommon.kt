package ninja.doskey.app.levana.widget

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.text.layoutDirection
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.text.TextAlign
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.ui.MISSING_TIME
import ninja.doskey.app.levana.ui.formatClockTime

/** The app language, which the widgets follow. */
internal fun Context.widgetLocale(): Locale = resources.configuration.locales[0]

internal fun Locale.isHebrewUi(): Boolean = language in setOf("he", "iw")

/**
 * Whether a widget must mirror its rows. The launcher lays the widget out in the device's
 * direction; mirror it when the app language reads the other way (e.g. Hebrew app language
 * on an English device).
 */
internal fun Context.widgetMirrored(): Boolean {
    val deviceLocale = getSystemService(LocaleManager::class.java)?.systemLocales?.get(0) ?: widgetLocale()
    return resources.configuration.layoutDirection != deviceLocale.layoutDirection
}

/** Opens the month calendar with [date] selected. */
internal fun openCalendar(context: Context, date: LocalDate): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )

/** Opens the Zmanim screen for [date]. */
internal fun openZmanim(context: Context, date: LocalDate): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            .putExtra(MainActivity.EXTRA_OPEN_ZMANIM, true)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )

/** Shown for a time that can't be computed. */
internal const val NO_TIME = MISSING_TIME

/** Text alignment that keeps a mirrored row's text against its edge. */
internal fun textAlign(mirror: Boolean) = if (mirror) TextAlign.End else TextAlign.Start

/** Formats a time on the device's 12/24-hour clock, isolated in the Hebrew UI. */
internal fun widgetTimeFormat(context: Context): (ZonedDateTime) -> String {
    val use24Hour = DateFormat.is24HourFormat(context)
    val hebrewUi = context.widgetLocale().isHebrewUi()
    return { formatClockTime(it.toLocalTime(), use24Hour, hebrewUi) }
}

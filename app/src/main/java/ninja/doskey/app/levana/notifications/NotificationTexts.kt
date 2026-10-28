package ninja.doskey.app.levana.notifications

import android.content.Context
import android.text.format.DateFormat
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.ui.omerDayLabel
import ninja.doskey.app.levana.ui.omerWeeksText
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Text builders for every notification except birthdays (see [birthdayNotificationText]).
 *
 * Each takes a context that already carries the app language (see [withAppLocale]) and
 * returns the title and body from string resources, so names and times are in that
 * language too.
 */
data class NotificationText(val title: String, val body: String)

private const val SEPARATOR = " · "

private val TIME_12H = DateTimeFormatter.ofPattern("h:mm a")
private val TIME_24H = DateTimeFormatter.ofPattern("HH:mm")

/** True when [this] context's language is Hebrew. */
internal fun Context.isHebrewLanguage(): Boolean =
    resources.configuration.locales[0].language in setOf("he", "iw")

/**
 * A notification time. In Hebrew this follows the app's screens (24-hour unless the device
 * forces 12-hour). In English it stays `h:mm a`, as notifications always were.
 */
fun notificationTime(context: Context, time: LocalTime): String {
    val formatter = if (context.isHebrewLanguage() && DateFormat.is24HourFormat(context)) {
        TIME_24H
    } else {
        TIME_12H.withLocale(context.resources.configuration.locales[0])
    }
    return time.format(formatter)
}

/** A holiday or fast name in [context]'s language, from [ninja.doskey.app.levana.data.HolidayMapper]. */
fun holidayDisplayName(context: Context, holiday: Holiday): String =
    if (context.isHebrewLanguage()) holiday.hebrewName else holiday.name

/**
 * Candle lighting on [date], with the havdalah of the last consecutive Shabbat / Yom Tov
 * day. The end is labelled Yom Tov when it includes one, and names its weekday when it is
 * not the next day.
 */
fun candleLightingNotificationText(
    context: Context,
    date: LocalDate,
    candleLightingTime: LocalTime,
    end: ObservanceEnd?
): NotificationText {
    val time = notificationTime(context, candleLightingTime)
    val endTime = end?.time
    val body = if (endTime != null) {
        val endText = if (end.date == date.plusDays(1)) {
            notificationTime(context, endTime)
        } else {
            val weekday = end.date.dayOfWeek
                .getDisplayName(TextStyle.FULL, context.resources.configuration.locales[0])
            "$weekday ${notificationTime(context, endTime)}"
        }
        context.getString(
            if (end.includesYomTov) {
                R.string.notification_candle_lighting_body_with_yom_tov_end
            } else {
                R.string.notification_candle_lighting_body_with_end
            },
            time,
            endText
        )
    } else {
        context.getString(R.string.notification_candle_lighting_body, time)
    }
    return NotificationText(context.getString(R.string.notification_candle_lighting_title), body)
}

fun holidayNotificationText(context: Context, holiday: Holiday, daysUntil: Int): NotificationText {
    val name = holidayDisplayName(context, holiday)
    val body = when (daysUntil) {
        0 -> context.getString(R.string.notification_holiday_today, name)
        1 -> context.getString(R.string.notification_holiday_tomorrow, name)
        else -> context.resources.getQuantityString(
            R.plurals.notification_holiday_in_days,
            daysUntil,
            name,
            daysUntil
        )
    }
    return NotificationText(context.getString(R.string.notification_holiday_title), body)
}

fun fastNotificationText(
    context: Context,
    fast: Holiday,
    startTime: LocalTime?,
    endTime: LocalTime?
): NotificationText {
    val parts = buildList {
        add(holidayDisplayName(context, fast))
        if (startTime != null) {
            add(context.getString(R.string.notification_fast_begins, notificationTime(context, startTime)))
        }
        if (endTime != null) {
            add(context.getString(R.string.notification_fast_ends, notificationTime(context, endTime)))
        }
    }
    return NotificationText(
        context.getString(R.string.notification_fast_title),
        parts.joinToString(SEPARATOR)
    )
}

/** [eventTitle] already includes the occurrence label, if any. */
fun personalEventNotificationText(context: Context, eventTitle: String): NotificationText =
    NotificationText(
        context.getString(R.string.notification_personal_event_title),
        context.getString(R.string.notification_personal_event_body, eventTitle)
    )

/**
 * The Omer count for [omerDay] (1–49). [tonight] is the tzait reminder; otherwise the
 * morning reminder. In Hebrew the day is written the traditional way by KosherJava
 * (e.g. "ח׳ בעומר", "ל״ג בעומר").
 */
fun omerNotificationText(context: Context, omerDay: Int, tonight: Boolean): NotificationText {
    val day = omerDayLabel(context, omerDay)
    val weeksText = omerWeeksText(context, omerDay)
    val body = when {
        weeksText == null && tonight -> context.getString(R.string.notification_omer_tonight, day)
        weeksText == null -> context.getString(R.string.notification_omer_today, day)
        tonight -> context.getString(R.string.notification_omer_tonight_with_weeks, day, weeksText)
        else -> context.getString(R.string.notification_omer_today_with_weeks, day, weeksText)
    }
    return NotificationText(context.getString(R.string.notification_omer_title), body)
}

fun updateAvailableNotificationText(context: Context, latestVersion: String): NotificationText =
    NotificationText(
        context.getString(R.string.notification_update_title),
        context.getString(R.string.notification_update_body, latestVersion)
    )

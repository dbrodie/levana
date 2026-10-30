package ninja.doskey.app.levana.ui.settings

/**
 * Format arguments for R.string.settings_at_morning_time, from minutes after midnight:
 * the 12-hour clock hour, the minutes and the 24-hour clock hour. English shows the
 * 12-hour hour with a fixed "AM" (so noon reads "12:00 AM", see TASK-32); Hebrew
 * shows the 24-hour hour.
 */
internal fun morningTimeFormatArgs(minutesOfDay: Int): Array<Any> {
    val hours = minutesOfDay / 60
    val minutes = minutesOfDay % 60
    return arrayOf(
        if (hours == 0) 12 else if (hours > 12) hours - 12 else hours,
        minutes,
        hours
    )
}

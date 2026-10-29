package ninja.doskey.app.levana.ui.calendar

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HolidayCategory

/**
 * A month's days laid out in Sunday-first weeks of seven cells, with null for the cells
 * before the first day and after the last. Shared by the calendar screen and the month widget.
 */
fun monthGridWeeks(days: List<HebrewDay>): List<List<HebrewDay?>> {
    if (days.isEmpty()) return emptyList()
    val leadingEmptyCells = days.first().gregorianDate.dayOfWeek.value % 7
    val cells: List<HebrewDay?> = List(leadingEmptyCells) { null } + days
    return cells.chunked(7) { week -> week + List(7 - week.size) { null } }
}

/** The marker color of a holiday category, from the app's [scheme]. */
fun holidayCategoryColor(category: HolidayCategory, scheme: ColorScheme): Color = when (category) {
    HolidayCategory.TORAH -> scheme.primary
    HolidayCategory.RABBINIC -> scheme.secondary
    HolidayCategory.FAST -> scheme.error
    HolidayCategory.MINOR -> scheme.tertiary
    HolidayCategory.MODERN_ISRAELI -> scheme.primary
}

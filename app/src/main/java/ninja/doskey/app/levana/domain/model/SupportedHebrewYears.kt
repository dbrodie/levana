package ninja.doskey.app.levana.domain.model

import com.kosherjava.zmanim.hebrewcalendar.JewishDate

/**
 * The Hebrew years the app lets a user enter (owner decision, TASK-24):
 * 5600–6000, about 1839–2240. The single source for the event and birthday
 * editors' validation and state and for the date pickers' bounds.
 *
 * Stored or imported dates outside this range are valid data, not errors:
 * the editors show and save them unchanged until the user picks a new date
 * (see specs/rfes/fix-adar-and-year-followups.md, section 2a).
 */
object SupportedHebrewYears {
    const val FIRST = 5600
    const val LAST = 6000

    val range: IntRange = FIRST..LAST

    operator fun contains(hebrewYear: Int): Boolean = hebrewYear in range

    /** [hebrewYear] moved to the nearest supported year. */
    fun coerce(hebrewYear: Int): Int = hebrewYear.coerceIn(FIRST, LAST)

    /**
     * Gregorian years lying wholly inside the supported Hebrew years
     * (1840..2239), for the Gregorian side of the date pickers.
     */
    val gregorianYears: IntRange by lazy {
        val first = JewishDate(FIRST, JewishDate.TISHREI, 1).gregorianYear
        val lastTishrei = JewishDate(LAST + 1, JewishDate.TISHREI, 1)
        first + 1..lastTishrei.gregorianYear - 1
    }
}

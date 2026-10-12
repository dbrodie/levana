package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate

object HebrewDateMatcher {

    /**
     * Earliest Hebrew year KosherJava accepts for 1 Tishrei. Stored years
     * below this (0 = unknown, missing, or garbage from other apps) are
     * treated as an unknown year.
     */
    const val MIN_KNOWN_YEAR = 3762

    /**
     * Largest year KosherJava can write as a Hebrew numeral
     * (HebrewDateFormatter.formatHebrewNumber rejects anything above it).
     * Stored years above this are treated as an unknown year too.
     */
    const val MAX_KNOWN_YEAR = 9999

    /** Years the date pickers (editors, Go to date, date override) accept. */
    val EDITABLE_YEARS = 5700..5900

    fun isKnownYear(hebrewYear: Int): Boolean =
        hebrewYear in MIN_KNOWN_YEAR..MAX_KNOWN_YEAR

    /**
     * A month number that exists in [hebrewYear]: Adar II of a regular year
     * becomes Adar. Used by the editors so a date is never saved with a month
     * its year does not have.
     */
    fun monthForYear(hebrewMonth: Int, hebrewYear: Int): Int =
        if (hebrewMonth == JewishDate.ADAR_II && !isEventLeapYear(hebrewYear)) {
            JewishDate.ADAR
        } else {
            hebrewMonth
        }

    /**
     * Whether the year an event was recorded in is a leap year. Never throws:
     * an unknown or out-of-range year counts as a non-leap year, so a stored
     * Adar is treated as plain Adar.
     */
    fun isEventLeapYear(hebrewYear: Int): Boolean {
        if (!isKnownYear(hebrewYear)) return false
        return (7 * hebrewYear + 1) % 19 < 7
    }

    /**
     * Month resolution for birthdays and custom events (no yahrzeit rule).
     *
     * In a leap target year anything recorded in Adar falls in Adar II. The
     * one exception is a date the user explicitly recorded in Adar I (month
     * 12 of a known leap year), which stays in Adar I. In a regular target
     * year Adar, Adar I and Adar II all fall in Adar.
     */
    fun resolveSimpleMonth(
        eventMonth: Int,
        eventIsLeapYear: Boolean,
        targetIsLeapYear: Boolean
    ): Int {
        if (!targetIsLeapYear) {
            return if (eventMonth == JewishDate.ADAR ||
                eventMonth == JewishDate.ADAR_II
            ) {
                JewishDate.ADAR
            } else {
                eventMonth
            }
        }

        // Target IS leap year
        return when {
            // Adar of a regular or unknown year -> Adar II
            eventMonth == JewishDate.ADAR && !eventIsLeapYear -> JewishDate.ADAR_II
            // Explicitly recorded Adar I -> Adar I
            eventMonth == JewishDate.ADAR && eventIsLeapYear -> JewishDate.ADAR
            eventMonth == JewishDate.ADAR_II -> JewishDate.ADAR_II
            else -> eventMonth
        }
    }

    /**
     * Yahrzeit Adar rules per Shulchan Aruch:
     * - Death in Adar of non-leap year -> observe in Adar II of leap year
     * - Death in Adar I of leap year -> observe in Adar I
     * - Death in Adar II of leap year -> observe in Adar II
     */
    fun resolveYahrzeitMonth(
        eventMonth: Int,
        eventIsLeapYear: Boolean,
        targetIsLeapYear: Boolean
    ): Int {
        if (!targetIsLeapYear) {
            return if (eventMonth == JewishDate.ADAR ||
                eventMonth == JewishDate.ADAR_II
            ) {
                JewishDate.ADAR
            } else {
                eventMonth
            }
        }

        // Target IS leap year
        return when {
            // Death in Adar of non-leap year -> Adar II
            eventMonth == JewishDate.ADAR && !eventIsLeapYear -> JewishDate.ADAR_II
            // Death in Adar I (stored as ADAR in leap year) -> Adar I
            eventMonth == JewishDate.ADAR && eventIsLeapYear -> JewishDate.ADAR
            // Death in Adar II -> Adar II
            eventMonth == JewishDate.ADAR_II -> JewishDate.ADAR_II
            else -> eventMonth
        }
    }

    /**
     * The month (KosherJava numbering) in which a stored recurring date falls
     * in a target year. Every mapping of a stored date onto a year goes
     * through here so the calendar, the day panel, lists and notifications
     * cannot disagree.
     */
    fun resolveMonth(
        hebrewMonth: Int,
        hebrewYear: Int,
        useYahrzeitRules: Boolean,
        targetIsLeapYear: Boolean
    ): Int {
        val eventIsLeapYear = isEventLeapYear(hebrewYear)
        return if (useYahrzeitRules) {
            resolveYahrzeitMonth(hebrewMonth, eventIsLeapYear, targetIsLeapYear)
        } else {
            resolveSimpleMonth(hebrewMonth, eventIsLeapYear, targetIsLeapYear)
        }
    }

    fun matchesDate(
        hebrewDay: Int,
        hebrewMonth: Int,
        hebrewYear: Int,
        useYahrzeitRules: Boolean,
        targetMonth: Int,
        targetDay: Int,
        targetIsLeapYear: Boolean
    ): Boolean {
        if (hebrewDay != targetDay) return false
        return resolveMonth(hebrewMonth, hebrewYear, useYahrzeitRules, targetIsLeapYear) ==
            targetMonth
    }
}

package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.util.GregorianCalendar
import ninja.doskey.app.levana.domain.model.Occurrence
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules

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

    /** The Hebrew year a Gregorian date falls in. */
    fun hebrewYearOf(date: LocalDate): Int =
        JewishDate(
            GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        ).jewishYear

    /**
     * The month and year an editor shows for a stored date, as (month, year).
     *
     * A known year is shown as stored. An unknown year is shown as
     * [currentYear], with the month the date falls in that year, so saving
     * without touching anything does not move an unknown-year Adar from
     * Adar II to Adar I.
     *
     * A yahrzeit in Adar with an unknown year is the exception: no single
     * date in the current year behaves like it under every yahrzeit setting,
     * so it is shown on the latest year that does (an ordinary year followed
     * by an ordinary year for Adar, a leap year for Adar II).
     */
    fun editorDate(
        hebrewMonth: Int,
        hebrewYear: Int,
        useYahrzeitRules: Boolean,
        currentYear: Int
    ): Pair<Int, Int> {
        if (isKnownYear(hebrewYear)) return monthForYear(hebrewMonth, hebrewYear) to hebrewYear
        if (useYahrzeitRules) {
            when (hebrewMonth) {
                JewishDate.ADAR -> return hebrewMonth to latestYearUpTo(currentYear) {
                    !isEventLeapYear(it) && !isEventLeapYear(it + 1)
                }
                JewishDate.ADAR_II -> return hebrewMonth to latestYearUpTo(currentYear) {
                    isEventLeapYear(it)
                }
            }
        }
        val month = resolveSimpleMonth(
            hebrewMonth,
            eventIsLeapYear = false,
            targetIsLeapYear = isEventLeapYear(currentYear)
        )
        return month to currentYear
    }

    private fun latestYearUpTo(year: Int, predicate: (Int) -> Boolean): Int =
        generateSequence(year) { it - 1 }.first(predicate)

    /**
     * Whether a year is a leap year. Never throws: an unknown or out-of-range
     * year counts as a non-leap year, so a stored Adar is treated as plain
     * Adar.
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
     * Every day on which a stored recurring date is observed in [targetYear]
     * (a Hebrew year). This is the one resolver behind the calendar markers
     * (both modes), the day panel, notifications and both repositories.
     *
     * Without the yahrzeit rule there is always exactly one occurrence
     * ([resolveSimpleMonth]) and [rules] are ignored. With it, the owner's
     * yahrzeit Adar rules apply (see specs/rfes/fix-adar-and-year-followups.md,
     * "Yahrzeit Adar rules"); some settings give two occurrences, each
     * labelled.
     */
    fun occurrences(
        hebrewDay: Int,
        hebrewMonth: Int,
        hebrewYear: Int,
        useYahrzeitRules: Boolean,
        targetYear: Int,
        rules: YahrzeitRules = YahrzeitRules()
    ): List<Occurrence> {
        val targetIsLeapYear = isEventLeapYear(targetYear)
        if (!useYahrzeitRules) {
            val month = resolveSimpleMonth(
                hebrewMonth,
                isEventLeapYear(hebrewYear),
                targetIsLeapYear
            )
            return listOf(Occurrence(month, hebrewDay))
        }
        return yahrzeitOccurrences(
            hebrewDay, hebrewMonth, hebrewYear, targetYear, targetIsLeapYear, rules
        )
    }

    private fun yahrzeitOccurrences(
        day: Int,
        month: Int,
        deathYear: Int,
        targetYear: Int,
        targetIsLeapYear: Boolean,
        rules: YahrzeitRules
    ): List<Occurrence> {
        if (day == 30 && (month == JewishDate.CHESHVAN || month == JewishDate.KISLEV)) {
            return day30CheshvanKislevOccurrences(month, deathYear, targetYear, rules)
        }
        if (month != JewishDate.ADAR && month != JewishDate.ADAR_II) {
            return listOf(Occurrence(month, day))
        }

        // Section 1, Adar II. Also a stored Adar II with an unknown year.
        if (month == JewishDate.ADAR_II) {
            val target = if (targetIsLeapYear) JewishDate.ADAR_II else JewishDate.ADAR
            return listOf(Occurrence(target, day))
        }

        // Section 1, Adar I: month 12 of a known leap year
        if (isEventLeapYear(deathYear)) {
            return when {
                targetIsLeapYear -> listOf(Occurrence(JewishDate.ADAR, day))
                day == 30 && rules.adarI30AlsoNisan1 -> listOf(
                    Occurrence(JewishDate.SHEVAT, 30, OccurrenceLabel.SHEVAT_30),
                    Occurrence(JewishDate.NISSAN, 1, OccurrenceLabel.NISAN_1)
                )
                day == 30 -> listOf(Occurrence(JewishDate.SHEVAT, 30))
                else -> listOf(Occurrence(JewishDate.ADAR, day))
            }
        }

        // Sections 2 and 3: Adar of an ordinary year, or an unknown death year
        if (!targetIsLeapYear) return listOf(Occurrence(JewishDate.ADAR, day))

        // Section 3 applies only to the year right after a known death year
        // (that year is a leap year here, since the target is)
        val isFirstYear = isKnownYear(deathYear) && targetYear == deathYear + 1
        val choice = when {
            isFirstYear && rules.firstYear == YahrzeitFirstYear.ADAR_I ->
                YahrzeitLeapYearAdar.ADAR_I
            isFirstYear && rules.firstYear == YahrzeitFirstYear.BOTH ->
                YahrzeitLeapYearAdar.BOTH
            else -> rules.leapYearAdar
        }
        return when (choice) {
            YahrzeitLeapYearAdar.ADAR_I -> listOf(Occurrence(JewishDate.ADAR, day))
            YahrzeitLeapYearAdar.ADAR_II -> listOf(Occurrence(JewishDate.ADAR_II, day))
            YahrzeitLeapYearAdar.BOTH -> listOf(
                Occurrence(JewishDate.ADAR, day, OccurrenceLabel.ADAR_I),
                Occurrence(JewishDate.ADAR_II, day, OccurrenceLabel.ADAR_II)
            )
        }
    }

    /**
     * Spec section 1c: a yahrzeit on 30 Cheshvan or 30 Kislev. Each year is
     * computed from the death date alone, never from an earlier recurrence.
     */
    private fun day30CheshvanKislevOccurrences(
        month: Int,
        deathYear: Int,
        targetYear: Int,
        rules: YahrzeitRules
    ): List<Occurrence> {
        // The date exists this year: always the original date
        if (daysInMonth(targetYear, month) == 30) return listOf(Occurrence(month, 30))

        val nextMonth = month + 1 // Cheshvan -> Kislev, Kislev -> Teves
        val roshChodesh = Occurrence(nextMonth, 1)
        val day29 = Occurrence(month, 29)
        return when (rules.missingDay) {
            YahrzeitMissingDay.ROSH_CHODESH -> listOf(roshChodesh)
            YahrzeitMissingDay.BOTH -> if (month == JewishDate.CHESHVAN) {
                listOf(
                    day29.copy(label = OccurrenceLabel.CHESHVAN_29),
                    roshChodesh.copy(label = OccurrenceLabel.KISLEV_1)
                )
            } else {
                listOf(
                    day29.copy(label = OccurrenceLabel.KISLEV_29),
                    roshChodesh.copy(label = OccurrenceLabel.TEVES_1)
                )
            }
            YahrzeitMissingDay.FIRST_ANNIVERSARY -> {
                // Unknown death year: no first anniversary, fall back to Rosh Chodesh
                val firstAnniversary = deathYear + 1
                if (isKnownYear(deathYear) && isKnownYear(firstAnniversary) &&
                    daysInMonth(firstAnniversary, month) == 29
                ) {
                    listOf(day29)
                } else {
                    listOf(roshChodesh)
                }
            }
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int =
        JewishDate(year, month, 1).daysInJewishMonth

    /** The occurrence of a stored date on a target Hebrew day, or null. */
    fun occurrenceOn(
        hebrewDay: Int,
        hebrewMonth: Int,
        hebrewYear: Int,
        useYahrzeitRules: Boolean,
        targetYear: Int,
        targetMonth: Int,
        targetDay: Int,
        rules: YahrzeitRules = YahrzeitRules()
    ): Occurrence? =
        occurrences(hebrewDay, hebrewMonth, hebrewYear, useYahrzeitRules, targetYear, rules)
            .firstOrNull { it.month == targetMonth && it.day == targetDay }

    fun matchesDate(
        hebrewDay: Int,
        hebrewMonth: Int,
        hebrewYear: Int,
        useYahrzeitRules: Boolean,
        targetYear: Int,
        targetMonth: Int,
        targetDay: Int,
        rules: YahrzeitRules = YahrzeitRules()
    ): Boolean = occurrenceOn(
        hebrewDay, hebrewMonth, hebrewYear, useYahrzeitRules,
        targetYear, targetMonth, targetDay, rules
    ) != null
}

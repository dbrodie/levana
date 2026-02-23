package com.levana.app.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate

object HebrewDateMatcher {

    /**
     * Simple month resolution for birthdays and custom events.
     * For Adar: map to whichever Adar exists in the target year.
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
            eventMonth == JewishDate.ADAR && !eventIsLeapYear -> JewishDate.ADAR
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
        val eventIsLeapYear =
            JewishDate(hebrewYear, JewishDate.TISHREI, 1).isJewishLeapYear
        val resolvedMonth = if (useYahrzeitRules) {
            resolveYahrzeitMonth(hebrewMonth, eventIsLeapYear, targetIsLeapYear)
        } else {
            resolveSimpleMonth(hebrewMonth, eventIsLeapYear, targetIsLeapYear)
        }
        return resolvedMonth == targetMonth
    }
}

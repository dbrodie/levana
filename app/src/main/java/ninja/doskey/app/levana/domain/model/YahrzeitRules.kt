package ninja.doskey.app.levana.domain.model

/**
 * Leap-year observance of a yahrzeit whose death was in Adar of an ordinary
 * year (or with an unknown death year).
 */
enum class YahrzeitLeapYearAdar { ADAR_I, ADAR_II, BOTH }

/**
 * The first yahrzeit when the death was in an ordinary year's Adar and the
 * following year is a leap year.
 */
enum class YahrzeitFirstYear { FOLLOW_ANNUAL, ADAR_I, BOTH }

/**
 * Where a 30 Cheshvan / 30 Kislev yahrzeit falls in a year when that month
 * has only 29 days.
 */
enum class YahrzeitMissingDay {
    /** 29th if the month had 29 days in the first anniversary year, else Rosh Chodesh. */
    FIRST_ANNIVERSARY,
    ROSH_CHODESH,
    BOTH
}

/**
 * Global yahrzeit settings, independent of [Minhag]. Defaults confirmed by
 * the owner: both Adars in a leap year, first year follows the annual
 * setting, no extra 1 Nisan, first-anniversary rule for 30 Cheshvan/Kislev.
 */
data class YahrzeitRules(
    val leapYearAdar: YahrzeitLeapYearAdar = YahrzeitLeapYearAdar.BOTH,
    val firstYear: YahrzeitFirstYear = YahrzeitFirstYear.FOLLOW_ANNUAL,
    /** For a yahrzeit on 30 Adar I, also mark 1 Nisan in ordinary years. */
    val adarI30AlsoNisan1: Boolean = false,
    /** 30 Cheshvan / 30 Kislev in a year when that day does not exist. */
    val missingDay: YahrzeitMissingDay = YahrzeitMissingDay.FIRST_ANNIVERSARY
)

/**
 * Why an occurrence is there: which of two dates it is when a date occurs
 * twice in a year, or that it was moved from a day 30 the month lacks.
 */
sealed interface OccurrenceLabel {
    data object ADAR_I : OccurrenceLabel
    data object ADAR_II : OccurrenceLabel
    data object SHEVAT_30 : OccurrenceLabel
    data object NISAN_1 : OccurrenceLabel
    data object CHESHVAN_29 : OccurrenceLabel
    data object KISLEV_1 : OccurrenceLabel
    data object KISLEV_29 : OccurrenceLabel
    data object TEVES_1 : OccurrenceLabel

    /** A birthday / custom event moved to the 1st because [month] has no day 30 this year. */
    data class DelayedFrom30(val month: HebrewMonth) : OccurrenceLabel
}

/** One day (KosherJava month numbering) on which a stored date is observed. */
data class Occurrence(
    val month: Int,
    val day: Int,
    val label: OccurrenceLabel? = null
)

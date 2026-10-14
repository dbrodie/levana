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
 * Global yahrzeit Adar settings. The defaults reproduce the app's behaviour
 * before these settings existed. Independent of [Minhag].
 */
data class YahrzeitRules(
    val leapYearAdar: YahrzeitLeapYearAdar = YahrzeitLeapYearAdar.ADAR_II,
    val firstYear: YahrzeitFirstYear = YahrzeitFirstYear.FOLLOW_ANNUAL,
    /** For a yahrzeit on 30 Adar I, also mark 1 Nisan in ordinary years. */
    val adarI30AlsoNisan1: Boolean = false,
    /** 30 Cheshvan / 30 Kislev in a year when that day does not exist. */
    val missingDay: YahrzeitMissingDay = YahrzeitMissingDay.FIRST_ANNIVERSARY
)

/** Why an occurrence is there, shown when a date occurs twice in a year. */
enum class OccurrenceLabel {
    ADAR_I, ADAR_II, SHEVAT_30, NISAN_1,
    CHESHVAN_29, KISLEV_1, KISLEV_29, TEVES_1
}

/** One day (KosherJava month numbering) on which a stored date is observed. */
data class Occurrence(
    val month: Int,
    val day: Int,
    val label: OccurrenceLabel? = null
)

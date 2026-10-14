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
 * Global yahrzeit Adar settings. The defaults reproduce the app's behaviour
 * before these settings existed. Independent of [Minhag].
 */
data class YahrzeitRules(
    val leapYearAdar: YahrzeitLeapYearAdar = YahrzeitLeapYearAdar.ADAR_II,
    val firstYear: YahrzeitFirstYear = YahrzeitFirstYear.FOLLOW_ANNUAL,
    /** For a yahrzeit on 30 Adar I, also mark 1 Nisan in ordinary years. */
    val adarI30AlsoNisan1: Boolean = false
)

/** Why an occurrence is there, shown when a date occurs twice in a year. */
enum class OccurrenceLabel { ADAR_I, ADAR_II, SHEVAT_30, NISAN_1 }

/** One day (KosherJava month numbering) on which a stored date is observed. */
data class Occurrence(
    val month: Int,
    val day: Int,
    val label: OccurrenceLabel? = null
)

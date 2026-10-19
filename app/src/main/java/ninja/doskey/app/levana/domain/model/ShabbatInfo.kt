package ninja.doskey.app.levana.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class ShabbatInfo(
    val candleLightingTime: LocalTime?,
    val havdalahTime: LocalTime?,
    val isErevShabbat: Boolean,
    val isShabbat: Boolean,
    val isErevYomTov: Boolean,
    val isYomTov: Boolean
) {
    /** Set only on days whose evening begins Shabbat or Yom Tov. */
    val showCandleLighting: Boolean
        get() = candleLightingTime != null

    /** Set only on the last of consecutive Shabbat / Yom Tov days. */
    val showHavdalah: Boolean
        get() = havdalahTime != null
}

/** The end of consecutive Shabbat / Yom Tov days: havdalah at [time] on [date]. */
data class ObservanceEnd(
    val date: LocalDate,
    val time: LocalTime?,
    val includesYomTov: Boolean
)

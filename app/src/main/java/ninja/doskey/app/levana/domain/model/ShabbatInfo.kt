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

/**
 * The end of consecutive Shabbat / Yom Tov days: havdalah at [time] on the civil [date], in
 * the location's zone. [lastDay] is the last holy day, whose nightfall ends them; [date] is
 * the following day when that nightfall is after midnight. Without a time, [date] is [lastDay].
 */
data class ObservanceEnd(
    val date: LocalDate,
    val time: LocalTime?,
    val includesYomTov: Boolean,
    val lastDay: LocalDate = date
)

package com.levana.app.domain.model

import java.time.LocalTime

data class ShabbatInfo(
    val candleLightingTime: LocalTime?,
    val havdalahTime: LocalTime?,
    val isErevShabbat: Boolean,
    val isShabbat: Boolean,
    val isErevYomTov: Boolean,
    val isYomTov: Boolean
) {
    val showCandleLighting: Boolean
        get() = (isErevShabbat || isErevYomTov) &&
            candleLightingTime != null

    val showHavdalah: Boolean
        get() = (isShabbat || isYomTov) && havdalahTime != null
}

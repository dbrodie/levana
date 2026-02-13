package com.levana.app.domain.model

data class UserPreferences(
    val location: Location? = null,
    val candleLightingOffset: Double = 18.0
)

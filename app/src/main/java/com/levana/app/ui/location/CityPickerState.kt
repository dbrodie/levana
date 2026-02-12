package com.levana.app.ui.location

import com.levana.app.domain.model.Location

data class CityPickerState(
    val query: String = "",
    val cities: List<Location> = emptyList(),
    val isSaving: Boolean = false
)

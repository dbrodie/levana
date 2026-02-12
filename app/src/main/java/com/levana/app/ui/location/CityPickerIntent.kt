package com.levana.app.ui.location

import com.levana.app.domain.model.Location

sealed interface CityPickerIntent {
    data class Search(val query: String) : CityPickerIntent
    data class SelectCity(val city: Location) : CityPickerIntent
}

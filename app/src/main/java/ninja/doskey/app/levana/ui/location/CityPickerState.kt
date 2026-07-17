package ninja.doskey.app.levana.ui.location

import ninja.doskey.app.levana.domain.model.Location

data class CityPickerState(
    val query: String = "",
    val cities: List<Location> = emptyList()
)

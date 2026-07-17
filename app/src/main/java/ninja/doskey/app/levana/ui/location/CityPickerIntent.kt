package ninja.doskey.app.levana.ui.location

import ninja.doskey.app.levana.domain.model.Location

sealed interface CityPickerIntent {
    data class Search(val query: String) : CityPickerIntent
    data class SelectCity(val city: Location) : CityPickerIntent
}

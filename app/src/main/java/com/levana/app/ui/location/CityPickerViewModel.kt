package com.levana.app.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.CityRepository
import com.levana.app.data.PreferencesRepository
import com.levana.app.domain.model.Location
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CityPickerViewModel(
    private val cityRepository: CityRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CityPickerState())
    val state: StateFlow<CityPickerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CityPickerEvent>()
    val events = _events.asSharedFlow()

    init {
        _state.value = _state.value.copy(
            cities = cityRepository.getAllCities()
        )
    }

    fun onIntent(intent: CityPickerIntent) {
        when (intent) {
            is CityPickerIntent.Search -> search(intent.query)
            is CityPickerIntent.SelectCity -> selectCity(intent.city)
        }
    }

    private fun search(query: String) {
        _state.value = _state.value.copy(
            query = query,
            cities = cityRepository.search(query)
        )
    }

    private fun selectCity(city: Location) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val newId = preferencesRepository.addSavedLocation(city)
            preferencesRepository.setActiveLocationId(newId)
            _events.emit(CityPickerEvent.LocationSaved)
        }
    }
}

sealed interface CityPickerEvent {
    data object LocationSaved : CityPickerEvent
}

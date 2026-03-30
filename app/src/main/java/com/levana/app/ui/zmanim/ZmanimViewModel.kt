package com.levana.app.ui.zmanim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PreferencesRepository
import com.levana.app.data.ZmanimRepository
import com.levana.app.domain.model.Location
import com.levana.app.domain.model.activeLocation
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ZmanimViewModel(
    private val zmanimRepository: ZmanimRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ZmanimState())
    val state: StateFlow<ZmanimState> = _state.asStateFlow()

    fun onIntent(intent: ZmanimIntent) {
        when (intent) {
            is ZmanimIntent.LoadDate -> loadZmanim(intent.date)
            is ZmanimIntent.LoadDefault -> viewModelScope.launch {
                val prefs = preferencesRepository.preferences.first()
                loadZmanim(prefs.devDateOverride ?: LocalDate.now())
            }
        }
    }

    private fun loadZmanim(date: LocalDate) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val prefs = preferencesRepository.preferences.first()
            val location = prefs.activeLocation ?: Location.JERUSALEM
            val zmanim = zmanimRepository.getZmanim(
                date,
                location,
                prefs.candleLightingOffset
            )
            _state.value = _state.value.copy(
                date = date,
                zmanim = zmanim,
                locationName = location.name,
                isLoading = false
            )
        }
    }
}

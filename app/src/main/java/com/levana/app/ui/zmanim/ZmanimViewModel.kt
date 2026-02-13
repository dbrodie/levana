package com.levana.app.ui.zmanim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PreferencesRepository
import com.levana.app.data.ZmanimRepository
import com.levana.app.domain.model.Location
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

    init {
        onIntent(ZmanimIntent.LoadToday)
    }

    fun onIntent(intent: ZmanimIntent) {
        when (intent) {
            is ZmanimIntent.LoadToday -> loadZmanim(LocalDate.now())
            is ZmanimIntent.LoadDate -> loadZmanim(intent.date)
        }
    }

    private fun loadZmanim(date: LocalDate) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val prefs = preferencesRepository.preferences.first()
            val location = prefs.location ?: Location.JERUSALEM
            val zmanim = zmanimRepository.getZmanim(date, location)
            _state.value = _state.value.copy(
                date = date,
                zmanim = zmanim,
                locationName = location.name,
                isLoading = false
            )
        }
    }
}

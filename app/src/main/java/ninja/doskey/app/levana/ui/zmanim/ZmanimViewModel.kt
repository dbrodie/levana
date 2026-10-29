package ninja.doskey.app.levana.ui.zmanim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.displayCity
import ninja.doskey.app.levana.ui.otherClockCityName
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ZmanimViewModel(
    private val zmanimRepository: ZmanimRepository,
    private val preferencesRepository: PreferencesRepository,
    private val deviceContext: DeviceContext
) : ViewModel() {

    private val _state = MutableStateFlow(ZmanimState())
    val state: StateFlow<ZmanimState> = _state.asStateFlow()

    fun onIntent(intent: ZmanimIntent) {
        when (intent) {
            is ZmanimIntent.LoadDate -> loadZmanim(intent.date)
            is ZmanimIntent.LoadDefault -> viewModelScope.launch {
                loadZmanim(deviceContext.today())
            }
        }
    }

    private fun loadZmanim(date: LocalDate) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val prefs = preferencesRepository.preferences.first()
            // The one screen that shows the selected city: its zmanim on the device's date
            val location = prefs.displayCity
            val zmanim = zmanimRepository.getZmanim(
                date,
                location,
                prefs.candleLightingOffset,
                prefs.isInIsrael
            )
            _state.value = _state.value.copy(
                date = date,
                zmanim = zmanim,
                locationName = location.name,
                otherClockCityName = otherClockCityName(location, deviceContext.now(prefs).zone),
                isLoading = false
            )
        }
    }
}

package ninja.doskey.app.levana.ui.zmanim

import ninja.doskey.app.levana.domain.model.ZmanTime
import java.time.LocalDate

data class ZmanimState(
    /** Null until the view model has loaded a date. */
    val date: LocalDate? = null,
    val zmanim: List<ZmanTime> = emptyList(),
    val locationName: String = "",
    /** Set when the city's clock differs from the device's; see [ninja.doskey.app.levana.ui.otherClockCityName]. */
    val otherClockCityName: String? = null,
    val isLoading: Boolean = true
)

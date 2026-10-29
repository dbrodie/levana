package ninja.doskey.app.levana.ui.zmanim

import ninja.doskey.app.levana.domain.model.ZmanTime
import java.time.LocalDate

data class ZmanimState(
    /** Null until the view model has loaded a date. */
    val date: LocalDate? = null,
    val zmanim: List<ZmanTime> = emptyList(),
    val locationName: String = "",
    val isLoading: Boolean = true
)

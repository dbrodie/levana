package ninja.doskey.app.levana.ui.zmanim

import ninja.doskey.app.levana.domain.model.ZmanTime
import java.time.LocalDate

data class ZmanimState(
    val date: LocalDate = LocalDate.now(),
    val zmanim: List<ZmanTime> = emptyList(),
    val locationName: String = "",
    val isLoading: Boolean = true
)

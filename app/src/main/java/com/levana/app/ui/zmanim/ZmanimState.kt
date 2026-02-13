package com.levana.app.ui.zmanim

import com.levana.app.domain.model.ZmanTime
import java.time.LocalDate

data class ZmanimState(
    val date: LocalDate = LocalDate.now(),
    val zmanim: List<ZmanTime> = emptyList(),
    val locationName: String = "",
    val isLoading: Boolean = true
)

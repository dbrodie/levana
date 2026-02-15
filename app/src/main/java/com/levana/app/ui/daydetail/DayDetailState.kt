package com.levana.app.ui.daydetail

import com.levana.app.data.db.PersonalEvent
import com.levana.app.domain.model.DayInfo

data class DayDetailState(
    val dayInfo: DayInfo? = null,
    val personalEvents: List<PersonalEvent> = emptyList(),
    val isLoading: Boolean = true
)

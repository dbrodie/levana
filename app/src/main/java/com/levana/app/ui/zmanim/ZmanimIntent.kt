package com.levana.app.ui.zmanim

import java.time.LocalDate

sealed interface ZmanimIntent {
    data class LoadDate(val date: LocalDate) : ZmanimIntent
    data object LoadDefault : ZmanimIntent
}

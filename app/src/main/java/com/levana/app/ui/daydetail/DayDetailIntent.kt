package com.levana.app.ui.daydetail

import java.time.LocalDate

sealed interface DayDetailIntent {
    data class LoadDay(val date: LocalDate) : DayDetailIntent
}

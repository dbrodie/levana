package ninja.doskey.app.levana.ui.daydetail

import java.time.LocalDate

sealed interface DayDetailIntent {
    data class LoadDay(val date: LocalDate) : DayDetailIntent
}

package ninja.doskey.app.levana.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

data class DayInfo(
    val hebrewDay: HebrewDay,
    val dayOfWeek: DayOfWeek,
    val gregorianDate: LocalDate,
    val holidays: List<Holiday>,
    val parsha: String?,
    val parshaHebrew: String? = null,
    val specialShabbat: String? = null,
    val molad: String? = null,
    val omerDay: Int?,
    val shabbatInfo: ShabbatInfo? = null
)

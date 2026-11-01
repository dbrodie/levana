package ninja.doskey.app.levana.domain.model

import java.time.DayOfWeek

/**
 * The molad of [month], as announced on its Shabbat Mevarchim: the traditional reckoning in
 * Jerusalem mean time (no summer time), so [hour] (0–23), [minute] and [chalakim] (0–17)
 * match published molad tables, e.g. Hebcal's "Molad Kislev: Monday, 10:27pm and 3 chalakim".
 */
data class Molad(
    val month: HebrewMonth,
    val dayOfWeek: DayOfWeek,
    val hour: Int,
    val minute: Int,
    val chalakim: Int
)

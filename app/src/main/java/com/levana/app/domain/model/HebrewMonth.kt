package com.levana.app.domain.model

import com.kosherjava.zmanim.hebrewcalendar.JewishDate

enum class HebrewMonth(val jewishDateValue: Int) {
    TISHREI(JewishDate.TISHREI),
    CHESHVAN(JewishDate.CHESHVAN),
    KISLEV(JewishDate.KISLEV),
    TEVES(JewishDate.TEVES),
    SHEVAT(JewishDate.SHEVAT),
    ADAR(JewishDate.ADAR),
    ADAR_I(JewishDate.ADAR),
    ADAR_II(JewishDate.ADAR_II),
    NISSAN(JewishDate.NISSAN),
    IYAR(JewishDate.IYAR),
    SIVAN(JewishDate.SIVAN),
    TAMMUZ(JewishDate.TAMMUZ),
    AV(JewishDate.AV),
    ELUL(JewishDate.ELUL);

    companion object {
        fun from(jewishDateMonth: Int, isLeapYear: Boolean): HebrewMonth = when (jewishDateMonth) {
            JewishDate.TISHREI -> TISHREI
            JewishDate.CHESHVAN -> CHESHVAN
            JewishDate.KISLEV -> KISLEV
            JewishDate.TEVES -> TEVES
            JewishDate.SHEVAT -> SHEVAT
            JewishDate.ADAR -> if (isLeapYear) ADAR_I else ADAR
            JewishDate.ADAR_II -> ADAR_II
            JewishDate.NISSAN -> NISSAN
            JewishDate.IYAR -> IYAR
            JewishDate.SIVAN -> SIVAN
            JewishDate.TAMMUZ -> TAMMUZ
            JewishDate.AV -> AV
            JewishDate.ELUL -> ELUL
            else -> throw IllegalArgumentException("Unknown Jewish month: $jewishDateMonth")
        }
    }
}

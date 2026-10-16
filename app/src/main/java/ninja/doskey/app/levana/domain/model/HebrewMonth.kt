package ninja.doskey.app.levana.domain.model

import com.kosherjava.zmanim.hebrewcalendar.JewishDate

/**
 * [englishName] is the single source of English Hebrew-month names for the
 * app's own UI. It matches KosherJava's transliterated formatter, which
 * produces the full English date strings (see HebrewMonthTest).
 */
enum class HebrewMonth(val jewishDateValue: Int, val englishName: String) {
    TISHREI(JewishDate.TISHREI, "Tishrei"),
    CHESHVAN(JewishDate.CHESHVAN, "Cheshvan"),
    KISLEV(JewishDate.KISLEV, "Kislev"),
    TEVES(JewishDate.TEVES, "Teves"),
    SHEVAT(JewishDate.SHEVAT, "Shevat"),
    ADAR(JewishDate.ADAR, "Adar"),
    ADAR_I(JewishDate.ADAR, "Adar I"),
    ADAR_II(JewishDate.ADAR_II, "Adar II"),
    NISSAN(JewishDate.NISSAN, "Nissan"),
    IYAR(JewishDate.IYAR, "Iyar"),
    SIVAN(JewishDate.SIVAN, "Sivan"),
    TAMMUZ(JewishDate.TAMMUZ, "Tammuz"),
    AV(JewishDate.AV, "Av"),
    ELUL(JewishDate.ELUL, "Elul");

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

        /** English name of a KosherJava month number: "Adar I" / "Adar II" in a leap year. */
        fun englishName(jewishDateMonth: Int, isLeapYear: Boolean): String =
            from(jewishDateMonth, isLeapYear).englishName
    }
}

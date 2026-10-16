package ninja.doskey.app.levana.domain.model

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate

/**
 * [englishName] is the single source of English Hebrew-month names for the
 * app's own UI. It matches KosherJava's transliterated formatter, which
 * produces the full English date strings (see HebrewMonthTest).
 *
 * [hebrewName] is the single source of Hebrew month names for the app's own
 * UI. It comes from KosherJava's Hebrew formatter, which produces the full
 * Hebrew date strings, so the spellings ("חשון", "סיון", "אדר א׳") always match.
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

    /** Hebrew name as KosherJava's Hebrew formatter spells it, e.g. "חשון", "אדר א׳". */
    val hebrewName: String
        get() {
            // Reference years for month names only: 5785 is an ordinary year, 5784 a leap year
            val referenceYear = if (this == ADAR_I || this == ADAR_II) 5784 else 5785
            return hebrewFormatter.formatMonth(JewishDate(referenceYear, jewishDateValue, 1))
        }

    companion object {
        private val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }

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

        /** Hebrew name of a KosherJava month number: "אדר א׳" / "אדר ב׳" in a leap year. */
        fun hebrewName(jewishDateMonth: Int, isLeapYear: Boolean): String =
            from(jewishDateMonth, isLeapYear).hebrewName
    }
}

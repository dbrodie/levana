package com.levana.app.data

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.levana.app.domain.model.Holiday
import com.levana.app.domain.model.HolidayCategory

object HolidayMapper {

    fun mapHoliday(yomTovIndex: Int): Holiday? = holidayMap[yomTovIndex]

    private fun torah(name: String, hebrew: String) = Holiday(name, hebrew, HolidayCategory.TORAH)

    private fun rabbinic(name: String, hebrew: String) =
        Holiday(name, hebrew, HolidayCategory.RABBINIC)

    private fun fast(name: String, hebrew: String) = Holiday(name, hebrew, HolidayCategory.FAST)

    private fun minor(name: String, hebrew: String) = Holiday(name, hebrew, HolidayCategory.MINOR)

    private fun modern(name: String, hebrew: String) =
        Holiday(name, hebrew, HolidayCategory.MODERN_ISRAELI)

    private val holidayMap = mapOf(
        JewishCalendar.EREV_PESACH to
            torah("Erev Pesach", "ערב פסח"),
        JewishCalendar.PESACH to
            torah("Pesach", "פסח"),
        JewishCalendar.CHOL_HAMOED_PESACH to
            torah("Chol HaMoed Pesach", "חול המועד פסח"),
        JewishCalendar.PESACH_SHENI to
            minor("Pesach Sheni", "פסח שני"),
        JewishCalendar.EREV_SHAVUOS to
            torah("Erev Shavuot", "ערב שבועות"),
        JewishCalendar.SHAVUOS to
            torah("Shavuot", "שבועות"),
        JewishCalendar.SEVENTEEN_OF_TAMMUZ to
            fast("17 of Tammuz", "שבעה עשר בתמוז"),
        JewishCalendar.TISHA_BEAV to
            fast("Tisha B'Av", "תשעה באב"),
        JewishCalendar.TU_BEAV to
            minor("Tu B'Av", "ט״ו באב"),
        JewishCalendar.EREV_ROSH_HASHANA to
            torah("Erev Rosh Hashanah", "ערב ראש השנה"),
        JewishCalendar.ROSH_HASHANA to
            torah("Rosh Hashanah", "ראש השנה"),
        JewishCalendar.FAST_OF_GEDALYAH to
            fast("Tzom Gedaliah", "צום גדליה"),
        JewishCalendar.EREV_YOM_KIPPUR to
            torah("Erev Yom Kippur", "ערב יום כיפור"),
        JewishCalendar.YOM_KIPPUR to
            torah("Yom Kippur", "יום כיפור"),
        JewishCalendar.EREV_SUCCOS to
            torah("Erev Sukkot", "ערב סוכות"),
        JewishCalendar.SUCCOS to
            torah("Sukkot", "סוכות"),
        JewishCalendar.CHOL_HAMOED_SUCCOS to
            torah("Chol HaMoed Sukkot", "חול המועד סוכות"),
        JewishCalendar.HOSHANA_RABBA to
            torah("Hoshana Rabba", "הושענא רבה"),
        JewishCalendar.SHEMINI_ATZERES to
            torah("Shemini Atzeret", "שמיני עצרת"),
        JewishCalendar.SIMCHAS_TORAH to
            torah("Simchat Torah", "שמחת תורה"),
        JewishCalendar.CHANUKAH to
            rabbinic("Chanukah", "חנוכה"),
        JewishCalendar.TENTH_OF_TEVES to
            fast("10 of Tevet", "עשרה בטבת"),
        JewishCalendar.TU_BESHVAT to
            minor("Tu B'Shevat", "ט״ו בשבט"),
        JewishCalendar.FAST_OF_ESTHER to
            fast("Ta'anit Esther", "תענית אסתר"),
        JewishCalendar.PURIM to
            rabbinic("Purim", "פורים"),
        JewishCalendar.SHUSHAN_PURIM to
            rabbinic("Shushan Purim", "שושן פורים"),
        JewishCalendar.PURIM_KATAN to
            minor("Purim Katan", "פורים קטן"),
        JewishCalendar.ROSH_CHODESH to
            minor("Rosh Chodesh", "ראש חודש"),
        JewishCalendar.YOM_HASHOAH to
            modern("Yom HaShoah", "יום השואה"),
        JewishCalendar.YOM_HAZIKARON to
            modern("Yom HaZikaron", "יום הזיכרון"),
        JewishCalendar.YOM_HAATZMAUT to
            modern("Yom HaAtzmaut", "יום העצמאות"),
        JewishCalendar.YOM_YERUSHALAYIM to
            modern("Yom Yerushalayim", "יום ירושלים"),
        JewishCalendar.LAG_BAOMER to
            minor("Lag B'Omer", "ל״ג בעומר"),
    )
}

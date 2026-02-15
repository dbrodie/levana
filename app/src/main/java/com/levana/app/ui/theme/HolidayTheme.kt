package com.levana.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.util.GregorianCalendar

enum class HolidayTheme(
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightTertiary: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkTertiary: Color
) {
    CHANUKAH(
        lightPrimary = Color(0xFF1565C0),
        lightSecondary = Color(0xFF90CAF9),
        lightTertiary = Color(0xFFC0C0C0),
        darkPrimary = Color(0xFF90CAF9),
        darkSecondary = Color(0xFF42A5F5),
        darkTertiary = Color(0xFFE0E0E0)
    ),
    SUKKOT(
        lightPrimary = Color(0xFF2E7D32),
        lightSecondary = Color(0xFF81C784),
        lightTertiary = Color(0xFF4CAF50),
        darkPrimary = Color(0xFF81C784),
        darkSecondary = Color(0xFF66BB6A),
        darkTertiary = Color(0xFFA5D6A7)
    ),
    PESACH(
        lightPrimary = Color(0xFFBF360C),
        lightSecondary = Color(0xFFFF8A65),
        lightTertiary = Color(0xFFE64A19),
        darkPrimary = Color(0xFFFF8A65),
        darkSecondary = Color(0xFFFF7043),
        darkTertiary = Color(0xFFFFAB91)
    ),
    PURIM(
        lightPrimary = Color(0xFF7B1FA2),
        lightSecondary = Color(0xFFCE93D8),
        lightTertiary = Color(0xFF9C27B0),
        darkPrimary = Color(0xFFCE93D8),
        darkSecondary = Color(0xFFBA68C8),
        darkTertiary = Color(0xFFE1BEE7)
    ),
    ROSH_HASHANAH(
        lightPrimary = Color(0xFFF9A825),
        lightSecondary = Color(0xFFFFF176),
        lightTertiary = Color(0xFFFBC02D),
        darkPrimary = Color(0xFFFFF176),
        darkSecondary = Color(0xFFFFEE58),
        darkTertiary = Color(0xFFFFF59D)
    ),
    YOM_KIPPUR(
        lightPrimary = Color(0xFF795548),
        lightSecondary = Color(0xFFBCAAA4),
        lightTertiary = Color(0xFF8D6E63),
        darkPrimary = Color(0xFFBCAAA4),
        darkSecondary = Color(0xFFA1887F),
        darkTertiary = Color(0xFFD7CCC8)
    ),
    SHAVUOT(
        lightPrimary = Color(0xFF558B2F),
        lightSecondary = Color(0xFFAED581),
        lightTertiary = Color(0xFF689F38),
        darkPrimary = Color(0xFFAED581),
        darkSecondary = Color(0xFF9CCC65),
        darkTertiary = Color(0xFFC5E1A5)
    ),
    FAST_DAY(
        lightPrimary = Color(0xFF616161),
        lightSecondary = Color(0xFFBDBDBD),
        lightTertiary = Color(0xFF757575),
        darkPrimary = Color(0xFFBDBDBD),
        darkSecondary = Color(0xFF9E9E9E),
        darkTertiary = Color(0xFFE0E0E0)
    ),
    SHMINI_ATZERET(
        lightPrimary = Color(0xFF4527A0),
        lightSecondary = Color(0xFFB39DDB),
        lightTertiary = Color(0xFF5E35B1),
        darkPrimary = Color(0xFFB39DDB),
        darkSecondary = Color(0xFF9575CD),
        darkTertiary = Color(0xFFD1C4E9)
    );

    fun lightScheme(): ColorScheme = lightColorScheme(
        primary = lightPrimary,
        secondary = lightSecondary,
        tertiary = lightTertiary
    )

    fun darkScheme(): ColorScheme = darkColorScheme(
        primary = darkPrimary,
        secondary = darkSecondary,
        tertiary = darkTertiary
    )
}

object HolidayThemeResolver {

    fun resolve(date: LocalDate = LocalDate.now()): HolidayTheme? {
        val gc = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        val jc = JewishCalendar(gc)
        return resolveFromCalendar(jc)
    }

    private fun resolveFromCalendar(jc: JewishCalendar): HolidayTheme? {
        val index = jc.yomTovIndex

        return when (index) {
            // Chanukah
            JewishCalendar.CHANUKAH -> HolidayTheme.CHANUKAH

            // Sukkot
            JewishCalendar.EREV_SUCCOS,
            JewishCalendar.SUCCOS,
            JewishCalendar.CHOL_HAMOED_SUCCOS,
            JewishCalendar.HOSHANA_RABBA -> HolidayTheme.SUKKOT

            // Shemini Atzeret / Simchat Torah
            JewishCalendar.SHEMINI_ATZERES,
            JewishCalendar.SIMCHAS_TORAH -> HolidayTheme.SHMINI_ATZERET

            // Pesach
            JewishCalendar.EREV_PESACH,
            JewishCalendar.PESACH,
            JewishCalendar.CHOL_HAMOED_PESACH -> HolidayTheme.PESACH

            // Purim
            JewishCalendar.PURIM,
            JewishCalendar.SHUSHAN_PURIM -> HolidayTheme.PURIM

            // Rosh Hashanah
            JewishCalendar.EREV_ROSH_HASHANA,
            JewishCalendar.ROSH_HASHANA -> HolidayTheme.ROSH_HASHANAH

            // Yom Kippur
            JewishCalendar.EREV_YOM_KIPPUR,
            JewishCalendar.YOM_KIPPUR -> HolidayTheme.YOM_KIPPUR

            // Shavuot
            JewishCalendar.EREV_SHAVUOS,
            JewishCalendar.SHAVUOS -> HolidayTheme.SHAVUOT

            // Fast days
            JewishCalendar.SEVENTEEN_OF_TAMMUZ,
            JewishCalendar.TISHA_BEAV,
            JewishCalendar.FAST_OF_GEDALYAH,
            JewishCalendar.TENTH_OF_TEVES,
            JewishCalendar.FAST_OF_ESTHER -> HolidayTheme.FAST_DAY

            else -> null
        }
    }
}

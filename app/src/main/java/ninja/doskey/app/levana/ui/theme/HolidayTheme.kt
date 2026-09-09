package ninja.doskey.app.levana.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.util.GregorianCalendar

/** Holiday accents adapt the Claude Design tone pairs; surfaces remain neutral. */
enum class HolidayTheme(
    val lightPrimary: Color,
    val lightContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val accentInk: Color,
) {
    CHANUKAH(Color(0xFF2C4B8F), Color(0xFFDCE3FF), Color(0xFFB3C5FF), Color(0xFF0F3170), Color(0xFF001A43)),
    SUKKOT(Color(0xFF4F6B12), Color(0xFFD5EFA0), Color(0xFFB8D873), Color(0xFF384E00), Color(0xFF182500)),
    PESACH(Color(0xFF2C4B8F), Color(0xFFDCE3FF), Color(0xFFB3C5FF), Color(0xFF0F3170), Color(0xFF001A43)),
    PURIM(Color(0xFF8E3E86), Color(0xFFFFD7F4), Color(0xFFFFACE9), Color(0xFF722B6C), Color(0xFF380032)),
    ROSH_HASHANAH(Color(0xFF8A5B00), Color(0xFFFFDEA6), Color(0xFFF5BE48), Color(0xFF664000), Color(0xFF2B1B00)),
    YOM_KIPPUR(Color(0xFF705C50), Color(0xFFF8DECE), Color(0xFFDEC0AE), Color(0xFF574236), Color(0xFF29180F)),
    SHAVUOT(Color(0xFF4F6B12), Color(0xFFD5EFA0), Color(0xFFB8D873), Color(0xFF384E00), Color(0xFF182500)),
    FAST_DAY(Color(0xFF5B6065), Color(0xFFE0E3E7), Color(0xFFC3C7CC), Color(0xFF43474C), Color(0xFF1B1C1E)),
    SHMINI_ATZERET(Color(0xFF64518C), Color(0xFFEBDCFF), Color(0xFFCFBCF8), Color(0xFF4C396F), Color(0xFF211039));

    fun lightScheme(): ColorScheme = LightColorScheme.copy(
        primary = lightPrimary,
        onPrimary = Color.White,
        primaryContainer = lightContainer,
        onPrimaryContainer = accentInk,
        inversePrimary = darkPrimary,
        surfaceTint = lightPrimary,
    )

    fun darkScheme(): ColorScheme = DarkColorScheme.copy(
        primary = darkPrimary,
        onPrimary = accentInk,
        primaryContainer = darkContainer,
        onPrimaryContainer = lightContainer,
        inversePrimary = lightPrimary,
        surfaceTint = darkPrimary,
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

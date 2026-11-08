package ninja.doskey.app.levana.data

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.HebrewDateText
import ninja.doskey.app.levana.domain.model.ParashaNames
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class TerminologyTest {
    @Test
    fun oldSelectionsKeepAllTimesAndUnknownKeys() {
        val old = setOf("Sunrise", "Sunset", "Nightfall", "Candle Lighting", "Chatzot", "Sof Zman Tefillah", "future-time")
        assertEquals(
            setOf("Netz hachama", "Shkia", "Tzeit hakochavim", "Candle lighting", "Chatzot hayom", "Sof zman tefillah (GRA)", "future-time"),
            parseSelectedZmanim(old)
        )
        assertEquals(emptySet<String>(), parseSelectedZmanim(emptySet()))
        ZmanimRepository.RENAMED_ZMAN_KEYS.forEach { (old, current) ->
            assertTrue(current in ZmanimRepository.ZMAN_NAMES.map { it.english })
            assertEquals(setOf(current), parseSelectedZmanim(setOf(old)))
            assertEquals(setOf(current), parseSelectedZmanim(parseSelectedZmanim(setOf(old))))
        }
    }

    @Test
    fun calendarNamesUseOneSchemeIncludingCombinedReadings() {
        assertEquals(JewishCalendar.Parsha.entries.toSet(), ParashaNames.english.keys)
        assertEquals("Bereshit", ParashaNames.english[JewishCalendar.Parsha.BERESHIS])
        assertEquals("Achrei Mot Kedoshim", ParashaNames.english[JewishCalendar.Parsha.ACHREI_MOS_KEDOSHIM])
        assertEquals("1 Nisan 5787", HebrewDateText.transliteratedDate(JewishDate(5787, JewishDate.NISSAN, 1)))
        assertEquals("1 Tevet 5787", HebrewDateText.transliteratedDate(JewishDate(5787, JewishDate.TEVES, 1)))
    }

    @Test
    fun bundledPlacesTranslateWithoutChangingStoredIdentityAndSearchBothLanguages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("he")) }
        val hebrew = context.createConfigurationContext(config)
        val repo = CityRepository(context)
        repo.getAllCities().forEach { city ->
            assertNotEquals(city.name, PlaceNames.display(hebrew, city.name))
            assertNotEquals(city.country, PlaceNames.display(hebrew, city.country))
        }
        assertEquals("Jerusalem", repo.search("ירושלים").single().name)
        assertEquals(repo.search("Jerusalem"), repo.search("ירושלים"))
        assertTrue(repo.search("ארצות הברית").all { it.country == "USA" })
        assertEquals("My custom place", PlaceNames.display(hebrew, "My custom place"))
    }
}

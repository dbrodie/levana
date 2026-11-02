package ninja.doskey.app.levana.ui

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.Molad
import ninja.doskey.app.levana.notifications.withAppLocale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.DayOfWeek

/** The molad text in the app language and both hour formats (TASK-57.6). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MoladTextTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private val localeManager get() = app.getSystemService(LocaleManager::class.java)

    private fun language(tag: String): Context {
        localeManager.applicationLocales = LocaleList.forLanguageTags(tag)
        return app.withAppLocale()
    }

    @After
    fun resetAppLanguage() {
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
    }

    private val kislev = Molad(HebrewMonth.KISLEV, DayOfWeek.MONDAY, 22, 27, 3)
    private val oneChelek = Molad(HebrewMonth.TAMMUZ, DayOfWeek.SUNDAY, 4, 19, 1)
    private val noChalakim = Molad(HebrewMonth.NISSAN, DayOfWeek.TUESDAY, 14, 7, 0)

    @Test
    fun english() {
        val en = language("en-US")
        assertEquals("Monday 10:27 PM and 3 chalakim", moladTimeText(en, kislev, use24Hour = false))
        assertEquals("Monday 22:27 and 3 chalakim", moladTimeText(en, kislev, use24Hour = true))
        assertEquals("Sunday 04:19 and 1 chelek", moladTimeText(en, oneChelek, use24Hour = true))
        assertEquals("Tuesday 2:07 PM", moladTimeText(en, noChalakim, use24Hour = false))
        assertEquals(
            "Mevarchim Kislev · Molad Monday 10:27 PM and 3 chalakim",
            agendaMevarchimText(en, kislev, use24Hour = false)
        )
    }

    @Test
    fun hebrew() {
        val he = language("iw")
        assertEquals("יום שני \u206822:27\u2069 ו־3 חלקים", moladTimeText(he, kislev, use24Hour = true))
        assertEquals("יום שני \u206810:27 PM\u2069 ו־3 חלקים", moladTimeText(he, kislev, use24Hour = false))
        assertEquals("יום ראשון \u206804:19\u2069 וחלק אחד", moladTimeText(he, oneChelek, use24Hour = true))
        assertEquals("יום שלישי \u206814:07\u2069", moladTimeText(he, noChalakim, use24Hour = true))
        assertEquals(
            "מברכים כסלו · מולד יום שני \u206822:27\u2069 ו־3 חלקים",
            agendaMevarchimText(he, kislev, use24Hour = true)
        )
    }
}

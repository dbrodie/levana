package ninja.doskey.app.levana.notifications

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class BirthdayNotificationTextTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val localeManager: LocaleManager
        get() = context.getSystemService(LocaleManager::class.java)

    private val delayed = OccurrenceLabel.DelayedFrom30(HebrewMonth.CHESHVAN)

    @After
    fun resetAppLanguage() {
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
    }

    @Test
    fun englishUnchanged() {
        val plain = birthdayNotificationText(context, "Dani", null)
        assertEquals("Personal Event", plain.title)
        assertEquals("Today is Dani's Hebrew Birthday", plain.body)

        val labelled = birthdayNotificationText(context, "Dani", delayed)
        assertEquals("Personal Event", labelled.title)
        assertEquals("Today is Dani's Hebrew Birthday (delayed from 30 Cheshvan)", labelled.body)
        assertEquals("Dani's Hebrew Birthday (delayed from 30 Cheshvan)", labelled.eventTitle)
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewDevice() {
        val plain = birthdayNotificationText(context, "דני", null)
        assertEquals("אירוע אישי", plain.title)
        assertEquals("היום יום ההולדת העברי של דני", plain.body)

        val labelled = birthdayNotificationText(context, "דני", delayed)
        assertEquals("היום יום ההולדת העברי של דני (נדחה מל׳ חשון)", labelled.body)
    }

    @Test
    fun hebrewAppLanguageOnEnglishDevice() {
        localeManager.applicationLocales = LocaleList.forLanguageTags("iw")

        val text = birthdayNotificationText(context.withAppLocale(), "Dani", delayed)
        assertEquals("אירוע אישי", text.title)
        assertEquals("היום יום ההולדת העברי של Dani (נדחה מל׳ חשון)", text.body)

        val adar = birthdayNotificationText(context.withAppLocale(), "Dani", OccurrenceLabel.ADAR_II)
        assertEquals("היום יום ההולדת העברי של Dani (אדר ב׳)", adar.body)
    }

    @Test
    fun systemDefaultKeepsDeviceLanguage() {
        assertSame(context, context.withAppLocale())
        val text = birthdayNotificationText(context.withAppLocale(), "Dani", delayed)
        assertEquals("Today is Dani's Hebrew Birthday (delayed from 30 Cheshvan)", text.body)
    }

    @Test
    @Config(qualifiers = "iw")
    fun systemDefaultOnHebrewDevice() {
        val text = birthdayNotificationText(context.withAppLocale(), "דני", null)
        assertEquals("היום יום ההולדת העברי של דני", text.body)
    }
}

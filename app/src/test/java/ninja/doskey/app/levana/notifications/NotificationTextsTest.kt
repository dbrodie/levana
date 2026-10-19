package ninja.doskey.app.levana.notifications

import android.app.LocaleManager
import android.app.NotificationManager
import android.content.Context
import android.os.LocaleList
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.data.HolidayMapper
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.ui.events.occurrenceTitle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class NotificationTextsTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()

    private val localeManager: LocaleManager
        get() = app.getSystemService(LocaleManager::class.java)

    private val rosh: Holiday = HolidayMapper.mapHoliday(JewishCalendar.ROSH_HASHANA)!!
    private val gedaliah: Holiday = HolidayMapper.mapHoliday(JewishCalendar.FAST_OF_GEDALYAH)!!

    private val candles = LocalTime.of(18, 6)
    private val havdalah = LocalTime.of(19, 12)

    /** Friday 2026-09-11, Erev Rosh Hashanah; Saturday is Shabbat. */
    private val friday = LocalDate.of(2026, 9, 11)
    private val shabbatEnd = ObservanceEnd(friday.plusDays(1), havdalah, includesYomTov = false)
    private val yomTovEndNextDay = ObservanceEnd(friday.plusDays(1), havdalah, includesYomTov = true)
    private val yomTovEndSunday = ObservanceEnd(friday.plusDays(2), havdalah, includesYomTov = true)

    /** English device, App language = System default. */
    private fun english(): Context = app.withAppLocale()

    /** English device, App language = Hebrew. */
    private fun hebrew(): Context {
        localeManager.applicationLocales = LocaleList.forLanguageTags("iw")
        return app.withAppLocale()
    }

    @After
    fun resetAppLanguage() {
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
    }

    @Test
    fun timesEnglish12HourHebrew24Hour() {
        assertEquals("6:06 PM", notificationTime(english(), candles))
        assertEquals("9:05 AM", notificationTime(english(), LocalTime.of(9, 5)))
        assertEquals("18:06", notificationTime(hebrew(), candles))
        assertEquals("09:05", notificationTime(hebrew(), LocalTime.of(9, 5)))
    }

    @Test
    fun candleLightingEnglish() {
        val withEnd = candleLightingNotificationText(english(), friday, candles, shabbatEnd)
        assertEquals("Candle Lighting", withEnd.title)
        assertEquals("Candle Lighting at 6:06 PM · Shabbat ends 7:12 PM", withEnd.body)
        assertEquals(
            "Candle Lighting at 6:06 PM",
            candleLightingNotificationText(english(), friday, candles, null).body
        )
        assertEquals(
            "Candle Lighting at 6:06 PM · Yom Tov ends 7:12 PM",
            candleLightingNotificationText(english(), friday, candles, yomTovEndNextDay).body
        )
        assertEquals(
            "Candle Lighting at 6:06 PM · Yom Tov ends Sunday 7:12 PM",
            candleLightingNotificationText(english(), friday, candles, yomTovEndSunday).body
        )
    }

    @Test
    fun candleLightingHebrew() {
        val withEnd = candleLightingNotificationText(hebrew(), friday, candles, shabbatEnd)
        assertEquals("הדלקת נרות", withEnd.title)
        assertEquals("הדלקת נרות בשעה 18:06 · צאת השבת 19:12", withEnd.body)
        assertEquals(
            "הדלקת נרות בשעה 18:06",
            candleLightingNotificationText(hebrew(), friday, candles, null).body
        )
        assertEquals(
            "הדלקת נרות בשעה 18:06 · צאת החג 19:12",
            candleLightingNotificationText(hebrew(), friday, candles, yomTovEndNextDay).body
        )
    }

    @Test
    fun holidayEnglish() {
        val context = english()
        assertEquals("Holiday", holidayNotificationText(context, rosh, 0).title)
        assertEquals("Today is Rosh Hashanah", holidayNotificationText(context, rosh, 0).body)
        assertEquals("Rosh Hashanah begins tomorrow", holidayNotificationText(context, rosh, 1).body)
        assertEquals("Rosh Hashanah begins in 2 days", holidayNotificationText(context, rosh, 2).body)
        assertEquals("Rosh Hashanah begins in 3 days", holidayNotificationText(context, rosh, 3).body)
    }

    @Test
    fun holidayHebrewUsesHebrewNameAndPlurals() {
        val context = hebrew()
        assertEquals("חג", holidayNotificationText(context, rosh, 0).title)
        assertEquals("היום ראש השנה", holidayNotificationText(context, rosh, 0).body)
        assertEquals("מחר: ראש השנה", holidayNotificationText(context, rosh, 1).body)
        assertEquals("בעוד יומיים: ראש השנה", holidayNotificationText(context, rosh, 2).body)
        assertEquals("בעוד 3 ימים: ראש השנה", holidayNotificationText(context, rosh, 3).body)
        assertEquals("בעוד 7 ימים: ראש השנה", holidayNotificationText(context, rosh, 7).body)
    }

    @Test
    fun fastEnglish() {
        val text = fastNotificationText(english(), gedaliah, LocalTime.of(5, 2), havdalah)
        assertEquals("Fast Day", text.title)
        assertEquals("Tzom Gedaliah · Begins 5:02 AM · Ends 7:12 PM", text.body)
        assertEquals("Tzom Gedaliah", fastNotificationText(english(), gedaliah, null, null).body)
    }

    @Test
    fun fastHebrew() {
        val text = fastNotificationText(hebrew(), gedaliah, LocalTime.of(5, 2), havdalah)
        assertEquals("יום צום", text.title)
        assertEquals("צום גדליה · התחלה 05:02 · סיום 19:12", text.body)
        assertEquals("צום גדליה · סיום 19:12", fastNotificationText(hebrew(), gedaliah, null, havdalah).body)
    }

    @Test
    fun personalEvent() {
        val delayed = OccurrenceLabel.DelayedFrom30(HebrewMonth.CHESHVAN)
        val en = english()
        val enText = personalEventNotificationText(en, occurrenceTitle(en, "Anniversary", delayed))
        assertEquals("Personal Event", enText.title)
        assertEquals("Today is Anniversary (delayed from 30 Cheshvan)", enText.body)

        val iw = hebrew()
        val iwText = personalEventNotificationText(iw, occurrenceTitle(iw, "יום נישואין", delayed))
        assertEquals("אירוע אישי", iwText.title)
        assertEquals("היום יום נישואין (נדחה מל׳ חשון)", iwText.body)
    }

    @Test
    fun omerEnglish() {
        val context = english()
        assertEquals("Sefirat HaOmer", omerNotificationText(context, 5, tonight = true).title)
        assertEquals("Tonight is day 5 of the Omer", omerNotificationText(context, 5, true).body)
        assertEquals("Tonight is day 7 of the Omer (1 week)", omerNotificationText(context, 7, true).body)
        assertEquals(
            "Tonight is day 8 of the Omer (1 week and 1 day)",
            omerNotificationText(context, 8, true).body
        )
        assertEquals(
            "Tonight is day 33 of the Omer (4 weeks and 5 days)",
            omerNotificationText(context, 33, true).body
        )
        assertEquals(
            "Today is day 14 of the Omer (2 weeks)",
            omerNotificationText(context, 14, tonight = false).body
        )
    }

    @Test
    fun omerHebrewUsesKosherJavaCount() {
        val context = hebrew()
        assertEquals("ספירת העומר", omerNotificationText(context, 5, tonight = true).title)
        assertEquals("הלילה ה׳ בעומר", omerNotificationText(context, 5, true).body)
        assertEquals("הלילה ז׳ בעומר (שבוע אחד)", omerNotificationText(context, 7, true).body)
        assertEquals("הלילה ח׳ בעומר (שבוע אחד ויום אחד)", omerNotificationText(context, 8, true).body)
        assertEquals("הלילה ט׳ בעומר (שבוע אחד ושני ימים)", omerNotificationText(context, 9, true).body)
        assertEquals("היום י״ד בעומר (שני שבועות)", omerNotificationText(context, 14, false).body)
        assertEquals("הלילה ל״ג בעומר (4 שבועות ו־5 ימים)", omerNotificationText(context, 33, true).body)
        assertEquals("הלילה מ״ה בעומר (6 שבועות ו־3 ימים)", omerNotificationText(context, 45, true).body)
        assertEquals("הלילה מ״ט בעומר (7 שבועות)", omerNotificationText(context, 49, true).body)
    }

    @Test
    fun updateNotice() {
        val en = updateAvailableNotificationText(english(), "1.4.0")
        assertEquals("Update Available", en.title)
        assertEquals("Levana v1.4.0 is available — tap to download", en.body)

        val iw = updateAvailableNotificationText(hebrew(), "1.4.0")
        assertEquals("עדכון זמין", iw.title)
        assertEquals("גרסה 1.4.0 של לבנה זמינה — יש להקיש להורדה", iw.body)
    }

    @Test
    fun channelsFollowAppLanguage() {
        val manager = app.getSystemService(NotificationManager::class.java)

        NotificationChannels.createAll(app)
        val en = manager.getNotificationChannel(NotificationChannels.CANDLE_LIGHTING)
        assertEquals("Candle Lighting", en.name)
        assertEquals("Candle lighting time reminders", en.description)
        assertEquals(
            "App Updates",
            manager.getNotificationChannel(NotificationChannels.UPDATE_AVAILABLE).name
        )

        localeManager.applicationLocales = LocaleList.forLanguageTags("iw")
        NotificationChannels.createAll(app)
        val iw = manager.getNotificationChannel(NotificationChannels.CANDLE_LIGHTING)
        assertEquals("הדלקת נרות", iw.name)
        assertEquals("תזכורות לזמן הדלקת הנרות", iw.description)
        assertEquals("ספירת העומר", manager.getNotificationChannel(NotificationChannels.OMER).name)
        assertEquals(6, manager.notificationChannels.size)
    }
}

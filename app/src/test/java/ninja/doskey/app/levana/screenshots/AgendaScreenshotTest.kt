package ninja.doskey.app.levana.screenshots

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.ContactBirthday
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The agenda (TASK-8) on a fixed range around [TODAY] (Tuesday 7 Kislev 5787) with every kind
 * of entry: a personal event, a contact birthday, a yahrzeit, candle lighting at the selected
 * city (Jerusalem, so its clock is labelled), device events, Shabbat with havdalah, and later
 * Rosh Chodesh and Chanukah.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class AgendaScreenshotTest(private val variant: Variant) : ScreenshotTest(variant) {

    private val hebrew = variant == Variant.HE_LIGHT || variant == Variant.HE_DARK

    override fun testModules(): List<Module> = listOf(
        module {
            single<ContactBirthdayRepository> {
                object : ContactBirthdayRepository(androidContext()) {
                    override suspend fun getAll() = listOf(
                        ContactBirthday("sarah", "Sarah Cohen", null, 8, JewishDate.KISLEV, 5750)
                    )
                }
            }
        }
    )

    override suspend fun seedPreferences(preferences: PreferencesRepository) {
        super.seedPreferences(preferences)
        preferences.saveCalendarHebrewMode(true)
        preferences.saveSelectedCalendarIds(setOf(CAL_ID))
    }

    @Before
    fun seedAgenda() {
        runBlocking {
            db.personalEventDao().insert(
                PersonalEvent(title = "Wedding anniversary", hebrewDay = 7, hebrewMonth = JewishDate.KISLEV, hebrewYear = 5770, notes = "Dinner at Rimon")
            )
            db.personalEventDao().insert(
                PersonalEvent(title = "Moshe ben Avraham", hebrewDay = 9, hebrewMonth = JewishDate.KISLEV, hebrewYear = 5760, useYahrzeitRules = true)
            )
        }
        Robolectric.setupContentProvider(ScreenshotCalendarProvider::class.java, CalendarContract.AUTHORITY)
    }

    private fun launchAgenda() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.nav_agenda)).performClick()
        awaitText("Moshe ben Avraham", partial = true)
    }

    /** Cards carry one TalkBack description in place of their texts. */
    private fun shows(text: String) = hasText(text, substring = true) or hasContentDescription(text, substring = true)

    private fun awaitText(text: String, partial: Boolean) = await(shows(text) and onScreen())

    @Test
    fun agenda() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        launchAgenda()
        awaitText("Shabbat dinner", partial = true)
        capture("app_agenda")

        // The leading calendar regroups the same days
        clickable(string(R.string.calendar_mode_gregorian)).performClick()
        awaitText("November 2026".takeUnless { hebrew } ?: "נובמבר 2026", partial = true)
        capture("app_agenda_gregorian")
        clickable(string(R.string.calendar_mode_hebrew)).performClick()
        // The mode is a saved preference: wait for it to come back before scrolling, so the
        // scroll never races the regrouping
        awaitText(if (hebrew) "כסלו תשפ״ז" else "כסלו 5787", partial = false)

        // A new month in the leading calendar starts under its own header: 30 Kislev (Rosh
        // Chodesh Teves) at the top, by its list key so the position is exact
        val kislev30 = JewishDate(5787, JewishDate.KISLEV, 30).localDate
        compose.onAllNodes(hasScrollToKeyAction())[0].performScrollToKey("day-${kislev30.toEpochDay()}")
        awaitText(if (hebrew) "ראש חודש טבת" else "Rosh Chodesh Teves", partial = true)
        capture("app_agenda_month_header")

        openDrawer()
        capture("app_agenda_drawer")
    }

    @Test
    fun withoutCalendarPermission() {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)
        launchAgenda()
        awaitText(string(R.string.agenda_calendar_choose))
        capture("app_agenda_no_calendar")
    }

    @Test
    fun search() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        launchAgenda()
        compose.onNode(hasContentDescription(string(R.string.agenda_search))).performClick()
        await(hasSetTextAction())
        // The parasha Chayei Sara and Sarah's birthdays in the twelve months either side
        compose.onNode(hasSetTextAction()).performTextInput("sara")
        awaitText("Sarah Cohen", partial = true)
        capture("app_agenda_search")

        compose.onNode(hasSetTextAction()).performTextReplacement("bar mitzvah in Haifa")
        awaitText(string(R.string.agenda_search_empty, "bar mitzvah in Haifa"))
        capture("app_agenda_search_empty")
    }

    /** One calendar: Friday's dinner and Shabbat's kiddush, New York time. */
    class ScreenshotCalendarProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val columns = projection!!
            return MatrixCursor(columns).apply {
                listOf(
                    event(1, "Shabbat dinner at the Levis", "Rechavia", TODAY.plusDays(3), 19, 0),
                    event(2, "Kiddush — Cohen bar mitzvah", "Yeshurun Synagogue", TODAY.plusDays(4), 11, 30),
                ).forEach { values -> addRow(columns.map { values[it] }) }
            }
        }

        private fun event(id: Long, title: String, place: String, day: LocalDate, hour: Int, minute: Int): Map<String, Any> {
            val start = day.atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli()
            return mapOf(
                CalendarContract.Instances.EVENT_ID to id,
                CalendarContract.Instances.TITLE to title,
                CalendarContract.Instances.BEGIN to start,
                CalendarContract.Instances.END to start + 2 * 3_600_000L,
                CalendarContract.Instances.ALL_DAY to 0,
                CalendarContract.Instances.CALENDAR_ID to CAL_ID,
                CalendarContract.Instances.CALENDAR_COLOR to 0xFF3F6FB5.toInt(),
                CalendarContract.Instances.EVENT_LOCATION to place
            )
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }

    companion object {
        private const val CAL_ID = 4L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}

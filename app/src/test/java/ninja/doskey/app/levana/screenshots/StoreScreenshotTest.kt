package ninja.doskey.app.levana.screenshots

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.notifications.NotificationPoster
import org.junit.Assume.assumeTrue
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
 * The Play and F-Droid phone screenshots (TASK-55; what each shows is in specs/STORE_LISTING.md).
 * Opt-in: they render only with `-Plevana.store.shots=all` (or e.g. `=2,5`) and write
 * 1080×1920 RGB PNGs straight into the Fastlane folder, so they never take part in the
 * baseline verify run. Regenerate with `scripts/store-screenshots.sh`; nothing under fastlane/
 * is committed without the owner's approval.
 *
 * The device is in Jerusalem (time zone and current location) on [TODAY], Tuesday 7 Kislev
 * 5787, with New York and London saved, a yahrzeit, an anniversary, a contact's Hebrew birthday
 * and two phone-calendar events.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = StoreScreenshotTest.STORE_DEVICE)
class StoreScreenshotTest(private val shot: Shot) : ScreenshotTest(shot.variant) {

    /** Each shot in store order; [number] is its position and file-name prefix. */
    enum class Shot(val number: Int, val slug: String, val variant: Variant) {
        HEBREW_MONTHS(1, "hebrew-months", Variant.EN_LIGHT),
        GREGORIAN_MONTH(2, "gregorian-month", Variant.EN_LIGHT),
        HEBREW_INTERFACE(3, "hebrew-interface", Variant.HE_LIGHT),
        AGENDA(4, "agenda", Variant.EN_LIGHT),
        DAY_SHEET_EREV_SHABBAT(5, "day-sheet-erev-shabbat", Variant.EN_LIGHT),
        ZMANIM(6, "zmanim", Variant.EN_LIGHT),
        EVENT_DATE_PICKER(7, "event-date-picker", Variant.EN_LIGHT),
        CITIES(8, "cities", Variant.EN_LIGHT);

        val fileName = "${number}_$slug.png"
    }

    private val hebrew = shot.variant == Variant.HE_LIGHT || shot.variant == Variant.HE_DARK

    override val zone: ZoneId = JERUSALEM_ZONE

    override fun testModules(): List<Module> = listOf(
        module {
            single<ContactBirthdayRepository> {
                object : ContactBirthdayRepository(androidContext()) {
                    override suspend fun getAll() = listOf(
                        ContactBirthday("sarah", if (hebrew) "שרה כהן" else "Sarah Cohen", null, 8, JewishDate.KISLEV, 5750)
                    )
                }
            }
        }
    )

    override suspend fun seedPreferences(preferences: PreferencesRepository) {
        preferences.updateCurrentLocation(Location.JERUSALEM)
        preferences.addSavedLocation(NEW_YORK)
        preferences.addSavedLocation(LONDON)
        preferences.setActiveLocation(null)
        preferences.saveCalendarHebrewMode(shot == Shot.HEBREW_MONTHS || shot == Shot.HEBREW_INTERFACE || shot == Shot.AGENDA)
        preferences.saveSelectedCalendarIds(setOf(CAL_ID))
    }

    @Before
    fun seedEvents() {
        assumeTrue("store screenshots are opt-in (-Plevana.store.shots)", selected(shot))
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        runBlocking {
            db.personalEventDao().insert(
                PersonalEvent(
                    title = if (hebrew) "יום נישואין" else "Wedding anniversary",
                    hebrewDay = 7, hebrewMonth = JewishDate.KISLEV, hebrewYear = 5770,
                )
            )
            db.personalEventDao().insert(
                PersonalEvent(
                    title = if (hebrew) "משה בן אברהם" else "Moshe ben Avraham",
                    hebrewDay = 9, hebrewMonth = JewishDate.KISLEV, hebrewYear = 5760, useYahrzeitRules = true,
                )
            )
        }
        Robolectric.setupContentProvider(StoreCalendarProvider::class.java, CalendarContract.AUTHORITY)
    }

    @Test
    fun store() {
        when (shot) {
            Shot.HEBREW_MONTHS, Shot.GREGORIAN_MONTH, Shot.HEBREW_INTERFACE -> launchCalendar()
            Shot.AGENDA -> {
                launchCalendar()
                openDrawer()
                clickable(string(R.string.nav_agenda)).performClick()
                await(hasText("Moshe ben Avraham", substring = true) or hasContentDescription("Moshe ben Avraham", substring = true))
                await(hasText("Shabbat dinner", substring = true) or hasContentDescription("Shabbat dinner", substring = true))
            }
            Shot.DAY_SHEET_EREV_SHABBAT -> {
                launchCalendar()
                compose.onAllNodes(day(FRIDAY))[0].performClick()
                compose.onNodeWithContentDescription(string(R.string.sheet_expand)).performClick()
                await(hasContentDescription(string(R.string.sheet_collapse)))
            }
            Shot.ZMANIM -> launch(
                hasText(string(R.string.nav_zmanim)),
                Intent(app, MainActivity::class.java)
                    .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, FRIDAY.toEpochDay())
                    .putExtra(MainActivity.EXTRA_OPEN_ZMANIM, true)
            )
            Shot.EVENT_DATE_PICKER -> {
                launchCalendar()
                openDrawer()
                clickable(string(R.string.design_my_events)).performClick()
                clickable(string(R.string.events_tab_events)).performClick()
                clickable("Moshe ben Avraham").performClick()
                awaitText(string(R.string.design_edit_event))
                clickable(string(R.string.event_hebrew_date_label)).performClick()
                await(hasText(string(R.string.date_picker_title)))
            }
            Shot.CITIES -> {
                launchCalendar()
                openDrawer()
                compose.onNodeWithContentDescription(string(R.string.design_switch_city)).performClick()
                awaitText("London")
            }
        }
        val path = "$STORE_DIR/${shot.fileName}"
        captureFile(path)
        if (System.getProperty("roborazzi.test.record") == "true") dropAlpha(File(path))
    }

    private fun day(date: LocalDate) = hasContentDescription(
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(app.resources.configuration.locales[0])),
        substring = true,
    )

    /** Play takes JPEG or 24-bit PNG only, without an alpha channel. */
    private fun dropAlpha(file: File) {
        val image = checkNotNull(BitmapFactory.decodeFile(file.path)) { "cannot read ${file.name}" }
        check(image.width == 1080 && image.height == 1920) { "${file.name} is ${image.width}×${image.height}, not 1080×1920" }
        // An opaque bitmap encodes as RGB; the screen is opaque, so no pixel changes
        image.setHasAlpha(false)
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Two phone-calendar events in Jerusalem: Friday's dinner and Shabbat's kiddush. */
    class StoreCalendarProvider : ContentProvider() {
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
                    event(1, "Shabbat dinner at the Levis", "Rechavia", FRIDAY, 19, 0),
                    event(2, "Kiddush — Cohen bar mitzvah", "Yeshurun Synagogue", FRIDAY.plusDays(1), 11, 30),
                ).forEach { values -> addRow(columns.map { values[it] }) }
            }
        }

        private fun event(id: Long, title: String, place: String, day: LocalDate, hour: Int, minute: Int): Map<String, Any> {
            val start = day.atTime(hour, minute).atZone(JERUSALEM_ZONE).toInstant().toEpochMilli()
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
        /** 432×768dp at 400dpi (2.5×): 1080×1920 pixels, Play's 9:16 portrait, at a large phone's width. */
        const val STORE_DEVICE = "w432dp-h768dp-normal-notlong-port-400dpi-keyshidden-nonav"

        /** Relative to the app module, like [ScreenshotTest.OUTPUT_DIR]. */
        const val STORE_DIR = "../fastlane/metadata/android/en-US/images/phoneScreenshots"

        private const val CAL_ID = 4L
        private val JERUSALEM_ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")

        /** Erev Shabbat after [TODAY]: Friday 20 November 2026, 10 Kislev 5787. */
        private val FRIDAY: LocalDate = TODAY.plusDays(3)

        /** `levana.store.shots`: `all`, or comma-separated shot numbers. Unset renders nothing. */
        private fun selected(shot: Shot): Boolean {
            val value = System.getProperty("levana.store.shots")?.trim() ?: return false
            return value == "all" || value.split(',').any { it.trim() == shot.number.toString() }
        }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = Shot.entries.map { arrayOf<Any>(it) }
    }
}

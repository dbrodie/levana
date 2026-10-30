package ninja.doskey.app.levana.screenshots

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings and its sub-screens and dialogs, reached through the drawer as a user would. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class SettingsScreenshotTest(variant: Variant) : ScreenshotTest(variant) {

    override suspend fun seedPreferences(preferences: PreferencesRepository) {
        super.seedPreferences(preferences)
        preferences.saveShowDeveloperSettings(true)
        preferences.saveNotifyCandleLighting(true)
        preferences.saveCandleLightingNotifyMode("morning")
        preferences.saveNotifyHolidays(true)
        preferences.saveNotifyOmer(true)
        preferences.saveNotifyOmerMorning(true)
    }

    private fun group(id: Int) = node(string(id).uppercase())

    @Test
    fun main() {
        openSettings()
        capture("settings_main_1_top")
        group(R.string.settings_group_halachic_times).performScrollTo()
        capture("settings_main_2_middle")
        clickable(string(R.string.settings_notify_mode)).performScrollTo()
        capture("settings_main_3_notifications")
        clickable(string(R.string.settings_about)).performScrollTo()
        capture("settings_main_4_bottom")
    }

    @Test
    fun dialogs() {
        openSettings()
        fun dialog(label: Int, name: String) {
            clickable(string(label)).performScrollTo().performClick()
            compose.waitForIdle()
            capture("settings_dialog_$name")
            compose.onAllNodes(hasText(string(R.string.common_cancel)))[0].performClick()
            compose.waitForIdle()
        }
        dialog(R.string.settings_israel_diaspora_label, "israel_mode")
        dialog(R.string.settings_candle_offset_label, "candle_offset")
        dialog(R.string.settings_yahrzeit_leap_label, "yahrzeit_leap")
        dialog(R.string.settings_yahrzeit_first_year_label, "yahrzeit_first_year")
        dialog(R.string.settings_yahrzeit_missing_day_label, "yahrzeit_missing_day")
        dialog(R.string.settings_app_language_label, "app_language")
        dialog(R.string.settings_notify_mode, "candle_mode")
    }

    @Test
    fun halachicTimes() {
        openSettings()
        clickable(string(R.string.settings_halachic_times_title)).performScrollTo().performClick()
        awaitText(string(R.string.zmanim_selection_hint))
        capture("settings_halachic_times")
    }

    @Test
    fun developer() {
        openSettings()
        clickable(string(R.string.settings_developer_settings)).performScrollTo().performClick()
        awaitText("Date Override")
        // Not captured: its date dialog is GoToDateDialog (ui.calendar, outside this split),
        // which never idles under Robolectric
        capture("settings_developer")
    }

    @Test
    fun about() {
        openSettings()
        clickable(string(R.string.settings_about)).performScrollTo().performClick()
        awaitText(VERSION_NAME)
        capture("settings_about")
    }

    @Test
    fun locationsAndCityPicker() {
        openSettings()
        clickable(string(R.string.settings_location_label)).performClick()
        awaitText(NEW_YORK.name)
        capture("settings_locations")
        clickable(string(R.string.location_add)).performClick()
        compose.waitForIdle()
        capture("settings_city_picker")
    }

    @Test
    fun systemCalendars() {
        openSettings()
        clickable(string(R.string.settings_system_calendars_label)).performClick()
        compose.waitForIdle()
        capture("settings_system_calendars")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}

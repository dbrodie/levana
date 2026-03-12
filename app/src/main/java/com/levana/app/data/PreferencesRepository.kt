package com.levana.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.levana.app.domain.model.Location
import com.levana.app.domain.model.Minhag
import com.levana.app.domain.model.UserPreferences
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val LOCATION_COUNTRY = stringPreferencesKey("location_country")
        val LOCATION_LAT = doublePreferencesKey("location_lat")
        val LOCATION_LON = doublePreferencesKey("location_lon")
        val LOCATION_ELEV = doublePreferencesKey("location_elev")
        val LOCATION_TZ = stringPreferencesKey("location_tz")
        val CANDLE_LIGHTING_OFFSET =
            doublePreferencesKey("candle_lighting_offset")
        val MINHAG = stringPreferencesKey("minhag")
        val IS_IN_ISRAEL = booleanPreferencesKey("is_in_israel")
        val IS_IN_ISRAEL_MANUAL =
            booleanPreferencesKey("is_in_israel_manual")
        val SHOW_MODERN_ISRAELI =
            booleanPreferencesKey("show_modern_israeli")
        val CALENDAR_HEBREW_MODE = booleanPreferencesKey("hebrew_primary")
        val DYNAMIC_HOLIDAY_THEME =
            booleanPreferencesKey("dynamic_holiday_theme")
        val SHOW_DEVELOPER_SETTINGS =
            booleanPreferencesKey("show_developer_settings")
        val DEV_DATE_OVERRIDE =
            longPreferencesKey("dev_date_override")
        val DEV_FORCE_HOLIDAY_THEME =
            stringPreferencesKey("dev_force_holiday_theme")
        val SELECTED_CALENDAR_IDS =
            stringSetPreferencesKey("selected_calendar_ids")
        val NOTIFY_CANDLE_LIGHTING =
            booleanPreferencesKey("notify_candle_lighting")
        val CANDLE_LIGHTING_NOTIFY_MODE =
            stringPreferencesKey("candle_lighting_notify_mode")
        val CANDLE_LIGHTING_MORNING_TIME =
            intPreferencesKey("candle_lighting_morning_time")
        val CANDLE_LIGHTING_HOURS_BEFORE =
            intPreferencesKey("candle_lighting_hours_before")
        val NOTIFY_HOLIDAYS =
            booleanPreferencesKey("notify_holidays")
        val HOLIDAY_NOTIFY_DAYS_BEFORE =
            intPreferencesKey("holiday_notify_days_before")
        val NOTIFY_FASTS =
            booleanPreferencesKey("notify_fasts")
        val NOTIFY_PERSONAL_EVENTS =
            booleanPreferencesKey("notify_personal_events")
        val NOTIFY_OMER =
            booleanPreferencesKey("notify_omer")
        val SELECTED_ZMANIM =
            stringSetPreferencesKey("selected_zmanim")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.LOCATION_NAME]
        val location = if (name != null) {
            Location(
                latitude = prefs[Keys.LOCATION_LAT] ?: 0.0,
                longitude = prefs[Keys.LOCATION_LON] ?: 0.0,
                elevation = prefs[Keys.LOCATION_ELEV] ?: 0.0,
                timezoneId = prefs[Keys.LOCATION_TZ] ?: "UTC",
                name = name,
                country = prefs[Keys.LOCATION_COUNTRY] ?: ""
            )
        } else {
            null
        }
        val candleOffset = prefs[Keys.CANDLE_LIGHTING_OFFSET] ?: 18.0
        val minhagStr = prefs[Keys.MINHAG]
        val minhag = minhagStr?.let {
            try {
                Minhag.valueOf(it)
            } catch (_: IllegalArgumentException) {
                Minhag.ASHKENAZI
            }
        } ?: Minhag.ASHKENAZI

        val isManualIsrael = prefs[Keys.IS_IN_ISRAEL_MANUAL] ?: false
        val isInIsrael = if (isManualIsrael) {
            prefs[Keys.IS_IN_ISRAEL] ?: false
        } else {
            val tz = prefs[Keys.LOCATION_TZ] ?: ""
            tz == "Asia/Jerusalem"
        }

        UserPreferences(
            location = location,
            candleLightingOffset = candleOffset,
            minhag = minhag,
            isInIsrael = isInIsrael,
            showModernIsraeliHolidays = prefs[Keys.SHOW_MODERN_ISRAELI] ?: true,
            calendarHebrewMode = prefs[Keys.CALENDAR_HEBREW_MODE] ?: false,
            dynamicHolidayTheme = prefs[Keys.DYNAMIC_HOLIDAY_THEME] ?: true,
            selectedCalendarIds = prefs[Keys.SELECTED_CALENDAR_IDS]
                ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet(),
            showDeveloperSettings = prefs[Keys.SHOW_DEVELOPER_SETTINGS] ?: true,
            devDateOverride = prefs[Keys.DEV_DATE_OVERRIDE]?.let {
                LocalDate.ofEpochDay(it)
            },
            devForceHolidayTheme = prefs[Keys.DEV_FORCE_HOLIDAY_THEME],
            notifyCandleLighting = prefs[Keys.NOTIFY_CANDLE_LIGHTING] ?: false,
            candleLightingNotifyMode = prefs[Keys.CANDLE_LIGHTING_NOTIFY_MODE] ?: "morning",
            candleLightingMorningTime = prefs[Keys.CANDLE_LIGHTING_MORNING_TIME] ?: 480,
            candleLightingHoursBefore = prefs[Keys.CANDLE_LIGHTING_HOURS_BEFORE] ?: 2,
            notifyHolidays = prefs[Keys.NOTIFY_HOLIDAYS] ?: false,
            holidayNotifyDaysBefore = prefs[Keys.HOLIDAY_NOTIFY_DAYS_BEFORE] ?: 1,
            notifyFasts = prefs[Keys.NOTIFY_FASTS] ?: false,
            notifyPersonalEvents = prefs[Keys.NOTIFY_PERSONAL_EVENTS] ?: false,
            notifyOmer = prefs[Keys.NOTIFY_OMER] ?: false,
            selectedZmanim = prefs[Keys.SELECTED_ZMANIM]
                ?: setOf("Sunrise", "Sunset", "Nightfall")
        )
    }

    suspend fun saveLocation(location: Location) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCATION_NAME] = location.name
            prefs[Keys.LOCATION_COUNTRY] = location.country
            prefs[Keys.LOCATION_LAT] = location.latitude
            prefs[Keys.LOCATION_LON] = location.longitude
            prefs[Keys.LOCATION_ELEV] = location.elevation
            prefs[Keys.LOCATION_TZ] = location.timezoneId
        }
    }

    suspend fun saveCandleLightingOffset(offset: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CANDLE_LIGHTING_OFFSET] = offset
        }
    }

    suspend fun saveMinhag(minhag: Minhag) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MINHAG] = minhag.name
        }
    }

    suspend fun saveIsInIsrael(inIsrael: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_IN_ISRAEL] = inIsrael
            prefs[Keys.IS_IN_ISRAEL_MANUAL] = true
        }
    }

    suspend fun saveShowModernIsraeliHolidays(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_MODERN_ISRAELI] = show
        }
    }

    suspend fun saveCalendarHebrewMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CALENDAR_HEBREW_MODE] = enabled
        }
    }

    suspend fun saveDynamicHolidayTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_HOLIDAY_THEME] = enabled
        }
    }

    suspend fun saveShowDeveloperSettings(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_DEVELOPER_SETTINGS] = show
        }
    }

    suspend fun saveDevDateOverride(date: LocalDate?) {
        context.dataStore.edit { prefs ->
            if (date != null) {
                prefs[Keys.DEV_DATE_OVERRIDE] = date.toEpochDay()
            } else {
                prefs.remove(Keys.DEV_DATE_OVERRIDE)
            }
        }
    }

    suspend fun saveDevForceHolidayTheme(theme: String?) {
        context.dataStore.edit { prefs ->
            if (theme != null) {
                prefs[Keys.DEV_FORCE_HOLIDAY_THEME] = theme
            } else {
                prefs.remove(Keys.DEV_FORCE_HOLIDAY_THEME)
            }
        }
    }

    suspend fun saveSelectedCalendarIds(ids: Set<Long>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_CALENDAR_IDS] =
                ids.map { it.toString() }.toSet()
        }
    }

    suspend fun saveNotifyCandleLighting(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_CANDLE_LIGHTING] = enabled
        }
    }

    suspend fun saveCandleLightingNotifyMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CANDLE_LIGHTING_NOTIFY_MODE] = mode
        }
    }

    suspend fun saveCandleLightingMorningTime(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CANDLE_LIGHTING_MORNING_TIME] = minutes
        }
    }

    suspend fun saveCandleLightingHoursBefore(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CANDLE_LIGHTING_HOURS_BEFORE] = hours
        }
    }

    suspend fun saveNotifyHolidays(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_HOLIDAYS] = enabled
        }
    }

    suspend fun saveHolidayNotifyDaysBefore(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOLIDAY_NOTIFY_DAYS_BEFORE] = days
        }
    }

    suspend fun saveNotifyFasts(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_FASTS] = enabled
        }
    }

    suspend fun saveNotifyPersonalEvents(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_PERSONAL_EVENTS] = enabled
        }
    }

    suspend fun saveNotifyOmer(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_OMER] = enabled
        }
    }

    suspend fun saveSelectedZmanim(zmanim: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_ZMANIM] = zmanim
        }
    }
}

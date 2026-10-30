package ninja.doskey.app.levana.data

import android.content.Context
import androidx.datastore.core.DataMigration
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
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.resolveIsInIsrael
import ninja.doskey.app.levana.device.systemTimeChanges
import ninja.doskey.app.levana.domain.model.IsraelMode
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
    produceMigrations = { listOf(LocationModeMigration, IsraelModeMigration) }
)

class PreferencesRepository(
    private val context: Context,
    private val cityRepository: CityRepository,
    /** Re-reads the preferences, so the zone's fallback city follows a time-zone change. */
    systemTimeChanges: Flow<Unit> = systemTimeChanges(context)
) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val SAVED_LOCATIONS = stringPreferencesKey("saved_locations")
        val ACTIVE_LOCATION_ID = stringPreferencesKey("active_location_id")
        // Key name predates the always-on current location; kept so stored fixes survive.
        val CURRENT_LOCATION = stringPreferencesKey("gps_location")

        val CANDLE_LIGHTING_OFFSET =
            doublePreferencesKey("candle_lighting_offset")
        // The removed general Minhag selection stored "minhag". Older stores may still
        // contain it; it is deliberately left unread and in place (DataStore keeps unknown keys).
        val ISRAEL_MODE = stringPreferencesKey(ISRAEL_MODE_KEY)
        val SHOW_MODERN_ISRAELI =
            booleanPreferencesKey("show_modern_israeli")
        val CALENDAR_HEBREW_MODE = booleanPreferencesKey("hebrew_primary")
        val DYNAMIC_HOLIDAY_THEME =
            booleanPreferencesKey("dynamic_holiday_theme")
        val SHOW_DEVELOPER_SETTINGS =
            booleanPreferencesKey("show_developer_settings")
        // Test only; see UserPreferences.devDateOverride
        val DEV_DATE_OVERRIDE = longPreferencesKey("dev_date_override")
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
        val NOTIFY_OMER_TZAIT =
            booleanPreferencesKey("notify_omer_tzait")
        val NOTIFY_OMER_MORNING =
            booleanPreferencesKey("notify_omer_morning")
        val NOTIFY_OMER_MORNING_TIME =
            intPreferencesKey("notify_omer_morning_time")
        val SELECTED_ZMANIM =
            stringSetPreferencesKey("selected_zmanim")
        val YAHRZEIT_LEAP_YEAR_ADAR =
            stringPreferencesKey("yahrzeit_leap_year_adar")
        val YAHRZEIT_FIRST_YEAR =
            stringPreferencesKey("yahrzeit_first_year")
        val YAHRZEIT_ADAR_I_30_NISAN_1 =
            booleanPreferencesKey("yahrzeit_adar_i_30_nisan_1")
        val YAHRZEIT_MISSING_DAY =
            stringPreferencesKey("yahrzeit_missing_day")

        // Internal update-checker state — NOT part of UserPreferences
        val LAST_NOTIFIED_UPDATE_VERSION =
            stringPreferencesKey("last_notified_update_version")

        // The time-zone mismatch last shown (TASK-46) — NOT part of UserPreferences
        val ZONE_MISMATCH_NOTICE = stringPreferencesKey("zone_mismatch_notice")
    }

    val preferences: Flow<UserPreferences> = combine(
        context.dataStore.data,
        systemTimeChanges.onStart { emit(Unit) }
    ) { prefs, _ -> prefs }.map { prefs ->
        val savedLocations = decodeSavedLocations(prefs)
        // A fix keeps only its place: it is always in the device's current zone, whatever zone
        // it was stored with, so a zone change re-zones it without waiting for a new fix
        val fix = prefs[Keys.CURRENT_LOCATION]?.let {
            runCatching { json.decodeFromString<Location>(it) }.getOrNull()
        }?.copy(timezoneId = DeviceContext.realZone().id)
        val currentLocation = fix
            ?: cityRepository.defaultForTimeZone(DeviceContext.realZone(), DeviceContext.realClock().instant())

        val candleOffset = prefs[Keys.CANDLE_LIGHTING_OFFSET] ?: 18.0

        val activeLocationId = prefs[Keys.ACTIVE_LOCATION_ID]

        val israelMode = IsraelMode.entries.firstOrNull { it.name == prefs[Keys.ISRAEL_MODE] }
            ?: IsraelMode.AUTO

        UserPreferences(
            currentLocation = currentLocation,
            currentLocationIsFix = fix != null,
            savedLocations = savedLocations,
            activeLocationId = activeLocationId,
            candleLightingOffset = candleOffset,
            israelMode = israelMode,
            // Auto-detect follows the device position, never the selected city
            isInIsrael = resolveIsInIsrael(israelMode, DeviceContext.realZone()),
            showModernIsraeliHolidays = prefs[Keys.SHOW_MODERN_ISRAELI] ?: true,
            calendarHebrewMode = prefs[Keys.CALENDAR_HEBREW_MODE] ?: false,
            dynamicHolidayTheme = prefs[Keys.DYNAMIC_HOLIDAY_THEME] ?: true,
            selectedCalendarIds = prefs[Keys.SELECTED_CALENDAR_IDS]
                ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet(),
            showDeveloperSettings = prefs[Keys.SHOW_DEVELOPER_SETTINGS] ?: true,
            devDateOverride = prefs[Keys.DEV_DATE_OVERRIDE]?.let { LocalDate.ofEpochDay(it) },
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
            notifyOmerTzait = prefs[Keys.NOTIFY_OMER_TZAIT] ?: true,
            notifyOmerMorning = prefs[Keys.NOTIFY_OMER_MORNING] ?: false,
            notifyOmerMorningTime = prefs[Keys.NOTIFY_OMER_MORNING_TIME] ?: 420,
            selectedZmanim = prefs[Keys.SELECTED_ZMANIM]
                ?: setOf("Sunrise", "Sunset", "Nightfall"),
            yahrzeitRules = parseYahrzeitRules(
                prefs[Keys.YAHRZEIT_LEAP_YEAR_ADAR],
                prefs[Keys.YAHRZEIT_FIRST_YEAR],
                prefs[Keys.YAHRZEIT_ADAR_I_30_NISAN_1],
                prefs[Keys.YAHRZEIT_MISSING_DAY]
            )
        )
    }

    suspend fun yahrzeitRules(): YahrzeitRules = preferences.first().yahrzeitRules

    suspend fun saveYahrzeitLeapYearAdar(value: YahrzeitLeapYearAdar) {
        context.dataStore.edit { prefs ->
            prefs[Keys.YAHRZEIT_LEAP_YEAR_ADAR] = value.name
        }
    }

    suspend fun saveYahrzeitFirstYear(value: YahrzeitFirstYear) {
        context.dataStore.edit { prefs ->
            prefs[Keys.YAHRZEIT_FIRST_YEAR] = value.name
        }
    }

    suspend fun saveYahrzeitMissingDay(value: YahrzeitMissingDay) {
        context.dataStore.edit { prefs ->
            prefs[Keys.YAHRZEIT_MISSING_DAY] = value.name
        }
    }

    suspend fun saveYahrzeitAdarI30AlsoNisan1(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.YAHRZEIT_ADAR_I_30_NISAN_1] = enabled
        }
    }

    private fun decodeSavedLocations(prefs: Preferences): List<SavedLocation> =
        prefs[Keys.SAVED_LOCATIONS]?.let {
            runCatching { json.decodeFromString<List<SavedLocation>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()

    /** Saves [location]. It is not selected: the city is chosen only in the sidebar. */
    suspend fun addSavedLocation(location: Location) {
        val newEntry = SavedLocation(id = UUID.randomUUID().toString(), location = location)
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_LOCATIONS] = json.encodeToString(decodeSavedLocations(prefs) + newEntry)
        }
    }

    /** Removes a saved location; if it was active, the current location takes over. */
    suspend fun removeSavedLocation(id: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_LOCATIONS] =
                json.encodeToString(decodeSavedLocations(prefs).filter { it.id != id })
            if (prefs[Keys.ACTIVE_LOCATION_ID] == id) {
                prefs.remove(Keys.ACTIVE_LOCATION_ID)
            }
        }
    }

    /** Selects a saved location by [id], or the current location when [id] is null. */
    suspend fun setActiveLocation(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[Keys.ACTIVE_LOCATION_ID] = id
            } else {
                prefs.remove(Keys.ACTIVE_LOCATION_ID)
            }
        }
    }

    suspend fun updateCurrentLocation(location: Location) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_LOCATION] = json.encodeToString(location)
        }
    }

    suspend fun saveCandleLightingOffset(offset: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CANDLE_LIGHTING_OFFSET] = offset
        }
    }

    suspend fun saveIsraelMode(mode: IsraelMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ISRAEL_MODE] = mode.name
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

    /** Test only: the developer override's fixed date; null turns it off. */
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

    suspend fun saveNotifyOmerTzait(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_OMER_TZAIT] = enabled
        }
    }

    suspend fun saveNotifyOmerMorning(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_OMER_MORNING] = enabled
        }
    }

    suspend fun saveNotifyOmerMorningTime(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_OMER_MORNING_TIME] = minutes
        }
    }

    suspend fun saveSelectedZmanim(zmanim: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_ZMANIM] = zmanim
        }
    }

    suspend fun getLastNotifiedUpdateVersion(): String? {
        return context.dataStore.data.first()[Keys.LAST_NOTIFIED_UPDATE_VERSION]
    }

    suspend fun setLastNotifiedUpdateVersion(version: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_NOTIFIED_UPDATE_VERSION] = version
        }
    }

    /** The key of the time-zone mismatch last shown, see [ninja.doskey.app.levana.device.zoneMismatchKey]. */
    suspend fun getZoneMismatchNotice(): String? =
        context.dataStore.data.first()[Keys.ZONE_MISMATCH_NOTICE]

    /** Records the mismatch shown, or null once there is none, so a new one shows again. */
    suspend fun setZoneMismatchNotice(key: String?) {
        context.dataStore.edit { prefs ->
            if (key != null) prefs[Keys.ZONE_MISMATCH_NOTICE] = key
            else prefs.remove(Keys.ZONE_MISMATCH_NOTICE)
        }
    }
}

/**
 * Replaces the old `location_mode` key ({"type":"gps"} or {"type":"saved","id":…}) with
 * `active_location_id`. GPS mode maps to "no id", i.e. the current location.
 */
internal object LocationModeMigration : DataMigration<Preferences> {
    private val LOCATION_MODE = stringPreferencesKey("location_mode")
    private val ACTIVE_LOCATION_ID = stringPreferencesKey("active_location_id")

    override suspend fun shouldMigrate(currentData: Preferences) = LOCATION_MODE in currentData

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        val mode = runCatching {
            Json.parseToJsonElement(prefs[LOCATION_MODE]!!).jsonObject
        }.getOrNull()
        if (mode?.get("type")?.jsonPrimitive?.content == "saved") {
            mode["id"]?.jsonPrimitive?.content?.let { prefs[ACTIVE_LOCATION_ID] = it }
        }
        prefs.remove(LOCATION_MODE)
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}

private const val ISRAEL_MODE_KEY = "israel_mode"

/**
 * Replaces the old "In Israel" switch (`is_in_israel`, made manual for good by
 * `is_in_israel_manual`) with the three-way setting: a manual choice is kept as Israel or
 * Diaspora; everyone else gets Auto-detect.
 */
internal object IsraelModeMigration : DataMigration<Preferences> {
    private val inIsrael = booleanPreferencesKey("is_in_israel")
    private val manual = booleanPreferencesKey("is_in_israel_manual")
    private val mode = stringPreferencesKey(ISRAEL_MODE_KEY)

    override suspend fun shouldMigrate(currentData: Preferences) =
        inIsrael in currentData || manual in currentData

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        if (prefs[manual] == true && mode !in prefs) {
            prefs[mode] = if (prefs[inIsrael] == true) IsraelMode.ISRAEL.name else IsraelMode.DIASPORA.name
        }
        prefs.remove(inIsrael)
        prefs.remove(manual)
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}

/** Stored yahrzeit settings; missing or unrecognised values fall back to the defaults. */
internal fun parseYahrzeitRules(
    leapYearAdar: String?,
    firstYear: String?,
    adarI30AlsoNisan1: Boolean?,
    missingDay: String? = null
): YahrzeitRules {
    val defaults = YahrzeitRules()
    return YahrzeitRules(
        leapYearAdar = YahrzeitLeapYearAdar.entries.firstOrNull { it.name == leapYearAdar }
            ?: defaults.leapYearAdar,
        firstYear = YahrzeitFirstYear.entries.firstOrNull { it.name == firstYear }
            ?: defaults.firstYear,
        adarI30AlsoNisan1 = adarI30AlsoNisan1 ?: defaults.adarI30AlsoNisan1,
        missingDay = YahrzeitMissingDay.entries.firstOrNull { it.name == missingDay }
            ?: defaults.missingDay
    )
}

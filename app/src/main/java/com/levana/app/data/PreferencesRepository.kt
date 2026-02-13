package com.levana.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.levana.app.domain.model.Location
import com.levana.app.domain.model.Minhag
import com.levana.app.domain.model.UserPreferences
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
        val HEBREW_PRIMARY = booleanPreferencesKey("hebrew_primary")
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
            hebrewPrimary = prefs[Keys.HEBREW_PRIMARY] ?: false
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

    suspend fun saveHebrewPrimary(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HEBREW_PRIMARY] = enabled
        }
    }
}

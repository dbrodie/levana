package com.levana.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.levana.app.domain.model.Location
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
        UserPreferences(location = location)
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
}

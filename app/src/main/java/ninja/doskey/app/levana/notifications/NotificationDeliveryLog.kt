package ninja.doskey.app.levana.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first

// Kept apart from the user preferences so delivery bookkeeping never looks like a settings change
private val Context.deliveryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_delivery"
)

/** Remembers which reminder notices were already delivered, so they are not posted twice. */
class NotificationDeliveryLog(context: Context) {

    private val dataStore = context.applicationContext.deliveryDataStore

    suspend fun lastCandleLightingDate(): LocalDate? =
        dataStore.data.first()[LAST_CANDLE_LIGHTING_EPOCH_DAY]?.let(LocalDate::ofEpochDay)

    suspend fun recordCandleLighting(date: LocalDate) {
        dataStore.edit { it[LAST_CANDLE_LIGHTING_EPOCH_DAY] = date.toEpochDay() }
    }

    private companion object {
        val LAST_CANDLE_LIGHTING_EPOCH_DAY = longPreferencesKey("last_candle_lighting_epoch_day")
    }
}

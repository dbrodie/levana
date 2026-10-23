package ninja.doskey.app.levana.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.util.GregorianCalendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Kept apart from the user preferences so delivery bookkeeping never looks like a settings change
private val Context.deliveryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_delivery"
)

/**
 * An Omer notice as the user sees it: the [evening] (nightfall) or morning reminder of count
 * [omerDay] in the Omer of [hebrewYear]. The same count next year is a different notice.
 */
internal data class OmerDelivery(val evening: Boolean, val hebrewYear: Int, val omerDay: Int) {
    companion object {
        /**
         * The notice an Omer alarm for the civil [date] posts. The evening reminder counts the
         * Hebrew day beginning that night, so its year is that of the next civil date.
         */
        fun of(evening: Boolean, date: LocalDate, omerDay: Int): OmerDelivery {
            val counted = if (evening) date.plusDays(1) else date
            val jewishYear = JewishCalendar(
                GregorianCalendar(counted.year, counted.monthValue - 1, counted.dayOfMonth)
            ).jewishYear
            return OmerDelivery(evening, jewishYear, omerDay)
        }
    }
}

/** Remembers which reminder notices were already delivered, so they are not posted twice. */
class NotificationDeliveryLog internal constructor(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.deliveryDataStore)

    suspend fun lastCandleLightingDate(): LocalDate? =
        dataStore.data.first()[LAST_CANDLE_LIGHTING_EPOCH_DAY]?.let(LocalDate::ofEpochDay)

    suspend fun recordCandleLighting(date: LocalDate) {
        dataStore.edit { it[LAST_CANDLE_LIGHTING_EPOCH_DAY] = date.toEpochDay() }
    }

    /**
     * Posts [notice] with [post] unless it was already delivered, e.g. before a westward
     * time-zone change planned the same reminder again. It is recorded only when [post]
     * reports that it was posted. Returns whether it was posted now.
     */
    internal suspend fun deliverOmerOnce(notice: OmerDelivery, post: () -> Boolean): Boolean =
        // One check-and-record at a time, so two alarms for one notice cannot both post it
        omerLock.withLock {
            val key = format(notice)
            if (key in dataStore.data.first()[DELIVERED_OMER].orEmpty()) return@withLock false
            if (!post()) return@withLock false
            dataStore.edit { prefs ->
                // Only this and last year's Omer can still be planned
                val kept = prefs[DELIVERED_OMER].orEmpty().filter { entry ->
                    parseYear(entry)?.let { it >= notice.hebrewYear - 1 } == true
                }
                prefs[DELIVERED_OMER] = kept.toSet() + key
            }
            true
        }

    private companion object {
        val LAST_CANDLE_LIGHTING_EPOCH_DAY = longPreferencesKey("last_candle_lighting_epoch_day")
        val DELIVERED_OMER = stringSetPreferencesKey("delivered_omer")

        val omerLock = Mutex()

        fun format(notice: OmerDelivery) = buildString {
            append(if (notice.evening) "evening" else "morning")
            append('|').append(notice.hebrewYear).append('|').append(notice.omerDay)
        }

        fun parseYear(entry: String): Int? = entry.split('|').getOrNull(1)?.toIntOrNull()
    }
}

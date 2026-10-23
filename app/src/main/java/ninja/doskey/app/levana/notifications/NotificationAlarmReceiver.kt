package ninja.doskey.app.levana.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()

    override fun onReceive(receiverContext: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = preferencesRepository.preferences.first()
                deliverAlarm(receiverContext, intent, prefs, NotificationDeliveryLog(receiverContext))
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Delivers an alarm [intent] through [deliverIfEnabled] and records what was posted in
 * [deliveryLog]. An Omer notice already delivered — e.g. before a westward time-zone change
 * planned the same reminder again — is not posted a second time. Returns whether a
 * notification was posted.
 */
internal suspend fun deliverAlarm(
    receiverContext: Context,
    intent: Intent,
    prefs: UserPreferences,
    deliveryLog: NotificationDeliveryLog
): Boolean {
    val date = LocalDate.ofEpochDay(
        intent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0L)
    )
    return when (intent.action) {
        NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING ->
            deliverIfEnabled(receiverContext, intent, prefs).also { posted ->
                if (posted) deliveryLog.recordCandleLighting(date)
            }
        NotificationAlarmScheduler.ACTION_OMER, NotificationAlarmScheduler.ACTION_OMER_MORNING -> {
            val omerDay = intent.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0)
            if (omerDay <= 0) return false
            val evening = intent.action == NotificationAlarmScheduler.ACTION_OMER
            deliveryLog.deliverOmerOnce(OmerDelivery.of(evening, date, omerDay)) {
                deliverIfEnabled(receiverContext, intent, prefs)
            }
        }
        else -> false
    }
}

/**
 * Posts the reminder carried by an alarm [intent], unless its category was switched off in
 * [prefs] after the alarm was scheduled. Returns whether a notification was posted.
 */
internal fun deliverIfEnabled(receiverContext: Context, intent: Intent, prefs: UserPreferences): Boolean {
    if (!isAlarmEnabled(intent.action, prefs)) return false
    // Notification text follows Settings → App language, not just the device locale
    val context = receiverContext.withAppLocale()
    val dateEpochDay = intent.getLongExtra(
        NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY,
        0L
    )
    val date = LocalDate.ofEpochDay(dateEpochDay)

    return when (intent.action) {
        NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING -> {
            val candleLightingTime = intent.getStringExtra(
                NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME
            )?.let { LocalTime.parse(it) } ?: return false

            val endTime = intent.getStringExtra(
                NotificationAlarmScheduler.EXTRA_END_TIME
            )?.let { LocalTime.parse(it) }

            val end = endTime?.let {
                val lastDay = LocalDate.ofEpochDay(intent.getLongExtra(
                    NotificationAlarmScheduler.EXTRA_END_DATE_EPOCH_DAY,
                    date.plusDays(1).toEpochDay()
                ))
                ObservanceEnd(
                    // Alarms set by previous versions only carry the last holy day
                    date = LocalDate.ofEpochDay(intent.getLongExtra(
                        NotificationAlarmScheduler.EXTRA_END_CIVIL_DATE_EPOCH_DAY,
                        lastDay.toEpochDay()
                    )),
                    time = it,
                    includesYomTov = intent.getBooleanExtra(
                        NotificationAlarmScheduler.EXTRA_END_INCLUDES_YOM_TOV, false
                    ),
                    lastDay = lastDay
                )
            }
            NotificationPoster.postCandleLighting(context, date, candleLightingTime, end)
        }
        NotificationAlarmScheduler.ACTION_OMER -> {
            val omerDay = intent.getIntExtra(
                NotificationAlarmScheduler.EXTRA_OMER_DAY,
                0
            )
            omerDay > 0 && NotificationPoster.postOmer(context, date, omerDay)
        }
        NotificationAlarmScheduler.ACTION_OMER_MORNING -> {
            val omerDay = intent.getIntExtra(
                NotificationAlarmScheduler.EXTRA_OMER_DAY,
                0
            )
            omerDay > 0 && NotificationPoster.postOmerMorning(context, date, omerDay)
        }
        else -> false
    }
}

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
                deliverIfEnabled(receiverContext, intent, prefs)
                if (isAlarmEnabled(intent.action, prefs) &&
                    intent.action == NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING &&
                    intent.hasExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME)
                ) {
                    NotificationDeliveryLog(receiverContext).recordCandleLighting(
                        LocalDate.ofEpochDay(intent.getLongExtra(
                            NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0L
                        ))
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Posts the reminder carried by an alarm [intent], unless its category was switched off in
 * [prefs] after the alarm was scheduled.
 */
internal fun deliverIfEnabled(receiverContext: Context, intent: Intent, prefs: UserPreferences) {
    if (!isAlarmEnabled(intent.action, prefs)) return
    // Notification text follows Settings → App language, not just the device locale
    val context = receiverContext.withAppLocale()
    val dateEpochDay = intent.getLongExtra(
        NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY,
        0L
    )
    val date = LocalDate.ofEpochDay(dateEpochDay)

    when (intent.action) {
        NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING -> {
            val candleLightingTime = intent.getStringExtra(
                NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME
            )?.let { LocalTime.parse(it) } ?: return

            val endTime = intent.getStringExtra(
                NotificationAlarmScheduler.EXTRA_END_TIME
            )?.let { LocalTime.parse(it) }

            val end = endTime?.let {
                ObservanceEnd(
                    date = LocalDate.ofEpochDay(intent.getLongExtra(
                        NotificationAlarmScheduler.EXTRA_END_DATE_EPOCH_DAY,
                        date.plusDays(1).toEpochDay()
                    )),
                    time = it,
                    includesYomTov = intent.getBooleanExtra(
                        NotificationAlarmScheduler.EXTRA_END_INCLUDES_YOM_TOV, false
                    )
                )
            }
            NotificationPoster.postCandleLighting(context, date, candleLightingTime, end)
        }
        NotificationAlarmScheduler.ACTION_OMER -> {
            val omerDay = intent.getIntExtra(
                NotificationAlarmScheduler.EXTRA_OMER_DAY,
                0
            )
            if (omerDay > 0) {
                NotificationPoster.postOmer(context, date, omerDay)
            }
        }
        NotificationAlarmScheduler.ACTION_OMER_MORNING -> {
            val omerDay = intent.getIntExtra(
                NotificationAlarmScheduler.EXTRA_OMER_DAY,
                0
            )
            if (omerDay > 0) {
                NotificationPoster.postOmerMorning(context, date, omerDay)
            }
        }
    }
}

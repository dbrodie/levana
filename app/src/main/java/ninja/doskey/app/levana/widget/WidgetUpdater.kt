package ninja.doskey.app.levana.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Refreshes the home-screen widgets and keeps the next refresh scheduled. */
object WidgetUpdater {
    const val ACTION_REFRESH = "ninja.doskey.app.levana.widget.REFRESH"
    private const val TAG = "WidgetUpdater"

    /** Counts refresh requests; a running widget recomputes what it shows on each one. */
    val refreshes = MutableStateFlow(0)

    /** Every widget's receiver; each one redraws its own widgets. */
    private val receivers: List<Class<out GlanceAppWidgetReceiver>> = listOf(
        TodayWidgetReceiver::class.java,
        MonthWidgetReceiver::class.java,
        ZmanimWidgetReceiver::class.java,
    )

    /** Asks every widget receiver to redraw; safe to call from any receiver or thread. */
    fun requestUpdate(context: Context) {
        receivers.forEach { context.sendBroadcast(refreshIntent(context, it)) }
    }

    /**
     * Redraws at [at] (the next zman or midnight). The non-wakeup alarm fires once the device
     * is awake, when the widget can be seen. Without an exact-alarm permission Android allows
     * a window of no less than 10 minutes, so a passed zman shows at most that long.
     */
    fun scheduleRefresh(context: Context, at: Instant, receiver: Class<out GlanceAppWidgetReceiver>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.setWindow(AlarmManager.RTC, at.toEpochMilli(), REFRESH_WINDOW_MS, refreshPendingIntent(context, receiver))
    }

    private const val REFRESH_WINDOW_MS = 10 * 60 * 1000L

    fun cancelRefresh(context: Context, receiver: Class<out GlanceAppWidgetReceiver>) {
        context.getSystemService(AlarmManager::class.java)?.cancel(refreshPendingIntent(context, receiver))
    }

    private fun refreshIntent(context: Context, receiver: Class<out GlanceAppWidgetReceiver>) =
        Intent(context, receiver).setAction(ACTION_REFRESH)

    // One alarm per receiver: the intents name different components, so they don't replace each other
    private fun refreshPendingIntent(context: Context, receiver: Class<out GlanceAppWidgetReceiver>) = PendingIntent.getBroadcast(
        context,
        0,
        refreshIntent(context, receiver),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun updateAsync(receiver: GlanceAppWidgetReceiver, context: Context, widget: GlanceAppWidget) {
        val pendingResult = receiver.goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                refreshes.update { it + 1 }
                widget.updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widgets", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** A widget receiver that redraws on refresh requests and locale changes. */
abstract class RefreshingWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetUpdater.ACTION_REFRESH,
            Intent.ACTION_LOCALE_CHANGED -> WidgetUpdater.updateAsync(this, context, glanceAppWidget)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdater.cancelRefresh(context, javaClass)
    }
}

class TodayWidgetReceiver : RefreshingWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

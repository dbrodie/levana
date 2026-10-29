package ninja.doskey.app.levana

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import ninja.doskey.app.levana.crash.CrashReporting
import ninja.doskey.app.levana.di.allModules
import ninja.doskey.app.levana.icon.DynamicIconManager
import ninja.doskey.app.levana.notifications.DailyNotificationWorker
import ninja.doskey.app.levana.notifications.NotificationChannels
import ninja.doskey.app.levana.notifications.NotificationRescheduling
import ninja.doskey.app.levana.update.UpdateCheckWorker
import ninja.doskey.app.levana.widget.WidgetRefreshing
import ninja.doskey.app.levana.widget.WidgetUpdater
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LevanaApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        CrashReporting.init(this)
    }

    override fun onCreate() {
        super.onCreate()
        // ACRA shows the crash dialog in its own process; don't start the app there
        if (CrashReporting.isReporterProcess()) return
        DynamicIconManager.restoreMainActivity(this)
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@LevanaApplication)
            modules(allModules)
        }
        NotificationChannels.createAll(this)
        DailyNotificationWorker.enqueueDaily(this)
        NotificationRescheduling.start()
        UpdateCheckWorker.enqueueDaily(this)
        WidgetRefreshing.start(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Changing Settings → App language (or the device language) arrives here; rename
        // the notification channels so system settings show them in the new language
        NotificationChannels.createAll(this)
        WidgetUpdater.requestUpdate(this)
    }
}

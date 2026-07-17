package ninja.doskey.app.levana

import android.app.Application
import ninja.doskey.app.levana.di.allModules
import ninja.doskey.app.levana.notifications.DailyNotificationWorker
import ninja.doskey.app.levana.notifications.NotificationChannels
import ninja.doskey.app.levana.update.UpdateCheckWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LevanaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@LevanaApplication)
            modules(allModules)
        }
        NotificationChannels.createAll(this)
        DailyNotificationWorker.enqueueDaily(this)
        UpdateCheckWorker.enqueueDaily(this)
    }
}

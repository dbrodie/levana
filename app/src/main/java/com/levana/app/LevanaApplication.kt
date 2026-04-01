package com.levana.app

import android.app.Application
import com.levana.app.di.allModules
import com.levana.app.notifications.DailyNotificationWorker
import com.levana.app.notifications.NotificationChannels
import com.levana.app.update.UpdateCheckWorker
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

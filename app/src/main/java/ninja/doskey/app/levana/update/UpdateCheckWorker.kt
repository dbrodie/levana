package ninja.doskey.app.levana.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.notifications.NotificationPoster
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val updateChecker: UpdateChecker by inject()
    private val preferencesRepository: PreferencesRepository by inject()

    override suspend fun doWork(): Result {
        if (updateChecker.isDevBuild()) return Result.success()

        when (updateChecker.detectUpdateSource()) {
            UpdateSource.GitHubRelease -> Unit
            UpdateSource.PlayStore,
            UpdateSource.FDroid,
            UpdateSource.Unknown -> return Result.success()
        }

        val latestVersion = updateChecker.fetchLatestVersion()
            ?: return Result.success()

        val currentVersion = updateChecker.currentVersionName()
        if (!updateChecker.isNewerVersion(currentVersion, latestVersion)) {
            return Result.success()
        }

        if (preferencesRepository.getLastNotifiedUpdateVersion() == latestVersion) {
            return Result.success()
        }

        if (NotificationPoster.postUpdateAvailable(applicationContext, latestVersion)) {
            preferencesRepository.setLastNotifiedUpdateVersion(latestVersion)
        }

        return Result.success()
    }

    companion object {
        private const val UPDATE_CHECK_WORK_NAME = "update_check_worker"

        fun enqueueDaily(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UPDATE_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS).build()
            )
        }
    }
}

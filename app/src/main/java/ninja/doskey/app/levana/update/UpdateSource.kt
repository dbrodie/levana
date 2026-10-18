package ninja.doskey.app.levana.update

/**
 * Represents the channel through which this APK was installed.
 *
 * Detection is done via [android.content.pm.PackageManager.getInstallSourceInfo]
 * (API 30+, always available on minSdk 34).
 */
sealed interface UpdateSource {
    /**
     * Sideloaded GitHub APK — installed by the system package installer (tapped in a
     * browser or file manager), ADB, or any other app. Checks GitHub Releases.
     */
    data object GitHubRelease : UpdateSource

    /** Installed via Google Play — Play handles its own update flow. */
    data object PlayStore : UpdateSource

    /** Installed via F-Droid — F-Droid handles its own update flow. */
    data object FDroid : UpdateSource

    companion object {
        private const val INSTALLER_PLAY_STORE = "com.android.vending"
        private const val INSTALLER_FDROID = "org.fdroid.fdroid"

        /**
         * Maps an `installingPackageName` to its update source. Levana ships only via
         * GitHub, Play and F-Droid, so any installer other than Play or F-Droid
         * (including none) means a GitHub-built APK.
         */
        fun fromInstaller(installingPackageName: String?): UpdateSource =
            when (installingPackageName) {
                INSTALLER_PLAY_STORE -> PlayStore
                INSTALLER_FDROID -> FDroid
                else -> GitHubRelease
            }
    }
}

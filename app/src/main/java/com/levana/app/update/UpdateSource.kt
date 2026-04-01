package com.levana.app.update

/**
 * Represents the channel through which this APK was installed.
 *
 * Detection is done via [android.content.pm.PackageManager.getInstallSourceInfo]
 * (API 30+, always available on minSdk 34).
 */
sealed interface UpdateSource {
    /** Sideloaded APK (null or ADB installer). Checks GitHub Releases. */
    data object GitHubRelease : UpdateSource

    /** Installed via Google Play — Play handles its own update flow. */
    data object PlayStore : UpdateSource

    /** Installed via F-Droid — F-Droid handles its own update flow. */
    data object FDroid : UpdateSource

    /** Unknown installer — skip update check conservatively. */
    data object Unknown : UpdateSource
}

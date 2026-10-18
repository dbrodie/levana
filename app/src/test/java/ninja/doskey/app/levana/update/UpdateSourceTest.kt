package ninja.doskey.app.levana.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateSourceTest {

    @Test
    fun `no installer (adb) is a GitHub release`() {
        assertEquals(UpdateSource.GitHubRelease, UpdateSource.fromInstaller(null))
    }

    @Test
    fun `Google Play is PlayStore`() {
        assertEquals(UpdateSource.PlayStore, UpdateSource.fromInstaller("com.android.vending"))
    }

    @Test
    fun `F-Droid is FDroid`() {
        assertEquals(UpdateSource.FDroid, UpdateSource.fromInstaller("org.fdroid.fdroid"))
    }

    @Test
    fun `system package installer (APK tapped in browser or file manager) is a GitHub release`() {
        // Observed on API 36 when tapping the release APK in the Files app.
        assertEquals(
            UpdateSource.GitHubRelease,
            UpdateSource.fromInstaller("com.google.android.packageinstaller")
        )
        assertEquals(
            UpdateSource.GitHubRelease,
            UpdateSource.fromInstaller("com.android.packageinstaller")
        )
    }

    @Test
    fun `Chrome is a GitHub release`() {
        assertEquals(UpdateSource.GitHubRelease, UpdateSource.fromInstaller("com.android.chrome"))
    }

    @Test
    fun `file managers are a GitHub release`() {
        assertEquals(
            UpdateSource.GitHubRelease,
            UpdateSource.fromInstaller("com.google.android.documentsui")
        )
        assertEquals(UpdateSource.GitHubRelease, UpdateSource.fromInstaller("com.mixplorer"))
    }

    @Test
    fun `Obtainium is a GitHub release`() {
        assertEquals(UpdateSource.GitHubRelease, UpdateSource.fromInstaller("dev.imranr.obtainium"))
    }
}

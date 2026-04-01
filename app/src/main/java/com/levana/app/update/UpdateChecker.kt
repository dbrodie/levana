package com.levana.app.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_OWNER = "dbrodie"
private const val GITHUB_REPO = "levana"
private const val GITHUB_RELEASES_URL =
    "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
const val GITHUB_RELEASES_PAGE =
    "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"

private const val INSTALLER_PLAY_STORE = "com.android.vending"
private const val INSTALLER_FDROID = "org.fdroid.fdroid"

@Serializable
private data class GitHubRelease(@SerialName("tag_name") val tagName: String)

class UpdateChecker(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun detectUpdateSource(): UpdateSource {
        val info = context.packageManager.getInstallSourceInfo(context.packageName)
        return when (info.installingPackageName) {
            INSTALLER_PLAY_STORE -> UpdateSource.PlayStore
            INSTALLER_FDROID -> UpdateSource.FDroid
            null -> UpdateSource.GitHubRelease
            else -> UpdateSource.Unknown
        }
    }

    fun currentVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
            ?: "0.0.0-dev"

    fun isDevBuild(): Boolean = currentVersionName().contains("-dev", ignoreCase = true)

    /**
     * Fetches the latest release tag from GitHub, returning the version string with
     * any leading "v" stripped (e.g. "0.1.1"), or null on any network/parse error.
     */
    suspend fun fetchLatestVersion(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(GITHUB_RELEASES_URL).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val body = conn.inputStream.bufferedReader().readText()
                json.decodeFromString<GitHubRelease>(body).tagName.trimStart('v')
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /**
     * Returns true if [candidate] is strictly newer than [current] by semver ordering.
     * Non-conforming version strings return false.
     */
    fun isNewerVersion(current: String, candidate: String): Boolean {
        val c = parseSemver(current) ?: return false
        val n = parseSemver(candidate) ?: return false
        if (n.first != c.first) return n.first > c.first
        if (n.second != c.second) return n.second > c.second
        return n.third > c.third
    }

    private fun parseSemver(version: String): Triple<Int, Int, Int>? {
        val parts = version.substringBefore('-').split('.')
        if (parts.size != 3) return null
        val (maj, min, pat) = parts.map { it.toIntOrNull() ?: return null }
        return Triple(maj, min, pat)
    }
}

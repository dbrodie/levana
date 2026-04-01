# RFE: Update Checker

## Summary

Once per day, the app checks whether a newer release is available and notifies the user if one exists. The feature is designed to support multiple distribution channels (GitHub sideload, F-Droid, Play Store) and uses Android's install-source API to pick the right update strategy automatically.

## Motivation

Now that the release pipeline is in place, users who installed the APK directly from GitHub have no automatic way to learn about new versions. A background check eliminates the need for users to manually monitor GitHub releases.

## Behaviour

- Runs once per day via WorkManager (periodic, 1-day interval).
- On first run after app install or device boot, the worker is scheduled (but does not run immediately at boot — urgency is low).
- Detects the installation source:
  - **Null / ADB** → sideloaded APK → checks GitHub Releases API.
  - **`com.android.vending`** (Google Play) → Play handles updates; skip.
  - **`org.fdroid.fdroid`** (F-Droid) → F-Droid handles updates; skip (future: F-Droid API).
  - **Other** → unknown installer; skip conservatively.
- Skips silently for dev builds (versionName contains `-dev`).
- On network failure or non-200 response: returns success and retries tomorrow.
- Posts a notification when a newer version is found.
- Deduplicates: does not re-post a notification for the same version.
- Tapping the notification opens the GitHub releases page in the browser.

## Install-Source Detection

Uses `PackageManager.getInstallSourceInfo(packageName).installingPackageName` (API 30+, always available on minSdk 34). No permissions required for own package.

## GitHub Releases API

```
GET https://api.github.com/repos/dbrodie/levana/releases/latest
Accept: application/vnd.github+json
```

Response field used: `tag_name` (e.g. `"v0.1.1"`). Leading `v` is stripped before comparison.

## Version Comparison

Parses `major.minor.patch` semver. Any pre-release suffix (e.g. `-dev`, `-beta.1`) is stripped before numeric comparison. A candidate is "newer" only if it is strictly greater.

## Notification

- **Channel**: "App Updates" (`update_available`), `IMPORTANCE_DEFAULT`
- **Title**: "Update Available"
- **Body**: "Levana v{version} is available — tap to download"
- **Action**: Opens `https://github.com/dbrodie/levana/releases` in the browser
- **ID**: Fixed (9001) — repeated triggers replace the same notification

## Persistence

Two new keys in `PreferencesRepository` (not in `UserPreferences`):

| Key | Type | Purpose |
|---|---|---|
| `last_notified_update_version` | String | Version string last notified; prevents re-notification |

## Architecture

```
UpdateSource          (sealed interface — GitHubRelease, PlayStore, FDroid, Unknown)
UpdateChecker         (Koin singleton — detects source, fetches API, compares semver)
UpdateCheckWorker     (WorkManager CoroutineWorker + KoinComponent — orchestrates the daily check)
```

HTTP is handled via `java.net.HttpURLConnection` (built-in). JSON is parsed via `kotlinx-serialization-json` (already a project dependency). No new libraries are added.

## Files

**New:**
- `app/src/main/java/com/levana/app/update/UpdateSource.kt`
- `app/src/main/java/com/levana/app/update/UpdateChecker.kt`
- `app/src/main/java/com/levana/app/update/UpdateCheckWorker.kt`

**Modified:**
- `app/src/main/AndroidManifest.xml` — add `INTERNET` permission
- `app/src/main/java/com/levana/app/notifications/NotificationChannels.kt` — add `UPDATE_AVAILABLE` channel
- `app/src/main/java/com/levana/app/notifications/NotificationPoster.kt` — add `postUpdateAvailable()`
- `app/src/main/java/com/levana/app/data/PreferencesRepository.kt` — add update-check state
- `app/src/main/java/com/levana/app/di/AppModules.kt` — register `UpdateChecker`
- `app/src/main/java/com/levana/app/LevanaApplication.kt` — enqueue `UpdateCheckWorker`
- `app/src/main/java/com/levana/app/notifications/BootReceiver.kt` — re-enqueue on boot

## Out of Scope

- In-app update UI (dialog, banner) — notification only for now
- F-Droid or Play Store update-check implementations
- Forced update / minimum version enforcement
- Settings toggle to disable update notifications

# RFE: Fix install-source detection for sideloaded GitHub APKs

## Summary

A GitHub release APK installed by tapping it (from Chrome, a file manager, etc.) is reported as **"Unknown"** in Settings → About, and never gets the daily GitHub update check. Fix the detection so any install that did not come from Google Play or F-Droid counts as a GitHub release.

## Problem

`UpdateChecker.detectUpdateSource()` maps `PackageManager.getInstallSourceInfo().installingPackageName`:

| Installer | Before |
|---|---|
| `com.android.vending` | PlayStore |
| `org.fdroid.fdroid` | FDroid |
| `null` | GitHubRelease |
| anything else | Unknown (no update check) |

`null` happens almost only with `adb install`. Real users get the system package installer recorded instead.

### Observed on the emulator (API 36, Google APIs image)

`levana-v0.2.1.apk` was downloaded from the GitHub release, pushed to `Download/`, and installed by tapping it in the Files app (DocumentsUI):

```
installerPackageName=com.google.android.packageinstaller
initiatingPackageName=com.google.android.packageinstaller
originatingPackageName=com.android.shell
packageSource=3   (PACKAGE_SOURCE_DOWNLOADED_FILE)
```

About → Install Source showed **Unknown**.

For comparison, `adb install` gives `installerPackageName=null` and `initiatingPackageName=com.android.shell`.

`initiatingPackageName` is no better: it is the same system package installer for tap installs. `originatingPackageName` cannot be read by the app itself (it needs `INSTALL_PACKAGES`). So detection stays on `installingPackageName`, which is also the field Play and F-Droid set when they own the install.

## Behaviour

| Installer | After |
|---|---|
| `com.android.vending` | PlayStore (unchanged, no GitHub call) |
| `org.fdroid.fdroid` | FDroid (unchanged, no GitHub call) |
| anything else, including `null` | GitHubRelease (update check runs) |

### Why `Unknown` is removed

Levana ships only through GitHub Releases, Google Play and F-Droid. Any installer other than Play or F-Droid is therefore installing a GitHub-built APK, whatever app records itself as installer:

- system package installer (`com.google.android.packageinstaller` / `com.android.packageinstaller`)
- browsers and file managers on OEM ROMs that install through a session
- GitHub-tracking installers such as Obtainium
- `adb`

For all of these, the GitHub release is the right update source, so `Unknown` has no real case left and is removed. That also removes the `about_install_source_unknown` string (en + iw).

Accepted risk: an F-Droid build installed through an alternative F-Droid client (Droid-ify, Neo Store, F-Droid Basic) will now be treated as a GitHub install. Today those are "Unknown". Recognising them can be a follow-up if Levana is published on F-Droid.

## Design

- Add a pure function `UpdateSource.fromInstaller(installingPackageName: String?): UpdateSource` in `UpdateSource.kt`, which holds the mapping.
- `UpdateChecker.detectUpdateSource()` delegates to it.
- Remove `UpdateSource.Unknown` and its branches in `SettingsScreen.AboutScreen` and `UpdateCheckWorker`.

## Tests

`app/src/test/.../update/UpdateSourceTest.kt` (plain JUnit) covers:

- `null` (adb) → GitHubRelease
- `com.android.vending` → PlayStore
- `org.fdroid.fdroid` → FDroid
- `com.google.android.packageinstaller` and `com.android.packageinstaller` → GitHubRelease
- `com.android.chrome` → GitHubRelease
- a file manager (`com.google.android.documentsui`, `com.mixplorer`) → GitHubRelease
- Obtainium (`dev.imranr.obtainium`) → GitHubRelease

## Files

**Modified:**
- `app/src/main/java/ninja/doskey/app/levana/update/UpdateSource.kt`
- `app/src/main/java/ninja/doskey/app/levana/update/UpdateChecker.kt`
- `app/src/main/java/ninja/doskey/app/levana/update/UpdateCheckWorker.kt`
- `app/src/main/java/ninja/doskey/app/levana/ui/settings/SettingsScreen.kt`
- `app/src/main/res/values/localized_ui_strings.xml`, `values-iw/localized_ui_strings.xml`
- `specs/rfes/update-checker.md` (detection rules)

**New:**
- `app/src/test/java/ninja/doskey/app/levana/update/UpdateSourceTest.kt`

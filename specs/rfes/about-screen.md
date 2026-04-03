# About Screen

## Summary

Add an "About" entry at the bottom of the Settings screen that navigates to a read-only About screen showing the app version number and install source.

## Motivation

Users have no way to discover what version of the app they are running or where it was installed from. This is useful for bug reports, update awareness, and transparency about the distribution channel.

## Behaviour

### Settings screen
- A new "About" section header and list item appear at the very bottom of the Settings screen, always visible (not conditional).
- The item shows the label "About" with an info icon and a forward arrow.
- Tapping navigates to the About screen.

### About screen (`AboutRoute`)
- Top bar title: "About"
- Displays two read-only fields:
  - **Version** — the app's `versionName` from the package manager (e.g. `1.2.3` or `0.0.0-dev`)
  - **Install Source** — human-readable label derived from `UpdateChecker.detectUpdateSource()`:
    - `UpdateSource.PlayStore` → "Google Play"
    - `UpdateSource.FDroid` → "F-Droid"
    - `UpdateSource.GitHubRelease` → "GitHub Release"
    - `UpdateSource.Unknown` → "Unknown"
- No user interaction; back button returns to Settings.

## Implementation Notes

- No new ViewModel — data is read synchronously from `UpdateChecker` (already a Koin singleton).
- `AboutScreen` composable lives in `SettingsScreen.kt` alongside `DeveloperSettingsScreen`.
- `AboutRoute` added to `Routes.kt`.
- Navigation wired in `MainActivity.kt` following the same pattern as `DeveloperSettingsRoute`.

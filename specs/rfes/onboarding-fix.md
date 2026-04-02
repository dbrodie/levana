# RFE: Fix & Improve Onboarding

## Summary

Fix the GPS onboarding bug and simplify the onboarding screen to a single, clean location-selection step.

## Problems

### 1. GPS saves a static location instead of enabling GPS mode

When the user taps "Use My Location" during onboarding, the app:
- Gets the current GPS coordinates once
- Saves them as a `SavedLocation` in the database
- Sets `LocationMode.Saved(newId)`

This is wrong. The Settings screen correctly sets `LocationMode.Gps` when the GPS toggle is enabled. As a result, users who onboard via GPS end up with a static saved location that never updates, rather than live GPS tracking.

### 2. "Enter Coordinates" should not be in onboarding

The manual lat/lon entry screen has different fields than the rest of the location UX in the app and is not appropriate as a first-run option. It will remain accessible from Settings.

### 3. Poor UI copy and button order

The screen has no context about what Levana is, and lists "Choose a City" before "Use My Location" — the reverse of what most users would want.

## Solution

### GPS fix (`MainActivity.kt`, `GpsOnboarding` composable)

Replace:
```kotlin
val newId = preferencesRepository.addSavedLocation(loc)
preferencesRepository.setLocationMode(LocationMode.Saved(newId))
```

With:
```kotlin
preferencesRepository.updateGpsLocation(loc)
preferencesRepository.setLocationMode(LocationMode.Gps)
```

### `OnboardingScreen.kt` changes

- Remove `onManualEntry` parameter and the "Enter Coordinates" button
- Reorder: GPS first (filled `Button`), City second (`OutlinedButton`)
- New subtitle: "A Hebrew calendar and zmanim app for Orthodox Jewish life. To get started, set your location."

### `GpsOnboarding` in `MainActivity.kt`

Remove the `onManualEntry = { navController.navigate(ManualLocationRoute) }` argument since the screen no longer has that callback.

## Out of Scope

- `ManualLocationScreen` and `ManualLocationRoute` are kept; they remain reachable from Settings.
- No changes to city picker, settings screens, or notifications.

## Verification

1. Fresh install (or clear app data) → OnboardingScreen shows "Use My Location" and "Choose a City" only
2. Tap "Use My Location" → grant permission → navigates to Calendar → Settings > Location shows GPS toggle **on**
3. Tap "Choose a City" → pick a city → navigates to Calendar → Settings > Location shows the chosen city
4. Existing users (location already set) open directly to Calendar, unaffected

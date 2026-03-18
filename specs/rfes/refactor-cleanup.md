# Spec: Refactor & Edge Case Fixes

**Branch:** `refactor-cleanup`
**Status:** In progress

## Context

Deep review of the codebase after the multi-location feature landed. Found a mix of logic bugs,
dead code, reactivity gaps, and small inconsistencies. This pass addresses all of them before
the next feature work.

---

## Issues

### 1. Midnight calculation is wrong
`ZmanimRepository.midnight()` uses `czc.sunrise` (today's sunrise, already past) to compute the
midpoint — yielding ~noon, not midnight. Fix: use `czc.solarMidnight`.

### 2. LocationsSettingsScreen: sequential deletes can strand the user
After deleting the active location the `wasActive` guard misses subsequent deletes because
`locationMode` is already null. Drop the guard — just check whether the list is empty and GPS
is off after each delete.

### 3. DailyNotificationWorker: `handleHolidays` has unused `location` param; `handleFasts` hardcodes Israel flag
Remove the unused `location` param from `handleHolidays`. Pass `prefs.isInIsrael` to
`handleFasts` (add `prefs: UserPreferences` param) instead of hardcoding `false`.

### 4. PreferencesRepository: remove legacy single-location migration
No users exist on the pre-multi-location build. Remove `Keys.LOCATION_NAME/COUNTRY/LAT/LON/ELEV/TZ`,
the migration branch in `preferences` Flow, and the inline migration in `addSavedLocation`.

### 5. Introduce `LocationMode` sealed interface
Replace the `activeLocationId: String?` + `useCurrentLocation: Boolean` pair with a single
sealed interface `LocationMode` (variants: `Gps`, `Saved(id)`). `null` means unset → onboarding.
The `activeLocation` extension handles orphaned IDs by falling back to the first saved location.

**Files:** `LocationMode.kt` (new), `UserPreferences.kt`, `PreferencesRepository.kt`,
`SettingsState.kt`, `SettingsScreen.kt`, `SettingsViewModel.kt`, `MainActivity.kt`,
`CityPickerViewModel.kt`.

### 6. Gregorian calendar loading spinner flickers on month swipe
Gregorian shows spinner on every `isLoading` → flickers on swipes. Hebrew only shows spinner on
`isLoading && monthDays.isEmpty()` (first load only). Align Gregorian to match Hebrew behavior.

### 7. Dead code: `DayOfWeekHeader` composable + duplicated `daysOfWeek` lists
`DayOfWeekHeader` is tagged `@VisibleForTesting` but never called. Each grid also duplicates the
`daysOfWeek` list. Fix: extract `DAYS_OF_WEEK` file-level constant, extract a shared
`LazyGridScope.dayHeaderRow(locale)` extension used by both grids, remove `DayOfWeekHeader`.

### 8. `isSaving` in `CityPickerViewModel` never resets and is never read
Set to `true` on city selection but never set back to `false` and never read in the UI. Remove
the field from `CityPickerState` and its one set-site in the ViewModel.

### 9. `DayDetailViewModel` only reacts to `activeLocation` changes
`candleLightingOffset`, `selectedZmanim`, `isInIsrael`, and `showModernIsraeliHolidays` all
affect the day detail output but are not watched. Replace the `map { activeLocation }` with a
full `distinctUntilChanged()` on the entire `UserPreferences` object — it's a data class so
structural equality covers all fields.

### 10. No timeout on GPS in `MainActivity`
`fusedClient.getCurrentLocation()` can hang indefinitely on the `ON_RESUME` refresh path.
Wrap with `withTimeoutOrNull(15_000L)` — the onboarding GPS path already handles its own errors.

### 11. `Locale("he")` uses deprecated constructor
Replace with `Locale.forLanguageTag("he")` as a file-level constant `HEBREW_LOCALE` in
`CalendarScreen.kt`.

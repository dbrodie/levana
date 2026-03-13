# RFE: Multi-Location Support

## Summary

Allow users to maintain a list of saved locations and switch the active location from the sidebar.
All time-sensitive calculations (zmanim, candle lighting, notifications) use the active location.

## Motivation

Users who travel, have family in multiple cities, or care about times in more than one place
currently have no way to switch locations without overwriting their saved location.

## User Stories

- As a user, I can save multiple named locations.
- As a user, I can switch the active location from the navigation drawer.
- As a user, I can enable GPS to use my current location automatically.
- As a user, I can manage my saved locations from a dedicated settings sub-screen.
- As a user, deleting the active location auto-switches to the next; if none remain and GPS is off,
  I am sent to the city picker.

## Architecture

### New model: `SavedLocation`

```kotlin
// domain/model/SavedLocation.kt
@Serializable
data class SavedLocation(
    val id: String,          // UUID, stable across edits
    val location: Location   // existing Location model
)
```

### `Location.kt` change

Add `@Serializable` annotation.

### `UserPreferences` changes

Replace `val location: Location?` with four fields:

```kotlin
val savedLocations: List<SavedLocation> = emptyList()
val activeLocationId: String? = null   // null = GPS or first saved
val useCurrentLocation: Boolean = false
val gpsLocation: Location? = null      // cached last GPS result
```

Computed extension:

```kotlin
val UserPreferences.activeLocation: Location?
    get() = when {
        activeLocationId == null && useCurrentLocation -> gpsLocation
        activeLocationId != null -> savedLocations.find { it.id == activeLocationId }?.location
        useCurrentLocation -> gpsLocation
        else -> savedLocations.firstOrNull()?.location
    }
```

### `PreferencesRepository` changes

New DataStore keys (old single-location keys remain for migration only):

| Key | Type | Purpose |
|-----|------|---------|
| `saved_locations` | String (JSON) | Serialized `List<SavedLocation>` |
| `active_location_id` | String | ID of active location, absent = GPS/first |
| `use_current_location` | Boolean | GPS mode |
| `gps_location` | String (JSON) | Cached GPS `Location` |

**Migration**: if `saved_locations` absent but old `location_name` exists → create a `SavedLocation`
from old fields, write it, clear old keys.

New methods:
- `addSavedLocation(location: Location): String` — returns new UUID
- `removeSavedLocation(id: String)`
- `setActiveLocationId(id: String?)`
- `setUseCurrentLocation(enabled: Boolean)`
- `updateGpsLocation(location: Location)`

Removed: `saveLocation(location: Location)`

### New route: `LocationsSettingsRoute`

Added to `Routes.kt`.

### New screen: `LocationsSettingsScreen`

Composable in `ui/settings/SettingsScreen.kt`:

```
┌─────────────────────────────────────────┐
│  [Switch] Use Current Location          │
│           Automatically detect location │
│  [if on] GpsFixed icon | city name      │
├─────────────────────────────────────────┤
│  Saved Locations                        │
│   LocationOn | New York       [Delete]  │
│   LocationOn | Tel Aviv       [Delete]  │
├─────────────────────────────────────────┤
│  [ + Add Location ]                     │
└─────────────────────────────────────────┘
```

- Delete: `Icons.Outlined.Delete`; if deleted location was active → switch to first remaining;
  if list becomes empty and GPS off → navigate to CityPickerRoute
- Add Location → navigates to `CityPickerRoute`

### Settings screen

Location row `onChangeLocation` → navigates to `LocationsSettingsRoute` instead of `CityPickerRoute`.

### Sidebar (MainActivity)

Replace single location `NavigationDrawerItem` with a header + item block:

```
LOCATIONS  [edit icon → LocationsSettingsRoute]
  ○ Current Location   (GpsFixed icon, shown if useCurrentLocation=true)
  ● Jerusalem          (LocationOn, active)
  ○ New York           (LocationOn, not active)
```

- Tap item → `setActiveLocationId(id)` (or `null` for GPS), drawer closes.
- GPS sentinel ID: `"__gps__"`.

### GPS foreground refresh

In `MainActivity`, `LifecycleEventObserver` on `ON_RESUME`: if `useCurrentLocation == true`,
call `locationService.getCurrentLocation()` and `preferencesRepository.updateGpsLocation(result)`.
Wrapped in try/catch — permission failures are silently skipped.

### CityPickerViewModel

`SelectCity` → `addSavedLocation(city)` + `setActiveLocationId(newId)` instead of `saveLocation`.

### ManualLocationScreen callback

Same: add + set active instead of replace.

## Files Changed

| File | Change |
|------|--------|
| `domain/model/Location.kt` | Add `@Serializable` |
| `domain/model/UserPreferences.kt` | Replace `location`; add 4 fields + `activeLocation` extension |
| `domain/model/SavedLocation.kt` | New file |
| `data/PreferencesRepository.kt` | New keys, migration, new methods, remove `saveLocation` |
| `ui/navigation/Routes.kt` | Add `LocationsSettingsRoute` |
| `MainActivity.kt` | Sidebar rewrite; GPS refresh; new route composable |
| `ui/settings/SettingsScreen.kt` | Location row → LocationsSettings; add `LocationsSettingsScreen` |
| `ui/location/CityPickerViewModel.kt` | add + set active instead of replace |
| `ui/zmanim/ZmanimViewModel.kt` | `prefs.location` → `prefs.activeLocation` |
| `ui/daydetail/DayDetailViewModel.kt` | `prefs.location` → `prefs.activeLocation` |
| `ui/settings/SettingsViewModel.kt` | `locationName` from `prefs.activeLocation` |
| `notifications/DailyNotificationWorker.kt` | `prefs.location` → `prefs.activeLocation` |

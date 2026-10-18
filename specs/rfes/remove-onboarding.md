# RFE: Remove Onboarding — Always-On Current Location

## Summary

Drop the onboarding screen. The app always has a **current location** (from the device) plus an
optional list of **saved locations**. One of them is active; by default it is the current location.
This removes the "no location yet" state, the GPS on/off toggle, and the `LocationMode` model.

## Motivation

Onboarding only existed to answer "where are you?". If the current location is always present,
the app can open straight to the calendar, and location settings shrink to "add/remove cities".

## Behaviour

- First launch opens the calendar directly. If location permission isn't granted, the app asks
  for it once per launch (Android stops showing the dialog after repeated denials).
- **Current location** = last device fix. Until there is one (no permission, no fix yet), it falls
  back to the first bundled city in the device's time zone, else Jerusalem. The fix is refreshed
  on every resume when permission is granted.
- **Saved locations** are added from the city picker and removed from the Locations screen.
- **Active location** = the selected saved location, or the current location when nothing is
  selected. Deleting the active saved location falls back to the current location — there is no
  "no locations left" state.
- Drawer city switcher: "Current location" row + one row per saved location.
- Locations screen: current location row (with a "Allow location access" action when permission
  is missing), saved locations with delete, "Add location". The "Use current location" switch is gone.

## Storage (DataStore)

| Key | Type | Meaning |
|-----|------|---------|
| `gps_location` | JSON `Location` | last current-location fix (key name kept so existing fixes survive) |
| `saved_locations` | JSON `List<SavedLocation>` | unchanged |
| `active_location_id` | String | selected saved location id; absent = current location |

Removed: `location_mode`. A DataStore migration converts a stored `{"type":"saved","id":X}` mode
into `active_location_id = X` and deletes the old key; GPS mode becomes "absent".

## Model

```kotlin
data class UserPreferences(
    val currentLocation: Location = Location.JERUSALEM,
    val savedLocations: List<SavedLocation> = emptyList(),
    val activeLocationId: String? = null,
    ...
)
val UserPreferences.activeLocation: Location   // non-null
```

`LocationMode` is deleted. `activeLocation` is never null, so the `?: Location.JERUSALEM` /
`?: return` fallbacks in view models and the notification worker go away.

## Removed code

- `ui/onboarding/OnboardingScreen.kt`, `OnboardingRoute`, `GpsOnboarding` in `MainActivity`
- `domain/model/LocationMode.kt`
- `ManualLocationScreen` + `ManualLocationRoute` (already unreachable — nothing navigated to it)
- `PreferencesRepository.setLocationMode`; replaced by `setActiveLocation(id: String?)`
- Onboarding/manual-entry/GPS-toggle strings (en + iw)
- `LocationsSettingsScreen`'s `onNoLocations` callback; city picker always pops back

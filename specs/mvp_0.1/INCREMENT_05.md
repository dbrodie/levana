# Increment 05: Location Management

## Overview

Add location selection and persistence so calendar calculations (and
future zmanim) use the correct geographic position and timezone.

## What We Build

### Data layer
- **Bundled city database** — JSON asset file (`cities.json`) with ~100 cities
  that have significant Jewish populations. Each entry:
  `{ name, country, latitude, longitude, elevation, timezoneId }`
- **`CityRepository`** — loads cities from the asset, exposes search
- **`PreferencesRepository`** — wraps Jetpack DataStore to persist
  `UserPreferences` (selected location, future settings)

### Domain
- Extend existing `Location` model (already has lat, lon, timezoneId, name)
  with `elevation` and `country` fields
- `UserPreferences` data class: `location: Location?`

### GPS
- `LocationService` — wraps `FusedLocationProviderClient`
- Requests `ACCESS_FINE_LOCATION`, falls back to `ACCESS_COARSE_LOCATION`
- Reverse-geocode to get a display name

### UI
- **Onboarding screen** — shown on first launch when no location is saved.
  Three options: pick a city, use GPS, enter manually
- **City picker screen** — searchable list of cities
- **Manual entry** — simple form with lat, lon, timezone fields
- **App bar** — show location name; tap → city picker

### Integration
- `CalendarRepository` accepts `Location` parameter so Hebrew date
  calculations respect the correct timezone
- `CalendarViewModel` observes `UserPreferences` flow

## Dependencies

- `com.google.android.gms:play-services-location` (GPS)
- `androidx.datastore:datastore-preferences` (persistence)
- Already have: Koin, Navigation Compose, KosherJava

## Acceptance Criteria

- [ ] First launch shows location onboarding (not the calendar)
- [ ] City search filters the list as user types
- [ ] Selecting a city saves it and proceeds to calendar
- [ ] GPS button requests location permission and detects location
- [ ] Manual entry accepts lat/lon and saves
- [ ] Location persists across app restarts
- [ ] Calendar/date calculations update to reflect selected timezone
- [ ] Location name shown in app bar
- [ ] `./gradlew build` passes

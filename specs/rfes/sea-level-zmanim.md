# Sea-level sunrise and sunset

## Goal

Calculate the displayed sunrise and sunset using KosherJava's sea-level methods,
without applying the saved location elevation.

## Scope

- Change `ZmanimRepository`'s displayed Sunrise and Sunset entries to use
  `seaLevelSunrise` and `seaLevelSunset`.
- Change the sunset helper used by notifications to use sea-level sunset as
  well, keeping all sunset-dependent behavior consistent.
- Keep latitude, longitude, timezone, and elevation storage unchanged.

## Verification

- Add a unit test demonstrating that the repository returns the sea-level
  sunrise and sunset for an elevated location.
- Run the debug unit-test task.

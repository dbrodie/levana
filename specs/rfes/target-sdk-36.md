# Target SDK 36

**Status:** Approved by the owner

## Problem

The v0.2.0 release was rejected by Google Play when the internal-testing edit was committed: "Target SDK of artifact is too low". The app targets API 35, and Play now requires app updates to target API 36 (Android 16). v0.1.8 was accepted with API 35 before the requirement took effect.

## Change

- `compileSdk` and `targetSdk` go from 35 to 36.
- Android Gradle Plugin 8.7.3 supports at most API 35, so it is upgraded to 8.13.2 (the latest 8.x), with the Gradle wrapper raised to the version it requires.
- Test tooling (Robolectric) is raised if needed so unit tests run against API 36.
- No app code changes are expected. If the build or Android 16 behaviour changes require any, they are kept minimal and listed in the commit.

## Verification

Owner-approved light check: build and unit tests pass, and a quick emulator smoke test (launch, onboarding, calendar, Settings, back navigation) shows nothing broken. Then release as v0.2.1 and confirm it appears on the Play internal testing track.

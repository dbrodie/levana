---
id: TASK-53.2
title: >-
  Remove Google Play Services location; use the platform LocationManager;
  disable the dependency-info block
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:25:00Z'
updated_date: '2026-10-30T12:37:00Z'
labels: []
dependencies: []
parent_task_id: TASK-53
priority: high
type: chore
ordinal: 63000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
F-Droid blockers 2 and 3 (see TASK-53).
- Replace `play-services-location` (FusedLocationProviderClient in data/LocationService.kt) with the platform `LocationManager.getCurrentLocation()` (minSdk 34). Use the best available provider (FUSED_PROVIDER where present, else NETWORK, then GPS), with the existing timeout, cancellation and fallback behaviour, and keep the TASK-46 rule: the position's zone follows the device zone. Remove the dependency entirely, and verify that no gms or firebase classes remain in the release APK (dexdump or apkanalyzer).
- Set `dependenciesInfo { includeInApk = false; includeInBundle = false }`.
- Check that the release APK and AAB still build and that Play upload still works.
- Emulator check: the location permission, the first fix, no-permission fallback and the timeout. Test with and without Google Play services if a non-GMS image is available.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 No com.google.android.gms or other proprietary classes in the release APK; location works as before on GMS and non-GMS images; the dependency-info block is absent; all tests and Roborazzi baselines pass
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extract the fix logic into data/LocationFix.kt: a small LocationSource interface (providers, isEnabled, lastKnown, suspend current(provider) with CancellationSignal) and a pure PlatformFixer that (a) orders candidate providers FUSED -> NETWORK -> GPS, keeping only those that exist and are enabled; (b) uses the newest lastKnown fix from those providers if it is <= 2 min old; (c) otherwise asks each provider in turn for getCurrentLocation, moving on when one returns null or throws (SecurityException etc.); (d) the whole thing runs under the existing 15 s timeout; cancellation propagates and cancels the platform request via CancellationSignal.
2. LocationService: replace FusedLocationProviderClient with a LocationManagerSource (LocationManager.getCurrentLocation(provider, CancellationSignal, direct executor, consumer), getLastKnownLocation, isProviderEnabled/getAllProviders). Keep hasPermission, PERMISSIONS, no-permission -> null, failure -> null, the TASK-46 zone stamp (DeviceContext.realZone) and the reverse geocoder unchanged. Public API unchanged so callers (AppStartupEffects, LocationsSettingsScreen, WalkthroughViewModel) don't change.
3. Remove play-services-location from libs.versions.toml and app/build.gradle.kts. Add android { dependenciesInfo { includeInApk = false; includeInBundle = false } }.
4. Unit tests (plain JUnit + coroutines test virtual time): provider order and filtering (fused absent/disabled, none enabled), recent vs stale last-known, null/exception fallback to the next provider, timeout -> null, cancellation cancels the request, plus a Robolectric LocationService test (no permission -> null; ShadowLocationManager fix -> Location in the device zone).
5. Verify: assembleDebug assembleRelease bundleRelease testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks (baselines unchanged). Release APK: dexdump/apkanalyzer scan for com.google.android.gms / firebase / com.google.android.play classes; signing-block ID scan (0x504b4453 dependency-info absent); AAB without BUNDLE-METADATA dependencies.pb.
6. Emulator (android-debug skill): Google APIs image - grant + first fix via adb emu geo fix; deny -> fallback city; timeout with no fix (location off / no provider fix); zone-mismatch notice with Jerusalem zone + Ukiah fix. Repeat the fix on a non-GMS AOSP image if one is installed.
7. Evidence outside the worktree with a README; record results, Play notes and limitations in the task notes; commit.
<!-- SECTION:PLAN:END -->

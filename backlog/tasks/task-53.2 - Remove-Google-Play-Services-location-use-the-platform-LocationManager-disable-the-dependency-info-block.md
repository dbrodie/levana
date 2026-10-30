---
id: TASK-53.2
title: >-
  Remove Google Play Services location; use the platform LocationManager;
  disable the dependency-info block
status: To Do
assignee: []
created_date: '2026-10-30T12:25:00Z'
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

---
id: TASK-53.2
title: >-
  Remove Google Play Services location; use the platform LocationManager;
  disable the dependency-info block
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T12:25:00Z'
updated_date: '2026-10-31T16:33:00Z'
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation (@claude; code 2929b4e→48eb6b9 after rebase onto origin/master 0c345af)
- data/LocationFix.kt: `LocationSource` interface + pure `LocationFixer`. Usable providers = FUSED → NETWORK → GPS, filtered to those present and enabled (provider errors skipped). The newest last-known fix from those providers is used if ≤ 2 min old (elapsedRealtimeNanos). Otherwise each provider is asked in turn for a current fix, moving on when one returns null or throws. All of it runs under the existing 15 s timeout; caller cancellation propagates.
- data/LocationService.kt: `PlatformLocationSource` over LocationManager: getCurrentLocation(provider, LocationRequest QUALITY_HIGH_ACCURACY with duration 15 s, CancellationSignal cancelled from invokeOnCancellation, direct executor). Public API unchanged (hasPermission, PERMISSIONS, getCurrentLocationOrNull). No permission → null, failure → null, TASK-46 zone stamp and reverse geocoder unchanged; callers untouched.
- play-services-location removed from libs.versions.toml and app/build.gradle.kts. `android { dependenciesInfo { includeInApk = false; includeInBundle = false } }`.
- proguard-rules.pro: `-dontwarn javax.annotation.processing.**`. R8 failed with missing javax.annotation.processing classes, referenced by ACRA's auto-service processor, which ACRA puts on the runtime classpath. play-services-basement's consumer rule `-dontwarn javax.annotation.**` used to hide this.
- THIRD_PARTY_NOTICES.md regenerated with scripts/generate_third_party_notices.py: 168 libraries, 0 proprietary. The Play Services section and its 4 rows are gone.
- Emulator finding fixed in 48eb6b9: the default getCurrentLocation request is BALANCED, and GMS fused then never turned on GPS (15 s, 0 locations). The old client asked for PRIORITY_HIGH_ACCURACY, so the request now asks for QUALITY_HIGH_ACCURACY.
- Tests: LocationFixerTest (12, plain JUnit): order; missing/disabled providers; location off → null with no request; first provider wins; recent last-known used (newest), stale or disabled-provider last-known ignored; null/SecurityException fall through to the next provider; no fix; provider list throws; hung provider → null at the timeout and the request cancelled; caller cancellation propagates and cancels the request. LocationServiceTest (3, Robolectric ShadowLocationManager): no permission → null; recent last-known fix → Location named by the geocoder in the device zone; fresh fix comes from the fused provider via getCurrentLocation.

## Verification (final commit 48eb6b9)
Evidence: /tmp/levana-task-53-2/ (README.md).
- `./gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks` from a clean app/build/test-results: BUILD SUCCESSFUL. 1066 tests, 0 failures/errors. Lint passes. Roborazzi 268 total, 268 unchanged (0 changed/added). Release APK and AAB build (debug signing fallback; no local release keystore).
- Release APK dexdump (all classes*.dex, 4873 classes): 0 com/google/android/{gms,play,datatransport,finsky}, com/google/firebase, com/google/android/libraries, billing or ads classes; 0 dex strings matching gms or firebase; no gms in the manifest. Baseline v0.4.2 has 16 gms classes, 22 gms strings, and GoogleApiActivity + com.google.android.gms.version in its manifest, so the scan would catch them.
- APK Signing Block IDs: only 0x7109871A (v2) and 0x42726577 (verity padding); no 0x504B4453 dependency-info block (v0.4.2 had one, 9264 bytes). apksigner verify passes. AAB BUNDLE-METADATA has no dependencies.pb.
- Emulator (Levana_Agent_Template_API_36_1, google_apis_playstore API 36.1, zone Asia/Jerusalem):
  - Grant: walkthrough → Use my location → While using the app → "Location allowed". First fix: fused HIGH_ACCURACY request, fix delivered, stored Haifa in Asia/Jerusalem.
  - Zone mismatch: GPS Ukiah → fresh fused fix stored as Ukiah, key Asia/Jerusalem|-8, the dialog shows.
  - Timeout: gps + fused as silent test providers → request registered 15:35:02.879 and removed 15:35:17.877 (15.0 s); the stored location is unchanged; no crash.
  - No fused provider (fused test provider disabled): the request goes to the gps provider (network is off on this image) → Tel Aviv-Jaffa stored.
  - Deny: pm clear → Don't allow → both permissions granted=false, no request, no gps_location; panel 5:53/4:51/4:33 and active city Jerusalem (fallback city).
  - Release APK 48eb6b9 (R8): fused HIGH_ACCURACY fix in ~4 s, mismatch dialog shows.
- Limitations:
  - No non-GMS (AOSP) system image or sdkmanager on this machine, so the closest check is "fused disabled, platform GPS only" above. A real AOSP/microG check is still open if wanted; it needs the owner to OK a system-image download into the SDK.
  - The host was overloaded by parallel agents (load 40–60). The first emulator session's system_server kept hitting ANRs and was restarted.
  - Grant, timeout and deny ran on debug builds whose location code equals 48eb6b9 except for the high-accuracy change. The deny and timeout paths don't depend on it.
- Behaviour note: a last-known fix up to 2 min old is reused, so a position change within 2 minutes of the previous fix is picked up on the next resume after that.

## Play
Nothing needs doing in Play Console. Same applicationId, signing and permissions (ACCESS_FINE/COARSE_LOCATION unchanged); Play app signing doesn't need the dependency-info block. The only visible effect is the Console's 'Dependencies' / SDK insights for this release, which will be empty. The Data safety answers are unchanged: location stays on the device, and the published privacy policy already says 'your device's location services', which still holds. Upload was not exercised here (CI tag workflow).
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-31T16:33:00Z

---

Coordinator integration 2026-10-31: merged as 68941a2. Clean run (with --continue): build incl. assembleRelease OK, 1116 tests 0 failures, Roborazzi 320 unchanged; release APK contains 0 com/google/android/gms references; THIRD_PARTY_NOTICES lists no proprietary libs. One earlier run hit the known DatePickerScreenshotTest load timeout. Limitation: no non-GMS (AOSP) emulator image available; GPS-only fallback (fused disabled) verified instead. AC #1 left unchecked for the non-GMS part pending owner decision on downloading an AOSP image.

---
<!-- COMMENTS:END -->

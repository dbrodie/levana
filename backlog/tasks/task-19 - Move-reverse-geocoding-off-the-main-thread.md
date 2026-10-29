---
id: TASK-19
title: Move reverse geocoding off the main thread
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-21T11:19:37Z'
updated_date: '2026-10-29T19:08:00Z'
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-3ay
priority: medium
type: chore
ordinal: 19000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — remaining findings: Move reverse geocoding off the UI thread (Open)

Run the remaining synchronous LocationService reverse-geocoder call away from the UI thread. TASKS.md says provider/file IO is already off-main; do not reopen that broad completed item.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-background-provider-io.md
- specs/rfes/remove-onboarding.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Reverse geocoding does not block the main thread. Cancellation, timeout, missing results, and failure preserve usable location fallback behavior.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Root cause: LocationService.reverseGeocode() calls the blocking Geocoder.getFromLocation() inside the fused-location success listener, which GMS invokes on the main thread.
2. Split the fix from naming: the fused-location coroutine resumes with coordinates only; naming happens afterwards in the caller's coroutine.
3. Add a ReverseGeocoder seam (fun interface, suspend name lookup). The Android implementation uses the non-blocking Geocoder.getFromLocation(lat, lon, 1, GeocodeListener) API (API 33+, minSdk is 34) wrapped in suspendCancellableCoroutine; it returns locality/subAdminArea/adminArea or null, and reports Geocoder.isPresent()==false as null.
4. resolvePlaceName(geocoder, lat, lon, timeout): bounded by a timeout; null result, timeout, or any failure -> "Current Location" fallback; caller cancellation is rethrown (the caller is gone, nothing to use).
5. Coordinates are kept on every fallback path, so the location update still applies.
6. Unit tests for resolvePlaceName: success, null/missing result, failure, timeout (hung geocoder), caller cancellation propagates.
7. Verify with assemble/test/lint and an emulator runtime check (fix + name on resume, no StrictMode/ANR).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: 892accd (code); notes committed on top.

Change: LocationService no longer calls the blocking Geocoder.getFromLocation() in the fused-location success listener (which GMS runs on the main thread). The fix coroutine now resumes with coordinates only; naming runs afterwards through the new ReverseGeocoder seam (data/ReverseGeocoder.kt). AndroidReverseGeocoder uses the async Geocoder.getFromLocation(..., GeocodeListener) API (API 33+, minSdk 34), so no thread is blocked. resolvePlaceName() applies a 5s timeout; null/blank result, onError, an exception or a timeout all give the 'Current Location' fallback while keeping the fix's coordinates. Caller cancellation is rethrown. Callers (MainActivity resume, SettingsScreen permission grant) are unchanged.

Verification:
- ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug -> BUILD SUCCESSFUL; 661 tests, 0 failures, 0 errors. New ResolvePlaceNameTest (6 tests): geocoded name, missing result, blank result, failure, hung geocoder timeout, caller cancellation propagates.
- Runtime on own emulator (Levana_Agent_Template_API_36_1, read-only, emulator-5612), debug APK from 892accd, location permission granted:
  * First launch, geo fix Jerusalem: GMS logged 'GmsGeocoder: reverse geocoding network failure' -> stored gps_location {lat 31.768, lon 35.214, name 'Current Location'} (failure fallback keeps coordinates).
  * Background -> geo fix Tel Aviv -> resume: stored gps_location {lat 32.085, lon 34.782, name 'Tel Aviv-Jaffa'} (success path).
  * No Choreographer 'Skipped frames', StrictMode, ANR or FATAL lines in the app process during the resume cycle. Logcat: /tmp/task19-resume-logcat.txt; emulator log: /tmp/task19-emulator.log.

Limitations: the timeout and cancellation paths are covered by unit tests only (cannot force a hung GMS geocoder on the emulator). No StrictMode instrumentation was added; the off-main claim rests on using the async Geocoder API plus the clean frame log.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-3ay",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:followup-geocoder",
  "metadata": {
    "import_key": "followup-geocoder",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — remaining findings: Move reverse geocoding off the UI thread (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```

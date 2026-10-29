---
id: TASK-19
title: Move reverse geocoding off the main thread
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-21T11:19:37Z'
updated_date: '2026-10-29T19:03:00Z'
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
- [ ] #1 Reverse geocoding does not block the main thread. Cancellation, timeout, missing results, and failure preserve usable location fallback behavior.
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

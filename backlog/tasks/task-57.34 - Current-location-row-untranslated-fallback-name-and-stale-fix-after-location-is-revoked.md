---
id: TASK-57.34
title: >-
  Current-location row: untranslated fallback name and stale fix after location
  is revoked
status: To Do
assignee: []
created_date: '2026-11-02T14:28:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
type: bug
ordinal: 104000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-57.23 (location states). No crash.
1. **Done in TASK-44:** the fallback name is now the string resource current_location_fallback, resolved when read in the app language; only item 2 (stale fix after revoke) remains open here. Original report: offline (airplane mode) the reverse geocoder fails and the current location is named 'Current Location', the hard-coded FALLBACK_PLACE_NAME (data/ReverseGeocoder.kt:45). It is not a string resource, so it stays English in the Hebrew UI, in the sidebar, Cities and widgets. Evidence: /Users/daniel/.paseo/evidence/task-57.23/57-cities-offline-geocoder.png, /Users/daniel/.paseo/evidence/task-57.23/74-he-cities-permdenied.png.
2. After location permission is revoked (pm revoke or system Settings), Cities keeps showing the last GPS fix's name ('القدس', later 'Current Location') with the subtitle 'Based on your time zone. Tap to allow location access.' The subtitle depends only on the permission (LocationsSettingsScreen.kt) while the name and coordinates are the stored last fix, so the row claims a time-zone city it isn't showing. Evidence: /Users/daniel/.paseo/evidence/task-57.23/55-cities-location-revoked.png, /Users/daniel/.paseo/evidence/task-57.23/59-restored-after-ui-revoke.png.
3. Related, already filed: the Arabic name for Jerusalem is TASK-44 (reproduced again: /Users/daniel/.paseo/evidence/task-57.23/46-notif-revoked-fg.png widget header '· القدس'); the fallback city looking like a success state is TASK-57.17 (L1). With permission granted but no fix yet, the row shows the time-zone city 'Jerusalem' with no subtitle at all (/Users/daniel/.paseo/evidence/task-57.23/28-cities-after-grant.png) — 'location unavailable' has no visible state; include it in the 57.17 design.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The fallback place name is a translated string resource (done in TASK-44)
- [ ] #2 After the permission is revoked the row's name and subtitle agree (either the stored fix is labelled as last known, or the time-zone city is used)
<!-- AC:END -->

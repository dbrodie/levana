---
id: TASK-57.23
title: Verify empty, error, permission and loading states as part of testing
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-02T13:43:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: task
ordinal: 93000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The review lists error/empty/denied/loading states that are not drawn in Figma. The owner's goal (2026-11-02) is verification, not design: confirm the app works and does not crash when permissions are denied or revoked, and that the listed states behave sensibly. Where a missing or broken screen/state is found, raise it as a separate task (design can follow there); don't design or build new screens in this task.

What to verify (on an emulator, English and Hebrew UI):
- Permissions — for each of location (fine/coarse), calendar (READ_CALENDAR), contacts (READ/WRITE_CONTACTS) and notifications (POST_NOTIFICATIONS): never asked, denied once, permanently denied ("Don't ask again"), and granted-then-revoked from system Settings while the app is in the background and while it is open (Android kills and restores the process on revoke). Check every screen/feature that uses the permission, the widgets/tile, and scheduled reminders. No crash, no ANR, no dead button; note where the UI gives no way forward (e.g. a button that can't reopen the system prompt).
- Empty states: Agenda empty, Events empty, Agenda search no results, city search no match, saved cities empty.
- Error/edge states: location unavailable, zmanim that can't be computed (polar latitudes, e.g. a far-north custom location in summer/winter), contacts import failure, no network for any network feature.
- Loading: first launch, calendar provider query.

Output: a results table in the task notes (state → result → evidence), crash logs if any, and a new task for each crash (bug) or missing/poor state (design follow-up). Small crash fixes may be filed rather than fixed here.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **ST1** (High) — No error states exist anywhere: location unavailable, zmanim unavailable at high latitudes (spec can't compute some zmanim in polar summers), import failure, crash-report dialog (Developer has "Test crash" but the dialog isn't designed), no network for update check. *Reviewer suggestion:* Design an error pattern (inline banner + snackbar) and the crash-report dialog.
- **ST2** (Medium) — Missing empty states: Agenda empty, Events tab empty, Search no-results, Cities search no-match, Saved cities empty. *Reviewer suggestion:* Add, reusing the existing birthdays empty-state pattern.
- **ST3** (Medium) — Missing "permanently denied" permission variants (location, contacts, calendar, notifications). *Reviewer suggestion:* Add "Open settings" variants.
- **ST4** (Low) — No loading/skeleton states (first launch computing, calendar provider query). *Reviewer suggestion:* Add minimal shimmer or progress on Agenda/System calendars.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each permission (location, calendar, contacts, notifications) is verified never-asked, denied, permanently denied and revoked-at-runtime, with no crash or ANR
- [ ] #2 Each listed empty, error and loading state is checked and recorded with evidence
- [ ] #3 A separate task is filed for every crash found and every missing or poor state that needs design
<!-- AC:END -->

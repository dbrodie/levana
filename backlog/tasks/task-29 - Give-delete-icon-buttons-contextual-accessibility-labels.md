---
id: TASK-29
title: Give delete icon buttons contextual accessibility labels
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-23T05:53:45Z"
updated_date: "2026-10-23T06:35:27Z"
labels:
  - accessibility
dependencies: []
references:
  - levana-q5h
priority: low
type: bug
ordinal: 29000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: accessibility source review of master 1649ccb, EventsScreen.kt birthday delete around line 485 and custom-event delete around line 614, plus SettingsScreen.kt saved-location delete around line 289. Each separately focusable action currently has only a generic Delete contentDescription, making its target unclear when navigating actions.

Owner clarification: longer labels are ACCESSIBILITY ONLY. Keep visible trash icons and existing confirmation dialogs unchanged; do not add long visible button text. Use localized content descriptions identifying the target, e.g. Delete birthday for Sarah, Delete event <title>, Delete saved location Jerusalem, with corresponding Hebrew resources and safe formatting of user text. Preserve the distinction between birthday removal and personal-event deletion where wording requires it.

Use a small shared delete-icon component or consistent helper if useful; avoid a generalized row redesign. Retain separate edit/open and delete actions: they are legitimately different, unlike duplicate handlers for one checkbox. Row-level custom accessibility actions may be evaluated for long lists but are optional and must not duplicate actions or remove normal pointer/keyboard access.

Verify names and roles in semantics, correct target callbacks, confirmations/cancellation, English/Hebrew RTL and unchanged visible layout. Actual TalkBack announcements require device verification rather than inferring speech from XML.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Birthday, custom-event and saved-location delete buttons have localized accessibility-only labels naming their targets. Visible trash icons, layout, confirmation dialogs and delete behavior remain unchanged. Edit/open and delete remain distinct and operable. Tests verify target-specific labels and callbacks, and runtime verification records English/Hebrew behavior and TalkBack limitations.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
IMPLEMENTED/VERIFIED — awaiting parent integration (do not close yet).
Branch fix/delete-action-a11y, worktree /Users/daniel/.paseo/worktrees/15o7vaq5/delete-action-a11y. Commits: 2de4dbe spec, 2e8451a fix, 0880e91 verification docs (head 0880e91).
Change: accessibility-only contentDescriptions via new res values{,-iw}/accessibility_q5h_strings.xml (a11y_remove_birthday_for, a11y_delete_event, a11y_delete_saved_location; user text only as %1$s arg; blank name falls back to common_delete/location_delete) and helper ui/TargetedLabel.kt. EventsScreen BirthdayCard/EventCard + SettingsScreen saved-location delete only; BirthdaysTab, CustomEventsTab, LocationsSettingsContent private->internal for tests. Icons, layout, dialogs, callbacks unchanged.
Tests: DeleteActionLabelsTest 10/10, SavedLocationDeleteLabelTest 3/3; ./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest -> BUILD OK, 538 tests 0 failures 0 errors; lint 0 errors (84 pre-existing warnings, none in touched files).
Emulator emulator-5556, owned session /tmp/levana-emu-hkwefswd (stopped), artifacts /tmp/levana-emu-hkwefswd/artifacts (commands.log, UI XML, screenshots, TalkBack logcat). EN+HE/RTL labels in hierarchy, cancel/confirm target correct row, keyboard Enter activation, persistence across relaunch. TalkBack verbose speech log: 'Remove Hebrew birthday for Sarah Cohen. Button', 'Delete event Siyum "Bava" 100%. Button', 'Delete saved location Tel Aviv. Button', HE 'מחיקת המיקום השמור Tel Aviv. Button'.
Limitations: speech from TalkBack log not audio; Hebrew TalkBack only for saved location; TalkBack touch gestures not exercised (adb taps bypass explore-by-touch). Emulator host heavily loaded (system ANR dialogs at boot, dismissed).
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-q5h",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T05:55:51Z",
  "closed_at": "2026-10-23T06:35:27Z",
  "close_reason": "Reviewed, emulator-verified and merged/pushed to origin/master at 6a75ee8 (first three at 296c6c2). Final combined verification: 576 unit tests, zero failures/errors/skips; lintDebug and assembleDebug passed. English/Hebrew RTL and runtime evidence preserved under individual emulator session artifacts. TalkBack speech logs inspected. Remaining keyboard focus/manual gesture and checkbox speech issues tracked in levana-wf2 and levana-bhe; pre-existing noon/plural wording bugs tracked in levana-851.",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 3
}
```

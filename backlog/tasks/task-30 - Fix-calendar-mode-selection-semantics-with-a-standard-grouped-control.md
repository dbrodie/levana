---
id: TASK-30
title: Fix calendar-mode selection semantics with a standard grouped control
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-23T05:53:47Z"
updated_date: "2026-10-23T06:35:27Z"
labels:
  - accessibility
dependencies: []
references:
  - levana-5xh
priority: low
type: bug
ordinal: 30000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: accessibility source review of master 1649ccb, CalendarScreen.kt CalendarMonthHeader around lines 379–384. Hebrew/Gregorian mode is implemented as two hand-styled Text controls with Role.Tab and selected semantics, but the currently selected option is also disabled via clickable(enabled = !selectedHebrew / selectedHebrew). That conflates selected with unavailable, and the parent lacks selection-group semantics.

Prefer Material 3 SingleChoiceSegmentedButtonRow and SegmentedButton for this single-choice mode selection, or retain the approved appearance with a reusable grouped selectable implementation if the standard component would materially alter layout. Selected state must not itself mean disabled. Avoid accidental inversion when tapping the already selected mode: use an explicit mode callback or guard no-op rather than blindly toggling. Preserve both modes, current selection persistence, Hebrew/English labels, RTL order, adequate touch targets and header sizing.

Verify selected/unselected state, group/role semantics, keyboard/TalkBack navigation, idempotent activation of the current option and exactly one switch when choosing the other mode. No calendar calculation or date-selection changes are requested. Runtime spoken output is not yet verified.

Official guidance: https://developer.android.com/develop/ui/compose/components/segmented-button
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Calendar mode uses a standard single-choice grouped control or an equivalent reusable selectable implementation with correct role/group/selected semantics. The selected option is not misleadingly disabled; activating it keeps the mode, choosing the other switches once. Mode persistence, date selection, header layout and Hebrew RTL remain correct. Focused semantics/interaction tests and runtime checks cover both options.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
IMPLEMENTED/VERIFIED, AWAITING PARENT INTEGRATION (do not close before merge).
Branch fix/calendar-mode-a11y, worktree /Users/daniel/.paseo/worktrees/15o7vaq5/calendar-mode-a11y. Commits: 2bb7057 spec (specs/rfes/calendar-mode-a11y.md), 5dbc00b fix+tests+verification notes.
Change: new internal CalendarModeSelector (ui/calendar/CalendarModeSelector.kt) using M3 SingleChoiceSegmentedButtonRow + SegmentedButton (Role.RadioButton, selectableGroup, always enabled, checkmark on selected, labelSmall SemiBold). CalendarIntent.ToggleCalendarHebrewMode replaced by SetCalendarHebrewMode(hebrew); the ViewModel saves only when the mode differs (re-selecting is a no-op, no reload). Existing strings reused; no new resources; no Settings changes.
Tests: CalendarModeSelectorTest 10/10 (group/role/selected/enabled, idempotent tap + a11y click on the selected option, single switch, stateful no-inversion, keyboard Enter, RTL order, 48dp touch target); ReactiveCalendarRefreshTest new case 'selecting a calendar mode is explicit and idempotent' (no pref change/reload for the current mode, selected date kept). Full ./gradlew :app:testDebugUnitTest: 536 tests, 0 failures, 0 errors, 0 skipped. :app:assembleDebug OK; :app:lintDebug OK (only the existing ModifierParameter warning at CalendarScreen.kt:127, unchanged).
Emulator: owned read-only session /tmp/levana-emu-gpi_auo0, serial emulator-5560 (stopped). Artifacts in /tmp/levana-emu-gpi_auo0/artifacts (commands.log with timestamps, before/after PNG+XML, kbd-*.xml, he-*.png/xml, talkback-*). Before: selected option enabled=false focusable=false. After (EN+HE): both options checkable/enabled/focusable, exactly one checked; the checked option is clickable=false (standard Android checked-radio behaviour), not disabled. Tapping the selected option keeps the mode; the other switches once; selected date kept; mode persists across force-stop in both languages; RTL mirrors order; header row heights unchanged; subtitles fit. Keyboard: Tab reaches both options; Enter switches; Enter/Space on the selected option is a no-op.
LIMITATIONS: TalkBack spoken output NOT verified (injected input didn't move TalkBack focus; no root for speech logs) -> follow-up levana-wf2. The selector is wider than the old tabs (~509px vs 349px at 1080px width) but the subtitle still fits in all EN/HE x mode combinations. Focus goes to the menu button after a switch (existing header recomposition).
TALKBACK UPDATE: spoken output verified via VERBOSE TalkBack logs + keyboard Tab/Enter (method from levana-q5h peer) on new owned session /tmp/levana-emu-qkq71rin, emulator-5556 (stopped). Speech evidence in *-logcat.txt (en-tab-5/6, en-enter-*, he-tabw-3/4, he-enter-gregorian). EN: 'Not selected, Hebrew, Radio button, 1 of 2, In list, 2 items'; 'Selected, Gregorian, Radio button, 2 of 2'. HE: 'נבחר, עברי, Radio button, 1 of 2, In list, 2 items'; 'לא נבחר, לועזי, Radio button, 2 of 2, In list, 2 items'. Enter on the selected option: silent, no change; Enter on the other option: one switch, announced as selected. Empty RadioButton child is not spoken. Not covered: touch gestures (double-tap). Remaining: keyboard focus restoration after a switch -> levana-wf2 (re-scoped). Spec verification notes commit: c707b96. Still awaiting parent integration.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-5xh",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T05:55:52Z",
  "closed_at": "2026-10-23T06:35:27Z",
  "close_reason": "Reviewed, emulator-verified and merged/pushed to origin/master at 6a75ee8 (first three at 296c6c2). Final combined verification: 576 unit tests, zero failures/errors/skips; lintDebug and assembleDebug passed. English/Hebrew RTL and runtime evidence preserved under individual emulator session artifacts. TalkBack speech logs inspected. Remaining keyboard focus/manual gesture and checkbox speech issues tracked in levana-wf2 and levana-bhe; pre-existing noon/plural wording bugs tracked in levana-851.",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 3
}
```

---
id: TASK-27
title: Unify calendar-selection checkbox labels and row interaction
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-23T05:53:41Z"
updated_date: "2026-10-23T06:35:27Z"
labels:
  - accessibility
dependencies: []
references:
  - levana-td7
priority: medium
type: bug
ordinal: 27000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: accessibility source review of master 1649ccb, CalendarSelectionScreen.kt CalendarRow (around lines 171–201). The standard Checkbox owns onCheckedChange, but calendar name/account are sibling text and the surrounding row is not toggleable. This leaves the checkbox without an explicitly associated calendar label and makes label/row taps ineffective. Unlike the former Halachic Times issue, there are not two click handlers here. Runtime TalkBack output remains unverified.

Reuse SettingsCheckboxRow, adding a leading-content slot for the color indicator if appropriate, or extract a shared CheckboxRow usable outside Settings. Parent owns toggleable(Role.Checkbox), selected/enabled state and sole callback; child Checkbox has onCheckedChange=null. Preserve calendar name/account, decorative color indicator, selection persistence and permission behavior. Avoid redundant custom content descriptions when natural merged text suffices.

Verify one named checkbox accessibility target, row/label/checkbox-area activation exactly once, keyboard and TalkBack operation, English/Hebrew RTL, and persisted calendar visibility. Related implementation: levana-8pf; its general TalkBack follow-up levana-bhe is separate.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Calendar selection uses a reusable labeled checkbox row with one interaction owner and one accessibility target containing the calendar identity and checked state. Tapping name, row or checkbox toggles exactly once; keyboard/accessibility activation and persistence work. Color/account presentation, permissions and Hebrew RTL remain correct. Record actual runtime/TalkBack evidence and any limitations.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
IMPLEMENTED/VERIFIED, AWAITING PARENT INTEGRATION. Branch fix/calendar-checkbox-a11y, worktree /Users/daniel/.paseo/worktrees/15o7vaq5/calendar-checkbox-a11y. Commits: 7b582a4 spec, 463386e fix, 5fbd449 verification docs (head 5fbd449).
Change: SettingsCheckboxRow gains optional leadingContent slot; CalendarRow uses SettingsCheckboxRow(title=name, description=accountName if not blank, leadingContent=decorative color dot) inside the existing Card. Row owns toggleable(Role.Checkbox); Checkbox onCheckedChange=null. VM/persistence/permissions unchanged; no new strings; SettingsScreen.kt untouched. Checkbox moved from start to end (matches Settings rows).
Tests: CalendarSelectionScreenTest 10/10 (new), SettingsCheckboxRowTest 11/11 (+1 leadingContent). Full :app:testDebugUnitTest 536 tests 0 failures 0 errors; :app:assembleDebug + :app:lintDebug OK (0 errors, 84 warnings, none in changed files).
Emulator emulator-5562 own read-only session /tmp/levana-emu-bwxvjnj9 (stopped; evidence in artifacts/, SUMMARY.md, commands.log): EN+HE taps on name/account/checkbox/dot/row flip once; one checkable node per calendar; D-pad focus + Enter/center toggle once; persistence across force-stop OK; permission grant flow OK; RTL mirrored.
TalkBack 16 verbose log: focus speaks 'not checked. Personal, me@example.com' + 'Double-tap to toggle', ROLE_CHECK_BOX (EN and HE app locale).
Limitations: after keyboard Enter/Alt+Enter toggle TalkBack speaks no state change (event src ROLE_NONE, not focused); TalkBack double-tap gesture not injectable via adb; not compared against existing Settings checkbox rows (logging intermittent). Noted on levana-bhe.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-td7",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T05:55:47Z",
  "closed_at": "2026-10-23T06:35:27Z",
  "close_reason": "Reviewed, emulator-verified and merged/pushed to origin/master at 6a75ee8 (first three at 296c6c2). Final combined verification: 576 unit tests, zero failures/errors/skips; lintDebug and assembleDebug passed. English/Hebrew RTL and runtime evidence preserved under individual emulator session artifacts. TalkBack speech logs inspected. Remaining keyboard focus/manual gesture and checkbox speech issues tracked in levana-wf2 and levana-bhe; pre-existing noon/plural wording bugs tracked in levana-851.",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```

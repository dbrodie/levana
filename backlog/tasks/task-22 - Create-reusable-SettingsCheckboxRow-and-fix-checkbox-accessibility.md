---
id: TASK-22
title: Create reusable SettingsCheckboxRow and fix checkbox accessibility
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-21T11:19:38Z"
updated_date: "2026-10-22T17:54:00Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-8pf
priority: low
type: bug
ordinal: 22000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/rfes/fix-nissan-row-toggle.md — checkbox-row follow-up. Scope updated following source review and owner discussion.

Problem:
HalachicTimesSection uses the standard Material 3 Checkbox inside a separately clickable ListItem. The parent row and child checkbox each own an interaction handler, splitting the setting into separate accessibility targets rather than one labeled checkbox. This is a composition problem, not a need to replace Material's checkbox. Ordinary tapping may work; no device reproduction or claim that one tap toggles twice has been established.

Implementation:
- Create a reusable SettingsCheckboxRow composable alongside the existing settings row components, following the conventions of SettingsSwitchRow.
- Expose a clear API for label, checked state, enabled state and onCheckedChange, with optional supporting text/content and modifier as needed by actual callers. Avoid an unnecessarily general component framework.
- Use Material 3 Checkbox as the visual control, with onCheckedChange = null. The parent row owns Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange).
- Let Compose combine the label and checkbox state into one accessibility target through the standard parent toggleable pattern. Do not leave another clickable modifier or interactive child checkbox. Avoid redundant manual content descriptions or custom gesture handling.
- Make the whole row, including the label and checkbox area, activate the same callback. Preserve minimum touch-target sizing, normal focus/keyboard behavior, and visually/semantically consistent disabled states.
- Replace the duplicated row/checkbox wiring in HalachicTimesSection with SettingsCheckboxRow. Retain current styling, zman names, ordering and preference persistence. Use the shared component for matching settings checkbox rows encountered in scope; do not refactor unrelated screens or reopen completed switch-row work.

Accepted Compose guidance:
Google recommends moving selection behavior to the parent container and setting the child control callback to null for a labeled, whole-row selection control.
https://developer.android.com/develop/ui/compose/accessibility/api-defaults
https://developer.android.com/codelabs/jetpack-compose-accessibility

Verification:
- Inspect/test semantics: one labeled checkbox target per row, carrying the correct role and checked/unchecked state, without a separately actionable checkbox child.
- Verify tapping the label, checkbox area or row activates exactly once; keyboard/accessibility activation works; disabled rows do not invoke callbacks.
- Check TalkBack announcement/focus behavior and English/Hebrew RTL presentation. Distinguish actual device verification from semantics tests in the completion report.
- Verify selections persist and existing Halachic Times behavior is unchanged. Add focused reusable-component coverage rather than duplicating tests for every zman.

Correction to original scope:
The previous acceptance criteria referred to preserving a five-selected-zman limit, but source review found no enforced five-selection cap: SettingsViewModel and PreferencesRepository store the selected set and the day panel filters all selected zmanim. Preserve current behavior. Do not add a new cap or disabled-at-five behavior as part of this accessibility task; selection limits require a separate product decision.

Review basis: source inspection of SettingsScreen.kt, SettingsViewModel.kt, PreferencesRepository.kt and DayDetailViewModel.kt, plus official Compose guidance. TalkBack runtime behavior remains to be verified during implementation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A reusable SettingsCheckboxRow wraps the standard Material 3 Checkbox and is used by HalachicTimesSection. The parent toggleable owns the Checkbox role, checked/enabled state and sole interaction callback; the child Checkbox uses onCheckedChange = null. Each row exposes one coherent labeled checkbox accessibility target. Taps on label, checkbox area and row invoke one state change; keyboard/accessibility activation and disabled behavior work. Focused semantics/interaction checks cover the shared component, and English/Hebrew RTL and TalkBack behavior are verified with evidence and limitations recorded. Existing styling, selected-zman behavior and persistence are preserved. No five-selection limit is introduced.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on branch fix/settings-checkbox-row (worktree /Users/daniel/.paseo/worktrees/15o7vaq5/settings-checkbox-row). Commits: eabe4a3 spec (specs/rfes/settings-checkbox-row.md), fe7d0c4 fix, abc620c verification docs.
Change: new internal SettingsCheckboxRow (ui/settings/SettingsCheckboxRow.kt): ListItem with Modifier.toggleable(value, enabled, Role.Checkbox, onValueChange); M3 Checkbox onCheckedChange=null; disabled text at onSurface 38%. HalachicTimesSection uses it; styling, names, order, onToggle/persistence unchanged. No selection cap added. SettingsSwitchRow/Minhag untouched.
Tests: SettingsCheckboxRowTest 10/10 (semantics: one Role.Checkbox node, merged label, no separately actionable child; label/checkbox/row taps once; OnClick action; keyboard Enter; disabled no callback; RTL; HalachicTimesSection one node per zman). Full testDebugUnitTest 524/0 fail; assembleDebug OK.
Emulator emulator-5558 session /tmp/levana-emu-mfydrpbd: EN+HE RTL hierarchy one checkable node per row; label/checkbox/row taps flip once; Tab focus + Enter/DPAD_CENTER toggle once (Space no-op, Compose default); persistence across force-stop verified in day panel. Concurrent with levana-mhf (emulator-5556) overlap 14:40:34-14:43:48Z UTC. Evidence /private/tmp/levana-parallel-settings.GHCWeN/levana-8pf-*.
Limitations: TalkBack spoken output NOT tested; disabled state only tested in Robolectric (no disabled caller). Awaiting owner approval + merge to master; not closed.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-8pf",
  "spec_id": "specs/rfes/fix-nissan-row-toggle.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-22T14:23:34Z",
  "closed_at": "2026-10-22T17:54:00Z",
  "close_reason": "Approved by owner, merged and pushed to origin/master at 1649ccb. Combined verification: 525 unit tests, zero failures/errors/skips; lintDebug and assembleDebug passed. Concurrent emulator feature testing independently verified at 14:40:34–14:43:48 UTC on emulator-5558 and emulator-5556. Remaining TalkBack spoken-output verification tracked separately in levana-bhe.",
  "external_ref": "specs-import:rfe-zman-checkbox-accessibility",
  "metadata": {
    "import_key": "rfe-zman-checkbox-accessibility",
    "review_basis": "source-and-official-guidance",
    "import_source": "specs/rfes/fix-nissan-row-toggle.md — Out of scope"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 3
}
```

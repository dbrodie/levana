---
id: TASK-24
title: Reconcile birthday and event editor year limits
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-21T12:03:24Z'
updated_date: '2026-10-30T06:30:00Z'
labels:
  - curated-import
  - verified-followup
dependencies: []
references:
  - levana-v57
priority: medium
type: task
ordinal: 24000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/rfes/fix-adar-and-year-followups.md, section 2a. Verified against source on 2026-10-21. The spec says editor state stays in the stepper range. HebrewDateMatcher.EDITABLE_YEARS and HebrewDatePicker use 5700..5900, but ContactBirthdayViewModel.setDate and AddEditEventViewModel.setDate clamp to 3762..9999; editorDate also retains known stored years outside the picker range. Existing tests expect the wider limits. Decide and document the supported year-range policy, then make validation and state consistent with it while preserving valid historical stored dates. Core recurrence, year-zero crash protection, import validation, and day-clamp work are implemented and tested; do not reopen that completed scope. Scope is the year-range policy and non-destructive date handling only. The date picker UI is being replaced by the shared dual-calendar picker (TASK-45), which will use this policy for its bounds; do not change picker UI here.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 One explicit year-range policy governs birthday/event editor validation and state, exposed in a single place that the date picker (TASK-45) can use for its bounds. Opening valid historical or future imported dates has defined non-destructive behavior. Tests cover values at and beyond both boundaries. The RFE records the chosen policy and removes conflicting claims.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Policy (owner decision 2026-10-30): supported Hebrew years 5600..6000.

1. Single definition: new `object SupportedHebrewYears` in domain/model (ninja.doskey.app.levana.domain.model.SupportedHebrewYears): FIRST = 5600, LAST = 6000, `range`, `contains(year)`, `coerce(year)`, and `gregorianYears` (Gregorian years lying wholly inside the range, computed with KosherJava) for the Gregorian side of pickers. Remove HebrewDateMatcher.EDITABLE_YEARS; the stepper year picker (AddEditEventScreen.HebrewDatePicker, used by both editors) and Go-to-date read SupportedHebrewYears. MIN/MAX_KNOWN_YEAR (3762..9999) stay, but only as "can the calendar library handle / format this stored year" (isKnownYear, import validation), not as editor limits.
2. Editors (AddEditEventViewModel, ContactBirthdayViewModel):
   - A year the user enters (SetHebrewYear) is coerced into 5600..6000; the default year (current year) too.
   - Day/month changes keep the year held in state, even if it is a stored out-of-range known year (e.g. 5500), so editing the day of an old yahrzeit doesn't move its year.
   - PreFillDate keeps a known year as-is (a real calendar date), unknown years coerce into the range.
   - Non-destructive open/save: when the stored year is outside 5600..6000 (incl. unknown years 0/1/-1/99999 and known 3762..5599 / 6001..9999), the VM remembers the stored (day, month, year) verbatim. Until the user changes the day, month or year, Save writes the stored values exactly (title/notes/contact edits don't count). The editor shows the stored known year as-is (stepper: "-" disabled below 5600, "+" steps into range at 5600; mirrored at 6000); unknown years keep showing the existing editorDate year. In-range stored dates keep the RFE 1e/2a behaviour (day clamp / Adar II normalisation saved).
3. Tests: SupportedHebrewYears bounds; editor SetHebrewYear at 5599/5600/6000/6001 and extremes; stepping from 5500/6100; open+save untouched for 5500, 6100, 0, 1, -1, 99999, 30 Iyar 5500, Adar II 5501(ordinary) for both editors; editing title only keeps it; editing day keeps 5500; import JSON/CSV of 5500/6100/3762/9999 preserved via serializer and import path. Update tests that pinned 3762..9999 editor clamping and the unknown-year save.
4. RFE fix-adar-and-year-followups.md: record the policy (new section), fix 2a / section 4 / 1e claims that contradict it.
5. Verify: assemble, unit tests, lint, verifyRoborazziDebug --rerun-tasks (116 unchanged); emulator: stepper stops at 5600/6000; out-of-range contact birthday opens and saves unchanged.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Owner decision 2026-10-30: the supported Hebrew year range is 5600–6000 (≈1840–2240) for the event and birthday editors and for the shared date picker (TASK-45). Stored or imported dates outside the range are always preserved unchanged, never clamped or rewritten. Expose the range in a single place that TASK-45 uses for its bounds.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-v57",
  "spec_id": "specs/rfes/fix-adar-and-year-followups.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:rfe-editor-year-bounds",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```

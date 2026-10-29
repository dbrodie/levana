---
id: TASK-24
title: Reconcile birthday and event editor year limits
status: To Do
assignee: []
created_date: "2026-10-21T12:03:24Z"
updated_date: "2026-10-22T10:04:48Z"
labels:
  - curated-import
  - verified-followup
dependencies: []
references:
  - levana-v57
priority: low
type: task
ordinal: 24000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/rfes/fix-adar-and-year-followups.md, section 2a. Verified against source on 2026-10-21. The spec says editor state stays in the stepper range. HebrewDateMatcher.EDITABLE_YEARS and HebrewDatePicker use 5700..5900, but ContactBirthdayViewModel.setDate and AddEditEventViewModel.setDate clamp to 3762..9999; editorDate also retains known stored years outside the picker range. Existing tests expect the wider limits. Decide and document the supported year-range policy, then make validation, state and existing control bounds consistent while preserving valid historical stored dates. Core recurrence, year-zero crash protection, import validation, and day-clamp work are implemented and tested; do not reopen that completed scope. Scope is year limits and non-destructive date handling only. Add-event form redesign, faster year selection, direct-entry controls and other usability changes are tracked separately in low-priority levana-oub and are not required to complete this task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 One explicit year-range policy governs birthday/event editor validation, state and existing control bounds. Opening valid historical or future imported dates has defined non-destructive behavior. Tests cover values at and beyond both boundaries. The RFE records the chosen policy and removes conflicting claims. Completion does not require new input widgets, faster year navigation or add-event layout changes; those belong to levana-oub.
<!-- AC:END -->

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

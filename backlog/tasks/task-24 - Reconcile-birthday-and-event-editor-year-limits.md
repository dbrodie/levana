---
id: TASK-24
title: Reconcile birthday and event editor year limits
status: To Do
assignee: []
created_date: '2026-10-21T12:03:24Z'
updated_date: '2026-10-29T21:29:00Z'
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
Source: specs/rfes/fix-adar-and-year-followups.md, section 2a. Verified against source on 2026-10-21. The spec says editor state stays in the stepper range. HebrewDateMatcher.EDITABLE_YEARS and HebrewDatePicker use 5700..5900, but ContactBirthdayViewModel.setDate and AddEditEventViewModel.setDate clamp to 3762..9999; editorDate also retains known stored years outside the picker range. Existing tests expect the wider limits. Decide and document the supported year-range policy, then make validation and state consistent with it while preserving valid historical stored dates. Core recurrence, year-zero crash protection, import validation, and day-clamp work are implemented and tested; do not reopen that completed scope. Scope is the year-range policy and non-destructive date handling only. The date picker UI is being replaced by the shared dual-calendar picker (TASK-45), which will use this policy for its bounds; do not change picker UI here.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 One explicit year-range policy governs birthday/event editor validation and state, exposed in a single place that the date picker (TASK-45) can use for its bounds. Opening valid historical or future imported dates has defined non-destructive behavior. Tests cover values at and beyond both boundaries. The RFE records the chosen policy and removes conflicting claims.
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

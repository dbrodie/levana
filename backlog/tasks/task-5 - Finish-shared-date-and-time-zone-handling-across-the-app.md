---
id: TASK-5
title: Finish shared date and time-zone handling across the app
status: To Do
assignee: []
created_date: "2026-10-21T11:19:29Z"
updated_date: "2026-10-21T11:19:29Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-wvk
priority: medium
type: chore
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #11 (Partial)

Remaining scope is an app-wide date-provider refactor and explicit device-zone versus selected-location-zone semantics. TASKS.md says alarm planning already uses explicit dates/zones and reads the current device zone on rebuild; do not reopen those completed fixes. Preserve developer date overrides.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-date-override.md
- specs/rfes/fix-device-timezone-reminders.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Screens and system entry points use a consistent shared date source with explicit zone semantics. Developer date override remains respected. Checks cover a device zone different from the selected location and date boundaries.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-wvk",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-11",
  "metadata": {
    "import_key": "original-11",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #11 (Partial)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```

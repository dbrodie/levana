---
id: TASK-10
title: Add per-event birthday and yahrzeit reminders
status: To Do
assignee: []
created_date: "2026-10-21T11:19:32Z"
updated_date: "2026-10-21T11:19:32Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-hlf
priority: medium
type: feature
ordinal: 10000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #18 (Open)

Allow reminder times and advance notice to be configured separately for individual birthdays and yahrzeits. This extends reminders beyond the existing category-level settings.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Individual birthdays and yahrzeits can set reminder timing and advance notice. Choices persist and control delivery. Editing, disabling, or removing an event reconciles its pending reminders without affecting other events.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-hlf",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-18",
  "metadata": {
    "import_key": "original-18",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #18 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```

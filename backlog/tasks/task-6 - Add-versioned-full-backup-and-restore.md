---
id: TASK-6
title: Add versioned full backup and restore
status: To Do
assignee: []
created_date: '2026-10-21T11:19:30Z'
updated_date: '2026-10-29T20:35:00Z'
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-a10
priority: low
type: feature
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #15 (Open)

Back up and restore personal events, saved locations, and preferences using a versioned format. Specify contact-birthday handling explicitly. Existing JSON/CSV personal-event export/import is narrower and should be reused where appropriate; it does not satisfy full backup.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/events-export-import.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A versioned archive round-trips events, saved locations, and preferences. Restore validates unsupported or invalid input before applying changes. Contact-birthday behavior and restore conflict handling are explicit. Existing event-only JSON/CSV exchange remains supported.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-a10",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-15",
  "metadata": {
    "import_key": "original-15",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #15 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```

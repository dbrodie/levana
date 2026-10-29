---
id: TASK-9
title: Add a home-screen calendar widget
status: To Do
assignee: []
created_date: "2026-10-21T11:19:31Z"
updated_date: "2026-10-21T11:19:31Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-crt
priority: medium
type: feature
ordinal: 9000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #16 (Open)

Display Hebrew/Gregorian date, next zman, upcoming candle lighting, and selected location in a home-screen widget. Existing Quick Settings tile and dynamic launcher icon are different surfaces and do not satisfy this item.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/quick-tile-hebrew-date.md
- specs/rfes/dynamic-launcher-icon.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A home-screen widget displays both dates, next zman, upcoming candle lighting, and selected location. It refreshes as dates, location, and relevant times change, with defined behavior when a time is unavailable.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-crt",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-16",
  "metadata": {
    "import_key": "original-16",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #16 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```

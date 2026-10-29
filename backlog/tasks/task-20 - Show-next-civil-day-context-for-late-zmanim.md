---
id: TASK-20
title: Show next-civil-day context for late zmanim
status: To Do
assignee: []
created_date: "2026-10-21T11:19:37Z"
updated_date: "2026-10-21T11:19:37Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-aw1
priority: low
type: chore
ordinal: 20000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — remaining findings: Show next-day context for late zmanim (Open)

Indicate when a displayed nightfall or fast-end time belongs to the following civil day. This is display context, distinct from preserving candle-lighting and havdalah instants in notification planning.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-omer-nightfall-rollover.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Displayed nightfall and fast-end times that occur on the following civil day carry clear next-day context. English and Hebrew presentations are understandable. Ordinary same-day times retain their normal presentation.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-aw1",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:followup-late-zmanim",
  "metadata": {
    "import_key": "followup-late-zmanim",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — remaining findings: Show next-day context for late zmanim (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```

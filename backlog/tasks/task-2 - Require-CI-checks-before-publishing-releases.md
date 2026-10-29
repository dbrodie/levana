---
id: TASK-2
title: Require CI checks before publishing releases
status: To Do
assignee: []
created_date: "2026-10-21T11:19:28Z"
updated_date: "2026-10-21T11:19:28Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-amk
priority: high
type: chore
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #6 (Open)

Run unit tests, lint, and Roborazzi screenshot verification on pull requests, and require successful checks before publishing. Extend the release pipeline described in the RFEs rather than recreating signing, APK/AAB publishing, or Play internal-track upload.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/release-process.md
- specs/rfes/play-publishing-workflow.md
- specs/rfes/rfe_add_roborazzi_support.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Pull requests run unit tests, lint, and screenshot verification. Failed checks prevent release publication. Existing tag-driven signing and publishing remain supported.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-amk",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-6",
  "metadata": {
    "import_key": "original-6",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #6 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 1
}
```

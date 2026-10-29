---
id: TASK-19
title: Move reverse geocoding off the main thread
status: To Do
assignee: []
created_date: "2026-10-21T11:19:37Z"
updated_date: "2026-10-21T11:19:37Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-3ay
priority: medium
type: chore
ordinal: 19000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — remaining findings: Move reverse geocoding off the UI thread (Open)

Run the remaining synchronous LocationService reverse-geocoder call away from the UI thread. TASKS.md says provider/file IO is already off-main; do not reopen that broad completed item.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-background-provider-io.md
- specs/rfes/remove-onboarding.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Reverse geocoding does not block the main thread. Cancellation, timeout, missing results, and failure preserve usable location fallback behavior.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-3ay",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:followup-geocoder",
  "metadata": {
    "import_key": "followup-geocoder",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — remaining findings: Move reverse geocoding off the UI thread (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```

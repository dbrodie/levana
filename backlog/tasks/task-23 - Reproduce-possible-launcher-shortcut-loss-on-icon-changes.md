---
id: TASK-23
title: Reproduce possible launcher shortcut loss on icon changes
status: To Do
assignee: []
created_date: "2026-10-21T11:19:39Z"
updated_date: "2026-10-21T11:19:39Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-4n2
priority: low
type: spike
ordinal: 23000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/rfes/fix-launcher-icon-exit.md — Known limitation (not addressed)

The RFE records Pixel Launcher removing disabled alias components and suspects that a home-screen Levana shortcut disappears when the Hebrew day changes. It explicitly says this was not yet verified on a home screen. Investigate before proposing an implementation or claiming this is a confirmed bug.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/dynamic-launcher-icon.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Place a real home-screen shortcut and change the dynamic icon/day. Record whether the shortcut survives and which launcher/version was tested. If reproduced, document constraints and a scoped fix proposal; otherwise record the result and close the investigation.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-4n2",
  "spec_id": "specs/rfes/fix-launcher-icon-exit.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:rfe-launcher-shortcut-investigation",
  "metadata": {
    "import_key": "rfe-launcher-shortcut-investigation",
    "review_basis": "documents-only",
    "import_source": "specs/rfes/fix-launcher-icon-exit.md — Known limitation (not addressed)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```

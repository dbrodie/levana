---
id: TASK-11
title: Reconcile project documentation and stale RFE statuses
status: To Do
assignee: []
created_date: "2026-10-21T11:19:33Z"
updated_date: "2026-10-21T12:04:29Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-334
priority: low
type: chore
ordinal: 11000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #20 (Open)

Update product, architecture, and MVP documentation to match implemented frameworks/features and remaining work. This import reviewed only TASKS.md and specs/rfes; no completion claim should be inferred for unmarked RFEs. Resolve stale In progress labels and unchecked acceptance lists before treating them as new work. Notable document conflicts: onboarding-fix is superseded by remove-onboarding; release-process future AAB work is covered by play-publishing-workflow; old About install-source labels are replaced by fix-install-source; older Stitch styling is superseded by the Claude refresh and system-font decision. Within fix-adar-and-year-followups, section 1c says non-yahrzeit short-month day-30 behavior is unchanged, whereas later section 1d explicitly changes it; reconcile that text without reopening an assumed bug.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/remove-onboarding.md
- specs/rfes/play-publishing-workflow.md
- specs/rfes/fix-install-source.md
- specs/rfes/claude-design-refresh.md
- specs/rfes/system-font.md
- specs/rfes/fix-adar-and-year-followups.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Documentation accurately distinguishes completed, superseded, planned, and remaining work. Ambiguous RFE statuses and contradictory text are reconciled against the actual state during implementation. Completed features are not reintroduced as backlog.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verification 2026-10-21: Source/test audit plus API 36.1 emulator checks resolve three stale RFE statuses. fix-birthday-year-zero-crash.md, localize-notifications-zman-names.md and fix-zmanim-hebrew-headers.md are now Complete with verification sections. fix-adar-and-year-followups.md is Partial only for the picker/state year-limit mismatch, tracked separately as levana-v57. Completed recurrence work must not be re-imported. Latest full :app:testDebugUnitTest :app:assembleDebug run passed all 509 tests in 71 suites, zero failures/errors/skips. Initial notification-refresh assertion failure passed in isolation and in the subsequent full run; keep this transient test failure visible when improving CI reliability. Runtime check: legacy contact birthday data3=0 loaded; editor showed 5787; untouched-year save persisted 5787; cold relaunch loaded calendar. English/Hebrew zmanim rendering checked. Temporary contact/permissions/locale cleaned up. Three completed RFEs were never imported as open Beads, so no existing implementation issue needed closing.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-334",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-20",
  "metadata": {
    "import_key": "original-20",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #20 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```

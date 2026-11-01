---
id: TASK-57.18
title: Explore permission-state wording and permanently-denied states
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: low
type: spike
ordinal: 88000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Explore wording of permission prompts and the permanently-denied variants (open system settings).

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **L3** (Low) — System calendars permission (29:2524). Title "Bring in your plans" is friendly but the body repeats "to display your events on the Hebrew calendar"; the button is "Grant Permission" (title case). *Why:* Tone/casing. *Reviewer suggestion:* "Grant permission"; mention "read-only, nothing leaves your device" as in onboarding.
- **L4** (Low) — Permission screens (29:2524, 23:794). No **permanently denied** state ("Open system settings"). *Why:* Common dead end on Android 13+. *Reviewer suggestion:* Add variant with "Open settings".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Recommendation recorded and designs added to Figma
<!-- AC:END -->

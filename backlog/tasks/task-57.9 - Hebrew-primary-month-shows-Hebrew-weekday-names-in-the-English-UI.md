---
id: TASK-57.9
title: Hebrew-primary month shows Hebrew weekday names in the English UI
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 79000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In Hebrew-primary mode the weekday header switches to Hebrew names even in the English UI. Weekday names should follow the UI language.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **C5** (Medium) — Weekday header, Hebrew mode, English UI (21:307). Weekday labels switch to Hebrew ("יום א׳ … שבת") when the user is in English UI. *Why:* Mode (which calendar is primary) should not change UI language. *Reviewer suggestion:* Keep weekday labels in the UI language; optionally "Sun · א׳".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Weekday labels follow the UI language in both calendar modes
<!-- AC:END -->

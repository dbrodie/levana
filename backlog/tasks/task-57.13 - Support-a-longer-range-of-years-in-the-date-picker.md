---
id: TASK-57.13
title: Support a longer range of years in the date picker
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
type: feature
ordinal: 83000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The year grid covers a narrow range; birth and yahrzeit years are often decades back. Support a longer range with fast navigation.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **D2** (Medium) — Year grid (28:2312). Shows 5780–5794 only; no indication of scroll range and no typed input. *Why:* Birth years in the 5690s–5750s are the common case for birthdays and yahrzeits. *Reviewer suggestion:* Scrollable grid with decade jumps, plus M3 text-input mode (keyboard icon) accepting "5745" or "1985".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Years far in the past/future can be reached quickly
<!-- AC:END -->

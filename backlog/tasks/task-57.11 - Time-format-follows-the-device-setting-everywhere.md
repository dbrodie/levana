---
id: TASK-57.11
title: Time format follows the device setting everywhere
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-12T22:12:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 81000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Times appear as 12-hour AM/PM even in the Hebrew UI, while the onboarding illustration uses 24-hour. Times should follow the device 12/24-hour setting (and locale) in the app, widgets, tile, notifications and illustrations; check bidi rendering of times in Hebrew text.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **Z9** (Medium) — Zmanim screen. Times in **12-hour AM/PM** across all frames (including Hebrew UI); the onboarding illustrations use 24-hour. *Why:* Israeli users read 24-hour; the inconsistency is visible in the file itself. *Reviewer suggestion:* Follow the device 24-hour setting; make the onboarding illustration match.
- **R1** (High) — All Hebrew frames. Times rendered "5:27 PM" with Latin AM/PM in Hebrew UI. *Why:* Israeli convention is 24-hour; "PM" in a Hebrew sentence is jarring. *Reviewer suggestion:* 24-hour when locale/device says so ("17:27").
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All times follow the device 12/24-hour setting
- [ ] #2 Hebrew UI renders times correctly inside RTL text
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
From TASK-57.26 (2026-11-13): in the Hebrew UI on a 12-hour clock, the candle-lighting notification's second time wraps between '5:18' and 'PM' (order still correct). A no-break space was tried and reverted in 57.26 because it split the AM/PM marker in the Fold cover-screen quick cards at 200% font; a notification-only fix would be needed. Evidence: /Users/daniel/.paseo/evidence/task-57.26/09-he-12h-notification.png
<!-- SECTION:NOTES:END -->

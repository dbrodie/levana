---
id: TASK-57.14
title: 'Date picker: holiday dots in Hebrew-led Figma frames'
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-02T08:47:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: low
type: feature
ordinal: 84000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The review found that the Gregorian-led date picker frames in Figma show holiday dots but the Hebrew-led frames don't. In the app the dot is already drawn in both layouts (HebrewDatePickerDialog.kt DayCell, day.holidayCategory), including Rosh Chodesh since TASK-57.6, so this is a Figma-only gap.

Agreed scope (owner, 2026-11-02):
- Update the Hebrew-led date picker frames in Figma (light, dark, Hebrew RTL) to show the holiday dots exactly as the app draws them, matching the Gregorian-led frames.
- Confirm on the emulator that the app shows the dots in Hebrew-led mode, and record the evidence.
- No code change expected. If the app turns out not to draw them, file a bug rather than fixing it here.
- Shabbat marker: not needed, out of scope.
- Today shortcut (the other half of D3): not part of this task; left for a separate decision.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **D3** (Medium) — Date picker. No "Today" shortcut; no Yom Tov/Shabbat markers in Hebrew-led grid (Gregorian-led has dots). *Why:* Parity and speed. *Reviewer suggestion:* Add "Today" text button; mirror markers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Hebrew-led date picker frames in Figma show the holiday dots as the app draws them, in light, dark and Hebrew RTL
- [ ] #2 The app is confirmed on the emulator to show the dots in Hebrew-led mode
<!-- AC:END -->

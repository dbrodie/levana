---
id: TASK-52.4
title: Large-screen adaptive layouts for the inner display (design decision)
status: To Do
assignee: []
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T12:54:00Z'
labels: []
dependencies:
  - TASK-52.2
parent_task_id: TASK-52
priority: low
type: feature
ordinal: 59000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Decide how Levana should use the inner display's width, then implement it with WindowSizeClass / Compose adaptive APIs. Candidates:
- two-pane Month (calendar grid and day panel side by side);
- Agenda list with a detail pane;
- Settings list/detail;
- a navigation rail instead of the drawer at expanded width;
- wider but capped content in editors.

Start with a short proposal and mockups (Claude Design) for owner approval. Phones must stay unchanged, and every pane must keep the TASK-5 device/display-city rules.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Owner-approved adaptive design recorded; implemented for the agreed screens with phone layouts unchanged; baselines at compact, medium and expanded widths
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Findings from TASK-52.2 (layout audit, 2026-10-30)
TASK-52.2 fixed only what needed no design decision: single-column content is capped at 640dp and centred above phone width (ui/LargeScreenLayout.kt), and text in fixed-size cells is capped at 1.3x. Evidence: /tmp/levana-task52.2-evidence/ (README, before/after per size). Open design questions for this task:
- Month (both modes): the grid fills the full width. On the inner displays the cells grow to ~120-135dp but stay near-square and readable, so 52.2 left them alone. Meanwhile the day sheet is capped at 640dp (Material) and centred, with empty gutters either side. Landscape inner (933/954dp): the sheet peeks under a wide grid, which looks unbalanced. This is the main two-pane candidate (grid | day panel side by side).
- Ultra inner portrait (859x954) and Fold8 inner portrait: the six-week grid takes ~800dp of height; the sheet peeks only the zmanim cards, and a blank band sits below the last week in five-week months.
- Agenda, Settings, My Events, Zmanim and the editors are now 640dp columns with empty gutters on the inner display. Candidates: list/detail (Agenda -> day or event; My Events -> editor; Settings -> sub-screen), or a wider cap for the agenda.
- Navigation: the modal drawer at expanded width; a navigation rail (or a permanent drawer) is the standard alternative.
- The top bars and the Agenda/Calendar headers (title, Hebrew/Gregorian toggle, actions) stay full width while their content is capped. Decide whether headers align with the content column.
- The cap value (640dp, matching the bottom sheet) is a judgement call; revisit it together with these layouts.
- Text at 200%: cell text in the month grid, date picker and walkthrough illustrations is capped at 1.3x. Alternatives are taller cells or fewer labels per cell, if the owner wants full scaling there.
<!-- SECTION:NOTES:END -->

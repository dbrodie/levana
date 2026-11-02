---
id: TASK-59
title: 'Agenda two-pane: only the date rail selects the panel''s day'
status: To Do
assignee: []
created_date: '2026-11-02T10:57:00Z'
labels: []
dependencies: []
priority: medium
type: task
ordinal: 99000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner decision 2026-11-02, reviewing TASK-52.6. In the two-pane Agenda (window ≥ 840dp), **only tapping a day's date rail selects that day for the day panel.** TASK-52.6 shipped a broader rule; narrow it:
- **Holiday, candle-lighting, Shabbat and havdalah cards** behave exactly as on phones (open Month on that day, or Zmanim) and **do not** change the panel's selected day.
- **Event and birthday cards** open their editor as on phones, **without** changing the panel's selection.
- **Today, Go to date and a search result** scroll the agenda. Whether they also select the day was not decided: the owner chose "only the date rail". Confirm with the owner before implementing; until then, they scroll without changing the selection.
- The selected-row highlight stays (two panes only), following the date-rail selection.

Keep the TASK-52.3/52.6 continuity behaviour (the selection survives fold, rotation and so on). Update the TASK-52.6 tests and baselines for the narrower rule, and the Figma screens if they depict the old behaviour (TASK-56 sync process). Emulator check on the Fold8 AVD.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 In two panes, only tapping a date rail changes the panel's day; card taps behave as on phones and leave the selection unchanged
- [ ] #2 Behaviour of Today, Go to date and search confirmed with the owner and implemented; tests, baselines and Figma updated; emulator check recorded
<!-- AC:END -->

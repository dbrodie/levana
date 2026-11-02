---
id: TASK-62
title: 'Hebrew UI: Today widget date line reads ''· Jerusalem2026 בנובמבר 2 ,יום שני'''
status: To Do
assignee: []
created_date: '2026-11-02T14:16:00Z'
labels:
  - design-review
dependencies: []
priority: low
ordinal: 99000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-57.26 runtime check (evidence: /Users/daniel/.paseo/evidence/task-57.26/05-he-12h-today-widget.png). In the Hebrew UI the Today widget's subtitle 'יום שני, 2 בנובמבר 2026 · Jerusalem' is reordered by the bidi algorithm: the Latin city name sits against the year ('Jerusalem2026') and the '·' separator ends up at the far left. The Zmanim widget header ('כ״ב חשון · Jerusalem') renders correctly. Likely fix: isolate the Latin city name (FSI…PDI) or build the line so each part is its own bidi run, as TASK-57.26 did for times.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 In the Hebrew UI the Today widget subtitle reads date, separator, city in RTL order
<!-- AC:END -->

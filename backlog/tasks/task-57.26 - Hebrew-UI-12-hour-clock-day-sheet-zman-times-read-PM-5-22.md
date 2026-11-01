---
id: TASK-57.26
title: 'Hebrew UI, 12-hour clock: day-sheet zman times read ''PM 5:22'''
status: To Do
assignee: []
created_date: '2026-11-01T18:03:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
ordinal: 97000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-57.6 runtime check (evidence: /Users/daniel/.paseo/evidence/task-57.6/09-he-daysheet-molad-12h.png). With the app in Hebrew and the device on a 12-hour clock, the day sheet's Zmanim card rows and the Havdalah card show times reversed by the bidi algorithm ('PM 5:22', 'AM 6:00'), while the quick zman cards show '5:22 PM'. Wrap LTR times in an LTR isolate (as MoladText does) or set the text direction for those Text composables; check other Hebrew 12-hour time displays too.
<!-- SECTION:DESCRIPTION:END -->

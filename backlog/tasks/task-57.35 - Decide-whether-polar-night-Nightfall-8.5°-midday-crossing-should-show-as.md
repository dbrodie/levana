---
id: TASK-57.35
title: >-
  Decide whether polar-night Nightfall (8.5° midday crossing) should show as
  --:--
status: To Do
assignee: []
created_date: '2026-11-04T14:16:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
ordinal: 105000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up from TASK-57.31. In the polar night (e.g. Longyearbyen Nov 2026) there is no sunrise or sunset, but the sun still crosses 8.5° below the horizon around midday, so KosherJava returns a Nightfall (tzais) time, e.g. 1:37 PM on Sat 2026-11-21. TASK-57.31 stopped deriving havdalah and candle lighting from it (both now --:--), but the plain Nightfall row in the day sheet / Zmanim screen, the day sheet TZEIT quick card on non-Friday days, the Today widget "Next: Nightfall", the fast end (getFastTimes) and the Omer nightfall reminder still use the crossing. The research (/Users/daniel/.paseo/evidence/task-57.31/high-latitude-zmanim-research.md 1.4, 2.4) notes other products also list the crossings, so this was left unchanged pending an owner decision. Evidence: /Users/daniel/.paseo/evidence/task-57.31/impl/10-daysheet-longyearbyen-sat-nov21-2026-en.png, 12-today-widget-longyearbyen-polar-night-en.png.
<!-- SECTION:DESCRIPTION:END -->

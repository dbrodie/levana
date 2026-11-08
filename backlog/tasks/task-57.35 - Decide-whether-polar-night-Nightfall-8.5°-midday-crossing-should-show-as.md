---
id: TASK-57.35
title: >-
  Decide whether polar-night Nightfall (8.5° midday crossing) should show as
  --:--
status: To Do
assignee: []
created_date: '2026-11-04T14:16:00Z'
updated_date: '2026-11-08T09:25:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
ordinal: 105000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up from TASK-57.31. In the polar night (e.g. Longyearbyen Nov 2026) there is no sunrise or sunset, but the sun still crosses 8.5° below the horizon around midday, so KosherJava returns a Nightfall (tzais) time, e.g. 1:37 PM on Sat 2026-11-21. TASK-57.31 stopped deriving havdalah and candle lighting from it (both now --:--), but the plain Nightfall row in the day sheet / Zmanim screen, the day sheet TZEIT quick card on non-Friday days, the Today widget "Next: Nightfall", the fast end (getFastTimes) and the Omer nightfall reminder still use the crossing. Evidence: /Users/daniel/.paseo/evidence/task-57.31/impl/10-daysheet-longyearbyen-sat-nov21-2026-en.png, 12-today-widget-longyearbyen-polar-night-en.png.

Decision (owner, 2026-11-08): follow TASK-57.31's rule — the app picks no substitute; a time it can't properly calculate shows "--:--".
- On a polar day (no sunrise and/or no sunset on that date), the twilight times derived from depression-angle crossings are not shown: Nightfall, Alot HaShachar and Misheyakir show "--:--" everywhere (day sheet rows and quick cards, Zmanim screen, Agenda, widgets incl. "Next: …", fast end). Chatzot and Chatzot HaLaylah stay (they are always defined). Sunrise/sunset-based times are already "--:--".
- Reminders that depend on those times (Omer nightfall reminder, fast-end and similar) are simply not scheduled on such days, without crashing.
- White nights (sunrise and sunset exist, crossing not reached) are unchanged: already "--:--" via KosherJava returning null.
- The 57.31 city note already covers these locations; no new UI, strings or Figma changes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 On a polar-night or midnight-sun day, Nightfall, Alot HaShachar and Misheyakir show "--:--" in the day sheet, Zmanim screen, Agenda and widgets (including the widget's next-zman line); Chatzot and Chatzot HaLaylah are still shown
- [ ] #2 Fast end and the Omer nightfall reminder use no polar crossing: the fast end shows "--:--" and the reminder is not scheduled, without crashing
- [ ] #3 Unit tests pin Longyearbyen polar night (Nov 2026) and midnight sun (Jun 2027), and St. Petersburg and Jerusalem are unchanged
<!-- AC:END -->

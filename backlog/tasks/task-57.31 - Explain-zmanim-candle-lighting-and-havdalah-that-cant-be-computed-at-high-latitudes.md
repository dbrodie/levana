---
id: TASK-57.31
title: >-
  Explain zmanim, candle lighting and havdalah that can't be computed at high
  latitudes
status: To Do
assignee: []
created_date: '2026-11-02T14:27:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: medium
type: feature
ordinal: 101000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-57.23 (ST1 'zmanim unavailable'). No crash or ANR anywhere at high latitude, but the missing times are unexplained:
- St. Petersburg (59.9°N), 21 Jun 2027: Nightfall shows '--:--' in the day sheet; Alot HaShachar and Misheyakir show '—' in More halachic times (two different placeholders). Evidence: /Users/daniel/.paseo/evidence/task-57.23/25-stpete-june21-zmanim.png, /Users/daniel/.paseo/evidence/task-57.23/26-stpete-june21-more.png, Hebrew /Users/daniel/.paseo/evidence/task-57.23/79-he-stpete-june21-zmanim.png
- Agenda, St. Petersburg late June: Shabbat cards have no Havdalah line at all (silently dropped). Evidence: /Users/daniel/.paseo/evidence/task-57.23/27-stpete-agenda-june.png
- GPS at Longyearbyen (78.2°N), midnight sun (21 Jun 2027): sunrise, sunset, nightfall all '--:--', most of More halachic times '—'. Evidence: /Users/daniel/.paseo/evidence/task-57.23/29-longyearbyen-jun21-2027.png, /Users/daniel/.paseo/evidence/task-57.23/30-longyearbyen-june-more.png
- Longyearbyen, polar night (Nov 2026): Fridays have no candle-lighting entry in Agenda (Month still shows a 'Candles' chip), no candle-lighting reminder is scheduled, widgets show 'Candle Lighting Fri —' and Sunrise/Sunset '—'. Evidence: /Users/daniel/.paseo/evidence/task-57.23/31-longyearbyen-agenda-nov2026.png, /Users/daniel/.paseo/evidence/task-57.23/40-home-widget.png, /Users/daniel/.paseo/evidence/task-57.23/41-home-widgets.png, /Users/daniel/.paseo/evidence/task-57.23/45-after-crash-relaunch.png

Needs: one placeholder, and a short explanation where a time is missing (e.g. 'Not reached today at this latitude') plus guidance for Shabbat times (which opinion to use is a halachic decision — see TASK-57.7). Design follow-up; the halachic fallback rules belong in TASK-57.7.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Missing zmanim use one consistent placeholder with an explanation the user can reach
- [ ] #2 Agenda/Shabbat cards and widgets say when candle lighting or havdalah can't be computed instead of omitting them
- [ ] #3 Design recorded before implementation
<!-- AC:END -->

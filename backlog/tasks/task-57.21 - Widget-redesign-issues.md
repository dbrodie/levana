---
id: TASK-57.21
title: Widget redesign issues
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: feature
ordinal: 91000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Today widget on Shabbat shows the previous Friday's candle lighting (W1, looks like a bug); Zmanim widget mostly empty at 3×3 (W2); Month widget lacks markers and mixes scripts (W3). Date format consistency (W4) is handled by the terminology task.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **W1** (Medium) — Widget / Today Full (54:74). On Shabbat the widget shows "Candle Lighting **Fri 4:33 PM**" (yesterday) and "Havdalah Sat 5:27 PM". *Why:* Stale info on the glanceable surface. *Reviewer suggestion:* After candle lighting has passed, show the *next* Friday's; on Shabbat show "Shabbat ends 5:27 PM" prominently.
- **W2** (Medium) — Widget / Zmanim (54:240). Three rows in a 3×3 cell leave ~60 % empty; only Sunrise/Sunset/Nightfall. *Why:* Wasted real estate on a widget users add *for* zmanim. *Reviewer suggestion:* Fill with the user's chosen zmanim (6–8 rows) or make it resizable with a density rule.
- **W3** (Medium) — Widget / Month (54:91). No holiday/Shabbat chips, no event dots, header "5787 חשון" with the same mixed-numeral issue (C4). *Why:* Low information density vs the in-app month. *Reviewer suggestion:* Add dots for events/holidays, a Shabbat tint, and "today" header.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each issue fixed or rejected with a reason
- [ ] #2 Figma widgets updated
<!-- AC:END -->

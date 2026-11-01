---
id: TASK-57.17
title: Improve the city picker and Cities settings
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: low
type: feature
ordinal: 87000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Improve city search and details (grouping, coordinates/time zone, custom location) and make Cities settings clearer about the fallback city versus a confirmed location and which city is selected. Related: TASK-44, TASK-41.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **L1** (Medium) — Cities settings (47:3269). "CURRENT LOCATION Jerusalem — Based on your time zone. Tap to allow location access." The city is a *fallback*, but the card looks like a success state. No indication which city is the *selected* one (spec: selection happens only in the drawer). *Why:* Users will add a city here and expect it to apply. *Reviewer suggestion:* Show status icon (approx/unknown), a clear "Allow location" button, and a "Selected" badge mirrored from the drawer.
- **L2** (Medium) — City picker (29:2593). Flat list "Jerusalem / Israel …" with no country grouping, no coordinates, no elevation, no time-zone, and no "custom location" entry. *Why:* Zmanim depend on elevation and exact coordinates; power users (and anyone outside the bundled list) need custom lat/long. *Reviewer suggestion:* Show "Israel · 31.78, 35.22 · 754 m · GMT+2" secondary line; add "Use coordinates…" at the bottom; recent/nearby section at top.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 City picker improvements designed and implemented
- [ ] #2 Fallback vs confirmed location is clear
<!-- AC:END -->

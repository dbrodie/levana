---
id: TASK-48
title: Day panel shows wrong zmanim after adding a saved city
status: To Do
assignee: []
created_date: '2026-10-30T00:56:00Z'
labels: []
dependencies: []
priority: medium
type: bug
ordinal: 52000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-12.3's emulator walkthrough (2026-10-30). Reproduced on origin/master 30867af and on the TASK-12.3 branch. The day panel is not part of that change.

Steps (fresh install, API 36 emulator, device location Ukiah CA, zone America/Los_Angeles, today 2026-10-30):
1. Launch the app. The day panel shows NETZ 5:53 AM, SHKIA 4:51 PM, CANDLES 4:33 PM, which are correct for Ukiah.
2. Open the drawer, tap Manage cities, then Add location, and pick Jerusalem. Go back to the calendar.
3. The day panel now shows NETZ 4:38 PM, SHKIA 3:13 AM, CANDLES 2:55 AM. The drawer still shows Ukiah as the active city.

Expected: the panel keeps the active city's times. If Jerusalem's times were intended, they should still appear in a sensible zone. These values look like Jerusalem's times shown in a mismatched zone, or the wrong city.

After selecting Jerusalem in the drawer, the panel showed NETZ 5:53 AM again (not yet investigated).

Evidence: /tmp/levana-emu-7y64kadl/artifacts/11_master_after_add_city.png (outside the repo; may be cleaned up).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After adding a saved city, the day panel shows the active city's zmanim in the correct zone
<!-- AC:END -->

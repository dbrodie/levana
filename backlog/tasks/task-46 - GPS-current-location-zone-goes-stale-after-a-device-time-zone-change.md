---
id: TASK-46
title: GPS current location keeps a stale or mismatched time zone (wrong zmanim)
status: To Do
assignee: []
created_date: '2026-10-29T21:36:00Z'
updated_date: '2026-10-30T05:47:00Z'
labels: []
dependencies: []
priority: high
type: bug
ordinal: 50000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Merged with TASK-48 by owner decision (2026-10-30). Priority raised to high: this shows wrong zmanim to users.

**Hypothesis (coordinator analysis, not yet verified):** a GPS current location stores the device time zone *at the time of the fix* as its `timezoneId`. If that zone doesn't match where the coordinates are, the device position's times are computed and shown in the wrong zone, and it stays that way until the next fix. Causes include travel before the next fix, a manually changed zone, or a fix that arrives just after a zone change. Under the TASK-5 rule the device zone is the source of truth, so a stored zone that disagrees with it is a defect. The device position feeds the day panel when no city is selected, the widgets, reminder triggers and Israel/Diaspora auto-detection.

**Evidence 1 (TASK-48, from the TASK-12.3 walkthrough, 2026-10-30, reproduced on master 30867af).** Fresh install on an API 36 emulator, device location Ukiah CA, zone reported as America/Los_Angeles, today 2026-10-30.
1. Launch the app. The day panel shows NETZ 5:53 AM, SHKIA 4:51 PM, CANDLES 4:33 PM.
2. Drawer → Manage cities → Add location → Jerusalem; go back to the calendar.
3. The day panel shows NETZ 4:38 PM, SHKIA 3:13 AM, CANDLES 2:55 AM, while the drawer still shows Ukiah as active.
4. After selecting Jerusalem in the drawer, the panel shows 5:53 AM again.

Coordinator's reading of the numbers:
- Step 1's times are **Jerusalem's** sunrise and sunset for 30 Oct, not Ukiah's (≈7:38 AM / 6:13 PM PDT). So the app was on a fallback or Jerusalem location before the GPS fix.
- Step 3's times are **Ukiah's sun shown on Jerusalem's clock**: 7:38 AM PDT = 4:38 PM IST, and 6:13 PM PDT = 3:13 AM IST.

So the current location most likely became Ukiah's coordinates with `timezoneId = Asia/Jerusalem`. Adding a city was probably a coincidence of timing with the GPS fix arriving. Evidence: /tmp/levana-emu-7y64kadl/artifacts/11_master_after_add_city.png (may be cleaned up).

**Evidence 2 (TASK-9.2 / 9.3 agents).** With a GPS fix in California and the emulator zone set to Pacific/Kiritimati (UTC+14), the Today widget showed "Next: Sunrise 10:38 AM" with no weekday. The TASK-9.3 widget kept the old zone's times after a zone change.

**Required approach:**
1. **Verify first, before changing code.** Reproduce on an emulator. For example, set the device zone to one that doesn't match the emulator's GPS position (or change the zone after a fix), grant location permission, and compare the stored current location (coordinates + `timezoneId`) and the displayed times on the day panel, the Zmanim screen and the widgets. Confirm or refute the hypothesis and record the exact reproduction steps and evidence. If the cause turns out to be different (e.g. adding a city really does change what the day panel uses), report it to the coordinator before fixing.
2. Then fix the root cause. The likely direction is that the device position's zone always follows the current device zone (via DeviceContext) instead of a zone stamped at fix time; and decide whether a zone change should invalidate the stored fix or trigger a new one. Keep the TASK-5 rules: the device decides dates, notifications and Israel/Diaspora; the selected city stays display-only.
3. Check how existing users' stored current locations with a stale zone are handled after the fix.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The issue is reproduced on an emulator before any fix, with the steps, stored location values and screenshots recorded (or the hypothesis is refuted and the real cause is documented and agreed with the coordinator)
- [ ] #2 After the fix, the device position's zmanim, day panel, Zmanim screen, widgets and reminder triggers use the current device zone; a GPS fix in a zone different from the coordinates, or a later zone change, never yields times shown in a mismatched zone
- [ ] #3 Existing stored current locations with a stale zone are handled; Israel/Diaspora auto-detect and the TASK-5 device/display-city rules are unchanged
- [ ] #4 Unit tests for the zone-mismatch and zone-change cases; emulator re-check of the original TASK-48 steps and the Kiritimati case passes
<!-- AC:END -->

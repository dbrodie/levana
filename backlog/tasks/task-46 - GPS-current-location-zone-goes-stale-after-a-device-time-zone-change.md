---
id: TASK-46
title: GPS current-location zone goes stale after a device time-zone change
status: To Do
assignee: []
created_date: '2026-10-29T21:36:00Z'
labels: []
dependencies: []
priority: low
type: bug
ordinal: 50000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found by the TASK-9.2 and TASK-9.3 agents (2026-10-29). A GPS current location stores the device time zone at the time of the fix as its timezoneId. When the device zone later changes without a new fix (travel before the next fix, or a manually changed zone), DeviceContext uses the new device zone for dates, but the device position keeps its old zone. Zmanim, the widgets' times and reminder triggers are then computed and labelled in the stale zone. Example: GPS fix in California, emulator zone set to Pacific/Kiritimati (UTC+14): the Today widget showed 'Next: Sunrise 10:38 AM' with no weekday, and the TASK-9.3 widget kept the old zone's times after a zone change. Under the TASK-5 rule the device zone is the source of truth. Decide whether the position's zone should follow the current device zone, and whether a zone change should trigger a fresh fix or invalidate the stored one.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After a device time-zone change, the device position's zmanim, widget times and reminder triggers use the current device zone (or a fresh fix), never a stale stored zone
- [ ] #2 A GPS fix whose coordinates don't match the device zone has defined, tested behavior
- [ ] #3 Unit tests for the zone-change case; emulator check with a GPS fix plus zone change
<!-- AC:END -->

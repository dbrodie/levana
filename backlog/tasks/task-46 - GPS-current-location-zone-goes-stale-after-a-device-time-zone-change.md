---
id: TASK-46
title: GPS current location keeps a stale or mismatched time zone (wrong zmanim)
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T21:36:00Z'
updated_date: '2026-10-30T06:01:00Z'
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Phase 1 verification (2026-10-30, @claude, commit cd38289, emulator Levana_Agent_Template_API_36_1 / emulator-5554)

Evidence: /tmp/levana-emu-l_tterxt/artifacts/ (screenshots 01–11, 20–21; 02_prefs.pb). Stored location read with `run-as … cat files/datastore/user_preferences.preferences_pb` (key `gps_location`).

**Key fact:** the emulator's zone is NOT America/Los_Angeles. It inherits the host's zone via the emulator NITZ override (`persist.sys.timezone=Asia/Jerusalem`, time_zone_detector: "Emulator time zone override … Asia!Jerusalem"). `adb emu geo fix -123.2078 39.1502` puts GPS in Ukiah.

### A. TASK-48 case: fix taken while the device zone ≠ the coordinates' zone
1. Fresh install, zone Asia/Jerusalem, GPS Ukiah, location granted, launch. Before the fix the panel shows NETZ 5:53 AM / SHKIA 4:51 PM / CANDLES 4:33 PM (fallback city for Asia/Jerusalem = Jerusalem) (01).
2. ~10–20 s later, with no other action, the fix lands: `{"latitude":39.1501983,"longitude":-123.2078,"elevation":190.0,"timezoneId":"Asia/Jerusalem","name":"Current Location"}` (then "Ukiah" once named). Panel shows NETZ 4:38 PM / SHKIA 3:13 AM / CANDLES 2:55 AM (02). Zmanim screen: Alot 3:17 PM, Sunrise 4:38 PM, Chatzot 9:56 PM, Nightfall 3:54 AM (04).
3. Original TASK-48 steps (pm clear, launch, Drawer → Manage cities → Add location → Jerusalem → back): the panel shows 4:38 PM / 3:13 AM / 2:55 AM (21). The DataStore has `saved_locations=[Jerusalem]` and **no** `active_location_id`. `addSavedLocation` never writes ACTIVE_LOCATION_ID.
- **Adding a city plays no role.** The change in TASK-48 was the GPS fix arriving.
- **The stored zone is not stale here.** It equals the device zone. The 4:38 PM sunrise is Ukiah's sun on the device's (Jerusalem) clock, which is what the TASK-5 rule "the device zone decides" produces. Making the position always use the current device zone would **not** change this output.

### B. Zone change after a fix (stale zone): CONFIRMED
4. With the app in the foreground (Zmanim screen) set the zone to America/Los_Angeles (`settings put global auto_time_zone 0; cmd alarm set-timezone America/Los_Angeles`). The device is now Thu 29 Oct PDT, and the calendar highlights the 29th. The stored zone stays Asia/Jerusalem, and the Zmanim screen and day panel still show 4:38 PM / 3:13 AM. The panel now labels them "Current Location time", because DisplayCityClock sees the stored zone ≠ the device zone (05, 06).
5. Leaving and re-opening the app (ON_RESUME re-fix) stores `timezoneId=America/Los_Angeles` and gives 7:38 AM / 6:13 PM / 5:55 PM (correct, 07). The Today widget agrees (08).
6. Kiritimati (Evidence 2): with the app closed, set the zone to Pacific/Kiritimati. The device is Fri 30 Oct 19:52 +14. The stored zone stays America/Los_Angeles. The widget shows "Next: Sunrise **7:38 AM**" (no weekday), "Candle Lighting Fri 5:55 PM", "Havdalah Sat 6:53 PM", all on the stale LA clock (09). Correct is Sat 4:38 AM / Fri 2:57 PM / Sat 3:54 PM, which is what it shows after the app is opened and re-fixes (10, 11).
- Root cause of B: `LocationService.getCurrentLocationOrNull` stamps `DeviceContext.realZone()` into `Location.timezoneId` at fix time. `DeviceNow.position` returns the stored Location unchanged, and the widgets, ReminderAlarmPlan/NotificationAlarmScheduler, DailyNotificationWorker and displayCity (no city selected) compute in `position.timezoneId`. Only an app resume refreshes it. Widgets and notifications built while the app isn't reopened (and the foreground app until the next resume) use the old zone.

### Conclusion
- Hypothesis **confirmed for the stale-zone case (B, Evidence 2)**. A fix is clear and within the TASK-5 rules: `DeviceNow.position` (and the no-city display) should always carry the current device zone instead of the stamped one. That also corrects already-stored stale locations at read time.
- Hypothesis **refuted for TASK-48 (A)**. That is not a stale zone: the GPS coordinates and the device clock genuinely disagree (an emulator artifact here; for real users, a manual zone or travel with auto-zone off). Showing the position's sun on the device clock is current spec behavior. Changing it is a **product decision**: keep the device clock (status quo, maybe with clearer labelling); show the position in its own geographic zone (needs an offline coordinate→zone lookup, e.g. nearest bundled city); or distrust a fix that is implausible for the device zone. It also affects Israel/Diaspora and the TASK-5 rule "a fix is in the device zone".
- Per the coordinator's instructions, production code is unchanged. Stopped to report before Phase 2.

Owner decision 2026-10-30 on GPS position vs device time zone mismatch (TASK-48 case): Option A. Keep the current rule (device zone decides; times for the device position are shown on the device clock). In addition, detect when the GPS position clearly does not fit the device's time zone and show a brief pop-up explaining that the phone's time zone doesn't match its location, so times may look wrong, and asking the user to fix the device's date & time settings (with a button that opens the system Date & time settings). Implemented as Phase 3 of this task.
<!-- SECTION:NOTES:END -->

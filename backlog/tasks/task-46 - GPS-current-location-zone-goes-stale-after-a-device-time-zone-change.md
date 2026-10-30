---
id: TASK-46
title: GPS current location keeps a stale or mismatched time zone (wrong zmanim)
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T21:36:00Z'
updated_date: '2026-10-30T06:18:00Z'
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
- [x] #1 The issue is reproduced on an emulator before any fix, with the steps, stored location values and screenshots recorded (or the hypothesis is refuted and the real cause is documented and agreed with the coordinator)
- [x] #2 After the fix, the device position's zmanim, day panel, Zmanim screen, widgets and reminder triggers use the current device zone; a GPS fix in a zone different from the coordinates, or a later zone change, never yields times shown in a mismatched zone
- [x] #3 Existing stored current locations with a stale zone are handled; Israel/Diaspora auto-detect and the TASK-5 device/display-city rules are unchanged
- [x] #4 Unit tests for the zone-mismatch and zone-change cases; emulator re-check of the original TASK-48 steps and the Kiritimati case passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Phase 2: stale zone (owner approved 2026-10-30)
1. PreferencesRepository.preferences: a stored GPS fix (key gps_location) keeps only its place. It is read with timezoneId = the device's current zone (DeviceContext.realZone()). The stamped zone is ignored, so already-stored stale fixes are corrected at read time. preferences already re-emits on system time/zone changes, so every reader (DeviceNow.position, displayCity with no city selected, widgets, reminder plans, daily notices) follows a zone change without a new fix. The zone-fallback city is unchanged.
2. Add UserPreferences.currentLocationIsFix (true only for a stored GPS fix) for Phase 3.
3. Robolectric unit tests (PreferencesRepository + DeviceContext + ZmanimRepository): a fix stored with a mismatched zone reads in the device zone; a zone change re-zones the same fix (incl. Kiritimati); the fallback city keeps its own zone and currentLocationIsFix=false.
4. Update ARCHITECTURE_SPEC "Dates and Notifications" and the LocationService comment.

Phase 3: GPS position doesn't fit the device zone (owner decision, Option A)
5. Detection, offline: device/ZoneMismatch.kt, a pure function. Solar offset of the fix = longitude/15 h. Compare it with the device zone's offset at now; the difference is wrapped to [-12, 12] h (date line: Kiritimati, Tonga, Samoa, Chukotka). Mismatch only when |diff| > 4 h. Real zones deviate from solar time by at most ~3–3.5 h (western China on Asia/Shanghai ≈2.9 h, Galicia in CEST ≈2.6 h, Nome AKDT ≈3 h, Attu/Adak HDT ≈3.5 h). Only a real fix (currentLocationIsFix) is checked, never the fallback city.
6. Not nagging: a notice key = device zone id + the fix's rounded solar hour. It is stored in DataStore as internal state (not UserPreferences), like LAST_NOTIFIED_UPDATE_VERSION. The dialog shows only when a mismatch exists and its key differs from the stored one; showing records the key. When the mismatch clears, the key is cleared, so a later new mismatch shows again.
7. When it shows: a ZoneMismatchDialog in the app shell (LevanaStartupEffects) collects the preferences (re-emitted on a new fix and on zone changes) while the app is started. So it shows when a fix arrives or the zone changes in the app, and on the next open if a mismatch arose while closed and wasn't shown yet.
8. UI: Material 3 AlertDialog. Title, plain-language text, confirm "Open Date & time settings" (Settings.ACTION_DATE_SETTINGS, guarded with resolveActivity/try), dismiss "Not now". English + Hebrew strings, no hard-coded direction.
9. No TASK-5 rule changes: Israel/Diaspora stays zone-based, and times stay on the device clock. Screenshot tests seed New York in America/New_York, so no mismatch is possible.
10. Unit tests: California fix + Jerusalem zone → mismatch; Madrid, Shanghai, Kolkata, Kashgar (Asia/Shanghai), Vigo (summer), Nome (summer), Adak, Kiritimati and Ukiah fixes in their own zones → none; fallback city → never; key logic (show once, re-show after the mismatch clears and recurs).
11. Verify: full build + unit tests + lint + verifyRoborazziDebug --rerun-tasks (116 unchanged). Emulator: Kiritimati + zone change with the app open, before/after. TASK-48 setup (Jerusalem zone, GPS Ukiah) shows the pop-up once; the button opens Date & time; it doesn't re-show after resume; no pop-up after setting a matching zone. EN + HE screenshots.
<!-- SECTION:PLAN:END -->

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

## Phase 2 + 3 implementation (@claude, code commit 7e086c2)
- `PreferencesRepository.preferences`: a stored fix is decoded and then `copy(timezoneId = DeviceContext.realZone().id)`. The preferences already re-emit on system time/zone changes, so all readers follow the device zone with no new fix: DeviceNow.position, displayCity with no city selected, widgets, ReminderAlarmPlan/NotificationAlarmScheduler and the daily notices. Already-stored stale fixes are corrected at read time, so no migration is needed. The zone-fallback city is unchanged. New `UserPreferences.currentLocationIsFix`.
- `device/ZoneMismatch.kt`: `solarDeviationHours` = zone offset − longitude/15. It is wrapped to ±12 h only when |lon| ≥ 145°, near the date line (Line Islands, Tonga, Samoa, Chatham, Fiji, Chukotka, Aleutians). Elsewhere a day's difference counts, so a California fix on a Kiritimati clock is caught: 22.2 h, although only −1.8 h after wrapping. `zoneMismatchKey` reports a mismatch when |deviation| > 4 h, and only for a real fix. Key = "<device zone>|<round(lon/15)>".
- `ui/navigation/ZoneMismatchNotice.kt`, in LevanaApp next to LevanaStartupEffects: collects the preferences while STARTED. When the key ≠ the stored `zone_mismatch_notice` (internal DataStore key, not in UserPreferences), it records the key and shows a Material 3 AlertDialog: "Date & time settings" opens Settings.ACTION_DATE_SETTINGS (ActivityNotFound guarded), "Not now" dismisses. When the mismatch clears, the key is removed, so a new one shows again. Strings are EN + HE in localized_ui_strings.xml.
- Spec: ARCHITECTURE_SPEC "Dates and Notifications" has a new bullet. TASK-5 rules are unchanged: Israel/Diaspora stays zone-based, and times stay on the device clock.
- Test change: DisplayCityZmanimTest now sets the JVM zone to New York, the device it models. It had a New York DeviceContext clock but read the host's JVM zone (IST here), which the stored fix now follows.

## Verification
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: BUILD SUCCESSFUL. 834 tests, 0 failures/errors. Lint passes. Roborazzi summary total 116, unchanged 116 (0 changed/added). No order-dependent failure was seen.
- New tests:
  - CurrentLocationZoneTest (4, Robolectric, real DataStore): fix stamped Asia/Jerusalem read in LA (sunrise 7 AM); zone change to Kiritimati re-zones the same fix, including displayCity and DeviceNow.position; Today widget in Kiritimati shows Sunrise Sat 31 Oct 4 AM and candles Fri 14:xx on the +14 clock; fallback city is not a fix and keeps its zone. 3 of the 4 fail without the fix.
  - ZoneMismatchTest (6): California/Jerusalem and California/Kiritimati are mismatches. No mismatch, summer and winter, for LA, Jerusalem, NY, London, Madrid, Vigo, Shanghai, Kashgar on Asia/Shanghai, Kolkata, Ahmedabad, Kaliningrad, Smolensk/Moscow, Samara, Mendoza, Nome, Adak, Attu, St John's, Kathmandu, Kiritimati, Tonga, Apia, Chatham, Fiji, Anadyr. The fallback city never mismatches; key stability; deviation values.
  - ZoneMismatchNoticeTest (3, Compose): shows once; not again on re-open; cleared by a matching zone and shown again when the mismatch returns; button starts ACTION_DATE_SETTINGS; nothing for no fix or a fitting fix.
- Screenshot tests: the harness clears preferences and seeds New York in America/New_York with a New York clock, so no mismatch is possible. 116 are unchanged.
- Emulator re-check on build 7e086c2, Levana_Agent_Template_API_36_1, evidence /tmp/levana-emu-5__m41z1/artifacts/:
  - A1: fresh install, zone Asia/Jerusalem, GPS Ukiah. The pop-up shows once the fix lands (key Asia/Jerusalem|-8); the panel stays on the device clock (4:38 PM, by design).
  - A2: the button opens com.android.settings DateTimeSettingsActivity.
  - A3: back/resume (new fix) and a full re-open: no pop-up again.
  - B1: with the app in the foreground, the zone changes to LA. The stored zone is still Asia/Jerusalem (no new fix), and the panel shows NETZ 7:38 AM / SHKIA 6:13 PM / CANDLES 5:55 PM with no pop-up; the key was cleared. Before the fix this step showed 4:38 PM / 3:13 AM (Phase 1 05/06).
  - C1: Kiritimati with the app in the background, stored zone America/Los_Angeles. The widget shows Next: Sunrise **Sat 4:38 AM**, Candle Lighting Fri 2:57 PM, Havdalah Sat 3:54 PM. Before the fix: 7:38 AM / Fri 5:55 PM / Sat 6:53 PM (Phase 1 09).
  - C2/C3: opening the app in Kiritimati shows the pop-up (California fix on a +14 clock); panel NETZ 4:36 AM / SHKIA 3:16 PM on the device clock.
  - D1: with the app locale set to Hebrew, the zone goes back to Jerusalem. The Hebrew pop-up is laid out RTL, with "לא עכשיו" and "הגדרות תאריך ושעה" buttons.
- Limitations:
  - The emulator's force-stop leaves the app's widget un-updated (stopped apps receive no broadcasts), which is platform behavior. The C1 check backgrounds the app with Home, as Phase 1 did.
  - Reminder triggers are covered by unit tests (they read the same re-zoned position); no notification was delivered on the emulator.
  - The detection can't tell Hawaii from Kiritimati (same longitude, a day apart): only lat/lon→zone data could, and that is out of scope.
<!-- SECTION:NOTES:END -->

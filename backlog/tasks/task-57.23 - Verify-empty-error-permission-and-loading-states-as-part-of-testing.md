---
id: TASK-57.23
title: Verify empty, error, permission and loading states as part of testing
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-02T17:51:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: task
ordinal: 93000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The review lists error/empty/denied/loading states that are not drawn in Figma. The owner's goal (2026-11-02) is verification, not design: confirm the app works and does not crash when permissions are denied or revoked, and that the listed states behave sensibly. Where a missing or broken screen/state is found, raise it as a separate task (design can follow there); don't design or build new screens in this task.

What to verify (on an emulator, English and Hebrew UI):
- Permissions — for each of location (fine/coarse), calendar (READ_CALENDAR), contacts (READ/WRITE_CONTACTS) and notifications (POST_NOTIFICATIONS): never asked, denied once, permanently denied ("Don't ask again"), and granted-then-revoked from system Settings while the app is in the background and while it is open (Android kills and restores the process on revoke). Check every screen/feature that uses the permission, the widgets/tile, and scheduled reminders. No crash, no ANR, no dead button; note where the UI gives no way forward (e.g. a button that can't reopen the system prompt).
- Empty states: Agenda empty, Events empty, Agenda search no results, city search no match, saved cities empty.
- Error/edge states: location unavailable, zmanim that can't be computed (polar latitudes, e.g. a far-north custom location in summer/winter), contacts import failure, no network for any network feature.
- Loading: first launch, calendar provider query.

Output: a results table in the task notes (state → result → evidence), crash logs if any, and a new task for each crash (bug) or missing/poor state (design follow-up). Small crash fixes may be filed rather than fixed here.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **ST1** (High) — No error states exist anywhere: location unavailable, zmanim unavailable at high latitudes (spec can't compute some zmanim in polar summers), import failure, crash-report dialog (Developer has "Test crash" but the dialog isn't designed), no network for update check. *Reviewer suggestion:* Design an error pattern (inline banner + snackbar) and the crash-report dialog.
- **ST2** (Medium) — Missing empty states: Agenda empty, Events tab empty, Search no-results, Cities search no-match, Saved cities empty. *Reviewer suggestion:* Add, reusing the existing birthdays empty-state pattern.
- **ST3** (Medium) — Missing "permanently denied" permission variants (location, contacts, calendar, notifications). *Reviewer suggestion:* Add "Open settings" variants.
- **ST4** (Low) — No loading/skeleton states (first launch computing, calendar provider query). *Reviewer suggestion:* Add minimal shimmer or progress on Agenda/System calendars.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Each permission (location, calendar, contacts, notifications) is verified never-asked, denied, permanently denied and revoked-at-runtime, with no crash or ANR
- [x] #2 Each listed empty, error and loading state is checked and recorded with evidence
- [x] #3 A separate task is filed for every crash found and every missing or poor state that needs design
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Test plan (tested on dedicated headless emulator Levana_Agent_Template_API_36_1, debug build of the task branch; evidence in /Users/daniel/.paseo/evidence/task-57.23/; logcat watched for FATAL EXCEPTION / ANR after every step).

Permission matrix — states: (a) never asked, (b) denied once, (c) permanently denied (denied twice / Don't ask again), (d) granted then revoked via pm revoke / appops / Settings UI with the app backgrounded, (e) revoked while the app is in the foreground (process kill + state restoration).
- Location (FINE/COARSE): walkthrough location screen, startup location ask (AppStartupEffects), Settings > Locations current-location row, current city in zmanim/day detail, widgets.
- Calendar (READ_CALENDAR): walkthrough, Settings > calendar selection, system events in Month/Agenda/Day detail, widgets.
- Contacts (READ/WRITE_CONTACTS): walkthrough, Events tab birthdays panel (import), add birthday to contact.
- Notifications (POST_NOTIFICATIONS): walkthrough, Settings notification switches, scheduled reminders (worker run), boot receiver, quick tile.
Also: all four revoked at once (pm reset-permissions) and cold start.

States list:
- Empty: Agenda empty, Events empty, Agenda search no results, city search no match, saved cities empty.
- Error/edge: location unavailable (permission granted, no fix), zmanim at polar latitude (custom far-north city, June and December dates), contacts import failure / no contacts, no network (update check, any other network feature).
- Loading: first launch, calendar provider query (many events).
- Language: repeat key screens in Hebrew UI (app locale he).

Output: results table (state → result → evidence → follow-up task), new TASK-57 subtasks (label design-review) for each crash, dead end or missing state.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Verification results (2026-11-02)

Tested commit: ec31da0 (task branch, no app code changed), debug build `0.4.3-debug`, installed with `adb install` on a dedicated headless emulator Levana_Agent_Template_API_36_1 (Android 36.1, Google Play image, device time zone Asia/Jerusalem, locale en-US; app locale switched to he with `cmd locale set-app-locales`). Logcat checked for `FATAL EXCEPTION` / `ANR in` / `E AndroidRuntime` after every step and after two reboots.
Evidence: /Users/daniel/.paseo/evidence/task-57.23/ (file numbers below refer to it).

**Crashes / ANRs: none.** The only crash dialog seen was the Developer "Test crash" (ACRA dialog `44-test-crash.png`); ACRA shows the unsent report again on the next launch until "Don't send", which is expected ACRA behaviour.

### Permissions

| Permission | State | Result | Evidence | Follow-up |
|---|---|---|---|---|
| All | Never asked (fresh install, first launch) | Walkthrough shows each permission screen with Allow / Not now; skipping leaves features in their no-permission state; no startup prompt | 02, 07, 08, 09 | — |
| Contacts | Denied once (walkthrough) | "Not allowed. Everything else works, and you can allow it later from Settings." | 04 | — |
| Contacts | Permanently denied | Walkthrough Allow, My events "Grant Permission" and Birthdays "+" do nothing (no dialog, no message) | 05, 16, 18, 72 (he) | TASK-57.29 |
| Contacts | Granted | Picker, Add birthday, Save, list all work | 33, 35 | — |
| Contacts | Revoked in background (pm revoke) | Birthdays returns to the permission panel; no crash | 54 | — |
| Contacts | Revoked from system Settings UI with Add birthday open | Process killed; Recents restores Add birthday (picked contact lost); picking again and Save shows a raw SecurityException message | 95, 96, 97 | TASK-57.32 |
| Notifications | Denied once / permanently denied (walkthrough) | Same as contacts; Allow does nothing once permanent | 06 | TASK-57.29 |
| Notifications | Permanently denied, Settings switch | Turning a reminder on does nothing; it stays off | 60 | TASK-57.29 |
| Notifications | Granted | Candle-lighting alarm scheduled for Fri 08:00 and posted when the clock was moved ("Candle Lighting at 4:22 PM · Shabbat ends 5:18 PM") | (dumpsys in session) | — |
| Notifications | Revoked in foreground (pm revoke) | Process killed, user lands on launcher; relaunch fine; reminder switches stay on with no warning; alarm consumed with nothing posted, no crash | 46, 48, 49 | TASK-57.30 |
| Calendar | Denied once (walkthrough), then permanently denied (System calendars) | System calendars "Grant Permission" does nothing once permanent (en + he) | 14, 15, 78 | TASK-57.29 |
| Calendar | Granted (local calendar seeded by adb) | Selection lists the calendar; events in Month and Agenda | 36, 37 | — |
| Calendar | Revoked in background | Events disappear from Month/Agenda, "See your phone's calendar events" banner returns, selection is kept and works again after re-grant | 52, 53, 93 | — |
| Location | Denied once, permanently denied (Cities row) | Dialog on first two taps; then "Tap to allow location access" does nothing | 20, 21, 74 (he) | TASK-57.29 |
| Location | Granted, no fix yet | Row shows the time-zone city with no state at all | 28 | TASK-57.34 (→ 57.17 design) |
| Location | Granted, fix | Current location updates on resume (Longyearbyen, Jerusalem) | — | — |
| Location | Revoked from system Settings UI while on Cities | Process killed; Recents restores Cities; row keeps stale fix name under "Based on your time zone" | 58, 59, 55 | TASK-57.34 |
| All | All permanently denied, cold start, Hebrew | Every screen opens; same dead buttons | 71–81 | TASK-57.29 |
| All | Reboot with all denied / all granted | BOOT_COMPLETED handled, DailyNotificationWorker SUCCESS, candle-lighting alarm rebuilt, no crash | (logcat in session) | — |
| Widgets + tile | Through all of the above | Today/Month/Zmanim widgets and the quick tile kept rendering; no widget crash | 38, 40, 41, 50, 51 | — |

### Empty, error and loading states

| State | Result | Evidence | Follow-up |
|---|---|---|---|
| Agenda empty | Not reachable: Agenda always has Shabbat/holiday entries | 12 | — |
| Events empty | "No custom events yet / Tap + to add a custom event" (en + he) | 17, 73 | — |
| Birthdays empty | "No birthdays yet / Tap + to add…" | 32 | — |
| Agenda search no results | "Nothing matches “zzqxw” in the past or next 12 months" (en + he) | 13, 77 | — |
| City search no match | Blank page (en + he) | 22, 75 | TASK-57.33 |
| Saved cities empty | Only the "Add location" row; acceptable | 20 | — |
| Location unavailable | Granted but no fix: silent time-zone city; offline geocoder: "Current Location" (untranslated) | 28, 57 | TASK-57.34 |
| Zmanim at high latitude (St. Petersburg June; Longyearbyen midnight sun and polar night) | No crash; '--:--' and '—' without explanation; havdalah and candle lighting silently omitted in Agenda, no reminder; widgets show '—' | 25–27, 29–31, 40, 41, 79 | TASK-57.31 |
| Contacts import/save failure | Deleted contact: silent "success"; no permission: raw exception text | 61, 97 | TASK-57.32 |
| No network | Only network uses are the update check (silent background worker, returns success on failure; no UI on adb installs) and reverse geocoding (falls back to "Current Location"); no crash | 56, 57 | TASK-57.34 |
| Crash-report dialog | ACRA dialog with app strings and theme; works | 44 | — |
| Loading: first launch | System splash, then walkthrough in < 1 s | 01, 02 | — |
| Loading: calendar provider (~400 events) | Agenda shows a small progress indicator for a moment, then the list; no jank observed | 90, 91, 92 | — |

### Limitations
- Permanently denied states were reached both through real dialogs and by `pm set-permission-flags … user-fixed`; revocation used `pm revoke` and the system Settings UI. `appops` was not used separately (Settings UI covers the same path).
- Revoke "while the app is in the foreground" can't literally happen with the Settings UI on a phone; it was done from the app's own task (Settings opened over it) and via `pm revoke` with the app resumed. With `pm revoke` the task was sometimes not restored (launcher / Month); with the Settings UI it was restored each time.
- Reminder delivery was tested only for candle lighting (moving the clock with `cmd alarm set-time`); other reminder types share NotificationPoster's permission check. Real device reboot / Doze not covered.
- Polar zmanim used the bundled St. Petersburg and GPS fixes; the app has no custom coordinate entry. Halachic correctness of the values was not reviewed (TASK-57.7).
- Emulator geocoder (Google backend) only; TASK-44 still needs a real-device check.
- In one Add-birthday attempt the first contact pick did not register; it did not reproduce on retries and was probably my tap landing during the picker's opening animation.
<!-- SECTION:NOTES:END -->

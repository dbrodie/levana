---
id: TASK-57.29
title: >-
  Permanently denied permissions leave dead Grant/Allow buttons with no way to
  system settings
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T14:27:00Z'
updated_date: '2026-11-14T21:38:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: high
type: bug
ordinal: 99000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-57.23 (emulator Levana_Agent_Template_API_36_1, API 36.1, debug build of ec31da0, English and Hebrew UI). After a permission is permanently denied (denied twice, flags USER_SET|USER_FIXED), every in-app control that requests it calls the system launcher, which returns 'denied' immediately: no dialog, no message, no change on screen. No code opens Settings.ACTION_APPLICATION_DETAILS_SETTINGS. No crash or ANR.

Dead controls:
- Calendar: Settings > System calendars (and Agenda 'Choose calendars') 'Grant Permission' (CalendarSelectionScreen.kt PermissionRequest). Evidence: /Users/daniel/.paseo/evidence/task-57.23/15-calsel-permdenied-tap.png, /Users/daniel/.paseo/evidence/task-57.23/78-he-calsel-permdenied.png
- Contacts: My events > Birthdays 'Grant Permission' and the '+' FAB (EventsScreen.kt ~239-298). Evidence: /Users/daniel/.paseo/evidence/task-57.23/16-events-empty.png, /Users/daniel/.paseo/evidence/task-57.23/18-add-birthday-nocontacts.png, /Users/daniel/.paseo/evidence/task-57.23/72-he-birthdays-permdenied.png
- Location: Cities > current-location row 'Tap to allow location access' (LocationsSettingsScreen.kt). Evidence: /Users/daniel/.paseo/evidence/task-57.23/21-settings-location-permdenied.png, /Users/daniel/.paseo/evidence/task-57.23/74-he-cities-permdenied.png
- Notifications: Settings reminder switches; turning one on does nothing and it stays off (NotificationsSection.kt setNotificationSwitch). Evidence: /Users/daniel/.paseo/evidence/task-57.23/60-settings-notif-permdenied-toggle.png
- Walkthrough 'Allow …' buttons: denied instantly, but here the screen does show 'Not allowed … you can allow it later from Settings' (/Users/daniel/.paseo/evidence/task-57.23/05-wt-contacts-permdenied-tap-allow.png, /Users/daniel/.paseo/evidence/task-57.23/81-he-wt-calendar-tap-allow-permdenied.png). The in-app Settings it points to has no route to grant contacts/calendar.

Fix direction: when the launcher returns denied and shouldShowRequestPermissionRationale is false after a request, offer 'Open settings' (app details). Visual design of the variant is TASK-57.18 (spike); this task is the functional fix.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Each listed control, when the permission is permanently denied, leads the user to the app's system settings page (or explains how) instead of doing nothing
- [x] #2 Returning from system settings with the permission granted updates the screen without a restart
- [x] #3 Verified on an emulator in English and Hebrew
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Shared helper ui/PermissionSettings.kt: pure isPermanentlyDenied(results, shouldShowRationale) (denied and no rationale for any requested permission), Context.openAppSettings() (ACTION_APPLICATION_DETAILS_SETTINGS) and openAppNotificationSettings() (ACTION_APP_NOTIFICATION_SETTINGS), and rememberPermissionRequester(permissions, notifications, onResult): request() launches the system dialog, or opens system settings once a request came back permanently denied; blocked flag saved across configuration changes.
2. Calendar selection (Settings > System calendars and Agenda Choose calendars): PermissionRequest takes blocked; when blocked, short explanation + the same button reads 'Open settings'. Reload on resume while permission is missing.
3. Birthdays tab + FAB: same button variant; FAB opens settings when blocked. Events already reloads birthdays on resume.
4. Cities current-location row: supporting text explains and tap opens app settings when blocked; recheck permission on resume and fetch the current location when newly granted.
5. Notification switches: when blocked, an AlertDialog (Notifications are off / Open settings / Cancel) opens app notification settings; on resume, if granted, the switch the user tried to turn on is turned on.
6. Walkthrough: when a permission is permanently denied, its Allow button reads 'Open settings' and opens app settings (existing resume refresh updates status).
7. Strings EN + iw (plural-you Hebrew per current strings). Unit tests for isPermanentlyDenied and settings intents; Roborazzi for the blocked variants (calendar selection, birthdays, cities). Build/test/lint; emulator check EN+HE with pm revoke + USER_FIXED, grant from settings, confirm refresh.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation (c489276):
- ui/PermissionSettings.kt: isPermanentlyDenied(results, rationale) (nothing granted, no rationale for any), app/notification settings intents, rememberPermissionRequester (request() asks, or opens system settings once blocked; blocked saved across config changes), PermissionButton / PermissionBlockedNote shared UI. findActivity moved here from AppStartupEffects.
- Calendar selection (Settings > System calendars, Agenda > Choose calendars), Birthdays tab + FAB: once blocked, explanation line + the same button reads 'Open settings' (FAB opens settings directly). Calendar selection now reloads on resume while permission is missing; Events already did.
- Cities current-location row: blocked text 'Tap to allow location access in system settings', tap opens app settings; permission rechecked on resume and the current location fetched when newly granted.
- Notification switches: once blocked, a dialog (Notifications are off / Open settings / Cancel) opens the app's notification settings; on return, if granted, the switch the user tried to turn on is turned on.
- Walkthrough: a blocked permission's Allow button reads 'Open settings' and opens system settings (existing resume refresh shows the granted state).
- Strings EN + iw: common_open_settings, permission_blocked_explanation, location_permission_blocked, notifications_blocked_title/text.

Verification (tested commit c489276):
- set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug: success; 1295 tests, 0 failures; lint clean; existing Roborazzi baselines unchanged. Log: /Users/daniel/.paseo/evidence/task-57.29/gate.log
- New tests: PermissionSettingsTest (denial logic, intents); PermissionBlockedScreenshotTest (Robolectric full app, EN/HE light/dark): Cities, System calendars, Birthdays, notifications dialog reach the blocked state after a denied/no-rationale result and open the right settings action. New baselines: app/src/test/screenshots/*permission_blocked*, settings_notifications_blocked_*.
- Emulator Levana_Agent_Template_API_36_1 (API 36.1), debug build of the working tree committed as c489276. All permissions pm revoke + USER_SET|USER_FIXED. English: walkthrough location/contacts/notifications/calendar (01-10), Settings > System calendars (11-13), My events Birthdays button + FAB (14-16), Cities (17-19), Fasts switch dialog (20-21). Hebrew (app locale iw): Agenda > Choose calendars (22-24), Birthdays (25-26), Cities (27-28), Fasts dialog (29-30), walkthrough contacts via About > replay (31-32). In each case the first tap turns the control into Open settings (or the dialog), the next opens com.android.settings app details (notification page for notifications); after granting there (pm grant while in Settings) and Back, the screen updated without restart (calendars list, birthdays list, location row, Fasts switch on, walkthrough 'allowed'). No crashes in logcat. Evidence: /Users/daniel/.paseo/evidence/task-57.29/*.png + matching UI dumps *.xml.

Limitations:
- Detection needs one tap: the system gives no 'permanently denied' state before a request, so the first tap after a fresh screen gets the instant denial and then shows Open settings.
- Dismissing the very first system dialog without choosing (tap outside) also returns denied with no rationale, so it shows Open settings early; that route still works.
- Grants were made with pm grant while the Settings page was open (same permission state as toggling there), not by tapping through the Settings UI.
- Visual design of the variant is TASK-57.18; this uses existing Button/AlertDialog components.

Store listing: no change (only permission-denied states, not in store screenshots or text).
<!-- SECTION:NOTES:END -->

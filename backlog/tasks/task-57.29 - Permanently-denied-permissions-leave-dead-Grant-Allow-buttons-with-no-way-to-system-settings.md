---
id: TASK-57.29
title: >-
  Permanently denied permissions leave dead Grant/Allow buttons with no way to
  system settings
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T14:27:00Z'
updated_date: '2026-11-14T21:00:00Z'
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
- [ ] #1 Each listed control, when the permission is permanently denied, leads the user to the app's system settings page (or explains how) instead of doing nothing
- [ ] #2 Returning from system settings with the permission granted updates the screen without a restart
- [ ] #3 Verified on an emulator in English and Hebrew
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

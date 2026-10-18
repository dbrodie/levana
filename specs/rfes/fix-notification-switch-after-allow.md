# Fix: notification switch stays OFF after granting permission

## Problem
In Settings → Notifications, turning a switch ON without POST_NOTIFICATIONS
granted shows the system "Allow Levana to send you notifications?" dialog.
After tapping **Allow** the permission is granted but the switch stays OFF;
the user has to tap it again.

## Root cause
`NotificationsSection` in `ui/settings/SettingsScreen.kt` has a helper
`ensurePermissionThenEnable(onGranted)`. When permission is missing it launches
the permission request, but the launcher's result callback only updates a local
`hasNotificationPermission` flag. The `onGranted` lambda (which sends the
`SetNotify…(true)` intent) is dropped, so nothing is ever enabled.

## Fix
- Add `NotificationSwitch` (enum of every notification switch: candle lighting,
  holidays, fasts, personal events, Omer, Omer at tzait, Omer morning) with a
  mapping to its `SettingsIntent`, plus a pure function
  `notificationPermissionResultIntent(pending, granted)` that returns the intent
  to send once the permission result arrives (or `null`).
- In `NotificationsSection`, remember the switch that triggered the request in
  `rememberSaveable` (stored by enum name) so it survives configuration changes
  while the dialog is showing. The launcher callback sends the intent for that
  switch only if granted, then clears the pending switch.
- Permission is checked at tap time (`checkSelfPermission`) instead of cached
  once, so revoking it from system settings is handled.
- All notification switches go through one `setNotificationSwitch(switch, enabled)`
  helper: OFF always applies directly with no request; ON applies directly if
  granted, otherwise requests permission.
- Denied (including when the system no longer shows the dialog after repeated
  denials and returns `false` immediately): the switch stays OFF. No new UI.

## Test plan
- Unit tests for `notificationPermissionResultIntent` and the switch → intent
  mapping.
- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest`.
- Emulator: with permission revoked, tap Candle Lighting → Allow → switch ON;
  force-stop + relaunch → still ON. Revoke again, tap Holidays → Don't allow →
  stays OFF.

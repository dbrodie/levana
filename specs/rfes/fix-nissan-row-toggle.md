# Fix: Settings switch rows only toggle via the switch

## Problem

In Settings → Minhag (now Settings → Yahrzeit settings), the "Also mark 1 Nissan" row (yahrzeit on 30 Adar I) only
toggles when the user taps the switch itself. Tapping the title or description
does nothing. The row is a `ListItem` with a `Switch` in `trailingContent` that
owns the click handler; the row has no click handling of its own.

Every other switch row in `ui/settings/SettingsScreen.kt` is built the same way
and has the same problem:

- Preferences: Israel / Diaspora, Modern Israeli holidays
- Minhag (now Yahrzeit settings): Also mark 1 Nissan
- Appearance: Holiday theming
- Notifications: Candle lighting, Holidays, Fasts, Personal events, Omer,
  Omer at tzait, Omer morning reminder
- Locations screen: Use current location

TalkBack also sees the switch as a separate element from its label.

## Fix

Add one private composable, `SettingsSwitchRow`, in `SettingsScreen.kt`:

- A `ListItem` whose modifier is
  `Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)`.
- The trailing `Switch` gets `onCheckedChange = null`, so it only displays state
  and the tap is handled once, by the row.

This is the standard Compose toggleable-row pattern. The row merges its
descendants' semantics, so TalkBack announces one switch with its title (and
description), and a tap anywhere on the row toggles it once.

All switch rows listed above use `SettingsSwitchRow`. Each row passes the same
callback it passes now, so saved values and the notification-permission check
work as before.

## Out of scope

- Halachic Times checkbox rows already toggle from the whole row. Their
  checkbox still has its own handler, which is a separate, smaller
  accessibility issue that this fix does not touch.
- Developer settings has no switches.

## Test plan

- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
- Emulator: in Settings → Yahrzeit settings (formerly Minhag), tap the "Also mark 1 Nissan" title text. The
  switch flips once. Force-stop and relaunch the app. The new value is still
  set.

# Fix: App Closes When the Launcher Icon Changes

## Summary

`DynamicIconManager` swaps the launcher icon by disabling one launcher component and enabling
another. Android finishes every activity that was started through a component when that
component is disabled — `DONT_KILL_APP` only spares the process. Because `MainActivity` is both
the UI and the default launcher entry, and the dated aliases target it directly, every icon
switch closes the app:

- **Fresh install:** the first `ON_RESUME` disables `MainActivity` itself; the app vanishes on
  first launch (now also right as the location prompt appears).
- **Hebrew day changes:** the alias the task was launched from is disabled (on resume, or at
  midnight via `DateChangeReceiver`); the task is closed and the back stack is lost.

Reproduced on master: launched via `MainActivityDay07`, set the dev date override to day 9,
resumed → task `CLOSE`d ~1 s later, launcher back on top.

This revives the launcher half of the abandoned `fix-onboarding-location-exit` branch
(commit `122d3a1`); its onboarding half is obsolete since onboarding was removed.

## Fix

Separate the launcher entry from the UI activity:

- New `LauncherActivity` — the default launcher component. It forwards its intent (action, data,
  extras, flags) to `MainActivity` and finishes in `onCreate`.
- All 30 `MainActivityDayNN` aliases target `LauncherActivity`.
- `LauncherActivity` uses `taskAffinity=""`, `noHistory` and `excludeFromRecents`, so the
  forwarded launch (which keeps the launcher's `NEW_TASK` flag) lands in `MainActivity`'s own
  task, whose root is `MainActivity` — not a toggled component.
- `MainActivity` is a plain exported activity with no launcher filter, never toggled.
  `DynamicIconManager` toggles `LauncherActivity` + aliases only.
- Notification and Quick Settings intents keep targeting `MainActivity` directly.

The alias names stay `MainActivityDayNN` so existing users' enabled-alias state carries over.

### Alternatives considered

- *Switch the icon only while backgrounded:* the task is still finished, just invisibly — the
  user loses their place, and the midnight receiver already runs in the background.
- *Foreground service during the switch:* doesn't stop activity finishing; adds complexity.

## Verification

- Robolectric: `LauncherActivity` forwards intent + finishes; `DynamicIconManager` never
  touches `MainActivity`; aliases target `LauncherActivity`.
- Emulator: repeat the day-override repro → app stays in the foreground. Launch from icon,
  relaunch while running (no duplicate instance), notification / Quick Settings still open.

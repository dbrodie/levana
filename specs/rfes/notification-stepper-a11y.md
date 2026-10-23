# Fix: notification steppers — disabled boundaries and contextual accessibility

Beads: `levana-419`. Related: `settings-checkbox-row.md` (`levana-8pf`).

## Problem

Settings → Notifications has four duplicated decrement/increment layouts:

| Setting | Headline | Value text | Bounds | Step | Intent |
|---|---|---|---|---|---|
| Candle lighting, morning mode | `settings_notification_time` | `settings_at_morning_time` | 360..720 min | 30 | `SetCandleLightingMorningTime` |
| Candle lighting, hours-before mode | `settings_hours_before` | `settings_hours_before_sunset` | 1..6 h | 1 | `SetCandleLightingHoursBefore` |
| Holidays | `settings_days_before` | `settings_days_before_value` | 0..14 days | 1 | `SetHolidayNotifyDaysBefore` |
| Omer morning reminder | `settings_reminder_time` | `settings_at_morning_time` | 360..720 min | 30 | `SetNotifyOmerMorningTime` |

Bounds are checked inside the click callbacks, so at a limit the `IconButton`
stays enabled and silently does nothing. Button labels are the generic
"Earlier/Later/Decrease/Increase" and do not name the setting or its value.

## Fix

New `internal` composable `SettingsStepperRow` in
`ui/settings/SettingsStepperRow.kt`:

```kotlin
@Composable
internal fun SettingsStepperRow(
    title: String,
    valueText: String,
    value: Int,
    range: IntRange,
    step: Int,
    decreaseAction: String,   // e.g. "Earlier" / "Decrease"
    increaseAction: String,   // e.g. "Later" / "Increase"
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
```

- Same visual layout as today: `ListItem` with headline `title`, supporting
  `valueText`, the 16 dp leading spacer, trailing `Row` of `Remove`/`Add`
  `IconButton`s.
- `canDecrease = value > range.first`, `canIncrease = value < range.last`
  (identical to the existing guards). The buttons use these as `enabled`, so
  they are visibly (Material 3 disabled colors) and semantically disabled.
- A click emits `onValueChange((value ∓ step).coerceIn(range))` exactly once.
  Clamping only matters for values that are not step-aligned, which the UI never
  produces; normal behavior is unchanged.
- Semantics:
  - The text part (`ListItem`) is one merged node reading
    "<title>, <value>"; the buttons stay separate focusable/clickable nodes and
    are not merged into it or into a single toggle.
  - Each button's content description is localized and contextual:
    `settings_stepper_action` = "%1$s, %2$s" (action, setting), e.g.
    "Earlier, Notification time"; Hebrew "מוקדם יותר, שעת ההתראה". The existing
    action words are reused.
  - Each button exposes `stateDescription = valueText`, so the current value is
    read with the button and TalkBack can announce the new value after
    activation while focus stays on the button.
- Keyboard: a disabled button drops focus (on device it jumped to the toolbar
  Back button). When a keyboard activation reaches a bound, focus is first
  moved to the opposite button of the row; only in keyboard input mode, so
  touch and TalkBack focus are unaffected.
- New strings live in `values/accessibility_419_strings.xml` and
  `values-iw/accessibility_419_strings.xml` to avoid merge conflicts.

`SettingsScreen` replaces only the four stepper `ListItem`s with
`SettingsStepperRow` calls carrying the same bounds, steps, value text and
intents. Persistence (ViewModel → `PreferencesRepository`) and rescheduling
(`DailyNotificationRefresh` observing preferences) are untouched.

## Out of scope

Event-editor year limits (`levana-v57`, `levana-oub`), other settings rows,
mode dialog, switch rows, the morning-time formatting itself.

## Test plan

- Robolectric Compose tests for `SettingsStepperRow`: labels and state
  description on both buttons; the text node merged as title+value and separate
  from the two buttons; lower bound disables decrease only, upper bound disables
  increase only, and clicks on disabled buttons emit nothing; each enabled click
  emits one value changed by one step; accessibility `OnClick` action; keyboard
  Enter on a focused button; RTL layout order; Hebrew label format.
- Screen-level test over the four settings' bounds/steps via `SettingsScreen`
  content if feasible, else via the row with the exact parameters.
- `:app:assembleDebug`, `:app:lintDebug`, `:app:testDebugUnitTest`.
- Emulator (own read-only session), English and Hebrew: step each setting to
  both bounds, confirm disabled state in the UI hierarchy and screenshot, labels
  and value in hierarchy, keyboard activation, value persists after force-stop.
  TalkBack spoken output only claimed if actually run.

## Verification (2026-10-23)

- Robolectric: `SettingsStepperRowTest` 16/16 (row semantics, both bounds for
  the row and for each of the four settings through `NotificationsSection`,
  one emission per activation, accessibility `OnClick`, keyboard Enter,
  keyboard focus kept in the row at a bound (fails without the fix), touch does
  not move focus, RTL order, Hebrew labels). Full `:app:testDebugUnitTest`
  541 tests, 0 failures; `:app:assembleDebug` and `:app:lintDebug` succeed with
  no lint issues in the touched files.
- Emulator `emulator-5558` (own read-only session), English: each setting
  stepped to both bounds; the boundary button is `enabled=false` in the
  hierarchy, visibly dimmed, and a tap on it does nothing; 5 taps from 1 h and
  14 taps from 0 days produced exactly 5 and 14 steps. Values survive
  force-stop/relaunch; a change triggers `DailyNotificationWorker` (SUCCESS).
  Keyboard: Tab reaches each enabled button, Enter and D-pad center step once,
  Space does nothing (Compose default); reaching a bound keeps focus on the
  other button.
- TalkBack 16 (verbose log output, focus moved by keyboard): buttons are spoken
  as "5 hours before sunset, Increase, Hours before, Button"; activating speaks
  the new value "6 hours before sunset", then "6 hours before sunset, Decrease,
  Hours before, Button". Hebrew: "12 ימים לפני. הגדלה, ימים לפני. Button", and
  activation speaks "13 ימים לפני".
- Hebrew RTL: labels localized, decrease on the right, disabled "+" dimmed.
  Font scale 2.0 in English and Hebrew: rows wrap without clipping.
- Not verified: TalkBack's spoken output for a *disabled* boundary button.
  Injected input cannot drive explore-by-touch or TalkBack's own linear
  navigation, and keyboard focus skips disabled controls; the disabled state is
  covered by the hierarchy (`enabled=false`) and Robolectric only.
- Pre-existing, unchanged: 720 min reads "At 12:00 AM" and singular values read
  "1 hours"/"1 days" (`levana-851`).

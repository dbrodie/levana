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

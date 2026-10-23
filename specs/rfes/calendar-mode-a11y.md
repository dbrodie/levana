# Fix: calendar mode selector semantics

Beads: `levana-5xh`.

## Problem

The calendar header's Hebrew/Gregorian mode switch (`CalendarMonthHeader` in
`CalendarScreen.kt`) is two hand-styled `Text` composables with `Role.Tab`,
`selected` semantics and `clickable(enabled = <not selected>)`. The selected
mode is therefore also reported as disabled, the parent row has no
selection-group semantics, and both options dispatch the same
`ToggleCalendarHebrewMode` intent, so any activation of an option blindly
inverts the stored mode instead of choosing the option that was activated.

## Fix

- New `internal` composable `CalendarModeSelector` in its own file
  `ui/calendar/CalendarModeSelector.kt`:

  ```kotlin
  @Composable
  internal fun CalendarModeSelector(
      hebrewSelected: Boolean,
      onSelectMode: (hebrew: Boolean) -> Unit,
      modifier: Modifier = Modifier,
  )
  ```

  It uses the Material 3 `SingleChoiceSegmentedButtonRow` with two
  `SegmentedButton`s (Hebrew first, Gregorian second, matching the current order;
  RTL mirrors automatically). The row provides `selectableGroup()`; each button
  is `selectable` with `Role.RadioButton`, always enabled, and reports
  `selected`. Each button's `onClick` calls `onSelectMode` with its own mode
  — never a toggle. The standard selected checkmark is kept, so selection is not
  conveyed by colour alone. Labels keep the current compact `labelSmall`
  SemiBold style, single line, so the header row does not grow; the standard
  minimum 48dp touch target is kept.

- `CalendarIntent.ToggleCalendarHebrewMode` is replaced by
  `CalendarIntent.SetCalendarHebrewMode(hebrew: Boolean)`. `CalendarViewModel`
  saves the preference only when the requested mode differs from the current
  one, so activating the already-selected option is a no-op and choosing the
  other option switches exactly once. Persistence (DataStore
  `calendarHebrewMode`), the reactive mode switch, month loading and date
  selection are unchanged.

- `CalendarMonthHeader` replaces the two `Text` tabs and their wrapper `Row`
  with `CalendarModeSelector`. Existing strings `calendar_mode_hebrew` /
  `calendar_mode_gregorian` are reused; no new strings.

## Out of scope

Calendar calculations, date selection, go-to-date dialog, other header
controls, Settings.

## Test plan

- Robolectric Compose tests for `CalendarModeSelector`:
  - exactly two `Role.RadioButton` nodes labelled Hebrew/Gregorian, both
    enabled, exactly one selected, inside a `SelectableGroup` parent;
  - tapping / accessibility-clicking the selected option reports its own mode
    (never the other); tapping the other reports the other mode once;
  - stateful harness: tapping the selected option twice leaves the selection,
    tapping the other switches once;
  - keyboard: focus an option and press Enter selects it;
  - RTL: Hebrew option is on the right; semantics unchanged;
  - minimum 48dp touch target height.
- ViewModel test: `SetCalendarHebrewMode(current)` does not change the stored
  preference; `SetCalendarHebrewMode(other)` switches once and the state
  follows.
- `:app:assembleDebug`, `:app:lintDebug`, full `:app:testDebugUnitTest`.
- Emulator, English and Hebrew: UI hierarchy shows two selectable/checkable
  options with one selected and neither disabled; tapping the selected option
  keeps the mode; tapping the other switches; the selected date is kept; mode
  persists across force-stop/relaunch; header layout screenshot compared to
  before. Keyboard (D-pad/Tab + Enter) via `adb input keyevent`. TalkBack
  spoken output is claimed only if actually run.

## Verification (2026-10-23)

- Robolectric: `CalendarModeSelectorTest` 10/10; new
  `ReactiveCalendarRefreshTest` case for `SetCalendarHebrewMode` passes; full
  `:app:testDebugUnitTest` 536 tests, 0 failures, 0 errors, 0 skipped.
  `:app:assembleDebug` and `:app:lintDebug` succeed; the only lint finding in the
  touched files is the pre-existing `ModifierParameter` on `CalendarScreen`
  (line 127, unchanged).
- Emulator `emulator-5560` (owned read-only `Levana_Agent_Template_API_36_1`
  session, evidence under `/tmp/levana-emu-gpi_auo0/artifacts/`):
  - Before (master APK): the selected Gregorian option was
    `selected=true enabled=false focusable=false`.
  - After, English and Hebrew: each option is one checkable, enabled, focusable
    node; exactly one is `checked=true`. The checked option reports
    `clickable=false`, which is how Android exposes a checked radio button (it
    cannot be unchecked); it is not disabled.
  - Tapping the selected option keeps the mode; tapping the other switches once;
    tapping it again keeps it. The selected day (Oct 15) is kept across the
    switch. Mode persists across force-stop/relaunch, in both languages.
  - Hebrew locale: order is mirrored (עברי on the right); both header rows keep
    their height (selector 126px = 48dp touch target, visual 40dp); subtitles
    fit in all four combinations.
  - Keyboard: Tab reaches both options (Hebrew then Gregorian), including the
    selected one; Enter on the unselected option switches; Enter/Space on the
    selected option keeps the mode and focus. After a switch, focus moves to the
    menu button because the per-mode header is recomposed (pre-existing
    Crossfade behaviour, not changed here).
- TalkBack: enabled on the emulator, but adb-injected taps, swipes and
  Alt+arrow keys did not move TalkBack focus, and the image has no root to raise
  TalkBack's speech logging, so spoken output was **not** verified. The
  accessibility tree shows each option as a checkable `android.view.View` with
  checked state, plus an empty, non-focusable `android.widget.RadioButton`
  child emitted by Material 3 1.3.1's `SegmentedButton`; whether TalkBack says
  "radio button" for the option needs a manual TalkBack check.

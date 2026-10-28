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
- TalkBack, first attempt (`emulator-5560`): adb-injected taps, swipes and
  Alt+arrow keys did not move TalkBack focus, so no speech was captured there.
- TalkBack spoken output, second session (`emulator-5556`, owned read-only
  session, evidence under `/tmp/levana-emu-qkq71rin/artifacts/`, `*-logcat.txt`
  per step). The TalkBack log level was set to VERBOSE in TalkBack's developer
  settings on the disposable emulator; focus was moved with keyboard Tab and
  activation was via Enter. The spoken text was read from
  `SpeechControllerImpl: Speaking fragment` lines (TalkBack 16.0, order
  "state, name, type"):
  - English: "Not selected, Hebrew, Radio button, 1 of 2, In list, 2 items";
    "Selected, Gregorian, Radio button, 2 of 2". Focus moves directly from one
    option to the other; the empty `RadioButton` child is never focused or
    spoken.
  - Enter on the selected Gregorian option: nothing spoken, mode unchanged.
    Enter on Hebrew: one switch, then "Selected, Hebrew, Radio button, 1 of 2,
    In list, 2 items" (TalkBack focus returns to the new Hebrew option).
  - Hebrew: "נבחר, עברי, Radio button, 1 of 2, In list, 2 items";
    "לא נבחר, לועזי, Radio button, 2 of 2, In list, 2 items"; Enter on לועזי
    switches once, then "נבחר, לועזי, Radio button, 2 of 2, In list, 2 items".
    Role words are English because the emulator's system language is English.
  - Not covered: TalkBack touch gestures (explore by touch, double-tap), which
    adb can't inject. After a switch, TalkBack focus lands back on the selected
    option, but keyboard input focus is lost: the next Tab starts from the menu
    button (follow-up `levana-wf2`).

# Fix: calendar-selection checkbox rows lack a label and a row target

Beads: `levana-td7`. Follow-up to `settings-checkbox-row.md` (`levana-8pf`).

## Problem

In Settings → Calendars (`CalendarSelectionScreen`), each `CalendarRow` is a
`Row` with a leading Material 3 `Checkbox` that owns `onCheckedChange`, a color
dot, and the calendar name and account as sibling `Text`s. The row itself is not
toggleable, so:

- the checkbox accessibility node has no associated label (name/account are
  separate text nodes);
- tapping the name, account or empty row space does nothing.

There is only one click handler today, so no double toggle.

## Fix

Reuse `SettingsCheckboxRow` (`ui/settings/SettingsCheckboxRow.kt`, `internal`,
so usable anywhere in the app module; the calendar screen is reached from
Settings). Add one optional parameter:

```kotlin
leadingContent: (@Composable () -> Unit)? = null
```

passed straight to `ListItem.leadingContent`. Existing callers are unchanged.

`CalendarRow` becomes, inside the existing `Card`:

```kotlin
SettingsCheckboxRow(
    title = calendar.name,
    description = calendar.accountName.takeIf { it.isNotBlank() },
    checked = isSelected,
    onCheckedChange = { onToggle() },
    leadingContent = { /* 12dp color dot, no semantics */ }
)
```

- The row is the sole interaction owner: `toggleable(role = Role.Checkbox)`;
  the `Checkbox` has `onCheckedChange = null`.
- One accessibility node merges name, account and checked state; the color dot
  is decorative (no semantics, no content description).
- The checkbox moves from the start to the end of the row, matching the other
  Settings checkbox rows (Halachic Times). Name, account and color stay.
- `onToggle` still sends `ToggleCalendar(id)`; ViewModel, persistence
  (`selectedCalendarIds`), permission request and empty/loading states are
  unchanged. No selection limits are added.

## Out of scope

- `SettingsScreen.kt`, other settings rows, the ViewModel, TalkBack follow-up
  `levana-bhe`.
- No new strings.

## Test plan

- Robolectric Compose tests for `CalendarSelectionContent` with fake calendars:
  - one toggleable `Role.Checkbox` node per calendar, containing name and
    account text and the correct state; only one clickable node per row in the
    unmerged tree;
  - tapping name, account, the checkbox area and the color dot area each sends
    exactly one `ToggleCalendar(id)`;
  - accessibility `OnClick` and keyboard Enter toggle once;
  - RTL: semantics unchanged, checkbox on the left;
  - permission state still shows the grant button, no checkbox rows.
- `SettingsCheckboxRowTest` keeps passing; one case for `leadingContent`.
- `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`.
- Emulator (own session), English and Hebrew: UI hierarchy shows one checkable
  node per calendar with name/account; label/row/checkbox taps flip once;
  keyboard toggles; selection persists across force-stop/relaunch. TalkBack
  spoken output only claimed if actually run.

## Verification (2026-10-23)

- Robolectric: `CalendarSelectionScreenTest` 10/10, `SettingsCheckboxRowTest`
  11/11; full `:app:testDebugUnitTest` 536 tests, 0 failures, 0 errors
  (75 suites); `:app:assembleDebug` and `:app:lintDebug` succeed (0 lint
  errors, 84 warnings, none in changed files).
- Emulator `emulator-5562` (commit `463386e`, own read-only session), two local
  calendars. English: the real permission prompt still appears and grants; each
  calendar is one checkable/clickable/focusable node with no separate checkbox
  node; tapping the name, account, checkbox, color dot and empty row space each
  flips the state once; D-pad focus lands on each row (visible highlight), Enter
  and D-pad center toggle once (Space does nothing, as in the Settings rows);
  selection survives force-stop and relaunch.
- Hebrew (`iw` app locale): RTL with the checkbox on the left and the color dot
  on the right; name, account, checkbox and dot taps each flip once.
- TalkBack 16 (verbose log): keyboard focus on a row speaks e.g.
  "not checked. Personal, me@example.com" with hint "Double-tap to toggle" and
  `ROLE_CHECK_BOX`; same in the Hebrew app locale (state words in English, the
  system language). Gap: after keyboard Enter / Alt+Enter toggles a row,
  TalkBack speaks nothing for the new state (state-change event source is
  `ROLE_NONE` and not focused). Not verified: TalkBack double-tap gesture (adb
  gestures bypass touch exploration on this image), and whether existing
  Settings checkbox rows share the silent-toggle behavior (tracked in
  `levana-bhe`).

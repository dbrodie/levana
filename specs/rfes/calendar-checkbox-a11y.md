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

# Fix: Halachic Times checkbox rows expose two interaction targets

Beads: `levana-8pf`. Follow-up to `fix-nissan-row-toggle.md`.

## Problem

In Settings → Halachic Times, each zman row is a `ListItem` with
`Modifier.clickable` and a trailing Material 3 `Checkbox` that has its own
`onCheckedChange`. The row and the checkbox each own a click handler, so
accessibility services see two targets: an unlabeled clickable row without a
checkbox role or state, and a separate checkbox without a label. Ordinary
tapping works; no double-toggle has been reproduced.

## Fix

Add one reusable composable, `SettingsCheckboxRow`, in its own file
`ui/settings/SettingsCheckboxRow.kt` (`internal`, so it can be tested and reused
by other settings checkbox rows). It mirrors `SettingsSwitchRow`:

```kotlin
@Composable
internal fun SettingsCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
)
```

- A `ListItem` whose modifier is
  `Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)`.
- The trailing Material 3 `Checkbox` uses `onCheckedChange = null` and the same
  `enabled`, so it only displays state (including the disabled look).
- `toggleable` merges descendant semantics, so the label and state form one
  checkbox target; no manual content descriptions or gesture handling.
- Disabled rows dim the headline/supporting text with the standard
  `ListItemDefaults.colors` disabled colors and do not invoke the callback.

`HalachicTimesSection` replaces its `ListItem` + `clickable` + `Checkbox` with
`SettingsCheckboxRow` inside the existing `Card`. Zman names, ordering, card
styling and the `onToggle(name, enabled)` callback stay the same, so persistence
is unchanged.

The component lives in a separate file to keep `SettingsScreen.kt` changes
minimal. `SettingsSwitchRow` is left untouched.

## Out of scope

- No selection cap: there is no existing five-zman limit, and none is added.
- `CalendarSelectionScreen` rows are not a settings screen and are not changed.
- Switch rows and Minhag settings.

## Test plan

- Robolectric Compose tests for `SettingsCheckboxRow`:
  - one node with `Role.Checkbox`, `ToggleableState` and the label merged; no
    separately clickable child in the unmerged tree;
  - tapping the label, the checkbox area and the row each invokes the callback
    exactly once with `!checked`;
  - `performSemanticsAction(OnClick)` toggles (accessibility activation);
  - keyboard: focus the row and press Enter/Space toggles once;
  - disabled: `isNotEnabled()`, taps do not invoke the callback;
  - RTL layout direction: same semantics, and the checkbox is on the left.
- `HalachicTimesSection` test: one checkbox node per zman, toggling a row calls
  `onToggle(name, !checked)` once.
- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
- Emulator (English and Hebrew): toggle zmanim by tapping label and checkbox,
  check UI hierarchy shows one checkable node per row, force-stop/relaunch and
  confirm selections persist and the day panel shows the selected zmanim.
  TalkBack spoken output is only claimed if actually run.

## Verification (2026-10-22)

- Robolectric: `SettingsCheckboxRowTest` 10/10; full `:app:testDebugUnitTest`
  524 tests, 0 failures; `:app:assembleDebug` succeeds.
- Emulator `emulator-5558` (commit `fe7d0c4`), English: each zman row is a
  single checkable/clickable/focusable node with no separate checkbox node;
  tapping the label, the checkbox and empty row space each flips the state once;
  Tab focuses the row, Enter and D-pad center toggle it once (Space does
  nothing, which is Compose's default `clickable` behavior, same as the switch
  rows); selections survive force-stop and relaunch and appear in the day panel.
- Hebrew (`iw` app locale): RTL layout with the checkbox on the left; label and
  checkbox taps each flip once.
- Not verified: TalkBack spoken output (hierarchy checks only); disabled state on
  device (no caller disables a row; covered by Robolectric test).

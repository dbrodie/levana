# Settings Screen MD3 Redesign

## Context

The current settings screen (`SettingsScreen.kt`) uses a custom `SectionCard` composable that wraps
every group in a `Card` with `surfaceVariant` background. This is a non-standard, bespoke pattern.
Material Design 3 settings screens are flat lists: no card borders, clean surface background,
section headers as styled text labels, and `ListItem` as the standard row primitive with leading
icons, headline text, supporting text (current value), and trailing controls (Switch, ChevronRight,
etc.). Radio selections (Minhag, AppLanguage) live in `AlertDialog` dialogs triggered by tapping
the row. Numeric sliders (candle lighting offset) live in an `AlertDialog` with a `Slider`. The
goal is to make the settings screen look like a first-class Android app.

Custom card-based settings feel non-standard; this replaces it with idiomatic MD3 flat-list
settings — as seen in Google's own apps and recommended by the AOSP settings guidelines.

## Goals

- Replace `SectionCard` with flat `ListItem`-based rows and `HorizontalDivider` separators
- Add leading icons to all rows for visual scanning
- Move radio-button selections (Minhag, AppLanguage) into `AlertDialog`s
- Replace the candle lighting +/- stepper with a `Slider` in an `AlertDialog`
- Add `SettingsSectionHeader` composable for group labels

## Design

### Sections and layout

```
[no header]
  Location             → ListItem (LocationOn icon, city name as supporting, ChevronRight)
  System Calendars     → ListItem (CalendarMonth icon, ChevronRight)

─────────────────────────────────────
Calendar Preferences               ← SettingsSectionHeader
  Israel / Diaspora    → ListItem (Public icon) + trailing Switch
  Modern Israeli Holidays → ListItem (Flag icon) + trailing Switch
  Candle Lighting Offset → ListItem (WbTwilight icon, "X min before sunset", ChevronRight)
                           → taps opens AlertDialog with Slider (range 10–60, 1-min steps)

─────────────────────────────────────
Minhag                             ← SettingsSectionHeader
  Minhag               → ListItem (MenuBook icon, current minhag as supporting, ChevronRight)
                          → taps opens MinhagDialog (AlertDialog + radio list)

─────────────────────────────────────
Appearance                         ← SettingsSectionHeader
  App Language         → ListItem (Language icon, current language as supporting, ChevronRight)
                          → taps opens AppLanguageDialog (AlertDialog + radio list)
  Holiday Theming      → ListItem (Palette icon) + trailing Switch

─────────────────────────────────────
Notifications                      ← SettingsSectionHeader
  Candle Lighting      → ListItem (NotificationsActive icon) + trailing Switch
    [if enabled]
    Notify mode        → indented ListItem (supporting=current mode) → CandleLightingModeDialog
    Time sub-row       → indented ListItem with +/- steppers in trailingContent
  Holidays             → ListItem (Event icon) + trailing Switch
    [if enabled]
    Days before        → indented ListItem with +/- steppers
  Fasts                → ListItem (NoFood icon) + trailing Switch
  Personal Events      → ListItem (Person icon) + trailing Switch
  Omer (at sunset)     → ListItem (Stars icon) + trailing Switch

─────────────────────────────────────  [conditional]
Developer Settings                 ← SettingsSectionHeader
  Date Override + HolidayThemePicker (unchanged, padded with horizontal 16dp)
```

### New composables added

- `SettingsSectionHeader(text: String)` — `labelLarge` style, `primary` color, 16dp start padding
- `MinhagDialog(selected, onSelect, onDismiss)` — `AlertDialog` with radio list
- `AppLanguageDialog(selected, onSelect, onDismiss)` — `AlertDialog` with radio list
- `CandleLightingOffsetDialog(offset, onOffsetChange, onDismiss)` — `AlertDialog` + `Slider`
- `CandleLightingModeDialog(mode, onSelect, onDismiss)` — `AlertDialog` with radio list (morning / hours_before)

### Composables removed

- `SectionCard`, `ToggleSection`, `LocationSection`, `SystemCalendarsSection`
- `MinhagSection`, `AppLanguageSection`, `CandleLightingSection`, `NotificationToggleRow`

### Composables unchanged

- `SettingsScreen` (entry point), `DateOverridePicker`, `HolidayThemePicker`
- `NotificationsSection` — permission logic preserved; `SectionCard` wrapper removed, rows converted to `ListItem`

### Icons to add (all from `material.icons.outlined` + `automirrored.outlined`)

`LocationOn, CalendarMonth, Public, Flag, WbTwilight, MenuBook, Language, Palette,
NotificationsActive, Event, NoFood, Person, Stars, ArrowForwardIos (AutoMirrored)`

No new Gradle dependencies — `compose.material.icons.extended` is already in `build.gradle.kts`.

No changes to `SettingsState`, `SettingsIntent`, or `SettingsViewModel`.

## Files Changed

- **Modified**: `app/src/main/java/com/levana/app/ui/settings/SettingsScreen.kt`

## Acceptance Criteria

- [ ] No `Card` or `CardDefaults` imports remain in `SettingsScreen.kt`
- [ ] All settings rows use `ListItem` with a leading icon
- [ ] Minhag selection opens an `AlertDialog` with radio buttons; summary text updates after selection
- [ ] App Language selection opens an `AlertDialog`; app restarts in Hebrew as before
- [ ] Candle Lighting offset opens a dialog with a `Slider`; summary shows updated value
- [ ] Candle Lighting notification mode opens a dialog when tapped
- [ ] All Switch-based settings still toggle correctly
- [ ] Sub-options (candle lighting mode, holiday days-before) still appear/disappear when parent switch is toggled
- [ ] Developer settings section still appears conditionally
- [ ] Screen renders correctly in both LTR and RTL layouts
- [ ] `DateOverridePicker` and `HolidayThemePicker` are visually aligned within the developer section

# Halachic Times Integration + Add Event Toolbar

## Context

Two related UI improvements delivered in one branch:

1. **Add Event** is currently a full-width `OutlinedButton` at the bottom of the day detail panel.
   It should live as an icon in the calendar header alongside the existing controls.

2. **Zmanim** is a standalone top-level screen reached via a button at the bottom of the day panel
   or via the drawer. It feels like a bolted-on utility rather than part of the calendar experience.
   This redesign integrates halachic times directly into the day detail panel (up to 5
   user-configurable times) with a "More" action that pushes a full-list sub-screen onto the
   back stack.

## Goals

- Move "Add Event" to the calendar header as a `+` icon button
- Show up to 5 user-selected halachic times inline in the day detail panel
- Provide a "More Halachic Times" route that shows all zmanim for the selected date (no date
  browsing), navigated to from the day detail, back-stack dismissible
- Add a "Halachic Times" section in Settings with checkboxes for all available zmanim (max 5)
- Remove Zmanim from the drawer navigation
- Reorder the day detail panel: Personal Events → Holidays/Shabbat/Parasha/Omer → Halachic Times

## Design

### 1. Add Event in Calendar Header

`GregorianMonthHeader` and `HebrewMonthHeader` both gain an `Add` (`Icons.Filled.Add`) icon button
wired to the existing `onAddEvent` callback already present on `CalendarScreen`.

The `OutlinedButton("Add Event for This Day")` at the bottom of `DayDetailContent` is removed.
The `onAddEvent` callback is removed from `DayDetailContent` and `DayDetailScreen` signatures.

---

### 2. Day Detail — Halachic Times section

**New section order** (top to bottom):
1. Personal Events (`PersonalEventsSection` — custom events + birthdays + system calendar events)
2. Holidays (`HolidaySection`)
3. Shabbat times (`ShabbatTimeCard` candle lighting + havdalah)
4. Parasha (`ParshaSection`)
5. Molad (`MoladSection`)
6. Omer (`OmerSection`)
7. **Halachic Times** (`HalachicTimesSection`) ← new, always shown when zmanim loaded

**`HalachicTimesSection` layout:**
```
Halachic Times                      ← section label (same style as other day-detail headers)
  Sunrise (Netz)        6:32 AM
  Sunset (Shkiah)       7:14 PM
  Nightfall (Tzeit)     7:42 PM
────────────────────────────────────
  More Halachic Times  →            ← TextButton, navigates to full screen
```

- Times are shown as flat rows: English name on leading side, time on trailing side
- Hebrew name shown as supporting text below the English name
- If a zman's `time` is null it is omitted from the inline list (not shown at all)
- The "More Halachic Times" button is always shown (even if 0 times are selected), since the full
  screen is independently useful
- `onShowAllZmanim: (LocalDate) -> Unit` replaces `onShowZmanim: () -> Unit` in
  `DayDetailContent` — the date is passed explicitly so the sub-screen knows what to load

---

### 3. Full Halachic Times Sub-Screen

Route: `ZmanimRoute(dateEpochDay)` — unchanged route definition.

**Changes to `ZmanimScreen`:**

- **Remove** `DateHeader` composable (date prev/next navigation + DatePicker)
- The screen title (in the `CenterAlignedTopAppBar` from `MainActivity`) reads "Halachic Times"
- The screen always displays the date it was opened with; no way to navigate to other dates
- `ZmanimIntent.LoadToday` is removed; `ZmanimIntent.LoadDate` is the only intent; the screen
  fires `LoadDate(date)` on composition

**Layout — Option i (colored sticky headers + flat rows):**

```
LazyColumn {
    // For each ZmanCategory in [MORNING, AFTERNOON, EVENING, NIGHT]:
    stickyHeader {
        Box(containerColor = categoryColor) {
            Text(categoryLabel)    // "Morning", "Afternoon", "Evening", "Night"
        }
    }
    items(zmanimForCategory) { zman ->
        ListItem(
            headlineContent   = { Text(zman.name) }
            supportingContent = { Text(zman.hebrewName) }
            trailingContent   = { Text(formattedTime) }
        )
        HorizontalDivider()
    }
}
```

Category colors (MD3 container tokens):
- `MORNING`   → `MaterialTheme.colorScheme.primaryContainer`
- `AFTERNOON` → `MaterialTheme.colorScheme.secondaryContainer`
- `EVENING`   → `MaterialTheme.colorScheme.tertiaryContainer`
- `NIGHT`     → `MaterialTheme.colorScheme.surfaceVariant`

Category labels: "Morning", "Afternoon", "Evening", "Night"

Zmanim with `null` time are still shown with "—" in the time column (they exist; the time is
just unavailable for this location/date).

---

### 4. Settings — Halachic Times Preferences

New section added to `SettingsScreen` between **Appearance** and **Notifications**:

```
─────────────────────────────────────
Halachic Times                     ← SettingsSectionHeader
  (Select up to 5 to show in the day panel)   ← supporting note, bodySmall
  Alot HaShachar     □             ← Checkbox row
  Misheyakir         □
  Sunrise            ☑  (default)
  Sof Zman Shema (GRA)  □
  Sof Zman Shema (MGA)  □
  Sof Zman Tefillah  □
  Chatzot            □
  Mincha Gedolah     □
  Mincha Ketanah     □
  Plag HaMincha      □
  Candle Lighting    □
  Sunset             ☑  (default)
  Nightfall          ☑  (default)
  Havdalah           □
  Chatzot HaLaylah   □
```

- Each row: `ListItem` with `headlineContent = name`, `trailingContent = Checkbox`
- Row is clickable → toggles the checkbox
- Once 5 are checked, all unchecked rows have their `Checkbox` and click disabled (greyed out)
- Unchecking any of the 5 re-enables the remaining rows
- Note text below the header: "Shown in the day panel (up to 5)"

**Ordering:** Zmanim appear in the same order they are returned by `ZmanimRepository.getZmanim()`
— chronological within the day. The list is static (no Candle Lighting / Havdalah conditional
removal here — the user picks from the full canonical list; the day detail simply skips any
zman whose `time` is null for the selected date).

---

### 5. Navigation — Remove Zmanim from Drawer

In `MainActivity`:
- Remove `DrawerNavItem("Zmanim", Icons.Filled.WbSunny, ZmanimRoute())` from `drawerNavItems`
- Update `showDrawer` check (remove ZmanimRoute from the set of drawer-enabled routes)
- `ZmanimRoute` stays registered in `NavHost` (needed for back-stack navigation from day detail)
- Top app bar title for `ZmanimRoute` reads **"Halachic Times"**

---

## Data / State Changes

### `AppPreferences` (new field)
```kotlin
val selectedZmanim: Set<String> = setOf("Sunrise", "Sunset", "Nightfall")
```
Persisted via DataStore as a `String` set (preference key `"selected_zmanim"`).

### `PreferencesRepository` (new methods)
```kotlin
suspend fun saveSelectedZmanim(zmanim: Set<String>)
// preferences flow already exposes selectedZmanim via AppPreferences
```

### `DayDetailState` (new field)
```kotlin
val zmanim: List<ZmanTime> = emptyList()
```

### `DayDetailViewModel.loadDay()`
After the existing `getShabbatInfo(...)` call, add:
```kotlin
val zmanim = zmanimRepository.getZmanim(date, location, prefs.candleLightingOffset)
```
Include in final `_state.value` assignment:
```kotlin
_state.value = DayDetailState(
    dayInfo = dayInfo.copy(shabbatInfo = shabbatInfo),
    calendarEvents = calendarEvents,
    systemEvents = systemEvents,
    zmanim = zmanim,          // ← new
    isLoading = false
)
```

### `SettingsState` (new field)
```kotlin
val selectedZmanim: Set<String> = setOf("Sunrise", "Sunset", "Nightfall")
```

### `SettingsIntent` (new intent)
```kotlin
data class ToggleZman(val name: String, val enabled: Boolean) : SettingsIntent
```

### `SettingsViewModel.onIntent()`
```kotlin
is SettingsIntent.ToggleZman -> {
    val current = _state.value.selectedZmanim.toMutableSet()
    if (intent.enabled) current.add(intent.name) else current.remove(intent.name)
    preferencesRepository.saveSelectedZmanim(current)
}
```

### `ZmanimIntent`
Remove `LoadToday`. Keep only `LoadDate(date: LocalDate)`.

### `ZmanimViewModel`
- Remove `LoadToday` branch from `onIntent`
- `init` block: remove `onIntent(ZmanimIntent.LoadToday)` — the screen fires `LoadDate` on
  composition via `LaunchedEffect`

---

## Files Changed

| File | Change |
|---|---|
| `ui/calendar/CalendarScreen.kt` | Add `+` icon to both month headers |
| `ui/daydetail/DayDetailScreen.kt` | Reorder sections, add `HalachicTimesSection`, remove Add Event button, update `onShowZmanim` → `onShowAllZmanim(date)` |
| `ui/daydetail/DayDetailState.kt` | Add `zmanim: List<ZmanTime>` |
| `ui/daydetail/DayDetailViewModel.kt` | Call `getZmanim()` in `loadDay()` |
| `ui/daydetail/DayDetailIntent.kt` | No change |
| `ui/zmanim/ZmanimScreen.kt` | Remove `DateHeader`, redesign to sticky-header flat-list |
| `ui/zmanim/ZmanimIntent.kt` | Remove `LoadToday` |
| `ui/zmanim/ZmanimViewModel.kt` | Remove `LoadToday` handling, remove `init` auto-load |
| `ui/settings/SettingsScreen.kt` | Add Halachic Times section with checkboxes |
| `ui/settings/SettingsState.kt` | Add `selectedZmanim: Set<String>` |
| `ui/settings/SettingsIntent.kt` | Add `ToggleZman` |
| `ui/settings/SettingsViewModel.kt` | Handle `ToggleZman`, observe `selectedZmanim` from prefs |
| `data/AppPreferences.kt` | Add `selectedZmanim: Set<String>` |
| `data/PreferencesRepository.kt` | Add `saveSelectedZmanim()`, DataStore key |
| `MainActivity.kt` | Remove Zmanim drawer item, update top-bar title for ZmanimRoute |

---

## Acceptance Criteria

- [ ] `+` icon appears in the calendar header (both Gregorian and Hebrew modes); tapping opens Add Event for the selected day
- [ ] No "Add Event for This Day" button in the day detail panel
- [ ] Day detail section order: Personal Events → Holidays/Shabbat → Parasha/Molad/Omer → Halachic Times
- [ ] Halachic Times section shows only the user-selected zmanim (up to 5, default: Sunrise/Sunset/Nightfall)
- [ ] Zmanim with null time are omitted from the inline section
- [ ] "More Halachic Times" button navigates to the full screen for the selected date
- [ ] Full Halachic Times screen shows all zmanim with colored sticky section headers (Morning/Afternoon/Evening/Night)
- [ ] Full Halachic Times screen has no date-navigation controls
- [ ] Back button on full Halachic Times screen returns to calendar
- [ ] Zmanim no longer appears in the drawer
- [ ] Settings Halachic Times section lists all 15 zmanim with checkboxes
- [ ] Checking a 6th zman is prevented (unchecked rows disabled once 5 are selected)
- [ ] Unchecking re-enables disabled rows
- [ ] Selected zmanim persisted across app restarts
- [ ] RTL layout correct throughout (Hebrew name appears correctly, sticky headers full-width)

---
title: Stitch UI Redesign
status: in-progress
---

# Stitch UI Redesign

## Overview

Update the Levana app's visual design to match the "Calendar with Jewish Times v1" Stitch design.
Key changes: indigo primary color (#4051B5), navigation drawer replacing the bottom nav bar,
improved dual-date calendar cells, and an inline day-detail panel below the calendar grid.

## Reference Designs (Stitch)

- **Primary**: "Calendar with Jewish Times v1" (`0616e2210658408e93addd9c701f8d79`)
- **Drawer**: "Sidebar Navigation V2 - No Calendars (Var 1)" (`446e7b3bebe3427f9057f929f1ca6252`)

Any "Shalom Calendar" branding in the Stitch designs maps to "Levana".

---

## Changes

### 1. Theme: Colors (`ui/theme/Color.kt`, `ui/theme/Theme.kt`)

Replace LevanaBlue palette with indigo:

| Token            | Value       | Usage                     |
|------------------|-------------|---------------------------|
| LevanaIndigo     | #4051B5     | Primary (light scheme)    |
| LevanaIndigoLight| #7680D4     | Primary (dark scheme)     |
| LevanaIndigoDark | #2D3A8C     | Tertiary / pressed state  |
| LevanaBackground | #F6F6F8     | Surface / background (light) |
| LevanaDarkBg     | #14161E     | Surface / background (dark)  |
| LevanaGold       | (keep)      | Secondary                 |

Update `LightColorScheme` and `DarkColorScheme` with new tokens.
Holiday theme override system is unchanged.

### 2. Navigation: Bottom Nav → Drawer (`MainActivity.kt`)

- Remove `BottomNavItem` list and `NavigationBar`.
- Add `ModalNavigationDrawer` wrapping the `Scaffold`.
- Drawer content:
  - App branding header ("Levana")
  - Items: Calendar, Zmanim, Events, Settings (with icons)
  - Location info at the bottom
- Top app bar: hamburger `Menu` icon on root screens; back arrow on child screens.
- Drawer open/close driven by a `DrawerState` and coroutine scope.
- `showDrawer` condition: `hasLocation && currentDestination is root route`.

### 3. Calendar Screen (`ui/calendar/`)

#### 3a. Header
- Add a "Today" `TextButton` that dispatches `CalendarIntent.GoToToday`.

#### 3b. DayCell dual-date display
- `aspectRatio(0.85f)` (was `1f`) for taller cells.
- Gregorian mode: Gregorian day number is primary (`bodyLarge`); Hebrew letter below (`labelSmall`).
- Hebrew mode: Hebrew letter is primary (`bodyLarge`); Gregorian day/month below (`labelSmall`).
- Selected cell: `secondaryContainer` background (distinct from today's `primaryContainer`).

#### 3c. Inline day-detail panel
- Tapping a day selects it (`CalendarIntent.SelectDay`) instead of navigating.
- CalendarScreen embeds a `DayDetailViewModel` and `DayDetailContent` below the calendar grid.
- Default selection = today.
- Layout: `BoxWithConstraints`-computed pager height + `weight(1f)` detail panel below.
- `DayDetailRoute` is kept only for the notification deep-link path.

#### State / Intent / ViewModel additions
| File | Addition |
|---|---|
| `CalendarState.kt` | `selectedDate: LocalDate = LocalDate.now()` |
| `CalendarIntent.kt` | `GoToToday`, `SelectDay(date: LocalDate)` |
| `CalendarViewModel.kt` | Handle `GoToToday` (reset month + selectedDate), `SelectDay` (update selectedDate) |

#### CalendarScreen signature change
Old: `CalendarScreen(onDayClick: (LocalDate) -> Unit, ...)`
New: `CalendarScreen(onShowZmanim: (LocalDate) -> Unit, onAddEvent: (Int,Int,Int) -> Unit, ...)`

### 4. Other Screens

No direct changes; updated `MaterialTheme` colors propagate automatically.
Verify: `DayDetailScreen`, `ZmanimScreen`, `SettingsScreen`, `EventsScreen` have no hardcoded
hex colors conflicting with the new scheme.

---

## Verification

1. Build and run.
2. Confirm primary color is indigo (#4051B5) throughout.
3. Navigation drawer opens via hamburger, all 4 items navigate correctly.
4. Calendar header shows both date systems; "Today" returns to current month.
5. Tapping a day shows inline detail cards below the grid.
6. Dark mode uses `#14161E` backgrounds.
7. Holiday theme override still applies on top of base theme.
8. Run unit tests: `./gradlew :app:testDebugUnitTest`.

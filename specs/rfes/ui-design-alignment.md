# UI Design Alignment

**Branch:** `ui-design-alignment`
**Date:** 2026-03-12
**Status:** Merged to master

## Context

Comparing the running app against the Stitch "Calendar with Jewish Times v1", "Sidebar Navigation V2", and "Swap Icon Variation Final" designs revealed several visual gaps. This branch addresses all of them.

---

## Change 1: Day Cell Selected Indicator Shape

**File:** `CalendarScreen.kt`

- `aspectRatio(0.85f)` → `aspectRatio(1f)` (square cells so circle looks correct)
- `RoundedCornerShape(8.dp)` → `CircleShape`

---

## Change 2: Navigation Drawer Header

**File:** `MainActivity.kt`

- Replaced single `Text("Levana")` with a `Row` containing:
  - App moon crescent logo (`ic_launcher_foreground`) on a primary-colored circle
  - `Column` with "Levana" (bold headline, primary color) + "Calendar" subtitle (labelMedium, onSurfaceVariant)

---

## Change 3: Settings Separation in Drawer

**File:** `MainActivity.kt`

- Removed Settings from the main `drawerNavItems` list
- Added standalone Settings `NavigationDrawerItem` at the bottom section (after divider)

---

## Change 4: LOCATIONS Section in Drawer

**File:** `MainActivity.kt`

- Added `Text("LOCATIONS", style = labelSmall)` section header before the location item
- Location `NavigationDrawerItem` uses `selected = true` to show active highlight

---

## Change 5: Today Button — Outline to Plain

**File:** `CalendarScreen.kt`

- Changed `FilledTonalIconButton` (then `OutlinedIconButton`) → plain `IconButton` for the today button in both Gregorian and Hebrew headers

---

## Change 6: Remove Personal Events from Settings

**File:** `SettingsScreen.kt`, `MainActivity.kt`

- Removed "Personal Events" section card from Settings screen
- Events is now only accessible via the sidebar navigation drawer

---

## Change 7: Calendar Mode Toggle Button (Swap Icon)

**Files:** `CalendarScreen.kt`, `CalendarIntent.kt`, `CalendarViewModel.kt`

- Added `SwapVert` icon button next to the month title in both Gregorian and Hebrew headers
- Tapping toggles between Gregorian and Hebrew-primary calendar modes
- Implemented via new `ToggleHebrewPrimary` intent → `preferencesRepository.saveHebrewPrimary(!current)`

---

## Change 8: Hebrew Calendar Swipe Navigation

**File:** `CalendarScreen.kt`

- Added `HorizontalPager` to `HebrewCalendarContent`, matching the Gregorian implementation
- Swiping left/right navigates Hebrew months (RTL layout means swipe direction is natural for Hebrew)
- Removed prev/next arrow buttons from `HebrewMonthHeader`; swiping replaces them
- Added `HebrewYearMonth.plusMonths(n)` and `HebrewYearMonth.stepsTo(other)` private helpers for pager ↔ month offset mapping
- "Go to today" animates the pager to the correct page

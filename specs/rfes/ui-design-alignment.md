# UI Design Alignment — Quick Wins

**Branch:** `ui-design-alignment`
**Date:** 2026-03-12

## Context

Comparing the running app against the Stitch "Calendar with Jewish Times v1" and "Sidebar Navigation V2" designs revealed several visual gaps. This spec covers the quick-win items. Inline zmanim integration is deferred.

---

## Gap 1: Day Cell Selected Indicator Shape

**File:** `app/src/main/java/com/levana/app/ui/calendar/CalendarScreen.kt` (~line 516)

**Change:**
- `aspectRatio(0.85f)` → `aspectRatio(1f)` (square cells so circle looks correct)
- `RoundedCornerShape(8.dp)` → `CircleShape`

---

## Gap 2: Navigation Drawer Header

**File:** `app/src/main/java/com/levana/app/MainActivity.kt` (~line 179)

**Change:** Replace single `Text("Levana")` with a `Row` containing:
- `CalendarMonth` icon (primary color)
- `Column` with "Levana" (bold headline, primary color) + "Calendar" subtitle (labelMedium, onSurfaceVariant)

---

## Gap 3: Settings Separation in Drawer

**File:** `app/src/main/java/com/levana/app/MainActivity.kt`

**Change:**
- Remove `DrawerNavItem("Settings", ...)` from `drawerNavItems` list
- Add standalone Settings `NavigationDrawerItem` in bottom section (after divider, before Locations)

---

## Gap 4: LOCATIONS Section in Drawer

**File:** `app/src/main/java/com/levana/app/MainActivity.kt`

**Change:**
- Add `Text("Locations", style = labelSmall, color = onSurfaceVariant)` section header before location item
- Set location `NavigationDrawerItem` with `selected = true` to show active highlight

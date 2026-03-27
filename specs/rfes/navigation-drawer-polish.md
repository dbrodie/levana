# Navigation Drawer Polish

## Summary

Refine the navigation drawer header and section label styling introduced in the Stitch UI Redesign, removing visual clutter and tightening spacing to better match Material 3 drawer conventions.

## Motivation

The initial drawer implementation used a two-line app header ("Levana" + "Calendar" subtitle) with dividers separating the header and settings footer. In practice the subtitle added no value, the dividers felt heavy, and the section label ("LOCATIONS" in all-caps with letter spacing) diverged from Material 3 style guidelines.

## Changes

### `app/src/main/java/com/levana/app/MainActivity.kt`

**Header:**
- Replace two-line `Column` (headline + "Calendar" subtitle) with a single `titleLarge` `Text`.
- Shrink logo circle: 40 dp → 36 dp; foreground icon: 28 dp → 24 dp.
- Adjust top spacer: 24 dp → 20 dp.
- Move horizontal padding from the outer `Column` to the header `Row` (`horizontal = 28.dp`) so nav items remain flush with the drawer edge per M3 spec.

**Dividers:**
- Remove `HorizontalDivider` after the header.
- Remove `HorizontalDivider` above the Settings item.

**Section label:**
- Change text from `"LOCATIONS"` to `"Locations"`.
- Change style from `labelSmall` with `letterSpacing = 1.5.sp` to plain `labelMedium`.
- Increase padding to `start = 28.dp, top = 16.dp, bottom = 18.dp` to provide visual breathing room above the location items.

## Verification

1. Build and run the app.
2. Open the navigation drawer — header should show only "Levana" in `titleLarge`, no subtitle, no dividers.
3. Scroll to the Locations section — label should read "Locations" in `labelMedium`.
4. Verify all drawer items still navigate correctly.
5. Check in both light and dark mode.

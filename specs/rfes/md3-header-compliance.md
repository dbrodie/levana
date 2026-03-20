# MD3 Header Compliance

## Problem

The Levana calendar screen uses custom `Row`-based headers (`GregorianMonthHeader` / `HebrewMonthHeader`) instead of Material 3 `TopAppBar` components. The header has no surface background, icons have no visual container, and the bar blends into the calendar content below it.

**Violations identified:**
1. **No `TopAppBar` component** — plain `Row` with no surface background, no elevation, no MD3 color tokens
2. **Wrong typography** — `titleMedium + FontWeight.Bold` instead of `titleLarge`
3. **No scroll elevation tinting** — MD3 TopAppBar should tint on scroll
4. **Missing `windowInsets = WindowInsets(0)`** — outer navigation layout already handles status bar insets; without this, `TopAppBar` would double-pad

**UI dump measurement (before):** Header at `[0,75][1080,201]` (~42dp height vs MD3's 56dp standard for small TopAppBar).

## Approach

Replace both `GregorianMonthHeader` and `HebrewMonthHeader` `Row` composables with `TopAppBar` from Material 3.

- Keep all 3 action icons visible (Today, GoToDate, Add) — Add is a frequent action
- Set `windowInsets = WindowInsets(0)` because the outer navigation scaffold already handles status bar insets
- Use `TopAppBarDefaults.topAppBarColors()` for correct surface background at rest
- Typography: `titleLarge` (no manual `FontWeight.Bold`)
- Scroll elevation tinting (`scrollBehavior`) is left unset for this feature; can be added later

## Changes

**File:** `app/src/main/java/com/levana/app/ui/calendar/CalendarScreen.kt`

- Add imports: `ExperimentalMaterial3Api`, `TopAppBar`, `TopAppBarDefaults`, `WindowInsets`
- Replace `GregorianMonthHeader`: `Row` → `TopAppBar`
- Replace `HebrewMonthHeader`: `Row` → `TopAppBar`
- Call sites unchanged (same signatures)

## Verification

1. `./gradlew :app:installDebug` — build must succeed
2. Header height ~168px (56dp × 3x) in UI dump — MD3 small TopAppBar standard
3. Header has surface background node
4. 3 action icons visible: Today, GoToDate, Add
5. Tapping title area toggles calendar mode (Gregorian ↔ Hebrew)
6. Add event button launches event creation flow
7. Hamburger opens drawer

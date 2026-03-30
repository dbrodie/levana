# Fix: Date Override Applied Consistently

## Problem

The developer date override (`devDateOverride` in `UserPreferences`) is only partially respected. Several screens and system components ignore it and fall back to `LocalDate.now()`, making it impossible to test the app consistently at a simulated date.

## Affected Components

| Component | Bug |
|-----------|-----|
| `CalendarViewModel` | `LoadToday` and `GoToToday` use `HebrewYearMonth.now()` / `YearMonth.now()` (real date) instead of the override |
| `CalendarViewModel` | `NextHebrewMonth` / `PreviousHebrewMonth` fallbacks also use `HebrewYearMonth.now()` |
| `ZmanimScreen` | Falls back to `LocalDate.now()` when no date is passed from navigation |
| `HebrewDateTileService` | Both `onStartListening` and `onClick` use `LocalDate.now()` directly |

## Already Correct

- `CalendarViewModel.loadMonth` / `loadHebrewMonth` — sets `state.today` from override ✓
- `DailyNotificationWorker` — uses override ✓
- `MainActivity` holiday theme — uses override ✓

## Solution

### 1. `HebrewYearMonth.from(LocalDate)` factory

Add a companion factory so callers can convert a `LocalDate` (which may be the override) to a `HebrewYearMonth` without duplicating the `GregorianCalendar` → `JewishDate` conversion.

### 2. `CalendarViewModel.today()` helper

Add `private fun today(): LocalDate = currentPrefs.devDateOverride ?: LocalDate.now()` and replace all `HebrewYearMonth.now()` / `YearMonth.now()` calls in intent handling with `HebrewYearMonth.from(today())` / `YearMonth.from(today())`.

### 3. `ZmanimIntent.LoadDefault`

Add a `LoadDefault` variant. The ViewModel reads `devDateOverride` from preferences when handling it. `ZmanimScreen` uses `LoadDefault` when `initialDate` is null.

### 4. `HebrewDateTileService` — Koin + coroutine

Implement `KoinComponent`, inject `PreferencesRepository`, add a `CoroutineScope(Dispatchers.Main + SupervisorJob())` tied to `onDestroy`. Move `onStartListening` and `onClick` logic inside a coroutine that reads `devDateOverride` first.

## Files Changed

- `app/src/main/java/com/levana/app/domain/model/HebrewYearMonth.kt`
- `app/src/main/java/com/levana/app/ui/calendar/CalendarViewModel.kt`
- `app/src/main/java/com/levana/app/ui/zmanim/ZmanimIntent.kt`
- `app/src/main/java/com/levana/app/ui/zmanim/ZmanimViewModel.kt`
- `app/src/main/java/com/levana/app/ui/zmanim/ZmanimScreen.kt`
- `app/src/main/java/com/levana/app/quicktile/HebrewDateTileService.kt`

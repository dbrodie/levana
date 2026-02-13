# Increment 08: Settings Screen

## Overview

Replace the placeholder settings tab with a full settings screen. All settings persist in DataStore and propagate reactively to downstream screens via Flow.

## What to Build

### Domain Model Changes
- `Minhag` enum: ASHKENAZI, SEPHARDI, YEMENITE, CHABAD
- Extend `UserPreferences` with: minhag, isInIsrael, showModernIsraeliHolidays, hebrewPrimary, candleLightingOffset (already exists)

### PreferencesRepository Changes
- New DataStore keys for each setting
- Save/read functions for each preference
- Auto-detect Israel from location timezone (`Asia/Jerusalem`)

### CalendarRepository Changes
- Accept `inIsrael` parameter and call `JewishCalendar.setInIsrael()` so holiday/parsha calculations respect Israel vs Diaspora

### HolidayMapper Changes
- Filter out `MODERN_ISRAELI` holidays when `showModernIsraeliHolidays` is false

### Settings Screen (MVI)
- `SettingsState`, `SettingsIntent`, `SettingsViewModel`
- Sections: Location, Minhag, Israel/Diaspora, Modern Israeli Holidays, Hebrew-primary, Candle Lighting Offset
- All changes immediate (no save button)

### Navigation
- Replace `SettingsPlaceholderRoute` with `SettingsRoute`
- Location tap navigates to CityPickerScreen

## Key Technical Notes
- `JewishCalendar.setInIsrael(true)` affects Yom Tov second day and parsha alignment
- Israel auto-detected from timezone but manually overridable
- Minhag stored but only affects display in Inc 9+ (parsha transliteration, etc.)
- Hebrew-primary toggle stored but calendar mode switch implemented in Inc 9

## Acceptance Criteria
- [ ] Settings screen accessible via bottom navigation
- [ ] Minhag selection persists across restarts
- [ ] Israel/Diaspora toggle changes Yom Tov day count
- [ ] Modern Israeli Holidays toggle shows/hides them on calendar
- [ ] Candle lighting offset change updates times immediately
- [ ] All settings persist across app restarts
- [ ] `./gradlew build` passes

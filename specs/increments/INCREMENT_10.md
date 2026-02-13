# Increment 10: Holiday Enrichment & Torah Readings

## Overview

Add color-coded holiday indicators on calendar cells, enriched Torah reading info on day detail, Molad on Shabbat Mevarchim, and special Shabbatot identification.

## What to Build

### HebrewDay Changes
- Add `holidayCategory: HolidayCategory?` for calendar cell color coding

### DayInfo Changes
- Add `specialShabbat: String?` for special Shabbatot names
- Add `molad: String?` for Molad display on Shabbat Mevarchim
- Add `parshaHebrew: String?` for Hebrew parsha name alongside transliterated

### CalendarRepository Changes
- Populate `holidayCategory` in `toHebrewDay()` from yomTovIndex via HolidayMapper
- In `getDayInfo()`:
  - Get special Shabbat via `JewishCalendar.getSpecialShabbos()` — map Parsha enum to display name
  - Detect Shabbat Mevarchim via `JewishCalendar.isShabbosMevorchim()`
  - Get Molad via `JewishCalendar.getMoladAsDate()` and format
  - Get Hebrew parsha via HebrewDateFormatter

### CalendarScreen Changes
- DayCell shows small colored dot based on `holidayCategory`
- Color mapping: Torah=red, Rabbinic=blue, Fast=gray, Minor=green, Modern=orange

### DayDetailScreen Changes
- Show Hebrew parsha name alongside transliterated
- Show special Shabbat name when applicable
- Show Molad info on Shabbat Mevarchim

## Key Technical Notes
- `getSpecialShabbos()` returns `Parsha` enum — includes Shekalim, Zachor, Para, Hachodesh, Hagadol, Chazon, Nachamu, Shuva, Shira
- `isShabbosMevorchim()` returns boolean — Shabbat before Rosh Chodesh (except before Tishrei)
- `getMoladAsDate()` returns java.util.Date in Jerusalem time
- HebrewDateFormatter has `formatParsha()` for Hebrew parsha names when isHebrewFormat=true

## Acceptance Criteria
- [ ] Calendar cells show color-coded dots for holidays
- [ ] Different holiday categories have distinct colors
- [ ] Shabbat detail shows parsha in both Hebrew and transliterated
- [ ] Special Shabbatot identified correctly
- [ ] Shabbat Mevarchim shows Molad info
- [ ] `./gradlew build` passes

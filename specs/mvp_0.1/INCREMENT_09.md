# Increment 09: Hebrew-Primary Calendar & RTL

## Overview

Add Hebrew-primary calendar mode where the grid is organized by Hebrew months (29/30 days). When toggled on in settings, the calendar shows Hebrew month navigation instead of Gregorian months. Also add RTL layout support and string extraction.

## What to Build

### CalendarRepository Changes
- `getHebrewMonthDays(year, month, inIsrael)` — returns all HebrewDay objects for a given Hebrew month
- Uses `JewishDate.getDaysInJewishMonth(month, year)` for count
- Constructs `JewishCalendar(year, month, day)` directly for each day
- Gets Gregorian date via `getGregorianCalendar()` for each day

### Domain Model
- `HebrewYearMonth` data class — holds (year: Int, month: HebrewMonth, jewishDateMonth: Int)
- Navigation: next/prev month accounting for leap years (Adar I→Adar II, skip Adar II in non-leap)

### CalendarState Changes
- Add `hebrewPrimary: Boolean`
- Add `hebrewYearMonth: HebrewYearMonth?` for Hebrew mode navigation
- Add `gregorianHeader: String` for secondary header in Hebrew mode

### CalendarIntent Changes
- Add `LoadHebrewMonth(HebrewYearMonth)`, `NextHebrewMonth`, `PreviousHebrewMonth`

### CalendarScreen Changes
- When `hebrewPrimary=true`, use Hebrew month grid:
  - Header shows Hebrew month name + year (with Gregorian range secondary)
  - Grid shows days of the Hebrew month
  - Leading empty cells based on first day's day-of-week
  - Pager navigates through Hebrew months

### Settings Update
- Remove "(coming soon)" from Hebrew-primary description

### RTL Support
- When `hebrewPrimary` is on, force RTL layout direction via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`
- Day-of-week header starts from Shabbat (Saturday) in RTL

### String Resources
- Extract key user-facing strings to `values/strings.xml`
- Hebrew translations in `values-iw/strings.xml`

## Key Technical Notes
- `JewishDate.getDaysInJewishMonth(month, year)` is static — use jewishDateValue from HebrewMonth enum
- Hebrew months: Tishrei(7) through Elul(6), with Adar=12, Adar II=13
- Leap year has 13 months (Adar I + Adar II); regular has 12 (just Adar)
- JewishCalendar(year, month, day) constructor takes the JewishDate month constants

## Acceptance Criteria
- [ ] Toggling Hebrew-primary in settings switches to Hebrew month grid
- [ ] Hebrew month grid shows correct number of days (29 or 30)
- [ ] Leap year shows Adar I and Adar II as separate months
- [ ] Gregorian dates appear as secondary in Hebrew-primary cells
- [ ] RTL layout when Hebrew-primary is enabled
- [ ] String resources extracted with Hebrew translations
- [ ] `./gradlew build` passes

# Increment 03: Monthly Calendar Grid

## Summary

Replace the single-date display with a full monthly calendar grid. Add month navigation via arrows and horizontal swipe, today highlighting, and a dual month/year header.

## What Will Be Built

### CalendarRepository Extension
- `getMonthDays(yearMonth: YearMonth): List<HebrewDay>` — returns Hebrew day data for every day in a Gregorian month
- `getHebrewDayLetter(day: Int): String` — returns Hebrew letter representation of a day number using HebrewDateFormatter

### Domain Model Update
- `HebrewDay` gains `hebrewDayOfMonthFormatted: String` — the day number as Hebrew letters (א, ב, ג, ...)

### CalendarState Expansion
- `currentMonth: YearMonth` — which Gregorian month is displayed
- `monthDays: List<HebrewDay>` — all days in that month
- `today: LocalDate` — for highlight comparison
- Remove single `hebrewDay` field (replaced by `monthDays`)

### CalendarIntent Expansion
- `LoadMonth(yearMonth: YearMonth)` — load a specific month
- `NextMonth` / `PreviousMonth` — relative navigation
- `GoToToday` — jump back to current month

### Calendar Grid UI
- 7-column `LazyVerticalGrid` with day-of-week header row (Sun–Sat)
- Each cell: Hebrew day in Hebrew letters (primary), Gregorian day number (secondary)
- Today cell highlighted with `MaterialTheme.colorScheme.primaryContainer`
- Empty cells for leading days before month start
- `HorizontalPager` wrapping the grid for swipe navigation
- Arrow buttons in the month header for forward/backward

### Month Header
- Format: "Shevat–Adar 5786 / February 2026"
- Shows Hebrew month range since a Gregorian month spans 2 Hebrew months
- Left/right arrows for navigation

## Acceptance Criteria

- [ ] Monthly grid displays with 7 columns and correct day layout
- [ ] Hebrew day numbers shown in Hebrew letters (א, ב, ג, ..., ל)
- [ ] Gregorian day numbers shown alongside
- [ ] Today's cell is visually highlighted
- [ ] Arrow buttons navigate forward/backward by month
- [ ] Swipe gesture navigates between months
- [ ] Month header shows Hebrew and Gregorian month info
- [ ] `./gradlew build` passes
- [ ] `./gradlew ktlintCheck` passes

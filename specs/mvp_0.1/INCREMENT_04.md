# Increment 04: Daily Detail View

## Summary

Add a day detail screen accessible by tapping a calendar cell. Display full Hebrew/Gregorian date, day of week, holidays with categories, parsha on Shabbat, and Omer count. Introduce Navigation Compose for screen routing.

## What Will Be Built

### Navigation
- Add `navigation-compose` dependency
- Type-safe route definitions: `CalendarRoute`, `DayDetailRoute(dateEpochDay: Long)`
- `NavHost` in `LevanaApp` composable, replacing direct `CalendarScreen` call

### Domain Models
- `HolidayCategory` enum: TORAH, RABBINIC, FAST, MINOR, MODERN_ISRAELI
- `Holiday` data class: name, hebrewName, category
- `DayInfo` data class: hebrewDay, dayOfWeek, holidays, parsha, omerDay

### CalendarRepository Extensions
- `getDayInfo(date: LocalDate): DayInfo` — full day information
- Holiday mapping from `JewishCalendar.getYomTovIndex()` to domain `Holiday`
- Parsha from `JewishCalendar.getParsha()`
- Omer count from `JewishCalendar.getDayOfOmer()`

### MVI Triad — Day Detail
- `DayDetailState` — dayInfo, formatted strings
- `DayDetailIntent` — `LoadDay(date: LocalDate)`
- `DayDetailViewModel` — processes intent, emits state

### UI
- Calendar cells become tappable → navigate to detail
- Day detail screen: date header, holiday list, parsha card, Omer display
- Back button returns to calendar

## Acceptance Criteria

- [ ] Tapping a day in the grid opens the detail screen
- [ ] Detail shows full Hebrew date, Gregorian date, and day of week
- [ ] A regular weekday shows no holiday or parsha
- [ ] A Shabbat shows the parsha name
- [ ] A holiday shows the holiday name and category
- [ ] During Omer period, count displays as "Day X, which is Y weeks and Z days"
- [ ] Back navigation returns to calendar grid
- [ ] `./gradlew build` passes
- [ ] `./gradlew ktlintCheck` passes

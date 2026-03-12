# Increment 02: KosherJava Integration & Hebrew Date Display

## Summary

Integrate the KosherJava zmanim library, create domain models for Hebrew dates and locations, build the data layer (`CalendarRepository`), and implement the first MVI triad to display today's Hebrew date on screen.

## What Will Be Built

### KosherJava Dependency
- Add `com.kosherjava:zmanim:2.5.0` to version catalog and app dependencies

### Domain Models
- `HebrewMonth` — enum of all Hebrew months (Tishrei through Elul, plus Adar I/II)
- `HebrewDay` — data class with day, month (HebrewMonth), year, and formatted strings
- `Location` — data class with latitude, longitude, timezone ID, and display name

### Data Layer
- `CalendarRepository` — wraps KosherJava's `JewishCalendar` and `HebrewDateFormatter`
  - `getHebrewDay(date: LocalDate, location: Location): HebrewDay`
  - Converts `JewishCalendar` state to domain `HebrewDay`
  - Uses `HebrewDateFormatter` for Hebrew letter and transliterated output
  - All `java.util.Date` conversions to `java.time` happen here

### MVI Triad — Calendar Screen
- `CalendarState` — data class holding `hebrewDay`, `gregorianDate`, `isLoading`
- `CalendarIntent` — sealed interface (just `LoadToday` for now)
- `CalendarViewModel` — processes intents, emits state via `StateFlow`

### UI Updates
- Replace hello world content with Hebrew date display
- Show three date formats: Hebrew letters, transliteration, Gregorian
- Hardcoded Jerusalem location (31.7683°N, 35.2137°E, Asia/Jerusalem)

### Koin Modules
- `dataModule` — provides `CalendarRepository`
- `domainModule` — placeholder for future use cases
- `viewModelModule` — provides `CalendarViewModel`

## Package Structure (new/modified files)
```
com.levana.app/
├── data/
│   └── CalendarRepository.kt
├── domain/
│   └── model/
│       ├── HebrewDay.kt
│       ├── HebrewMonth.kt
│       └── Location.kt
├── ui/
│   └── calendar/
│       ├── CalendarIntent.kt
│       ├── CalendarState.kt
│       ├── CalendarViewModel.kt
│       └── CalendarScreen.kt
└── di/
    └── AppModules.kt
```

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| KosherJava version | 2.5.0 | Latest stable release |
| Date conversion boundary | Repository | Clean architecture — domain layer uses java.time only |
| Hardcoded location | Jerusalem | Simplifies this increment; location management in Inc. 5 |
| Hebrew formatting | HebrewDateFormatter | Never hardcode Hebrew text per architecture decisions |
| ViewModel base | Koin ViewModel | `koinViewModel()` in Compose, standard lifecycle |

## Acceptance Criteria

- [ ] App shows today's Hebrew date in Hebrew letters (e.g., ט״ו בשבט תשפ״ו)
- [ ] App shows transliterated Hebrew date (e.g., 15 Shevat 5786)
- [ ] App shows corresponding Gregorian date
- [ ] Date is correct (verify against hebcal.com or similar)
- [ ] `./gradlew build` passes
- [ ] `./gradlew ktlintCheck` passes

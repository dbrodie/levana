# Fix: crash on launch after adding a Hebrew birthday (year 0)

**Status:** In progress

## Problem

After adding a Hebrew birthday to a contact (My events -> Birthdays -> + ->
pick contact -> 10 Kislev 5784 -> Save), every app launch crashes on the
calendar screen:

```
IllegalArgumentException: A Jewish date earlier than 18 Teves, 3761 ... 1, 7, 1 is invalid.
  at HebrewDateMatcher.matchesDate(HebrewDateMatcher.kt:79)
  at ContactBirthdayRepository.getBirthdayDatesForGregorianMonth
  at CalendarViewModel.loadMonthDaysOnly
```

The contacts data row is written with `data3=0` (year 0) although the editor
showed 5784.

## Root cause

1. **Save:** `ContactBirthdayState.hebrewYear` defaults to `0` ("unknown").
   `BirthdayDatePicker` displays `5784` when the state year is 0, but that
   substitution is display-only. Unless the user scrolls the year picker, the
   state stays 0 and `setBirthday(..., year = 0)` is persisted.
2. **Read:** `ContactBirthdayRepository` passes `hebrewYear.coerceAtLeast(1)`
   to `HebrewDateMatcher.matchesDate`, which builds
   `JewishDate(year, TISHREI, 1)` to learn whether the event year was a leap
   year. KosherJava rejects any date before 18 Teves 3761, so year 1 (and any
   other out-of-range year coming from old data, imports or other apps)
   throws on the calendar load path. `PersonalEventRepository` has the same
   unguarded construction for personal events.

## Fix

- **Editor:** the year shown in the editor is the year in state. New
  birthdays start at the year the picker already displayed (5784); loading a
  stored birthday with an unknown/out-of-range year also falls back to it, so
  what the user sees is what is saved. The display-only substitution in
  `BirthdayDatePicker` is removed.
- **Reading:** add `HebrewDateMatcher.isEventLeapYear(year)`, which never
  throws. A year below the KosherJava minimum (`<= 3761`, including 0 /
  missing) is treated as **unknown = non-leap**. For simple (birthday) rules
  that means a stored Adar maps to Adar in non-leap years and Adar (I) in leap
  years, and a stored Adar II maps to Adar / Adar II; for yahrzeit rules an
  unknown-year Adar is observed in Adar II of a leap year (the non-leap-year
  default). `matchesDate` and all repository call sites
  (`ContactBirthdayRepository`, `PersonalEventRepository`) use it; the
  `coerceAtLeast(1)` calls are removed.

## Test plan

- Unit tests for `HebrewDateMatcher`: year 0 / negative / 1 / 3761 do not
  throw and are non-leap; valid leap (5784) and non-leap (5785) years;
  `matchesDate` for 10 Kislev with year 0 and 5784; Adar / Adar II birthday
  and yahrzeit cases with unknown year and with known leap/non-leap years.
- Unit test for `ContactBirthdayViewModel`: a new birthday's initial year is a
  real year (not 0), and `SetHebrewYear` updates it.
- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
- Manual (emulator): add birthday 10 Kislev 5784, relaunch, no crash, birthday
  indicator on the right day; contacts row has `data3=5784`.

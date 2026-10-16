# Fix: Hebrew month spelling in the event/birthday date picker

## Problem

The month stepper in the Add/Edit event editor and the birthday editor (shared
`HebrewDatePicker` in `ui/events/AddEditEventScreen.kt`) uses its own hardcoded
list of Hebrew month names. It spells Cheshvan "חשוון" and Sivan "סיוון", while
every other part of the app (calendar subtitle, day cells, My events rows, date
previews, occurrence labels, Go to date) gets month names from KosherJava's
`HebrewDateFormatter`, which spells them "חשון" and "סיון".

## Fix

- Add a single shared source for Hebrew month names on the `HebrewMonth` domain
  model (`hebrewName` / `HebrewMonth.hebrewName(month, isLeapYear)`), produced by
  KosherJava's Hebrew formatter so it always matches the rest of the app,
  including the leap-year Adar names (Adar → "אדר א׳" in a leap year).
- The editors' month list takes its Hebrew names from that source instead of the
  hardcoded list. English names are unchanged (already from `HebrewMonth`).
- `OccurrenceLabels` uses the same shared helper instead of its own copy of the
  formatter logic.

## Test plan

- Unit test: for every `HebrewMonth` entry (all 14, ordinary and leap year), the
  shared Hebrew name equals KosherJava's `formatMonth` output, and the editor's
  month list (ordinary and leap year) uses exactly those names — in particular
  "חשון" and "סיון", never "חשוון" / "סיוון".
- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
- Emulator: app in Hebrew, open Add event, step through the months and confirm
  "חשון" and "סיון" appear in the stepper.

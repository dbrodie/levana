# Fix: Adar in leap years, bad years, English Adar names, stale default years

Status: In progress

Four small related follow-ups to the birthday/event work. Month numbers are
KosherJava's: Nisan = 1 … Adar = 12, Adar II = 13. In a leap year month 12 is
Adar I and month 13 is Adar II; in a regular year only month 12 (Adar) exists.

## 1. Adar in leap years

### Problem

`HebrewDateMatcher.resolveSimpleMonth` (birthdays and custom events without
the yahrzeit rule) puts a stored Adar into Adar I when the target year is a
leap year. The owner decided it belongs in Adar II.

### Decided behaviour

Every birthday and every custom event whose stored date is in Adar falls in
**Adar II** when the target year is a leap year. The only exception is an
event with "Use yahrzeit Adar rules" on, which keeps today's yahrzeit
behaviour. This is not minhag-dependent; there is no setting.

Interpretation implemented (owner: correct this if it is wrong):

| Stored month | Stored year | Regular target year | Leap target year |
|---|---|---|---|
| 12 (Adar) | regular year | Adar | **Adar II** (was Adar I) |
| 12 (Adar) | unknown (fails `isKnownYear`) | Adar | **Adar II** (was Adar I) |
| 12 (Adar I) | known leap year — the user explicitly recorded Adar I | Adar | Adar I (unchanged) |
| 13 (Adar II) | any | Adar | Adar II (unchanged) |

With this interpretation the simple rule and the yahrzeit rule give the same
month in every case. Both stay as separate entry points so that a later
change to one does not silently change the other, and the yahrzeit code is
not touched.

### Consistency

Every place that maps a stored Hebrew date onto a target year must go through
one function, `HebrewDateMatcher.resolveMonth(month, year, useYahrzeitRules,
targetIsLeapYear)`: calendar day markers (Gregorian and Hebrew mode), the day
panel, notifications, contact birthdays and personal events. The audit result
is recorded in the hand-back report.

## 2. Bad years

### 2a. Creating or storing a bad year

A "bad" year is one the calendar library cannot handle: below
`MIN_KNOWN_YEAR` (3762), or above `MAX_KNOWN_YEAR` (9999, the largest number
KosherJava can format as a Hebrew numeral). 0 remains the stored value for
"year unknown" and is not an error.

Audit every path that accepts a year (birthday editor, Add/Edit event, event
import from JSON/CSV, Go-to-date, Developer Settings → Date Override) and
guard the ones that can produce a bad year:

- Editors: the year held in editor state is always clamped to the stepper
  range, and Adar II is turned into Adar when the year is stepped to a
  regular year (otherwise month 13 would be saved with a year that has no
  month 13).
- Import: a file containing an event whose year is neither 0 nor a known
  year, whose month is not 1–13, or whose day is not 1–30 is rejected as a
  whole with the existing "Could not parse file: …" error, naming the event.
  Nothing is imported from such a file.
- Date Override: a Hebrew year outside the range the date pickers accept is
  ignored like other invalid input.

### 2b. Display

My events → Birthdays shows raw numbers ("10/9/1") for a stored year of 1 or
-1, while year 0 shows as a year-less Hebrew date (י׳ כסלו). Any year that
fails `HebrewDateMatcher.isKnownYear` is treated as unknown everywhere a
stored date is displayed (birthday rows, event rows, editor previews), and
opening such an item in an editor shows the current Hebrew year.

The year-less format no longer formats through a hardcoded reference year
(5784, a leap year, which made a year-less Adar read "אדר א׳" and broke on
day 30 of months that were short that year).

## 3. "Adar i–Adar ii" in English

The English calendar subtitle reads "Adar i–Adar ii 5787" because the name is
derived from the enum constant name. English Hebrew-month names come from one
shared source, `HebrewMonth.englishName` ("Adar", "Adar I", "Adar II"), used
by the calendar subtitle, the Go-to-date month dropdown and the
birthday/event editors. A test pins the shared names to KosherJava's
transliterated formatter, which still produces the full English date strings
(quick tile). Hebrew-language output does not change.

## 4. Stale default years

`DEFAULT_BIRTHDAY_YEAR = 5784` and `hebrewYear = 5786` are hardcoded. Both
editors default a new item to the current Hebrew year, computed when the
editor opens and respecting the Developer Settings date override. Opening a
stored item with an unknown year also shows the current Hebrew year, and its
month is shown where the item falls this year (so an unknown-year Adar opened
in a leap year shows Adar II and saving it does not move it to Adar I).

Not changed: the year is still entered with the steppers, and there is no
"year unknown" option.

## Test plan

Unit tests (`./gradlew :app:testDebugUnitTest`):

- `HebrewDateMatcherTest`: full matrix — stored month 12/13 × stored year
  regular / leap / unknown × target regular / leap × yahrzeit rule on/off;
  non-Adar months unaffected; year bounds; current Hebrew year.
- `EventSerializerTest`: JSON and CSV import reject bad years, months and
  days; year 0 and boundary years are accepted.
- `HebrewMonthTest`: English names, leap-year Adar names, parity with
  KosherJava.
- `CalendarHeaderFormatterTest`: "Adar I–Adar II 5787" for March 2027;
  Hebrew output unchanged.
- `HebrewDateFormatTest`: years 1, -1, 0 give the same year-less date; no raw
  numbers; year-less Adar names.
- View model tests: new birthday/event defaults to the current Hebrew year
  (with and without date override); unknown stored year opens as the current
  year; year clamping; Adar II → Adar when the year becomes regular.
- Date override parsing rejects out-of-range years.

Build: `./gradlew :app:assembleDebug`.

Emulator (if the shared emulator is free): March 2027 subtitle in English;
an Adar birthday marker in Adar II 5787; My events row for a year-1 birthday.

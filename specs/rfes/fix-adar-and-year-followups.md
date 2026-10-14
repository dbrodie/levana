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

The yahrzeit rule is specified separately; see "Yahrzeit Adar rules" below.

### Consistency

Every place that maps a stored Hebrew date onto a target year must go through
one function, `HebrewDateMatcher.resolveMonth(month, year, useYahrzeitRules,
targetIsLeapYear)`: calendar day markers (Gregorian and Hebrew mode), the day
panel, notifications, contact birthdays and personal events. The audit result
is recorded in the hand-back report.

## 1b. Yahrzeit Adar rules

Owner-specified rules for events with "Use yahrzeit Adar rules" on. They
replace the previous yahrzeit behaviour. Birthdays and custom events without
the yahrzeit rule keep the rule in section 1 and are unaffected by every
setting below. "Death year" is the stored event year; "target year" is the
Hebrew year being displayed.

### Section 1: death in a leap year (no setting involved except the 1 Nisan option)

| Date of death | Target year ordinary | Target year leap |
|---|---|---|
| 1–29 Adar I | same day in Adar | same day in Adar I |
| 1–29 Adar II | same day in Adar | same day in Adar II |
| 30 Adar I | 30 Shevat | 30 Adar I |

Setting (off by default): "For a yahrzeit on 30 Adar I, also mark 1 Nisan in
ordinary years". When on, an ordinary target year has both 30 Shevat and
1 Nisan marked.

### Section 2: death in Adar of an ordinary year

- Target ordinary → same day in Adar.
- Target leap → annual setting with three values: Adar I (same day in
  Adar I) / Adar II (same day in Adar II) / Both Adars (same day in both).

### Section 3: first-year exception

Applies only when the death was in an ordinary year's Adar and the
immediately following Hebrew year (death year + 1) is a leap year, and only
to that first target year. Setting with three values:

- Follow annual minhag → section 2.
- First observance in Adar I → same day in Adar I in that first year only;
  later leap years use section 2.
- Mark both first-year dates → same day in both Adar I and Adar II in that
  first year; later leap years use section 2.

### Defaults and interpretations

- Defaults: annual setting = Adar II (today's behaviour); first-year setting
  = Follow annual minhag; 1 Nisan option = off. With the defaults the
  observed dates are the same as before this change, except 30 Adar I of a
  leap-year death in an ordinary year: it used to map to a non-existent
  30 Adar (never marked) and is now 30 Shevat, as section 1 requires.
- The three settings are global (not per event), stored in DataStore with
  the other preferences, and shown in Settings in the Minhag group under the
  Minhag row. They are independent of the Minhag (Ashkenazi / Sephardi / …)
  setting; no default is derived from it. All labels and option names are
  string resources with English and Hebrew translations.
- Unknown death year (fails `isKnownYear`): a stored month 12 is treated as
  an ordinary year's Adar (section 2; the first-year exception cannot apply
  because there is no death year to add 1 to). A stored month 13 is treated
  as Adar II of a leap year (section 1, row 2).
- A stored month 13 with a known ordinary death year (inconsistent data the
  editors can no longer create) is also treated as section 1, row 2.
- Only 30 Adar I gets special handling. Other day-30 dates (30 Cheshvan,
  30 Kislev) behave as before: they are marked only in years where that
  month has 30 days.
- The rules are applied to every target year, including years before the
  death year, as the matcher always has.

### Occurrences, labels, display and notifications

- One resolver, `HebrewDateMatcher.occurrences(day, month, year,
  useYahrzeitRules, targetYear, rules)`, returns the list of occurrences
  (month, day, optional label) of a stored date in a target Hebrew year.
  `matchesDate`, the Hebrew-month day lookups and the Gregorian-month date
  lookups of both repositories, the day panel and notifications all use it.
- When a date has two occurrences in a year (both Adars, or 30 Shevat +
  1 Nisan), each occurrence is marked separately on the calendar (Gregorian
  and Hebrew modes) and listed separately in the day panel, and each gets a
  label so a user looking at either day understands why it is there: the
  event title plus "(Adar I)" / "(Adar II)" or "(30 Shevat)" / "(1 Nissan)",
  localized ("(אדר א׳)", "(אדר ב׳)", "(ל׳ שבט)", "(א׳ ניסן)"). A single
  occurrence is not labelled.
- A notification is posted for each occurrence, with the same label.
- Changing a setting refreshes the calendar markers.

### Editors

Opening a yahrzeit event whose death year is unknown cannot show "the current
year" for an Adar date without changing which rules apply once saved. The
editor instead shows the most recent year, up to the current one, for which
the stored date behaves identically: for month 12 an ordinary year that is
followed by an ordinary year (so section 2 applies and the first-year
exception never does), for month 13 a leap year. Other months still open on
the current year.

### Tests

The matrix: sections 1–3 × ordinary/leap target × each setting value × first
year vs. later years, including 30 Adar I with the 1 Nisan option off/on,
unknown death year, and checks that non-yahrzeit events are unaffected by
every setting. Repository tests for both "both" cases across the three
lookups and the labels. Preference round trip for the three settings.

## 1c. Yahrzeits on 30 Cheshvan / 30 Kislev

Owner-specified rules for events with "Use yahrzeit Adar rules" on whose
stored date is day 30 of Cheshvan (8) or Kislev (9). Both months have 29 or
30 days depending on the year. Everything in 1b stays as is.

### Rules

1. If the date exists in the target year (that month has 30 days that year),
   observe it on the original date, 30 Cheshvan or 30 Kislev, whatever the
   setting.
2. If the target month has only 29 days, a new global setting, the
   missing-date minhag, decides:

| Setting | Death on 30 Cheshvan | Death on 30 Kislev |
|---|---|---|
| First-anniversary-based (common practice), **the default** | 29 Cheshvan if Cheshvan had 29 days in the first anniversary year; otherwise 1 Kislev | 29 Kislev if Kislev had 29 days in the first anniversary year; otherwise 1 Teves |
| Always follow Rosh Chodesh | 1 Kislev | 1 Teves |
| Observe both when missing | 29 Cheshvan and 1 Kislev | 29 Kislev and 1 Teves |

Owner's developer notes:

- First anniversary year = Hebrew year of death + 1.
- The length checked is that specific month's length in that year.
- The first-anniversary rule only matters in years where the original date is
  missing; if day 30 exists, it is always used.
- Each recurrence is calculated from the original death date, never from the
  previous recurrence.

### Interpretations (owner: correct these if wrong)

- The setting is global, stored in DataStore like the three Adar settings,
  and shown with them in Settings (Minhag group), with English and Hebrew
  strings and an explanation in its dialog.
- "Observe both" produces two separate occurrences, labelled like the Adar
  "Both" case: "(29 Cheshvan)" / "(1 Kislev)" and "(29 Kislev)" /
  "(1 Teves)", localized ("(כ״ט חשון)" / "(א׳ כסלו)", "(כ״ט כסלו)" /
  "(א׳ טבת)"). Each is marked separately on the calendar in both month
  modes, listed separately in the day panel, and gets its own notification.
  A single occurrence has no label.
- English uses "Teves", the app's shared English spelling (see section 3),
  for the month the owner wrote as "Tevet".
- Unknown death year (fails `isKnownYear`): the first-anniversary rule cannot
  be computed, so under that setting it falls back to Rosh Chodesh (1 Kislev /
  1 Teves). The other two settings work as usual.
- Applies to every target year, including years before the death year,
  consistent with the Adar rules. A year before the death year still uses
  death year + 1 as the first anniversary year.
- Birthdays and custom events with the yahrzeit switch off are not affected by
  this setting and keep their current day-30 behaviour: they are observed on
  day 30 only in years when that month has 30 days, and are not shown or
  notified at all in years when it has 29.
- Day 30 of other months is unchanged. The 30 Adar I rule from 1b is separate
  and unchanged.
- Default behaviour changes for these yahrzeits: before, a 30 Cheshvan /
  30 Kislev yahrzeit was not shown at all in a year with a 29-day month; with
  the default it is now shown on 29 or on Rosh Chodesh.
- All paths go through `HebrewDateMatcher.occurrences`.

### Tests

Each setting × target year with Cheshvan / Kislev of 29 vs 30 days × first
anniversary year with 29 vs 30 days, with concrete years found by asking
KosherJava for month lengths in the test. Also: unknown death year, a target
year before the death year, a recurrence that does not depend on the previous
year's result, non-yahrzeit events ignoring the setting, labels through the
repository lookups, and the preference round trip.

## 1d. Day-30 birthdays and events

Owner's decision for birthdays, and custom events with the yahrzeit switch
off, stored on day 30 of a month. There is no setting. The yahrzeit rules
(1b, 1c) are unaffected.

### Problem

Today such a date is observed only on day 30. In a year where that month has
29 days it is not shown anywhere (no marker, no day-panel entry, no
notification).

### Rule

After the existing Adar mapping (section 1), if the month the date falls in
has only 29 days in the target year, the date is observed on the 1st of the
following month instead. When day 30 exists it stays on day 30.

- 30 Cheshvan → 1 Kislev in years with a 29-day Cheshvan.
- 30 Kislev → 1 Teves in years with a 29-day Kislev.
- 30 Adar I (month 12 of a known leap year) → 1 Nisan in ordinary years, where
  Adar has 29 days. In leap years it stays on 30 Adar I.
- The same fallback applies to any stored day 30 the target month lacks, so
  no date is ever silently dropped. Months that always have 29 days (Iyar,
  Tammuz, Elul, Teves, Adar II, and Adar of an ordinary year) therefore
  always move: 30 Iyar → 1 Sivan, 30 Tammuz → 1 Av, 30 Teves → 1 Shevat,
  30 Adar (ordinary or unknown year) and 30 Adar II → 1 Nisan, and 30 Elul →
  1 Tishrei. The editors normally cap the day at the month's length, but
  such dates can come from an import, from contacts written by other apps,
  or from the editor keeping day 30 when the month is changed.

### Interpretations (owner: correct these if wrong)

- "The following month" is the next month in the target year's calendar:
  Adar (ordinary year) and Adar II → Nisan. 30 Elul → 1 Tishrei: Elul always
  has 29 days, so the date is observed on 1 Tishrei every year (in the
  calendar's terms, 1 Tishrei of each Hebrew year).
- The moved occurrence is labelled "(delayed from 30 Cheshvan)", in Hebrew
  "(נדחה מל׳ חשון)". The month named is the month as stored: "30 Adar I" for
  a date recorded in Adar I, "30 Adar" for an Adar of an ordinary or unknown
  year, "30 Adar II" for Adar II. The English month name comes from the
  shared source (section 3), and the Hebrew from KosherJava. An unmoved
  occurrence has no label.
- Calendar chips show only a generic "Personal event" / birthday marker, never
  titles or labels, so chips are unaffected. The full label appears in the
  day panel (birthdays and custom events) and in notifications.
- Applies to every target year, like everything else; the stored year only
  matters through the Adar mapping.
- Goes through `HebrewDateMatcher.occurrences`, so the calendar markers (both
  month modes), the day panel and notifications agree.

### Tests

Each of the three named cases with the target month 29 vs 30 days (years
found through KosherJava), for birthdays and custom events; the generic
always-29-day months; the label; yahrzeit events unaffected; the repository
lookups and labels. The test that pinned "day 30 is dropped in short years"
is updated.

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
- Yahrzeit matrix and repository occurrence tests (section 1b).
- View model tests: new birthday/event defaults to the current Hebrew year
  (with and without date override); unknown stored year opens as the current
  year; year clamping; Adar II → Adar when the year becomes regular.
- Date override parsing rejects out-of-range years.

Build: `./gradlew :app:assembleDebug`.

Emulator (if the shared emulator is free): March 2027 subtitle in English;
an Adar birthday marker in Adar II 5787; My events row for a year-1 birthday.

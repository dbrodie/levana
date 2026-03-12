# Levana - MVP 0.1 Implementation Plan

## Document Status

**Version:** 0.1
**Status:** Complete
**Architecture:** MVI + Koin (per `ARCHITECTURE_SPEC.md`)

---

## Development Workflow

Every increment follows this process:

1. **Branch** — Create a feature branch from `master` named `increment-XX-short-description`
2. **Spec** — Write a detailed spec at `specs/mvp_0.1/INCREMENT_XX.md` covering what will be built, key decisions, and acceptance criteria. Commit this spec to the branch.
3. **Implement** — Build the increment on the branch, committing as needed
4. **Build & Test** — Run `./gradlew build` (and any tests) to verify everything compiles and passes
5. **Review** — User reviews the branch (code + on-device verification against the spec's acceptance criteria)
6. **Merge** — Once approved, merge the branch back into `master`

---

## Dependency Graph

```
1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 ─┬─ 11 ─┬─ 13
                                             │      └─ 15
                                             ├─ 12
                                             └─ 14
```

After Increment 10, work can proceed in parallel:
- 11, 12, 14 in parallel → 13 (after 11) and 15 (after 11)

---

## Key Technical Decisions

These decisions apply across all increments:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Architecture | MVI | Per `ARCHITECTURE_SPEC.md` — predictable state, testable |
| DI | Koin | Per `ARCHITECTURE_SPEC.md` — no annotation processing |
| Hebrew calendar | KosherJava | Battle-tested, comprehensive halachic calculations |
| Date/time | `java.time` internally | KosherJava returns `java.util.Date` — convert at repository boundary |
| City data | KosherJava built-in | Per `ARCHITECTURE_SPEC.md` — start simple, expand later |
| Network | None | Fully offline — no network calls anywhere |
| Snapshots | State Catalog pattern | Every screen defines explicit state catalogs for Paparazzi |
| Hebrew text | `HebrewDateFormatter` | Never hardcode Hebrew text — always use formatter |
| Min API | 34 (Android 14) | Per `PROJECT_SPEC.md` |

---

## Increments

### ~~Increment 1: Project Skeleton & Hello World~~ DONE

**Branch:** `increment-01-project-skeleton` (merged)
**Depends on:** Nothing

**What to build:**
- Android project with Gradle Kotlin DSL and version catalog (`libs.versions.toml`)
- Koin initialization in `Application` class
- Material 3 theme (dynamic color on API 31+, fallback palette)
- Single `MainActivity` with Compose content showing "Levana" title
- ktlint configured via Gradle plugin
- Basic project structure: `ui/`, `domain/`, `data/` packages

**Key files:**
- `gradle/libs.versions.toml` — centralized dependency versions
- `app/build.gradle.kts` — application module config
- `app/src/main/java/com/levana/app/LevanaApplication.kt` — Koin setup
- `app/src/main/java/com/levana/app/ui/theme/Theme.kt` — Material 3 theme
- `app/src/main/java/com/levana/app/MainActivity.kt` — entry point

**Acceptance criteria:**
- [ ] App installs on device/emulator running API 34+
- [ ] Shows a screen with "Levana" title text
- [ ] Light and dark themes work (follow system setting)
- [ ] `./gradlew build` passes with no errors
- [ ] `./gradlew ktlintCheck` passes
- [ ] Koin initializes without crash (visible in logcat)

---

### ~~Increment 2: KosherJava Integration & Hebrew Date Display~~ DONE

**Branch:** `increment-02-hebrew-date` (merged)
**Depends on:** Increment 1

**What to build:**
- Domain models: `HebrewDay` (day, month, year, holidays), `Location` (lat, lon, timezone, name)
- `CalendarRepository` wrapping KosherJava's `JewishCalendar` and `HebrewDateFormatter`
- MVI triad for calendar screen: `CalendarState`, `CalendarIntent`, `CalendarViewModel`
- Hardcoded Jerusalem location (31.7683°N, 35.2137°E, Asia/Jerusalem)
- Display today's Hebrew date in three formats: Hebrew letters (ט״ו בשבט), transliteration, and Gregorian
- Koin module for data and domain layers

**Key technical notes:**
- `JewishCalendar` → `HebrewDay` conversion happens in `CalendarRepository`
- All `java.util.Date` from KosherJava converted to `java.time.LocalDate` at repository boundary
- `HebrewDateFormatter` configured for Hebrew and transliterated output

**Acceptance criteria:**
- [ ] App shows today's Hebrew date in Hebrew letters (e.g., ט״ו בשבט תשפ״ו)
- [ ] App shows transliterated Hebrew date (e.g., 15 Shevat 5786)
- [ ] App shows corresponding Gregorian date
- [ ] Date is correct (verify against a known Hebrew calendar source)
- [ ] `./gradlew build` passes

---

### ~~Increment 3: Monthly Calendar Grid~~ DONE

**Branch:** `increment-03-monthly-grid` (merged)
**Depends on:** Increment 2

**What to build:**
- 7-column calendar grid composable (Sunday–Saturday or Shabbat–Friday based on locale)
- Each cell shows Hebrew day number + Gregorian day number
- Month navigation: left/right arrows and horizontal swipe (HorizontalPager)
- Today cell highlighted with accent color
- Month/year header showing Hebrew and Gregorian month names
- Navigation Compose setup with type-safe routes (single route for now, prepared for more)
- First Paparazzi snapshot test with State Catalog for calendar grid

**Key technical notes:**
- Grid must handle months that span 4–6 rows
- Hebrew months have 29 or 30 days — calculate leading/trailing empty cells correctly
- Swipe uses `HorizontalPager` with preloaded adjacent months for smooth transition

**Acceptance criteria:**
- [ ] Monthly grid displays with 7 columns and correct day layout
- [ ] Hebrew day numbers shown in Hebrew letters (א, ב, ג, ..., ל)
- [ ] Gregorian day numbers shown alongside
- [ ] Today's cell is visually highlighted
- [ ] Arrow buttons navigate forward/backward by month
- [ ] Swipe gesture navigates between months
- [ ] Month header shows "Shevat 5786 / January–February 2026" style text
- [ ] Paparazzi snapshot test runs: `./gradlew app:testDebugUnitTest --tests '*Paparazzi*'`
- [ ] `./gradlew build` passes

---

### Increment 4: Daily Detail View -- DONE

**Branch:** `increment-04-daily-detail`
**Depends on:** Increment 3

**What to build:**
- Tap a calendar cell → navigate to day detail screen
- Day detail shows: full Hebrew date, full Gregorian date, day of week
- Holiday display: holiday name (Hebrew + English), holiday category badge
- Parsha display on Shabbat days (from KosherJava `JewishCalendar.getParsha()`)
- Omer count during Sefirat HaOmer (days and weeks format)
- Holiday model with categories: Torah, Rabbinic, Fast, Minor, Modern Israeli
- MVI triad for detail screen: `DayDetailState`, `DayDetailIntent`, `DayDetailViewModel`
- Navigation route: calendar → day detail (passing date as argument)

**Key technical notes:**
- `JewishCalendar` provides holidays via `getYomTovIndex()` — map to domain `Holiday` model
- Parsha from `JewishCalendar.getParsha()` and `getSpecialShabbos()`
- Omer from `JewishCalendar.getDayOfOmer()`

**Acceptance criteria:**
- [x] Tapping a day in the grid opens the detail screen
- [x] Detail shows full Hebrew date, Gregorian date, and day of week
- [x] A regular weekday shows no holiday or parsha
- [x] A Shabbat shows the parsha name
- [x] A holiday (e.g., navigate to next Pesach) shows the holiday name and category
- [x] During Omer period, count displays as "Day 15, which is 2 weeks and 1 day"
- [x] Back navigation returns to calendar grid
- [x] `./gradlew build` passes

---

### Increment 5: Location Management -- DONE

**Branch:** `increment-05-location`
**Depends on:** Increment 4

**What to build:**
- City selection using KosherJava's built-in location data, searchable list
- GPS location detection via `FusedLocationProviderClient`
- Manual latitude/longitude entry screen
- DataStore for persisted user preferences (`UserPreferences` data class)
- First-launch onboarding flow: prompt user to pick a location before showing calendar
- Location displayed in app bar; tapping opens location picker
- `PreferencesRepository` wrapping DataStore, exposed as `Flow<UserPreferences>`

**Key technical notes:**
- KosherJava's `GeoLocation` class provides city data — wrap in domain `Location` model
- FusedLocationProvider requires `ACCESS_FINE_LOCATION` permission — handle permission flow
- DataStore stores serialized preferences (location, timezone, etc.)
- Onboarding screen shown only when no location is saved

**Acceptance criteria:**
- [x] First launch shows location onboarding (not the calendar)
- [x] City search filters the list as user types
- [x] Selecting a city saves it and proceeds to calendar
- [x] GPS button requests location permission and detects location
- [x] Manual entry accepts lat/lon and saves
- [x] Location persists across app restarts (kill and reopen)
- [x] Calendar/date calculations update to reflect selected location's timezone
- [x] Location name shown in app bar
- [x] `./gradlew build` passes

---

### Increment 6: Zmanim Display -- DONE

**Branch:** `increment-06-zmanim`
**Depends on:** Increment 5

**What to build:**
- Zmanim screen showing 14 prayer times from `ComplexZmanimCalendar`:
  - Dawn (Alot HaShachar), Misheyakir, Sunrise (HaNetz), Latest Shema (MGA & GRA),
    Latest Shacharit, Chatzot, Mincha Gedolah, Mincha Ketanah, Plag HaMincha,
    Sunset (Shkiah), Nightfall (Tzet), Midnight (Chatzot HaLaylah)
- Times grouped by time-of-day category (Morning, Afternoon, Evening, Night)
- Bottom navigation bar with three tabs: Calendar, Zmanim, Settings (placeholder)
- "Show Zmanim" button on day detail screen → navigates to zmanim for that date
- MVI triad: `ZmanimState`, `ZmanimIntent`, `ZmanimViewModel`
- `ZmanimRepository` wrapping `ComplexZmanimCalendar`

**Key technical notes:**
- `ComplexZmanimCalendar` needs a `GeoLocation` — construct from saved `Location`
- All times are `java.util.Date` from KosherJava — convert to `java.time.LocalTime`
- Some zmanim may be `null` (e.g., extreme latitudes) — handle gracefully
- Date picker on zmanim screen to view times for other dates

**Acceptance criteria:**
- [x] Bottom navigation shows Calendar, Zmanim, Settings tabs
- [x] Zmanim screen lists all 13 times grouped by category
- [x] Times are formatted in 12-hour or 24-hour based on system setting
- [x] Sunrise and sunset times are reasonable for the saved location
- [x] Tapping "Show Zmanim" on day detail navigates to zmanim for that day
- [x] Changing the date on zmanim screen updates all times
- [x] `./gradlew build` passes

---

### Increment 7: Shabbat & Candle Lighting -- DONE

**Branch:** `increment-07-candle-lighting` (merged)
**Depends on:** Increment 6

**What to build:**
- Candle lighting time calculation: sunset minus configurable offset (default 18 min)
- Havdalah time calculation based on nightfall
- Jerusalem option: 40 minutes before sunset
- Yom Tov candle lighting times (first night and second night where applicable)
- Flame icon (🕯️ or Material icon) on Friday cells in the calendar grid
- Candle lighting and Havdalah times shown on day detail for Friday/Saturday
- Candle lighting and Havdalah included in zmanim display when applicable

**Key technical notes:**
- `ComplexZmanimCalendar.getCandleLighting()` and related methods
- Candle lighting offset stored in `UserPreferences` via DataStore
- Jerusalem detection: if saved location is Jerusalem, default to 40-min offset
- Halachically critical — times must be verified against published luach

**Acceptance criteria:**
- [x] Friday cells in calendar grid show candle lighting indicator
- [x] Day detail for Friday shows candle lighting time
- [x] Day detail for Saturday shows Havdalah time
- [x] Default offset is 18 minutes before sunset
- [x] Candle lighting times appear in zmanim list on Friday
- [x] Yom Tov eve shows candle lighting time
- [x] Times match a published source (e.g., chabad.org for the same city/date)
- [x] `./gradlew build` passes

---

### Increment 8: Settings Screen -- DONE

**Branch:** `increment-08-settings` (merged)
**Depends on:** Increment 7

**What to build:**
- Settings screen (replacing placeholder tab) with sections:
  - **Location:** Current location display, tap to change
  - **Minhag:** Ashkenazi / Sephardi / Yemenite / Chabad radio selection
  - **Israel/Diaspora:** Toggle (auto-detected from location, with manual override)
  - **Modern Israeli Holidays:** Toggle to show/hide
  - **Hebrew-primary:** Toggle (switches calendar to Hebrew-month view — implemented in Inc. 9)
  - **Candle lighting offset:** Number picker (minutes before sunset)
- All settings persisted in DataStore and exposed as `Flow<UserPreferences>`
- All downstream screens react to settings changes via Flow collection

**Key technical notes:**
- Minhag affects parsha readings, some holiday observances, and zmanim defaults
- Israel/Diaspora affects Yom Tov second day and parsha alignment
- Settings changes must propagate immediately (no "save" button)
- MVI triad: `SettingsState`, `SettingsIntent`, `SettingsViewModel`

**Acceptance criteria:**
- [x] Settings screen accessible via bottom navigation
- [x] Minhag selection persists and updates relevant calculations
- [x] Israel/Diaspora toggle changes Yom Tov day count (navigate to a Yom Tov to verify)
- [x] Modern Israeli Holidays toggle shows/hides them on calendar
- [x] Candle lighting offset change updates candle lighting times immediately
- [x] All settings persist across app restarts
- [x] `./gradlew build` passes

---

### Increment 9: Hebrew-Primary Calendar & RTL -- DONE

**Branch:** `increment-09-hebrew-primary` (merged)
**Depends on:** Increment 8

**What to build:**
- Hebrew-primary calendar mode: grid organized by Hebrew months (29 or 30 days)
  - Handles Adar I/II in leap years
  - Gregorian date shown as secondary in each cell
- Language picker in settings: Hebrew / System language
- Full RTL layout support when Hebrew is selected
- String resources in `values/strings.xml` (English) and `values-iw/strings.xml` (Hebrew)
- All user-facing strings extracted to resources

**Key technical notes:**
- Hebrew-primary grid shows the Hebrew month's days (not Gregorian month mapped to Hebrew)
- RTL layout triggered by `LocalLayoutDirection` override when Hebrew is selected
- Bidirectional text handling for mixed Hebrew/English content
- `HebrewDateFormatter` handles all Hebrew text rendering

**Acceptance criteria:**
- [x] Toggling "Hebrew-primary" in settings switches to Hebrew month grid
- [x] Hebrew month grid shows correct number of days (29 or 30)
- [x] Leap year shows both Adar I and Adar II as separate months
- [x] Gregorian dates appear as secondary text in Hebrew-primary cells
- [ ] Language picker offers Hebrew and System options (deferred — RTL tied to Hebrew-primary toggle)
- [x] Selecting Hebrew triggers RTL layout throughout the app
- [x] Mixed Hebrew/English text displays correctly
- [x] `./gradlew build` passes

---

### Increment 10: Holiday Enrichment & Torah Readings -- DONE

**Branch:** `increment-10-holiday-enrichment` (merged)
**Depends on:** Increment 9

**What to build:**
- Color-coded calendar cell indicators by holiday category:
  - Torah holidays: one color, Rabbinic: another, Fast days: muted, etc.
- Full Torah reading information on day detail:
  - Weekly parsha name (Hebrew + transliterated)
  - Special maftir readings (Shekalim, Zachor, Parah, HaChodesh)
  - Double parshiot handled correctly
  - Holiday-specific Torah readings
- Omer count with formatted text: "Today is X days, which is Y weeks and Z days of the Omer"
- Molad calculation and display on Shabbat Mevarchim
- Special Shabbatot names (Shabbat Hagadol, Shabbat Shuva, etc.)

**Key technical notes:**
- KosherJava `JewishCalendar.getSpecialShabbos()` for special Shabbatot
- `JewishCalendar.getMoladAsDate()` for Molad
- Color mapping: define a sealed class or enum mapping `HolidayCategory → Color`
- Double parshiot: `JewishCalendar.getParsha()` returns the combined reading

**Acceptance criteria:**
- [x] Calendar cells show color-coded dots/indicators for holidays
- [x] Different holiday categories have distinct colors
- [x] Shabbat detail shows parsha with Hebrew and transliterated names
- [x] Special maftir readings appear on the correct Shabbatot
- [x] Double parshiot display correctly (e.g., "Vayakhel-Pekudei")
- [x] Omer count shows day and week breakdown
- [x] Shabbat Mevarchim shows Molad information
- [x] Special Shabbatot (Hagadol, Shuva, etc.) identified correctly
- [x] `./gradlew build` passes

---

### Increment 11: Personal Events -- DONE

**Branch:** `increment-11-personal-events` (merged)
**Depends on:** Increment 10

**What to build:**
- Room database (v1) with `PersonalEvent` entity:
  - Fields: id, name, hebrewDay, hebrewMonth, hebrewYear, eventType (BIRTHDAY/YAHRZEIT/CUSTOM), customTitle (for CUSTOM type), notes
- Three event types:
  - **Birthday** — name + Hebrew date, recurs annually on the same Hebrew date
  - **Yahrzeit** — name + Hebrew date, recurs annually with special Adar rules
  - **Custom** — user-provided title + Hebrew date, recurs annually on the same Hebrew date
- CRUD screens: event list, add event, edit event, delete confirmation
- Hebrew date picker composable (select Hebrew day, month, year)
- Yahrzeit recurrence calculation with leap year Adar rules:
  - Death in Adar of non-leap year → observe in Adar II of leap year
  - Death in Adar I of leap year → observe in Adar I
  - Death in Adar II of leap year → observe in Adar II
- Explanatory text on the add/edit screen when Yahrzeit is selected, noting the special Adar rules
- Calendar cell indicators for days with personal events
- Koin module for Room database

**Key technical notes:**
- Room database created with `@Database(version = 1)`
- Hebrew date picker: scrollable columns for day (1-30), month (Tishrei-Elul), year
- Yahrzeit Adar rules are halachically defined — implement per Shulchan Aruch
- Custom events use `customTitle` as display name; Birthday/Yahrzeit use `name` (the person's name)
- Personal events accessible from a FAB or menu on the calendar screen

**Acceptance criteria:**
- [x] Can add a birthday with Hebrew date and name
- [x] Can add a yahrzeit with Hebrew date and name
- [x] Can add a custom event with user-provided title and Hebrew date
- [x] Selecting Yahrzeit type shows explanatory text about Adar rules
- [x] Events appear in a list view
- [x] Can edit and delete existing events
- [x] Events show as indicators on their calendar cells
- [x] Yahrzeit in Adar of non-leap year appears in Adar II of leap year
- [x] Yahrzeit in Adar I of leap year stays in Adar I
- [x] Hebrew date picker allows selecting valid Hebrew dates
- [x] `./gradlew build` passes

---

### Increment 12: Dynamic Holiday Theming -- DONE

**Branch:** `increment-12-holiday-theming` (merged)
**Depends on:** Increment 10

**What to build:**
- Auto accent color changes based on current/upcoming holiday:
  - Chanukah → Blue/Silver
  - Sukkot → Green
  - Pesach → Orange/Red
  - Purim → Purple
  - Rosh Hashanah → Gold/White
  - Yom Kippur → White/Cream
  - Shavuot → Yellow/Green
  - Fast days → Muted/Gray
  - Shemini Atzeret/Simchat Torah → Deep Purple
  - Regular days → Material You / default
- Toggle in settings to enable/disable dynamic theming
- Theme transition: color change applies from erev (evening before) through end of holiday

**Key technical notes:**
- Use `MaterialTheme` with dynamically computed `ColorScheme`
- Holiday detection from `JewishCalendar` determines current theme
- Theme colors defined as `Map<HolidayThemePeriod, ColorScheme>`
- When disabled, fall back to default Material You or static palette

**Acceptance criteria:**
- [x] During a holiday period, app accent color matches the defined theme
- [x] Color changes at the correct halachic times (erev start)
- [x] Disabling dynamic theming in settings reverts to default colors
- [x] Regular days use the default Material You / static palette
- [x] Theme change is visually smooth (no jarring flash)
- [x] `./gradlew build` passes

---

### Increment 13: Android Contacts Integration

**Branch:** `increment-13-contacts`
**Depends on:** Increment 11

**What was built:**
- Separate birthday system stored in Android Contacts DB via custom MIME type
- `ContactBirthdayRepository` for CRUD on contact birthday data rows
- `HebrewDateMatcher` for birthday and yahrzeit Adar-aware date matching
- Dedicated birthday UI: `ContactBirthdayScreen` with contact picker and Hebrew date picker
- Tabbed `EventsScreen` with Birthdays tab and Custom Events tab
- `CalendarEvent` sealed interface unifying birthdays and custom events for `DayDetailScreen`
- `PersonalEvent` simplified: removed `EventType` enum, added `useYahrzeitRules` boolean
- `READ_CONTACTS` + `WRITE_CONTACTS` runtime permission flow
- Calendar dot indicators for contact birthdays

**Key technical notes:**
- Custom MIME type: `vnd.android.cursor.item/com.levana.hebrew_birthday`
- Birthdays are stored entirely in the Android Contacts DB (not Room) as custom data rows
- Personal events (yahrzeits, custom) remain in Room — the two systems are independent
- `HebrewDateMatcher` implements both simple birthday rules and Shulchan Aruch yahrzeit rules for Adar in leap years
- `CalendarEvent` sealed interface (`Birthday` | `CustomEvent`) unifies display in DayDetail
- Permission gate checks both READ and WRITE before showing birthday features

**Acceptance criteria:**
- [x] Adding a birthday picks a contact and sets a Hebrew date
- [x] Birthdays tab shows all contacts with Hebrew birthdays and their photos
- [x] Birthday data is stored as a custom MIME row in Android Contacts
- [x] Deleting a birthday removes the custom data row from Contacts
- [x] Custom events (yahrzeits, etc.) still work independently in Room
- [x] DayDetail shows both birthdays and custom events for the selected date
- [x] Calendar shows dot indicators for days with birthdays
- [x] `./gradlew build` passes

---

### Increment 14: Android Calendar Integration

**Branch:** `increment-14-system-calendar`
**Depends on:** Increment 10

**What to build:**
- Read system calendar events via `CalendarContract`
- Calendar selection screen: list device calendars with checkboxes
- Persist selected calendars in DataStore
- Show colored dots on Levana calendar cells for days with system events
- Day detail lists system calendar events (title, time, calendar color)
- `READ_CALENDAR` permission flow

**Key technical notes:**
- Use `CalendarContract.Events` and `CalendarContract.Calendars` content providers
- Read-only — no event creation or modification
- Respect each calendar's display color for dots
- Permission denied → gracefully hide the feature, don't crash

**Acceptance criteria:**
- [x] Calendar selection screen lists all device calendars
- [x] Selected calendars' events appear as colored dots on Levana calendar
- [x] Day detail shows event titles and times from selected calendars
- [x] Calendar colors from the system are respected
- [x] Denying calendar permission hides the feature gracefully
- [x] Selected calendars persist across restarts
- [x] `./gradlew build` passes

---

### ~~Increment 15: Notifications~~ DONE

**Branch:** `increment-15-notifications` (merged)
**Depends on:** Increment 11

**What to build:**
- Notification channels: Candle Lighting, Holidays, Fasts, Personal Events, Omer
- `AlarmManager` for time-based notifications (candle lighting, Omer at sunset)
- `WorkManager` for daily scheduling of event-based notifications
- Per-category enable/disable and timing configuration in settings
- `BootReceiver` to reschedule alarms after device reboot
- Deep-link from notification tap → day detail screen
- `POST_NOTIFICATIONS` (API 33+) permission flow

**Key technical notes:**
- `AlarmManager.setAndAllowWhileIdle()` for time-based notifications (no special permission needed)
- `WorkManager` periodic task recalculates and schedules next day's alarms
- Boot receiver registered in manifest with `RECEIVE_BOOT_COMPLETED`
- Notifications must work when app is not in foreground
- All fasts notify the day before (Tisha B'Av and minor fasts alike)

**Acceptance criteria:**
- [x] Candle lighting notification fires at configured time on erev Shabbat/Yom Tov
- [x] Holiday notification fires configurable days before holiday begins
- [x] Fast notification fires the day before the fast with start/end times
- [x] Personal event notification fires on the day of the event
- [x] Omer counting reminder fires at sunset during Omer period
- [x] Tapping a notification opens the relevant day detail
- [x] Notifications survive device reboot
- [x] Per-category toggles in settings enable/disable notifications
- [x] `./gradlew build` passes

---

## Verification Checklist

After completing all MVP 0.1 increments, the app should satisfy:

- [ ] Fully offline — no network permissions in manifest
- [ ] Hebrew dates accurate against published calendars
- [ ] Zmanim times accurate against published sources for major cities
- [ ] Candle lighting times accurate (halachically critical)
- [ ] Israel/Diaspora differences correct (Yom Tov days, parsha alignment)
- [ ] Yahrzeit Adar rules correct per halacha
- [ ] All four minhagim selectable and affecting relevant calculations
- [ ] RTL layout correct in Hebrew mode
- [ ] `./gradlew build` passes

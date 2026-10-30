---
id: TASK-8
title: Add an Agenda view and Month/Agenda drawer navigation (design 1a/1b)
status: Done
assignee:
  - '@claude'
created_date: '2026-10-21T11:19:31Z'
updated_date: '2026-10-30T09:05:00Z'
labels:
  - curated-import
  - source-specs
dependencies:
  - TASK-45
references:
  - levana-bpd
priority: medium
type: feature
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Show holidays, Shabbat/Yom Tov times, birthdays, yahrzeits, personal events and selected device-calendar events together in one chronological, scrollable agenda. Reuse the existing occurrence rules (Hebrew-date recurrence, Adar/yahrzeit settings), the chosen device calendars, and the holiday/zmanim calculations. Add no new calculation logic.

**Design source:** Claude Design "Jewish Calendar App Design", Agenda view, https://claude.ai/artifact/7AbFzp2Z1vRcvhbx7Nt71Q (owner, 2026-10-30). Agents cannot open it directly (login and bot check). The coordinator captured it through the Paseo browser:
- `/tmp/levana-agenda/01-agenda-and-drawer-top.png`
- `/tmp/levana-agenda/02-agenda-and-drawer-scrolled.png`

If /tmp has been cleared, ask the owner to reopen the link in the Paseo browser tab, or for an export. The interactions below are inferred from static frames; the mockup could not be clicked.

**1a Agenda screen**
- **Top bar:**
  - menu (drawer);
  - title: the current month in the leading calendar (e.g. "חשוון ה׳תשפ״ז") with a dropdown arrow, presumably to jump to a month or date;
  - search icon;
  - "today" calendar icon to jump back to today.
- **Sub-header:** the other calendar's span (e.g. "October – November 2026") and the same **Hebrew | Gregorian** segmented toggle as the calendar screen, which chooses the leading calendar for titles and month grouping.
- **List grouped by day:** a left date rail shows the weekday (FRI), the Hebrew day in letters (large) and the Gregorian date (small, "30 Oct"). **Today's** Hebrew day sits in a filled circle. Only days with items appear. Each day has one or more cards to the right.
- **Card types, each with a leading icon and a title; secondary line and trailing time optional:**
  - **Candle lighting:** "Candle lighting · 16:21" (outlined card, candles icon).
  - **Personal or device-calendar event:** title (ellipsized), location as the secondary line (e.g. "Shabbat dinner at the Lev… / Rechavia · 19:00", "Kiddush — Cohen bar mit… / Yeshurun Synagogue · 11:30"); outlined card, calendar icon.
  - **Shabbat:** "Parashat Vayera / Havdalah 17:34" (tonal or filled card, book icon).
  - **Yahrzeit:** "Yahrzeit — Moshe ben Avraham / 23 Cheshvan · candle at tzeit" (outlined, memorial-candle icon). **Birthday** cards follow the same pattern.
  - **Holiday or Rosh Chodesh:** "Rosh Chodesh Kislev" (accent-tinted card, celebration icon). Holiday cards use the app's holiday category colours.
- **Month headers** when a new month begins in the leading calendar: "כסלו ה׳תשפ״ז  Nov – Dec 2026".
- **Order within a day:** all-day items (holiday, Shabbat/parasha) and timed items in chronological order. The mockup puts candle lighting first on Friday, then a 19:00 event.

**1b Navigation drawer (part of this design)**
- **Header:** "Levana".
- **ACTIVE CITY card:** unchanged from today's drawer. The mockup's per-city local times are **out of scope** (owner, 2026-10-30): don't change the city list.
- **Navigation:**
  - a **Calendar** section with **Month** (the existing calendar screen) and **Agenda** (this screen; selected state shown);
  - **My events**, **Cities** (the existing Manage cities) and **Settings**.

**Requirements**
- Sources:
  - holidays, fasts and Rosh Chodesh (existing holiday logic, Israel/Diaspora from the resolved setting);
  - Shabbat/Yom Tov candle lighting and havdalah, with the parasha;
  - personal events, birthdays (including contact birthdays) and yahrzeits, using the existing Hebrew recurrence rules;
  - selected device-calendar events, respecting calendar selection and permission (if permission is missing, show the rest and offer to grant it).
- "Today" comes from DeviceContext (TASK-5), including the developer fixed date.
- Scrolling: opens on today. Loads lazily forward with no fixed end, and backward into the past. Smooth on a month boundary.
- Tapping a card opens its target: an event or birthday opens its editor or details; a holiday or Shabbat item opens the day in the calendar or Zmanim; a device event opens the system calendar event.
- Empty state for long empty ranges, and loading states.
- English and Hebrew with RTL (the date rail mirrors). Material 3, the app theme, holiday themes, dark mode and Material You (TASK-13). TalkBack: each day is announced with both dates, and each card with its type, title and time. Text scaling.
- Month stays the start screen; Agenda is reached from the drawer (default; the owner didn't ask for a remembered start view).
- MVI; occurrence aggregation testable outside Compose; no duplication of the day panel's or the notifications' occurrence code. Extract shared logic instead.

**Owner decisions (2026-10-30)**
1. **Agenda times** (candle lighting, havdalah, any zman shown) use the **selected city**, like the day panel and Zmanim screen, with the TASK-5 zone label when its zone differs from the device's. Dates and "today" still come from DeviceContext.
2. **Search** covers everything the agenda lists: event, birthday and yahrzeit titles, holidays, parashot and device-calendar events, over a bounded range (about the past and next 12 months; record the exact range in the plan). Tapping a result scrolls the agenda to that day.
3. **Tapping the month title** opens the shared dual-calendar date picker (TASK-45); the agenda scrolls to the chosen date.
4. **Drawer:** add only the navigation (a Calendar section with Month and Agenda). Do not add per-city times or otherwise change the city list; that is unrelated to this task.
5. **Start screen:** unchanged (Month).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Agenda screen per design 1a: date rail (weekday, Hebrew day, Gregorian date, today highlight), typed cards (candle lighting, Shabbat/parasha + havdalah, personal and device events with location and time, yahrzeits, birthdays, holidays and Rosh Chodesh), month headers and the Hebrew/Gregorian toggle; top bar with a month jump through the TASK-45 picker, search across all listed items, and today
- [x] #2 All sources are included with correct chronological order and Hebrew recurrence; times use the selected city; device-calendar selection and permission are respected; no occurrence logic is duplicated
- [x] #3 Drawer gains a Calendar section with Month and Agenda; the city list and selection are unchanged (no per-city times); Month stays the start screen
- [x] #4 Unit tests for aggregation, ordering and search; emulator screenshots compared against the design in English/Hebrew, light/dark, a holiday theme; TalkBack labels; Roborazzi baselines added for the new screen and drawer
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
**Data (no new calculation logic, one occurrence resolver)**
1. `HebrewDateMatcher.occurrencesBetween(day, month, year, useYahrzeitRules, start, end, rules)`: the observed Gregorian dates in a range, from `occurrences()` for each Hebrew year spanned (invalid month/day skipped). Unit test: equals the per-day `occurrenceOn` result over several years (Adar, day 30, yahrzeit rules).
2. `PersonalEventRepository.getEventsBetween(start, end)` and `ContactBirthdayRepository.getBirthdaysBetween(start, end)`: one read, then (1). `getEventsForDate` / `getBirthdaysForDate` unchanged.
3. `SystemCalendarRepository.getEventsForDateRange(start, end, ids)`: the existing instance query, grouped by every covered date; `getEventsForDate` delegates to it. `SystemCalendarEvent` gains `location` (EVENT_LOCATION, default "").
4. `domain/agenda`: `AgendaItem` (Holiday, CandleLighting, Shabbat [parasha + havdalah], Havdalah, PersonalEvent [yahrzeit flag], Birthday, DeviceEvent), `AgendaDay(date, hebrewDay, items)`. `AgendaAssembler` (pure): builds one day's items from `DayInfo`, `ShabbatInfo`, occurrences and device events, and orders them: all-day items first (holiday, Shabbat/parasha, yahrzeit, birthday, personal event, all-day device event), then timed items by time (candle lighting, havdalah, timed device events); ties by type then title. Days without items are dropped.
5. `AgendaRepository.load(start, end, prefs)`: holidays via `CalendarRepository.getDayInfo` (Israel/Diaspora + modern-holiday setting), candle lighting/havdalah via `ZmanimRepository.getShabbatInfo` at the **selected city** (`displayCity`), events/birthdays/device events via (2)/(3) with the selected calendars. Missing contacts/calendar permission → those sources empty, `calendarPermission=false` reported. Runs off the main thread.
6. `AgendaSearch` (pure): case-insensitive match (gershayim/apostrophes ignored) on titles, English and Hebrew holiday/parasha names, event notes, device-event locations. Range: **today − 12 months .. today + 12 months (Gregorian, inclusive)**, loaded once per search session, reloaded on data change.

**Screen (MVI: `ui/agenda/AgendaState/Intent/ViewModel/Screen`)**
7. VM: today from DeviceContext (`todayUpdates`), leading calendar from the shared `calendarHebrewMode` preference (same toggle as Month). Window loads in 6-week chunks: opens on today−6w..today+12w, extends forward/backward as the list nears an end (no fixed end). Jump (picker/search/today) inside the window scrolls; outside it reloads a window around the date. One-shot scroll targets through a channel. ChangeRefresher on events/permissions; prefs changes reload. `otherClockCityName` (TASK-5) for the zone label.
8. UI: own header like Month (menu, month title with dropdown → TASK-45 `HebrewDatePickerDialog`, search, today), sub-header with the other calendar's span + `CalendarModeSelector`. LazyColumn: month headers when the leading month changes ("כסלו תשפ״ז  Nov – Dec 2026"), day rows with a date rail (weekday, leading-calendar day large, other date small, today in a filled circle; mirrors in RTL) and typed cards (outlined: candle lighting, events, yahrzeit, birthday; tonal: Shabbat; holiday-category-tinted: holidays/Rosh Chodesh/fasts). Times 12/24h per device; the selected city's zone label on candle-lighting/havdalah when it differs. Calendar-permission banner with Allow (session dismiss) when missing. Loading, loading-more and empty (search) states. TalkBack: day = both dates; card = type, title, time.
9. Taps: personal event/yahrzeit → event editor; birthday → contact birthday editor; device event → system calendar (shared helper extracted from the day panel); holiday → Month on that day (calendar back-stack savedStateHandle, small additive CalendarScreen param); candle lighting/Shabbat/havdalah → Zmanim for that date.
10. Search mode: top bar becomes a search field; results list (date + card); tapping scrolls the agenda to that day.

**Navigation (small, additive; TASK-43 also edits ui/navigation)**
11. `AgendaRoute`; drawer gets a "Calendar" section with Month (CalendarRoute) and Agenda, a divider, then My events and Settings unchanged; city card/list/Manage cities untouched. Agenda is a drawer route with its own header (global top bar hidden as for Month). Start screen stays Month.

**Strings/tests/verification**
12. English + Hebrew strings. Unit tests: occurrencesBetween equivalence, assembler ordering/types, repository aggregation (recurrence, selected city times, permission), search, header formatting, VM window/jump. Roborazzi: list changed drawer images, re-record and inspect; add `app_agenda`, `app_agenda_gregorian`, `app_agenda_search`, `app_agenda_search_empty` (en/he × light/dark) on a fixed seeded range.
13. Build/test/lint/verifyRoborazzi; emulator screenshots (en/he, light/dark, holiday theme, search, picker jump, card taps, calendar permission on/off, drawer) saved outside the worktree with a README.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
**Implementation (tested commit e54ee1b)**
- Shared occurrence logic, no duplication: `HebrewDateMatcher.occurrencesBetween` (range form of the one resolver; unit-tested equal to the day panel's per-day `occurrenceOn` over 2023–2029, Adar/day-30/yahrzeit rule sets); `PersonalEventRepository.getEventsBetween`, `ContactBirthdayRepository.getBirthdaysBetween`; `SystemCalendarRepository.getEventsForDateRange` (day panel's `getEventsForDate` now delegates to it; events gain `location`); `openDeviceCalendarEvent` extracted from the day panel; `CalendarHeaderFormatter.hebrewMonthRange` gained a month-based overload.
- `domain/agenda`: `AgendaItem`/`AgendaDay`, pure `AgendaAssembler` (ordering: all-day holiday → Shabbat → yahrzeit → birthday → event → all-day device; then timed by time), `AgendaSearch` (range today −12 months .. today +12 months, inclusive; titles, notes, locations, English+Hebrew holiday/parasha names incl. "Rosh Chodesh <month>"). `data/AgendaRepository` gathers DayInfo + ShabbatInfo at `displayCity` (like the day panel) + events/birthdays/selected device events; reports calendar permission.
- `ui/agenda` MVI. Window opens today −42 .. today +83 days, extends 42 days at either end near the list edges; jumps outside the window reload around the date; scroll requests are state (handled by the screen). Leading calendar = the shared `calendarHebrewMode` preference. Zone label ("Jerusalem time") on candle-lighting/havdalah when the city's zone differs.
- Taps: event/yahrzeit → editor; birthday → contact birthday editor; device event → system calendar; holiday → Month on that day (CalendarRoute back-stack savedStateHandle `showDay`, new optional `showDate` param on CalendarScreen); candle lighting/Shabbat/havdalah → Zmanim.
- Navigation (additive): `AgendaRoute`; drawer "Calendar" heading with Month + Agenda, divider, then My events / Settings (DrawerNavigationItem extracted); city card/list/Manage cities unchanged; LevanaApp hides the global top bar for Agenda as for Month. Start screen unchanged. ScreenshotHarness gained an `extraModules()` hook.

**Verification**
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks` on e54ee1b: BUILD SUCCESSFUL (963 tests). An earlier full run had one failure in the untouched `AppShellScreenshotTest.contactBirthday[HE_DARK]` (timeout waiting for the birthday title); it passed 3/3 when rerun alone and in the final full run, so treated as intermittent.
- New unit tests: OccurrencesBetweenTest, AgendaAssemblerTest, AgendaSearchTest, AgendaRepositoryTest (selected-city times, recurrence incl. 30 Cheshvan and leap Adar, Israel/Diaspora, permission off/on with a fake provider, unselected calendar), AgendaFormatterTest, AgendaViewModelTest (window, edge loading, jumps, search + jump, shared toggle, city change reload + zone label). LevanaAppNavigationTest now walks Agenda → Month.
- Roborazzi: first `verifyRoborazziDebug --rerun-tasks` failed only `app_drawer_{en,he}_{light,dark}` (the test stops at the first mismatch). Re-recorded; changed baselines (all 8 read; only the drawer nav section differs, header/city card/city list/Manage cities identical): app_drawer_en_light, app_drawer_en_dark, app_drawer_he_light, app_drawer_he_dark, app_drawer_cities_en_light, app_drawer_cities_en_dark, app_drawer_cities_he_light, app_drawer_cities_he_dark. Added (en/he × light/dark, 28 images): app_agenda, app_agenda_gregorian, app_agenda_month_header (Rosh Chodesh Teves/Chanukah/fast + month header, scrolled by list key), app_agenda_drawer, app_agenda_no_calendar, app_agenda_search, app_agenda_search_empty.
- Emulator (API 36, device date 30 Oct 2026 Jerusalem): screenshots + README at /tmp/levana-agenda/task8-emulator/ — drawer, agenda en/he light/dark, Chanukah theme, calendar permission off → banner → grant/select → device events, search + far jump, month jump through the TASK-45 picker, card taps (holiday → Month day, candles → Zmanim, yahrzeit → editor, device event → Google Calendar launched). TalkBack labels confirmed in uiautomator dumps (e.g. day "כ׳ חשון תשפ״ז, Saturday, October 31, 2026"; card "Calendar event, Kiddush — Cohen bar mitzvah, 11:30 AM, Yeshurun Synagogue").
- Limitations: Google Calendar showed "event not found" for the adb-created LOCAL calendar (emulator data, not the intent); the default emulator GPS (Mountain View) read in the Jerusalem zone gave 2:55 AM candle times until GPS was set to Jerusalem (existing TASK-5 behaviour); one ClassCastException on back-to-back locale+night changes = known TASK-50, not reproducible otherwise. Not tested on device: text scaling beyond the defaults, live TalkBack navigation.

**Review round 2 (coordinator: month-header flake, rebase onto TASK-43, agenda walkthrough screen)**

Root cause of the missing "טבת 5787" header in app_agenda_month_header:
- Reproduced with forced reruns (≈1 in 15–20 runs of AgendaScreenshotTest.agenda). Semantics dumps taken just before and after the failing capture show the Teves header node absent (stable state, not a mid-frame capture) while the title, toggle and rails are Hebrew-led.
- Temporary instrumentation of each list composition (mode, window, days, first index, header keys) gave identical sequences in passing and failing runs: window 2026-10-06..2027-02-08, 51 days, Hebrew → Gregorian → Hebrew; the last composition held the correct Hebrew rows including the Teves header (month-20798 = 11 Dec 2026) with first visible index 32. So the window/chunk loading and the grouping were deterministic and correct; no header depended on load window or page.
- What varied was the LazyColumn's item set: the test switched to Hebrew and immediately scrolled by key, while the mode change (a DataStore preference, written on IO) arrived asynchronously. When that regrouping (separate header items added/removed) landed together with the pending programmatic scrollToItem, the list occasionally kept its previous item set; items already shown recomposed with Hebrew rails, but the inserted header item was never composed.
- Fix (875e4a1→rebased d942ede): month headers are now part of the day item they introduce (`ui/agenda/AgendaRows.kt`; list keys are days only), so changing the leading calendar changes items' content, never which items the list holds. Also fixed an off-by-one: the scroll-request index and the title's top-day lookup now account for the "loading earlier" row. The screenshot test now waits for the saved Hebrew mode before scrolling. Images are pixel-identical to the previous baselines.
- New tests: AgendaRowsTest (headers at the first listed day of each leading month; identical however the window is assembled — boundaries on a month's first/last day, mid-month, appended or prepended; sparse days; keys unchanged across a mode switch) and AgendaViewModelTest now checks that after LoadLater + LoadEarlier the days and grouping equal one load of the whole range.
- Correction: my earlier "contactBirthday passed 3/3 when rerun alone" used `verifyRoborazziDebug --rerun`, which did not re-execute the up-to-date test task, so it was not evidence. Forced reruns need `:app:testDebugUnitTest --rerun` (or --rerun-tasks).

contactBirthday[HE_DARK] flake: a different race, not the shared harness. My events' Add only opens the birthday editor once `hasContactsPermission` is true, and EventsViewModel set it only after the contacts query (`getAll()`, IO) returned; the test taps Add as soon as the tab label shows, so under load Add took the "request permission" branch and the editor title never appeared. Fixed in 8ef6bd0 (two lines, outside TASK-8's scope, flagged for review): the permission is published before the query. EventsPermissionStateTest fails without the fix and passes with it. User-visible effect: a quick tap on + in My events now opens the editor instead of silently re-asking for a granted permission.

Rebase onto origin/master cfa82e2 (TASK-43): conflicts only in di/AppModules.kt (both view models kept) and ScreenshotHarness.kt (TASK-43's `testModules()` hook replaces my `extraModules()`; AgendaScreenshotTest uses it). LevanaApp/AppStartupEffects merged cleanly.

Agenda walkthrough screen (db4b297): registry entry `agenda` after `days-shabbat-holidays`, no permission; illustration = the real agenda `DayRow` (now internal) with fixed sample entries (Fri 30 Oct: candle lighting 16:23, "Dinner at the Levis · Rechavia" 19:00; Shabbat: Parashat Vayera, havdalah 17:34), at 0.85 text scale and 24-hour times like the other illustrations' samples (`DayRow(use24Hour)`); title/body/sample strings in English and Hebrew. Tests: WalkthroughSelectionTest approved list + placement/upgrade test (generic upgrade tests now use a fake `next-feature`); WalkthroughViewModelTest's single-new-screen test uses the real registry (everything but `agenda` seen); runOfThree uses the last three screens and runOfTwelve pads to 12, so their baselines are unchanged; new `agendaAfterUpdate` capture.
- Added baselines (8): walkthrough_agenda_{en,he}_{light,dark}, walkthrough_agenda_update_{en,he}_{light,dark}.
- Changed baselines (48): every full-run walkthrough page and its _granted variant (welcome_hebrew_calendar, zmanim_location(+granted), days_shabbat_holidays, personal_events(+granted), reminders(+granted), device_calendars(+granted), widgets, appearance × en/he × light/dark). A pixel-row diff of all 48 against HEAD shows differences only in rows 755–762, the page-dot band (8 → 9 dots).
- Specs: PROJECT_SPEC lists the agenda; PROJECT_SPEC/ARCHITECTURE_SPEC (and the displayCity/DeviceContext KDoc) now include the agenda's candle-lighting/havdalah times among the selected city's readers (owner decision 1).

Verification on 913584b (tree clean):
- 6 forced runs of `:app:verifyRoborazziDebug :app:testDebugUnitTest --rerun --tests '*AgendaScreenshotTest*' --tests '*AppShellScreenshotTest.contactBirthday*'`: all exit 0, each compared 32 images (28 agenda + 4 contact birthday), 0 changed. (Runs 1–2 were on 8ef6bd0; the later commit only changed specs/KDoc.)
- 2 clean full runs (`rm -rf app/build/test-results`, then `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`): both BUILD SUCCESSFUL; 1025 tests, 0 failures, 0 skipped.
- Emulator (fresh, API 36): origin/master installed, its walkthrough skipped; this build installed over it → only the agenda screen as a single "Got it" page; relaunch shows no walkthrough; Agenda works. Screenshots 19–21 in /tmp/levana-agenda/task8-emulator/ (README updated). Logs: /tmp/task8-stability/, /tmp/task8-flake/.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T09:05:00Z

---

Coordinator integration 2026-10-30: first integration attempt caught a non-deterministic missing month header; agent found the root cause (header items added/removed during a pending scroll) and made headers part of their day's item, plus fixed a contacts-permission race (8ef6bd0) and added the 'agenda' walkthrough screen. Merged as 177653a after TASK-43. Two consecutive clean runs: 1025 tests, 0 failures; Roborazzi 256 unchanged. Evidence: /tmp/levana-agenda/task8-emulator/ (README, 21 shots).

---
<!-- COMMENTS:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-bpd",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-17",
  "metadata": {
    "import_key": "original-17",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #17 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```

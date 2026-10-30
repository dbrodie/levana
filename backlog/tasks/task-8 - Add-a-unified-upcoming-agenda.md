---
id: TASK-8
title: Add an Agenda view and Month/Agenda drawer navigation (design 1a/1b)
status: To Do
assignee: []
created_date: '2026-10-21T11:19:31Z'
updated_date: '2026-10-30T06:39:00Z'
labels:
  - curated-import
  - source-specs
dependencies: []
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
- **ACTIVE CITY card:** shows the selected city and expands into the city list (existing behaviour). New: each city row shows that city's **current local time** on the right (e.g. Jerusalem 14:12, New York 08:12, London 13:12), with a check mark on the selected one.
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
- The app remembers whether the user last used Month or Agenda (decide in the plan whether it becomes the start screen).
- MVI; occurrence aggregation testable outside Compose; no duplication of the day panel's or the notifications' occurrence code. Extract shared logic instead.

**Open product questions** (owner to confirm before implementation):
1. **Location for agenda times** (candle lighting, havdalah): the **selected city**, like the day panel and Zmanim screen, or the **device position**? Per TASK-5 the selected city currently drives only the Zmanim screen and day-panel times. The agenda is a calendar view, so the selected city is the likely answer, with the TASK-5 zone label when zones differ.
2. **Search:** what does it search (event titles, holidays, parashot), and over what range?
3. **Title dropdown:** a month/date jump, using the shared dual-calendar picker from TASK-45?
4. **Drawer city times:** confirm that showing each city's current local time is wanted. It needs to refresh each minute while the drawer is open.
5. **Start screen:** should the last-used view (Month or Agenda) become the app's start screen?
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Agenda screen per design 1a: date rail (weekday, Hebrew day, Gregorian date, today highlight), typed cards (candle lighting, Shabbat/parasha + havdalah, personal and device events with location and time, yahrzeits, birthdays, holidays and Rosh Chodesh), month headers and the Hebrew/Gregorian toggle; top bar with month jump, search and today
- [ ] #2 All sources are included with correct chronological order and Hebrew recurrence; device-calendar selection and permission are respected; no occurrence logic is duplicated
- [ ] #3 Drawer per design 1b: a Calendar section with Month and Agenda, My events, Cities, Settings; city rows show each city's current local time; selection behaviour is unchanged (display-only city)
- [ ] #4 Open product questions are answered by the owner and recorded before implementation
- [ ] #5 Unit tests for aggregation and ordering; emulator screenshots compared against the design in English/Hebrew, light/dark, a holiday theme; TalkBack labels; Roborazzi baselines added for the new screen and drawer
<!-- AC:END -->

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

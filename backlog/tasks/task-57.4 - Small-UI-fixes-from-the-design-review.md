---
id: TASK-57.4
title: Small UI fixes from the design review
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: chore
ordinal: 74000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A batch of small UI fixes. Decide each one (accept, adjust or reject with a reason), update Figma, then implement. Font-size findings (C1, D4, R7) live in the foundations task instead. Related: TASK-29 (delete icon labels).

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **N3** (High) — Calendar top bar (11:9) vs My events (23:876) vs Agenda (27:1100). Add affordance inconsistent: Month has a **+** in the top bar, My events has a **FAB**, Agenda has none. The calendar_today icon in the top bar is unlabelled and its function (Today? Go to date?) is ambiguous given the title also has a dropdown that opens "Go to date". *Why:* Primary actions should be predictable. *Reviewer suggestion:* Use one pattern: FAB "Add event" on Month/Agenda/My events; top bar keeps only "Today" (icon with the day number, like Google Calendar) and search. Title dropdown = go to date.
- **N4** (Medium) — Settings (47:3015) vs My events (23:876). Settings is a drawer destination but uses a **back arrow**; My events (also a drawer destination) uses the **hamburger**. *Why:* Users lose the drawer on Settings; nav model is muddled. *Reviewer suggestion:* Drawer destinations get the hamburger; pushed screens (Zmanim, Halachic times, Cities, About) get back.
- **N7** (Medium) — Drawer (28:1501). Group label "Calendar" exists but "My events" and "Settings" float ungrouped; "Manage cities" is an action styled like a destination. Drawer has no today's-date line or app icon. *Why:* Minor hierarchy confusion. *Reviewer suggestion:* Groups: "Calendar" (Month, Agenda, Zmanim), "Personal" (My events), footer (Settings, About). Make "Manage cities" a text button inside the city card. Show today's Hebrew date under "Levana".
- **N8** (Medium) — Whole file. **Zmanim is not reachable from the drawer**; only via Month → sheet → "More halachic times". The spec lists zmanim as a core pillar. *Why:* Three taps to the signature feature. *Reviewer suggestion:* Add "Zmanim" to the drawer (and consider a bottom navigation bar: Month · Agenda · Zmanim · Events — four top-level destinations is exactly M3's sweet spot and would replace the drawer).
- **C3** (High) — All month frames. "Candles" chip on every Friday is repeated 4–5× per month and says nothing useful; the "Personal" event chip shows the generic word "Persona…" (HE: "אירוע אי…") instead of the event's name. *Why:* Noise where signal belongs; truncated generic labels waste the only chip slot. *Reviewer suggestion:* Replace the Friday chip with the candle-lighting *time* (e.g. "🕯 4:33") or a small candle glyph; show the event title ("Grandpa Moshe") with a kind-coloured bar.
- **C6** (Medium) — Day cell states (7:46). Selected (primaryContainer #D6E1E8), Saturday (surfaceContainerLow #F3F1ED) and Today (outline only) are visually very close; a *selected Saturday that is not today* and a *today that is not selected* differ by a 1 dp stroke. *Why:* Users scan for "today" first; it must be the strongest cell signal. *Reviewer suggestion:* Today = filled primary circle around the day number (M3 convention); Selected = container fill; Saturday/Yom Tov = warm tint + bold weekday header.
- **C7** (Medium) — Month frames. Shabbat/Yom Tov cells don't show candle-lighting/havdalah times, and Yom Tov days (Oct 3 Shemini Atzeret) aren't distinguished from Shabbat (same column tint). *Why:* Experts expect Yom Tov emphasis and times at a glance. *Reviewer suggestion:* Yom Tov cell tint (tertiary) distinct from Shabbat; optional "Show times in grid" setting.
- **C9** (Low) — Month, 6-row months. Only 5-row months are shown; a 6-row month (e.g. a 31-day month starting Saturday) with the collapsed sheet will squeeze rows to ~50 dp. *Why:* Layout risk at the extremes. *Reviewer suggestion:* Add a 6-row frame; define minimum row height and sheet peek height.
- **Z7** (Medium) — Day sheet "My events" row; Agenda card. Yahrzeit row shows name, "Yahrzeit", notes — but **not which yahrzeit (year count)** or the deceased's Hebrew name. *Why:* "3rd yahrzeit" matters for Kaddish customs and family planning. *Reviewer suggestion:* "Grandpa Moshe · 3rd yahrzeit · כ׳ חשון".
- **A3** (Medium) — Agenda, search results (27:1366). "Yahrzeit — Grandpa Moshe" (em dash) vs sheet "Grandpa Moshe / Yahrzeit" — different composition for the same item. *Why:* Consistency. *Reviewer suggestion:* One Event row component: title = name, overline = kind, trailing = time.
- **A4** (Medium) — Agenda. No empty state ("Nothing in the next 12 months"), no "no results" search state, no end-of-range affordance ("Show earlier"). *Why:* Missing states ship as blank screens. *Reviewer suggestion:* Design the three states.
- **A5** (Low) — Agenda device event. "Team meeting / Office / 10:00 AM" — no end time, no calendar name/colour legend. *Why:* Context. *Reviewer suggestion:* "10:00–11:00 · Work".
- **A6** (Low) — Agenda two "Rosh Chodesh Kislev" cards. Identical cards on 10 and 11 Nov. *Why:* Users may think it's a duplicate bug. *Reviewer suggestion:* "Rosh Chodesh Kislev (1 of 2)" / "(2 of 2)" or "30 Cheshvan · Rosh Chodesh".
- **E2** (High) — List cards (20:190). A red **delete** icon sits on every card, one tap from destruction; no confirmation/undo state is designed. *Why:* Accidental loss of irreplaceable data; also the trash is the only visible action, so edit is undiscoverable. *Reviewer suggestion:* Tap card = edit; delete inside the edit screen or via swipe with Undo snackbar. Design the confirmation.
- **E5** (Medium) — Forms. Helper line "Choose the Hebrew date for this event." sits at the top of every form, above the Title field, like an instruction banner. *Why:* Odd hierarchy, repeated. *Reviewer suggestion:* Remove; the date card's own label suffices.
- **E6** (Medium) — Add event. Primary action is a full-width "Save" at the end of the form; M3 forms typically use a top-bar "Save" action or a bottom bar. Disabled state is grey-on-grey (12%/38%) with no hint why. *Why:* Discoverability on long forms; disabled without reason. *Reviewer suggestion:* Keep in top app bar as a text/filled button; show validation hint under Title.
- **S5** (Medium) — Settings group cards. Elevated cards with uppercase headers and 1 dp shadows; M3 settings use flat lists with headers. Group "HALACHIC TIMES" contains one row; "DEVELOPER" and "ABOUT" contain one row each. *Why:* Visual weight > content; single-row groups are headers for nothing. *Reviewer suggestion:* Flat list; merge Halachic times into Calendar preferences; About/Developer as plain rows at the end.
- **S6** (Low) — About (47:3314). "Version 0.4.2-debug", "Install Source: None" — debug strings; no update check, licence link or privacy statement (spec mentions GitHub update checks, MIT, crash-report email). *Why:* Polish and transparency. *Reviewer suggestion:* "Version 0.4.2", "Check for updates", "Privacy", "Open-source licences", "Send feedback".
- **S7** (Low) — Israel/Diaspora dialog (49:3379). Auto-detect option lacks the explanation given in the row subtitle; Theme dialog has one. *Why:* Inconsistent dialog pattern. *Reviewer suggestion:* Add supporting text "Israel when the device time zone is Israel's".
- **S8** (Nit) — Settings row "Israel / Diaspora" subtitle. "Affects second day Yom Tov and parsha readings" — good; also affects Shavuot length and Purim/Shushan Purim not at all; fine, but "Yom Tov Sheni" wording is the term experts use. *Why:* Precision. *Reviewer suggestion:* "Affects Yom Tov Sheni and parasha schedule".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each listed finding is fixed or explicitly rejected with a reason
- [ ] #2 Figma updated to match
<!-- AC:END -->

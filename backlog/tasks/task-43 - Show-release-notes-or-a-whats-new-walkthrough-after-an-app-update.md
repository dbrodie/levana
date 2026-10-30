---
id: TASK-43
title: 'Feature walkthrough: per-feature screens shown once, growing with each release'
status: To Do
assignee: []
created_date: '2026-10-29T20:55:00Z'
updated_date: '2026-10-30T07:19:00Z'
labels: []
dependencies: []
priority: medium
type: feature
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner vision, 2026-10-30. This replaces the earlier per-version release-notes idea.

**Product model: a feature walkthrough that grows with the app**
- Every **major feature** has one **walkthrough screen**. Each screen has a permanent, stable ID that is never reused or renamed.
- The app remembers which screen IDs the user has **seen**. A seen screen is never shown again.
- **New install:** the first launch shows the walkthrough with all current feature screens, in their defined order.
- **Existing user after an update:** if the update adds feature screens the user hasn't seen, they are shown on the next launch, and only those. A long-time user who gets a new feature sees just that feature's screen; a new user sees it as one of the walkthrough's screens.
- **Adding a feature to the walkthrough later** is just adding a screen (ID, order position, illustration, title, body, optional action) to one registry. No version bookkeeping.
- Bug fixes and small changes get no screens.

**Design source:** Claude Design "Jewish Calendar App Design", Walkthrough, https://claude.ai/artifact/6QPAibYPQzQJhZhEXDZ87H (owner, 2026-10-30). Captured by the coordinator through the Paseo browser:
- `/tmp/levana-walkthrough/01-welcome-and-city-top.png`
- `/tmp/levana-walkthrough/02-welcome-and-city-text.png`

The bottom of each frame (page indicator and next/done controls) couldn't be scrolled into view; get it from the owner or the live link before building.

**Screen anatomy (from the design)**
- Full-screen page with **Skip** at the top end.
- A large rounded illustration card showing a realistic mini-UI of the feature (not a static image).
- A large title and a one- or two-line body below.
- Next/Done controls and a page indicator are presumably at the bottom (to confirm).
- **1a Welcome:** "Your Hebrew calendar" / "Hebrew dates, holidays and zmanim, side by side with the Gregorian calendar." The illustration is a two-week Hebrew month strip (חשוון ה׳תשפ״ז, Oct – Nov 2026) with event chips (candles, parasha, yahrzeit), today highlighted, and NETZ / SHKIA / CANDLES tiles.
- **1b Choose a city:** "Zmanim for where you are" / "Use your location, or add the cities you care about. Switch between them from the menu." The illustration shows a CURRENT LOCATION card (Jerusalem, Israel, ticked), a city list (Tel Aviv-Yafo, Israel · GMT+2 · 14:12; New York, United States · GMT−4 · 08:12; London, United Kingdom · GMT+0 · 12:12) and an **Add city** button. This screen can be **interactive**: it can request location permission or add a city in place.

**Requirements**
- Registry of screens: ID, order, minimum condition (e.g. hidden if the feature is unavailable on the device), content (illustration composable, title and body strings in English and Hebrew) and an optional action (e.g. grant location / notifications / contacts / calendar permission, or add a city).
- Seen IDs persist in DataStore and survive upgrades. A screen counts as seen once it has been displayed. **Skip** marks all screens in the current run as seen.
- When to show it:
  - on launch, before the main UI, if there are unseen eligible screens;
  - never on top of a deep link (notification or widget tap); defer to the next normal launch;
  - never in screenshot or instrumentation tests unless explicitly requested.
- "Show walkthrough again" in About (or Settings) replays all screens without changing seen state.
- Developer settings can reset the seen state for testing.
- Works offline; Material 3 theme (light/dark, Material You, holiday colours); RTL in Hebrew; TalkBack (each page is announced as "page n of m" with its title and body; the controls are labelled); respects reduced motion.
- Documented in ARCHITECTURE_SPEC: how to add a screen when a new major feature ships. Feature tasks that introduce a major feature should add their screen.

**Open product decisions**
1. **Existing users the first time the walkthrough ships:** show them every screen (they've never seen any), or mark the screens for features that existed before as already seen and show only genuinely new ones? (Recommendation: show only a short "What's new" subset, i.e. pre-mark the baseline screens as seen for upgraders.)
2. **Permission actions inside the walkthrough** (location on the city screen, notifications on the reminders screen): ask in-flow, or only describe and leave asking to the existing in-app prompts?
3. **The final list of feature screens** (draft below).
4. The missing bottom controls of the design (page dots? Next / Get started?).

**Draft feature-screen list (for owner review)**
1. `welcome-hebrew-calendar`: Your Hebrew calendar (design 1a). Hebrew and Gregorian side by side, holidays, zmanim at a glance.
2. `zmanim-location`: Zmanim for where you are (design 1b). Current location or saved cities, switched from the menu; optional location permission and add-city.
3. `day-details`: Tap any day for details. Day panel with zmanim, parasha, Omer and events; choose which zmanim to show.
4. `shabbat-holidays`: Shabbat and holidays. Candle lighting and havdalah, Yom Tov, fasts, Israel/Diaspora auto-detect.
5. `personal-events`: Birthdays, yahrzeits and events. Recurring Hebrew dates with correct leap-year and yahrzeit rules; Hebrew birthdays from Contacts.
6. `reminders`: Never miss candle lighting. Reminders for candle lighting, holidays, fasts, events and the Omer; optional notification permission.
7. `device-calendars`: Your calendars together. Selected phone calendars alongside Jewish dates; optional calendar permission.
8. `widgets`: Levana on your home screen. Today, Month and Zmanim widgets, the Quick Settings tile and the Hebrew-date app icon.
9. `appearance`: Make it yours. Light/dark, Material You, holiday colours, Hebrew or English.
- Future screens are added as features ship, e.g. `agenda` with TASK-8 and `date-picker` with TASK-45 if it counts as major.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A registry of feature screens with stable IDs; seen IDs persist; new installs see all eligible screens in order; after an update, existing users see only unseen screens; Skip marks the current run as seen
- [ ] #2 Screens follow the walkthrough design (illustration card, title, body, Skip, bottom controls) in English/Hebrew RTL, light/dark, Material You and holiday themes, with TalkBack page announcements
- [ ] #3 Never interrupts deep links or tests; replay from About; developer reset of seen state
- [ ] #4 Owner-approved screen list and the upgrade-baseline and permission decisions are recorded before implementation; ARCHITECTURE_SPEC documents how to add a screen
- [ ] #5 Unit tests for the seen/unseen selection (fresh install, upgrade with new screens, skip, replay); Roborazzi baselines for each screen; emulator screenshots of a fresh install and of an upgrade showing only a new screen
<!-- AC:END -->

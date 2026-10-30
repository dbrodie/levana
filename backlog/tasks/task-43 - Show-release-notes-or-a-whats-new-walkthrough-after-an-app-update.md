---
id: TASK-43
title: 'Feature walkthrough: per-feature screens shown once, growing with each release'
status: To Do
assignee: []
created_date: '2026-10-29T20:55:00Z'
updated_date: '2026-10-30T07:29:00Z'
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
- `/tmp/levana-walkthrough/03-remember-every-date-bottom-controls.png` (third screen and the bottom controls)

To see the whole page in the Paseo browser, scroll the page background, not a phone frame: the frames capture the wheel.

**Screen anatomy (from the design)**
- Full-screen page with **Skip** at the top end.
- A large rounded illustration card showing a realistic mini-UI of the feature (not a static image).
- A large title and a one- or two-line body below.
- **Bottom controls:**
  - centred page dots, with the current page drawn as a wider dark pill;
  - a text **Back** button (start side), which is not on the first page;
  - a filled primary button (end side): **Next** on intermediate pages, presumably, and **Get started** on the last page.
- **1a Welcome:** "Your Hebrew calendar" / "Hebrew dates, holidays and zmanim, side by side with the Gregorian calendar." The illustration is a two-week Hebrew month strip (חשוון ה׳תשפ״ז, Oct – Nov 2026) with event chips (candles, parasha, yahrzeit), today highlighted, and NETZ / SHKIA / CANDLES tiles.
- **1b Choose a city:** "Zmanim for where you are" / "Use your location, or add the cities you care about. Switch between them from the menu." The illustration shows a CURRENT LOCATION card (Jerusalem, Israel, ticked), a city list (Tel Aviv-Yafo, Israel · GMT+2 · 14:12; New York, United States · GMT−4 · 08:12; London, United Kingdom · GMT+0 · 12:12) and an **Add city** button. This screen can be **interactive**: it can request location permission or add a city in place.
- **1c Remember every date:** "Remember every date" / "Add birthdays, yahrzeits and anniversaries on their Hebrew date. Levana reminds you each year." The illustration shows event cards (e.g. "Avi & Noa — anniversary · כ״א כסלו · every year", with a heart icon and a bell for its reminder) and a + button to add one. It maps to the `personal-events` screen. The design shows 3 pages; the approved list has 8, so the dots scale with the count.
- **Page indicator rules** (owner, 2026-10-30): the count is the number of screens **in the current run**, not the registry total.
  - **1 screen** (typical after an update): no dots, no Back/Next; a single **Got it** button. Skip is redundant and can be hidden.
  - **2–9 screens:** dots, as designed.
  - **10 or more:** replace the dots with a text counter, e.g. "3 of 12" ("3 מתוך 12" in Hebrew).
  - TalkBack always announces "page n of m" from the same count.

**Requirements**
- Registry of screens: ID, order, minimum condition (e.g. hidden if the feature is unavailable on the device), content (illustration composable, title and body strings in English and Hebrew) and an optional action (permission request or add city, per the owner decisions below).
- Seen IDs persist in DataStore and survive upgrades. A screen counts as seen once it has been displayed. **Skip** marks all screens in the current run as seen.
- When to show it:
  - on launch, before the main UI, if there are unseen eligible screens;
  - never on top of a deep link (notification or widget tap); defer to the next normal launch;
  - never in screenshot or instrumentation tests unless explicitly requested.
- "Show walkthrough again" in About (or Settings) replays all screens without changing seen state.
- Developer settings can reset the seen state for testing.
- Works offline; Material 3 theme (light/dark, Material You, holiday colours); RTL in Hebrew; TalkBack (each page is announced as "page n of m" with its title and body; the controls are labelled); respects reduced motion.
- Documented in ARCHITECTURE_SPEC: how to add a screen when a new major feature ships. Feature tasks that introduce a major feature should add their screen.

**Owner decisions (2026-10-30)**
1. **Existing users see all screens** the first time the walkthrough ships. They are test users, so there is no baseline pre-marking. Afterwards, the normal seen/unseen rule applies.
2. **Permissions are requested inside the walkthrough**, each on its feature's screen, after a short explanation and with a "Not now" option that never blocks progress:
   - **Location** on the zmanim/location screen.
   - **Notifications** on the reminders screen.
   - **Contacts** on the birthdays screen. Explain that Hebrew birthdays are saved on the contact itself (a custom field) and that **nothing leaves the device**.
   - **Calendar** (read-only) on the device-calendars screen. Make the same "nothing leaves the device" point.
   If a permission is already granted, show the granted state instead of a button. A denial must leave the app fully usable, and the existing in-app prompts still work later.
3. **Screens 3 and 4 of the draft are merged** into one screen. Final list below.

**Feature-screen list (owner-approved, 8 screens, in order)**
1. `welcome-hebrew-calendar`: **Your Hebrew calendar** (design 1a). Hebrew and Gregorian side by side, holidays, zmanim at a glance.
2. `zmanim-location`: **Zmanim for where you are** (design 1b). Current location or saved cities, switched from the menu. Location permission request plus Add city.
3. `days-shabbat-holidays`: **Every day, Shabbat and holiday.** Tap a day for zmanim, parasha, Omer and events, and choose which zmanim to show; candle lighting and havdalah, Yom Tov, fasts, Israel/Diaspora auto-detect.
4. `personal-events`: **Remember every date** (design 1c). Recurring Hebrew dates with correct leap-year and yahrzeit rules; Hebrew birthdays saved on your contacts. Contacts permission request, explaining storage on the contact and that nothing leaves the device.
5. `reminders`: **Never miss candle lighting.** Reminders for candle lighting, holidays, fasts, events and the Omer. Notification permission request.
6. `device-calendars`: **Your calendars together.** Selected phone calendars alongside Jewish dates. Calendar permission request, read-only, nothing leaves the device.
7. `widgets`: **Levana on your home screen.** Today, Month and Zmanim widgets, the Quick Settings tile and the Hebrew-date app icon.
8. `appearance`: **Make it yours.** Light/dark, Material You, holiday colours, Hebrew or English.
- Future screens are added as features ship, e.g. `agenda` with TASK-8.

<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A registry of feature screens with stable IDs; seen IDs persist; new installs and (this first time) existing users see all eligible screens in order; after later updates users see only unseen screens; Skip marks the current run as seen
- [ ] #2 The 8 owner-approved screens follow the walkthrough design (illustration card, title, body, Skip, bottom controls) in English/Hebrew RTL, light/dark, Material You and holiday themes, with TalkBack page announcements
- [ ] #3 Location, notification, contacts and calendar permissions are requested on their screens with short explanations (contacts: saved on the contact, nothing leaves the device); Not now never blocks; granted state shown; denial leaves the app usable
- [ ] #4 Never interrupts deep links or tests; replay from About; developer reset of seen state; ARCHITECTURE_SPEC documents how to add a screen
- [ ] #5 Unit tests for the seen/unseen selection (fresh install, upgrade with a new screen, skip, replay); Roborazzi baselines for each screen; emulator screenshots of a fresh install including each permission step (grant and deny), and of an upgrade showing only a new screen
- [ ] #6 The page indicator follows the run's screen count: a single screen shows only Got it (no dots, no Back/Next); 2–9 show dots; 10 or more show an 'n of m' counter; tests and Roborazzi images cover runs of 1, 3 and 12 screens
<!-- AC:END -->

# Levana — Design Review (Figma file `E1IFrxKcldXbXnvXy5Nzhd`)

Reviewed 2026-11-01, read-only, from the Figma file and `specs/PROJECT_SPEC.md` only. No app source was read. Sample data in the file: Shabbat Vayera, 31 Oct 2026 / 20 Cheshvan 5787, Jerusalem, 412×915 dp.

Severity scale: **Critical** (blocks core task or is halachically misleading) · **High** (expert users will notice and be hurt) · **Medium** (clear quality/consistency debt) · **Low** · **Nit**.

---

## 1. What the designs say the app is

**Product.** An offline Hebrew/Gregorian calendar for Orthodox users with location-based zmanim, Shabbat/Yom Tov times, Torah readings, personal Hebrew-date events (yahrzeits, birthdays via Contacts, anniversaries), read-only device-calendar overlay, reminders, widgets, a Quick Settings tile and a dynamic launcher icon.

**Information architecture (from the drawer).**
- Drawer: *Active city* card (expandable list of saved cities + current location), *Manage cities*; group **Calendar** → *Month* (start screen), *Agenda*; then *My events*, *Settings*.
- **Month** (Gregorian- or Hebrew-primary via a segmented control) with a persistent bottom day sheet (collapsed: category chip + three quick zman tiles; expanded: Zmanim card → "More halachic times", Parsha, Havdalah/Candle lighting, My events).
- **Zmanim** (full list grouped Morning / Afternoon / Evening) reached only from the day sheet.
- **Agenda**: month-sectioned chronological list with a date rail, search (±12 months), calendar-permission banner, Hebrew/Gregorian segmented control.
- **Date picker** dialog, Hebrew-led or Gregorian-led, with a year grid.
- **My events**: tabs *Birthdays* (contacts) / *Events*; Add/Edit event, Add birthday; overflow Export JSON/CSV, Import.
- **Settings**: Calendar (Location, System calendars) · Calendar preferences (Israel/Diaspora, Modern Israeli holidays, Candle lighting offset) · Yahrzeit settings (4 rules) · Appearance (language, theme, wallpaper colours, holiday theming) · Halachic times (which zmanim appear in the day panel) · Notifications (5 toggles) · Developer · About.
- **Onboarding**: 9-page walkthrough, 2 built, 7 spec cards.
- **Widgets**: Today (Full/Compact), Month 4×4, Zmanim 3×3, QS tile, launcher icon.

**Overall impression.** The file is unusually complete as a *mirror* of a shipped app: light/dark/RTL coverage, a tokenised colour system with holiday accents, a type scale and ~70 components. Visual language is calm, low-chroma slate-and-sand, M3-ish. The weaknesses are not polish; they are (a) naming/terminology chaos across screens, (b) context loss on the two most important screens (day sheet and Zmanim have no date or place), (c) halachic depth an Orthodox power user expects (shitot, Havdalah offset, after-sunset handling, Rosh Chodesh in the grid), and (d) a day-cell micro type size that fails accessibility.

---

## 2. Findings by area

### 2.1 Cross-cutting: terminology, naming, transliteration

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| X1 | **High** | Day sheet (21:660), Zmanim (13:236), Settings (47:2671), Halachic times (47:3078) | The same concept is called **Zmanim** (screen title, sheet card title), **Halachic times** (settings group + screen), **More halachic times** (link) and **halachic times** (settings row). | Users build a mental model from labels; three names for one thing makes "where do I set which times show?" a hunt. | Pick one user-facing term per language ("Zmanim" in both EN and HE; Hebrew: "זמני היום") and use it for screen title, card title, link and settings row. Keep "halachic" only in a body-text explanation. |
| X2 | **High** | Day sheet quick tiles vs. Zmanim card vs. Zmanim screen | The three quick tiles are labelled **NETZ / SHKIA / TZEIT**; the card right below them labels the *same* times **Sunrise / Sunset / Nightfall** with Hebrew sublabels; the Zmanim screen uses **Alot HaShachar, Sof Zman Shema…** (transliterated). Three naming systems on one screen. | A beginner can't connect "TZEIT 5:27" with "Nightfall 5:27"; an expert finds it sloppy. | Choose a primary and a secondary language per UI language and apply it uniformly: EN UI → transliteration primary + Hebrew secondary everywhere (so tiles become "Netz", list rows "Netz HaChama / הנץ החמה"), or English primary everywhere. Do not mix within a screen. |
| X3 | **High** | Month grid chips (11:9, 21:307), Agenda (27:1431), Search (27:1346) | Parsha spelled four ways: **Parsha** (sheet card), **Parashat** (agenda), **parashot** (search placeholder), and parsha *names* switch pronunciation systems: **Bereshis**, **Toldos** (Ashkenazi *sav*) next to **Vayera**, **Chayei Sara**, **Noach**, and the app elsewhere says **Shabbat**, **Chatzot**, **Tefillah** (Sephardi/Israeli *tav*). | Transliteration scheme is a trust signal for this audience. Mixed *s/t* reads as auto-generated. | Decide one scheme (Israeli/Sephardi "t" matches "Shabbat", "Sukkot", "Chatzot" already in use): Bereshit, Toldot, Chayei Sarah. Standardise on "Parashat X" (agenda, sheet card title "Parashat Vayera") and "parashot" only as a plural noun. Consider a user setting (Ashkenazi/Sephardi transliteration) later. |
| X4 | Medium | Settings Yahrzeit rows (47:2671) | "Also mark 1 **Nissan**" — the month is conventionally "Nisan" in English; "Nissan" is the car brand. | Credibility. | "1 Nisan". Audit all month names (Cheshvan, Kislev, Tevet, Shevat, Adar I/II, Nisan, Iyar, Sivan, Tammuz, Av, Elul). |
| X5 | Medium | Settings, Agenda, Halachic times | Casing inconsistency: "Candle Lighting" (settings notification row, Halachic times list) vs "Candle lighting" (agenda card, settings row "Candle lighting offset"); "Halachic Times" vs "Halachic times". | M3 uses sentence case; inconsistency looks unedited. | Sentence case everywhere. |
| X6 | Medium | Drawer (28:1501), Settings → Location, Cities (29:2593), Cities settings (47:3269), Onboarding p2 | Location concept named **Active city**, **Manage cities**, **Location**, **Cities**, **Locations**, **Add location**, **Add city**, **Current location**. | Seven labels for one subsystem. | Settings row "Cities" → screen "Cities" with sections "Current location" / "Saved cities" / "+ Add city"; drawer "Selected city" (matches the spec's "selected city is a display lens") and "Manage cities". |
| X7 | Medium | RTL Agenda (44:2115), RTL My events (44:2528) | Hebrew uses **יארצייט** (Yiddish loanword). Israeli Hebrew speakers say **יום השנה** / **אזכרה**; Yiddish spelling also varies (יארצייט/יאָרצײַט). | Target users include Israelis; the Yiddish term reads as a diaspora-only app. | Use "יום השנה" (or "יום זיכרון" with care — collides with Yom HaZikaron). Keep "Yahrzeit" in English UI. |
| X8 | Low | Add/Edit event forms | Toggle label "Use yahrzeit Adar rules" with a long technical description ("Adar dates follow Settings → Yahrzeit settings. Adar I and II remain separate.") | Jargon on a form; also shown for non-Adar dates. | Replace with an event *type* selector (see E1). If kept, show only when the chosen date is in Adar, and reword: "Treat as yahrzeit (leap-year Adar rules apply)". |

### 2.2 Information architecture & navigation

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| N1 | **Critical** | Calendar / Day sheet — expanded (21:660, 37:211, 44:657) | The expanded sheet hides the month grid and **shows no date at all**. The top bar still says "October 2026"; the sheet shows only a "Shabbat" chip. Which day's times are these? | The day sheet is the app's primary content surface. A screenshot, a glance after returning to the app, or TalkBack gives no date. | Add a sheet header: "Shabbat, 31 October · כ׳ חשון תשפ״ז" (weekday, Gregorian, Hebrew), plus the active city and "Jerusalem time" when it differs (spec). Add ‹ › day stepping or horizontal swipe in the sheet. |
| N2 | **High** | Zmanim — Day list (13:236, 37:470, 44:1931) | Screen title is only "Zmanim": no date, no city, no way to move to another day. | Experts open a zmanim screen to compare days (erev Shabbat, Yom Kippur). Here it's a dead end for one unlabelled day. | Title "Zmanim" with subtitle "Sat 31 Oct · 20 Cheshvan · Jerusalem"; add prev/next day arrows and a date-picker action; consider a "Share / copy today's zmanim" action. |
| N3 | **High** | Calendar top bar (11:9) vs My events (23:876) vs Agenda (27:1100) | Add affordance inconsistent: Month has a **+** in the top bar, My events has a **FAB**, Agenda has none. The calendar_today icon in the top bar is unlabelled and its function (Today? Go to date?) is ambiguous given the title also has a dropdown that opens "Go to date". | Primary actions should be predictable. | Use one pattern: FAB "Add event" on Month/Agenda/My events; top bar keeps only "Today" (icon with the day number, like Google Calendar) and search. Title dropdown = go to date. |
| N4 | Medium | Settings (47:3015) vs My events (23:876) | Settings is a drawer destination but uses a **back arrow**; My events (also a drawer destination) uses the **hamburger**. | Users lose the drawer on Settings; nav model is muddled. | Drawer destinations get the hamburger; pushed screens (Zmanim, Halachic times, Cities, About) get back. |
| N5 | Medium | Month & Agenda headers | The **Hebrew / Gregorian** segmented control is permanently visible on both screens and consumes a 56 dp row above the grid. In Agenda it sits above a list that already prints both calendars per row and per month header. | It is a sticky preference, not a per-session toggle; the row costs a week of grid height. | Move to the title dropdown or an overflow item; or make the title tap-toggle ("October 2026 · Tishrei–Cheshvan 5787" ⇄). On Agenda remove it entirely. |
| N6 | Medium | Agenda (27:1431) | Top bar "November 2026 ▾ / Cheshvan–Kislev 5787" duplicates the first list section header "November 2026 Cheshvan–Kislev 5787" directly below. | Redundant 90 dp. | Pin the section header as the scrolling title (collapsing top bar) and drop the subtitle row. |
| N7 | Medium | Drawer (28:1501) | Group label "Calendar" exists but "My events" and "Settings" float ungrouped; "Manage cities" is an action styled like a destination. Drawer has no today's-date line or app icon. | Minor hierarchy confusion. | Groups: "Calendar" (Month, Agenda, Zmanim), "Personal" (My events), footer (Settings, About). Make "Manage cities" a text button inside the city card. Show today's Hebrew date under "Levana". |
| N8 | Medium | Whole file | **Zmanim is not reachable from the drawer**; only via Month → sheet → "More halachic times". The spec lists zmanim as a core pillar. | Three taps to the signature feature. | Add "Zmanim" to the drawer (and consider a bottom navigation bar: Month · Agenda · Zmanim · Events — four top-level destinations is exactly M3's sweet spot and would replace the drawer). |
| N9 | Low | Settings "Developer" group visible in production Settings (47:2671) | Date override, force holiday theme, test crash are user-visible. | Confusing and risky for end users. | Hide behind 7 taps on Version in About. |

### 2.3 Calendar — Month view (01)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| C1 | **Critical** | Day cell component (7:46) and all month frames; type token `app/cell-micro` 9sp | The secondary date in each cell (Hebrew in Gregorian mode, Gregorian in Hebrew mode) is **9 sp**, and Hebrew numerals with geresh/gershayim (כ״ו, י״ט) at 9 sp are not legible; chip labels are 10–11 sp. | Fails the spec's own "scalable text, adequate contrast" requirement and WCAG target sizes; the Hebrew date is half the point of the app. | Minimum 11 sp (labelSmall) for the secondary date, 12 sp for chips; consider a two-line cell (Gregorian top-left, Hebrew below) and reduce chip count (see C3). Test at 1.3× font scale. |
| C2 | **High** | Month — Gregorian (11:9), Hebrew mode (21:307) | **Rosh Chodesh is not marked in the grid.** 30 Tishrei (11 Oct) and 1 Cheshvan (12 Oct) have no chip; in Hebrew mode ל׳ Cheshvan (10 Nov) has none — yet the Agenda shows "Rosh Chodesh Kislev" cards. | Rosh Chodesh changes davening (Hallel, Yaaleh v'Yavo, Musaf); its absence from the grid is a halachic completeness gap and an internal inconsistency. | Add a Rosh Chodesh chip (tertiary tone) and a Shabbat Mevarchim marker; surface molad in the day sheet on Shabbat Mevarchim (spec promises it; no frame shows it). |
| C3 | **High** | All month frames | "Candles" chip on every Friday is repeated 4–5× per month and says nothing useful; the "Personal" event chip shows the generic word "Persona…" (HE: "אירוע אי…") instead of the event's name. | Noise where signal belongs; truncated generic labels waste the only chip slot. | Replace the Friday chip with the candle-lighting *time* (e.g. "🕯 4:33") or a small candle glyph; show the event title ("Grandpa Moshe") with a kind-coloured bar. |
| C4 | Medium | Month header, Hebrew mode (21:307) | Title "5787 חשון" mixes Arabic numerals with a Hebrew month name and reads awkwardly in an LTR bar; the date picker shows the same year as "ה׳תשפ״ז". | Inconsistent year representation. | EN UI: "Cheshvan 5787" (transliterated month). HE UI: "חשון תשפ״ז". Keep numerals consistent with the picker. |
| C5 | Medium | Weekday header, Hebrew mode, English UI (21:307) | Weekday labels switch to Hebrew ("יום א׳ … שבת") when the user is in English UI. | Mode (which calendar is primary) should not change UI language. | Keep weekday labels in the UI language; optionally "Sun · א׳". |
| C6 | Medium | Day cell states (7:46) | Selected (primaryContainer #D6E1E8), Saturday (surfaceContainerLow #F3F1ED) and Today (outline only) are visually very close; a *selected Saturday that is not today* and a *today that is not selected* differ by a 1 dp stroke. | Users scan for "today" first; it must be the strongest cell signal. | Today = filled primary circle around the day number (M3 convention); Selected = container fill; Saturday/Yom Tov = warm tint + bold weekday header. |
| C7 | Medium | Month frames | Shabbat/Yom Tov cells don't show candle-lighting/havdalah times, and Yom Tov days (Oct 3 Shemini Atzeret) aren't distinguished from Shabbat (same column tint). | Experts expect Yom Tov emphasis and times at a glance. | Yom Tov cell tint (tertiary) distinct from Shabbat; optional "Show times in grid" setting. |
| C8 | Low | Gregorian-led picker (28:1752) | Dots under 2 and 3 Oct but not 1 Oct (Chol HaMoed) — undocumented marker semantics. | Unexplained indicators. | Define: dot = holiday/Yom Tov; omit for Chol HaMoed or add a lighter dot; add a legend in the Foundations page. |
| C9 | Low | Month, 6-row months | Only 5-row months are shown; a 6-row month (e.g. a 31-day month starting Saturday) with the collapsed sheet will squeeze rows to ~50 dp. | Layout risk at the extremes. | Add a 6-row frame; define minimum row height and sheet peek height. |

### 2.4 Day sheet (01) & Zmanim (02)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| Z1 | **High** | Zmanim (13:236) | No indication of **calculation opinion** for any zman (Alot: 72 min? 16.1°? Misheyakir 11°/10.2°? Tzeit 8.5°/ 3 stars / 72?). Only Shema shows GRA/MGA. | For this audience the shita is the data. A bare "Alot HaShachar 4:41" is unusable for someone who holds 72 fixed minutes. | Secondary line per row: "16.1° · 72 min equiv." or settings-driven method label; an info icon opening a short explanation. Long-term: per-zman method setting. |
| Z2 | **High** | Zmanim (13:236) | **Not chronological**: Sof Zman Shema (GRA) 8:38 is listed before Sof Zman Shema (MGA) 8:02. | A zmanim list is read top-to-bottom as the day unfolds. | Sort by time; label "Sof Zman Shema (MGA) 8:02", "(GRA) 8:38", and likewise show Sof Zman Tefillah for both (currently only one, unlabelled). |
| Z3 | **High** | Day sheet (21:660), Settings | **Havdalah has no configurable offset** (only candle lighting does). Havdalah 5:27 equals Tzeit 5:27 (37 min after a 4:50 sunset). | Many users hold 42, 50, 60 or 72 min; the single implicit value is a halachic choice made for the user without disclosure. | Settings → "Havdalah: Tzeit (8.5°) / 42 min / 50 min / 72 min (Rabbeinu Tam)"; label the method in the Havdalah card. |
| Z4 | **High** | Settings → Candle lighting offset dialog (49:3592) | Slider with default 18 min and no presets; the sample city is **Jerusalem, whose minhag is 40 min**. The thumb sits at the far left, implying 18 is the minimum. | Wrong default for the most prominent sample location; sliders are a poor control for a discrete, minhag-driven value. | Segmented presets 18 / 20 / 30 / 40 + "Custom"; per-city default (Jerusalem 40, Haifa 30, elsewhere 18); show the range. |
| Z5 | Medium | Day sheet | Quick tiles (NETZ/SHKIA/TZEIT), the Zmanim card (Sunrise/Sunset/Nightfall) and the Havdalah card all show the **same three or four numbers**; 5:27 PM appears three times. | Redundancy costs a full screen of scrolling on the most used surface. | Make tiles the *user-chosen* three (they already are per the Settings row "3 shown in day panel") and drop the duplicate list; or drop the tiles and keep the list. Havdalah/Candle lighting belong in a single "Shabbat" card with Parsha. |
| Z6 | Medium | Day sheet quick tiles | The featured (dark) tile is TZEIT — unclear whether "featured" means *next upcoming* (at 9:47 the next would be Shkia) or *most important today*. | Ambiguous emphasis. | Define as "next zman" and relabel ("Next · Shkia 4:50 PM"), or remove emphasis. |
| Z7 | Medium | Day sheet "My events" row; Agenda card | Yahrzeit row shows name, "Yahrzeit", notes — but **not which yahrzeit (year count)** or the deceased's Hebrew name. | "3rd yahrzeit" matters for Kaddish customs and family planning. | "Grandpa Moshe · 3rd yahrzeit · כ׳ חשון". |
| Z8 | Medium | Day sheet | Shabbat day shows no "Shabbat Mevarchim / molad" info, no "Shabbat ends" vs "Tzeit" distinction, no Omer/Chanukah count surface designed anywhere. | Spec commits to Omer count and molad; nothing in the file shows them. | Add frames for Shabbat Mevarchim (molad line), Omer day (count card + nusach), Chanukah (candle count), fast day (start/end). |
| Z9 | Medium | Zmanim screen | Times in **12-hour AM/PM** across all frames (including Hebrew UI); the onboarding illustrations use 24-hour. | Israeli users read 24-hour; the inconsistency is visible in the file itself. | Follow the device 24-hour setting; make the onboarding illustration match. |
| Z10 | Low | Zmanim screen | Evening group cut off in every frame; Chatzot HaLaylah presence unverifiable. | Coverage. | Add a full-scroll frame like Settings has. |

### 2.5 Agenda (04)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| A1 | Medium | Agenda (27:1431) | Rosh Chodesh uses the **celebration (party-popper)** icon. | Semantics: Rosh Chodesh is a lunar event, not a party; the same icon presumably serves holidays. | A crescent/moon icon for Rosh Chodesh; keep celebration for Yom Tov/Purim. |
| A2 | Medium | Agenda | Shabbat card is "Parashat Chayei Sara / Havdalah 5:22 PM"; the Friday card is "Candle lighting 4:27 PM". Shabbat Mevarchim (7 Nov) isn't marked. | Mevarchim is a weekly-relevant flag. | Add "Mevarchim Kislev · Molad …" as a second line on that Shabbat. |
| A3 | Medium | Agenda, search results (27:1366) | "Yahrzeit — Grandpa Moshe" (em dash) vs sheet "Grandpa Moshe / Yahrzeit" — different composition for the same item. | Consistency. | One Event row component: title = name, overline = kind, trailing = time. |
| A4 | Medium | Agenda | No empty state ("Nothing in the next 12 months"), no "no results" search state, no end-of-range affordance ("Show earlier"). | Missing states ship as blank screens. | Design the three states. |
| A5 | Low | Agenda device event | "Team meeting / Office / 10:00 AM" — no end time, no calendar name/colour legend. | Context. | "10:00–11:00 · Work". |
| A6 | Low | Agenda two "Rosh Chodesh Kislev" cards | Identical cards on 10 and 11 Nov. | Users may think it's a duplicate bug. | "Rosh Chodesh Kislev (1 of 2)" / "(2 of 2)" or "30 Cheshvan · Rosh Chodesh". |

### 2.6 Date picker (03)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| D1 | **High** | Date picker Gregorian-led (28:1752) used from event form | No **"after sunset"** option when entering a Gregorian date for a yahrzeit/birthday. | A death or birth after shkia belongs to the next Hebrew day; without the toggle users will record the wrong yahrzeit. This is the classic Hebrew-calendar-app failure. | Add a "After sunset" switch in Gregorian-led mode; show the resulting Hebrew date live in the footer (already there) and a hint "Night of 21 Cheshvan". |
| D2 | Medium | Year grid (28:2312) | Shows 5780–5794 only; no indication of scroll range and no typed input. | Birth years in the 5690s–5750s are the common case for birthdays and yahrzeits. | Scrollable grid with decade jumps, plus M3 text-input mode (keyboard icon) accepting "5745" or "1985". |
| D3 | Medium | Date picker | No "Today" shortcut; no Yom Tov/Shabbat markers in Hebrew-led grid (Gregorian-led has dots). | Parity and speed. | Add "Today" text button; mirror markers. |
| D4 | Low | Picker cells | Gregorian sub-dates at 9–10 sp (`picker/*-sub` tokens). | Same legibility problem as C1. | 11 sp minimum. |

### 2.7 My events & forms (05)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| E1 | **High** | Add event (24:971), Edit event (24:1076), Events list (23:920) | There is **no event type**. A yahrzeit — the most important personal date in this app — is "an event with the Adar-rules switch on". Anniversaries and yahrzeits share a generic calendar icon in the list; only the "Yahrzeit rules" caption differs. | Type drives icon, colour, recurrence rule, reminder text ("Light a candle", Kaddish), first-year logic and the "nth yahrzeit" count. | Type chips at the top of the form: Yahrzeit · Birthday · Anniversary · Other. Yahrzeit adds "Hebrew name", "Relationship", "Year of passing" (auto first-yahrzeit rule). |
| E2 | **High** | List cards (20:190) | A red **delete** icon sits on every card, one tap from destruction; no confirmation/undo state is designed. | Accidental loss of irreplaceable data; also the trash is the only visible action, so edit is undiscoverable. | Tap card = edit; delete inside the edit screen or via swipe with Undo snackbar. Design the confirmation. |
| E3 | Medium | Events list | No **next occurrence** ("Thu 5 Nov · in 5 days"), no sort indication, no grouping. | The list exists to answer "when is it next?". | Add trailing "in 5 days" and sort by next occurrence. |
| E4 | Medium | Add birthday (24:1127) | Birthdays can only be attached to a contact; form shows the Hebrew date card *before* a contact is chosen with a default of today. | Users without the person in Contacts, or who want privacy from Contacts, are blocked. The default date looks like data. | Allow "No contact — local birthday"; show the date card empty until set; display computed age ("turns 3 in 5787"). |
| E5 | Medium | Forms | Helper line "Choose the Hebrew date for this event." sits at the top of every form, above the Title field, like an instruction banner. | Odd hierarchy, repeated. | Remove; the date card's own label suffices. |
| E6 | Medium | Add event | Primary action is a full-width "Save" at the end of the form; M3 forms typically use a top-bar "Save" action or a bottom bar. Disabled state is grey-on-grey (12%/38%) with no hint why. | Discoverability on long forms; disabled without reason. | Keep in top app bar as a text/filled button; show validation hint under Title. |
| E7 | Low | Birthdays tab sample | "Sarah Cohen · א׳ תשרי תשפ״ז" implies born 1 Tishrei *this* year; sample data confuses the semantic of the year in a recurring birthday. | Sample realism. | Use a birth year (e.g. תשנ״ה) and show "turns 32". |
| E8 | Low | Overflow menu (23:988) | "Export as JSON / Export as CSV / Import events" — no explanation that this is not a backup (spec explicitly says so). | Users will treat it as backup. | Rename "Export events…" with submenu; add a one-line note in the dialog. |

### 2.8 System calendars & Cities (06)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| L1 | Medium | Cities settings (47:3269) | "CURRENT LOCATION Jerusalem — Based on your time zone. Tap to allow location access." The city is a *fallback*, but the card looks like a success state. No indication which city is the *selected* one (spec: selection happens only in the drawer). | Users will add a city here and expect it to apply. | Show status icon (approx/unknown), a clear "Allow location" button, and a "Selected" badge mirrored from the drawer. |
| L2 | Medium | City picker (29:2593) | Flat list "Jerusalem / Israel …" with no country grouping, no coordinates, no elevation, no time-zone, and no "custom location" entry. | Zmanim depend on elevation and exact coordinates; power users (and anyone outside the bundled list) need custom lat/long. | Show "Israel · 31.78, 35.22 · 754 m · GMT+2" secondary line; add "Use coordinates…" at the bottom; recent/nearby section at top. |
| L3 | Low | System calendars permission (29:2524) | Title "Bring in your plans" is friendly but the body repeats "to display your events on the Hebrew calendar"; the button is "Grant Permission" (title case). | Tone/casing. | "Grant permission"; mention "read-only, nothing leaves your device" as in onboarding. |
| L4 | Low | Permission screens (29:2524, 23:794) | No **permanently denied** state ("Open system settings"). | Common dead end on Android 13+. | Add variant with "Open settings". |

### 2.9 Settings (07)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| S1 | **High** | Settings full scroll (47:2671) Notifications group | Five bare switches; the spec describes per-category configuration (hours before candle lighting, advance days for holidays, Omer at nightfall and/or morning time). None of that is visible or designed. | The most asked-about feature ("remind me 1 hour before candles") has no UI in the file. | Each row → "Candle lighting · 1 hour before, 08:00 Friday" with chevron to a detail screen; design those detail screens. |
| S2 | Medium | Settings → App language dialog (49:3524) | Options: "System default", "Hebrew (עברית)". **No "English"**. | A user whose system is Hebrew cannot choose English UI. | Add "English". |
| S3 | Medium | Settings icons | The same twilight/candle icon is used for Candle lighting offset, all four Yahrzeit rows, and Halachic Times; Developer Settings and Personal Events share the person icon. | Icons stop carrying meaning. | Yahrzeit rows: candle/memorial icon; Halachic times: schedule icon; Developer: code icon. |
| S4 | Medium | Halachic times screen (47:3078) | Checkbox list with English names only, no times preview, no reordering; "Candle Lighting"/"Havdalah" listed as zmanim; the settings row says "3 shown in day panel" but it's unclear whether this controls the three *tiles*, the *list*, or both. | Ambiguous scope. | Subtitle "Shown in the day panel and as quick tiles"; show Hebrew names and today's time per row; drag handles. |
| S5 | Medium | Settings group cards | Elevated cards with uppercase headers and 1 dp shadows; M3 settings use flat lists with headers. Group "HALACHIC TIMES" contains one row; "DEVELOPER" and "ABOUT" contain one row each. | Visual weight > content; single-row groups are headers for nothing. | Flat list; merge Halachic times into Calendar preferences; About/Developer as plain rows at the end. |
| S6 | Low | About (47:3314) | "Version 0.4.2-debug", "Install Source: None" — debug strings; no update check, licence link or privacy statement (spec mentions GitHub update checks, MIT, crash-report email). | Polish and transparency. | "Version 0.4.2", "Check for updates", "Privacy", "Open-source licences", "Send feedback". |
| S7 | Low | Israel/Diaspora dialog (49:3379) | Auto-detect option lacks the explanation given in the row subtitle; Theme dialog has one. | Inconsistent dialog pattern. | Add supporting text "Israel when the device time zone is Israel's". |
| S8 | Nit | Settings row "Israel / Diaspora" subtitle | "Affects second day Yom Tov and parsha readings" — good; also affects Shavuot length and Purim/Shushan Purim not at all; fine, but "Yom Tov Sheni" wording is the term experts use. | Precision. | "Affects Yom Tov Sheni and parasha schedule". |

### 2.10 Dark mode (Screens — Dark)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| K1 | Medium | Dark month (37:75) | Saturday column tint (#1B1D1F on #131517) is nearly invisible; "Candles"/parsha chips (#2A2C2F on #1B1D1F) are low-contrast grey-on-grey. | Shabbat emphasis disappears in dark mode. | Use surfaceContainerHigh for the Shabbat column and labelled chips with primary-tinted text. |
| K2 | Medium | Dark day sheet (37:211) | Sheet (#1F2123) on background (#131517) with cards (#1F2123?) — cards and sheet are nearly the same value, so the card boundary relies on radius alone. | Hierarchy flattens. | Sheet = surfaceContainer, cards = surfaceContainerLowest (#0E1012) or High; verify ≥ 2 tonal steps. |
| K3 | Low | Dark events list (37:560) | Delete icons #FFB4AB on dark cards are the only saturated colour on screen — they dominate. | Destructive affordance gets visual priority. | See E2; otherwise use onSurfaceVariant for the trash and error only on press. |
| K4 | Low | Dark agenda (37:746) | Device-calendar orange icon is full-saturation on dark; M3 suggests tone-mapping third-party colours (harmonise). | Harsh. | Harmonise calendar colours to the palette in dark. |
| K5 | Nit | Dark variants complete | Good: every light frame has a dark twin; tokens exist for both. | — | Keep. |

### 2.11 Hebrew / RTL (Screens — Hebrew)

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| R1 | **High** | All Hebrew frames | Times rendered "5:27 PM" with Latin AM/PM in Hebrew UI. | Israeli convention is 24-hour; "PM" in a Hebrew sentence is jarring. | 24-hour when locale/device says so ("17:27"). |
| R2 | **High** | RTL Drawer (44:1279), RTL month, RTL settings | City names not localised: "Jerusalem" in a Hebrew drawer; "Grandpa Moshe" is user content (fine), but bundled cities must have Hebrew names ("ירושלים"). | Mixed-script primary labels in the most-used chrome. | Localise bundled city and country names. |
| R3 | Medium | RTL day sheet (44:657), RTL Zmanim (44:1931) | Each zman shows Hebrew primary and an **English transliteration sublabel** ("Sunrise", "Sof Zman Shema (GRA)", "Vayera"). | In a Hebrew UI the transliteration is noise; Hebrew speakers don't need "Sunrise". | Drop sublabels in HE UI (or make them optional: "Show English names"). |
| R4 | Medium | RTL month (44:75) | Day-number cell: Gregorian number right-aligned, Hebrew date left (tiny) — correct mirroring, but the Hebrew UI only has a *Gregorian-primary* month frame; the Hebrew-primary month in Hebrew UI (the likely default for Israelis) is not designed. | Coverage gap for the most likely Israeli configuration. | Add "Calendar / Month — Hebrew mode (Hebrew UI)". |
| R5 | Medium | RTL Agenda (44:2115) | Mixed-direction strings: "יארצייט — Grandpa Moshe", "Light a candle" LTR inside RTL cards; "PM 5:27 הבדלה" — the "PM" wraps to the wrong side visually. | Bidi rendering of times/numbers is where RTL apps break. | Use 24-hour (removes AM/PM), wrap user LTR content with isolation marks, and keep numerals + units together. |
| R6 | Low | RTL Settings (50:1302) | "ארץ ישראל / חו״ל" good; "זיהוי אוטומטי (ארץ ישראל)" good; "הקדמת הדלקת נרות" good. "לוחות שנה במכשיר" is fine. | Mostly good Hebrew. | Minor: "חגים ישראליים מודרניים" → "חגים לאומיים" is more natural. |
| R7 | Low | RTL frames | Hebrew in Noto Sans Hebrew next to Roboto digits: weight mismatch (Noto Hebrew looks heavier/larger at the same sp). | Texture inconsistency. | Set Hebrew styles 1 sp smaller or use a matched pair (e.g. Heebo/Rubik). |

### 2.12 Onboarding

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| O1 | **High** | Onboarding page (52:75, 53:169) | Only pages 1 and 2 are built; pages 3–9 are text spec cards. AGENTS.md declares Figma the UI design source. | The source of truth is 22 % complete for onboarding. | Build the remaining 7 frames (light + dark + RTL for at least one). |
| O2 | Medium | Onboarding p1 illustration (52:91) | Illustration uses 24-hour times (6:12, 16:41, 16:23) and candle time 16:23 for 30 Oct; the app frames use 12-hour and 4:33 PM for the same evening (widget). | Internal contradiction visible to a careful user; also a sign the illustration isn't data-driven. | Use the same formatter and the same sample day as the rest of the file. |
| O3 | Medium | Walkthrough flow | 9 pages, Israel/Diaspora never asked, language choice is on page **9**. A Hebrew speaker on an English-system device reads 8 English pages first. | Long first-run; the one decision that changes halachic output is auto-detected silently. | Put language + Israel/Diaspora confirmation on page 1 ("Detected: Israel · Change"). Cut to 5–6 pages; merge Widgets+Appearance; merge Agenda into Calendar. |
| O4 | Low | Bottom bar (51:437) | Dots are centred while Back/Next are at the edges; the dots row has 9 dots at 8 dp — fine, but "n of m" fallback above 9 pages is spec'd, indicating awareness the flow is at its limit. | Signal to shorten. | See O3. |
| O5 | Low | Permission panel Denied variant (51:454) | "Not allowed. Everything else works, and you can allow it later from Settings." still offers "Use my location" — on permanent denial this button can't open the system prompt. | Dead button. | Change CTA to "Open settings" in the Denied state. |

### 2.13 Widgets, tile, icon

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| W1 | Medium | Widget / Today Full (54:74) | On Shabbat the widget shows "Candle Lighting **Fri 4:33 PM**" (yesterday) and "Havdalah Sat 5:27 PM". | Stale info on the glanceable surface. | After candle lighting has passed, show the *next* Friday's; on Shabbat show "Shabbat ends 5:27 PM" prominently. |
| W2 | Medium | Widget / Zmanim (54:240) | Three rows in a 3×3 cell leave ~60 % empty; only Sunrise/Sunset/Nightfall. | Wasted real estate on a widget users add *for* zmanim. | Fill with the user's chosen zmanim (6–8 rows) or make it resizable with a density rule. |
| W3 | Medium | Widget / Month (54:91) | No holiday/Shabbat chips, no event dots, header "5787 חשון" with the same mixed-numeral issue (C4). | Low information density vs the in-app month. | Add dots for events/holidays, a Shabbat tint, and "today" header. |
| W4 | Low | Today widget & QS tile | "20 Cheshvan, 5787" — comma between month and year is unusual. | Formatting. | "20 Cheshvan 5787". |
| W5 | Low | Launcher icon (54:455) | The rendered icon shows a navy disc with a white calendar glyph and a small "כ"; the spec text mentions a gold crescent that isn't visible. Hebrew numeral is small (38 %) and reads poorly at 48 dp. | The name means "moon"; the brand mark doesn't say moon. The daily letter is the icon's whole point. | Make the numeral ≥ 50 % of the safe zone, bold; bring the crescent into the mark; test at 48 dp on light and dark wallpapers. |
| W6 | Low | Widgets | No Hebrew-UI widget frames; no widget in holiday-theme colours. | Coverage. | Add one RTL widget set. |

### 2.14 Foundations & design-system quality

| # | Sev | Location | Finding | Why it matters | Recommendation |
|---|---|---|---|---|---|
| F1 | **High** | Typography (32:454): `app/cell-micro` 9sp, `app/cell-micro-bold` 9sp, `hebrew/cell-micro` 9sp, `picker/gregorian-sub` 9sp, `picker/hebrew-sub` 10sp | Five text styles below 11 sp are codified as tokens. | Institutionalises an accessibility failure (see C1, D4). | Remove sub-11 sp styles; add `labelSmall` 11 sp Hebrew variant. |
| F2 | Medium | Colour (32:6) | Palette is almost achromatic: primary #3B4A55, secondary #5A6570, surfaces warm greys. primaryContainer #D6E1E8 vs secondaryContainer #E0E5E9 vs surfaceVariant #E5E1DC are within a few ΔE. Holiday accents exist but the default has no accent at all. | Selection, "today", chips and tabs all have weak semantic colour; state differentiation relies on shape. | Introduce a modest-chroma primary (e.g. deep blue or Jerusalem-stone gold as tertiary already is) for today/selected/CTA; keep neutrals for surfaces. |
| F3 | Medium | Components page | Every component is duplicated as `Direction=LTR` / `Direction=RTL` variants (≈2× the component count). | Maintenance debt; changes must be made twice; RTL drifts. | Build components with auto-layout so RTL is a frame-level property; keep explicit RTL variants only where mirroring is wrong (icons, numerals). |
| F4 | Medium | Components | Missing interaction states: Button has Disabled only for Filled; no pressed/focused/hover anywhere; Text field has no Error/Disabled; Switch/Checkbox no Disabled; Checkbox no Indeterminate; Drawer item no Pressed. | Engineers implementing from Figma will invent states. | Add state matrices for interactive components, or document "use M3 defaults" explicitly. |
| F5 | Medium | Foundations: Shape (32:687) | Radii 4/8/12/14/18/24/28: `medium` 18 and `quickCard` 14 are off the M3 scale (4/8/12/16/28) and the file uses at least 7 radii. | Shape rhythm looks arbitrary. | Collapse to 4/8/12/16/28; map quick card → 12, badge → 8. |
| F6 | Low | Elevation (32:716) | Settings and zman group cards use shadows in light mode; M3 favours tonal elevation. | Dated look, double edges (shadow + fill). | Shadow only for FAB/menus/sheet; cards tonal. |
| F7 | Low | Component naming | Mixed namespaces: `Agenda / Card`, `Day cell / Event bar`, `Quick zman card`, `Zman row`, `Sheet zman row`, `My events / List card`, `Settings / List item`. | Hard to search; unclear ownership. | Scheme `Area / Component / Variant`: `Calendar / Day cell`, `Calendar / Event chip`, `Zmanim / Row`, `Zmanim / Group card`, `Agenda / Row`… |
| F8 | Low | Icons (32:730) | Icon set mixes Filled and Outlined Material Icons (e.g. `event` and `event_outlined`, `delete` and `delete_outlined`). | Visual texture inconsistency, visible in Settings. | Standardise on Outlined (M3 default) except for selected states. |
| F9 | Nit | Typography | Hebrew styles missing line heights (`hebrew/headline-small`, `hebrew/title-medium`, `hebrew/body-medium-on`); `body-medium-on` is an unexplained name. | Token hygiene. | Fill in and rename. |
| F10 | Nit | Foundations header text | "Design tokens mirrored from the Android app…" is excellent documentation; keep this practice and add a "Markers & chips legend" and "Date/time formatting" section (12h/24h, numerals, year formats). | Would resolve several inconsistencies above at the source. | Add. |

### 2.15 Empty, permission and error states (file-wide)

| # | Sev | Finding | Recommendation |
|---|---|---|---|
| ST1 | **High** | No error states exist anywhere: location unavailable, zmanim unavailable at high latitudes (spec can't compute some zmanim in polar summers), import failure, crash-report dialog (Developer has "Test crash" but the dialog isn't designed), no network for update check. | Design an error pattern (inline banner + snackbar) and the crash-report dialog. |
| ST2 | Medium | Missing empty states: Agenda empty, Events tab empty, Search no-results, Cities search no-match, Saved cities empty. | Add, reusing the existing birthdays empty-state pattern. |
| ST3 | Medium | Missing "permanently denied" permission variants (location, contacts, calendar, notifications). | Add "Open settings" variants. |
| ST4 | Low | No loading/skeleton states (first launch computing, calendar provider query). | Add minimal shimmer or progress on Agenda/System calendars. |

---

## 3. Top 10 changes to make first

1. **Give the day sheet and Zmanim screen a date + city header** and day-stepping (N1, N2). Nothing else matters if the user doesn't know which day they're looking at.
2. **Fix the 9 sp micro type** — raise every date/chip label to ≥ 11 sp and redesign the day cell (C1, F1, D4).
3. **Unify terminology**: one word for zmanim, one for parsha, one transliteration scheme, one set of location labels, sentence case (X1–X6).
4. **Add an event type** (Yahrzeit / Birthday / Anniversary / Other) and the **after-sunset** toggle in the date picker (E1, D1).
5. **Mark Rosh Chodesh (and Shabbat Mevarchim/molad) in the grid and sheet**; replace generic "Candles"/"Personal" chips with times and titles (C2, C3).
6. **Expose halachic method**: label shitot per zman, sort chronologically, add Havdalah offset and candle-lighting presets with Jerusalem = 40 (Z1–Z4).
7. **24-hour time following device/locale**, especially in Hebrew UI; localise city names (R1, R2, Z9).
8. **Design the notification detail screens** promised by the spec (S1).
9. **Remove one-tap delete from list cards**; tap = edit, delete behind confirmation/undo (E2).
10. **Finish onboarding pages 3–9 and the error/empty/denied states** so Figma is actually the source of truth (O1, ST1–ST3).

## 4. Quick wins vs larger redesigns

**Quick wins (hours, copy/token-level):** X1–X8, C4, C5, C8, A1, A3, A6, S2, S6, S7, W4, F5, F8, F9, Z2 (sort), Z9/R1 (formatter), O2 (illustration numbers), N4 (nav icons).

**Medium (component or screen changes, days):** N1/N2 headers and stepping, C1/C6 day-cell redesign, C2/C3 chips, Z3/Z4 offsets dialogs, D1/D2 picker additions, E2/E3 list card, S1 notification detail screens, S3/S5 settings cleanup, K1/K2 dark tonal fixes, R3/R4 RTL frames, W1–W3 widgets, ST1–ST3 states, F4 state matrices.

**Larger redesigns (worth a spec):**
- **Top-level navigation**: replace the drawer with a 4-destination bottom bar (Month · Agenda · Zmanim · Events) and move Settings/city selection to the top bar (N3, N5, N8).
- **Day sheet consolidation**: one "Shabbat/Yom Tov" card (parsha, candle lighting, havdalah, mevarchim/molad), one zmanim card (user-chosen rows), one events card; drop the duplicated tiles (Z5–Z8).
- **Personal events model**: typed events with yahrzeit-specific fields, next-occurrence list, nth-yahrzeit count (E1, E3, Z7).
- **Zmanim methods**: per-zman shita selection and labelling, Havdalah opinions (Z1, Z3).
- **Design system refactor**: auto-layout-driven RTL instead of duplicated variants, state matrices, naming scheme, mild-chroma primary (F2–F4, F7).

## 5. What I could not verify

- Whether "Jerusalem time" labelling (spec) appears when the selected city's zone differs — no frame shows a non-device-zone city selected.
- The Zmanim screen's Evening group and whether Chatzot HaLaylah / candle lighting appear there (all frames truncate below "Sunset").
- Interaction behaviours: what the top-bar calendar icon does, whether the title dropdown opens the Go-to-date picker, swipe gestures, long-press, whether list cards are tappable for edit.
- Whether notification settings have detail screens that simply weren't drawn.
- Actual contrast ratios: judged from hex values in Foundations and screenshots, not measured in context with alpha tints.
- Font-scale behaviour (1.3×/2.0×) — no large-text frames exist.
- Tablet/foldable/landscape layouts — none exist (onboarding spec mentions a 640 dp cap, nothing else does).
- Holiday-theme application on real screens — only swatches in Foundations; no screen frame in Chanukah/Pesach theme.
- Whether variables are actually bound on components (metadata suggests tokens exist; binding wasn't audited node-by-node).
- Component descriptions/Code Connect mappings — not read.
- Some sample-data checks were done by hand (5787 is a leap year; Cheshvan 5787 is full; 20 Cheshvan = 31 Oct 2026; parasha sequence Bereshit→Toldot and Shemini Atzeret on 3 Oct are consistent); Jerusalem sunrise/sunset values look plausible but were not recomputed.

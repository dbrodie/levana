---
id: TASK-57.1
title: Audit and unify terminology in Hebrew and English
status: In Progress
assignee:
  - '@codex'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-08T15:50:00Z'
labels:
  - design-review
  - store-listing
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: high
type: task
ordinal: 71000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Audit every user-facing term in English and Hebrew (app, widgets, tile, notifications, store text) and make it consistent, following the naming principles in specs/PROJECT_SPEC.md (e.g. Netz vs Sunrise, Parsha/Parashat, transliteration scheme, sentence case, city/location naming, Hebrew vs English names in each UI language, date formats such as W4 "20 Cheshvan, 5787"). First decide and record the policy (glossary per language: which term is primary/secondary, transliteration scheme, casing, date/number formats); then apply it. The reviewer's recommendations are not necessarily right (e.g. the X8 suggestion is not wanted).

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **X1** (High) — Day sheet (21:660), Zmanim (13:236), Settings (47:2671), Halachic times (47:3078). The same concept is called **Zmanim** (screen title, sheet card title), **Halachic times** (settings group + screen), **More halachic times** (link) and **halachic times** (settings row). *Why:* Users build a mental model from labels; three names for one thing makes "where do I set which times show?" a hunt. *Reviewer suggestion:* Pick one user-facing term per language ("Zmanim" in both EN and HE; Hebrew: "זמני היום") and use it for screen title, card title, link and settings row. Keep "halachic" only in a body-text explanation.
- **X2** (High) — Day sheet quick tiles vs. Zmanim card vs. Zmanim screen. The three quick tiles are labelled **NETZ / SHKIA / TZEIT**; the card right below them labels the *same* times **Sunrise / Sunset / Nightfall** with Hebrew sublabels; the Zmanim screen uses **Alot HaShachar, Sof Zman Shema…** (transliterated). Three naming systems on one screen. *Why:* A beginner can't connect "TZEIT 5:27" with "Nightfall 5:27"; an expert finds it sloppy. *Reviewer suggestion:* Choose a primary and a secondary language per UI language and apply it uniformly: EN UI → transliteration primary + Hebrew secondary everywhere (so tiles become "Netz", list rows "Netz HaChama / הנץ החמה"), or English primary everywhere. Do not mix within a screen.
- **X3** (High) — Month grid chips (11:9, 21:307), Agenda (27:1431), Search (27:1346). Parsha spelled four ways: **Parsha** (sheet card), **Parashat** (agenda), **parashot** (search placeholder), and parsha *names* switch pronunciation systems: **Bereshis**, **Toldos** (Ashkenazi *sav*) next to **Vayera**, **Chayei Sara**, **Noach**, and the app elsewhere says **Shabbat**, **Chatzot**, **Tefillah** (Sephardi/Israeli *tav*). *Why:* Transliteration scheme is a trust signal for this audience. Mixed *s/t* reads as auto-generated. *Reviewer suggestion:* Decide one scheme (Israeli/Sephardi "t" matches "Shabbat", "Sukkot", "Chatzot" already in use): Bereshit, Toldot, Chayei Sarah. Standardise on "Parashat X" (agenda, sheet card title "Parashat Vayera") and "parashot" only as a plural noun. Consider a user setting (Ashkenazi/Sephardi transliteration) later.
- **X4** (Medium) — Settings Yahrzeit rows (47:2671). "Also mark 1 **Nissan**" — the month is conventionally "Nisan" in English; "Nissan" is the car brand. *Why:* Credibility. *Reviewer suggestion:* "1 Nisan". Audit all month names (Cheshvan, Kislev, Tevet, Shevat, Adar I/II, Nisan, Iyar, Sivan, Tammuz, Av, Elul).
- **X5** (Medium) — Settings, Agenda, Halachic times. Casing inconsistency: "Candle Lighting" (settings notification row, Halachic times list) vs "Candle lighting" (agenda card, settings row "Candle lighting offset"); "Halachic Times" vs "Halachic times". *Why:* M3 uses sentence case; inconsistency looks unedited. *Reviewer suggestion:* Sentence case everywhere.
- **X6** (Medium) — Drawer (28:1501), Settings → Location, Cities (29:2593), Cities settings (47:3269), Onboarding p2. Location concept named **Active city**, **Manage cities**, **Location**, **Cities**, **Locations**, **Add location**, **Add city**, **Current location**. *Why:* Seven labels for one subsystem. *Reviewer suggestion:* Settings row "Cities" → screen "Cities" with sections "Current location" / "Saved cities" / "+ Add city"; drawer "Selected city" (matches the spec's "selected city is a display lens") and "Manage cities".
- **X7** (Medium) — RTL Agenda (44:2115), RTL My events (44:2528). Hebrew uses **יארצייט** (Yiddish loanword). Israeli Hebrew speakers say **יום השנה** / **אזכרה**; Yiddish spelling also varies (יארצייט/יאָרצײַט). *Why:* Target users include Israelis; the Yiddish term reads as a diaspora-only app. *Reviewer suggestion:* Use "יום השנה" (or "יום זיכרון" with care — collides with Yom HaZikaron). Keep "Yahrzeit" in English UI.
- **X8** (Low) — Add/Edit event forms. Toggle label "Use yahrzeit Adar rules" with a long technical description ("Adar dates follow Settings → Yahrzeit settings. Adar I and II remain separate.") *Why:* Jargon on a form; also shown for non-Adar dates. *Reviewer suggestion:* Replace with an event *type* selector (see E1). If kept, show only when the chosen date is in Adar, and reword: "Treat as yahrzeit (leap-year Adar rules apply)".
- **R2** (High) — RTL Drawer (44:1279), RTL month, RTL settings. City names not localised: "Jerusalem" in a Hebrew drawer; "Grandpa Moshe" is user content (fine), but bundled cities must have Hebrew names ("ירושלים"). *Why:* Mixed-script primary labels in the most-used chrome. *Reviewer suggestion:* Localise bundled city and country names.
- **R3** (Medium) — RTL day sheet (44:657), RTL Zmanim (44:1931). Each zman shows Hebrew primary and an **English transliteration sublabel** ("Sunrise", "Sof Zman Shema (GRA)", "Vayera"). *Why:* In a Hebrew UI the transliteration is noise; Hebrew speakers don't need "Sunrise". *Reviewer suggestion:* Drop sublabels in HE UI (or make them optional: "Show English names").
- **R4** (Medium) — RTL month (44:75). Day-number cell: Gregorian number right-aligned, Hebrew date left (tiny) — correct mirroring, but the Hebrew UI only has a *Gregorian-primary* month frame; the Hebrew-primary month in Hebrew UI (the likely default for Israelis) is not designed. *Why:* Coverage gap for the most likely Israeli configuration. *Reviewer suggestion:* Add "Calendar / Month — Hebrew mode (Hebrew UI)".
- **R5** (Medium) — RTL Agenda (44:2115). Mixed-direction strings: "יארצייט — Grandpa Moshe", "Light a candle" LTR inside RTL cards; "PM 5:27 הבדלה" — the "PM" wraps to the wrong side visually. *Why:* Bidi rendering of times/numbers is where RTL apps break. *Reviewer suggestion:* Use 24-hour (removes AM/PM), wrap user LTR content with isolation marks, and keep numerals + units together.
- **R6** (Low) — RTL Settings (50:1302). "ארץ ישראל / חו״ל" good; "זיהוי אוטומטי (ארץ ישראל)" good; "הקדמת הדלקת נרות" good. "לוחות שנה במכשיר" is fine. *Why:* Mostly good Hebrew. *Reviewer suggestion:* Minor: "חגים ישראליים מודרניים" → "חגים לאומיים" is more natural.
- **W4** (Low) — Today widget & QS tile. "20 Cheshvan, 5787" — comma between month and year is unusual. *Why:* Formatting. *Reviewer suggestion:* "20 Cheshvan 5787".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A written terminology policy and glossary (EN + HE) is recorded in the specs
- [x] #2 All strings, widgets, tile and notifications follow it
- [x] #3 Figma screens updated to match
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Audit resources, calendar formatters, location names and all presentation surfaces; record EN/HE terminology policy and glossary in specs. 2. Load required Figma guidance, inspect and update existing frames to the policy before app edits. 3. Apply shared naming and formatting consistently across app, widgets, tile and notifications; audit store text without changing fastlane. 4. Run required build, unit tests and lint, then verify light/dark/Hebrew on a dedicated emulator using android-debug. 5. Record evidence and tested commit, commit changes and hand off to coordinator without merging or marking Done.

6. Coordinator: review and integrate; add concise English/transliteration rules and a canonical spelling inventory; verify combined master, push, mark Done, push status and clean up.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Terminology policy recorded in specs/TERMINOLOGY.md before implementation. Retain Hebrew יארצייט for precise Orthodox usage; reject X8 as instructed. Use consistent English solar names, sentence-case ritual transliterations and Hebrew-only Hebrew rows. Store listing: screenshots 1–8 and walkthrough wording affected; audit only, no fastlane changes.

Owner reviewed online naming comparison and approved: Halachic times for feature/navigation; transliterated individual zman names using Israeli/Sephardi forms; compact Netz/Shkia/Tzeit; Candle lighting remains English. Supersedes the initial English solar-name proposal. Continue audit and align Figma/app to this decision.

Implemented the owner-approved glossary in specs/TERMINOLOGY.md and linked it from project/Figma specs. Figma Components, Light, Dark, Hebrew, Onboarding and Widgets pages updated in session e8ece6ba-7faa-4a6b-a023-19f02a0a070f (file E1IFrxKcldXbXnvXy5Nzhd). Visually reviewed nodes 13:236, 44:1931, 21:660, 50:1302, 52:75 and 54:74. Hebrew rows omit English secondary labels; city/country resources cover the bundled gazetteer and bilingual search. Old selected-time keys migrate through RENAMED_ZMAN_KEYS. Torah reading is the feature/category; named readings retain Parashat and one Israeli/Sephardi spelling map. Date formatting uses Nisan/Tevet and omits the English month/year comma. X8 behavior remains unchanged. Store listing: screenshots 1–8 and walkthrough wording need release refresh; fastlane audited only and untouched.

Verification: required assembleDebug, testDebugUnitTest and lintDebug plus recordRoborazziDebug passed (1254 tests, 0 failures/errors, 8 opt-in tests skipped), log /tmp/task-57.1-checks-pass.log. Reviewed representative updated EN/HE time settings, walkthrough and settings baselines. Dedicated android-debug session /tmp/levana-emu-v2943nak (emulator-5554); screenshots and UI XML under artifacts cover EN light/dark times, HE light/dark times, day sheet, drawer, Hebrew city picker and persisted Tel Aviv, final city accessibility labels, and Hebrew QS tile semantics. Initial full run had 11 old-copy assertions; corrected all and reran successfully. Runtime screenshot artifacts remain outside the worktree.

Tested commit: 7665f400cc32d1959b5981c2f55edb65308bd9b1. From worktree root with .env loaded in the same command, assembleDebug + testDebugUnitTest + lintDebug + verifyRoborazziDebug passed in 2m38s. 1254 tests, zero failures/errors, 8 opt-in tests skipped. Lint: zero errors, 83 warnings. Evidence: /tmp/levana-emu-v2943nak/artifacts/commit-verification.log, test-results/, lint-results-debug.txt; final-he-cities.png/xml confirms localized saved city and delete accessibility label; final-he-tile.png/xml and final-en-tile-verified.png/xml confirm Hebrew-letter and comma-free English dates. EN/HE light/dark time-screen captures are en-times, en-times-dark, he-times-light, he-times-dark. final-runtime-errors.log is empty. Owned emulator stopped. Limitations: no timed notification delivery or home-screen widget placement exercised manually (shared formatter/repository and notification/widget tests passed); system-default Hebrew tile fallback is code-reviewed, explicit HE/EN app languages checked on device. Existing layout, bidi/time-format follow-ups and current-location fallback behavior remain with dedicated tasks. No fastlane changes, merge, push or Done transition. This notes-only follow-up commit does not alter the tested implementation.

Coordinator reviewed implementation, compatibility mapping, glossary and verification log; merged cleanly at cba5e0e. Added specs/LANGUAGE_RULES.md as concise policy and specs/SPELLINGS.md with canonical time, month, holiday and complete Torah-reading spellings and links to bilingual place-name inventory. Combined master checks underway; prior runtime evidence and limitations retained.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented and verified the owner-approved EN/HE terminology policy across app, widgets, tile, notifications and Figma. Includes compatibility mapping for old time selections, consistent Torah/month names and date formatting, Hebrew-only secondary-label policy, and bundled city/country localization with bilingual search. Build, 1254-test suite, lint and Roborazzi verification pass on 7665f40. Evidence and limitations are recorded above. Ready for coordinator review/integration; retained In Progress with store-listing label.
<!-- SECTION:FINAL_SUMMARY:END -->

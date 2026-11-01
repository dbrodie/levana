---
id: TASK-57.1
title: Audit and unify terminology in Hebrew and English
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
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
- [ ] #1 A written terminology policy and glossary (EN + HE) is recorded in the specs
- [ ] #2 All strings, widgets, tile and notifications follow it
- [ ] #3 Figma screens updated to match
<!-- AC:END -->

---
id: TASK-57.2
title: Redesign the day sheet and Zmanim screen from a product perspective
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
type: feature
ordinal: 72000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Rethink the day sheet (collapsed/expanded) and the Zmanim screen as a product: date/place context, day navigation, duplicated times, the meaning of the featured tile, and the day-specific sections (Shabbat Mevarchim/molad, Omer, Chanukah, fasts). Design in Figma first, then implement.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **N1** (Critical) — Calendar / Day sheet — expanded (21:660, 37:211, 44:657). The expanded sheet hides the month grid and **shows no date at all**. The top bar still says "October 2026"; the sheet shows only a "Shabbat" chip. Which day's times are these? *Why:* The day sheet is the app's primary content surface. A screenshot, a glance after returning to the app, or TalkBack gives no date. *Reviewer suggestion:* Add a sheet header: "Shabbat, 31 October · כ׳ חשון תשפ״ז" (weekday, Gregorian, Hebrew), plus the active city and "Jerusalem time" when it differs (spec). Add ‹ › day stepping or horizontal swipe in the sheet.
- **N2** (High) — Zmanim — Day list (13:236, 37:470, 44:1931). Screen title is only "Zmanim": no date, no city, no way to move to another day. *Why:* Experts open a zmanim screen to compare days (erev Shabbat, Yom Kippur). Here it's a dead end for one unlabelled day. *Reviewer suggestion:* Title "Zmanim" with subtitle "Sat 31 Oct · 20 Cheshvan · Jerusalem"; add prev/next day arrows and a date-picker action; consider a "Share / copy today's zmanim" action.
- **Z5** (Medium) — Day sheet. Quick tiles (NETZ/SHKIA/TZEIT), the Zmanim card (Sunrise/Sunset/Nightfall) and the Havdalah card all show the **same three or four numbers**; 5:27 PM appears three times. *Why:* Redundancy costs a full screen of scrolling on the most used surface. *Reviewer suggestion:* Make tiles the *user-chosen* three (they already are per the Settings row "3 shown in day panel") and drop the duplicate list; or drop the tiles and keep the list. Havdalah/Candle lighting belong in a single "Shabbat" card with Parsha.
- **Z6** (Medium) — Day sheet quick tiles. The featured (dark) tile is TZEIT — unclear whether "featured" means *next upcoming* (at 9:47 the next would be Shkia) or *most important today*. *Why:* Ambiguous emphasis. *Reviewer suggestion:* Define as "next zman" and relabel ("Next · Shkia 4:50 PM"), or remove emphasis.
- **Z8** (Medium) — Day sheet. Shabbat day shows no "Shabbat Mevarchim / molad" info, no "Shabbat ends" vs "Tzeit" distinction, no Omer/Chanukah count surface designed anywhere. *Why:* Spec commits to Omer count and molad; nothing in the file shows them. *Reviewer suggestion:* Add frames for Shabbat Mevarchim (molad line), Omer day (count card + nusach), Chanukah (candle count), fast day (start/end).
- **Z10** (Low) — Zmanim screen. Evening group cut off in every frame; Chatzot HaLaylah presence unverifiable. *Why:* Coverage. *Reviewer suggestion:* Add a full-scroll frame like Settings has.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Product decisions recorded in this task
- [ ] #2 Figma designs updated (light, dark, Hebrew)
- [ ] #3 Implemented and verified on the emulator
<!-- AC:END -->

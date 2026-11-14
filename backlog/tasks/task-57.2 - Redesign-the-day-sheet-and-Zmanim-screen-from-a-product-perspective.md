---
id: TASK-57.2
title: Redesign the day sheet and Zmanim screen from a product perspective
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-14T21:50:00Z'
labels:
  - design-review
  - store-listing
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
## Product decisions (owner, 2026-11-14)

Scope: the day sheet (phone bottom sheet) and the tablet day panel, which share `DayDetailContent`. The separate Zmanim screen keeps its full list; its date/city header and day stepping (N2) are a follow-up once the sheet design settles.

Goals of the sheet, in priority order: (1) state which day and which place; (2) show the limited set of zmanim the user selected; (3) make anything interesting about the day obvious (Shabbat, Yom Tov, Rosh Chodesh, Omer, Chanukah, fasts), each with its details; (4) show the day's events, Levana's and the phone calendar's, clearly told apart; (5) never show the same number twice; (6) an ordinary weekday may be short, header and times only; no filler.

1. **Header.** Every state starts with weekday, civil date and Hebrew date (the calendar-mode setting decides which leads), the city, and the "Jerusalem time" label when the city's clock differs from the device's. Day stepping with ‹ › arrows in the header; no horizontal swipe in the sheet.
2. **Chips.** The day's states as chips under the header: Shabbat, holiday names, Rosh Chodesh, Chanukah, fast. Multiple chips stack.
3. **Times card.** The three quick tiles are removed. One card lists the user's selected zmanim as rows in time order, uncapped (the Settings selection stays as it is; the row text becomes "Shown in the day panel"). Rows carry the primary name, the Hebrew secondary line in the English UI, and the time; a secondary line is reserved for TASK-20 (next-day marker) and TASK-57.7 (method). On today the next upcoming row is marked "Next" (same rule as the widgets, `nextSelectedZman`) and past rows are dimmed; other days show the plain list. "More times" at the bottom opens the Zmanim screen.
4. **Candle lighting and Havdalah** are not zmanim rows and are removed from the selectable set in Settings. They live in the Shabbat / Yom Tov card, which is ordered first, above the times card, on the days it applies, so the collapsed peek shows it.
5. **Day cards**, each with a fixed set of details: Shabbat (parsha with Hebrew, special Shabbat, Candle lighting on Friday, Havdalah on Saturday, Shabbat Mevarchim with molad); Yom Tov / Chol Hamoed (name, day n of m, candle lighting, havdalah); Rosh Chodesh (month, day 1 or 2 of 2); Omer (count, week and day); Chanukah (which night; candle count later); fast day (name; start and end times later). Fast times and Chanukah candle counts are designed now with placeholders and implemented in follow-up tasks, since neither is computed yet.
6. **Events.** Two separate cards, "My events" (Levana: birthdays, yahrzeits, custom events) and "Phone calendar" (device calendars, with the calendar colour), after the day cards. Only the day's own events; no upcoming events on empty days.
7. **Evening state (today only, after sunset).** The sheet keeps the civil day. The header gains a "Tonight" line with the incoming Hebrew date and its chips (Shabbat begins, Omer day n, Chanukah night n), and a "Tonight" preview card after the times card previews the next day's first times and its day cards. The Next marker moves to the remaining evening rows.
8. **Collapsed peek** is the top of the same content (header, chips, first card); nothing is rendered twice for the peek.
9. **Ordinary days** show header and times card, plus events if any. Nothing else.
10. **Interaction.** Tapping a zman row opens its explanation (TASK-57.7 method, TASK-57.31 unavailable times) once those exist; tapping an event opens it. Designed as tappable rows now.

Order of work: Figma frames (light, dark, Hebrew; ordinary day, Friday, Shabbat Mevarchim, Yom Tov, Rosh Chodesh + Omer, evening state, collapsed peek, tablet panel), then implementation in a worktree. Store listing: the day sheet appears in store screenshots; the coordinator refreshes them at release.
## Figma (done 2026-11-14)

File https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd. All colours, radii and spacing are bound to the existing variables; Dark frames resolve through the section's Dark mode.

Components (page "Components"):
- `Day sheet / Header` (set 119:905, Direction LTR/RTL): ‹ › day arrows, title, Hebrew date · city, optional clock note, optional Tonight line.
- `Day sheet / Day card` (set 115:404, Style Surface/Tonal × Direction): icon, title, subtitle, up to two `Day sheet / Key time` rows, highlight (tertiary), detail.
- `Day sheet / Key time` (set 119:911): label, Hebrew label, time, for candle lighting, havdalah and later fast times.
- `Day sheet / Calendar event row` (set 119:959): calendar-colour dot, title, time range.
- `Sheet zman row` (set 40:435) gained `State = Default | Next | Past` (NEXT badge, dimmed past rows). The `Quick zman card` component is no longer used by the redesign frames.

Frames, section "01b Calendar — Day sheet redesign (TASK-57.2)" on Screens — Light (116:6979): Shabbat Mevarchim 116:6980, ordinary weekday today 117:7269, Friday erev Shabbat with phone-calendar card 117:7642, Chanukah + Rosh Chodesh Friday 117:7996, Chol Hamoed Pesach + Omer 117:8397, today after sunset with Tonight card 117:8775, month with collapsed Shabbat peek 118:8607, foldable two-pane panel 118:8949. Dark (section 120:15718): 120:15719, 120:15798, 120:16120, 120:16451. Hebrew (RTL) in section 44:74: Shabbat Mevarchim 120:20026, ordinary weekday 120:20408, after sunset 122:3444.

Open for the owner: on a plain Shabbat the "Shabbat" chip and the Shabbat card sit next to each other; the card title now carries the parasha so they don't repeat, but the chip row could be dropped on days with a single card. The collapsed peek grew from ~175dp to 230dp (header + chips + top of the first card); the month grid loses about half a week-row of height.

Implementation notes for the task agent: `DayDetailContent` keeps one column with 16dp spacing; remove `QuickZmanimCards`, `ShabbatTimeCard`, `ParshaSection`, `MoladSection` and fold them into a `DayCard` composable with `KeyTimeRow`; `HalachicTimesSection` rows get `next`/`past` flags from `nextSelectedZman` on today; remove Candle lighting and Havdalah from the selectable zmanim in Settings (migrate saved selections); add `DaySheetHeader` with the date stepping intent in `DayDetailViewModel`/`CalendarViewModel`; the Tonight state needs sunset-crossing detection on today plus the next day's `DayInfo`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Product decisions recorded in this task
- [x] #2 Figma designs updated (light, dark, Hebrew)
- [ ] #3 Implemented and verified on the emulator
<!-- AC:END -->

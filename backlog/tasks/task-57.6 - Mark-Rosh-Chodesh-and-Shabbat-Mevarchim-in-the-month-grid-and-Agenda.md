---
id: TASK-57.6
title: Mark Rosh Chodesh and Shabbat Mevarchim in the month grid and Agenda
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T17:39:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: feature
ordinal: 76000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Close Jewish-calendar gaps: Rosh Chodesh is not marked in the month grid although the Agenda shows it; Shabbat Mevarchim (and molad) is not surfaced in the grid or Agenda.

Cause of the grid gap: CalendarRepository.toHebrewDay (used by the grid) takes the holiday only from KosherJava's yomTovIndex, which never returns ROSH_CHODESH. getDayInfo (day sheet, Agenda) adds Rosh Chodesh separately via isRoshChodesh. The grid needs the same fallback (a Yom Tov / Chanukah chip keeps priority on days that are both).

Agreed design (owner, 2026-11-01):
- Month grid, Rosh Chodesh: a chip on both days of a two-day Rosh Chodesh, in the existing holiday-chip style (reviewer suggested a tertiary tone; follow the existing Rosh Chodesh category colour unless Figma says otherwise).
- Month grid, Shabbat Mevarchim: no extra slot; append to the Saturday's parasha chip, e.g. "Vayera · Mevarchim" / Hebrew equivalent. It may truncate in narrow cells; the full text is in the day sheet.
- Agenda: on Shabbat Mevarchim the Shabbat card gets a second line, "Mevarchim Kislev · Molad …" (reviewer A2).
- Molad text: KosherJava already computes the molad and the day sheet shows it, but formatMolad is hardcoded English ("Molad: Friday 3:12 AM, 5 chalakim"), uses the device default locale for the weekday and a fixed "h:mm a". Localize it (English and Hebrew strings, app language for the weekday, device 12/24-hour setting) and use it in both the day sheet and the Agenda.
- Molad correctness: formatMolad converts getMoladAsDate to Asia/Jerusalem civil time (including summer time) while taking chalakim from the traditional calculation. Published molad announcements use the traditional reckoning (KosherJava getMolad(): moladHours/moladMinutes/moladChalakim, Jerusalem mean time). Check which the app should show against a published molad table (e.g. Molad Kislev and Molad Nisan/Tammuz 5787, to cover summer time), fix if wrong, and record the source in the task notes.
- Figma: design first, per specs/FIGMA.md. Add the Rosh Chodesh chip and the Mevarchim parasha chip to the month frames (light, dark, Hebrew), the Agenda Shabbat Mevarchim line, and the localized molad card in the day sheet.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **C2** (High) — Month — Gregorian (11:9), Hebrew mode (21:307). **Rosh Chodesh is not marked in the grid.** 30 Tishrei (11 Oct) and 1 Cheshvan (12 Oct) have no chip; in Hebrew mode ל׳ Cheshvan (10 Nov) has none — yet the Agenda shows "Rosh Chodesh Kislev" cards. *Why:* Rosh Chodesh changes davening (Hallel, Yaaleh v'Yavo, Musaf); its absence from the grid is a halachic completeness gap and an internal inconsistency. *Reviewer suggestion:* Add a Rosh Chodesh chip (tertiary tone) and a Shabbat Mevarchim marker; surface molad in the day sheet on Shabbat Mevarchim (spec promises it; no frame shows it).
- **A2** (Medium) — Agenda. Shabbat card is "Parashat Chayei Sara / Havdalah 5:22 PM"; the Friday card is "Candle lighting 4:27 PM". Shabbat Mevarchim (7 Nov) isn't marked. *Why:* Mevarchim is a weekly-relevant flag. *Reviewer suggestion:* Add "Mevarchim Kislev · Molad …" as a second line on that Shabbat.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Rosh Chodesh (both days) has a chip in the month grid, Gregorian and Hebrew mode
- [ ] #2 Shabbat Mevarchim is marked on the Saturday's parasha chip in the grid and as a Mevarchim/molad line on the Agenda Shabbat card
- [ ] #3 Molad text is localized (English/Hebrew, 12/24-hour) and verified against a published molad table
- [ ] #4 Figma month, Agenda and day-sheet frames updated to match
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Figma first (Levana file, specs/FIGMA.md): month frames (light, dark, Hebrew RTL) get a Rosh Chodesh chip on both RC days in the existing holiday-chip style/Rosh Chodesh category colour and the Mevarchim suffix on the Saturday parasha chip; Agenda Shabbat Mevarchim card gets a 'Mevarchim Kislev · Molad …' line; day-sheet molad card shows the localized text.
2. Data: CalendarRepository.toHebrewDay falls back to ROSH_CHODESH when jc.isRoshChodesh and no (visible) Yom Tov/Chanukah holiday — grid, date picker and month widget share HebrewDay so all gain the chip. HebrewDay gets isShabbatMevarchim.
3. Molad model: replace DayInfo.molad String with a Molad(month, dayOfWeek, hour, minute, chalakim) built from KosherJava's traditional getMolad() (moladHours/Minutes/Chalakim, Jerusalem mean time, no DST) for the month being blessed (the next month — check current code, which reads the current month's molad). Pin unit tests to a published molad table (Kislev, Nisan, Tammuz 5787) and record the source.
4. Text: ui/MoladText.kt formats it from string resources (values + values-iw), weekday in the app language, 12/24-hour from the device setting; used by the day sheet MoladSection and the Agenda.
5. Grid: parasha chip reads '<parasha> · Mevarchim' / Hebrew equivalent on Shabbat Mevarchim, no extra slot.
6. Agenda: AgendaItem.Shabbat carries the Molad; card adds the Mevarchim/molad line (also in its content description).
7. Tests: toHebrewDay RC (two-day RC Kislev 5787, one-day RC, RC Tevet during Chanukah keeps Chanukah), Shabbat Mevarchim detection (7 Nov 2026 yes, not in Elul), molad values vs published table, molad text en/he × 12/24h, Agenda assembler. Update Roborazzi baselines only where intended.
8. Verify: assembleDebug, testDebugUnitTest, lintDebug, verifyRoborazziDebug; emulator check (Gregorian + Hebrew grid around 7–11 Nov 2026, Agenda, day sheet; en + he); evidence in /Users/daniel/.paseo/evidence/task-57.6/.
<!-- SECTION:PLAN:END -->

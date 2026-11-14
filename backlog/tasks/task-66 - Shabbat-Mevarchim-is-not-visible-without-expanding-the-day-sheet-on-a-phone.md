---
id: TASK-66
title: Shabbat Mevarchim is not visible without expanding the day sheet on a phone
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-14T21:14:00Z'
labels:
  - calendar
  - day-sheet
dependencies: []
type: bug
ordinal: 109000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner report: on Shabbat 7 Nov 2026 (Shabbat Mevarchim Kislev 5787) the app did not visibly mark Shabbat Mevarchim.

Repro (phone, 1080x2400, any language, Israel or Diaspora, Gregorian or Hebrew grid mode; device date 7 Nov 2026): open the app. The month grid's Saturday chip reads 'Chayei S…' / 'חיי שרה …' (the ' · Mevarchim' suffix from TASK-57.6 is ellipsized; full text only in semantics). The day sheet opens at its peek height for today and shows only a 'Shabbat' badge and the Netz/Shkia/Tzeit cards. The 'Shabbat Mevarchim Kislev' molad card is below Halachic times and Torah reading, so it only appears after expanding/scrolling the sheet. The Agenda does show 'Mevarchim Kislev · Molad …' correctly, but it is not the start screen.

Root cause: not a calculation bug (isShabbosMevorchim and the molad are correct in every setting); the marking is placed where a phone user does not see it by default. The grid truncation was an owner-accepted trade-off in TASK-57.6; the day-sheet peek was not considered.

Evidence: /Users/daniel/.paseo/evidence/verify-shabbat-mevarchim (00 onboarding preview truncated, 01 en grid, 02 en peek sheet, 03 en expanded sheet, 04 en launch with today = 7 Nov, 05 en Agenda, 06 en Hebrew-mode grid, 07 he grid, 08 he Hebrew-mode grid with today = 7 Nov, 09 he expanded sheet, 10 he Agenda in a Diaspora city; .xml UI dumps show the full chip text).

Proposed fix: on Shabbat Mevarchim, the day-sheet badge reads 'Shabbat Mevarchim Kislev' / 'שבת מברכים חודש כסלו' instead of 'Shabbat', so it is visible at peek height. Owner decision still open (not part of this fix): whether the month grid should also show it on phones (e.g. Mevarchim-first chip text or a small moon marker; both were declined in TASK-57.6).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 On Shabbat Mevarchim the day sheet's peek shows a 'Shabbat Mevarchim <month>' badge (English and Hebrew) in place of the plain 'Shabbat' badge
- [ ] #2 Other Shabbatot keep the 'Shabbat' badge
- [ ] #3 Unit test pins isShabbatMevarchim true for 2026-11-07 (Israel and Diaspora, both grid modes) and false for 2026-11-14
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. DayDetailScreen.DaySheetBadges: when dayInfo.molad != null (set only on Shabbat Mevarchim), use stringResource(sheet_shabbat_mevarchim, moladMonthName(context, molad)) instead of sheet_shabbat.
2. Extend RoshChodeshMevarchimTest: 14 Nov false; Israel/Diaspora and Hebrew-mode grid true for 7 Nov.
3. Re-record/verify Roborazzi baselines touching a Shabbat Mevarchim day sheet, if any.
4. Build/test/lint; runtime check on own emulator; record evidence.
<!-- SECTION:PLAN:END -->

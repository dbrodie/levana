---
id: TASK-57.6
title: Mark Rosh Chodesh and Shabbat Mevarchim in the month grid and Agenda
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
type: feature
ordinal: 76000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Close Jewish-calendar gaps: Rosh Chodesh is not marked in the month grid although the Agenda shows it; Shabbat Mevarchim (and molad) is not surfaced in the grid or Agenda.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **C2** (High) — Month — Gregorian (11:9), Hebrew mode (21:307). **Rosh Chodesh is not marked in the grid.** 30 Tishrei (11 Oct) and 1 Cheshvan (12 Oct) have no chip; in Hebrew mode ל׳ Cheshvan (10 Nov) has none — yet the Agenda shows "Rosh Chodesh Kislev" cards. *Why:* Rosh Chodesh changes davening (Hallel, Yaaleh v'Yavo, Musaf); its absence from the grid is a halachic completeness gap and an internal inconsistency. *Reviewer suggestion:* Add a Rosh Chodesh chip (tertiary tone) and a Shabbat Mevarchim marker; surface molad in the day sheet on Shabbat Mevarchim (spec promises it; no frame shows it).
- **A2** (Medium) — Agenda. Shabbat card is "Parashat Chayei Sara / Havdalah 5:22 PM"; the Friday card is "Candle lighting 4:27 PM". Shabbat Mevarchim (7 Nov) isn't marked. *Why:* Mevarchim is a weekly-relevant flag. *Reviewer suggestion:* Add "Mevarchim Kislev · Molad …" as a second line on that Shabbat.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Rosh Chodesh and Shabbat Mevarchim are visible in the grid and Agenda
- [ ] #2 Figma updated
<!-- AC:END -->

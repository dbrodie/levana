---
id: TASK-57.8
title: Hebrew-mode month title mixes scripts ("5787 חשון")
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T15:22:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 78000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In the English UI, the Hebrew-mode month title mixes scripts: "5787 חשון" (Hebrew month name, Arabic year, and reads awkwardly in an LTR bar). The Hebrew UI is already right ("חשון תשפ״ז").

Agreed formats (owner, 2026-11-01):
- English UI: transliterated month, then the year in digits — "Cheshvan 5787" (and "Adar I 5787", "Adar II 5787"), using the same transliterated month names the header's month-range line already uses.
- Hebrew UI: unchanged — "חשון תשפ״ז".

Where: CalendarHeaderFormatter.hebrewMonthTitle. It is also used by the Agenda month headers (AgendaFormatter) and the Month home-screen widget (MonthWidget), so those change too — this also resolves the mixed-script widget header noted in TASK-57.21 W3. Update the KDoc example and tests/snapshots for all three.

Out of scope: the date picker. Its Hebrew-led title is written in Hebrew letters in both UI languages by design (1b) and uses the long year form "ה׳תשפ״ז"; leave it unchanged.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **C4** (Medium) — Month header, Hebrew mode (21:307). Title "5787 חשון" mixes Arabic numerals with a Hebrew month name and reads awkwardly in an LTR bar; the date picker shows the same year as "ה׳תשפ״ז". *Why:* Inconsistent year representation. *Reviewer suggestion:* EN UI: "Cheshvan 5787" (transliterated month). HE UI: "חשון תשפ״ז". Keep numerals consistent with the picker.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Hebrew-mode month title reads "Cheshvan 5787" in the English UI and "חשון תשפ״ז" in the Hebrew UI, including leap-year Adar I/II
- [ ] #2 Calendar header, Agenda month headers and the Month widget all use the new format, covered by unit tests and snapshots
<!-- AC:END -->

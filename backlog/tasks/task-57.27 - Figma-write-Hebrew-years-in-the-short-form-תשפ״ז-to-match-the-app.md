---
id: TASK-57.27
title: 'Figma: write Hebrew years in the short form (תשפ״ז) to match the app'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T09:23:00Z'
updated_date: '2026-11-02T09:27:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
type: chore
ordinal: 98000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-57.25 changed the app to write Hebrew years in the short form everywhere ("תשפ״ז", not "ה׳תשפ״ז"), but the Figma file was not updated. The date picker frames (title, year grid, footer) and any other frames (e.g. the onboarding/walkthrough illustrations, Agenda, widgets, day sheet) still show the long form "ה׳תשפ״ז"/"ה׳ תשפ״ז".

Scope: text-only Figma update. Find every Hebrew year written in the long form across the Levana Figma file (link in specs/FIGMA.md) and change it to the short form the app uses (HebrewDateText.hebrewYear). Don't change month spellings or anything else; note other mismatches in the task instead. Also check the Foundations "Date/time formatting" notes, if any, mention the year form.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 No frame in the Figma file shows a Hebrew year in the long form; all match the app's short form
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Load figma-use skill; open a session on file E1IFrxKcldXbXnvXy5Nzhd.
2. Read-only scan of every page for TEXT nodes containing a long-form Hebrew year (ה׳/ה' prefix, with or without space, before a year in letters); list node ids, parents (component/instance), and text.
3. Screenshot the main affected frames (before) into /Users/daniel/.paseo/evidence/task-57.27/.
4. Replace each with HebrewDateText.hebrewYear's short form (drop the ה׳ prefix and following space only), components first then overridden instance text, loading the existing fonts; leave everything else unchanged.
5. Re-scan to confirm zero long-form years; after screenshots; check the Foundations formatting notes.
6. Record changed nodes (old → new), other mismatches and evidence in notes; check AC #1; commit task update.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Result (2026-11-02)

Searched every page of the Figma file (Foundations, Components, Screens — Light/Dark/Hebrew, Onboarding, Widgets, Explorations) for TEXT nodes containing ה׳/ה'/ה’ followed by an optional space and ת. Each match was edited with deleteCharacters on only the "ה׳" prefix, so fonts and styles are unchanged. The short forms match HebrewDateText.hebrewYear (KosherJava formatHebrewNumber), e.g. 5780 → תש״פ, 5797 → תשצ״ז. None used the spaced "ה׳ תשפ״ז" form.

Changed text nodes: 48 (old → new)
- Components / "Date picker / Year" component set (20:242): text 20:228 ה׳תשפ״ז → תשפ״ז. The six variants share this text, so 20:231/20:234/20:237/20:239/20:241 followed with the one edit.
- Screens — Light, section "03 Date picker" (28:1751), 23 nodes:
  - footers 28:2049, 28:2302, 28:2423: כ׳ חשון ה׳תשפ״ז → כ׳ חשון תשפ״ז
  - titles 28:2102, 28:2354: חשון ה׳תשפ״ז → חשון תשפ״ז
  - year grid overrides I28:2362…I28:2418 (18 cells): ה׳תש״פ…ה׳תשצ״ז → תש״פ…תשצ״ז, selected cell I28:2385;20:234 → תשפ״ז
- Screens — Dark, section "03 Date picker — Dark" (37:785), 23 nodes: footers 37:973, 37:1108, 37:1173; titles 37:1005, 37:1141; year grid overrides I37:1148…I37:1170 (18 cells). Same edits as Light.
- Screens — Hebrew (RTL), frame "Date picker / Hebrew-led — day grid (Hebrew UI)" (44:2712): title 44:2741 חשון ה׳תשפ״ז → חשון תשפ״ז, footer 44:2844 כ׳ חשון ה׳תשפ״ז → כ׳ חשון תשפ״ז.

Not changed:
- Foundations, Onboarding, Widgets and Explorations had no long-form years. Their only year text was already תשפ״ז or 5787. The Foundations page has no note about the year form.
- The remaining "ה׳" matches are weekday names ("יום ה׳") or day 5 of the month, not years.

Verification: a second scan of all 8 pages found 0 long-form years.

Other mismatches: none noticed within this scope.

Evidence: /Users/daniel/.paseo/evidence/task-57.27/{before,after}-{components-year,light-date-picker,dark-date-picker,hebrew-date-picker}.png. Hebrew glyph order looks reversed in the MCP screenshot renderer in both the before and after shots; it is a rendering artifact, not a text change.

No app code changed, so no Gradle build or emulator run was needed.
<!-- SECTION:NOTES:END -->

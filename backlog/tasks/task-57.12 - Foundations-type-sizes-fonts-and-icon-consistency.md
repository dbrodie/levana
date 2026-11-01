---
id: TASK-57.12
title: 'Foundations: type sizes, fonts and icon consistency'
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
type: chore
ordinal: 82000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Fix design-system issues: text below 11sp (day cells, picker), Hebrew font weight versus Roboto, palette and state colours, radius scale, component states and naming, icon style and icon choices (A1 Rosh Chodesh icon, S3 repeated icons). Changes start in Figma Foundations and Theme.kt/Type.kt. Related: TASK-49 (accessibility audit).

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **F1** (High) — Typography (32:454): `app/cell-micro` 9sp, `app/cell-micro-bold` 9sp, `hebrew/cell-micro` 9sp, `picker/gregorian-sub` 9sp, `picker/hebrew-sub` 10sp. Five text styles below 11 sp are codified as tokens. *Why:* Institutionalises an accessibility failure (see C1, D4). *Reviewer suggestion:* Remove sub-11 sp styles; add `labelSmall` 11 sp Hebrew variant.
- **F2** (Medium) — Colour (32:6). Palette is almost achromatic: primary #3B4A55, secondary #5A6570, surfaces warm greys. primaryContainer #D6E1E8 vs secondaryContainer #E0E5E9 vs surfaceVariant #E5E1DC are within a few ΔE. Holiday accents exist but the default has no accent at all. *Why:* Selection, "today", chips and tabs all have weak semantic colour; state differentiation relies on shape. *Reviewer suggestion:* Introduce a modest-chroma primary (e.g. deep blue or Jerusalem-stone gold as tertiary already is) for today/selected/CTA; keep neutrals for surfaces.
- **F3** (Medium) — Components page. Every component is duplicated as `Direction=LTR` / `Direction=RTL` variants (≈2× the component count). *Why:* Maintenance debt; changes must be made twice; RTL drifts. *Reviewer suggestion:* Build components with auto-layout so RTL is a frame-level property; keep explicit RTL variants only where mirroring is wrong (icons, numerals).
- **F4** (Medium) — Components. Missing interaction states: Button has Disabled only for Filled; no pressed/focused/hover anywhere; Text field has no Error/Disabled; Switch/Checkbox no Disabled; Checkbox no Indeterminate; Drawer item no Pressed. *Why:* Engineers implementing from Figma will invent states. *Reviewer suggestion:* Add state matrices for interactive components, or document "use M3 defaults" explicitly.
- **F5** (Medium) — Foundations: Shape (32:687). Radii 4/8/12/14/18/24/28: `medium` 18 and `quickCard` 14 are off the M3 scale (4/8/12/16/28) and the file uses at least 7 radii. *Why:* Shape rhythm looks arbitrary. *Reviewer suggestion:* Collapse to 4/8/12/16/28; map quick card → 12, badge → 8.
- **F6** (Low) — Elevation (32:716). Settings and zman group cards use shadows in light mode; M3 favours tonal elevation. *Why:* Dated look, double edges (shadow + fill). *Reviewer suggestion:* Shadow only for FAB/menus/sheet; cards tonal.
- **F7** (Low) — Component naming. Mixed namespaces: `Agenda / Card`, `Day cell / Event bar`, `Quick zman card`, `Zman row`, `Sheet zman row`, `My events / List card`, `Settings / List item`. *Why:* Hard to search; unclear ownership. *Reviewer suggestion:* Scheme `Area / Component / Variant`: `Calendar / Day cell`, `Calendar / Event chip`, `Zmanim / Row`, `Zmanim / Group card`, `Agenda / Row`…
- **F8** (Low) — Icons (32:730). Icon set mixes Filled and Outlined Material Icons (e.g. `event` and `event_outlined`, `delete` and `delete_outlined`). *Why:* Visual texture inconsistency, visible in Settings. *Reviewer suggestion:* Standardise on Outlined (M3 default) except for selected states.
- **F9** (Nit) — Typography. Hebrew styles missing line heights (`hebrew/headline-small`, `hebrew/title-medium`, `hebrew/body-medium-on`); `body-medium-on` is an unexplained name. *Why:* Token hygiene. *Reviewer suggestion:* Fill in and rename.
- **F10** (Nit) — Foundations header text. "Design tokens mirrored from the Android app…" is excellent documentation; keep this practice and add a "Markers & chips legend" and "Date/time formatting" section (12h/24h, numerals, year formats). *Why:* Would resolve several inconsistencies above at the source. *Reviewer suggestion:* Add.
- **C1** (Critical) — Day cell component (7:46) and all month frames; type token `app/cell-micro` 9sp. The secondary date in each cell (Hebrew in Gregorian mode, Gregorian in Hebrew mode) is **9 sp**, and Hebrew numerals with geresh/gershayim (כ״ו, י״ט) at 9 sp are not legible; chip labels are 10–11 sp. *Why:* Fails the spec's own "scalable text, adequate contrast" requirement and WCAG target sizes; the Hebrew date is half the point of the app. *Reviewer suggestion:* Minimum 11 sp (labelSmall) for the secondary date, 12 sp for chips; consider a two-line cell (Gregorian top-left, Hebrew below) and reduce chip count (see C3). Test at 1.3× font scale.
- **D4** (Low) — Picker cells. Gregorian sub-dates at 9–10 sp (`picker/*-sub` tokens). *Why:* Same legibility problem as C1. *Reviewer suggestion:* 11 sp minimum.
- **R7** (Low) — RTL frames. Hebrew in Noto Sans Hebrew next to Roboto digits: weight mismatch (Noto Hebrew looks heavier/larger at the same sp). *Why:* Texture inconsistency. *Reviewer suggestion:* Set Hebrew styles 1 sp smaller or use a matched pair (e.g. Heebo/Rubik).
- **A1** (Medium) — Agenda (27:1431). Rosh Chodesh uses the **celebration (party-popper)** icon. *Why:* Semantics: Rosh Chodesh is a lunar event, not a party; the same icon presumably serves holidays. *Reviewer suggestion:* A crescent/moon icon for Rosh Chodesh; keep celebration for Yom Tov/Purim.
- **S3** (Medium) — Settings icons. The same twilight/candle icon is used for Candle lighting offset, all four Yahrzeit rows, and Halachic Times; Developer Settings and Personal Events share the person icon. *Why:* Icons stop carrying meaning. *Reviewer suggestion:* Yahrzeit rows: candle/memorial icon; Halachic times: schedule icon; Developer: code icon.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Type scale has no text below the agreed minimum and is verified at 1.3× font scale
- [ ] #2 Icon set is consistent (style and meaning)
- [ ] #3 Foundations page and theme code updated together
<!-- AC:END -->

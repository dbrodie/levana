---
id: TASK-57.7
title: Review zmanim calculations and add needed configuration
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-03T22:59:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: feature
ordinal: 77000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Review the zmanim shown and add configuration where needed: method visibility (the Z1 suggestion is not the right solution; coordinate with TASK-7, which covers calculation profiles), Havdalah/tzeit offset, candle-lighting presets (e.g. Jerusalem 40 min), and the Halachic times selection screen.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **Z1** (High) — Zmanim (13:236). No indication of **calculation opinion** for any zman (Alot: 72 min? 16.1°? Misheyakir 11°/10.2°? Tzeit 8.5°/ 3 stars / 72?). Only Shema shows GRA/MGA. *Why:* For this audience the shita is the data. A bare "Alot HaShachar 4:41" is unusable for someone who holds 72 fixed minutes. *Reviewer suggestion:* Secondary line per row: "16.1° · 72 min equiv." or settings-driven method label; an info icon opening a short explanation. Long-term: per-zman method setting.
- **Z3** (High) — Day sheet (21:660), Settings. **Havdalah has no configurable offset** (only candle lighting does). Havdalah 5:27 equals Tzeit 5:27 (37 min after a 4:50 sunset). *Why:* Many users hold 42, 50, 60 or 72 min; the single implicit value is a halachic choice made for the user without disclosure. *Reviewer suggestion:* Settings → "Havdalah: Tzeit (8.5°) / 42 min / 50 min / 72 min (Rabbeinu Tam)"; label the method in the Havdalah card.
- **Z4** (High) — Settings → Candle lighting offset dialog (49:3592). Slider with default 18 min and no presets; the sample city is **Jerusalem, whose minhag is 40 min**. The thumb sits at the far left, implying 18 is the minimum. *Why:* Wrong default for the most prominent sample location; sliders are a poor control for a discrete, minhag-driven value. *Reviewer suggestion:* Segmented presets 18 / 20 / 30 / 40 + "Custom"; per-city default (Jerusalem 40, Haifa 30, elsewhere 18); show the range.
- **S4** (Medium) — Halachic times screen (47:3078). Checkbox list with English names only, no times preview, no reordering; "Candle Lighting"/"Havdalah" listed as zmanim; the settings row says "3 shown in day panel" but it's unclear whether this controls the three *tiles*, the *list*, or both. *Why:* Ambiguous scope. *Reviewer suggestion:* Subtitle "Shown in the day panel and as quick tiles"; show Hebrew names and today's time per row; drag handles.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Decisions recorded (what is configurable and defaults)
- [ ] #2 Implemented and verified against known values
- [ ] #3 Figma updated
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Cross-reference from TASK-57.31 high-latitude research (2026-11-04)

Report: /Users/daniel/.paseo/evidence/task-57.31/high-latitude-zmanim-research.md (Parts 2.2, 3.3, 3.7). Relevant to the Z3 havdalah method and the method-visibility work:
- A havdalah/nightfall method picker (Tzeit 8.5° default / 42 / 50 / 72 min / custom) also resolves most high-latitude havdalah gaps: fixed-minute tzais exists whenever sunset exists (St. Petersburg 21 Jun 2027: 8.5° null, 72 min = 23:37, 50 min = 23:15). Label it as the method, not as a fallback, and note in the picker that fixed-minute options work in northern summers. KosherJava: getTzais72/getTzais50/getTzais60 or getTimeOffset(seaLevelSunset, minutes).
- One opt-in setting recommended, default off: "When nightfall or dawn isn't reached: use solar midnight (chatzot halayla)". Backing: Star-K sub-Arctic summer rule (Shabbos ends at chatzos halayla), Chabad.org calculator (dawn/misheyakir only, labelled "The Chabad custom ... consult your local rabbi"), Reykjavik/Helsinki/Oslo practice. KosherJava: ComplexZmanimCalendar.getSolarMidnight() (midnight after the date; for dawn use the previous date's). Never apply in polar day/night.
- No polar-region method (Ben Ish Chai 6/18, Sternbuch, Tiferes Yisrael) in v1; show "ask your rabbi".
- Note: Levana's Sof Zman Shema/Tefillah (MGA) use KosherJava's fixed 72-minute alos/tzais (getSofZmanShmaMGA), not 16.1°, so they stay available in white nights; the Z1 method label should say "72 min".
- Decisions needed before implementation are listed in the report, section 3.7 (defaults, alot fallback, Omer reminder behaviour, label wording).
<!-- SECTION:NOTES:END -->

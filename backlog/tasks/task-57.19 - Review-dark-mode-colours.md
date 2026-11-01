---
id: TASK-57.19
title: Review dark-mode colours
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
ordinal: 89000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Review dark-mode tonal separation (Shabbat column, sheet vs cards), saturated accents (delete icon) and device-calendar colours.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **K1** (Medium) — Dark month (37:75). Saturday column tint (#1B1D1F on #131517) is nearly invisible; "Candles"/parsha chips (#2A2C2F on #1B1D1F) are low-contrast grey-on-grey. *Why:* Shabbat emphasis disappears in dark mode. *Reviewer suggestion:* Use surfaceContainerHigh for the Shabbat column and labelled chips with primary-tinted text.
- **K2** (Medium) — Dark day sheet (37:211). Sheet (#1F2123) on background (#131517) with cards (#1F2123?) — cards and sheet are nearly the same value, so the card boundary relies on radius alone. *Why:* Hierarchy flattens. *Reviewer suggestion:* Sheet = surfaceContainer, cards = surfaceContainerLowest (#0E1012) or High; verify ≥ 2 tonal steps.
- **K3** (Low) — Dark events list (37:560). Delete icons #FFB4AB on dark cards are the only saturated colour on screen — they dominate. *Why:* Destructive affordance gets visual priority. *Reviewer suggestion:* See E2; otherwise use onSurfaceVariant for the trash and error only on press.
- **K4** (Low) — Dark agenda (37:746). Device-calendar orange icon is full-saturation on dark; M3 suggests tone-mapping third-party colours (harmonise). *Why:* Harsh. *Reviewer suggestion:* Harmonise calendar colours to the palette in dark.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Dark palette adjustments agreed in Figma and applied in Theme.kt
- [ ] #2 Verified on the emulator
<!-- AC:END -->

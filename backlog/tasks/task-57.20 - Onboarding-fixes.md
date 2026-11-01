---
id: TASK-57.20
title: Onboarding fixes
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
ordinal: 90000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Onboarding improvements: page count and order (language/region early), the >9-page counter fallback, and the denied-permission action. Related: TASK-55 (single source for walkthrough content).

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **O3** (Medium) — Walkthrough flow. 9 pages, Israel/Diaspora never asked, language choice is on page **9**. A Hebrew speaker on an English-system device reads 8 English pages first. *Why:* Long first-run; the one decision that changes halachic output is auto-detected silently. *Reviewer suggestion:* Put language + Israel/Diaspora confirmation on page 1 ("Detected: Israel · Change"). Cut to 5–6 pages; merge Widgets+Appearance; merge Agenda into Calendar.
- **O4** (Low) — Bottom bar (51:437). Dots are centred while Back/Next are at the edges; the dots row has 9 dots at 8 dp — fine, but "n of m" fallback above 9 pages is spec'd, indicating awareness the flow is at its limit. *Why:* Signal to shorten. *Reviewer suggestion:* See O3.
- **O5** (Low) — Permission panel Denied variant (51:454). "Not allowed. Everything else works, and you can allow it later from Settings." still offers "Use my location" — on permanent denial this button can't open the system prompt. *Why:* Dead button. *Reviewer suggestion:* Change CTA to "Open settings" in the Denied state.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Changes designed in Figma and implemented
<!-- AC:END -->

---
id: TASK-57.15
title: Present yahrzeit information more richly
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: low
type: feature
ordinal: 85000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make yahrzeit entries nicer to read (e.g. which yahrzeit it is, the person's Hebrew name). The event-type model proposed in E1 is out of scope. (Z7 — yahrzeit count and Hebrew name on the sheet row — is tracked in TASK-57.4.)

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **E1** (High) — Add event (24:971), Edit event (24:1076), Events list (23:920). There is **no event type**. A yahrzeit — the most important personal date in this app — is "an event with the Adar-rules switch on". Anniversaries and yahrzeits share a generic calendar icon in the list; only the "Yahrzeit rules" caption differs. *Why:* Type drives icon, colour, recurrence rule, reminder text ("Light a candle", Kaddish), first-year logic and the "nth yahrzeit" count. *Reviewer suggestion:* Type chips at the top of the form: Yahrzeit · Birthday · Anniversary · Other. Yahrzeit adds "Hebrew name", "Relationship", "Year of passing" (auto first-yahrzeit rule).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Yahrzeit presentation design agreed in Figma and implemented
<!-- AC:END -->

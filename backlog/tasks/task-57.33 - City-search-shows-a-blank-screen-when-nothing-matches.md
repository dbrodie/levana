---
id: TASK-57.33
title: City search shows a blank screen when nothing matches
status: To Do
assignee: []
created_date: '2026-11-02T14:28:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
type: feature
ordinal: 103000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-57.23 (ST2 empty states). Cities > Add location, searching for a string with no match ('zzqxw') leaves only the search field and an empty page, in English and Hebrew. The other listed empty states have messages: Agenda search ('Nothing matches “…” in the past or next 12 months'), Events ('No custom events yet / Tap + to add a custom event'), Birthdays ('No birthdays yet'), saved cities (the Add location row). Evidence: /Users/daniel/.paseo/evidence/task-57.23/22-city-search-nomatch.png, /Users/daniel/.paseo/evidence/task-57.23/75-he-city-search-nomatch.png; for comparison /Users/daniel/.paseo/evidence/task-57.23/13-agenda-search-noresults.png, /Users/daniel/.paseo/evidence/task-57.23/17-events-tab-empty.png.

Add a no-match message reusing the Agenda search pattern; if TASK-57.17 adds custom coordinates, the message could point to it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 City search with no match shows a message in English and Hebrew
- [ ] #2 Design matches the existing Agenda search no-results pattern
<!-- AC:END -->

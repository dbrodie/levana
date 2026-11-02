---
id: TASK-57.27
title: 'Figma: write Hebrew years in the short form (תשפ״ז) to match the app'
status: To Do
assignee: []
created_date: '2026-11-02T09:23:00Z'
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
- [ ] #1 No frame in the Figma file shows a Hebrew year in the long form; all match the app's short form
<!-- AC:END -->

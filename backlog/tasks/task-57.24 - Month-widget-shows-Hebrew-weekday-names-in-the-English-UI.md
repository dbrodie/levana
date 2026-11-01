---
id: TASK-57.24
title: Month widget shows Hebrew weekday names in the English UI
status: To Do
assignee: []
created_date: '2026-11-01T15:05:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
ordinal: 95000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while fixing TASK-57.9. MonthWidget.kt (MonthContent) names the weekday header with a hardcoded HEBREW_LOCALE (TextStyle.NARROW), commented 'The calendar's Hebrew grid names the days in Hebrew in either language'. That mirrors the in-app Hebrew grid's old behaviour, which TASK-57.9 changed: weekday names now follow the UI language in both calendar modes. The widget should use context.widgetLocale() too (already in scope as `locale`). Note NARROW English names are ambiguous (S M T W T F S); decide whether to keep NARROW or use SHORT in the widget's narrow cells, and update the widget snapshots.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Month widget weekday labels follow the UI language (English and Hebrew), covered by snapshots
<!-- AC:END -->

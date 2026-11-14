---
id: TASK-57.24
title: Month widget shows Hebrew weekday names in the English UI
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T15:05:00Z'
updated_date: '2026-11-14T21:44:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. MonthWidget.MonthContent: name the weekday header with context.widgetLocale() (already in scope) instead of the hardcoded HEBREW_LOCALE; drop the constant.
2. NARROW vs SHORT: check the in-app grid (CalendarScreen uses TextStyle.SHORT) and the Figma Widgets frames; pick the style consistent with them and record the decision in the notes. Check SHORT fits 11sp bold in the compact (180dp) widget's ~23dp columns for English and Hebrew.
3. Snapshots: there are no widget Roborazzi baselines yet. Add a Robolectric test that renders MonthContent with Glance's GlanceRemoteViews.compose (experimental) for a fixed snapshot, applies the RemoteViews to a view and captures it with Roborazzi: English and Hebrew, light, compact and full sizes. Make MonthContent internal (or add an internal entry point) for the test.
4. Build/test/lint + recordRoborazziDebug/verifyRoborazziDebug; emulator check with the month widget placed in English and Hebrew UI; evidence under /Users/daniel/.paseo/evidence/task-57.24/.
<!-- SECTION:PLAN:END -->

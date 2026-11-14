---
id: TASK-57.24
title: Month widget shows Hebrew weekday names in the English UI
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T15:05:00Z'
updated_date: '2026-11-14T21:52:00Z'
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
- [x] #1 Month widget weekday labels follow the UI language (English and Hebrew), covered by snapshots
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. MonthWidget.MonthContent: name the weekday header with context.widgetLocale() (already in scope) instead of the hardcoded HEBREW_LOCALE; drop the constant.
2. NARROW vs SHORT: check the in-app grid (CalendarScreen uses TextStyle.SHORT) and the Figma Widgets frames; pick the style consistent with them and record the decision in the notes. Check SHORT fits 11sp bold in the compact (180dp) widget's ~23dp columns for English and Hebrew.
3. Snapshots: there are no widget Roborazzi baselines yet. Add a Robolectric test that renders MonthContent with Glance's GlanceRemoteViews.compose (experimental) for a fixed snapshot, applies the RemoteViews to a view and captures it with Roborazzi: English and Hebrew, light, compact and full sizes. Make MonthContent internal (or add an internal entry point) for the test.
4. Build/test/lint + recordRoborazziDebug/verifyRoborazziDebug; emulator check with the month widget placed in English and Hebrew UI; evidence under /Users/daniel/.paseo/evidence/task-57.24/.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: eb6b35b (fix: month widget weekday names follow the app language).

Decision (NARROW vs SHORT): language-dependent. English uses TextStyle.SHORT (Sun Mon … Sat), matching the in-app grid (CalendarScreen DayHeaderRow uses SHORT) and avoiding the ambiguous S M T W T F S. Hebrew uses TextStyle.NARROW (א׳ … ש׳): the in-app grid's SHORT Hebrew names (יום א׳) don't fit the compact widget's ~23dp columns, and the narrow letters are unambiguous and are what the Figma widget frame shows. The Figma 'Widget / Month' frame (54:91) still shows Hebrew letters in the English frame (the pre-TASK-57.9 behaviour); it should be updated to show Sun…Sat in English (not done here; Figma writes were out of scope).

Implementation: MonthWidget.kt — dropped HEBREW_LOCALE; header uses context.widgetLocale() with the style above. Extracted internal MonthWidgetContent(snapshot, light, dark) from provideGlance (no behaviour change) so a test can render it.

Snapshots: new MonthWidgetScreenshotTest (Robolectric + Roborazzi) renders MonthWidgetContent via GlanceRemoteViews.compose (experimental API), applies the RemoteViews in an activity and captures them: full and compact × en/he × light/dark (8 baselines, widget_month_*.png, 18 Cheshvan 5787). No widget baselines existed before.

Results: ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug — BUILD SUCCESSFUL (exit 0).
Emulator (Levana_Agent_Template_API_36_1, Pixel launcher, English device): placed Levana · Month (4×4 full size). English app language: header Sun Mon Tue Wed Thu Fri Sat. Hebrew app language (cmd locale set-app-locales iw): header א׳ … ש׳, widget mirrored RTL. No AndroidRuntime fatals.

Limitations: compact size verified only in Robolectric snapshots, not on the emulator. English SHORT names fit the 180dp compact cells at default font size but are tight; at large system font scales they may clip (maxLines = 1), as the widget's other 11sp text would.

Evidence: /Users/daniel/.paseo/evidence/task-57.24/ (emulator_month_widget_en.png, emulator_month_widget_he.png, widget_month_*.png, build.log, build_summary.txt).

Store listing: none — widgets are not in the store screenshots (STORE_LISTING.md: 'widgets' feature has no related shots); no label added.
<!-- SECTION:NOTES:END -->

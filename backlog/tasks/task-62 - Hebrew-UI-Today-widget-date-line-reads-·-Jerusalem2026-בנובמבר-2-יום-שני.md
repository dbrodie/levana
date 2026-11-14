---
id: TASK-62
title: 'Hebrew UI: Today widget date line reads ''· Jerusalem2026 בנובמבר 2 ,יום שני'''
status: Done
assignee:
  - '@claude'
created_date: '2026-11-02T14:16:00Z'
updated_date: '2026-11-14T21:54:00Z'
labels:
  - design-review
dependencies: []
priority: low
ordinal: 99000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-57.26 runtime check (evidence: /Users/daniel/.paseo/evidence/task-57.26/05-he-12h-today-widget.png). In the Hebrew UI the Today widget's subtitle 'יום שני, 2 בנובמבר 2026 · Jerusalem' is reordered by the bidi algorithm: the Latin city name sits against the year ('Jerusalem2026') and the '·' separator ends up at the far left. The Zmanim widget header ('כ״ב חשון · Jerusalem') renders correctly. Likely fix: isolate the Latin city name (FSI…PDI) or build the line so each part is its own bidi run, as TASK-57.26 did for times.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 In the Hebrew UI the Today widget subtitle reads date, separator, city in RTL order
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Cause: the widget TextViews lay out their paragraph in the device direction (LTR on an English device with Hebrew app language, where the widget is mirrored). The Today widget's location label is ' · <city>', so in an LTR paragraph the leading separator stays at the far left and the city touches the date, whatever script the city name is in. The Zmanim header avoids this by writing '<city> · ' when mirrored, which only works while the paragraph direction is LTR.
1. widget/WidgetCommon.kt: add widgetLocationSuffix(city, hebrewUi) returning ' · <city>' wrapped in a directional isolate of the UI direction (RLI…PDI for Hebrew, LRI…PDI otherwise) with the city in its own first-strong isolate (FSI…PDI). The text then reads separator-then-city in the UI direction whatever the TextView's paragraph direction; isolate chars built from Char constants (lint BidiSpoofing).
2. TodayWidget.DateBlock: use it for the location label (mirror ordering of the Row and alignment unchanged).
3. Leave ZmanimWidget as is (renders correctly; not in scope). Do not touch MonthWidget.
4. Unit test WidgetLocationSuffixTest: exact strings for Hebrew/English UI with Latin and Hebrew city names, plus a java.text.Bidi check that in both LTR and RTL paragraphs the visual order puts the separator between date and city.
5. No Glance Roborazzi infrastructure exists in the repo (no widget snapshot tests); verification of rendering is on the emulator: Hebrew UI on English device (mirrored) with Jerusalem, Latin-name city, and English UI with a Hebrew name if feasible.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: 117e3aa (code). Branch task-62-today-widget-bidi-subtitle.

Cause: the widget TextViews lay their paragraph out in the device direction. With Hebrew app language on an English device (mirrored widget) the location label ' · <city>' was an LTR paragraph, so the leading separator stayed at the far left and the city touched the year. This happened with Latin, Hebrew ('ירושלים') and fallback ('מיקום נוכחי') names alike.

Change
- WidgetCommon.widgetLocationSuffix(city, hebrewUi): ' · <city>' wrapped in an isolate of the app-language direction (RLI…PDI for Hebrew, LRI…PDI for English), with the city in its own first-strong isolate (FSI…PDI). Isolate chars come from Char constants, as in ClockTime (lint BidiSpoofing). The result no longer depends on the TextView's paragraph direction, so it also holds for an RTL device (non-mirrored).
- TodayWidget.DateBlock uses it. ZmanimWidget not changed: it renders correctly and its mirror trick ('city · ') still holds. MonthWidget not touched.
- WidgetLocationSuffixTest: exact strings, plus java.text.Bidi visual order in both LTR and RTL paragraphs for Hebrew UI (Latin city, Hebrew city, fallback) and English UI (Latin city, Hebrew city).

Verification
- ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug: BUILD SUCCESSFUL, 1304 tests, 0 failures. No Roborazzi baseline changed.
- Emulator (own session, Levana_Agent_Template_API_36_1, device en-US, GPS fix Jerusalem), evidence in /Users/daniel/.paseo/evidence/task-62/:
  01-he-today-widget-jerusalem.png: Hebrew app language, the line reads 'יום שבת, 14 בנובמבר 2026 · ירושלים' in RTL order (was '· ירושלים2026 …', cf. task-44/06).
  02-en-today-widget-jerusalem.png: English UI unchanged, 'Saturday, November 14, 2026 · Jerusalem'.

Limitations
- No Roborazzi snapshot for the Today widget. The repo has no Glance widget snapshot setup (no glance-appwidget testing dependency, no widget snapshot tests), and Robolectric would not show this bidi issue reliably anyway. The ordering is covered by the java.text.Bidi unit test and the emulator check. A widget snapshot setup could be filed as its own task.
- Not checked at runtime: a Latin city name in the Hebrew UI (the geocoder gives Jerusalem a Hebrew name), a Hebrew name in the English UI (the English UI never shows a Hebrew name, see TASK-44), and an RTL device locale. The unit test covers all three.
<!-- SECTION:NOTES:END -->

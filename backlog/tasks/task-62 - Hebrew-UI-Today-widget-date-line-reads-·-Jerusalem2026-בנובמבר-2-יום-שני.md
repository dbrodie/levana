---
id: TASK-62
title: 'Hebrew UI: Today widget date line reads ''· Jerusalem2026 בנובמבר 2 ,יום שני'''
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T14:16:00Z'
updated_date: '2026-11-14T21:44:00Z'
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
- [ ] #1 In the Hebrew UI the Today widget subtitle reads date, separator, city in RTL order
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
TASK-44 (2026-11-08): in the Hebrew UI the current location is now named in Hebrew when the geocoder has a Hebrew name (e.g. 'ירושלים'), else in Latin script, or the Hebrew fallback 'מיקום נוכחי'. The bug still happens with a Hebrew name: the line renders '· ירושלים2026 בנובמבר 8 ,יום ראשון' and '· מיקום נוכחי2026…' (evidence /Users/daniel/.paseo/evidence/task-44/06-he-today-widget-jerusalem.png, 14-he-offline-today-widget-fallback.png). The cause is not only the Latin name; the fix here is still needed.
<!-- SECTION:NOTES:END -->

---
id: TASK-44
title: >-
  Current-location name can come back in a foreign language (Arabic for
  Jerusalem)
status: To Do
assignee: []
created_date: '2026-10-29T21:03:00Z'
updated_date: '2026-10-29T21:36:00Z'
labels: []
dependencies: []
priority: low
type: bug
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-9.3 emulator verification (2026-10-29). With a GPS fix at Jerusalem (31.7683, 35.2137) on the Levana_Agent_Template_API_36_1 emulator (device locale en-US), the reverse-geocoded current-location name was stored as 'القدس' (Arabic). It shows in the sidebar's ACTIVE CITY and in the Today/Zmanim widgets' headers, in both the English and Hebrew app languages. AndroidReverseGeocoder (data/ReverseGeocoder.kt) calls Geocoder(context, Locale.getDefault()). It may be the emulator's geocoder backend; check on a real device first. Screenshot: /tmp/task93-widget-shots/07-he-dark-purim-crop.png
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Current-location name is shown in the app language (English or Hebrew) or a sensible fallback, never an unrelated script
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Also seen 2026-10-29: in the Today widget's Hebrew Gregorian line the Arabic name runs into the year without separation ('…באוקטובר 2026 · القدس' renders as 'القدس2026 ·'), a bidi-isolation issue when the location's script differs from the app language. Screenshot: /tmp/task93-widget-shots/13-he-today-and-month-page.png
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-29T21:36:00Z

---

Coordinator 2026-10-29: also fold in the related fallback-name issue. When reverse geocoding yields no name, the current location is labelled with the hardcoded English 'Current Location' (LocationService fallback), so it shows in English in the Hebrew UI (e.g. /tmp/task93-widget-shots/06-he-dark-default-crop.png). The fallback should be a localized string resolved at display time. Also seen: the Hebrew Today-widget date line runs the name into the year ('القدس2026').

---
<!-- COMMENTS:END -->

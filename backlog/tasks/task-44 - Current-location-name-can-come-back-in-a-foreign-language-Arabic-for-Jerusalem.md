---
id: TASK-44
title: >-
  Current-location name can come back in a foreign language (Arabic for
  Jerusalem)
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T21:03:00Z'
updated_date: '2026-11-08T09:38:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Root cause (emulator Levana_Agent_Template_API_36_1, en-US, GPS 31.7683,35.2137, probe logging all Address fields for several locales, 2026-11-08):
- The locale passed is right (Locale.getDefault() = en_US) and the geocoder honours it (countryName 'Israel' / 'ישראל').
- The nearest result is an address whose data exists only in Arabic: locality 'القدس', adminArea 'منطقة القدس', line 'القدس 3، القدس، Israel' — for en_US, en AND he alike. Results 2 and 3 have locality 'Jerusalem' (en) / 'ירושלים' (he).
- AndroidReverseGeocoder asks for maxResults = 1 and takes locality ?: subAdminArea ?: adminArea unchecked, so the Arabic-only nearest address wins. It is looked up once in the device locale and stored as a single string, so it also ignores the per-app language and a later language switch.
- The no-name fallback is the constant 'Current Location' stored as the name, so it stays English.

Plan:
1. ReverseGeocoder takes a locale and returns candidate names from up to 5 addresses (all localities first, then subAdminAreas, then adminAreas). AndroidReverseGeocoder stays async (GeocodeListener), off the main thread.
2. Pure pickPlaceName(candidates, language): first candidate whose letters are all in a script that fits the language (en: Latin; he: Hebrew or Latin, as the city list is English in the Hebrew UI).
3. resolvePlaceNames: looks up English and Hebrew in parallel when the fix is taken, each with a 5s timeout; errors, offline or no fitting name leave that language out. Stored with the fix as Location.localizedNames {en, he}. No lookup at display time.
4. PreferencesRepository resolves the fix's name at read time for the current app language (withAppLocale): he → he ?: en ?: fallback; en → en ?: fallback. The fallback is the string resource current_location_fallback (en/he). Legacy stored fixes (no names): the stored name is used if it fits the language, else the fallback (covers old 'Current Location' and 'القدس'); the next fix (on resume) re-resolves them.
5. preferences re-emits on a locale change (ComponentCallbacks on the app context, only when the locales change), so the sidebar, Cities, widgets, tile and notifications follow an app-language switch without a new lookup.
6. Tests: fake geocoder per locale (Arabic-first results, he/en), script picking, offline/timeout → fallback in en and he, language switch after the name was stored, legacy names.
7. Runtime check: Jerusalem + one other city, en then he, sidebar/Cities/widgets, then no network.
<!-- SECTION:PLAN:END -->

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

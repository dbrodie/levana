---
id: TASK-44
title: >-
  Current-location name can come back in a foreign language (Arabic for
  Jerusalem)
status: Done
assignee:
  - '@claude'
created_date: '2026-10-29T21:03:00Z'
updated_date: '2026-11-12T22:12:00Z'
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
- [x] #1 Current-location name is shown in the app language (English or Hebrew) or a sensible fallback, never an unrelated script
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

## Result (2026-11-08, @claude)
Root cause (probe: /Users/daniel/.paseo/evidence/task-44/00-geocoder-probe-jerusalem.txt): the locale was right and the geocoder honours it. The nearest address at Jerusalem's coordinates has Arabic-only data in every locale (en_US, en, he); the 2nd and 3rd results are 'Jerusalem' / 'ירושלים'. The app read 1 result's locality unchecked, in the device locale only, and stored it as one string. The no-name fallback was the English constant stored as the name.

Changes:
- ReverseGeocoder.placeNames(lat, lon, locale): up to 5 addresses; all localities, then subAdminAreas, then adminAreas. Still the async GeocodeListener API (off the main thread, TASK-19).
- pickPlaceName: English takes Latin script only; Hebrew prefers Hebrew script, then Latin. Digits and punctuation are ignored.
- resolvePlaceNames: English and Hebrew looked up in parallel when the fix is taken, 5s timeout each; failure, offline or no fitting name leaves that language out. Stored with the fix as Location.localizedNames (name = the English one, for older readers).
- PreferencesRepository resolves the fix's name on every read in the app language (withAppLocale): he → he ?: en ?: fallback; en → en ?: fallback. Fallback = new string current_location_fallback ('Current Location' / 'מיקום נוכחי'). Fixes stored before this change: their name is kept if it fits the language; otherwise (old 'Current Location', 'القدس') the fallback is shown until the next fix (taken on resume) renames it.
- preferences re-emits on a locale change (device/LocaleChanges.kt: ComponentCallbacks, only when the locales change), so open screens follow a language switch without a new lookup. Widgets already refresh on config change (LevanaApplication).
- Side effect: a language switch changes currentLocation.name, so readers that diff the device position (notification rebuild, the fasts daily-refresh key) may recompute once. Harmless and idempotent.
- Tests: ResolvePlaceNamesTest (fake geocoder per locale with Arabic-first results, script picking, offline/timeout/one language failing, cancellation, display choice, legacy names), CurrentLocationNameTest (Robolectric: en/he name, localized fallback in both, language switch after storing, legacy stored names), LocationServiceTest updated. The old ResolvePlaceNameTest is replaced.

Verification, tested commit 488bc19 (on 2bf057c):
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug`: BUILD SUCCESSFUL. 1265 tests, 0 failures/errors (8 skipped). No Roborazzi baseline changes.
- Emulator Levana_Agent_Template_API_36_1 (emulator-5556, own session), device en-US. The Jerusalem and New York checks ran on 888e417 (488bc19 only changes the Hebrew script preference); the offline check ran on 488bc19's code.
  - Jerusalem fix: stored localizedNames {en: Jerusalem, he: ירושלים}. English: sidebar ACTIVE CITY 'Jerusalem' (02), Cities 'Jerusalem' (03). After switching to Hebrew (cmd locale set-app-locales, the same LocaleManager API the setting uses) with Cities open, the row changed to 'ירושלים' at once, with no new lookup (04). Sidebar 'ירושלים' (05), Today widget (06), Zmanim widget 'כ״ח חשון · ירושלים' (07). Back to English: both widgets show 'Jerusalem' (08, 09).
  - Upgrade path: the probe build had stored 'القدس'; after installing the fix it was replaced on the next resume.
  - New York: the 1st fix was stored unnamed (lookup gave nothing, likely the 5s timeout on a cold geocoder); the next fix gave {en: New York, he: New York}.
  - Offline (wifi + data off), London fix: stored unnamed. English: sidebar and Cities 'Current Location' (10, 11). Hebrew: Cities, sidebar, Today and Zmanim widgets 'מיקום נוכחי' (12–15).
- Evidence: /Users/daniel/.paseo/evidence/task-44/ (00–15).

Limitations:
- The emulator's Google geocoder may answer differently from a real device; not checked on a real device. The Hebrew geocoder answered 'New York' in Latin script; whether real devices return 'ניו יורק' is not known.
- The tile and notifications don't show the city name today; they get the same resolved name through preferences if they ever do.
- An intermittent lookup miss stores no name until the next fix (on resume). No retry was added.
- The Today widget bidi issue (TASK-62) is unchanged and happens with Hebrew names too (06, 14); noted on TASK-62.
- Store listing: no store screenshots or store text change (store screenshots use saved Jerusalem fixtures without localized names; the baselines are unchanged).
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-29T21:36:00Z

---

Coordinator 2026-10-29: also fold in the related fallback-name issue. When reverse geocoding yields no name, the current location is labelled with the hardcoded English 'Current Location' (LocationService fallback), so it shows in English in the Hebrew UI (e.g. /tmp/task93-widget-shots/06-he-dark-default-crop.png). The fallback should be a localized string resolved at display time. Also seen: the Hebrew Today-widget date line runs the name into the year ('القدس2026').

---
<!-- COMMENTS:END -->

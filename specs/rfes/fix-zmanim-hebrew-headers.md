# Fix: zmanim section headers stay in English in Hebrew

**Status:** In progress

## Problem

On the full zmanim screen (calendar -> swipe up the day panel -> "More halachic
times"), the section headers MORNING, AFTERNOON, EVENING and NIGHT are shown in
English even when the app language is Hebrew. `ZmanGroup` in
`ui/zmanim/ZmanimScreen.kt` maps each `ZmanCategory` to a hardcoded English
string.

## Fix

- Add four string resources (`zmanim_category_morning`, `_afternoon`,
  `_evening`, `_night`) to `values/strings.xml` with the current English words
  ("Morning", "Afternoon", "Evening", "Night").
- Add Hebrew translations to `values-iw/strings.xml`:
  בוקר, צהריים, ערב, לילה.
- In `ZmanGroup`, resolve the label with `stringResource(...)` and keep the
  existing `.uppercase()` call, so English still renders as MORNING /
  AFTERNOON / EVENING / NIGHT (uppercasing is a no-op for Hebrew).
- Only the headers change. Zman names, Hebrew sub-names and order are untouched.

## Test plan

- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
- Emulator: open the full zmanim screen in English (headers unchanged, upper
  case) and in Hebrew (headers in Hebrew).

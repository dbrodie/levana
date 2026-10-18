# Fix: untranslated English strings in Hebrew mode

## Problem
With the app language set to Hebrew, several user-visible strings remain English:

1. Calendar month header subtitle. In Gregorian mode it shows the Hebrew month
   range transliterated with an Arabic-numeral year ("Tishrei–Cheshvan 5787").
   In Hebrew mode it shows a Gregorian range formatted with the JVM default
   locale ("Sep 12, 2026 – Oct 11, 2026"). The strings are built once in
   `CalendarViewModel` using `Locale.getDefault()` and cached in state, so they
   do not follow the in-app (per-app) locale and go stale when it changes,
   because the ViewModel survives the configuration change.
2. My events screen: tab labels, empty states, FAB content description and
   other texts are hardcoded English literals in Compose code.
3. Other hardcoded English literals (`Text("...")`, `contentDescription = "..."`)
   across the `ui/` package.

## Fix
- Move calendar header formatting out of the ViewModel into a pure formatter
  (`CalendarHeaderFormatter`) invoked from the composable with the current
  `LocalConfiguration` locale.
  - Hebrew locale: Hebrew month names (KosherJava `HebrewDateFormatter`,
    Hebrew format) and gematria year, e.g. "תשרי–חשון תשפ״ז"; Gregorian range
    formatted with the Hebrew locale.
  - English/other locales: output identical to before
    ("Tishrei–Cheshvan 5787", "Sep 12, 2026 – Oct 11, 2026", "תשרי 5787").
- Replace hardcoded user-visible literals in `ui/` with string resources, and
  add Hebrew translations in `values-iw`. English text stays identical.
- Developer-only debug settings may stay English (not user-facing).

## Test plan
- Unit tests for `CalendarHeaderFormatter` (English output unchanged, Hebrew
  output uses Hebrew month names + gematria year, Gregorian range localized).
- `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest` pass.
- Emulator: switch app language to Hebrew, verify the calendar subtitle in both
  modes and the My events screen; switch back to English and verify unchanged.

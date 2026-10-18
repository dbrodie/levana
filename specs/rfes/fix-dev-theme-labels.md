# Fix: Developer "Force Holiday Theme" labels

## Problem
Settings → Developer Settings → "Force Holiday Theme" dropdown shows raw enum
names (`CHANUKAH`, `ROSH_HASHANAH`, `SHMINI_ATZERET`, ...) both in the menu and
in the closed field. "None" is a hardcoded English literal.

## Fix
- Add a `@StringRes` label per `HolidayTheme` entry (exhaustive `when`, so new
  entries cannot be added without a label), plus a localized "None" string.
- English and Hebrew (`values-iw`) string resources. Spelling matches the
  holiday names used by `HolidayMapper` on the calendar (e.g. "Shemini Atzeret",
  "Rosh Hashanah", "Sukkot", "Chanukah"); `FAST_DAY` is "Fast day" / "יום צום".
- `HolidayThemePicker` iterates `HolidayTheme?` options (null = None) and shows
  the label resource for both menu items and the selected value.
- Persisted value is unchanged: selection still emits `HolidayTheme.name`
  (or null for None); stored values are parsed with `HolidayTheme.valueOf` as
  before. Unknown stored values display as "None".

## Test plan
- Unit test: every `HolidayTheme` entry maps to a distinct label resource.
- `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest` pass.
- Emulator: open Developer Settings, verify readable labels in menu and closed
  field (English), select a theme and confirm the theme accent changes, reset
  to None.

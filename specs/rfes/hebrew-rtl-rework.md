# Hebrew/RTL Rework

## Context

The current `hebrewPrimary` preference conflates two separate concerns:
1. **Calendar view mode** — whether the month view uses Hebrew months
2. **App-wide language/direction** — RTL layout + Hebrew string resources

## Goals

- Decouple calendar Hebrew toggle from app language/direction.
- The calendar header toggle switches the month view between Hebrew and Gregorian, keeping the calendar **always LTR**, regardless of the app language setting.
- A new **App Language** setting controls whether the whole app uses Hebrew strings + RTL direction (or follows the system locale).

## Design

### Calendar Hebrew Toggle (month-view only, always LTR)
- Rename `hebrewPrimary` → `calendarHebrewMode` throughout all calendar-related files.
- DataStore key stays `"hebrew_primary"` (no migration needed).
- Remove the `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` wrapper around the Hebrew calendar content in `CalendarScreen`.
- Add a `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` wrapper around the entire calendar content block so the calendar is always LTR even when the app is in RTL mode.
- Remove the inner `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` overrides inside `HebrewMonthHeader` (redundant once the parent is always LTR).
- Remove the "Hebrew-Primary Mode" toggle from `SettingsScreen`.

### App Language Setting (new)
- Uses Android's `LocaleManager` API (API 33+; `minSdk = 34` so always available).
- No DataStore needed — `LocaleManager` persists the locale itself.
- Setting Hebrew: `localeManager.applicationLocales = LocaleList.forLanguageTags("iw")`
- Resetting to system: `localeManager.applicationLocales = LocaleList.getEmptyLocaleList()`
- Android **recreates the Activity automatically** when locale changes.
- `SettingsViewModel` reads `localeManager.applicationLocales` on init to populate state.
- Requires `android:localeConfig="@xml/locales_config"` in manifest.

### DayOfWeekHeader
- Keep `calendarHebrewMode`-based locale logic: `Locale("he")` when in Hebrew calendar mode, otherwise `Locale.getDefault()`.

## Files Changed

### New
- `app/src/main/res/xml/locales_config.xml` — declares supported locales (en, iw)
- `app/src/main/java/com/levana/app/ui/settings/AppLanguage.kt` — `enum class AppLanguage { SYSTEM, HEBREW }`

### Modified
- `domain/model/UserPreferences.kt` — `hebrewPrimary` → `calendarHebrewMode`
- `data/PreferencesRepository.kt` — rename field/method, keep DataStore key
- `ui/calendar/CalendarState.kt` — rename field
- `ui/calendar/CalendarIntent.kt` — `ToggleHebrewPrimary` → `ToggleCalendarHebrewMode`
- `ui/calendar/CalendarViewModel.kt` — all renames + updated toggle handler
- `ui/calendar/CalendarScreen.kt` — remove RTL wrapper, add LTR wrapper, remove inner overrides, rename param
- `ui/settings/SettingsState.kt` — remove `hebrewPrimary`, add `appLanguage: AppLanguage`
- `ui/settings/SettingsIntent.kt` — remove `SetHebrewPrimary`, add `SetAppLanguage`
- `ui/settings/SettingsViewModel.kt` — add Context param, read/write `LocaleManager`, remove old Hebrew handling
- `ui/settings/SettingsScreen.kt` — remove Hebrew-Primary toggle, add App Language radio section
- `AndroidManifest.xml` — add `android:localeConfig="@xml/locales_config"`
- `di/AppModules.kt` — pass `androidContext()` to `SettingsViewModel`

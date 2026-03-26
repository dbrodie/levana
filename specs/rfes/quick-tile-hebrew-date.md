# Quick Settings Tile — Hebrew Date

## Summary

Add an Android Quick Settings tile that displays today's Hebrew date in the notification shade. Tapping the tile opens the app to today's calendar date.

## Motivation

Users want quick access to the current Hebrew date without opening the app. A QS tile is the most natural Android surface for this — always visible with a single swipe.

## Behaviour

- **Label:** Today's Hebrew date formatted via `HebrewDateFormatter`, in Hebrew script when the app language is Hebrew, transliterated otherwise.
- **Subtitle:**
  - If today is a Yom Tov / holiday: the holiday name (English or Hebrew per app language).
  - Else if today is Rosh Chodesh: "Rosh Chodesh" / "ראש חודש".
  - Otherwise: the day-of-week display name.
- **Tap:** Opens `MainActivity` navigated to today's date (via `EXTRA_DATE_EPOCH_DAY`).
- **State:** Always `Tile.STATE_ACTIVE` (the tile is informational, not a toggle).

## Implementation

### New files

- `app/src/main/java/com/levana/app/quicktile/HebrewDateTileService.kt` — `TileService` subclass. Pure computation using KosherJava directly; no Koin/ViewModel needed.
- `app/src/main/res/drawable/ic_tile_calendar.xml` — 24×24dp vector calendar icon.

### Modified files

- `app/src/main/res/values/strings.xml` — add `tile_name = "Hebrew Date"`.
- `app/src/main/res/values-iw/strings.xml` — add `tile_name = "תאריך עברי"`.
- `app/src/main/AndroidManifest.xml` — declare the `<service>` with `BIND_QUICK_SETTINGS_TILE` permission.

### Key reuse

| Artifact | Location | Use |
|---|---|---|
| `NotificationPoster.EXTRA_DATE_EPOCH_DAY` | `notifications/NotificationPoster.kt` | Deep-link intent extra |
| `HolidayMapper.mapHoliday()` | `data/HolidayMapper.kt` | Holiday name for subtitle |
| `JewishCalendar` / `HebrewDateFormatter` | KosherJava (existing dep) | Date computation |
| `LocaleManager` locale detection | `ui/settings/SettingsViewModel.kt` | Hebrew language detection |
| Deep-link handling | `MainActivity.kt` | Already navigates to date via `EXTRA_DATE_EPOCH_DAY` |

## Constraints

- `minSdk = 34` — `startActivityAndCollapse(PendingIntent)` (API 34 signature) is safe to call directly.
- No network, location, or other permissions needed — date is computed locally.
- No Koin injection — `TileService` has a system-managed lifecycle; pure computation avoids lifecycle complexity.

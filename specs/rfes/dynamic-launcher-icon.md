# Dynamic Hebrew Day Launcher Icon

## Summary

Replace the app launcher icon with a new Material Design 3 design (navy gradient background, gold moon crescent, white calendar square), then make the calendar square show the current Hebrew day-of-month letter — updating daily so the launcher icon always reflects today's Hebrew date.

## Motivation

The Quick Settings tile already shows the Hebrew day letter dynamically. The launcher icon should offer the same at-a-glance information, giving users the current Hebrew date visible from the home screen without opening the app.

## Design

The new icon is based on the provided reference image:
- **Background:** Deep navy blue (`#0D1B2A`) with a subtle radial gradient lightening toward center
- **Foreground:** Gold moon crescent on the left + white calendar square (with a small dark header bar) on the right
- **Letter:** Hebrew day-of-month letter drawn inside the calendar square in dark navy; default/fallback icon shows `א`

### Letter placement (in 108 dp × 108 dp adaptive-icon foreground viewport)

The calendar square's main content area (below its header bar) is centered at approximately:
- `cx = sizePx * 0.57f`
- `cy = sizePx * 0.52f`
- Font size: `sizePx * 0.40f`
- Color: `#0D1525` (dark navy, contrasts against white box)

These proportions were derived by analyzing the 1024 × 1024 reference image: the box spans roughly x 420–750, y 300–710, with a ~60 px header bar at top.

## Behaviour

- On first install / fresh launch the icon shows `א` (the static default in `@mipmap/ic_launcher`).
- On `MainActivity.onResume()`, `DynamicIconManager` reads today's Hebrew day (respecting `devDateOverride`) and enables the matching `<activity-alias>`, which carries the correct per-day icon.
- A `DateChangeReceiver` listens for `ACTION_DATE_CHANGED` to update the icon at midnight without requiring the user to open the app.
- On device reboot, `BootReceiver` calls `DynamicIconManager` so the icon is correct after restart.

## Implementation

### New assets

| File | Purpose |
|---|---|
| `app/src/main/res/drawable/ic_launcher_background.xml` | Navy radial gradient (replaces solid blue) |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Gold moon + white calendar box vector (no letter) |
| `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_day_NN.png` | 30 × 5 pre-baked foreground PNGs with Hebrew letter |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_day_NN.xml` | 30 adaptive-icon XMLs (same background + day foreground) |
| `scripts/generate_day_icons.main.kts` | Kotlin/JVM script that generates the 30 foreground PNGs |

### New source files

| File | Purpose |
|---|---|
| `app/src/main/java/com/levana/app/icon/DynamicIconManager.kt` | Enables the correct alias via `PackageManager.setComponentEnabledSetting()` |
| `app/src/main/java/com/levana/app/icon/DateChangeReceiver.kt` | `BroadcastReceiver` for `ACTION_DATE_CHANGED` |

### Modified files

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Add 30 `<activity-alias>` entries + `DateChangeReceiver` |
| `app/src/main/java/com/levana/app/MainActivity.kt` | Call `DynamicIconManager.update()` in `onResume()` |
| `app/src/main/java/com/levana/app/notifications/BootReceiver.kt` | Call `DynamicIconManager.update()` on boot |

### Key reuse

| Artifact | Location | Use |
|---|---|---|
| `buildTileIcon()` Canvas pattern | `quicktile/HebrewDateTileService.kt:80–102` | Same technique in icon gen script |
| `devDateOverride` | `data/PreferencesRepository.kt` | Respected by `DynamicIconManager` |
| `BootReceiver` | `notifications/BootReceiver.kt` | Piggyback for post-boot icon update |
| `JewishCalendar` + `HebrewDateFormatter.formatHebrewNumber()` | KosherJava | Compute current Hebrew day number |

### Activity-alias structure

`MainActivity`'s existing `<intent-filter>` (with `@mipmap/ic_launcher` showing `א`) stays **enabled by default**. Each alias starts `android:enabled="false"`. `DynamicIconManager` disables all components then enables only the one matching today's Hebrew day.

```xml
<activity-alias
    android:name=".MainActivityDay01"
    android:targetActivity=".MainActivity"
    android:icon="@mipmap/ic_launcher_day_01"
    android:roundIcon="@mipmap/ic_launcher_day_01"
    android:enabled="false"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity-alias>
```

## Constraints

- `minSdk = 34` — all APIs used are available.
- No new permissions required.
- `PackageManager.setComponentEnabledSetting()` causes a brief launcher flicker on some devices — this is expected and unavoidable with the alias approach.
- Icon generation script requires a JDK with AWT (standard `kotlinc` script runtime). Run once and commit the output PNGs.

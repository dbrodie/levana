# Increment 12: Dynamic Holiday Theming

## Overview

Auto-change the app's accent color based on the current Jewish holiday period. Colors apply from erev (evening before) through the end of the holiday. A toggle in settings enables/disables this feature.

## Holiday Color Mapping

| Holiday Period | Light Primary | Dark Primary | Seed Color |
|---|---|---|---|
| Chanukah | Blue | Light Blue | #1565C0 |
| Sukkot (incl. Chol HaMoed) | Green | Light Green | #2E7D32 |
| Pesach (incl. Chol HaMoed) | Red-Orange | Orange | #BF360C |
| Purim / Shushan Purim | Purple | Light Purple | #7B1FA2 |
| Rosh Hashanah | Gold | Light Gold | #F9A825 |
| Yom Kippur | Cream/White | Warm White | #795548 |
| Shavuot | Green-Yellow | Light Green | #558B2F |
| Fast days | Muted Gray | Gray | #616161 |
| Shemini Atzeret / Simchat Torah | Deep Purple | Purple | #4527A0 |
| Regular days | Default Material You / Levana Blue | | |

## Implementation

- `HolidayTheme` enum with seed colors for light and dark schemes
- `HolidayThemeResolver` uses `JewishCalendar` to detect current holiday period
- Holiday detection checks today's yomTovIndex and includes erev dates
- `LevanaTheme` composable accepts optional `holidayTheme` parameter
- Uses `ColorScheme` built from seed colors via Material 3 color generation
- Settings toggle: `dynamicHolidayTheme` in UserPreferences/DataStore

## Acceptance Criteria

- [ ] During a holiday period, app accent color matches the defined theme
- [ ] Color changes at the correct halachic times (erev start)
- [ ] Disabling dynamic theming in settings reverts to default colors
- [ ] Regular days use the default Material You / static palette
- [ ] Theme change is visually smooth (no jarring flash)
- [ ] `./gradlew build` passes

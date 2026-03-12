# Increment 06: Zmanim Display

## Overview

Add a zmanim (prayer times) screen showing 14 halachic times grouped by
time-of-day categories. Add bottom navigation with Calendar, Zmanim, and
Settings tabs. Add "Show Zmanim" from day detail.

## What We Build

### Data layer
- **`ZmanimRepository`** — wraps KosherJava `ComplexZmanimCalendar`, converts
  `java.util.Date` to `java.time.LocalTime`, handles null zmanim gracefully

### Domain
- `ZmanTime` — name, hebrewName, time (LocalTime?), category
- `ZmanCategory` — MORNING, AFTERNOON, EVENING, NIGHT

### UI
- **Zmanim screen** — grouped list of zmanim times for today (or selected date)
- **Bottom navigation** — Calendar, Zmanim, Settings (placeholder)
- **"Show Zmanim" button** on day detail screen
- Date picker on zmanim screen

### Zmanim to Display (GRA-based defaults)
1. Alot HaShachar (Dawn) — `getAlosHashachar()`
2. Misheyakir — `getMisheyakir11Degrees()` from ComplexZmanimCalendar
3. Sunrise — `getSunrise()`
4. Sof Zman Shema (GRA) — `getSofZmanShmaGRA()`
5. Sof Zman Shema (MGA) — `getSofZmanShmaMGA()`
6. Sof Zman Tefillah (GRA) — `getSofZmanTfilaGRA()`
7. Chatzot (Midday) — `getChatzos()`
8. Mincha Gedolah — `getMinchaGedola()`
9. Mincha Ketanah — `getMinchaKetana()`
10. Plag HaMincha — `getPlagHamincha()`
11. Sunset — `getSunset()`
12. Tzet HaKochavim (Nightfall) — `getTzais()`
13. Chatzot HaLaylah (Midnight) — calculated as midpoint sunset→sunrise

## Acceptance Criteria

- [ ] Bottom navigation shows Calendar, Zmanim, Settings tabs
- [ ] Zmanim screen lists all times grouped by category
- [ ] Times formatted per system 12/24-hour setting
- [ ] Sunrise/sunset times reasonable for saved location
- [ ] "Show Zmanim" on day detail navigates to zmanim for that day
- [ ] Changing date on zmanim screen updates all times
- [ ] `./gradlew build` passes

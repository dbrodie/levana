# Increment 07: Shabbat & Candle Lighting

## Overview

Add candle lighting and Havdalah time calculations. Show candle lighting
indicators on Friday calendar cells, display times in day detail and
zmanim screens.

## What We Build

### Data layer
- Extend `ZmanimRepository` with candle lighting / Havdalah methods
- Add `candleLightingOffset` to `UserPreferences` (default 18 min)
- Use `ZmanimCalendar.getCandleLighting()` with configurable offset
- Use `ZmanimCalendar.getTzais()` for Havdalah (8.5 degrees)

### Domain
- `ShabbatInfo` — candleLightingTime, havdalahTime, isErevShabbat,
  isShabbat, isErevYomTov, isYomTov

### Calendar grid
- Flame indicator on Friday cells (and erev Yom Tov)
- Small candle lighting time text below the Hebrew date

### Day detail
- Candle lighting card on Friday / erev Yom Tov
- Havdalah card on Saturday / motzei Yom Tov

### Zmanim screen
- Include candle lighting and Havdalah when applicable

### JewishCalendar helpers
- `isErevShabbat()` — Friday detection
- `isAssurBemelacha()` or day-of-week check for Shabbat
- `isErevYomTov()` — Yom Tov eve detection

## Acceptance Criteria

- [ ] Friday cells show candle lighting indicator
- [ ] Day detail for Friday shows candle lighting time
- [ ] Day detail for Saturday shows Havdalah time
- [ ] Default offset is 18 minutes before sunset
- [ ] Candle lighting appears in zmanim on Friday
- [ ] Erev Yom Tov shows candle lighting time
- [ ] `./gradlew build` passes

# Fix: Candle lighting / Havdalah across holy-day transitions

## Problem

`ZmanimRepository.getShabbatInfo` treats every candle-lighting day the same and has no
Israel/Diaspora input:

- Candle lighting is always `sunset − offset`, even when the evening follows Shabbat or
  Yom Tov (lighting then is only permitted after nightfall, from an existing flame).
- Havdalah is shown on any `JewishCalendar.isYomTov` day (including Chanukah, Purim,
  Chol HaMoed) and on Shabbat/Yom Tov days that are followed by another holy day, so the
  app "ends" the observance before consecutive holy days finish.
- The Jewish calendar is always built for the Diaspora, so Israel users get second-day
  Yom Tov candles/havdalah and lose them on the real last day.
- The candle-lighting notification's "ends" time is taken from the next day only and is
  always labelled "Shabbat ends".

## Rules

A civil date *D* is **holy** when KosherJava `JewishCalendar.isAssurBemelacha()` is true
(Shabbat or a Yom Tov on which melacha is forbidden), with `setInIsrael(inIsrael)`.
`inIsrael` comes from `UserPreferences.isInIsrael` (manual Israel/Diaspora override,
otherwise derived from the active location).

| Today (D) | Tomorrow (D+1) | Candle lighting on D | Havdalah on D |
|---|---|---|---|
| weekday | holy | sunset − offset (unchanged) | – |
| Yom Tov | Shabbat | sunset − offset (before Shabbat) | – |
| Shabbat | Yom Tov | nightfall (tzeit) | – |
| Yom Tov | Yom Tov | nightfall (tzeit) | – |
| holy | not holy | – | nightfall (tzeit) |

Ordinary Friday (sunset − offset) and Saturday (tzeit) are unchanged.

## Changes

- `ZmanimRepository.getShabbatInfo / getZmanim / hasCandles` take `inIsrael` and apply
  the table above. `ShabbatInfo` flags reflect real Shabbat / Yom Tov state.
- New `ZmanimRepository.getObservanceEnd(date, location, inIsrael)` walks forward through
  consecutive holy days and returns the final date/time and whether a Yom Tov is
  included.
- Consumers (Day detail, Zmanim screen, daily notification worker) pass
  `prefs.isInIsrael`.
- Candle-lighting notification: end time is the end of the whole consecutive run;
  labelled "Yom Tov ends" when the run includes Yom Tov, and prefixed with the weekday
  when the end is not the next day.

## Tests (JVM / Robolectric)

5787/2026 dates verified against KosherJava: Pesach (Diaspora Thu–Fri + Shabbat, Israel
Thu only), Rosh Hashanah on Shabbat–Sunday, Sukkot from Friday, Shemini Atzeret /
Simchat Torah Diaspora vs Israel, ordinary Friday/Saturday, Chanukah (no havdalah),
notification texts.

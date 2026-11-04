---
id: TASK-57.31
title: >-
  Explain zmanim, candle lighting and havdalah that can't be computed at high
  latitudes
status: Done
assignee:
  - '@claude'
created_date: '2026-11-02T14:27:00Z'
updated_date: '2026-11-04T16:34:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: medium
type: feature
ordinal: 101000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
At high latitudes some zmanim can't be computed: in white nights (e.g. St. Petersburg in June) the sun never reaches the depression for nightfall/havdalah (8.5°), Misheyakir (11°) or Alot (16.1°); in the polar day/night (e.g. Longyearbyen, Tromsø) there is no sunrise or sunset, so candle lighting and every sunrise/sunset-based time is missing. Today the app shows two different placeholders ("--:--" and "—"), silently drops Havdalah from the Agenda, and in the polar night shows a Havdalah around midday with no candle lighting. Research (2026-11-04, see Notes and /Users/daniel/.paseo/evidence/task-57.31/high-latitude-zmanim-research.md) found no halachic consensus and no product that substitutes a time.

Agreed design (owner, 2026-11-04) — deliberately minimal, this is an edge case affecting few users:
- The app does not pick any halachic substitute (no solar-midnight fallback, no polar method). A time that can't be computed is shown as missing.
- One placeholder everywhere: "--:--" (replace the "—" uses, e.g. ZmanimScreen, WidgetCommon.NO_TIME), in the day sheet, Zmanim screen, Agenda, widgets.
- Don't drop lines: the Agenda/Shabbat card keeps its Candle lighting and Havdalah lines with "--:--" instead of omitting them.
- Polar day/night correctness: when there is no sunset on the lighting day, candle lighting AND havdalah are both "--:--" (don't derive a midday havdalah from the 8.5° crossing). Reminders for a missing time are simply not scheduled (no crash).
- One label, on the selected city: where the city is chosen/shown (drawer city card, and the Cities settings row), show a short note "Some times can't be calculated at this location on some days" (Hebrew equivalent). Show it when, for that city's coordinates, sunrise, sunset, candle lighting or nightfall/havdalah is missing on at least one day of the year (roughly 58°+ N/S: St. Petersburg, Helsinki, Stockholm, Oslo, Reykjavik, polar). Alot/Misheyakir-only gaps (London, Paris, Berlin in June) do not trigger the label; they just show "--:--".
- No per-time explanations, banners, settings or new screens. Fixed-minute havdalah options belong to TASK-57.7.
- Figma: add the city note to the drawer city card frame (light, dark, Hebrew) and use "--:--" in any frame that shows a missing time. Keep it to that.

Original finding details (TASK-57.23 evidence):
- St. Petersburg (59.9°N), 21 Jun 2027: Nightfall "--:--" in the day sheet; Alot HaShachar and Misheyakir "—" in More halachic times. Evidence: /Users/daniel/.paseo/evidence/task-57.23/25-stpete-june21-zmanim.png, 26-stpete-june21-more.png, 79-he-stpete-june21-zmanim.png
- Agenda, St. Petersburg late June: Shabbat cards have no Havdalah line. Evidence: 27-stpete-agenda-june.png
- Longyearbyen (78.2°N) midnight sun: sunrise, sunset, nightfall "--:--", most halachic times "—". Evidence: 29-longyearbyen-jun21-2027.png, 30-longyearbyen-june-more.png
- Longyearbyen polar night (Nov 2026): no candle-lighting entry in Agenda, havdalah around midday, widgets "Candle Lighting Fri —". Evidence: 31-longyearbyen-agenda-nov2026.png, 40-home-widget.png, 41-home-widgets.png
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every time that can't be computed shows "--:--" (one placeholder) in the day sheet, Zmanim screen, Agenda and widgets
- [x] #2 Agenda/Shabbat cards keep the Candle lighting and Havdalah lines with "--:--" instead of dropping them; with no sunset (polar) both are "--:--" and no midday havdalah is shown
- [x] #3 The selected city shows "Some times can't be calculated at this location on some days" (English and Hebrew) in the drawer and Cities settings when sunrise, sunset, candle lighting or nightfall is missing on some day of the year, and not for cities where only Alot/Misheyakir are missing
- [x] #4 Unit tests pin St. Petersburg (white night), Longyearbyen (polar night and midnight sun) and London (no label); Figma drawer city card and placeholder updated
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Figma (figma-use skill): add the city note under the city name in the drawer city card frames (Light, Dark, Hebrew); switch every frame showing a missing time to "--:--" (e.g. widget "—" placeholders, Zmanim screen).
2. Placeholder: one constant MISSING_TIME = "--:--" in ui/ClockTime.kt; use it in DayDetailScreen, ZmanimScreen (was "—"), Agenda cards and widgets (WidgetCommon.NO_TIME aliases it).
3. ShabbatInfo: add hasCandleLighting/hasHavdalah (date-driven: candles tonight / last holy day) with defaults derived from the times; showCandleLighting/showHavdalah read them, so the lines stay with --:-- when the time is null.
4. ZmanimRepository polar guard: candle lighting (incl. at-nightfall lighting) and havdalah/observance end are null when sea-level sunset is missing on the lighting day (eve of the run) or on the last holy day, so no midday havdalah from the 8.5° crossing. Zmanim list otherwise unchanged (missing values already null). Never call getSunriseSolarDipFromOffset/isAssurBemlacha.
5. Agenda: AgendaItem.CandleLighting/Havdalah time nullable, Shabbat gets hasHavdalah; AgendaAssembler emits the lines from the date-driven flags; order() keeps missing-time candle/havdalah with timed entries (after the timed ones); cards render --:--.
6. Widgets: Today widget keeps candle/havdalah rows with --:--; Zmanim widget rows --:--.
7. City label: ZmanimRepository.hasIncalculableTimes(latitude) = |lat| > 90 - 8.5 - 23.44 (~58.06°: below that, sunrise/sunset/candles/tzais 8.5° exist every day; above, tzais 8.5° fails at the solstice and polar latitudes lose sunrise/sunset). Pure function, no caching needed; validated in tests against a full-year KosherJava scan over a latitude sweep and the named cities. Shown in the drawer city card (under the city name) and on the selected city's row in Settings → Cities. Strings in values and values-iw.
8. Reminders: candleLightingAlarm already skips a null instant; verify with a test (polar Friday, white-night Omer) that planning doesn't crash and no alarm is scheduled for the missing time.
9. Tests: ZmanimRepository high-latitude tests (St. Petersburg Fri 2027-06-25 / Sat 06-26; Longyearbyen Nov 2026 polar night and June 2027 midnight sun), city-label rule (St. Petersburg/Helsinki/Longyearbyen yes; London/Paris/Jerusalem/New York no) + scan validation, AgendaAssembler lines, reminder planning. Update Roborazzi baselines only where intended (placeholder change).
10. Verify: assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug; emulator runtime checks with screenshots in /Users/daniel/.paseo/evidence/task-57.31/impl/.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Research (2026-11-04, branch task-57.31-high-latitude-research)

Full report with citations and probe evidence: /Users/daniel/.paseo/evidence/task-57.31/high-latitude-zmanim-research.md (plus Probe.java, Scan.java and their outputs next to it). Research only; no app code changed.

**KosherJava 2.5.0 (the version we ship):** every sunrise/sunset-anchored time uses the NOAA sunrise equation; when the sun never reaches the required depression the hour-angle acos has no solution, the calculator returns NaN and every Date getter returns null (Long.MIN_VALUE for shaos zmaniyos). No fallback, no exception, no latitude cap. Only chatzos (longitude-only solar noon) and solarMidnight never go null. Fixed-minute times (tzais72/50, alos72, candleLighting) exist whenever sunset/sunrise exists, so they survive white nights but not polar day/night. Degree-based times exist whenever the sun crosses that depression, so in the polar night alos 16.1, misheyakir 11 and tzais 8.5 are non-null around midday while sunrise/sunset/candle lighting are null. That is why the Longyearbyen Nov 2026 agenda shows Havdalah 3:57 PM / 2:59 PM / 1:37 PM with no candle lighting (evidence 31-longyearbyen-agenda-nov2026.png). Null ranges for 16 cities in 2027 are tabulated in the report (St. Petersburg: tzais 8.5 null 29 May-14 Jul, alos 16.1 null 28 Apr-15 Aug; London alos 16.1 null 4 Jun-9 Jul; Tromso no sunrise/sunset 18 May-25 Jul and 28 Nov-14 Jan; Longyearbyen 18 Apr-24 Aug and 27 Oct-15 Feb). The library author documents this (FAQ "Why Some Zmanim Never Occur"), renders null as "N/A", gives no halachic guidance and points to Star-K; 3.0/3.1 (Oct/Nov 2026) add opt-in polar methods (Sternbuch lowest/highest sun, Ben Ish Chai due east/west day) but keep the null policy. Beware: getSunriseSolarDipFromOffset loops forever when the dip is unreachable; isAssurBemlacha NPEs on null sunset; NOAACalculator.getSolarElevation needs a UTC calendar.

**Accepted practice:** white nights (sunset exists, 8.5/11/16.1 not reached): the published community practice (Star-K sub-Arctic summer rule, Chabad.org calculator for dawn/misheyakir, Reykjavik, Helsinki, Oslo, Fairbanks luchos) is solar midnight (chatzot halayla) standing in for nightfall/dawn; UK keeps an 8.5-based nightfall (exists south of ~58N) and lives without 16.1 alos; fixed 42/50/72 min are opinions that happen to stay computable, not a documented fallback. Polar day/night: no consensus on the hour (Tiferes Yisrael departure clock, Ben Ish Chai 6/18, Sternbuch lowest/highest sun), rough consensus on the weekday, universal "ask your rav". No product (Chabad.org, Hebcal, MyZmanim, OU) substitutes a Shabbat-end time; all show a blank with a note ("––:––", "X:XX", "N/A"); Chabad.org substitutes chatzot for dawn only, labelled "The Chabad custom"; Hebcal silently omits the Havdalah event like we do. All products show the polar-night dawn/misheyakir/nightfall crossings in the zmanim list.

**Recommendation (details, wording, implementation outline and tests in report Part 3):**
1. Detect a day state per date (NORMAL / MIDNIGHT_SUN / POLAR_NIGHT / TRANSITION) from seaLevelSunrise/Sunset nullness plus sun altitude at chatzos; carry a status on each ZmanTime (available / not reached / no sun / fallback-with-method).
2. One placeholder glyph ("—", already WidgetCommon.NO_TIME; replace the "--:--" literals in DayDetailScreen) and the reason reachable in place: "Not reached today at this latitude", "Midnight sun: the sun does not set here today", "Polar night: the sun does not rise here today".
3. Shabbat pair must be consistent: white nights keep the Agenda havdalah line with the placeholder and reason instead of dropping it; in polar states suppress both candle lighting and havdalah with "can't be computed here, ask your rabbi" (no more 13:37 havdalah). Make ShabbatInfo.showCandleLighting/showHavdalah date-driven, not time != null.
4. Keep showing chatzot/chatzot halayla; keep polar-night dawn/nightfall crossings in the zmanim list under the day banner (industry practice, used for fast end).
5. No polar-region method in v1. The only fallback worth offering is opt-in "when nightfall/dawn isn't reached: use solar midnight (chatzot halayla)", labelled; default off. TASK-57.7's havdalah method picker (8.5 / 42 / 50 / 72 min) gives most affected users a havdalah with no fallback logic; cross-reference added there.
6. Reminders: morning-mode notice should still fire with the polar text; Omer nightfall reminder when tzais null is an open decision. Optional later: plag hint for very late sunsets (Reykjavik/Anchorage practice).

**Open questions for owner/rabbi:** (1) default when 8.5 is not reached: nothing (recommended, matches all products) vs chatzot halayla vs the user's fixed-minute method; (2) alot/misheyakir chatzot fallback automatic (Chabad.org) or only via setting; (3) polar night: suppress havdalah entirely (recommended) or show the 8.5 crossing when alos exists; (4) any polar method at all (recommended none); (5) Omer reminder/fast end in white nights: skip, fallback, or sunset; (6) plag hint wording/threshold; (7) is "Chabad/Star-K practice" acceptable label wording; (8) Hebrew wording.

## Implementation (2026-11-04, tested commit ca8cf75)

**Figma** (file E1IFrxKcldXbXnvXy5Nzhd): component set "Drawer city card" (19:136) got a hidden-by-default `Location note` boolean + `Note` text property (bodySmall / hebrew/body-small, onSecondaryContainer, under the city name, all 4 variants). New frames with St. Petersburg and the note on: Light 101:15975 "Navigation / Drawer — high-latitude city note (TASK-57.31)", Dark 101:16331, Hebrew 101:16687 (Hebrew text "בחלק מהימים לא ניתן לחשב חלק מהזמנים במיקום זה"). No existing frame showed a missing time (searched Light/Dark/Hebrew/Widgets for "—"/"--:--"), so nothing to switch to "--:--". Renders: /Users/daniel/.paseo/evidence/task-57.31/impl/figma/.

**Code:**
- `ui/ClockTime.kt`: `MISSING_TIME = "--:--"`, used by DayDetailScreen, ZmanimScreen (was "—"), AgendaScreen cards and widgets (`WidgetCommon.NO_TIME` = MISSING_TIME).
- `ShabbatInfo`: `hasCandleLighting`/`hasHavdalah` (date-driven, defaults derived from the times); show* read them. Day sheet quick card shows CANDLES --:-- on a candle evening without a time (no fallback to TZEIT).
- `ZmanimRepository`: havdalah (getShabbatInfo, getObservanceEnd) is null when there is no sea-level sunset on the lighting day (eve of the run) or the last holy day; lighting at nightfall needs a sunset too. No solar-dip/isAssurBemlacha calls. `hasIncalculableTimes(latitude)` = |lat| > 90 − 8.5 − 23.44 (≈58.06°), a pure function (no cache needed).
- Agenda: `CandleLighting`/`Havdalah` time nullable, `Shabbat.hasHavdalah`; assembler keeps the lines; a missing-time candle/havdalah sorts after the day's timed entries.
- Note: drawer city card (bodySmall under the city) and the selected city's row in Settings → Cities (saved city or current location). Strings `city_incalculable_times` in values and values-iw.
- Reminders unchanged: `candleLightingAlarm` already skipped a null instant; Omer evening skips null tzais (both pinned by tests).

**Tests:** new `data/ZmanimHighLatitudeTest` (11): St. Petersburg Fri 2027-06-25 candles 22:07 = sunset − 18; Sat 06-26 havdalah null with the line kept (ShabbatInfo, getZmanim, getObservanceEnd, Agenda Shabbat card); Longyearbyen polar night Fri/Sat 2026-11-20/21 both missing although tzais crossing exists; midnight sun Fri/Sat 2027-06-18/19 (sunrise/sunset/nightfall/plag null, chatzot present); ordering; reminder planning with missing candle lighting (morning + hours_before) and white-night Omer; city rule (St. Petersburg/Helsinki/Longyearbyen yes, London/Paris/Jerusalem/New York no, each checked against a full 2027 repository scan) and a −85°..85° sweep against a KosherJava year scan (±0.15° edge band excluded). New `ui/settings/CityIncalculableTimesNoteTest` (4, incl. Hebrew).

**Verification** (ca8cf75): `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug` BUILD SUCCESSFUL; 1243 tests, 0 failures; lint 0 errors; Roborazzi verified with no baseline changes (no existing snapshot shows a missing time). Emulator (own headless Levana_Agent_Template_API_36_1, debug build of this tree), no crashes in logcat. St. Petersburg via city list + date override 2027-06-25/26; Longyearbyen via GPS fix as current location (not in cities.json), real date Nov 2026 + override 2026-11-20 (polar night) and 2027-06-25 (midnight sun); English and Hebrew: drawer, Cities settings, day sheet, Zmanim screen, Agenda, Today widget; London selected → no note. Screenshots: /Users/daniel/.paseo/evidence/task-57.31/impl/00–22-*.png.

**Limitations / follow-up:** the plain Nightfall row (and TZEIT quick card, widget "Next: Nightfall", fast end, Omer reminder) still shows the polar-night 8.5° midday crossing (e.g. 1:37 PM, evidence 10-, 12-); only havdalah/candle lighting were changed, as agreed. Filed TASK-57.35 for the owner decision. Zmanim widget not placed on the home screen (Today widget checked; Zmanim widget shares NO_TIME). The rule's exact edge (±0.15° around 58.06°) can differ by a few hundredths from a given year's scan; no city in cities.json is near it.
<!-- SECTION:NOTES:END -->

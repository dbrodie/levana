---
id: TASK-57.31
title: >-
  Explain zmanim, candle lighting and havdalah that can't be computed at high
  latitudes
status: To Do
assignee: []
created_date: '2026-11-02T14:27:00Z'
updated_date: '2026-11-04T13:38:00Z'
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
- [ ] #1 Every time that can't be computed shows "--:--" (one placeholder) in the day sheet, Zmanim screen, Agenda and widgets
- [ ] #2 Agenda/Shabbat cards keep the Candle lighting and Havdalah lines with "--:--" instead of dropping them; with no sunset (polar) both are "--:--" and no midday havdalah is shown
- [ ] #3 The selected city shows "Some times can't be calculated at this location on some days" (English and Hebrew) in the drawer and Cities settings when sunrise, sunset, candle lighting or nightfall is missing on some day of the year, and not for cities where only Alot/Misheyakir are missing
- [ ] #4 Unit tests pin St. Petersburg (white night), Longyearbyen (polar night and midnight sun) and London (no label); Figma drawer city card and placeholder updated
<!-- AC:END -->

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
<!-- SECTION:NOTES:END -->

---
id: TASK-57.14
title: 'Date picker: holiday dots in Hebrew-led Figma frames'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-02T08:55:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: low
type: feature
ordinal: 84000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The review found that the Gregorian-led date picker frames in Figma show holiday dots but the Hebrew-led frames don't. In the app the dot is already drawn in both layouts (HebrewDatePickerDialog.kt DayCell, day.holidayCategory), including Rosh Chodesh since TASK-57.6, so this is a Figma-only gap.

Agreed scope (owner, 2026-11-02):
- Update the Hebrew-led date picker frames in Figma (light, dark, Hebrew RTL) to show the holiday dots exactly as the app draws them, matching the Gregorian-led frames.
- Confirm on the emulator that the app shows the dots in Hebrew-led mode, and record the evidence.
- No code change expected. If the app turns out not to draw them, file a bug rather than fixing it here.
- Shabbat marker: not needed, out of scope.
- Today shortcut (the other half of D3): not part of this task; left for a separate decision.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **D3** (Medium) — Date picker. No "Today" shortcut; no Yom Tov/Shabbat markers in Hebrew-led grid (Gregorian-led has dots). *Why:* Parity and speed. *Reviewer suggestion:* Add "Today" text button; mirror markers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Hebrew-led date picker frames in Figma show the holiday dots as the app draws them, in light, dark and Hebrew RTL
- [x] #2 The app is confirmed on the emulator to show the dots in Hebrew-led mode
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Figma file E1IFrxKcldXbXnvXy5Nzhd. Date picker frames (component set 'Date picker / Day cell' 20:226, BOOLEAN prop Holiday shows the 4dp dot 3dp from bottom):
- Light Hebrew-led day grid 28:2059 (dialog 28:2080): already has Holiday=true on 1 Cheshvan (28:2135) and 30 Cheshvan (28:2284).
- Dark Hebrew-led day grid 37:976: already has Holiday=true on 37:1031 and 37:1093.
- Hebrew (RTL) Hebrew-led day grid 44:2712: no dots. Set Holiday=true on 1 Cheshvan (44:2767) and 30 Cheshvan (44:2829). Month shown is Cheshvan 5787 (12 Oct - 10 Nov 2026): only holidays are Rosh Chodesh on 1 and 30 Cheshvan.
- Colour: the component's dot is bound to color/primary, but the app colours Rosh Chodesh (HolidayCategory.MINOR) with tertiary. Override the dot fill on Rosh Chodesh cells to color/tertiary in the Hebrew-led frames (light, dark, RTL), and the Gregorian-led Rosh Chodesh cells (30 Tishrei, 1 Cheshvan, light and dark) for parity. Sukkot dots (TORAH) stay primary.
- Screenshots before/after to /Users/daniel/.paseo/evidence/task-57.14/.
- Emulator: open the picker in Hebrew-led mode on Cheshvan 5787 and on Tishrei (Sukkot), English and Hebrew UI; confirm the dots and their colours.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Figma changes (file E1IFrxKcldXbXnvXy5Nzhd, 2026-11-02; all use the existing 'Date picker / Day cell' component set 20:226 and its Holiday boolean, so the dot is the component's own 4dp ellipse 3dp from the bottom):
- Screens — Hebrew (RTL), 'Date picker / Hebrew-led — day grid (Hebrew UI)' 44:2712: Holiday=true on 1 Cheshvan (44:2767, 12 Oct) and 30 Cheshvan (44:2829, 10 Nov). It previously had no dots.
- Screens — Light 'Date picker / Hebrew-led — day grid (from event form)' 28:2059 and Screens — Dark 37:976 already had Holiday=true on 1 and 30 Cheshvan (28:2135/28:2284, 37:1031/37:1093). No dot was added there.
- Colour: the component binds the dot to color/primary, but the app draws Rosh Chodesh (HolidayCategory.MINOR) in tertiary. The dot fill on every Rosh Chodesh cell is now overridden to color/tertiary (VariableID:4:14): Hebrew-led light/dark/RTL above, plus Gregorian-led 30 Tishrei/1 Cheshvan (light 28:1932/28:1937, dark 37:919/37:921) for parity. Sukkot dots (TORAH) stay primary. The variable resolves per mode (brown in light, gold in dark).
- Month shown in the Hebrew-led frames: Cheshvan 5787 (12 Oct to 10 Nov 2026, 30 days). The only holidays are Rosh Chodesh on 1 and 30 Cheshvan, confirmed on the emulator.

Emulator verification (build of commit 37832a9, no code change, Levana_Agent_Template_API_36_1, default Diaspora settings):
- English, light: Hebrew-led Cheshvan 5787 shows tertiary dots on 1 and 30 Cheshvan, matching the Figma frame. Hebrew-led Tishrei shows primary dots on Yom Tov/Chol HaMoed days, an error-red dot on Tzom Gedaliah and a tertiary dot on 30 Tishrei.
- Hebrew UI (RTL): the same dots on 1 and 30 Cheshvan, matching 44:2712.
- Dark: the same, with gold (tertiary) dots, matching 37:976.
Result: the app draws the dots in Hebrew-led mode, so no bug was filed.

Evidence (/Users/daniel/.paseo/evidence/task-57.14/): before-{light,dark,rtl}-hebrew-led.png, before-light-gregorian-led.png, after-{light,dark,rtl}-hebrew-led.png, after-{light,dark}-gregorian-led.png, emu-en-gregorian-led-cheshvan.png, emu-en-hebrew-led-cheshvan.png, emu-en-hebrew-led-tishrei.png, emu-he-hebrew-led-cheshvan.png, emu-en-dark-hebrew-led-cheshvan.png.

Limitations / observations:
- The Gregorian-led frames (Oct 2026) have dots on 20-22 Tishrei but none on 23 Tishrei (Oct 4). That matches Israel mode, but in the default Diaspora mode the app also dots Simchat Torah on Oct 4. This was left unchanged because it is out of scope and depends on which location the frames are meant to show.
- The Today shortcut and Shabbat marker are out of scope, as agreed.
<!-- SECTION:NOTES:END -->

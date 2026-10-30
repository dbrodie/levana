---
id: TASK-52.2
title: 'Foldable layout audit: every screen on cover, inner and split-screen sizes'
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T13:19:00Z'
labels: []
dependencies:
  - TASK-52.1
parent_task_id: TASK-52
priority: medium
type: task
ordinal: 57000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Audit every screen and dialog on the Fold 8 and Fold 8 Ultra cover display, inner display (portrait and landscape) and split-screen or freeform widths, in English/Hebrew (RTL) and light/dark:
- calendar (Month, both modes) and the day panel;
- Agenda and search;
- Zmanim;
- My Events, the event and birthday editors;
- the date picker;
- Settings and all sub-screens and dialogs;
- the drawer;
- the walkthrough;
- the time-zone mismatch dialog and the permission prompts.

Look for: stretched or awkwardly wide content, clipped or overlapping text, the calendar grid and day cells scaling badly, dialogs or pickers at full tablet width (cap the width), bottom sheets, touch targets, the hinge and display cutouts, and 200% text on the cover screen. Fix the issues that need no design decision. File or defer anything needing a new adaptive layout to the adaptive-layout subtask. Add Roborazzi baselines for key screens at the foldable sizes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every screen is audited on all foldable sizes with screenshots and recorded findings; non-design issues are fixed; design-level items are moved to the adaptive-layout subtask
- [x] #2 Roborazzi foldable baselines for the key screens; existing baselines unchanged except for justified fixes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Audit capture: add an opt-in audit mode to ScreenshotTest (Gradle properties -Plevana.audit.size=<FoldableSize|all>, optional font scale, output dir outside the worktree) so every existing screenshot test (all screens, dialogs, drawer, walkthrough, picker, editors, settings) renders at each FoldableSize in EN/HE x light/dark. Default runs unchanged.
2. Capture the 'before' set for all 8 sizes + 200% font on the covers into /tmp/levana-task52.2-evidence/before; review every screen; record findings per screen/size.
3. Emulator spot checks on Levana_Fold8 / Levana_Fold8_Ultra AVDs: inner and cover display separately (avoid fold mid-test because of TASK-50), insets, cutouts, landscape, 200% font on the cover.
4. Fix non-design issues with width caps / max content widths that only take effect above phone width (e.g. widthIn(max) + centring for dialogs, editors, walkthrough, month grid content; text clipping; touch targets; insets). Phone baselines must stay pixel-identical: verifyRoborazzi on all existing baselines after each fix; stop and report if a fix changes them.
5. Capture the 'after' set; build before/after pairs per issue with a README in the evidence dir.
6. Add FoldableScreenshotTest baselines for key screens (calendar, day panel, agenda, zmanim, events, editor, settings, a dialog, date picker, walkthrough, drawer) at the foldable sizes, keeping the image count reasonable.
7. Record design-level findings (two-pane, nav rail, using extra width) in TASK-52.4 notes.
8. Verify: clean app/build/test-results, assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks; record evidence; commit. No merge/push/Done.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Audit (tested commit 74f76df; before = master 8a86ad0)
Method: an opt-in audit mode in ScreenshotTest (-Plevana.audit.size/-dir/-fontScale; see ARCHITECTURE_SPEC) rendered all screenshot tests at each of the 8 FoldableSize sizes, plus 200% text on both covers, in EN/HE x light/dark. That is ~268 images per set, 10 sets before and 10 after. It covers every screen and dialog listed in the description: calendar in both modes, day panel, drawer and city switcher, Agenda and search (with and without calendar access), Zmanim, My Events, the event and birthday editors, the date picker in all states, Settings with all sub-screens and dialogs, the walkthrough with its permission asks and granted states, and the time-zone mismatch dialog (on the emulator). Evidence: /tmp/levana-task52.2-evidence/ (README.md; pairs/ has before|after per issue; before/, after/, emulator/).

Findings and fixes (no design decision needed):
1. Inner displays (704-954dp wide): the editors (event, birthday), Settings and all its sub-screens, the city picker, System calendars, Zmanim, Agenda and search, My Events (lists and tabs) and the walkthrough all stretched to full width: 900dp text fields and buttons, switches and times far from their labels. Fix: ui/LargeScreenLayout.kt caps single-column content at 640dp (the day sheet's Material cap) and centres it. Scrolling columns use Modifier.contentWidth() after verticalScroll; lazy lists use ContentWidthBox's gutter as content padding, so the gutters still scroll (checked on the emulator). Below 640dp the code measures exactly as before.
2. Walkthrough on the landscape inner display: the illustration grows with the width and pushed the appearance title ('Make it yours') below the fold. Fixed by the same cap.
3. 200% text on the covers (also on phones at 200%):
   - the date picker title wrapped a letter or two per line beside the toggle. Fix: TitleBesideOrAbove puts it above the toggle when it doesn't fit on one line; otherwise it lays out exactly like the old Row.
   - the numerals in the picker day grid and year list overlapped or clipped;
   - the month grid's day numbers, Hebrew numerals and weekday headers overlapped;
   - the walkthrough mock-ups wrapped ('Octobe/r', '16:4/1').
   Fix for the last three: CappedFontScale caps text in those fixed-size cells at 1.3x. The full dates stay in the content descriptions, and the day panel scales fully.
Checked and fine: dialogs (Material AlertDialog caps itself at 560dp; picker 420dp), the drawer (Material width), bottom sheet (640dp cap), cover and split sizes (411-477dp, phone-like), Hebrew RTL at every size, touch targets (unchanged phone components; no new small targets), status/navigation bar insets on both emulator displays.
Moved to TASK-52.4 (design): two-pane month/day panel; blank space under the grid on the inner portrait displays; list/detail for Agenda, My Events and Settings; a navigation rail; whether the headers should align with the capped column; the cap value; full text scaling in cells. Details are in the 52.4 notes.

Limitations:
- The emulator displays report no cutout, so cutout handling was only checked through the existing safeDrawing insets.
- No real device was available.
- Fold8 Ultra on the emulator: the AVD booted, but adb install hung twice under host load (load average ~40, three other agents' emulators running). Ultra sizes are covered by Roborazzi only.
- The 420 dpi density is assumed (TASK-52.1).
- Flake (pre-existing): DatePickerScreenshotTest sometimes times out waiting for the picker title under heavy load. It happened in 4 of 10 'before' audit runs on master code and in one full run on this branch. Master's app code passed 3 isolated reruns and this branch passed the final clean run. Its 10 s wait looks too short for a loaded host; not filed, coordinator's call.

## Emulator spot checks (Levana_Fold8 AVD, debug build of the same app code)
- Inner landscape (933x704): walkthrough capped and centred; calendar with the time-zone mismatch dialog; Add event; Settings; a swipe in the empty gutter scrolls Settings.
- Rotated to portrait (704x933): Settings kept its scroll position; calendar fine.
- Folded, app launched on the cover (475x751; no fold mid-test because of TASK-50): calendar at 100% and 200% system font, date picker (title above the toggle) and Settings at 200%.
- No crashes (crash buffer empty).

## Roborazzi
FoldableScreenshotTest covers calendar, date picker, Add event, Zmanim, Agenda (with the calendar banner) and Settings at all 8 sizes. FoldableWalkthroughScreenshotTest covers the appearance screen at 8 sizes. FoldableLargeTextScreenshotTest covers the calendar and picker at 200% on both covers. ScreenshotTest gained an optional fontScale. In total 52 new baselines; no existing baseline changed (git shows additions only).

## Verification (tested commit 74f76df)
From a clean app/build/test-results: assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL, 1101 tests, 0 failures, 0 errors, 0 skipped. Lint: 0 errors, 83 warnings (same as master). Log: /tmp/levana-task52.2-evidence/gradle-verify.log. The first full run hit the picker flake above (2 timeouts, testDebugUnitTest only); the rerun was clean.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T13:19:00Z

---

Coordinator integration 2026-10-30: reviewed before/after pairs (settings 640dp cap, 200% picker header); merged as 7667f11. Clean run (load avg ~40): 1101 tests, 0 failures; Roborazzi 320 unchanged (52 foldable baselines added, no phone baseline changed). Fold8 Ultra checked via Roborazzi only (AVD install hung under load). Design items recorded in TASK-52.4. Evidence: /tmp/levana-task52.2-evidence/.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Foldable layout audit of every screen at the 8 Fold8/Fold8 Ultra sizes and 200% text on the covers, in EN/HE and light/dark, with Roborazzi captures and Fold8 emulator spot checks.
- Fixed:
  - on inner displays, single-column screens (editors, Settings and sub-screens, Zmanim, Agenda and search, My Events, city picker, walkthrough) are capped at 640dp and centred, and their gutters still scroll;
  - the walkthrough title is no longer pushed below the fold on the landscape inner display;
  - at 200% text the date picker title no longer breaks a letter per line;
  - at 200% text the numerals in the month grid, the picker and the walkthrough mock-ups no longer overlap (cell text capped at 1.3x).
- Phones are pixel-identical: no existing baseline changed. 52 new foldable baselines were added.
- Design-level items are in TASK-52.4: two-pane month, list/detail, nav rail, header alignment, the cap value.
- Verified: full build, 1101 tests, lint and Roborazzi verify are green.
- Limitations: Ultra checked only in Roborazzi (emulator install hung under host load); no cutout on the emulator; no real device.
- A pre-existing DatePickerScreenshotTest timeout flake appears under heavy load.
- Evidence: /tmp/levana-task52.2-evidence/README.md.
<!-- SECTION:FINAL_SUMMARY:END -->

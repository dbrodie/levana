---
id: TASK-52.5
title: >-
  Foldable postures and outer surfaces: half-folded, cover screen, widgets,
  tile, icon
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T13:37:00Z'
labels: []
dependencies:
  - TASK-52.1
parent_task_id: TASK-52
priority: low
type: task
ordinal: 60000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Cover the remaining foldable-specific surfaces:
- **Half-folded (Flex/tabletop) posture:** nothing renders across the hinge in a broken way; decide whether any screen should adapt (e.g. Zmanim top/bottom), and make it optional.
- **Cover screen:** the app runs well on the narrow cover display, including continuing on the cover screen when the phone is folded with the app open.
- **Home-screen widgets** (Today, Month, Zmanim list) on both displays' launchers and at their resize limits.
- **The Quick Settings tile and the dynamic launcher icon** on both displays.
- **Notifications and their deep links** while folded and unfolded.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Each surface is verified on the cover and inner displays and in half-folded posture, with screenshots; issues are fixed or tracked
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Setup: build the debug APK; start Levana_Fold8 and Levana_Fold8_Ultra AVDs headless (own sessions); install; disable lock screen; get past onboarding.
2. Cover screen and fold continuity (both AVDs): open the app on the inner display on a few screens (month, zmanim, a day sheet, settings), fold, dismiss the keyguard, screenshot the cover, record the configuration and relaunches; unfold back. Note the TASK-50 crash if it hits and continue. Samsung-only behaviour (One UI 'continue apps on cover screen' setting, Flex Mode panel, Samsung launcher grids) is recorded as untested.
3. Half-folded (posture 2) on both AVDs and both rotations of the inner display: screenshot the main screens; check WindowLayoutInfo / folding feature bounds against what the app draws; nothing is broken across the hinge. Write a proposal (not built) for an optional tabletop/book adaptation.
4. Widgets: add Today, Month and Zmanim to the launcher on the inner and cover displays (Launcher3 UI via adb input/uiautomator), resize each to its min resize size and to large sizes; screenshot. Cross-check the Glance size buckets (COMPACT/FULL) and text overflow at the limits. Optionally add Roborazzi renders of the widgets at the limit sizes as new baselines (existing baselines untouched).
5. Quick Settings tile (add via cmd statusbar, check label/icon/subtitle, tap opens the app) and the dynamic launcher icon (day alias shown on both displays' launchers).
6. Notifications: post via the app's alarm receiver (root on emulator, or a near-time reminder), open from the shade folded and unfolded, confirm the deep link lands on the right screen/date.
7. Fix issues found within these surfaces only (widget sizes, breakpoints, overflow); keep the phone look; no in-app layout or state-restoration changes (52.2/52.3). File anything needing a decision as notes or tasks.
8. Save screenshots + README outside the worktree (/tmp/levana-task52.5-evidence/); run the full Gradle verify from clean test-results; record results in the notes; commit.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Result (tested commit 5bd9c33 = master 8a86ad0 app code; no app code changed)

No bug was found in these surfaces, so no app code changed and every Roborazzi baseline is unchanged. Evidence (60 screenshots + README): `/tmp/levana-task52.5-evidence/` (fold8/, fold8ultra/, README.md, gradle-verify.log).

Setup: `Levana_Fold8_API_36_1` and `Levana_Fold8_Ultra_API_36_1`, headless and read-only, Pixel Launcher, 420 dpi (assumed). The host was heavily loaded by parallel agents (load averages 20-57). That caused one System UI ANR and, on the Ultra, app/Messages "failed to complete startup" ANRs with CPU pressure ~100%. These were environmental, and the Ultra was restarted fresh.

### Cover screen and fold continuity — OK
- Fold8: 4 fold + 4 unfold cycles. The app moves to the cover (475x751dp) and back (933x704dp) on the same screen: Month, Agenda, Zmanim, and the time-zone dialog.
- Ultra: 2 cycles (411x960dp cover, 859x954dp inner). Month, day sheet and Zmanim stay on the same screen.
- Cover layouts look like the phone layout (event chips truncate as on a 393dp phone; already noted for TASK-52.2). The TASK-50 restore crash did not occur in any of these cycles, and the crash buffer stayed empty.

### Half-folded (posture 2) — OK, nothing broken across the hinge
- The app is not relaunched on HALF_OPENED (same configuration). It has no WindowManager/FoldingFeature code, so it draws straight through the fold.
- Book posture (vertical hinge; Fold8 natural landscape, Ultra natural portrait): the hinge crosses the month grid's middle column and the centred day sheet's middle zman card. Text stays readable.
- Tabletop (rotated, horizontal hinge at ~466dp / 430dp): the hinge crosses a week row of the grid, the expanded day sheet, and the Zmanim list between rows. Nothing is clipped or mispositioned. The Pixel Fold hinge is non-occluding, as is Samsung's.

### Home-screen widgets — OK on both launchers and at their limits
- All three were added on the inner launcher and shown on the cover launcher. Pixel Launcher shares one workspace: the inner two-panel home appears as two cover pages, so a widget's cell size carries across fold/unfold. Widgets are limited to one panel (4 columns) on the inner display.
- Today: 2x1 cover minimum (~180dp on the Ultra) and 4x1 use COMPACT (date + Gregorian · location, nothing cut). 4x2 is FULL, and 4x4 on the inner display is FULL, vertically centred.
- Zmanim: 2x2 minimum (2 columns on the Ultra cover; the location ellipsizes as designed) up to 4x4 on the inner display; the list scrolls.
- Month: the minimum this launcher allows is 3x2 (~330x300dp), which still renders FULL. Month's COMPACT bucket (180-249dp) is never reached on these grids. The panel maximum (4x5 on the inner display) is fine.
- Taps: a Zmanim row opens the Zmanim screen (inner); a Month day opens the calendar on that date (cover, Oct 12). Widgets refresh after a time-zone change.

### Quick Settings tile and dynamic launcher icon — OK
- The tile shows the dynamic Hebrew day (יט; כח under a date override) on the cover and inner shades of both devices. A tap opens the calendar on today.
- The launcher icon alias (MainActivityDay19, יט) shows in both displays' app drawers on both devices.

### Notifications and deep links — OK (Fold8)
- Holiday notifications were posted via Developer date override + the Holidays toggle (refresh worker). Folded: tapped from the cover shade, the app opened on the notified date (Dec 6). Unfolded: from Agenda, tapping "Chanukah begins tomorrow" opened the calendar with Dec 9 selected.
- Not repeated on the Ultra: the deep link uses the same MainActivity intent (EXTRA_DATE_EPOCH_DAY, NEW_TASK|CLEAR_TOP) as the widget taps verified there.
- Found, not foldable-specific: on a day that's already Chanukah, tomorrow's reminder says "Chanukah begins tomorrow". Filed as TASK-54 (low, bug).

### Untested (Samsung-specific; the emulator can't reproduce these)
- One UI's "Continue apps on cover screen" setting (Settings > Advanced features > Labs / Display): the default may ask "swipe up to continue" or close the app. On the emulator, Android's own "Swipe up to continue" screen appears each time.
- Flex Mode panel / Labs "Flex mode panel" for apps that don't handle folding features.
- The Samsung One UI launcher: separate or synced cover/inner home screens, its grid sizes (so the widget cell sizes and resize limits), and its widget picker.
- Samsung's QS panel layout; Samsung's real hinge geometry, cutouts and actual density.
- Real-device fold timing (TASK-50 double recreate).

## Decisions for the owner (not built)

1. **Optional tabletop adaptation (proposal).** Use androidx.window `WindowInfoTracker` to read `FoldingFeature` (state HALF_OPENED, orientation HORIZONTAL). In tabletop, pin the content above the hinge and put the controls below it. For example:
   - Zmanim screen: the next/current zman and the countdown on the top half; the scrollable list on the bottom half.
   - Calendar: the month grid on the top half; the day sheet docked fully expanded below the hinge, so it no longer floats over it.
   - Book posture (vertical hinge) on the inner display: list/detail. This overlaps TASK-52.4's large-screen two-pane decision and should share it.
   Make it a setting (default off), or limit it to tabletop. It needs the androidx.window dependency, belongs with TASK-52.4, and needs Roborazzi baselines with a simulated FoldingFeature.
2. **Widget picker previews.** The picker shows only the app icon for all three widgets: there's no `previewLayout`/`previewImage`. Options: XML `previewLayout`s (Android 12+), or Glance generated previews (Glance 1.2 / Android 15 `setWidgetPreview`). This matters more on the big inner picker; it's a design decision.
3. **Month widget at large sizes.** On the inner display at 4x5 the cells are large but the text stays phone-sized (15sp/9sp). An optional third size bucket (e.g. ≥ 300x400dp) could use larger numbers and holiday labels. This is a design decision; the phone look stays unchanged without it.
4. Month COMPACT is unreachable on foldable launchers. No action needed; it still applies on phones.

## Verification
`rm -rf app/build/test-results && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: BUILD SUCCESSFUL. 1051 tests, 0 failures, 0 errors, 0 skipped. Roborazzi verify passed, and the git tree was clean afterwards (only the new TASK-54 file). Lint: 0 errors, 83 warnings (unchanged; no code touched). Log: /tmp/levana-task52.5-evidence/gradle-verify.log. Both emulators were stopped after use.
<!-- SECTION:NOTES:END -->

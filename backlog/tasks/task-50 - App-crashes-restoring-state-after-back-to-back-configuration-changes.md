---
id: TASK-50
title: App crashes restoring state after back-to-back configuration changes
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T07:27:00Z'
updated_date: '2026-10-30T12:28:00Z'
labels: []
dependencies: []
priority: medium
type: bug
ordinal: 53000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-45 emulator verification (2026-10-30). Reproduces on master c063b34; it is not caused by the date picker.

Repro (emulator Levana_Agent_Template_API_36_1, any screen, e.g. My events): with the app in the foreground, run back to back:
  adb shell cmd locale set-app-locales ninja.doskey.app.levana --locales iw
  adb shell cmd uimode night yes
The activity is recreated twice in quick succession, and the second restore crashes:
  java.lang.ClassCastException: androidx.compose.material3.DrawerValue cannot be cast to android.os.Bundle
    at androidx.navigation.compose.NavHostControllerKt$NavControllerSaver$2.invoke(NavHostController.kt:79)
    at androidx.navigation.compose.NavHostControllerKt.rememberNavController(NavHostController.kt:60)
    at ninja.doskey.app.levana.ui.navigation.LevanaAppKt.LevanaApp(LevanaApp.kt:31)
A single config change does not crash. This looks like positional rememberSaveable keys shifting between compositions before rememberNavController: the drawer state's saved value is restored into the nav controller's saver. Users can hit it by changing the app language and dark mode, or the system theme, in quick succession.
Log: /tmp/levana-task45-evidence/config-change-crash-master.log
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Back-to-back locale and night-mode changes no longer crash; the restored screen and drawer state are correct
- [x] #2 A regression test covers state restoration across two consecutive recreations
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Root cause (confirmed from LevanaApp bytecode + Compose runtime-saveable 1.7.6): rememberNavController() and rememberDrawerState() are value-returning composables with no group of their own, called directly in LevanaApp's restart group, so both rememberSaveable calls get the same positional key (currentCompositeKeyHash); SaveableStateRegistry tells same-key entries apart only by registration order. LevanaApp returns early until the preferences load and the walkthrough is decided, and the drawer state is created after those returns. On a recreation, the first frame registers only the nav controller, which consumes the first restored value (its Bundle) and leaves [DrawerValue] under the shared key. Its saver returns null until NavHost sets the graph (NavController.saveState() is null with an empty back stack), so if a second recreation saves in that window, performSave keeps the leftover [DrawerValue] as the key's only value and the next restore hands it to NavControllerSaver: ClassCastException. The same window also silently loses the back stack.

1. Reproduce on the Fold8 Ultra AVD (fold) and with back-to-back locale + night-mode changes on master code; keep the crash log.
2. Fix in the shell: create the nav controller and drawer state before any early return, each under its own explicit saveable key (no positional sharing): rememberLevanaNavController() with the same navigators and saver as rememberNavController, which keeps its restored state when saved before the graph is set (so a second recreation in that window does not drop the back stack); the drawer via rememberSaveable(key, DrawerState.Saver).
3. Regression tests: (a) Compose StateRestorationTester: shell state with a gate closed across two consecutive save/restores, then the route and drawer value come back, no crash (fails on the old code); (b) Robolectric MainActivity recreated twice back to back with new qualifiers keeps the route and the drawer.
4. Verify: build, unit tests, lint, Roborazzi verify; emulator before/after; notes + separate commit.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Also reproduces on fold (found in TASK-52.1, 2026-10-30, master 91ec9d5 + TASK-52.1 branch with no app-code changes): on the Levana_Fold8_Ultra_API_36_1 foldable emulator (see the android-debug skill's foldables reference), `adb emu fold` then `adb shell wm dismiss-keyguard` moves MainActivity to the cover display; it was relaunched twice within ~80 ms (handleRelaunchActivity) and the second restore crashed with the same ClassCastException (DrawerValue -> Bundle in NavControllerSaver, LevanaApp.kt:36). 1 of 4 clean fold cycles crashed (the first one after a fresh install); 0 of 1 on the Fold8 AVD. Folding is therefore a user-facing trigger on Samsung foldables; TASK-52.3 (fold continuity) depends on this fix. Trace: /tmp/levana-emu-038r4dp9/artifacts/fold8ultra_crash.txt (copied to the TASK-52.1 evidence directory).

## Root cause (confirmed)
rememberSaveable keys a value by currentCompositeKeyHash. rememberNavController() and rememberDrawerState() are value-returning composables without a group of their own (bytecode: only source markers), called directly in LevanaApp's body, so both registered under one key; SaveableStateRegistry keeps same-key values as a list and hands them out in registration order. LevanaApp returns early until the preferences load and the walkthrough is decided, and the drawer state was created after those returns. On a recreation the first frame registers only the nav controller; it takes the list's first value (its Bundle) and leaves [DrawerValue]. Before the NavHost sets the graph, NavController.saveState() returns null, so a second recreation in that window saved just the leftover [DrawerValue] under the key, and the next restore gave it to NavControllerSaver. Proven on the emulator with a temporary build logging the root registry: 7f4rns=[Bundle(backStack), DrawerValue:Closed] after the first recreation, 7f4rns=[DrawerValue:Closed] after the second, then the crash. The same window also lost the back stack silently when the save did not crash.

## Fix
- ui/navigation/ShellState.kt: rememberLevanaNavController() (the navigators and saver of rememberNavController, under its own key; while the controller has nothing to save it saves the state it was restored with, so the back stack survives a recreation before the NavHost shows) and rememberLevanaDrawerState() (DrawerState.Saver under its own key).
- LevanaApp creates both before any early return.

## Tests
- ShellStateRestorationTest (StateRestorationTester, LevanaApp's shape: nav controller, early return, drawer + NavHost): two consecutive save/restores while the shell waits keep the back stack [month, settings] and the open drawer; and two after it shows. With the library calls inline (the old code) the first test fails with the same ClassCastException (run during development).

## Emulator (Fold8 Ultra AVD)
Before (master 8a86ad0): locale iw + night yes back to back from Settings crashed on the first try. Fold: 0 of 6 cycles this time (timing-dependent; TASK-52.1 saw 1 of 4).
After: 6 back-to-back locale+night cycles and 6 fold/unfold cycles from Settings: no crash, Settings and the back stack kept (45 activity creations in the fold run, 14 pairs under 500 ms apart, min 95 ms); drawer open on Month stays open through locale+night.
Evidence: /tmp/levana-task52.3-evidence/task50/ (README.md, crash and lifecycle logs, registry excerpt, screenshots, gradle-verify.log).

## Verification (tested commit: the TASK-50 commit on task-52.3-fold-continuity)
rm -rf app/build/test-results && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL; 1053 tests, 0 failures/errors/skipped; Roborazzi verify passed, no image changes; lint 0 errors, 83 warnings (unchanged).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed the crash after two quick recreations (back-to-back locale/night changes, folding). The nav controller and drawer state shared one positional saved-state key and LevanaApp returned early while preferences loaded, so a second save in that window stored the drawer's DrawerValue where the nav controller's Bundle was expected. Both are now created before any early return under their own keys (ShellState.kt); the nav controller keeps its restored state until the NavHost sets the graph, so the back stack also survives. Regression test ShellStateRestorationTest (fails with the ClassCastException on the old code). Emulator: crash reproduced before, none after in 6 locale/night and 6 fold cycles on the Fold8 Ultra AVD. Full build/test/lint/Roborazzi verify green. Leftover for TASK-52.3: the blank first frame while preferences load after each recreation.
<!-- SECTION:FINAL_SUMMARY:END -->

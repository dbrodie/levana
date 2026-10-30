---
id: TASK-50
title: App crashes restoring state after back-to-back configuration changes
status: To Do
assignee: []
created_date: '2026-10-30T07:27:00Z'
updated_date: '2026-10-30T11:46:00Z'
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
- [ ] #1 Back-to-back locale and night-mode changes no longer crash; the restored screen and drawer state are correct
- [ ] #2 A regression test covers state restoration across two consecutive recreations
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Also reproduces on fold (found in TASK-52.1, 2026-10-30, master 91ec9d5 + TASK-52.1 branch with no app-code changes): on the Levana_Fold8_Ultra_API_36_1 foldable emulator (see the android-debug skill's foldables reference), `adb emu fold` then `adb shell wm dismiss-keyguard` moves MainActivity to the cover display; it was relaunched twice within ~80 ms (handleRelaunchActivity) and the second restore crashed with the same ClassCastException (DrawerValue -> Bundle in NavControllerSaver, LevanaApp.kt:36). 1 of 4 clean fold cycles crashed (the first one after a fresh install); 0 of 1 on the Fold8 AVD. Folding is therefore a user-facing trigger on Samsung foldables; TASK-52.3 (fold continuity) depends on this fix. Trace: /tmp/levana-emu-038r4dp9/artifacts/fold8ultra_crash.txt (copied to the TASK-52.1 evidence directory).
<!-- SECTION:NOTES:END -->

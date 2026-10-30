---
id: TASK-50
title: App crashes restoring state after back-to-back configuration changes
status: To Do
assignee: []
created_date: '2026-10-30T07:27:00Z'
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

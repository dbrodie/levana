---
id: TASK-12.3
title: Split MainActivity.kt / LevanaApp into app-shell files
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:50:00Z'
updated_date: '2026-10-30T00:59:00Z'
labels: []
dependencies:
  - TASK-12.1
parent_task_id: TASK-12
priority: low
type: chore
ordinal: 46000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Target: ui/navigation/LevanaApp.kt, LevanaNavHost.kt, AppDrawer.kt (LevanaDrawerContent), TopBarTitles.kt (pure topBarTitleFor(destination)), startup effects (permission, resume, deep link) extracted as a composable; MainActivity keeps only the Activity and theme resolution (resolveHolidayTheme as a pure function). Behavior unchanged.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MainActivity.kt is roughly 80 lines or fewer; move-only diff
- [x] #2 New tests: topBarTitleFor per route (incl. Add vs Edit event); Compose tests that drawer items navigate to Calendar, My Events, Settings and Manage cities, that a city tap selects it and closes the drawer, and that the back arrow shows on non-drawer routes
- [x] #3 verifyRoborazziDebug passes with zero diffs; emulator check of drawer, deep link, back navigation and My Events top-bar actions recorded
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Re-survey (2026-10-30, master 30fd7fb): MainActivity.kt is 594 lines. It holds MainActivity (EXTRA_OPEN_ZMANIM, deep-link extras, setContent with RTL provider and holiday-theme resolution) and LevanaApp:
- prefs early return;
- location-permission ask (rememberSaveable askedForLocation);
- ON_RESUME current-location refresh;
- launcher-icon/DateRollover refresh while STARTED;
- deep-link LaunchedEffect (Calendar, then Zmanim when openZmanim);
- drawer with citiesExpanded and the city-select coroutine, Manage cities and the top-level items;
- top bar with the title `when`, menu/back icon and the My Events actions slot;
- NavHost with 13 routes.
`activeHolidayTheme(prefs, date)` already lives in ui/theme/HolidayTheme.kt and is tested in WidgetThemeTest. Only the null-safe wrapper is inline in MainActivity.

Final layout (move-only; names, strings and routes unchanged):
1. MainActivity.kt (package levana, about 60 lines): the Activity, EXTRA_OPEN_ZMANIM, extras, the RTL provider and `resolveHolidayTheme(prefs: UserPreferences?, today: LocalDate?)`, an internal pure wrapper over activeHolidayTheme.
2. ui/navigation/LevanaApp.kt: LevanaApp. It holds navController, backStackEntry, prefs collection and `val currentPrefs = prefs ?: return` ahead of every remember. Below the return come drawerState, the topBarActionsContent slot, the scope, ModalNavigationDrawer, Scaffold, and calls to the pieces below. LevanaApp moves from package levana to ui.navigation; only MainActivity calls it.
3. ui/navigation/AppStartupEffects.kt: LevanaStartupEffects(navController, deepLinkEpochDay, openZmanim). It takes the permission ask, resume refresh, icon refresh and deep-link effects verbatim and in the same order. It is called right after the early return, where the effects were. It has its own rememberCoroutineScope, with the same lifetime as LevanaApp's.
4. ui/navigation/AppDrawer.kt: LevanaDrawerContent(prefs, drawerState, scope, currentDestination, onSelectCity: suspend (String?) -> Unit, onManageCities, onNavigateTopLevel). `remember citiesExpanded` stays inside it. City select keeps its order in one coroutine: setActiveLocation, then citiesExpanded = false, then drawerState.close(). The file also holds DrawerNavItem and `isDrawerRoute(destination)`, used for showDrawer.
5. ui/navigation/TopBarTitles.kt (pure): TitledRoutes, the list of route classes, and `@StringRes topBarTitleFor(route: KClass<*>?, eventId: Long)`. Add vs Edit is decided by eventId != 0. Calendar, unknown and null map to app_name.
6. ui/navigation/LevanaTopBar.kt: LevanaTopBar(backStackEntry, showDrawer, canGoBack, onOpenDrawer, onBack, actions). It holds the TopAppBar colors, the title, the menu/back icon and the actions slot, which shows only on PersonalEventsRoute. The Calendar-route hide stays in LevanaApp.
7. ui/navigation/LevanaNavHost.kt: LevanaNavHost(navController, deepLinkEpochDay, onOpenDrawer, onRegisterTopBarActions, onUnregisterTopBarActions, modifier). The 13 composable<> blocks and transitions are copied verbatim.

Tests:
- TopBarTitlesTest: every route; Add (eventId 0) vs Edit; Calendar and null map to app_name; every route in Routes.kt except Calendar has a non-app_name title.
- MainActivityThemeTest: resolveHolidayTheme. Null prefs or a null today gives null. Chanukah 2026-12-08 gives CHANUKAH. Dynamic off gives null. A forced theme overrides.
- LevanaAppNavigationTest: Robolectric, reusing the 12.1 ScreenshotTest harness on EN_LIGHT without capturing. Drawer items reach My Events, Settings, Manage cities and back to Calendar. Tapping London selects it in preferences and closes the drawer. The back arrow (not the menu) shows on Settings, Halachic Times and deep-linked Zmanim, and back returns. Drawer routes show the menu.

Verification:
- Full command with --rerun-tasks: 116 images unchanged per results-summary.json.
- Emulator walkthrough: drawer, deep links, back, My Events actions, a forced holiday theme.
- Record file sizes before and after.

Revision during implementation: LevanaDrawerContent takes LevanaApp's own scope instead of making one, so the city-select coroutine runs on the same scope as before. It takes onSelectCity, onManageCities and onNavigateTopLevel callbacks. Rebased onto origin/master 30867af after TASK-12.2 merged; the settings screens kept the ui.settings package and names, so no import changes were needed.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Verification (tested commit 2d428f2, rebased on origin/master 30867af; b3302bf adds only the TASK-47 file)
- `set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: exit 0. 821 tests, 0 failures, 0 errors; lint clean.
- Roborazzi `app/build/test-results/roborazzi/debug/results-summary.json`: total 116, unchanged 116, changed 0, added 0, recorded 0. No baselines re-recorded.
- Before the rebase (base 30fd7fb), the same command passed with 820 tests and Roborazzi 116/116 unchanged. Two earlier runs failed on pre-existing order-dependent tests, filed as TASK-47 and described below.
- Mutation check: with drawerState.close() removed from city select and the back arrow hidden, all 4 LevanaAppNavigationTest cases failed. Restored afterwards.

## Layout and sizes
MainActivity.kt went from 594 lines to 65. New files under ui/navigation (Routes.kt is unchanged at 44):
- LevanaApp.kt 101: early return on null prefs ahead of every remember. It holds drawerState, the topBarActionsContent slot, the scope, the drawer, Scaffold and the NavHost call.
- AppStartupEffects.kt 95: the permission ask, ON_RESUME location refresh, icon/DateRollover refresh and deep-link effect. They are verbatim and in the original order.
- AppDrawer.kt 201: LevanaDrawerContent with `remember citiesExpanded`. City select runs in one coroutine: setActiveLocation, then collapse, then close. The file also has isDrawerRoute.
- LevanaTopBar.kt 75: title, menu/back and the actions slot, shown only on PersonalEventsRoute.
- TopBarTitles.kt 40: pure TitledRoutes + topBarTitleFor(route, eventId).
- LevanaNavHost.kt 174: the 13 destinations, verbatim. EventsScreen still registers and unregisters top-bar actions through LevanaApp's slot.
- MainActivity keeps the Activity, extras, the RTL provider and the pure `resolveHolidayTheme(prefs?, today?)`.
- LevanaApp moved from package levana to ui.navigation; MainActivity is its only caller.

## New tests
- TopBarTitlesTest (3): every titled route, Add vs Edit, Calendar and null map to app_name, and TitledRoutes is complete.
- MainActivityThemeTest (5): resolveHolidayTheme. Null prefs or today gives none. Chanukah 2026-12-08 gives CHANUKAH. An ordinary day gives none, and so does dynamic off. A forced theme wins. This covers holiday-theme application, which the 12.1 images (non-holiday date) cannot.
- LevanaAppNavigationTest (4), on the 12.1 harness (EN_LIGHT, no captures):
  - drawer items reach My Events, Calendar, Settings and Manage cities;
  - tapping London selects it in preferences, closes the drawer and collapses the picker;
  - the back arrow (not the menu) shows on Settings and Halachic Times, and the menu shows on My Events;
  - deep-linked Zmanim shows back, which returns to the calendar.
- Harness (test-only): added `awaitGone`, made `onScreen` protected.

## Emulator walkthrough (API 36 emulator, emulator-5556, en/light; artifacts in /tmp/levana-emu-7y64kadl/artifacts, outside the repo)
Run on 2c5d149-equivalent code (pre-rebase), then a smoke run on 2d428f2:
- Location-permission ask on launch with location revoked (01).
- Drawer: expand cities (02/03); Manage cities opens Cities with a back arrow. Added Jerusalem via Add location; the picker pops back to Cities.
- Selecting Jerusalem in the drawer closed it; on reopening the active city is Jerusalem and the list is collapsed (04).
- My Events: the Events tab shows More options in the top bar with Export JSON/CSV and Import; the Birthdays tab shows none (05).
- Settings shows back (06). Halachic times → back → Settings → back → Calendar.
- Notification-style deep link (epoch day for 2026-12-08, via the exported MainActivityDay19 alias): calendar on December 2026 with the 8th selected and the Chanukah panel (07).
- Widget zmanim deep link: Zmanim with back, which returns to December 2026 (08). This works on cold start and warm, using the widget's NEW_TASK|CLEAR_TOP flags.
- Developer → Force Holiday Theme → Purim: the app recolors to the Purim theme (10, vs 09).
- After the rebase: drawer, Settings and back, My Events actions, Manage cities and back, widget link warm and cold. No AndroidRuntime errors.
- Limitations: English/light only (en/he × light/dark is covered by the 116 Roborazzi images). A real notification tap and a real home-screen widget were not exercised; the same extras were sent via am start. An `am start` without the widget's flags on a running activity keeps the current screen, because extras are read only in onCreate; this is unchanged from master.

## Findings filed
- TASK-47: order-dependent unit tests.
  - VerifyReschedulingEndToEndTest calls NotificationAlarmReceiver.onReceive directly; the null goAsync() NPE leaks into the next Compose test.
  - LegacyMinhagPreferenceTest relies on the DataStore migration, which runs only on the first read in the JVM.
  - Gradle runs previously failed classes first, so after a failing run the suite can fail again. Clearing app/build/test-results restores the natural order.
- TASK-48: after adding a saved city, the day panel shows wrong zmanim (NETZ 4:38 PM, SHKIA 3:13 AM) while Ukiah is still active. Reproduced identically on an origin/master build (11), so it is unrelated to this split.

Correction: the pre-rebase walkthrough ran on f5a93a8 (the split commit before the rebase), not 2c5d149.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Split MainActivity.kt (594 lines) into MainActivity (65 lines: Activity plus pure resolveHolidayTheme) and six ui/navigation files: LevanaApp, AppStartupEffects, AppDrawer, LevanaTopBar, TopBarTitles (pure topBarTitleFor) and LevanaNavHost. Move-only. The prefs early return, citiesExpanded and the city-select coroutine order, the top-bar actions slot, the deep-link and widget handling, and the resume/icon/permission effects are all preserved. Added 12 tests: top-bar titles, holiday-theme resolution, and Compose navigation on the 12.1 harness. The full build, 821 tests, lint and verifyRoborazziDebug pass after rebasing onto 30867af, with 116/116 images unchanged. The emulator walkthrough covered the drawer, city select, Manage cities, My Events actions, back, notification and widget deep links and a forced theme. Pre-existing issues filed as TASK-47 (order-dependent tests) and TASK-48 (day panel after adding a city).
<!-- SECTION:FINAL_SUMMARY:END -->

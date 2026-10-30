---
id: TASK-12.3
title: Split MainActivity.kt / LevanaApp into app-shell files
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:50:00Z'
updated_date: '2026-10-30T00:35:00Z'
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
- [ ] #1 MainActivity.kt is roughly 80 lines or fewer; move-only diff
- [ ] #2 New tests: topBarTitleFor per route (incl. Add vs Edit event); Compose tests that drawer items navigate to Calendar, My Events, Settings and Manage cities, that a city tap selects it and closes the drawer, and that the back arrow shows on non-drawer routes
- [ ] #3 verifyRoborazziDebug passes with zero diffs; emulator check of drawer, deep link, back navigation and My Events top-bar actions recorded
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
<!-- SECTION:PLAN:END -->

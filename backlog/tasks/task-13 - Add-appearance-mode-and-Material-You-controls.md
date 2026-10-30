---
id: TASK-13
title: Add appearance mode and Material You controls
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-21T11:19:34Z'
updated_date: '2026-10-30T06:34:00Z'
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-er1
priority: low
type: feature
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #21 (Open)

Add Light/Dark/System selection and optional Material You colors alongside holiday themes. The system-font RFE is an approved font decision, not a request to reintroduce bundled fonts.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/system-font.md
- specs/rfes/claude-design-refresh.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Light, Dark, and System choices persist and apply. Material You colors are optionally enabled on supported devices. Interaction and precedence with holiday themes are defined and verified.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Product defaults (chosen by the coordinator; the owner may adjust):
- Appearance: Light / Dark / System, default System. Persisted in DataStore, applied app-wide immediately.
- Material You (wallpaper colours): optional switch, off by default, shown only when supported (API 31+; minSdk 34 so effectively always).
- Precedence (unchanged order): holiday scheme (holiday theming on and a holiday active, or the developer forced theme) > Material You (when on) > Levana colours. Light/Dark applies to whichever scheme wins. One line of supporting text on the Material You row explains that holiday colours take priority.
- Widgets keep following the system light/dark (launcher behaviour). They use the same scheme selection and apply Material You through the shared function.
- Status/navigation bar icons follow the resolved app mode, not the system mode.

Implementation:
1. domain/model: ThemeMode enum (SYSTEM, LIGHT, DARK) with isDark(systemDark). UserPreferences gains themeMode = SYSTEM and materialYou = false.
2. PreferencesRepository: keys theme_mode (string) and material_you (bool); saveThemeMode/saveMaterialYou; an unknown stored value falls back to SYSTEM.
3. ui/theme/Theme.kt: supportsMaterialYou, wallpaperColorScheme(context, dark), and levanaColorScheme(context, dark, holidayTheme, materialYou) built on the existing levanaColorScheme (no duplicate palette logic). LevanaTheme uses it.
4. MainActivity (keep the edit small): resolve darkTheme from prefs.themeMode (System until prefs load), pass materialYou, and re-apply enableEdgeToEdge with SystemBarStyle.auto keyed on the resolved darkTheme so bar icons stay legible.
5. Settings MVI: State themeMode/materialYou/materialYouSupported; Intents SetThemeMode/SetMaterialYou; ViewModel saves through the repository. Appearance group: App language, Theme (reuses OptionDialog with radio semantics), Wallpaper colours (SettingsSwitchRow, hidden when unsupported), Holiday theming. English and Hebrew strings.
6. Widgets: WidgetSettings includes materialYou (so toggling refreshes widgets); Today/Month/Zmanim widgets build light/dark schemes through the shared context-aware levanaColorScheme.
7. Specs: PROJECT_SPEC appearance line; ARCHITECTURE_SPEC note on theme precedence and the widget decision.
8. Tests: preference round trip and fallback; precedence (holiday > dynamic > Levana, both modes); ThemeMode -> dark mapping; ViewModel intents. Roborazzi: verify first and list the changed images (only Settings rows/scroll shifts expected), then re-record and inspect. New baselines: Settings Appearance section, Theme dialog, and the app in forced Dark under system light (and vice versa).
9. Verify: assembleDebug, testDebugUnitTest, lintDebug, verifyRoborazziDebug --rerun-tasks; emulator checks (each mode vs opposite system mode, Material You on/off with a non-default wallpaper, forced holiday + Material You, widgets, EN/HE, process death).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation (commits bd5345f plan, 2355abf feature, 5c8bb87 test isolation; rebased on origin/master 7318ba2)
- Precedence as implemented (ui/theme/Theme.kt `levanaColorScheme`, shared by the app and all three widgets): holiday scheme (holiday theming on and a holiday active, or the developer forced theme) > Material You wallpaper scheme (switch on and API 31+) > Levana colours. Light/Dark applies to whichever wins.
- Theme: ThemeMode SYSTEM/LIGHT/DARK (default SYSTEM), DataStore key `theme_mode` (an unknown value means SYSTEM). Material You: `material_you`, default off. Until the preferences load, MainActivity follows the system mode.
- Status/navigation bars: MainActivity re-applies enableEdgeToEdge(SystemBarStyle.auto { darkTheme }) whenever the resolved mode changes, so the icons follow the app mode rather than the system mode.
- Settings → Appearance: App language, Theme (OptionDialog, radio semantics), Wallpaper colors (SettingsSwitchRow, one Role.Switch element, hidden when !supportsMaterialYou; supporting text: "Use Material You colors from your wallpaper. Holiday theming takes priority during holidays."), Holiday theming. Strings are in English and Hebrew.
- Widgets: they keep the launcher/system light/dark (Glance ColorProviders day/night) and ignore the Theme setting. Material You applies through the shared function. WidgetSettings includes materialYou, so toggling it refreshes the widgets. Limitation: the scheme is a snapshot, so after a wallpaper change the widgets pick up the new colours on their next refresh or settings change, not instantly.
- Specs: PROJECT_SPEC (Presentation) and ARCHITECTURE_SPEC (Application Structure) are updated.

## Verification (tested commit 5c8bb87)
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: BUILD SUCCESSFUL; 854 tests, 0 failures. Lint adds no new warnings (the one ObsoleteSdkInt in Theme.kt was already there).
- New tests: AppearanceThemeTest (mode→dark mapping; precedence holiday > wallpaper > Levana in both modes; context overload with Material You on and off), AppearancePreferenceTest (defaults, unknown value fallback, round trip; uses AppDataStores), MainActivityThemeTest (resolveDarkTheme), WidgetThemeTest (materialYou refreshes widgets; themeMode does not).
- Roborazzi: the first verify run before re-recording failed on exactly 12 images: settings_main_2_middle ×4, settings_dialog_candle_mode ×4 (dialog backdrop scroll shift), settings_notifications_hours_before ×4 (scroll shift). Re-recording also changed settings_main_3_notifications ×4: verify had not reached it, because main() stops at its first failing capture (main_2). It shows the new rows; main_4 is unchanged. I read the compare images: the only differences are the Theme and Wallpaper colors rows and the scroll shift below them.
- New baselines (16): settings_appearance ×4, settings_dialog_theme_mode ×4, app_calendar_forced_theme ×4 and settings_appearance_forced_theme ×4 (forced Dark under a light system, forced Light under a dark system). Verify after the rebase passes with no further changes.
- Emulator (Levana_Agent_Template_API_36_1, API 36; evidence in /tmp/levana-task13-evidence/):
  - Forced Dark under system light (04) and forced Light under system dark (05/06/08): content and bar icons are correct. dumpsys shows LIGHT_STATUS_BARS for forced Light under system dark. One capture during the system's night-mode transition showed light icons briefly.
  - Material You with a non-default seed (secure theme_customization_overlay_packages, system_palette B3261E): on in light (10) and in dark (13); off (19).
  - Forced Chanukah with Material You on: Chanukah blue wins in the app (15) and in the widget (w_light_holiday_my_p2).
  - Widgets: Today, Month and Zmanim follow the system light/dark with Material You (widgets_light_my, widgets_dark_my), Levana dark with it off (widgets_dark_levana), and Hebrew (widgets_he_dark_my).
  - Hebrew: Settings (20) and the Theme dialog (21) in RTL.
  - Process death (am kill, then relaunch): the app restored to Settings with Dark and Material You persisted (13).
  - The TASK-46 zone-mismatch dialog follows the choice: Material You light (11) and Dark + Material You (14).
  - TalkBack semantics: the Wallpaper row is a single checkable node (settings_appearance_window.xml); the Theme dialog uses OptionDialog's selectableGroup with Role.RadioButton.
- Observed, out of scope: with the emulator's GPS fix (Mountain View) and an IST/GMT zone, zmanim show in the device zone (for example sunrise 2:38 PM). This is the TASK-46 mismatch case; its dialog appears as designed.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-er1",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-21",
  "metadata": {
    "import_key": "original-21",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #21 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```

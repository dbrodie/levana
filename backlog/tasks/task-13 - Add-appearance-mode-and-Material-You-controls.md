---
id: TASK-13
title: Add appearance mode and Material You controls
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-21T11:19:34Z'
updated_date: '2026-10-30T06:08:00Z'
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
- [ ] #1 Light, Dark, and System choices persist and apply. Material You colors are optionally enabled on supported devices. Interaction and precedence with holiday themes are defined and verified.
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

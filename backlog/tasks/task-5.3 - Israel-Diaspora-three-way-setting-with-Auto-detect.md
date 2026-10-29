---
id: TASK-5.3
title: Israel/Diaspora three-way setting with Auto-detect
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:47:00Z'
updated_date: '2026-10-29T19:22:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: medium
type: feature
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-5 case table row 6. Replace the 'In Israel' switch with Auto-detect (default) / Israel / Diaspora. Auto-detect uses the device position/zone, never the selected city. Migrate users who set it manually (IS_IN_ISRAEL_MANUAL) to their explicit choice; everyone else gets Auto-detect. English and Hebrew strings; accessible single-choice control matching existing settings dialogs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Three-way setting persists; Auto-detect follows the device, not the selected city
- [x] #2 Migration of existing manual and automatic users is tested
- [ ] #3 English/Hebrew UI and TalkBack semantics verified
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
- New enum IsraelMode { AUTO, ISRAEL, DIASPORA } stored under key israel_mode; UserPreferences.israelMode + resolved isInIsrael.
- Resolution in the device layer: resolveIsInIsrael(mode, devicePosition) — AUTO = device position zone is Asia/Jerusalem; the selected city is never consulted.
- DataMigration: IS_IN_ISRAEL_MANUAL=true -> ISRAEL/DIASPORA from IS_IN_ISRAEL; otherwise AUTO; old keys removed.
- Settings: replace the 'In Israel' switch with a row opening a single-choice dialog (Auto-detect / Israel / Diaspora) matching the existing radio dialogs, with selectableGroup/selectable(role=RadioButton) semantics; English + Hebrew strings. SettingsIntent/ViewModel updated; PreferencesRepository.saveIsraelMode.
- Tests: migration (manual Israel, manual Diaspora, untouched, already-migrated), resolution (NY device + Jerusalem city -> Diaspora under AUTO), persistence.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation (5.3):
- domain/model/IsraelMode {AUTO, ISRAEL, DIASPORA}; UserPreferences.israelMode + resolved isInIsrael. device/resolveIsInIsrael(mode, devicePosition): AUTO = device position in Asia/Jerusalem; the selected city is never read.
- PreferencesRepository: key israel_mode, saveIsraelMode (replaces saveIsInIsrael). IsraelModeMigration: is_in_israel_manual=true -> ISRAEL/DIASPORA from is_in_israel; otherwise AUTO (no key); old keys removed.
- Settings: the 'Israel / Diaspora' switch becomes a row (summary 'Auto-detect (Israel|Diaspora)' or the manual choice, plus the existing description) opening a single-choice dialog (selectableGroup + selectable(role = RadioButton)), same pattern as the App language dialog. Strings EN + HE (settings_israel_mode_*).
- PROJECT_SPEC.md Calendar bullet updated.
Tests: data/IsraelModeTest (resolution; NY device + Jerusalem selected = Diaspora under AUTO, device move to Jerusalem = Israel; persistence; manual Israel/Diaspora migration; untouched/automatic users -> AUTO). Tests that set saveIsInIsrael(x) now set the equivalent manual mode (mechanical).
Verification: assembleDebug + testDebugUnitTest (690, 0 failures) + lintDebug. Emulator/TalkBack: see TASK-5 notes.
<!-- SECTION:NOTES:END -->

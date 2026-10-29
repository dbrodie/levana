---
id: TASK-5.3
title: Israel/Diaspora three-way setting with Auto-detect
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-29T18:47:00Z'
updated_date: '2026-10-29T18:54:00Z'
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
- [ ] #1 Three-way setting persists; Auto-detect follows the device, not the selected city
- [ ] #2 Migration of existing manual and automatic users is tested
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

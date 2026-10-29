---
id: TASK-12.2
title: Split SettingsScreen.kt into focused files
status: To Do
assignee: []
created_date: '2026-10-29T18:50:00Z'
labels: []
dependencies:
  - TASK-12.1
parent_task_id: TASK-12
priority: low
type: chore
ordinal: 45000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Target, all in ui/settings: SettingsScreen.kt (main screen + SettingsContent), SettingsComponents.kt, SettingsDialogs.kt, NotificationsSection.kt, HalachicTimesSettingsScreen.kt, LocationsSettingsScreen.kt, DeveloperSettingsScreen.kt (with HolidayThemePicker), AboutScreen.kt, SettingsTimeFormat.kt. Remove dead code (SettingsSectionHeader, DeveloperSettingsSection, DateOverridePicker, and parseHebrewDateOverride with its test if still unused) in a separate commit. Extract the duplicated 12-hour formatter into a pure function with unit tests that pin the CURRENT output, including noon → '12:00 AM', so TASK-32 can fix it afterwards.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 No file in ui/settings exceeds ~350 lines; move-only diff apart from the formatter extraction and dead-code commit
- [ ] #2 Formatter unit tests pin current output; all existing settings tests pass unchanged
- [ ] #3 verifyRoborazziDebug passes with zero diffs; emulator walkthrough of every Settings row and dialog, notification permission (incl. rotation), Locations add/delete and Developer screen recorded
<!-- AC:END -->

---
id: TASK-12.2
title: Split SettingsScreen.kt into focused files
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:50:00Z'
updated_date: '2026-10-30T00:34:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Re-survey (2026-10-30, master 30fd7fb): SettingsScreen.kt is 1373 lines. Public/internal names referenced elsewhere: SettingsScreen, SettingsContent, HalachicTimesSettingsScreen, DeveloperSettingsScreen, LocationsSettingsScreen, LocationsSettingsContent (SavedLocationDeleteLabelTest), AboutScreen, HalachicTimesSection (SettingsCheckboxRowTest), NotificationsSection (SettingsStepperRowTest), parseHebrewDateOverride (DateOverrideParsingTest only). DayDetailScreen has its own private HalachicTimesSection (unrelated).

Dead code re-checked: SettingsSectionHeader (no callers), DeveloperSettingsSection (no callers; DeveloperSettingsScreen now uses GoToDateDialog), DateOverridePicker (only from DeveloperSettingsSection), parseHebrewDateOverride (only DateOverridePicker + DateOverrideParsingTest). HolidayThemePicker stays: DeveloperSettingsScreen uses it.

Commits:
1. Plan (this).
2. Dead code: delete the four items above plus DateOverrideParsingTest.kt and imports they alone used. Nothing else changes.
3. Move-only split, all in package ui.settings, private helpers -> internal where used across files:
   - SettingsScreen.kt: SettingsScreen + SettingsContent (the 7 remember dialog flags and dialog hosting stay inside SettingsContent).
   - SettingsComponents.kt: SettingsGroup, SettingsSwitchRow, SettingsRowDivider.
   - SettingsDialogs.kt: OptionDialog, IsraelModeDialog, AppLanguageDialog, CandleLightingOffsetDialog (remember(offset) key unchanged), CandleLightingModeDialog, and the yahrzeit/israel-mode label functions.
   - NotificationsSection.kt: NotificationsSection with its rememberSaveable pendingSwitchName and POST_NOTIFICATIONS launcher together.
   - HalachicTimesSettingsScreen.kt: HalachicTimesSettingsScreen + HalachicTimesSection.
   - LocationsSettingsScreen.kt: LocationsSettingsScreen + LocationsSettingsContent (direct repository calls unchanged).
   - DeveloperSettingsScreen.kt: DeveloperSettingsScreen + HolidayThemePicker + holidayThemeLabel, hardcoded developer strings unchanged.
   - AboutScreen.kt: AboutScreen.
   Bodies byte-identical apart from visibility modifiers; imports trimmed per file.
4. Formatter: SettingsTimeFormat.kt with pure morningTimeFormatArgs(minutesOfDay) = [12-hour hour, minutes, 24-hour hour], used by both stepper rows in NotificationsSection through stringResource(R.string.settings_at_morning_time, ...) as today. Unit tests pin args and the formatted English/Hebrew strings across 360..720, including 720 -> 'At 12:00 AM' (TASK-32 bug, not fixed).
5. Verify: assembleDebug, testDebugUnitTest, lintDebug, verifyRoborazziDebug --rerun-tasks; 116 images unchanged per results-summary.json. Emulator walkthrough (android-debug): every Settings row and dialog, notification permission prompt incl. rotation, Locations add/delete, Developer screen. Record file sizes before/after, tested commit and evidence in notes.
<!-- SECTION:PLAN:END -->

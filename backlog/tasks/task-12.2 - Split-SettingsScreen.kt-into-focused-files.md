---
id: TASK-12.2
title: Split SettingsScreen.kt into focused files
status: Done
assignee:
  - '@claude'
created_date: '2026-10-29T18:50:00Z'
updated_date: '2026-10-30T00:52:00Z'
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
- [x] #1 No file in ui/settings exceeds ~350 lines; move-only diff apart from the formatter extraction and dead-code commit
- [x] #2 Formatter unit tests pin current output; all existing settings tests pass unchanged
- [x] #3 verifyRoborazziDebug passes with zero diffs; emulator walkthrough of every Settings row and dialog, notification permission (incl. rotation), Locations add/delete and Developer screen recorded
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Verification (tested commit 0bcceb0)
- `set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: BUILD SUCCESSFUL (61 tasks executed). 809 unit tests, 0 failures, 0 errors (808 before, -3 DateOverrideParsingTest, +4 SettingsTimeFormatTest). Lint: no issues in ui/settings.
- Roborazzi: app/build/test-results/roborazzi/debug/results-summary.json = total 116, unchanged 116, changed 0, added 0, recorded 0. No baselines re-recorded.
- Existing settings tests (SettingsCheckboxRowTest, SettingsStepperRowTest, NotificationSwitchTest, SavedLocationDeleteLabelTest) unchanged and passing.

## Commits
- 47bd9ad plan; f48885b dead code (SettingsSectionHeader, DeveloperSettingsSection, DateOverridePicker, parseHebrewDateOverride + DateOverrideParsingTest; each re-checked with grep: no other callers); 958a09b move-only split; 0bcceb0 formatter.
- Split checked by script: the multiset of non-import lines across the 8 files equals the old file's, modulo private->internal. The only import dropped (Icons.Outlined.Edit) was already unused.

## File sizes (lines)
Before: SettingsScreen.kt 1373 (1225 after the dead-code commit).
After: SettingsScreen.kt 370, SettingsDialogs.kt 301, NotificationsSection.kt 205, DeveloperSettingsScreen.kt 154, LocationsSettingsScreen.kt 146, SettingsComponents.kt 76, HalachicTimesSettingsScreen.kt 69, AboutScreen.kt 36, SettingsTimeFormat.kt 17.
SettingsScreen.kt is 370 (target ~350): SettingsContent alone is ~305 lines of rows and dialog hosting with its 7 remember flags; trimming further would mean restructuring it, not moving code. Left as is.
Layout differs from the guide only in that OptionDialog lives in SettingsDialogs.kt with the other dialogs and labels, and HalachicTimesSection moved with its screen.

## Formatter
morningTimeFormatArgs(minutesOfDay) in SettingsTimeFormat.kt returns [12-hour hour, minutes, 24-hour hour] for R.string.settings_at_morning_time; both stepper rows still call stringResource. SettingsTimeFormatTest pins args and strings: en 'At 6:00 AM' .. 'At 11:30 AM', noon 'At 12:00 AM' (TASK-32, not fixed); iw 'בשעה 6:00', 'בשעה 12:00'.

## Emulator walkthrough (Levana_Agent_Template_API_36_1, read-only, APK from 0bcceb0)
Evidence: /tmp/levana-emu-b_tn6msd/artifacts/ (33 PNGs). APK copy: /tmp/task122/app-debug-0bcceb0.apk.
- Every Settings row: Location, System calendars (permission screen), Israel/Diaspora dialog (Diaspora then Auto-detect, summary updated), Modern holidays switch, Candle offset dialog (slider 18->44 + OK applied; reopen shows 44; slide + Cancel kept 44), Yahrzeit leap dialog (Adar II, back to Both), First-year dialog, Also mark 1 Nissan switch, Missing-day dialog, App language dialog (Hebrew applied, RTL; reverted to System default), Holiday theming switch, Halachic Times screen (toggle Alot: count 3->4->3), Developer, About (version 0.4.0-debug, install source None).
- Notification permission: tapping Candle Lighting with permission not granted showed the POST_NOTIFICATIONS prompt; rotated to landscape with the prompt open, tapped Allow, rotated back: Candle Lighting came on (pending switch survived rotation). Then Holidays, Fasts, Personal Events, Omer, At tzait, Morning reminder all toggled; Notify mode dialog (Morning -> Hours before); steppers: candle time to noon shows 'At 12:00 AM', hours before 2->3, Omer reminder 7:00 -> 12:00 AM -> 11:30 AM.
- Locations: without permission shows 'Tap to allow location access'; tap -> system prompt -> grant -> current location updated (Ukiah, emulator GPS). Add location -> city picker -> Haifa added under Saved locations; delete button (a11y 'Delete saved location Haifa') removed it.
- Developer: Date Override opens GoToDateDialog, Go to date sets '19/8/5787 (Hebrew)', Clear resets 'Not set'; Force Holiday Theme dropdown lists all themes, Chanukah then None; Test crash throws 'Test crash from Developer Settings' into the crash dialog.
- Hebrew + dark mode: Settings rendered RTL in dark theme.
- No FATAL/ANR in logcat other than the intentional test crash.
- Limitation: denying location twice makes Android stop prompting (existing platform behaviour); flags were reset with pm clear-permission-flags to exercise the grant path. Notification 'Don't allow' path not walked (covered by NotificationSwitchTest).
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T00:52:00Z

---

Coordinator integration 2026-10-30: accepted SettingsScreen.kt at 370 lines against the ~350 guide (SettingsContent alone is ~305 lines; going lower would be restructuring, not moving). Merged as 305f3e2. Combined assembleDebug + testDebugUnitTest + lintDebug + verifyRoborazziDebug (--rerun-tasks): 809 tests, 0 failures; Roborazzi 116 unchanged, 0 changed.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Split SettingsScreen.kt (1373 lines) into 9 files in ui.settings with no behavior change. The bodies were moved unchanged; shared helpers went from private to internal, and all names tests reference were kept. The dead code went in its own commit: SettingsSectionHeader, DeveloperSettingsSection, DateOverridePicker, and parseHebrewDateOverride with its test. The duplicated 12-hour formatter is now morningTimeFormatArgs, with tests pinning the current output, including noon as 'At 12:00 AM' (TASK-32). The full build, 809 tests and lint pass on 0bcceb0; Roborazzi shows 116/116 unchanged. An emulator walkthrough covered every row and dialog, the notification prompt across rotation, Locations add/delete and Developer. Open for the coordinator: SettingsScreen.kt is 370 lines (AC #1 says ~350), because SettingsContent alone is ~305 lines and cutting it further would mean restructuring, not moving; AC #1 is left unchecked.
<!-- SECTION:FINAL_SUMMARY:END -->

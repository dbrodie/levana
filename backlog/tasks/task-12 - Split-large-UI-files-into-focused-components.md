---
id: TASK-12
title: Split SettingsScreen.kt and MainActivity.kt into focused files
status: In Progress
assignee: []
created_date: '2026-10-21T11:19:33Z'
updated_date: '2026-10-29T21:42:00Z'
labels:
  - curated-import
  - source-specs
dependencies:
  - TASK-5
  - TASK-9
references:
  - levana-dw6
priority: low
type: chore
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Pure structural cleanup: split the two oversized UI files into focused files with **no user-visible or behavior change**, proven by screenshot baselines recorded before the split.

Sequencing (owner decision 2026-10-29): start only after TASK-5 and TASK-9 are merged, because both change Settings, the drawer and the app shell. Baselines (12.1) must be recorded from master after those land.

Scoping review (2026-10-29, verified by the coordinator):
- Only two files need splitting. `ui/settings/SettingsScreen.kt` (1296 lines) holds five routed screens (Settings, Halachic Times, Developer, Locations, About), `SettingsContent` with eight groups and six `remember` dialog flags, shared private helpers (SettingsGroup, SettingsSwitchRow, SettingsRowDivider, OptionDialog), three more dialogs, NotificationsSection with its POST_NOTIFICATIONS launcher, the dev pickers, and a duplicated 12-hour formatter (the TASK-32 noon bug). `MainActivity.kt` (583 lines) holds theme resolution and `LevanaApp`: location-permission ask, ON_RESUME observer, deep link, drawer, top-bar title `when`, and the NavHost.
- Other large files (CalendarScreen, GoToDateDialog, AddEditEventScreen, EventsScreen, DayDetailScreen) are cohesive. They are out of scope unless another task opens them.
- Dead code, confirmed: `SettingsSectionHeader` and `DeveloperSettingsSection` have no callers; `DateOverridePicker` is called only from `DeveloperSettingsSection`; `parseHebrewDateOverride` is used only by that picker and `DateOverrideParsingTest`.
- **Roborazzi is wired in Gradle (plugin 1.59.0, Robolectric NATIVE graphics) but no test captures screenshots, there are no baselines, and there is no `@Preview` anywhere**, although ARCHITECTURE_SPEC claims snapshots exist. The coordinator ran a throwaway probe: `recordRoborazziDebug` with `captureRoboImage` works in this setup and renders Hebrew correctly.
- No tests cover LevanaApp, the drawer or navigation. Existing settings tests: SettingsCheckboxRowTest, SettingsStepperRowTest, NotificationSwitchTest, SavedLocationDeleteLabelTest, DateOverrideParsingTest, plus DeleteActionLabelsTest.
- SettingsScreen is the merge-conflict hotspot: 15 commits in the 14 days before 2026-10-29.

Line numbers above predate TASK-5 and TASK-9. Re-survey both files when starting.

Risks to preserve during the move:
- Keep the dialog flags as `remember` (not `rememberSaveable`).
- Keep `rememberSaveable pendingSwitchName` and the permission launcher together inside NotificationsSection.
- Keep the `remember(offset)` key in CandleLightingOffsetDialog.
- `val currentPrefs = prefs ?: return` early return in LevanaApp: extracted pieces stay below it or take prefs as a parameter.
- Drawer `citiesExpanded` state and its city-select coroutine.
- The `topBarActionsContent` slot ↔ EventsScreen `DisposableEffect` registration.
- Keep the names and the `ui.settings` package that tests reference; helpers go from private to internal.

Out of scope: visual or behavior changes, string or route renames, new ViewModels, fixing TASK-32 (pin current output instead), moving LocationsSettingsScreen's direct repository calls into MVI (file separately if wanted), and EventsScreen/DayDetailScreen tidy-ups.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Screenshot baselines recorded from pre-refactor master cover Settings, its dialogs and sub-screens, the drawer and top bar, and verifyRoborazziDebug passes with zero diffs after each split
- [ ] #2 SettingsScreen.kt and MainActivity.kt are split per subtasks 12.2/12.3 with no user-visible or behavior change; dead code removed in its own commit
- [ ] #3 Build, unit tests, lint and verifyRoborazziDebug pass; emulator walkthrough in English/Hebrew, light/dark recorded
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-dw6",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-19",
  "metadata": {
    "import_key": "original-19",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #19 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```

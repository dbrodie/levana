---
id: TASK-53.1
title: Add the MIT licence and third-party notices
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T12:24:00Z'
updated_date: '2026-10-30T13:07:00Z'
labels: []
dependencies: []
parent_task_id: TASK-53
priority: high
type: chore
ordinal: 62000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner decision 2026-10-30: Levana is licensed **MIT**. Add a LICENSE file (MIT, copyright holder: Daniel Brodie, with the years of the project), mention the licence in the README if one exists (or create a minimal README with a description, licence and links), and add third-party notices listing the bundled libraries and their licences, e.g. KosherJava (LGPL-2.1) and the AndroidX, Kotlin and Material libraries. Generate the list from the actual dependency graph rather than by hand. Optionally show the licences in About. Confirm that GitHub then detects the licence.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 LICENSE (MIT) is committed and GitHub shows MIT; third-party notices are generated from the real dependency graph and include KosherJava's LGPL-2.1
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. LICENSE at repo root: verbatim MIT text, 'Copyright (c) 2025-2026 Daniel Brodie' (first commit 2025-12-30).
2. README.md (none exists): short factual description from specs/PROJECT_SPEC.md, requirements, install links (GitHub Releases), licence section pointing to LICENSE and THIRD_PARTY_NOTICES.md.
3. scripts/generate_third_party_notices.py: runs ./gradlew :app:dependencies --configuration releaseRuntimeClasspath (the real release graph), parses resolved coordinates, reads each POM (following parent POMs) from the Gradle cache for name, URL and licence, and writes THIRD_PARTY_NOTICES.md. No build-script changes, so no conflict with TASK-53.2; rerun after 53.2 drops play-services-location. Google Play Services / Firebase artifacts are flagged as proprietary, to be removed by TASK-53.2.
4. Commit the generated THIRD_PARTY_NOTICES.md; check that KosherJava shows LGPL-2.1.
5. About: add an 'Open-source licences' ListItem (EN + HE strings) that opens THIRD_PARTY_NOTICES.md on GitHub via ACTION_VIEW. Re-record the settings_about Roborazzi baselines and justify the changed images.
6. Verify: assembleDebug, testDebugUnitTest, lintDebug, verifyRoborazziDebug --rerun-tasks from a clean app/build/test-results. Check that the LICENSE matches GitHub's MIT template (licensee-compatible). Record the results in the notes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation (commits 4df1263, e2f2c6f; base 2d5cec6)
- LICENSE: the verbatim MIT text, 'Copyright (c) 2025-2026 Daniel Brodie' (first commit 2025-12-30). Diffed against GitHub's template (gh api licenses/mit): identical apart from the template's trailing blank line, so GitHub/licensee should detect MIT. **Detection on github.com can only be confirmed after the push (coordinator).**
- README.md (new): description and features taken from specs/PROJECT_SPEC.md, a GitHub Releases link, the privacy policy, a build command, and a licence section.
- scripts/generate_third_party_notices.py (stdlib Python, no build-script changes, so no conflict with 53.2): runs `:app:dependencies --configuration releaseRuntimeClasspath`, takes resolved versions, skips (c) constraints and BOM/pom-only modules, and reads POM licences (following parents) from the Gradle cache. When a POM isn't cached (modules with Gradle metadata), it downloads it from Google Maven or Maven Central into the gitignored build/pom-cache. It normalises licences to SPDX and fails on a missing POM or licence. Rerun: `set -a && . ./.env && set +a && python3 scripts/generate_third_party_notices.py`.
- THIRD_PARTY_NOTICES.md (generated): 172 libraries (Apache-2.0 165, LGPL-2.1 1 = com.kosherjava:zmanim 2.5.0, verified from its POM, BSD-3-Clause 1 glance-appwidget-external-protobuf, MIT 1 checker-qual, proprietary 4). It includes an LGPL-2.1 notice for KosherJava. **play-services-location/base/basement/tasks are flagged** in a 'Proprietary dependencies (pending removal)' section; after TASK-53.2 lands, rerun the script and the section disappears automatically.
- About: a new 'Open-source licenses' row (EN and HE strings, Outlined.Description icon) opens THIRD_PARTY_NOTICES.md on GitHub via LocalUriHandler. Re-recorded the settings_about_{en,he}_{light,dark} baselines; the only visual change is the added row, checked visually in EN light and HE dark (RTL correct). No other baselines changed.

## Verification (tested commit e2f2c6f)
`rm -rf app/build/test-results && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`
- Run 3 (with --continue): BUILD SUCCESSFUL; 1051 tests, 0 failures/errors/skipped; verifyRoborazziDebug passed; lint 0 errors, 83 warnings, none in changed files. Log: /tmp/t531-verify3.log.
- Runs 1 and 2 each had one unrelated wall-clock timeout under host load (load average about 35-41 from parallel agents): DatePickerScreenshotTest.outOfRangeStoredDate[HE_LIGHT] (await timeout; that class passed alone afterwards) and DeviceTodayUpdatesTest.aDateChangeSignalAfterMidnightMovesToday (5 s withTimeout). Different tests each time, both timing-based, and neither touches About or licences. Logs: /tmp/t531-verify.log, /tmp/t531-verify2.log.
- No on-device check: the About row is covered by the Roborazzi baselines; the link target exists only after the push.

## Handoff
The AC is left unchecked for the coordinator: confirm that GitHub shows MIT after the push, and rerun the notices script after 53.2 merges (or ask 53.2 to).
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T13:07:00Z

---

Coordinator integration 2026-10-30: merged as 7720dbf. Clean run (load avg ~40): 1051 tests, 0 failures; Roborazzi 268 unchanged (4 settings_about baselines re-recorded for the new Open-source licenses row). Pushed; GitHub now detects MIT License. Follow-up after TASK-53.2: rerun scripts/generate_third_party_notices.py to drop the Play Services entries.

---
<!-- COMMENTS:END -->

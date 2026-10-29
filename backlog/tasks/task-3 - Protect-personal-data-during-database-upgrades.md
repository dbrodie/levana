---
id: TASK-3
title: Protect personal data during database upgrades
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-21T11:19:28Z"
updated_date: "2026-10-22T09:24:39Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-tvw
priority: high
type: chore
ordinal: 3000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — original backlog #3 (Open)

Export Room schemas, replace destructive migration fallback with explicit migrations, and test preservation of personal data. This is upgrade protection; it is distinct from full user backup/restore.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Room (personal_events) schema v1 is exported and committed under app/schemas; production builds the DB with no destructive fallback and an explicit migration list; every future schema change requires a version bump, a committed schema JSON and an explicit migration with a MigrationTestHelper test. Tests prove v1 personal events survive production open/reopen and that an unsupported upgrade or downgrade fails without wiping data. Saved locations and preferences are DataStore (not Room): verify the DataStore migration preserves them and that they survive an app upgrade install. Contact birthdays live in Android Contacts and are out of scope, as is full backup/restore (levana-a10).
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
IMPLEMENTED AND VERIFIED, AWAITING MERGE APPROVAL (2026-10-22).
Branch fix/protect-upgrade-data, worktree /Users/daniel/.paseo/worktrees/15o7vaq5/protect-upgrade-data. Commits: ac40552 (spec specs/rfes/protect-upgrade-data.md), a41d9ee (implementation), 75ebcbe (spec results + TASKS.md #3 Done on branch).
Changes: LevanaDatabase exportSchema=true with VERSION/NAME/MIGRATIONS (empty: every release v0.1.0-v0.3.0 shipped the identical 7-column v1 schema, so no bump or no-op migration) and build() via withUpgradePolicy (addMigrations only, no fallbackToDestructiveMigration); AppModules uses LevanaDatabase.build. KSP room.schemaLocation=app/schemas; committed 1.json. room-testing + kspTest; schemas added to debug assets (Robolectric only reads merged variant assets; absent from release APK); mergeDebugAssets dependsOn kspDebugKotlin.
Tests: LevanaDatabaseUpgradeTest (v1 rows created from committed schema survive production open/reopen; schema+migration-chain guard with runMigrationsAndValidate; v2 test DB without migration fails with IllegalStateException and v1 rows remain; downgrade fails and rows remain); LocationModeMigrationTest keeps saved_locations/settings.
Validation: ./gradlew :app:testDebugUnitTest -> 514 tests, 0 failures; :app:assembleDebug and :app:assembleRelease succeed. Mutation checks (re-add destructive fallback; change entity without bump; bump VERSION without schema/migration) each make tests fail. ktlint: new files clean; repo-wide ktlintCheck still fails on pre-existing violations (~5.5k, incl. import order in AppModules/LocationModeMigrationTest). Emulator (isolated Levana_Agent_Template_API_36_1 session): master debug build (versionCode 300) seeded via UI with event 'Upgrade check keep-me' and saved location Tel Aviv; branch build (301) adb install -r; both present after relaunch, Tel Aviv still active, no FATAL logs.
Limitations: no v2 schema exists, so no real migration is exercised yet; the first schema change must add one with a test. Debug APK carries the schema JSON. Not merged; worktree retained.
Merged to master as e9aa663 (no-ff merge of fix/protect-upgrade-data at 75ebcbe; tree identical to the verified branch) and pushed to origin/master with user approval on 2026-10-22. Worktree and branch removed.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-tvw",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-22T09:09:15Z",
  "closed_at": "2026-10-22T09:24:39Z",
  "close_reason": "Implemented, verified and merged to master (e9aa663, pushed). See notes for tests and emulator upgrade evidence.",
  "external_ref": "specs-import:original-3",
  "metadata": {
    "import_key": "original-3",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #3 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 1
}
```

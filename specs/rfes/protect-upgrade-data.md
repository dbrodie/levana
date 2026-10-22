# Protect personal data during database upgrades

**Status:** Implemented and verified on `fix/protect-upgrade-data`; awaiting merge approval
**Beads:** levana-tvw (backlog #3 in `specs/TASKS.md`)

## Problem

`LevanaDatabase` is built with `fallbackToDestructiveMigration()` and
`exportSchema = false`. The first time a release bumps the Room version
without a matching migration, Room silently drops `personal_events` and the
user loses every personal event and yahrzeit. There is also no committed
record of the schema that shipped, so a future migration has nothing to be
tested against.

## Where personal data actually lives

| Data | Storage | Affected by Room upgrades? |
| --- | --- | --- |
| Personal events / yahrzeits | Room `levana-database`, table `personal_events` | Yes |
| Saved locations, active location, all settings | DataStore `user_preferences` (`PreferencesRepository`) | No |
| Notification delivery log | DataStore (`NotificationDeliveryLog`) | No |
| Contact birthdays | Android Contacts provider | No (owned by Contacts) |

Saved locations are **not** Room tables. Destructive Room fallback cannot
touch them. Their only upgrade path is the DataStore `LocationModeMigration`,
which must preserve `saved_locations` and unrelated settings.

## Current schema history

Every release tag from `v0.1.0` to `v0.3.0` ships `version = 1` with the same
seven `PersonalEvent` columns (`id`, `title`, `hebrewDay`, `hebrewMonth`,
`hebrewYear`, `notes`, `useYahrzeitRules`). No migration is needed today, so
this change does **not** bump the version or add an empty migration.

## Changes

1. **Export the schema.** Set `exportSchema = true` and pass KSP the
   `room.schemaLocation` argument `app/schemas`. Commit the generated
   `app/schemas/ninja.doskey.app.levana.data.db.LevanaDatabase/1.json`.
   Exported schema files are append-only: a shipped version's JSON is never
   edited.
2. **Remove destructive fallback.** Production builds the database through
   `LevanaDatabase.build(context)`, which registers
   `LevanaDatabase.MIGRATIONS` and has no `fallbackToDestructiveMigration*`.
   An upgrade or downgrade without a migration path makes Room throw
   `IllegalStateException` on open and leaves the file untouched, instead of
   wiping it.
3. **Migration policy** (documented on `LevanaDatabase`): any schema change
   bumps `version`, adds an explicit `Migration(n, n + 1)` (or a verified
   `@AutoMigration`) to `MIGRATIONS`, commits the new schema JSON, and adds a
   `MigrationTestHelper` test from every shipped version to the new one.
4. **Test infrastructure.** Add `room-testing` (plus `kspTest` for a
   test-only future database) and add `app/schemas` to the **debug** assets so
   Robolectric's `MigrationTestHelper` can read them (Robolectric only sees the
   variant's merged assets, not test-source-set assets). Debug APKs carry the
   small schema JSON; release APKs do not. `mergeDebugAssets` depends on
   `kspDebugKotlin` so a newly exported schema is visible in the same build.

## Verification

Robolectric unit tests on an on-disk database:

- A v1 database created from the committed schema with raw SQL rows opens
  with the production configuration (identity hash matches) and its rows are
  read back unchanged through `PersonalEventDao`, including after
  close/reopen.
- A guard test fails if `LevanaDatabase.VERSION` changes without a committed
  schema JSON for every version and a complete migration chain from 1.
- **Unsupported upgrade:** a test-only database class at version 2 with no
  migration fails to open a v1 file with `IllegalStateException`; the v1 rows
  are still there when reopened with the production configuration.
- **Unsupported downgrade:** a file marked with a newer `user_version` fails
  to open with the production configuration and is not wiped.
- **DataStore:** `LocationModeMigration` (the only DataStore upgrade step)
  keeps `saved_locations` and other settings intact.
- **Emulator upgrade:** install the `master` debug build, create a personal
  event and a saved location, install this branch's build over it with
  `adb install -r`, and confirm both are still present.

Plus `./gradlew :app:testDebugUnitTest`, `ktlintCheck` and
`assembleDebug`/`assembleRelease` regression checks.

## Results

- `LevanaDatabaseUpgradeTest` (4) and `LocationModeMigrationTest` (5) pass;
  full `:app:testDebugUnitTest` passes (514 tests). `assembleDebug` and
  `assembleRelease` succeed; the schema JSON is in the debug APK only.
- Mutation checks: re-adding `fallbackToDestructiveMigration()` fails the
  upgrade and downgrade tests; changing `PersonalEvent` without a version bump
  fails the v1 tests; bumping `VERSION` without a schema/migration fails the
  guard test.
- Emulator (isolated `Levana_Agent_Template_API_36_1`): `master` debug build
  (versionCode 300) seeded through the UI with event "Upgrade check keep-me"
  (5 Tishrei 5787, yahrzeit rules) and saved location Tel Aviv; this branch
  (versionCode 301) installed with `adb install -r`. After relaunch both are
  present and Tel Aviv is still the active city; no fatal log entries.
- Limitation: there is no shipped v2 yet, so no real migration is exercised.
  The first schema change must add one plus its MigrationTestHelper test.

## Out of scope

Full user backup/restore (levana-a10), Android Auto Backup policy, CI.

---
id: TASK-4
title: Remove the currently irrelevant Minhag selection
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-21T11:19:29Z"
updated_date: "2026-10-22T17:54:00Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-mhf
priority: medium
type: task
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace the previous proposal to make general Minhag selection meaningful with removal of that selection. Owner decision: a general Ashkenazi/Sephardi/Yemenite/Chabad selector is not relevant at present. Do not implement community profiles or infer new religious defaults.

Source review for this task rewrite (2026-10-22): the Minhag enum is persisted through PreferencesRepository, UserPreferences and Settings MVI state/intent, and shown in a Settings dialog. No calculation consumer was found. The same Settings group also contains independent, functional yahrzeit rules; removing the whole group would remove real functionality.

Implementation scope and repercussions:

1. Settings UI and MVI plumbing
- Remove the general Minhag row, selected-value summary, dialog and dialog state, label helper, SetMinhag intent/handler, and corresponding SettingsState field.
- Retain all four yahrzeit controls: leap-year Adar, first-year behavior, optional 1 Nissan for 30 Adar I, and missing-day behavior for 30 Cheshvan/Kislev.
- Rename the remaining group to Yahrzeit settings (with natural Hebrew wording). Remove any leading/orphan divider left by deleting the selector. Preserve accessible labels, focus order, touch targets and RTL layout.

2. Domain and persistence compatibility
- Remove the unused UserPreferences.minhag field, repository decoding/saving API and imports, and the Minhag enum after confirming there are no remaining runtime consumers. Fix the YahrzeitRules KDoc link to the removed enum.
- Existing DataStore files may contain the legacy string key "minhag" with any former enum value or an unknown value. Prefer leaving this inert key ignored; document that choice. If cleanup is chosen, remove only that key without clearing or recreating the preference store. Do not reset other preferences, yahrzeit overrides, saved/active locations or notification choices.
- This is not a Room schema change and requires no Room version bump or migration. Existing personal events and contact birthdays must remain intact.

3. Preserve actual religious/calculation behavior
- Keep Israel/Diaspora selection, candle-lighting offset, selected zmanim and independently configured yahrzeit rules and defaults.
- Removing the unused selection must not change parsha, holidays, zmanim, candle-lighting/havdalah calculations, recurrence, calendar indicators or notification content/timing.
- Do not replace the removed preference with a hidden religious profile or silently map old values to yahrzeit choices.

4. English/Hebrew text and help
- Remove selector-only labels/descriptions and community-choice strings from all relevant resource files once unused.
- Update event_yahrzeit_rules_description, which currently points to Settings -> Minhag, to the new Yahrzeit settings location in both languages.
- Clarify "Follow annual minhag" / its Hebrew equivalent as following the annual yahrzeit setting, so it cannot imply the removed community selector. Preserve FOLLOW_ANNUAL behavior and stored value.
- Do not blindly delete every occurrence of the word minhag: references to actual observance customs can remain where accurate.

5. Tests and verification
- Update fixtures/imports and tests that construct Minhag values or call saveMinhag, including AlarmSettingsTest, DailyCategorySettingsTest and DailyNotificationWorkerTest. Preserve their substantive checks for unrelated preference changes and notification behavior rather than deleting those checks.
- Verify an existing preference store carrying a legacy minhag key opens successfully and preserves the other saved settings and yahrzeit overrides.
- Verify the selector is absent and retained yahrzeit controls remain usable and persistent in English/Hebrew, including RTL. Run relevant recurrence, preferences and notification tests plus a build; use existing UI/screenshot coverage where available.

6. Documentation and related backlog
- Update TASKS.md original item #13 to the removal decision. Reconcile direct current requirements/claims in PROJECT_SPEC.md (minhag selection/defaults/support), ARCHITECTURE_SPEC.md storage examples and MVP plan completion claims.
- Annotate the superseded selector requirements in INCREMENT_08 and settings-md3-redesign, and update the current grouping/help guidance in fix-adar-and-year-followups. Keep historical implementation/verification records identifiable as historical; avoid rewriting unrelated history.
- Coordinate with levana-334 (documentation reconciliation) so the old defaults-mapping feature is not reintroduced as unfinished work.
- levana-cx4 (explicit zmanim calculation methods) remains a separate task and does not depend on a general Minhag selector. Future full backup levana-a10 should not reintroduce the obsolete field as an active setting.
- Limit documentation work to consequences of this removal; no broad settings redesign, calculation-method feature or new yahrzeit policy is requested.

Deliver implementation through the project's spec-first worktree workflow, record verification and compatibility policy, and update this issue with results.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The general Minhag selector, its dialog and unused model/MVI/repository APIs are removed. Independent yahrzeit controls remain under an accurately named localized group, and event help points to that group. Former or unknown legacy minhag key values are tolerated without resetting user data or other preferences; the key-retention/cleanup policy is documented. Existing yahrzeit choices/defaults, Israel/Diaspora, candle-lighting offsets, calculations, recurrence and notification behavior are preserved. Relevant fixtures and regression tests are updated without weakening unrelated notification assertions; build and targeted checks pass. English/Hebrew/RTL usability is verified. Current specs/backlog reflect removal, with historical selector requirements clearly superseded and explicit calculation-method work kept separate.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on branch fix/remove-minhag-selection (worktree /Users/daniel/.paseo/worktrees/15o7vaq5/remove-minhag-selection). Commits: 8ac8d9e spec (specs/rfes/remove-minhag-selection.md), e41d6d4 code+tests, ffef5b8 docs.
Changes: removed Minhag row/dialog/label, SetMinhag intent+handler, SettingsState/UserPreferences.minhag, PreferencesRepository MINHAG key/decoding/saveMinhag, Minhag enum, selector-only strings (EN/HE). Group renamed settings_group_yahrzeit = 'Yahrzeit settings' / 'הגדרות יארצייט' (leading divider removed; 4 rows unchanged). event_yahrzeit_rules_description -> Settings → Yahrzeit settings; 'Follow annual minhag' -> 'Follow the leap-year Adar setting' / 'לפי הגדרת אדר בשנה מעוברת' (FOLLOW_ANNUAL unchanged). YahrzeitRules KDoc fixed. Legacy policy: 'minhag' DataStore key left inert (unread, never written/removed), documented in spec + Keys comment. No Room change.
Docs: TASKS #13, PROJECT_SPEC, ARCHITECTURE_SPEC, MVP plan, INCREMENT_08 + settings-md3-redesign superseded notes, fix-adar-and-year-followups, fix-nissan-row-toggle.
Tests: new LegacyMinhagPreferenceTest (SEPHARDI/CHABAD/unknown/empty legacy values; offset, Israel, yahrzeit overrides preserved; key retained across saves). AlarmSettingsTest/DailyCategorySettingsTest/DailyNotificationWorkerTest use another unrelated pref, all assertions kept. Full :app:testDebugUnitTest 515 tests 0 failures; :app:lintDebug + assembleDebug OK. No Roborazzi coverage of Settings exists.
Emulator (emulator-5556, session /tmp/levana-emu-y4r25nai): upgraded from baseline e9aa663 seeded with minhag=SEPHARDI + offset 44 + non-default yahrzeit rules -> all preserved, key retained; selector absent; 4 rules changed + persisted across force-stop in EN and HE/RTL; event help verified EN/HE; 0 fatal logs. Concurrency with levana-8pf: overlapping feature commands 14:40:34-14:43:48Z (12 iterations of my loop). Evidence: /tmp/levana-emu-y4r25nai/artifacts, /private/tmp/levana-parallel-settings.GHCWeN/levana-mhf-*.
Limitations: TalkBack speech not tested (hierarchy only). Awaiting approval/merge to master; not closed.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-mhf",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-22T14:23:34Z",
  "closed_at": "2026-10-22T17:54:00Z",
  "close_reason": "Approved by owner, merged and pushed to origin/master at 1649ccb. Combined verification: 525 unit tests, zero failures/errors/skips; lintDebug and assembleDebug passed. Concurrent emulator feature testing independently verified at 14:40:34–14:43:48 UTC on emulator-5558 and emulator-5556. Remaining TalkBack spoken-output verification tracked separately in levana-bhe.",
  "external_ref": "specs-import:original-13",
  "metadata": {
    "import_key": "original-13",
    "review_basis": "source-reviewed-task-rewrite",
    "import_source": "specs/TASKS.md — original backlog #13 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```

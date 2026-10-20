# Bug-fix review

All fixing and independent verification agents use Claude Opus 5.5 with Medium reasoning. Each bug has a separate fixing worktree. Verification uses fresh agents with only the bug requirements and candidate patch; fixing-agent conversations and plans are excluded. No emulator, ADB, or device installation is used.

The `review/bugfixes` branch combines accepted fixes and independent regression tests for review. `master` is unchanged. Individual branches and worktrees are preserved.

| Bug | Fix branch | Verification branch | Status |
| --- | --- | --- | --- |
| Shabbat/Yom Tov transitions | `fix-holiday-transitions` | `verify-holiday-transitions` | PASS: 17-year independent sweep and old-code regression checks |
| Evening Omer counts | `fix-evening-omer-count` | `verify-evening-omer-count` | PASS: independent multi-year boundaries and baseline-behavior mutation check |
| All-day/multi-day calendar indicators | `fix-calendar-event-indicators` | `verify-calendar-event-indicators` | PASS after fixing independently discovered midnight DST boundary |
| Immediate notification rescheduling | `fix-notification-rescheduling` | `verify-notification-rescheduling` | PASS after extending refresh to daily categories; real WorkManager path verified |
| Reminder scheduling gaps | `fix-notification-lookahead` | `verify-notification-lookahead` | PASS after preserving overdue undelivered alarms; previous candidate fails regressions |
| Notification identifier collisions | `fix-notification-identifiers` | `verify-notification-identifiers` | PASS: 21 independent tests and six reproduced baseline collisions |
| Blocking provider/file IO | `fix-background-provider-io` | `verify-background-provider-io` | PASS after correcting stale results and late errors; 327 tests twice with no failures or skips |
| Stale calendar/day event data | `fix-reactive-calendar-refresh` | `verify-reactive-calendar-refresh` | PASS: 15 independent tests; seven applicable baseline regressions all fail |

## Combined validation

- Final combined suite: **488 tests across 68 suites, 0 failures, errors or skips**.
- `:app:assembleDebug` and `:app:lintDebug` both passed.
- Final command: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon`, using Android Studio's bundled JDK.
- Independent integration verdict: **PASS**, verifier commit `e913191`. The approved production code is `2f2a44e`; subsequent review-branch changes only retain additional tests and this report. The production diff is empty.
- The independently discovered settings-only overdue candle-lighting cancellation is fixed and verified through the real rebuilder with a controlled clock. Settings rebuilds keep the pending alarm; daily catch-up cancels it and posts once; disabling cancels it.

## Verification limits

Tests run on the JVM, including Robolectric where Android APIs are needed. Calendar provider tests use fake provider data; real device delivery, platform scheduling delays, and device-specific provider behavior are not tested. No emulator is used, as requested.

Provider and stream IO are off the UI thread. File parsing and export serialization remain synchronous; imports are capped at 1 MiB. Existing handling of active provider failures outside permission/cancellation errors is unchanged.

## Independently reviewed commits

| Fix branch | Final candidate | Verifier evidence |
| --- | --- | --- |
| `fix-holiday-transitions` | `aa99d70` | `dca4606` |
| `fix-evening-omer-count` | `2225da7` | `aff910e` |
| `fix-calendar-event-indicators` | `d6b9b94` | `4c6dc96` |
| `fix-notification-rescheduling` | `12b4ec0` | `9bb8534` |
| `fix-notification-lookahead` | `dc22b79` | `579ec02` |
| `fix-notification-identifiers` | `966ebec` | `92025ab` |
| `fix-background-provider-io` | `ae05348` | `70d0e4b` |
| `fix-reactive-calendar-refresh` | `8bfe675` | `101fe25` |

The combined branch also includes `fix-settings-overdue-candle` (`d5bac28`), covering the interaction between settings rebuilds and delayed candle-lighting delivery. Independent integration verification covers that correction and the final IO cancellation correction together. No requested fix or verification remains pending.

# Bug-fix review

All fixing and independent verification agents use Claude Opus 5.5 with Medium reasoning. Each bug has a separate fixing worktree. Verification uses fresh agents with only the bug requirements and candidate patch; fixing-agent conversations and plans are excluded. Initial development and JVM verification excluded emulators; subsequent emulator verification was explicitly authorized by the user.

The eight original fixes and two reminder follow-ups are approved for merge to master. Individual fixing and verification branches/worktrees are preserved. The [original task list](specs/TASKS.md) records completion statuses and remaining work.

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

## Original combined validation (6118c98)

- Final combined suite: **488 tests across 68 suites, 0 failures, errors or skips**.
- `:app:assembleDebug` and `:app:lintDebug` both passed.
- Final command: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon`, using Android Studio's bundled JDK.
- Independent integration verdict: **PASS**, verifier commit `e913191`. The approved production code is `2f2a44e`; subsequent review-branch changes only retain additional tests and this report. The production diff is empty.
- The independently discovered settings-only overdue candle-lighting cancellation is fixed and verified through the real rebuilder with a controlled clock. Settings rebuilds keep the pending alarm; daily catch-up cancels it and posts once; disabling cancels it.

## Verification limits

Initial tests ran on the JVM, including Robolectric with fake calendar providers. Later emulator verification exercised real providers and alarm delivery; physical devices and OEM-specific scheduling behavior remain untested. See the follow-up validation below.

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

The combined branch also includes `fix-settings-overdue-candle` (`d5bac28`), covering the interaction between settings rebuilds and delayed candle-lighting delivery. Independent integration verification covers that correction and the final IO cancellation correction together.

## Reminder follow-ups and emulator verification

- Final merge check on the exact production candidate plus task/report updates: **509 tests across 71 suites, zero failures/errors/skips**; `:app:assembleDebug` and `:app:lintDebug` passed. Command: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon`.
- Post-midnight Omer nightfall dates: candidate `dce8f19`, independent verifier `44309ce`; 528 JVM tests and debug build passed on the verifier branch.
- Device time-zone changes: candidate `5b9331b`, independent verifier `f82161e`; 510 JVM tests and debug build passed on the verifier branch.
- Combined production candidate `d2832e2` applies both follow-ups cleanly on `6118c98`. Its app code matches the emulator-tested build; later verification commits add only tests/spec/evidence.
- Combined emulator verifier: 511 JVM tests, zero failures, and successful debug build. This includes two verifier-only expectation tests retained in its worktree; separate verifier test counts are not additive.
- Emulator: `emulator-5554`, `Medium_Phone_API_36.1`, Android 16/API 36. APK: 0.3.0-debug / 300; SHA-256 `f2b7644e116aa2e9ae12b54da3f37c5d4776dd2117aa6a54d86aad7ecccc49a9`; installed hash matched.
- Real post-midnight Omer delivery passed for St Petersburg count 33, consecutive evenings, first night in Nome and last night in Stornoway. A real daily worker run after midnight kept the previous evening's alarm once. Missing nightfall retained morning delivery; settings switches cancelled/reinstated previous-evening alarms correctly.
- Actual time-zone broadcasts reached the manifest receiver with the UI backgrounded and the same PID, and also started a process that was not running. Future mornings moved to 07:00 Moscow and delivered there. Manual clock changes, eastward date crossing and combined post-midnight/time-zone scenarios passed.
- The earlier emulator run exercised all eight original fixes, including real Contacts/Calendar providers, notification identities, settings refresh and reactive data. Some photo/cancellation/race scenarios remained JVM-only.

## Remaining findings

Moving east after the new local morning time preserves an already-overdue pending reminder, so it may arrive late. Westward travel can repeat an already-delivered Omer morning notice. Alarms outside a date-shifted reconciliation window can retain old times, including a disabled alarm; the receiver checks settings before posting, but that particular out-of-window firing was not exercised.

Post-midnight candle-lighting reminder dates and havdalah end dates need separate fixes. The calendar grid's two-chip limit was accepted by the user. The final follow-up emulator run did not directly exercise candle-lighting delivery, fast/personal-event reposts with real records or the out-of-window disabled alarm.

Emulator settings and backed-up app/WorkManager state were restored. Detailed reports and runtime evidence remain in the retained `verify-emulator-all-fixes` and `verify-reminder-followups-emulator` branches/worktrees. Their test harnesses and bulk artifacts are not part of the production merge. See [tasks and remaining findings](specs/TASKS.md).

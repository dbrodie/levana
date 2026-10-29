---
id: TASK-28
title: Fix notification stepper accessibility and disabled boundary buttons
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-23T05:53:43Z"
updated_date: "2026-10-23T06:35:27Z"
labels:
  - accessibility
dependencies: []
references:
  - levana-419
priority: medium
type: bug
ordinal: 28000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: accessibility source review of master 1649ccb, SettingsScreen.kt notification steppers around lines 976–1048, 1063–1096 and 1154–1187. Candle-lighting morning time, hours-before advance notice, holiday days-before and Omer morning time duplicate decrement/increment layouts. Bounds are checked inside callbacks, but IconButtons remain enabled at the limits and silently do nothing. Accessibility labels are generic Earlier/Later/Increase/Decrease without naming the affected setting or its value.

Create a focused reusable SettingsStepperRow for these four settings. Pass setting label, formatted value, bounds/step or canIncrease/canDecrease, and callbacks. Set enabled accurately at each boundary. Supply localized contextual accessibility action labels and current-value semantics, ensuring the individual increase/decrease controls remain operable; do not merge distinct actions into a single toggle or erase their semantics. Preserve existing values, units, bounds, step sizes, preference persistence and notification rescheduling.

Verify lower/upper bounds, one callback per activation, current-value/context announcements, keyboard/TalkBack, English/Hebrew RTL and appropriate layout at large font sizes. This concerns notification settings; it is separate from event-editor year limits/UI tasks levana-v57 and levana-oub.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All four notification steppers use a shared component. Boundary actions are disabled visibly and semantically; available actions change the value once by the existing step. Localized labels identify the setting and accessible state exposes its current formatted value. Persistence, notification rescheduling, existing bounds and units are preserved. Focused interaction/semantics tests and runtime English/Hebrew checks document results.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
IMPLEMENTED/VERIFIED — AWAITING PARENT INTEGRATION (do not close before merge).
Branch fix/notification-stepper-a11y, head ed7659e (commits f725671 spec, 2217353 fix, 5b0fbb8 keyboard focus fix, ed7659e verification doc). Worktree /Users/daniel/.paseo/worktrees/15o7vaq5/notification-stepper-a11y.
Files: ui/settings/SettingsStepperRow.kt (new), SettingsScreen.kt (four steppers -> SettingsStepperRow, removed unused Remove icon import, NotificationsSection private->internal for tests), res/values{,-iw}/accessibility_419_strings.xml (settings_stepper_action '%1$s, %2$s'), test SettingsStepperRowTest.kt, specs/rfes/notification-stepper-a11y.md.
Behavior: boundary buttons disabled (visible+semantic); labels '<action>, <setting>'; buttons stateDescription = formatted value; title+value merged node; bounds/steps/intents unchanged (360..720/30, 1..6/1, 0..14/1, 360..720/30). Keyboard activation reaching a bound moves focus to the opposite button (device showed focus jumping to toolbar Back otherwise).
Validation: ./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest -> BUILD SUCCESSFUL; 541 tests, 0 failures/errors/skipped; SettingsStepperRowTest 16/16; lint 85 pre-existing warnings, none in touched files.
Emulator: own read-only session /tmp/levana-emu-in_0y4_t serial emulator-5558 (stopped). Artifacts /tmp/levana-emu-in_0y4_t/artifacts (commands.log timestamped, screenshots, UI XML, logcat, TalkBack logs 56-66, gradle logs, lint report). Verified EN bounds for all four, step counts, persistence after force-stop, DailyNotificationWorker reschedule, keyboard Tab/Enter/DPAD, Hebrew RTL, font scale 2.0 EN/HE, actual TalkBack speech EN+HE (value, action, setting; new value on activation).
Limitation: TalkBack speech for a disabled button not captured (injected input cannot drive explore-by-touch/TalkBack linear nav); covered by hierarchy enabled=false + Robolectric. Follow-up filed levana-851 (12:00 AM noon label, plurals).
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-419",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T05:56:04Z",
  "closed_at": "2026-10-23T06:35:27Z",
  "close_reason": "Reviewed, emulator-verified and merged/pushed to origin/master at 6a75ee8 (first three at 296c6c2). Final combined verification: 576 unit tests, zero failures/errors/skips; lintDebug and assembleDebug passed. English/Hebrew RTL and runtime evidence preserved under individual emulator session artifacts. TalkBack speech logs inspected. Remaining keyboard focus/manual gesture and checkbox speech issues tracked in levana-wf2 and levana-bhe; pre-existing noon/plural wording bugs tracked in levana-851.",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```

# Fix: Add/Edit event form layout shift on yahrzeit toggle

## Problem
In the Add/Edit event form (`AddEditEventScreen`), the "Use yahrzeit Adar rules" card
only shows its explanation text (`event_yahrzeit_rules_description`) while the switch is on.
Toggling the switch inserts/removes that text, pushing the Notes field and Save button
down/up by ~95px. Fields jump under the user's finger, causing mis-taps.

## Fix
Render the explanation as always-visible supporting text under the switch label
(title + subtitle in the same column, next to the switch), matching settings rows that
have a subtitle. The card height no longer depends on the switch state, so the fields
below stay put. The text wraps freely (no maxLines), so it is not truncated in English
or Hebrew (RTL). The italic style is dropped to match other subtitle rows.
The whole row is made toggleable so tapping the label also flips the switch.
Saving / the `useYahrzeitRules` flag behaviour is unchanged.

## Test plan
- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
- On the emulator: open drawer → My events → Events → +, dump UI with the switch off and on;
  Notes and Save bounds must be identical in both states. Check the explanation is fully shown.
- Check in Hebrew (RTL) that the text wraps and is not truncated.

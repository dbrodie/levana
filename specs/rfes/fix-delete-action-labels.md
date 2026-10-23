# Fix: delete icon buttons announce only "Delete"

Beads: `levana-q5h`.

## Problem

Three list rows have a separately focusable trash `IconButton` whose only
accessibility label is a generic "Delete":

- Events → Birthdays: `BirthdayCard` (`R.string.common_delete`);
- Events → Custom events: `EventCard` (`R.string.common_delete`);
- Settings → Locations → Saved locations (`R.string.location_delete`).

When a screen-reader or keyboard user moves between actions, every button
sounds the same and does not say which row it acts on.

## Fix

Give each delete button a localized, accessibility-only content description
naming its target. Nothing visible changes: same trash icons, tint, size,
layout, confirmation dialogs (birthday "Remove", event "Delete"; saved
locations still delete immediately, as today) and callbacks.

New resources in an issue-specific file, `values/accessibility_q5h_strings.xml`
and `values-iw/accessibility_q5h_strings.xml`:

| Name | English | Hebrew |
|---|---|---|
| `a11y_remove_birthday_for` | Remove Hebrew birthday for %1$s | הסרת יום ההולדת העברי של %1$s |
| `a11y_delete_event` | Delete event %1$s | מחיקת האירוע %1$s |
| `a11y_delete_saved_location` | Delete saved location %1$s | מחיקת המיקום השמור %1$s |

The birthday label keeps the existing "remove" wording (removing a birthday
from a contact, matching its dialog), distinct from deleting a personal event.

User text is passed only as a format argument (`stringResource(id, name)`), so
`%`, quotes or markup in a name are not interpreted. A blank target name falls
back to the existing generic label. A tiny `@Composable` helper,
`targetedLabel(targeted, target, generic)` in `ui/TargetedLabel.kt`, does this
in one place; no new button component is introduced because the three buttons
differ in icon and tint and are otherwise a plain `IconButton`.

Edit/open (card or row click) and delete stay separate actions; no row-level
custom accessibility actions are added.

To test the rows, `BirthdaysTab`, `CustomEventsTab` (EventsScreen.kt) and
`LocationsSettingsContent` (SettingsScreen.kt) change from `private` to
`internal`. SettingsScreen changes are limited to the saved-location delete
button and that visibility change.

## Out of scope

- Other icon buttons and generic labels; visible text; dialog wording.
- Adding a confirmation to saved-location deletion.
- Row redesigns or custom accessibility actions.

## Test plan

- Robolectric Compose tests:
  - Birthdays: with two contacts, each delete button has the target-specific
    label and `Role.Button`; the visible text has no long label; tapping
    Sarah's delete opens the remove dialog; Cancel does not call the callback;
    Remove calls it once with Sarah's lookup key; tapping the card calls edit
    with that key, not delete.
  - Custom events: same for two events, including a title containing `%` and
    quotes; Delete calls back with that event only.
  - Saved locations: each delete button labelled with its location and
    invokes `onDelete` with its id once.
  - Hebrew (`iw` locale) labels and RTL direction.
  - Blank name falls back to the generic label.
- `:app:assembleDebug`, `:app:lintDebug`, `:app:testDebugUnitTest`.
- Emulator, English and Hebrew: UI hierarchy shows the labelled buttons,
  screenshots show unchanged visuals; delete/cancel flows and persistence
  after relaunch; keyboard focus and activation. TalkBack spoken output is
  claimed only if actually run.

## Verification (2026-10-23)

- Robolectric: `DeleteActionLabelsTest` 10/10, `SavedLocationDeleteLabelTest`
  3/3. Full `:app:testDebugUnitTest` 538 tests, 0 failures, 0 errors;
  `:app:assembleDebug` and `:app:lintDebug` succeed (0 lint errors; no lint or
  ktlint findings in touched files).
- Emulator `emulator-5556` (owned read-only session, commit `2e8451a`),
  evidence under `/tmp/levana-emu-hkwefswd/artifacts/`:
  - English hierarchy: "Remove Hebrew birthday for Sarah Cohen/David Levi",
    "Delete event Anniversary", `Delete event Siyum "Bava" 100%`,
    "Delete saved location Tel Aviv/Haifa"; no long visible text; screenshots
    show the same trash icons and layout.
  - Birthday: Cancel keeps the birthday; Remove (via keyboard Tab + Enter on
    David's button) removes only David's contact data row. Card tap opens
    Sarah's editor. Event: Cancel keeps the event; Delete removes only
    "Anniversary". Saved location: tap removes only Tel Aviv; keyboard Enter on
    the focused Haifa button removes Haifa. Card and delete are separate
    keyboard stops.
  - Hebrew (`iw` app locale): RTL with the trash on the left; labels
    "הסרת יום ההולדת העברי של Sarah Cohen", `מחיקת האירוע Siyum "Bava" 100%`,
    "מחיקת המיקום השמור Tel Aviv/Haifa"; Hebrew dialog and cancel unchanged.
  - Birthdays, events and saved locations persist across force-stop/relaunch.
  - TalkBack 16 (developer verbose speech log), focus moved with D-pad/Tab:
    spoken "Remove Hebrew birthday for Sarah Cohen. Button",
    `Delete event Siyum "Bava" 100%. Button`,
    "Delete saved location Tel Aviv. Button", and in Hebrew
    "מחיקת המיקום השמור Tel Aviv. Button" (role word from TalkBack's English
    system locale).
- Limitations: TalkBack speech is from TalkBack's verbose speech log, not
  audio capture. Hebrew TalkBack was checked for the saved-location button
  only; the Hebrew birthday/event labels were checked in the hierarchy and
  Robolectric. adb-injected taps/swipes bypass explore-by-touch, so TalkBack
  gestures (swipe, double-tap) were not exercised.

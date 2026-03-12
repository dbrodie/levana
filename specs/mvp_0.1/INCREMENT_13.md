# Increment 13: Android Contacts Integration

## Summary

Hebrew birthdays stored directly in the Android Contacts database as custom MIME type rows. Birthdays are managed separately from personal events (yahrzeits, custom) which remain in Room. A tabbed Events screen provides CRUD for both. DayDetail and calendar dots show birthdays alongside custom events.

## What Was Built

### Contact Birthday Repository
- `ContactBirthdayRepository` handles all Contacts DB operations
- Custom MIME type: `vnd.android.cursor.item/com.levana.hebrew_birthday`
- Stores Hebrew day, month, year as DATA1/DATA2/DATA3 columns on a contact's raw data row
- CRUD: `setBirthday`, `removeBirthday`, `getAll`, `getBirthdayForContact`
- Query helpers: `getBirthdaysForDate`, `getBirthdayDatesForGregorianMonth`, `getBirthdayDaysForHebrewMonth`

### Hebrew Date Matcher
- `HebrewDateMatcher` handles Adar leap-year logic for both birthdays and yahrzeits
- Birthday rules: Adar maps to whichever Adar exists in the target year
- Yahrzeit rules (Shulchan Aruch): Adar of non-leap year maps to Adar II of leap year

### Contact Birthday UI
- `ContactBirthdayScreen` — add/edit birthday with contact picker and Hebrew date picker
- `ContactBirthdayViewModel` / `ContactBirthdayState` / `ContactBirthdayIntent` — MVI pattern
- Auto-launches system contact picker (`ActivityResultContracts.PickContact`) for new birthdays
- Shows contact photo and name after selection

### Tabbed Events Screen
- `EventsScreen` refactored with `HorizontalPager`: Birthdays tab + Custom Events tab
- Birthdays tab: list of contacts with Hebrew birthdays, photos, edit/delete actions
- Custom Events tab: existing personal events (yahrzeits, etc.)
- FAB context-aware: adds birthday on Birthdays tab, adds event on Events tab

### Calendar Event Unification
- `CalendarEvent` sealed interface with `Birthday` and `CustomEvent` variants
- `DayDetailScreen` displays both types in a unified personal events section
- `CalendarViewModel` includes birthday dates in dot indicators

### PersonalEvent Simplification
- Removed `EventType` enum and Room `Converters`
- Added `useYahrzeitRules: Boolean` column to `PersonalEvent` (simpler, more direct)
- `AddEditEventScreen` uses a checkbox instead of a type dropdown

### Permissions
- `READ_CONTACTS` + `WRITE_CONTACTS` declared in manifest
- Runtime permission requested when user first accesses birthday features
- Permission check gates both read and write before enabling birthday tab

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Birthday storage | Android Contacts DB | Birthdays are a property of contacts, not app-internal data |
| Personal events storage | Room (unchanged) | Yahrzeits/custom events aren't tied to contacts |
| Custom MIME type | `com.levana.hebrew_birthday` | Namespaced to app, stored per raw contact |
| Date matching | `HebrewDateMatcher` utility | Centralizes Adar leap-year logic for both birthdays and yahrzeits |
| Event unification | `CalendarEvent` sealed interface | Single list in DayDetail without merging storage layers |
| EventType removal | `useYahrzeitRules` boolean | Simpler than an enum; only distinction that matters is Adar rules |

## Files Modified

- `AndroidManifest.xml` — Added `READ_CONTACTS` and `WRITE_CONTACTS` permissions
- `MainActivity.kt` — Added `ContactBirthdayRoute` navigation
- `PersonalEvent.kt` — Replaced `eventType` with `useYahrzeitRules`
- `LevanaDatabase.kt` — Removed `Converters` reference
- `PersonalEventRepository.kt` — Updated for simplified `PersonalEvent`
- `AppModules.kt` — Added `ContactBirthdayRepository` and birthday ViewModel to Koin
- `CalendarViewModel.kt` — Added birthday dot indicators
- `DayDetailScreen.kt` — Unified birthday + custom event display
- `DayDetailState.kt` — Uses `CalendarEvent` list
- `DayDetailViewModel.kt` — Loads both birthdays and custom events
- `EventsScreen.kt` — Tabbed layout with Birthdays and Custom Events tabs
- `EventsState.kt` — Added `birthdays` and `hasContactsPermission` fields
- `EventsViewModel.kt` — Loads birthdays from `ContactBirthdayRepository`
- `EventsIntent.kt` — Added `DeleteBirthday` and `ContactsPermissionGranted`
- `AddEditEventScreen.kt` — Simplified for yahrzeit checkbox
- `AddEditEventState.kt` — Replaced `eventType` with `useYahrzeitRules`
- `AddEditEventIntent.kt` — Updated intents
- `AddEditEventViewModel.kt` — Updated for simplified model
- `Routes.kt` — Added `ContactBirthdayRoute`

## New Files

- `ContactBirthdayRepository.kt` — Contacts DB CRUD for Hebrew birthdays
- `HebrewDateMatcher.kt` — Adar-aware date matching for birthdays and yahrzeits
- `ContactBirthday.kt` — Domain model for a contact's Hebrew birthday
- `CalendarEvent.kt` — Sealed interface unifying birthday and custom event types
- `ui/birthday/ContactBirthdayScreen.kt` — Add/edit birthday composable
- `ui/birthday/ContactBirthdayViewModel.kt` — Birthday screen ViewModel
- `ui/birthday/ContactBirthdayState.kt` — Birthday screen state
- `ui/birthday/ContactBirthdayIntent.kt` — Birthday screen intents

## Deleted Files

- `Converters.kt` — No longer needed without `EventType` enum
- `EventType.kt` — Replaced by `useYahrzeitRules` boolean

## Acceptance Criteria

- [x] Adding a birthday picks a contact and sets a Hebrew date
- [x] Birthdays tab shows all contacts with Hebrew birthdays and their photos
- [x] Birthday data is stored as a custom MIME row in Android Contacts
- [x] Deleting a birthday removes the custom data row from Contacts
- [x] Custom events (yahrzeits, etc.) still work independently in Room
- [x] DayDetail shows both birthdays and custom events for the selected date
- [x] Calendar shows dot indicators for days with birthdays
- [x] `./gradlew build` passes

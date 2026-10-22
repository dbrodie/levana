# Remove the general Minhag selection

Beads: `levana-mhf` (TASKS.md original #13).

## Problem

Settings has a "Minhag" group whose first row opens an Ashkenazi / Sephardi /
Yemenite / Chabad dialog. The value is stored in DataStore (`"minhag"`), carried
through `UserPreferences.minhag` and the Settings MVI state/intent, but nothing
reads it: parsha, holidays, zmanim, candle lighting, recurrence and
notifications ignore it. The owner decided a general community selector is not
relevant at present, so it should go rather than be made meaningful.

The same group also holds the four yahrzeit rules (leap-year Adar, first year,
also mark 1 Nissan, 30 Cheshvan/Kislev). Those are real, independent settings
and stay.

## Change

### Settings UI / MVI

- Remove the Minhag row, `MinhagDialog`, `showMinhagDialog`, `minhagLabel`,
  `SettingsIntent.SetMinhag`, its ViewModel handler and `SettingsState.minhag`.
- The remaining group keeps the four yahrzeit rows in the same order, with the
  leading divider (which separated them from the Minhag row) removed.
- Rename the group: string `settings_group_minhag` → `settings_group_yahrzeit`,
  English "Yahrzeit settings", Hebrew "הגדרות יארצייט". Row labels, icons,
  click/toggle semantics and dialogs are unchanged.

### Domain / persistence

- Delete `domain/model/Minhag.kt`, `UserPreferences.minhag`,
  `PreferencesRepository.Keys.MINHAG`, its decoding and `saveMinhag`.
- Fix the `YahrzeitRules` KDoc reference to `[Minhag]`.
- **Legacy key policy: leave it inert.** Existing DataStore files may contain
  `"minhag"` with any former or unknown string. The repository no longer reads
  or writes it; DataStore preserves unknown keys, so the store opens normally
  and every other preference is untouched. No cleanup migration is added: it
  would rewrite the store for no user benefit, and keeping the key costs a few
  bytes. Old values are not mapped onto yahrzeit rules or any other setting.
- No Room change, version bump or migration.

### Strings

- Remove now-unused `settings_minhag`, `settings_minhag_desc`,
  `settings_minhag_label`, `settings_minhag_{ashkenazi,sephardi,yemenite,chabad}`
  in English and Hebrew.
- `event_yahrzeit_rules_description`: point to Settings → Yahrzeit settings
  (Hebrew: הגדרות ← הגדרות יארצייט).
- `settings_yahrzeit_first_year_follow` ("Follow annual minhag" / "לפי המנהג
  השנתי") → "Follow the leap-year Adar setting" / "לפי הגדרת אדר בשנה מעוברת",
  which is what `FOLLOW_ANNUAL` does. The enum value and stored string are
  unchanged.

### Docs

- TASKS.md #13 → removal decision, Done once merged.
- PROJECT_SPEC.md: drop "general minhag selection" and the per-minhag
  defaults/support claims; keep the zmanim-method requirement (levana-cx4) as
  independent of any community selector.
- ARCHITECTURE_SPEC.md storage example; MVP plan completion claims annotated as
  superseded.
- INCREMENT_08, settings-md3-redesign: add a superseded note; keep history.
- fix-adar-and-year-followups, fix-nissan-row-toggle: update current group
  name/help guidance.

## Out of scope

Calculation profiles / zmanim methods (levana-cx4), new yahrzeit policy, any
broader settings redesign, the Halachic Times section (owned by a parallel
change).

## Test plan

- Update `AlarmSettingsTest`, `DailyCategorySettingsTest`,
  `DailyNotificationWorkerTest` to use another unrelated preference in place of
  minhag, keeping every assertion.
- New Robolectric test: a DataStore file containing `minhag` = `SEPHARDI` and
  an unknown value, plus candle offset, Israel flag and yahrzeit overrides,
  opens through `PreferencesRepository`; all those values are read back, a
  subsequent save keeps them and leaves the legacy key in place.
- `LocationModeMigrationTest` keeps checking that an unrelated legacy key
  survives the location migration.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` (plus screenshot tests
  if any cover Settings).
- Emulator (English and Hebrew/RTL): Minhag row absent; group titled
  Yahrzeit settings with four rows; change each rule, force-stop, relaunch,
  values persist; event-form help text shows the new location. Legacy key: an
  install upgraded from a build that stored a minhag value still opens with
  prior settings intact.

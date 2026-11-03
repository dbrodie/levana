# Store Listing

How Levana presents itself on Google Play, F-Droid and GitHub, and how that stays current (TASK-55). It covers the store text, each screenshot and what it depends on, and the in-app walkthrough, so a task can tell whether it changes any of them.

**Rule: nothing under `fastlane/` is committed without the owner's explicit approval.** This covers the text, screenshots, icon, feature graphic and changelogs. Agents may regenerate files to show the owner, then `git restore fastlane/` whatever is not approved.

## Store text

`fastlane/metadata/android/en-US/` is the single source for both stores. Play's listing is uploaded from it, and F-Droid reads it from the tagged source. English only.

| File | Play limit | F-Droid limit |
|---|---|---|
| `title.txt` | 30 | 50 |
| `short_description.txt` | 80 | 80 |
| `full_description.txt` | 4000 | 4000 |
| `changelogs/<versionCode>.txt` | 500 ("What's new") | 500 |

- Every claim must be true of the shipped app; check against `specs/PROJECT_SPEC.md` and the code. Don't claim the app is fully offline: the GitHub update check (sideloaded installs) and naming the current location use the network.
- `images/icon.png` (512×512) and `images/featureGraphic.png` (1024×500) are uploaded too. The feature graphic still says "Fully offline"; fix it before its next upload.
- README.md's Features section is written by hand. Keep it consistent with the full description.

## Screenshots

There are 8 phone screenshots, 1080×1920 (9:16, Play's portrait size for promotional placements). They are RGB PNG without alpha, which Play requires. `StoreScreenshotTest` renders them under Robolectric into `fastlane/metadata/android/en-US/images/phoneScreenshots/<n>_<slug>.png`. Both stores show them in file-name order.

The common setup is the harness's fixed day, Tuesday 17 November 2026 (7 Kislev 5787), at noon in Jerusalem. The current location is Jerusalem, with New York and London saved. The seeded data:
- a wedding anniversary on 7 Kislev;
- a contact's Hebrew birthday on 8 Kislev (Sarah Cohen);
- a yahrzeit on 9 Kislev (Moshe ben Avraham);
- two phone-calendar events: a Shabbat dinner on Friday and a kiddush on Shabbat.

The window is 432×768dp at 400dpi, with no system bars.

| # | File | Shows | Depends on |
|---|---|---|---|
| 1 | `1_hebrew-months.png` | Month view in Hebrew months (Kislev 5787, with Rosh Chodesh, parshiyot, candle lighting and Chanukah), day sheet collapsed | Month grid and day cells, holiday/parsha/event chips, top bar and mode toggle, collapsed day sheet tiles |
| 2 | `2_gregorian-month.png` | Month view in Gregorian months (November 2026) | Same as 1 |
| 3 | `3_hebrew-interface.png` | Hebrew interface (RTL), month view in Hebrew months | Same as 1, plus Hebrew strings and RTL layout |
| 4 | `4_agenda.png` | Agenda: anniversary, birthday, yahrzeit, candle lighting, phone-calendar events, Shabbat with havdalah | Agenda list, entry cards, date column, top bar |
| 5 | `5_day-sheet-erev-shabbat.png` | Day sheet expanded on Friday 20 November (erev Shabbat): tiles, zmanim card, candle lighting, calendar events | Expanded day sheet, quick tiles, zmanim/candle-lighting/calendar cards |
| 6 | `6_zmanim.png` | Zmanim screen for that Friday, the morning group | Zmanim screen, zman names and order |
| 7 | `7_event-date-picker.png` | Editing the yahrzeit, with the Hebrew/Gregorian date picker open | Event editor, date picker dialog |
| 8 | `8_cities.png` | Sidebar open with the city list expanded (current location, New York, London) | Navigation drawer, city switcher, drawer items |

All shots use the light theme (owner decision, 2026-11-03: no dark-mode shot). Theme, typography, terminology (string) and color changes affect every shot.

**Regenerate:** `scripts/store-screenshots.sh` (all shots) or `scripts/store-screenshots.sh 2,5` (only those). Then `git status fastlane/` shows what changed. The test only runs when asked, so it never takes part in `verifyRoborazziDebug`.

To change the list, edit `StoreScreenshotTest.Shot` and this table together. Keep at most 8 shots (Play's limit per device type).

## Walkthrough

The in-app walkthrough (`ui/walkthrough/WalkthroughRegistry`, see ARCHITECTURE_SPEC "Feature Walkthrough") presents these features:

| ID | Feature | Related shots |
|---|---|---|
| `welcome-hebrew-calendar` | Hebrew and Gregorian dates, holidays and zmanim together | 1, 2 |
| `zmanim-location` | Zmanim for your location or chosen cities (location permission) | 6, 8 |
| `days-shabbat-holidays` | Day details: zmanim, parsha, Omer, events; Israel/Diaspora | 5 |
| `agenda` | Agenda list, search, jump to date | 4 |
| `personal-events` | Hebrew-date birthdays, yahrzeits, anniversaries; contacts (contacts permission) | 4, 7 |
| `reminders` | Reminders for candle lighting, holidays, fasts, events, Omer (notification permission) | none |
| `device-calendars` | Phone calendars alongside Jewish dates (calendar permission) | 4, 5 |
| `widgets` | Widgets, Quick Settings tile, Hebrew-date app icon | none (not rendered by the harness) |
| `appearance` | Light/dark, Material You and holiday colors, Hebrew/English | 3 |

A new major feature gets a walkthrough screen (ARCHITECTURE_SPEC). It may also deserve a store bullet or screenshot; propose that to the owner.

## Keeping it current

**During a task:** if the change visibly alters a screen in the table above, a claim in the store text, or a walkthrough feature, the task agent does two things:
1. adds the Backlog label `store-listing` (`backlog task edit <id> --add-label store-listing`);
2. writes one line in the task notes, e.g. `Store listing: shot 5 (day sheet tiles changed); full description bullet on zmanim tiles.`

It doesn't commit any `fastlane/` change.

**At release** (the owner asks for vX.Y.Z), the coordinator:
1. Lists the tasks merged since the last tag (`git log <last-tag>..HEAD --merges`; merge commits name their task) and picks those labelled `store-listing`. It also skims the commits for changes to anything in the "Depends on" column.
2. Regenerates only the affected shots and drafts any text change and `changelogs/<versionCode>.txt`.
3. Asks the owner to approve the changelog, the before/after images and the text diff, in one message.
4. Commits what was approved and restores the rest, then follows `specs/rfes/release-process.md` to tag.

CI then puts the changelog in the GitHub release notes and Play "What's new". It uploads the listing to Play only if `fastlane/` (other than `changelogs/`) changed since the last upload; the tag `store-listing-published` marks that commit.

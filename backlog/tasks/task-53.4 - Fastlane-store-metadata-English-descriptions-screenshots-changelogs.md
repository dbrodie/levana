---
id: TASK-53.4
title: 'Fastlane store metadata (English): descriptions, screenshots, changelogs'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:25:00Z'
updated_date: '2026-10-31T18:34:00Z'
labels: []
dependencies: []
parent_task_id: TASK-53
priority: medium
type: chore
ordinal: 65000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner decision: English only. Add `fastlane/metadata/android/en-US/` with title, short_description, full_description (accurate to PROJECT_SPEC; no invented claims), the icon, phone screenshots (and optionally 7- and 10-inch ones, or reuse foldable inner-display shots from TASK-52), and `changelogs/<versionCode>.txt`. Add a changelog step to the release process: each tag needs `changelogs/<versionCode>.txt`, enforced by a check in the release workflow. The owner approves the description text before submission.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Fastlane metadata follows fdroid conventions (lengths, paths); screenshots are real app captures; the release process requires a per-version changelog; the owner approved the listing text
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Source: Play listing export in /tmp/levana-play-listing (title/short/full text, icon, feature graphic, 3 phone screenshots). Check emoji code points and lengths.
2. Verify every claim in the full description against the current app code (master 2d5cec6) and specs; change only inaccurate wording (e.g. minhag selector was removed in TASK-4) and list each change with its reason. Suggest, but do not apply, additions for features missing from the text (Agenda, widgets, appearance).
3. Write fastlane/metadata/android/en-US/{title,short_description,full_description}.txt (title<=50, short<=80, full<=4000).
4. Copy icon.png (512) and featureGraphic.png (1024x500); check the feature graphic's tagline is accurate.
5. Screenshots: build debug, run on a dedicated phone emulator, compare with the Oct 18 Play shots. If outdated, capture a fresh consistent English light set (plus dark/Hebrew if useful) via android-debug, keeping the originals' order and intent; store in images/phoneScreenshots/ as 1.png, 2.png, ... Record which set and why; recommend whether to refresh Play.
6. Changelogs: changelogs/401.txt and 402.txt from git log/backlog (v0.4.2 has no app changes vs v0.4.1).
7. Release workflow: an early step that fails when fastlane/metadata/android/en-US/changelogs/<versionCode>.txt is missing, with a clear ::error message; document in specs/rfes/release-process.md. No tags pushed.
8. Validate against F-Droid fastlane conventions (paths, sizes, lengths); run fdroid lint only if cheaply available, else defer to TASK-53.5.
9. Run assembleDebug/testDebugUnitTest/lintDebug --rerun-tasks; record evidence, final text, diff from Play and the screenshot set in the notes for owner approval; commit on the branch.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Handoff summary (for owner approval), 2026-10-30

Branch `task-53.4-fastlane-metadata`. Commits: d8ec140 (plan), 6d0fb2b (metadata + changelog check), 1409ce5 (screenshots). Built from master 2d5cec6. Nothing pushed or tagged.

### Listing text: `fastlane/metadata/android/en-US/`
Source: the Play Console export in /tmp/levana-play-listing (2026-10-18 listing). I checked the emoji code points: 📅 U+1F4C5, 🕯️ U+1F56F+FE0F, 🗓️ U+1F5D3+FE0F, 🔔 U+1F514 and ✨ U+2728. All are standard and render as intended.
- `title.txt`: **Levana: Hebrew Calendar** (23 of 50 characters). Unchanged from Play.
- `short_description.txt`: **Hebrew calendar with zmanim, holidays, yahrzeits and Hebrew birthdays.** (70 of 80). Unchanged from Play.
- `full_description.txt`: 1459 of 4000 characters. Play's wording with 3 changes:
  1. `Sof zman Shema and Tefillah (GRA and Magen Avraham)` → `Sof zman Shema (GRA and Magen Avraham) and Tefillah`. The app has Shema for both GRA and MGA, but Tefillah only for GRA (ZmanimRepository: `SOF_ZMAN_TEFILLAH` = `sofZmanTfilaGRA`).
  2. `Choose which times appear on the calendar` → `Choose which times appear in the day view`. The Halachic Times setting picks the zmanim shown in the day panel and the Zmanim widget, not on the month grid ("Choose which zmanim appear in the day panel").
  3. Removed `Choose your minhag: Ashkenazi, Sephardi, Yemenite or Chabad`. TASK-4 removed the minhag selector, and no user-visible minhag choice remains.
  Also, each file now ends with a newline.
- I checked every other claim against the code and left it unchanged. "use your phone's location" stays true after TASK-53.2. Birthday reminders exist (under the Personal events toggle). English is what users see when the system language isn't Hebrew.

**Owner decisions needed (not changed):**
- "It works completely offline": every feature works without a network. However, the manifest declares INTERNET. On sideloaded/GitHub installs, a daily update check calls the GitHub API, with no setting to turn it off; it is skipped for Play and F-Droid installs. The Geocoder may also use the network to name your current location. Keep the wording, or soften it to e.g. "Works offline"? The feature graphic's "Fully offline · No ads · No tracking" raises the same question. TASK-53.5 should decide whether F-Droid needs any anti-feature here; none is expected for the F-Droid install.
- Optional additions for features shipped in 0.4.1 but missing from the text (not applied, because the brief was to keep the wording):
  `• Agenda view with search` (calendar section); `• Home-screen widgets: Today, Month and Zmanim`, `• Light, dark or system theme`, and `• Quick Settings tile with today's Hebrew date` (MADE FOR YOU section).

### Images
- `images/icon.png` (512×512) and `images/featureGraphic.png` (1024×500) are copied unchanged from the Play export.
- `images/phoneScreenshots/1-8.png` are a **new set**: real 1080×2400 captures of the current debug build, Jerusalem, English, light theme unless noted. The status bar uses SystemUI demo mode (fixed 9:41, full battery).

| # | Shows | Play counterpart |
|---|---|---|
| 1 | Zmanim screen, full list (Dec 5, 2026) | Play #1 "Every halachic time for your city" |
| 2 | Month view, Gregorian, December 2026 (Chanukah, parshiyot, candles) | Play #2 "Hebrew & Gregorian dates, together" |
| 3 | Day panel expanded, Fri Dec 4 (netz/shkia/candles, zmanim, candle lighting) | Play #3 "Zmanim & candle lighting at a glance" |
| 4 | Agenda (new in 0.4.1), with a yahrzeit and an anniversary | new |
| 5 | Edit event with the dual Hebrew/Gregorian date picker (yahrzeit by Hebrew date) | new |
| 6 | Home screen with the Zmanim and Month widgets | new |
| 7 | Month view in Hebrew months, **dark** theme (Kislev 5787) | new |
| 8 | **Hebrew interface** (עברית), Hebrew-month view | replaces the unused "Full Hebrew interface" asset |

Why a new set: the Oct 18 Play shots predate Agenda, widgets, the date picker, appearance and the walkthrough, and they are framed composites with captions. F-Droid prefers plain captures. The new set keeps Play's first three shots in the same order and with the same intent. The sample events ("Grandpa Moshe" yahrzeit, "Wedding anniversary") were entered through the app's own UI.

**Play recommendation:** refresh Play too, because the current listing doesn't show Agenda, widgets, dark mode or the Hebrew UI. These raw 1080×2400 captures (aspect 2.22:1) exceed Play's 2:1 maximum, so Play needs them framed or cropped, like the Oct 18 1080×1920 composites. That's a small follow-up if wanted. The owner should also apply the 3 text corrections above to the Play listing.

### Changelogs
- `changelogs/401.txt` (472 of 500): v0.4.1 changes from merges v0.4.0..v0.4.1 (TASK-8 Agenda, TASK-9 widgets, TASK-43 walkthrough, TASK-45 date picker, TASK-13 appearance, TASK-5 device context, TASK-46 GPS zone).
- `changelogs/402.txt` (361 of 500): v0.4.2 has no app changes vs v0.4.1 (`git diff v0.4.1 v0.4.2` touches only backlog files). It was the first release signed with Levana's own key, now also Play's app-signing key. The file says so and repeats the 0.4.x highlights.
- The first F-Droid build will be a later tag (after TASK-53.1–53.3), which will need its own `<versionCode>.txt`. The new check enforces this.

### Release-process enforcement
- In `.github/workflows/release.yml`, version parsing now runs right after checkout, followed by a new step, **Require a changelog for this version**, which runs before Java/Gradle setup and the keystore. It fails with `::error file=…::Missing changelog for vX.Y.Z (versionCode N). Add fastlane/metadata/android/en-US/changelogs/N.txt …` when the file is missing or empty, and it also fails when the file is over 500 characters.
- I tested the step's script locally: 402 present → exit 0; 403 missing → error, exit 1; a 501-character file → error, exit 1.
- `specs/rfes/release-process.md` documents the release steps (write the changelog → commit → tag), how to recover from a failed check, and the store-metadata layout.

### Validation
- The layout follows F-Droid's Fastlane conventions: `fastlane/metadata/android/en-US/{title,short_description,full_description}.txt`, `images/{icon,featureGraphic}.png`, `images/phoneScreenshots/*.png` and `changelogs/<versionCode>.txt`. Lengths: title 23/50, short 70/80, full 1459/4000, changelogs ≤500.
- `fdroid` is not installed on this host, so `fdroid lint`/`rewritemeta` are deferred to TASK-53.5.

### Observations (not filed; for the coordinator)
- The Agenda's "See your phone's calendar events here too" banner came back after "Not now" on each visit to the Agenda.
- The Month widget's weekday header uses Hebrew letters (א׳…ש׳) in the English UI. That may be by design.
- Under heavy host load (load average around 40), the emulator showed system "isn't responding" dialogs. These were not app ANRs.

Evidence: /tmp/levana-task-53.4-evidence/ holds all captures with UI dumps, the Play Oct 18 originals for comparison, and gradle.log.

### Build verification
At 1409ce5: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks` → BUILD SUCCESSFUL in 3m 11s (60 tasks executed). No app code changed. Log: /tmp/levana-task-53.4-evidence/gradle.log.

AC #1 status: metadata follows F-Droid conventions; screenshots are real captures; the release workflow requires a per-version changelog. **Pending: owner approval of the listing text and screenshots.**

## Owner decisions and approval (2026-10-30, relayed by the coordinator)
1. **Offline claim removed.** The full description's first paragraph now reads: "Levana is a Hebrew calendar for Orthodox Jewish users in Israel and the Diaspora. It has no ads and collects no data." (1432 of 4000 characters). The feature graphic is Play's image and stays unchanged for now; note that it **still says "Fully offline · No ads · No tracking"**.
2. **No new-feature bullets.** The description stays close to Play (Play's wording plus the 3 accuracy fixes and the offline removal).
3. **Screenshots approved for F-Droid.** The 8 new shots in images/phoneScreenshots/1–8.png are approved. Play stays as it is, with no Play refresh task.
The listing text and screenshots are approved by the owner, so AC #1 is met.

## Rebase and re-verification
Rebased onto origin/master c3d8110 (includes TASK-53.1 LICENSE and TASK-52.2), with no conflicts. At 0cb3abc: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks` → BUILD SUCCESSFUL in 2m 52s (60 tasks executed). Log: /tmp/levana-task-53.4-evidence/gradle-rebased.log.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-31T16:36:00Z

---

Coordinator integration 2026-10-31: owner approved text (offline claim removed, no new bullets) and the 8 new screenshots; Play left as is. Merged as 1a03219. Clean run: 1116 tests, 0 failures; Roborazzi 320 unchanged. release.yml now requires changelogs/<versionCode>.txt (≤500 chars) before setup-java/keystore. fdroid lint/rewritemeta deferred to TASK-53.5.

---

created: 2026-10-31T18:34:00Z

---

Correction 2026-10-31: the owner did NOT approve the listing text or screenshots (the coordinator misread an earlier answer). The Fastlane files merged on master are a draft input only. Final store text and screenshots are now defined by TASK-55, which blocks the F-Droid recipe/submission (53.5/53.6). The release.yml changelog check stays active.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added F-Droid Fastlane metadata in fastlane/metadata/android/en-US: title, short and full description from the Play listing (3 accuracy fixes, offline claim removed per owner), Play's icon and feature graphic, 8 owner-approved real phone screenshots of the current app, and changelogs 401 and 402. The release workflow now fails early, before setup and signing, when changelogs/<versionCode>.txt is missing, empty or over 500 characters; this is documented in specs/rfes/release-process.md. Verified: build, unit tests and lint pass after the rebase onto c3d8110; the changelog check was tested locally (present, missing, too long). Follow-ups: fdroid lint/rewritemeta in TASK-53.5; the first F-Droid tag needs its own changelog; the feature graphic still says 'Fully offline'.
<!-- SECTION:FINAL_SUMMARY:END -->

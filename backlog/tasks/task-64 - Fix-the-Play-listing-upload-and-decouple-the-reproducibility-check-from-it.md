---
id: TASK-64
title: Fix the Play listing upload and decouple the reproducibility check from it
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-03T22:55:00Z'
updated_date: '2026-11-04T13:51:00Z'
labels: []
dependencies: []
priority: high
type: bug
ordinal: 106000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found on the v0.4.4 release (2026-11-04, run 41732858048). The APK side passed (signing, certificate check, GitHub Release with the changelog notes, Play internal upload with "What's new"), but two release-workflow defects showed up.

1. **The Play listing upload fails.** The `store-listing` job (TASK-55) runs `fastlane supply` with no track and no version code, so supply defaults to `track: production`. Levana has no production release, and supply aborts with "[!] Could not find release for version code '' to update changelog", even with SUPPLY_SKIP_UPLOAD_CHANGELOGS=true. It is not a permissions problem; the service account authenticated. Fix: give supply a track that has a release (`internal`, where the release job just uploaded) and the release's version code (e.g. SUPPLY_TRACK=internal and SUPPLY_VERSION_CODE from the tag), or an equivalent metadata-only mode that doesn't look up a release. Verify against fastlane's current docs. The `store-listing-published` tag stayed at v0.4.3, so the approved listing is still pending upload.
2. **A listing failure blocks the reproducibility check.** `verify-release.yml` runs on `workflow_run` only when the whole Release run succeeded, so the listing failure skipped it. That left `fdroid-version.txt` unpublished, so F-Droid's update check returned 404. The coordinator ran it manually for v0.4.4 (run 41734042324: ✅, fdroid-version.txt = 404/0.4.4). Fix: verification must depend only on the APK release job, e.g. trigger on `completed` and check that the build-release job succeeded, or move the listing upload out of the Release run's conclusion. A listing failure must still be clearly visible.
3. **A way to upload the listing without cutting a release.** Add a `workflow_dispatch` path for the listing job: release environment, owner-triggered, same change detection and the same tag move on success. Then the approved listing can go to Play now, and failed uploads can be retried without a new version.

Keep the security model: secrets only in the release environment, which only `v*.*.*` tags can deploy to. If dispatch needs the environment, adjust the environment's deployment rule deliberately (e.g. allow master for that job only) and document why; or the owner decides. Validate with actionlint, and if possible run supply with `--validate_only` first. Then do a real listing upload with owner authorization and check it in Play Console.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The store-listing job is removed from release.yml; verify-release runs whenever the APK release succeeds
- [x] #2 A documented local script uploads the en-US listing with fastlane supply: dry-run by default, explicit confirmed upload, credentials from a path outside the repo, fastlane pinned, the production-track release lookup fixed per current docs
- [x] #3 No real listing upload performed in this task; the first upload is discussed with the owner afterwards
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. release.yml: delete the store-listing job; build-release unchanged (changelog -> GitHub notes + Play What's new). verify-release.yml already gates on the Release run's conclusion == success, which now reflects only build-release; update its comment.
2. scripts/upload-store-listing.sh wrapping fastlane supply 2.241.2 (latest; supply/uploader.rb identical on master):
   - Root cause (read from supply source): perform_upload_meta always calls fetch_track_and_release!(track, version_code) when any metadata/images upload is on, even with skip_upload_changelogs; there is no metadata-only mode that skips it. Fix: --track internal + --version_code of a release on that track (default: code of the newest vX.Y.Z tag, overridable).
   - Stage only en-US minus changelogs in a temp dir; skip APK/AAB/changelogs; sync_image_upload.
   - Dry run (validate_only) by default; --upload requires a typed confirmation from the terminal and a committed listing.
   - Credentials: PLAY_SERVICE_ACCOUNT_JSON_PATH (file outside the repo), passed as json_key path; never read or printed.
   - Pin: exact fastlane version invoked via RubyGems version selector (fastlane _2.241.2_); fastlane needs Ruby >= 3.2. No Gemfile (a lockfile can't be produced here: system Ruby is 2.6).
   - Local summary: files, length limits, diff since store-listing-published.
3. store-listing-published tag: keep as the record of the last uploaded listing; the script shows the diff since it and prints the commands to move it after a successful upload. No tag changes in this task.
4. Docs: specs/STORE_LISTING.md, specs/rfes/release-process.md, others mentioning the job.
5. Verify: actionlint, shellcheck, run script without credentials (help, bad args, missing creds).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Owner decision 2026-11-04: CI keeps uploading releases to the internal track only. Promotion to closed and, later, production is done manually in Play Console; no promote job. Note: the store listing (text, screenshots, feature graphic) is app-wide, not per track, so the listing upload doesn't need to follow promotion. Release notes ('What's new') travel with the release when it's promoted in the Console. The SUPPLY_TRACK/VERSION_CODE setting in this task only satisfies fastlane's release lookup.

## Owner decisions (2026-11-04)
1. **Separate the listing upload from the release (2a).** The Release workflow does only the APK work: build, sign, certificate check, GitHub Release with changelog notes, Play internal upload with "What's new". **Remove the `store-listing` job from release.yml entirely.** The reproducibility check (verify-release.yml) then depends only on the APK release succeeding.
2. **No GitHub workflow for the listing at all, for now.** Uploading the store listing to Play is a **manual action the owner runs locally from the repo**: a script, e.g. `scripts/upload-store-listing.sh`, wrapping `fastlane supply`. A better long-term mechanism can be decided later.
3. **Listing upload is manual only.** Releases never touch the store listing.
4. **The first real listing upload** is to be discussed with the owner after everything else is done. **Don't perform any real upload in this task.**

## Implementation guidance
- **The local script:**
  - **Dry-run by default** (`fastlane supply --validate_only` or equivalent) with a clear summary of what would be uploaded. A real upload only with an explicit flag (e.g. `--upload`) and a confirmation prompt.
  - Upload only the main listing for en-US: title, short and full description, icon, feature graphic, phone screenshots. Skip APK, AAB and changelogs.
  - Fix the original failure: supply looked up a release on the default production track. Use the internal track with the latest version code, or whatever metadata-only mode fastlane's **current** docs recommend. Verify against the docs, and with a dry run if credentials are available.
  - **Credentials:** read the service-account JSON from a path the owner provides (e.g. an env var such as `PLAY_SERVICE_ACCOUNT_JSON_PATH`), never from the repo. Never print it. The owner currently has no local copy of the JSON; creating a key is part of TASK-58. Document this prerequisite, and fail with a helpful message if the path isn't set.
  - **Fastlane version:** pin it (Gemfile/Gemfile.lock, or a documented version) so runs are reproducible.
  - **The `store-listing-published` tag** (currently at v0.4.3): either keep it as the record of the last uploaded listing (show the diff since the tag; move and push the tag after a successful real upload), or remove the concept. Choose, justify it, and keep it simple. Don't move or delete the tag in this task.
- **Docs:** update specs/STORE_LISTING.md, specs/rfes/release-process.md and AGENTS.md if they mention the listing job. Releases keep "What's new" via `whatsNewDirectory` and the changelog warning.
- **Checks:** actionlint on workflows; shellcheck on the script if available. Run the dry-run path as far as possible without credentials (argument parsing, the missing-credentials message).

## Implementation (2026-11-04, commit 2618094)
- **release.yml:** store-listing job removed; build-release unchanged (changelog -> GitHub notes + Play What's new). **verify-release.yml:** logic unchanged (it already runs when the Release run's conclusion is success, which is now only build-release's); comment updated.
- **scripts/upload-store-listing.sh** (new): wraps fastlane supply. Stages a temp copy of en-US without changelogs (supply uploads every language folder, so this guarantees en-US only); SKIP_UPLOAD_APK/AAB/CHANGELOGS, SYNC_IMAGE_UPLOAD. Dry run (SUPPLY_VALIDATE_ONLY=true) by default; --upload needs a committed listing and typing 'upload' at the terminal. Key path from PLAY_SERVICE_ACCOUNT_JSON_PATH: must be a readable file outside the repo, passed by path (never read/printed). Local summary first: field lengths vs Play limits (30/80/4000), images, diff since store-listing-published.
- **Root cause / fix, from supply 2.241.2 source** (latest release; supply/lib/supply/uploader.rb is identical on master): perform_upload_meta calls fetch_track_and_release!(track, version_code) whenever any metadata/image upload is enabled, even with skip_upload_changelogs, and errors if no matching release. No metadata-only mode skips this. Track defaults to production (no release there) and version_code to nil. Fix: SUPPLY_TRACK=internal + SUPPLY_VERSION_CODE = newest vX.Y.Z tag's code (404 now), override with --version-code. The dry run exercises this lookup too.
- **Pinning:** FASTLANE_VERSION=2.241.2 in the script, invoked as fastlane _2.241.2_ supply (RubyGems version selector); setup: Ruby >= 3.2 + gem install fastlane -v 2.241.2. No Gemfile/Gemfile.lock: fastlane 2.241.2 needs Ruby >= 3.2 and only system Ruby 2.6 is here, so a lockfile couldn't be resolved. A Gemfile.lock could be added later from a Ruby 3.2+ machine if transitive pinning is wanted. **No fastlane config files (Gemfile/Appfile) added; nothing under fastlane/ changed.**
- **store-listing-published tag: kept** as the record of the last uploaded listing. Why: it's the only in-repo answer to 'is the approved listing on Play yet?' (coordinator checks it at release), and the script needs it only for the diff. Simple: the script never moves it; after a successful --upload it prints the git tag -f / git push --force commands. Tag not touched in this task (still at v0.4.3 = 39e1fb8).
- **Docs:** specs/rfes/release-process.md ('Store listing upload' rewritten, Store Metadata, Files Changed), specs/STORE_LISTING.md (intro + 'At release'). AGENTS.md doesn't mention the job. TASK-58 notes still describe the CI listing job and the secret; not edited here (separate task), the owner now also needs a local copy of the key there.

## Verification (commit 2618094)
- actionlint .github/workflows/*.yml: clean. shellcheck scripts/upload-store-listing.sh: clean.
- Script without credentials: --help (rc 0); unknown arg, --version-code without value, non-numeric code: usage/error, rc 1. No PLAY_SERVICE_ACCOUNT_JSON_PATH: local summary (versionCode 404, 18 files changed since store-listing-published) then the helpful error, rc 1. Nonexistent path, key inside the repo, fastlane not installed: each a clear error.
- With stub gem/fastlane on PATH (no network): dry run passes SUPPLY_TRACK=internal, VERSION_CODE=404, VALIDATE_ONLY=true, skip APK/AAB/changelogs, staged dir has only en-US with txt + images. --upload via expect: answer 'nope' -> 'not confirmed', rc 1; 'upload' -> VALIDATE_ONLY=false, prints the tag commands.
- **Limitations:** no real supply run (no fastlane/Ruby 3.2 here, no local key; real upload forbidden). The release lookup fix is verified against the source, not against Play; the owner's first dry run is the real check. Assumes the internal track's current release has the newest tag's versionCode (true while every tag uploads to internal); otherwise pass --version-code.
<!-- SECTION:NOTES:END -->

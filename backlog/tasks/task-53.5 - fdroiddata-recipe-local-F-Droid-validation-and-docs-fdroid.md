---
id: TASK-53.5
title: fdroiddata recipe, local F-Droid validation and docs/fdroid
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:25:00Z'
updated_date: '2026-11-03T22:23:00Z'
labels: []
dependencies:
  - TASK-53.1
  - TASK-53.2
  - TASK-53.3
parent_task_id: TASK-53
priority: medium
type: task
ordinal: 66000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Write `metadata/ninja.doskey.app.levana.yml` for fdroiddata:
- the source repo and the build recipe for the release tag;
- `Binaries: https://github.com/dbrodie/levana/releases/download/v%v/levana-v%v.apk`;
- `AllowedAPKSigningKeys` set to the verified SHA-256;
- licence MIT, a category, and anti-features if any;
- `AutoUpdateMode: Version` and `UpdateCheckMode: Tags` with the right `UpdateCheckData` or version handling. Resolve how versionCode is derived when versions exist only in git tags; prefer no change to tag-driven releases.

Validate locally with fdroidserver, in Docker or a pinned venv, in a fdroiddata fork checkout: `fdroid readmeta`, `fdroid rewritemeta`, `fdroid lint`, `fdroid checkupdates`, `fdroid build` (buildserver image), and the signature-aware comparison against the GitHub APK.

Write `docs/fdroid/README.md` (release and verification flow, key backup and recovery requirements without credentials) and a compliance report: licences, dependencies, anti-features, test results and remaining blockers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 fdroid lint, rewritemeta and checkupdates pass, and fdroid build reproduces and accepts the GitHub APK locally; docs/fdroid/README.md and the compliance report are committed
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Evidence dir /tmp/levana-task-53.5-evidence (README). Pinned fdroidserver (latest release from PyPI, exact version recorded) in a venv there; shallow clone of gitlab.com/fdroid/fdroiddata there as the local 'fork' checkout (no remote fork, no push).
2. Read fdroidserver checkupdates/build/publish code to settle: UpdateCheckMode Tags + UpdateCheckData/VercodeOperation so versionCode = maj*10000+min*100+patch is derived from the tag alone (no repo change); how Binaries is downloaded and compared (build vs publish).
3. Write metadata/ninja.doskey.app.levana.yml (v0.4.3 build, gradle yes, subdir app, JDK 17 via sudo, Binaries, AllowedAPKSigningKeys ab02841e…5117, MIT, categories from fdroiddata config, no anti-features expected – justify INTERNET/update checker/ACRA mail). Copy it into the repo under docs/fdroid/ as the reference copy.
4. Run fdroid readmeta, rewritemeta (no diff), lint, checkupdates (--allow-dirty, local only) and record output.
5. fdroid build of v0.4.3 in the official buildserver image (amd64) through a separate colima profile with Rosetta (does not touch the default profile and its running containers); fallback: arm64 Debian 13 container with the same fdroidserver. Then fdroid publish with a throwaway repo key to prove the developer-signed APK is accepted (apksigcopier), plus an explicit apksigcopier compare against the GitHub v0.4.3 APK.
6. docs/fdroid/README.md: recipe, update-check handling, release/verification flow, local validation commands; compliance report docs/fdroid/COMPLIANCE.md (licences, deps, anti-features, results, remaining blockers). No fastlane/ or release.yml changes.
7. Build/test command from AGENTS.md; record evidence in notes; commit on branch; file follow-ups (e.g. alternative F-Droid clients and the GitHub update check).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-03 dependencies: owner direction (relayed via the TASK-55 advisor) lifted the block on TASK-55 for the technical work. Dependencies set to TASK-53.1, 53.2 and 53.3 only (TASK-53.4 and TASK-55 dropped). TASK-53.6 (submission) still depends on 53.5 and 55. Validation uses the draft fastlane/ metadata on master as-is; nothing under fastlane/ is changed here.

## Implementation (@claude), branch task-53.5-fdroiddata
Nothing under fastlane/ and nothing in release.yml changed. Nothing pushed, tagged, forked or submitted.

### Recipe (docs/fdroid/ninja.doskey.app.levana.yml = fdroiddata metadata/ninja.doskey.app.levana.yml)
- Calendar & Agenda; MIT; no anti-features (justified in docs/fdroid/COMPLIANCE.md). Binaries = GitHub levana-v%v.apk; AllowedAPKSigningKeys ab02841e…5117.
- Build 0.4.3/403 at commit 39e1fb8a (tag v0.4.3), subdir app, gradle yes. No JDK install: the buildserver's OpenJDK 21 reproduces the Temurin 17 APK.
- **versionCode from tags:** fdroidserver's Tags mode can only take one regex group (one contiguous number) from the tag, so 403 can't be derived from v0.4.3. I chose the common fdroiddata pattern instead: UpdateCheckMode HTTP on releases/latest/download/fdroid-version.txt, with AutoUpdateMode 'Version v%v'. **verify-release.yml** (report job) now uploads fdroid-version.txt (versionCode=/versionName=) only after a successful reproducibility check. No change to the tag-driven release; release.yml untouched.
- **F-Droid strips signingConfigs {} from build.gradle.kts (remove_signing_keys).** v0.4.3's signingConfigs.getByName("release") then fails. For 0.4.3 the recipe has a prebuild sed (getByName → create, an empty config; APK unchanged). Master now uses findByName + null check (app/build.gradle.kts). Recommendation for TASK-53.6: submit with the first release that has the fix and drop the prebuild.

### Validation (evidence: /tmp/levana-task-53.5-evidence/README.md)
fdroiddata master 80bcda3e, fdroidserver master cdfdce83; jobs run as in fdroiddata .gitlab-ci.yml.
- readmeta pass; rewritemeta no changes; lint pass (no warnings); check-jsonschema pass; tools/check-* pass.
- checkupdates --auto:
  - real URL → HTTP 404 (asset not yet published);
  - stubbed 0.4.3/403 → up to date, no diff;
  - stubbed 0.4.4/404 → new build entry with commit v0.4.4.
- **fdroid build --test --refresh-scanner --on-server --no-tarball ninja.doskey.app.levana:403: PASS.**
  - 'compared built binary to supplied reference binary successfully'
  - 'supplied reference binary has allowed signer ab02841e…5117'
  - JDK OpenJDK 21.0.13. Scanner: only gradle-wrapper.jar removed.
  - Independent apksigcopier 1.1.1 compare --unsigned against levana-v0.4.3.apk: exit 0; badging 403/0.4.3.
- Limitation: built in an arm64 Debian 13 stand-in for buildserver-trixie (same OpenJDK 21 package, SDK licences, gradlew-fdroid and bsenv.sh copied from the official image; amd64 libs for aapt2 under qemu). The official amd64 image is too slow under qemu here; colima Rosetta could not be enabled. The MR pipeline will run the real image.
- findByName fix: a master copy with remove_signing_keys applied builds the same APK as the untouched tree (1279098f…).
- AGENTS.md command (assembleDebug testDebugUnitTest lintDebug): BUILD SUCCESSFUL, 1228 tests, 0 failures. actionlint 1.7.12 on both workflows: clean.

### Docs
docs/fdroid/README.md (recipe, update-check handling, signing-config stripping, release flow, local validation); docs/fdroid/COMPLIANCE.md (licences, deps, anti-features, permissions, results, blockers); release-process.md note.

### Remaining / owner
- TASK-55 approval of fastlane text and screenshots.
- After merge: publish fdroid-version.txt for the current release (Run workflow 'Verify reproducible release' for v0.4.3, or the next release).
- TASK-53.6 submission.
- Follow-up TASK-63 (other F-Droid clients run the GitHub update check). The ID may collide with tasks on other branches.
- Cleanup: colima delete --profile fdroid (separate VM created for this; the default profile was untouched).

## 2026-11-04 rebase and revalidation (@claude)
- Rebased onto origin/master d995974 (TASK-55 merged): no conflicts. Kept the findByName fix and the verify-release.yml fdroid-version.txt upload; release.yml is master's (this branch doesn't touch it). Branch commit is now e8c3ef4 plus this update.
- Approved fastlane metadata, read with fdroidserver's own reader (insert_localized_app_metadata, F-Droid char_limits): nothing truncated.
  - Text: title 23/50, summary 70/80, description 671/4000; changelogs 401/402/403 at 471/360/417 (limit 500).
  - Images: icon 512x512, featureGraphic 1024x500, eight phoneScreenshots/<n>_<name>.png at 1080x1920 (F-Droid sorts by file name). Log: /tmp/levana-task-53.5-evidence/fastlane-parse.log.
- Recipe unchanged (byte-identical to docs/fdroid/ninja.doskey.app.levana.yml): readmeta, rewritemeta (no changes), lint, check-jsonschema and tools/check-* pass again (meta-checks-rerun.log).
- fdroid build :403 not rerun: it builds the v0.4.3 tag (39e1fb8), which the rebase doesn't change, and the recipe is unchanged; the 2026-11-03 PASS stands.
- New point for TASK-53.6: F-Droid reads the listing from the built commit's fastlane/. v0.4.3 still has the pre-TASK-55 draft, which is a second reason to submit with the next release (with findByName, and without the prebuild).
- rm -rf app/build/test-results, then assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL, 61 tasks executed, 1236 tests, 0 failures; Roborazzi verified with no screenshot changes. actionlint 1.7.12 on all workflows: clean.
- Docs: TASK-55 approval removed from Remaining in COMPLIANCE.md; store-metadata check section added; README recommendation extended.
- Cleanup: colima profile fdroid deleted (its containers and images went with it); the default profile and Docker context are unchanged; evidence kept in /tmp/levana-task-53.5-evidence.
<!-- SECTION:NOTES:END -->

---
id: TASK-53.3
title: Reproducible release build and an automated rebuild-and-compare check
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:25:00Z'
updated_date: '2026-11-01T14:31:00Z'
labels: []
dependencies:
  - TASK-53.2
parent_task_id: TASK-53
priority: high
type: chore
ordinal: 64000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
- Pin the build: the exact JDK (Temurin 17.0.x) in release.yml and documented for F-Droid; Gradle, AGP, SDK and build-tools versions, the Kotlin compiler and dependencies (consider Gradle dependency verification or locking).
- Remove nondeterminism: check baseline profiles (assets/dexopt/baseline.prof and .profm), R8 output, resources, timestamps, file order and embedded paths.
- Add a **verification job** that runs without any secrets: check out the exact tag, build the unsigned release APK in a clean environment (ideally F-Droid's buildserver image or an equivalent Docker image), download the signed `levana-v<ver>.apk` from the GitHub Release and run `apksigcopier compare --unsigned`. On a mismatch, run diffoscope and upload the report as an artifact. Failures must be visible on the release. Also check the certificate SHA-256 (it must equal the Play app-signing certificate; record it), versionCode and package ID.
- Keep signing secrets only in the tag-triggered release job; never give secrets to PR builds.
- Prove it on a real tag: either a pre-release tag (owner authorization required before pushing any tag) or by re-verifying the existing v0.4.2 APK if it is already reproducible.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An independent unsigned rebuild of a release tag matches the signed GitHub APK under apksigcopier compare; the CI job reports pass/fail and attaches diffoscope output on failure
- [ ] #2 The signing certificate SHA-256 is recorded and equals the Play app-signing certificate; no secrets are reachable from non-release workflows
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Tooling outside the repo: venv (/tmp/levana-task-53-3/venv) with apksigcopier + diffoscope (+ brew actionlint). Evidence dir /tmp/levana-task-53-3 with README.
2. Baseline: download the v0.4.2 GitHub APK; record cert SHA-256, package, versionCode (aapt2/apksigner). Rebuild the v0.4.2 tag unsigned (detached worktree outside the repo, VERSION_NAME=0.4.2, no keystore) and run apksigcopier compare + diffoscope; explain any mismatch (expected: dependency-info block, possibly other content).
3. Fallback signing: when no release keystore is configured, the release build must produce an UNSIGNED APK (F-Droid style), not debug-signed. Keep the debug fallback only behind an explicit opt-in if needed for local installs (check current users).
4. Pin the toolchain: Gradle wrapper distributionSha256Sum; explicit buildToolsVersion; AGP/Kotlin/compileSdk already pinned in the catalog; exact Temurin 17.0.x in release.yml via setup-java; consider gradle/verification-metadata.xml (sha256) for dependencies.
5. Reproducibility of master: build release twice (different dirs, times; local JBR vs Temurin 17) and once in a Debian bookworm container with Temurin 17 + SDK (F-Droid-like). Compare with apksigcopier compare --unsigned / diffoscope; fix nondeterminism (baseline.prof/.profm, VCS info META-INF/version-control-info.textproto, timestamps, paths, R8 map ids).
6. Signature-aware proof: sign one build with a throwaway key (apksigner), then apksigcopier compare signed vs unsigned rebuild.
7. CI: new .github/workflows/verify-release.yml, no secrets, permissions contents: read (+ actions write only if needed for artifacts); triggers workflow_run on Release completed (tag) and workflow_dispatch(tag). Checkout the tag, pinned JDK, assembleRelease unsigned, download levana-v<ver>.apk via gh, check package/versionCode/versionName/cert SHA-256 (expected value recorded in repo), apksigcopier compare; on mismatch diffoscope and upload artifact, fail. Pin release.yml JDK too. Validate with actionlint.
8. Docs: docs/fdroid/README.md (how it works, manual verification, pinned toolchain, key backup/recovery without credentials); update specs/rfes/release-process.md.
9. Verify: assembleDebug assembleRelease testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks; record evidence and owner steps for a real tag proof in the notes; commit. No push/tag/release.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation (@claude), branch task-53.3-reproducible-build
Commits: 1cfed0b (plan), cb937c3 (build), cd90a67 (CI + docs). Nothing merged, pushed or tagged.

### Build (app/build.gradle.kts, gradle-wrapper.properties)
- Without a release keystore, assembleRelease now writes an **unsigned** app-release-unsigned.apk (F-Droid style). The old silent debug-key fallback is opt-in with -Plevana.debugSignRelease=true. Nothing in the repo relied on it.
- vcsInfo.include = false: META-INF/version-control-info.textproto (the commit hash) is no longer packaged. It only matched when built from a git checkout of the same commit.
- packaging.jniLibs.keepDebugSymbols += "**/*.so": the prebuilt libandroidx.graphics.path.so and libdatastore_shared_counter.so are packaged as they are. Before, AGP stripped them only when an NDK was installed, so an NDK on the build machine (F-Droid's image has an ndk dir) would change the APK. The bytes are the same as before (no NDK was ever present in CI).
- buildToolsVersion = "35.0.0" (the AGP 8.13 default, now explicit); Gradle distributionSha256Sum 20f1b117…0aed78 for 8.13.
- Already pinned and unchanged: AGP 8.13.2, Kotlin 2.1.0, KSP 2.1.0-1.0.29, compileSdk 36. Every catalog version is exact. The resolved releaseRuntimeClasspath and buildEnvironment have no dynamic or snapshot versions.
- Baseline profile and R8 output were deterministic in every build; no change needed.
- Not adopted: Gradle dependency verification (verification-metadata.xml). It is supply-chain integrity, not reproducibility, and costs upkeep on every dependency/lint/Robolectric bump; the reasons are recorded in docs/fdroid/README.md.
- The wrapper jar is the official Gradle 9.3.1 wrapper jar (checksum matched services.gradle.org; the jar is version-independent). wrapper-validation now runs in both workflows.

### CI
- **verify-release.yml** (new):
  - Triggers: workflow_run after a successful tag-push Release run, plus workflow_dispatch(tag). Workflow-level permissions: {}.
  - verify job (contents: read, no secrets, persist-credentials false, Gradle cache disabled, --no-build-cache):
    - validates the tag ^vX.Y.Z$ (passed through env, not interpolated into the script);
    - checks out refs/tags/<tag>, sets up Temurin 17.0.21+9 and builds assembleRelease with VERSION_NAME;
    - gh release download levana-<tag>.apk;
    - aapt2 badging on both APKs: package ninja.doskey.app.levana, versionCode/versionName from the tag;
    - apksigner: exactly one signer, cert SHA-256 = ab02841e…5117;
    - apksigcopier==1.1.1 compare --unsigned;
    - on mismatch: diffoscope-minimal HTML+text, artifact reproducibility-<tag> (report + both APKs), and the run fails.
  - report job (contents: write, runs no repo code): adds an idempotent ✅/❌ marker block linking the run to the release notes, and fails again on failure, so the result shows on the release.
- **release.yml**:
  - permissions: {} at workflow level;
  - Temurin pinned to 17.0.21+9 (the version v0.4.2 was built with, from the run log);
  - gradle wrapper-validation;
  - all actions pinned to commit SHAs on their current majors;
  - checkout persist-credentials false;
  - new step: refuse to publish if the APK's cert SHA-256 isn't ab02841e…5117.
- actionlint 1.7.12 (with shellcheck): 0 errors on both workflows.
- Local simulation of the verify shell steps against v0.4.2: pass. Negative cases fail with clear ::error lines: wrong cert, wrong versionCode, injection-style tag. The release-notes marker replacement is idempotent.

### Docs
- docs/fdroid/README.md (for TASK-53.5 to extend): how it works, pinned toolchain, the CI check, manual verification commands, signing identity, and key backup/recovery without credentials. specs/rfes/release-process.md has a Reproducible Build Check section. Its local versionCode fallback was corrected from 0 to 1, matching the code.

## Verification
Evidence: /tmp/levana-task-53-3/ (README.md).
- **v0.4.2 is already reproducible.** The published levana-v0.4.2.apk vs an unsigned rebuild of tag v0.4.2 (macOS arm64, Temurin 17.0.21+9, VERSION_NAME=0.4.2): apksigcopier compare --unsigned exit 0. CI built it on Linux x86_64, so this is also a cross-OS match.
  - The rebuild needed one local edit: the signing fallback set to null, because v0.4.2 debug-signs without a key.
  - The dependency-info block didn't break it: it lives inside the APK Signing Block, which apksigcopier copies whole. diffoscope shows only the signing block and the zip size differ. F-Droid still wants it removed (opaque Google-encrypted blob; done in TASK-53.2).
  - v0.4.2 can't be re-verified by verify-release.yml dispatch, because its build.gradle.kts produces a debug-signed APK without a key. The first CI proof is the next tag.
- **master (cb937c3) is reproducible.** These four builds are byte-identical (SHA-256 5f4e697a…7107861):
  - A: worktree, Temurin 17.0.21;
  - B: fresh clone in another directory, at another time, with TZ=America/Los_Angeles and LC_ALL=de_DE, without the build cache;
  - D: JetBrains Runtime 21.0.9;
  - E: clean Debian 13 arm64 container with Temurin 17.0.21 aarch64, a fresh SDK and an empty Gradle home.
  The final full-verify release build also matched.
- **Signature-aware:** S, the same source signed by AGP with a throwaway key (as in release.yml), passes apksigcopier compare --unsigned against A, B, D and E (exit 0). A 1-byte-flipped copy gives exit 1.
  - apksigner sign on the unsigned APK does not reproduce it: it adds v1 JAR files and realigns. Documented.
- **F-Droid buildserver image:** registry.gitlab.com/fdroid/fdroidserver:buildserver is Debian 13 with **OpenJDK 21 by default**. Under amd64 emulation (colima on Apple silicon) it was impractically slow (stuck in lintVital for over 1.5 h; two parallel runs died, probably OOM), so it was abandoned in favour of E. Cross-JDK (17 vs 21) and cross-OS identity suggest it will match. Real validation in fdroid build is TASK-53.5.
- Cert: v0.4.1 and v0.4.2 GitHub APKs are both signed by CN=Daniel Brodie, SHA-256 ab02841e88825c51c8f0b0f013ff4e312bca47ea1bfbffa894b51890ad5a5117 (v2 only). Per the owner's Play setup (2026-10-30) this is the Play app-signing key. I didn't open Play Console; the owner should confirm the fingerprint there.
- `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks` from a clean app/build/test-results on cd90a67: BUILD SUCCESSFUL. 1144 tests, 0 failures; lint OK; Roborazzi 320 unchanged.
  - Two earlier runs, made while emulated containers loaded the host, hit timing flakes: DatePickerScreenshotTest.outOfRangeStoredDate ('Timed out waiting', 2 runs) and FoldContinuityTest.theDatePickerKeepsItsYearListAndSelection[FOLD8_ULTRA]. Both classes passed on their own and in the final run. These are the known DatePicker load flakes; nothing here touches app code or tests.

## For TASK-53.5 (recipe)
- Build: gradle: [yes] with no prebuild patches. Output: app/build/outputs/apk/release/app-release-unsigned.apk.
- Version: VERSION_NAME, or git describe --tags --abbrev=0 in F-Droid's full clone. Make sure the tagged commit carries only one release tag.
- Binaries: https://github.com/dbrodie/levana/releases/download/v%v/levana-v%v.apk; AllowedAPKSigningKeys: ab02841e88825c51c8f0b0f013ff4e312bca47ea1bfbffa894b51890ad5a5117.
- JDK: the image default is 21, which matched here; installing 17 is the conservative choice.

## Owner steps for the end-to-end proof (not done: needs authorization)
1. Coordinator merges this branch to master and pushes. workflow_run only fires for workflow files on the default branch.
2. Recommended: in GitHub Settings → Environments, create **release** with deployment tags limited to v*.*.*. Move KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD and PLAY_SERVICE_ACCOUNT_JSON into it and delete the repo-level copies. Then add `environment: release` to the build-release job in release.yml. This makes the secrets unreachable from any other workflow or ref, not just unreferenced.
3. Next release as usual: write changelogs/403.txt, commit, push, then tag v0.4.3 and push. This is a real release, including the Play internal upload; no dry-run tag exists, because 'v*.*.*' would match an -rc tag but the versionCode parse would break.
4. Check:
   - the Release run passes the new 'Check signing certificate' step;
   - 'Verify reproducible release' starts automatically and passes;
   - the v0.4.3 release notes end with the ✅ line.
   On ❌, download the reproducibility-v0.4.3 artifact (diffoscope report).
5. Optionally re-run it later from Actions → Verify reproducible release → Run workflow → v0.4.3.

## AC status
- #1: local proof done (v0.4.2 tag and master). The CI job is written, linted and simulated, but not yet run on GitHub; that needs the merge plus a tag (owner).
- #2: cert recorded and enforced. Play equality relies on the owner's 2026-10-30 Play setup; confirm in Console. No workflow other than release.yml references secrets. Hard isolation needs owner step 2.
Left unchecked for the coordinator.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-31T21:41:00Z

---

Coordinator integration 2026-10-31: reviewed release.yml (pinned JDK 17.0.21+9, SHA-pinned actions, permissions {}, cert check ab02841e…5117 matching the Play signing note) and verify-release.yml (no secrets; read-only build job; separate notes job). Merged as 13d0e00. Clean run: assembleRelease (now unsigned without keystore) + 1144 tests, 0 failures; Roborazzi 320 unchanged. Stays In Progress: ACs need the first real tagged release to pass verify-release.yml and an owner check of the cert in Play Console (owner steps in notes).

---

created: 2026-11-01T14:31:00Z

---

2026-11-01 coordinator: (1) Created GitHub environment 'release' (deployment policy: tags v*.*.* only) and set environment: release on the release job (962df02). Secrets are still repo-level: writing secrets was blocked by the session's permission policy, so the owner must add KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD and PLAY_SERVICE_ACCOUNT_JSON as release-environment secrets and then delete the repo-level ones. (2) Verified in Play Console (App signing page, via the Paseo browser): the app signing key SHA-256 is AB:02:84:1E:88:82:5C:51:C8:F0:B0:F0:13:FF:4E:31:2B:CA:47:EA:1B:FB:FF:A8:94:B5:18:90:AD:5A:51:17, which equals EXPECTED_CERT_SHA256 in release.yml and verify-release.yml. Remaining for AC: the first real tagged release passing verify-release.yml.

---
<!-- COMMENTS:END -->

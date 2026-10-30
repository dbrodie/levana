---
id: TASK-53
title: Publish Levana on F-Droid with reproducible developer-signed builds
status: To Do
assignee: []
created_date: '2026-10-30T12:24:00Z'
updated_date: '2026-10-30T12:24:00Z'
labels: []
dependencies: []
priority: medium
type: feature
ordinal: 61000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner request 2026-10-30: publish Levana in the **official F-Droid repository** using F-Droid's **reproducible builds with developer signatures**. F-Droid builds the app from the tagged public source, verifies that its build matches our signed GitHub Release APK (`Binaries` + `AllowedAPKSigningKeys`, compared with `apksigcopier compare --unsigned`), and then distributes **our** signed APK. There is one APK and one signing identity for GitHub, Play and F-Droid, and no second release pipeline.

The owner supplied a detailed write-up from another agent; it is kept as the reference spec in the notes. Coordinator verification against the repo (master after v0.4.2):

**Blockers (verified)**
1. **No licence.** The repo is public (dbrodie/levana), but there is no LICENSE file and GitHub reports no licence. Owner decision: **MIT**. KosherJava is LGPL-2.1, which is compatible; it goes in the third-party notices.
2. **Proprietary dependency.** `com.google.android.gms:play-services-location` (FusedLocationProviderClient in LocationService). Owner direction: replace it with the platform `LocationManager.getCurrentLocation()` **in the single build used for every channel**, not a separate flavor.
3. **The AGP dependency-info signing block** is included by default. Disable it: `dependenciesInfo { includeInApk = false; includeInBundle = false }`.

**Verified facts**
- applicationId `ninja.doskey.app.levana`; minSdk 34, targetSdk 36; AGP 8.13.2, Gradle 8.13, Kotlin 2.1.0; R8 on for release.
- The release workflow (`.github/workflows/release.yml`) runs on `v*.*.*` tags. It sets up Java 17 (Temurin, **not pinned to an exact version**), decodes the keystore from secrets, builds `assembleRelease` + `bundleRelease`, publishes `levana-v<ver>.apk` to GitHub Releases, and uploads the AAB to the Play internal track.
- Versions come **only from git tags**: versionName is the tag without the `v`; versionCode = maj*10000 + min*100 + patch, via `VERSION_NAME` or `git describe` in `app/build.gradle.kts`. Nothing is in a committed file.
- **Signing:** per the parallel Play session (2026-10-30), `levana-release.keystore` is now the **Play app signing key** (uploaded with PEPK), and v0.4.2 was released with it. Verify the certificate SHA-256 against Play Console's app-signing certificate.
- **Update checker:** it calls the GitHub releases API, but `UpdateCheckWorker` already skips it when the installer is F-Droid (`org.fdroid.fdroid`). Document this in the submission; no anti-feature is expected.
- There is no Fastlane metadata yet. Owner decision: **English only**.

**Open technical items (resolve in the subtasks)**
- How F-Droid's update check reads the version, given that versions exist only in tags (e.g. `UpdateCheckMode: Tags` with a suitable `UpdateCheckData`, or a committed version file). Choose the least intrusive option and keep tag-driven releases.
- Reproducibility risks: an exact JDK pin; baseline profile (`baseline.prof`) determinism; R8 determinism; timestamps and paths in the APK.

**Subtasks:** licence → GMS removal and dependency-info → reproducible build and verification CI → Fastlane metadata → fdroiddata recipe and local F-Droid validation → submission (with owner authorization).

**Security** (from the write-up): never commit the key; signing secrets only in the tag-triggered release workflow; the rebuild/verification job gets no secrets; document key backup and recovery without credentials.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 F-Droid independently builds the tagged source and its signature-aware comparison matches the developer-signed GitHub Release APK
- [ ] #2 One APK and one signing identity across GitHub, Play and F-Droid; the certificate matches the Play app-signing certificate
- [ ] #3 The fdroiddata recipe passes fdroid lint, rewritemeta, checkupdates and build locally; the merge request is prepared and submitted only with owner authorization
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Reference spec: owner-supplied write-up (2026-10-30), verbatim summary
Goal: the official F-Droid repo distributes the exact developer-signed APKs published on GitHub Releases, through F-Droid's reproducible-build verification. Add no second signing identity or upstream APK pipeline unless technically necessary.
1. Inspect the release workflow: build commands, signing, output APK, applicationId, versioning, triggers. Confirm the APK uses the app signing cert, not just the Play upload cert. Audit proprietary dependencies, build tools, licences and network anti-features. Prefer keeping the release APK unchanged; add an F-Droid flavor only if necessary.
2. Make the release build reproducible: pin JDK, Gradle, AGP, SDK, NDK and dependencies; remove timestamps, paths and other unstable inputs; buildable from public source without secrets; keep package ID and versioning; build from the exact tag; keep signing separate from compilation; don't touch the key.
3. GitHub Releases stay the binary source: an immutable tag, a developer-signed APK, a stable filename and a version-specific URL, with accurate version info. Keep the Play workflow. Never regenerate a different APK just for F-Droid.
4. Verification: an independent clean build without the key; `apksigcopier compare --unsigned` of the signed release APK against the unsigned rebuild; visible failures; diffoscope for diagnosis. Check the cert fingerprint, versionCode, package ID and Play signing compatibility.
5. F-Droid metadata: `metadata/<applicationId>.yml` with source repo, build recipe, `Binaries` (GitHub release APK URL), `AllowedAPKSigningKeys` (SHA-256), `AutoUpdateMode: Version` + `UpdateCheckMode: Tags` if compatible, exact versions and commits, licence, category and anti-features. Upstream Fastlane metadata, screenshots, descriptions and changelogs. Don't invent metadata.
6. Validate with a local fdroiddata checkout or F-Droid GitLab CI: readmeta, rewritemeta, lint, checkupdates, build, and the APK comparison. Gradle passing is not enough.
7. Security: never commit the key; secrets only in trusted release workflows; no untrusted code with key access; keep signing unchanged; document key backup and recovery without credentials.
8. Prepare the GitLab MR against https://gitlab.com/fdroid/fdroiddata, with metadata, compliance evidence, reproducibility results and references. **Do not submit or publish without authorization.**
Deliverables: workflow changes; reproducible build config; verification commands and tests; the fdroiddata YAML; store metadata; `docs/fdroid/README.md`; a compliance report with remaining blockers; MR submission instructions.
<!-- SECTION:NOTES:END -->

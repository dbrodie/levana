# Release Process

## Summary

Git-tag-driven APK release pipeline. Pushing a semver tag (e.g. `v0.1.1`) triggers a GitHub Actions workflow that derives the version from the tag, builds a signed release APK, and publishes it to GitHub Releases.

## Motivation

The app had no release pipeline. Builds were signed with the debug keystore and there was no versioning workflow or CI/CD. This change establishes a production-ready, low-friction release process with a single command to ship.

## Version Strategy

- `versionName` = tag with `v` prefix stripped (e.g. `v0.1.1` → `"0.1.1"`)
- `versionCode` = `major * 10000 + minor * 100 + patch` (e.g. `v1.2.3` → `10203`)
- Local/dev builds without a tag fall back to `versionName = "0.0.0-dev"`, `versionCode = 1`

The versionCode scheme is strictly increasing for monotonically increasing semver tags and supports up to `v2147.99.99`.

## Signing

Release signing credentials are stored as GitHub Secrets and injected as environment variables at build time. A gitignored `keystore.properties` file provides the same credentials for local release builds.

**Required GitHub Secrets:**

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.keystore` file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

**One-time setup:**
```bash
keytool -genkeypair -v -keystore levana-release.keystore -alias levana \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -i levana-release.keystore | pbcopy
```
Store the `.keystore` file securely offline (not in the repo).

## Release Workflow

1. Write the changelog for the new version in
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   (e.g. `v0.4.3` → `403.txt`). Plain text, at most 500 characters, user-facing
   changes only, based on what was actually merged since the previous tag. F-Droid
   shows this file for the version.
2. Commit it to `master` and push.
3. Tag that commit and push the tag:

```bash
git tag v0.1.1
git push origin v0.1.1
```

GitHub Actions builds and publishes automatically. The workflow's first step after
parsing the tag checks the changelog: if `changelogs/<versionCode>.txt` is missing,
empty or longer than 500 characters, the run fails before building, with an error
naming the expected file. To recover, add the changelog, commit, delete the tag
(`git tag -d vX.Y.Z && git push origin :refs/tags/vX.Y.Z`) and tag the new commit.

## Reproducible Build Check

Every release APK must be reproducible from the tag without the signing key, because F-Droid
publishes our signed APK only after its own unsigned rebuild matches it (TASK-53). After each
successful Release run, `.github/workflows/verify-release.yml` rebuilds the tag with no secrets and
compares it with the published `levana-v<ver>.apk` (`apksigcopier compare --unsigned`). It also
checks the package, versionCode, versionName and the signing certificate SHA-256. On a mismatch,
it uploads a diffoscope report and the release notes get a ❌ line; on success they get a ✅ line.
To re-check any tag, run it from the Actions tab (**Run workflow**, enter the tag).

The release workflow pins the JDK (Temurin 17.0.21+9), and both workflows must stay on the same
one. Without a keystore, `assembleRelease` produces an unsigned APK; use
`-Plevana.debugSignRelease=true` to sign a local release build with the debug key. Details,
manual verification and key backup: `docs/fdroid/README.md`.

## Store Metadata

`fastlane/metadata/android/en-US/` holds the store listing in Fastlane layout, which
F-Droid reads from the tagged source: `title.txt` (≤ 50 chars), `short_description.txt`
(≤ 80), `full_description.txt` (≤ 4000), `images/icon.png`, `images/featureGraphic.png`,
`images/phoneScreenshots/` and `changelogs/`. English only. Keep the text in step
with the Play Console listing.

## Files Changed

- `app/build.gradle.kts` — version from env vars; release signing config
- `.gitignore` — ignore `keystore.properties` and `*.keystore`
- `.github/workflows/release.yml` — CI workflow (including the changelog and certificate checks)
- `.github/workflows/verify-release.yml` — secret-free reproducible-build check
- `docs/fdroid/README.md` — reproducible build, manual verification, key backup
- `fastlane/metadata/android/en-US/` — store metadata and per-version changelogs

## Future Extensions

- **Play Store (AAB)**: Add `bundleRelease` step; same env vars apply
- **F-Droid**: tag-based versioning is compatible; per-version changelogs are enforced (TASK-53)

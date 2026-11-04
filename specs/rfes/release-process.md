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

When the owner asks for a release, the coordinator:

1. **Checks the store listing** (`specs/STORE_LISTING.md`, "Keeping it current"). It lists
   the tasks merged since the previous tag that carry the `store-listing` label, skims the
   commits, and regenerates only the affected screenshots (`scripts/store-screenshots.sh 2,5`).
   It drafts any store-text change.
2. **Drafts the changelog** in
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   (e.g. `v0.4.3` → `403.txt`): plain text, at most 500 characters, user-facing changes
   only, based on what was actually merged since the previous tag. F-Droid shows it for the
   version, it becomes the GitHub release notes, and Play shows it as "What's new"
   (no promotional wording, per Play's rules).
3. **Asks the owner to approve** the changelog, the before/after screenshots and any text
   change, in one message. Nothing under `fastlane/` is committed without that approval.
4. Commits what was approved to `master` (`git restore` the rest) and pushes.
5. Tags that commit and pushes the tag:

```bash
git tag v0.1.1
git push origin v0.1.1
```

GitHub Actions builds and publishes automatically. The workflow checks the changelog
after parsing the tag. If `changelogs/<versionCode>.txt` is missing, empty or longer than
500 characters, it **warns** and carries on: the GitHub release then has only the
generated notes, and Play gets no "What's new". An over-long file is still used on GitHub,
but not sent to Play.

### Store listing upload

Releases never touch the Play listing; the Release workflow uploads only the AAB and its
"What's new". The owner uploads the listing (text and images from
`fastlane/metadata/android/en-US/`, not the changelogs) by hand from a checkout of `master`:

```bash
PLAY_SERVICE_ACCOUNT_JSON_PATH=~/secrets/levana-play.json scripts/upload-store-listing.sh           # dry run
PLAY_SERVICE_ACCOUNT_JSON_PATH=~/secrets/levana-play.json scripts/upload-store-listing.sh --upload  # publish
```

The script wraps fastlane `supply` (pinned version, Ruby >= 3.2, `gem install fastlane -v <version>`;
see the script). The default is a dry run: Google Play validates the edit and nothing is
published. `--upload` asks for a typed confirmation and refuses uncommitted listing changes.
The service-account JSON key stays outside the repo; the account needs permission to edit the
store listing (TASK-58).

supply looks up a release on a track even when it uploads only the listing, so the script
passes the `internal` track and the newest release tag's versionCode (`--version-code`
overrides it). Without them supply defaults to `production`, which has no release, and fails
with "Could not find release for version code ''" (v0.4.4).

The tag `store-listing-published` marks the commit of the last uploaded listing. The script
shows what changed since it, and after a successful upload prints the commands to move it.
A pending listing change is therefore `git diff store-listing-published -- fastlane/metadata/android/en-US`.

## Reproducible Build Check

Every release APK must be reproducible from the tag without the signing key, because F-Droid
publishes our signed APK only after its own unsigned rebuild matches it (TASK-53). After each
successful Release run, `.github/workflows/verify-release.yml` rebuilds the tag with no secrets and
compares it with the published `levana-v<ver>.apk` (`apksigcopier compare --unsigned`). It also
checks the package, versionCode, versionName and the signing certificate SHA-256. On a mismatch,
it uploads a diffoscope report and the release notes get a ❌ line; on success they get a ✅ line.
To re-check any tag, run it from the Actions tab (**Run workflow**, enter the tag). On success it
also uploads `fdroid-version.txt` (`versionCode=…`, `versionName=…`) to the release. That's how
F-Droid's update check learns about the new version (`docs/fdroid/README.md`).

The release workflow pins the JDK (Temurin 17.0.21+9), and both workflows must stay on the same
one. Without a keystore, `assembleRelease` produces an unsigned APK; use
`-Plevana.debugSignRelease=true` to sign a local release build with the debug key. Details,
manual verification and key backup: `docs/fdroid/README.md`.

## Store Metadata

`fastlane/metadata/android/en-US/` holds the store listing in Fastlane layout, which
F-Droid reads from the tagged source: `title.txt` (≤ 50 chars), `short_description.txt`
(≤ 80), `full_description.txt` (≤ 4000), `images/icon.png`, `images/featureGraphic.png`,
`images/phoneScreenshots/` and `changelogs/`. English only. It is the single source
for both stores: Play's listing is uploaded from it by hand (see above). What the screenshots show,
how to regenerate them and the approval rules are in `specs/STORE_LISTING.md`.

## Files Changed

- `app/build.gradle.kts` — version from env vars; release signing config
- `.gitignore` — ignore `keystore.properties` and `*.keystore`
- `.github/workflows/release.yml` — CI workflow (changelog and certificate checks, Play "What's new")
- `scripts/upload-store-listing.sh` — manual Play listing upload
- `.github/workflows/verify-release.yml` — secret-free reproducible-build check
- `docs/fdroid/README.md` — reproducible build, manual verification, key backup
- `fastlane/metadata/android/en-US/` — store metadata and per-version changelogs

## Future Extensions

- **Play Store (AAB)**: Add `bundleRelease` step; same env vars apply
- **F-Droid**: tag-based versioning is compatible; per-version changelogs are enforced (TASK-53)

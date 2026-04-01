# Release Process

## Summary

Git-tag-driven APK release pipeline. Pushing a semver tag (e.g. `v0.1.1`) triggers a GitHub Actions workflow that derives the version from the tag, builds a signed release APK, and publishes it to GitHub Releases.

## Motivation

The app had no release pipeline. Builds were signed with the debug keystore and there was no versioning workflow or CI/CD. This change establishes a production-ready, low-friction release process with a single command to ship.

## Version Strategy

- `versionName` = tag with `v` prefix stripped (e.g. `v0.1.1` → `"0.1.1"`)
- `versionCode` = `major * 10000 + minor * 100 + patch` (e.g. `v1.2.3` → `10203`)
- Local/dev builds without a tag fall back to `versionName = "0.0.0-dev"`, `versionCode = 0`

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

```bash
git tag v0.1.1
git push origin v0.1.1
```

GitHub Actions builds and publishes automatically. No file edits needed.

## Files Changed

- `app/build.gradle.kts` — version from env vars; release signing config
- `.gitignore` — ignore `keystore.properties` and `*.keystore`
- `.github/workflows/release.yml` — CI workflow

## Future Extensions

- **Play Store (AAB)**: Add `bundleRelease` step; same env vars apply
- **F-Droid**: No changes needed; tag-based versioning is compatible

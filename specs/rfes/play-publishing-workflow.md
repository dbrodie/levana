# Play publishing workflow

## Goal

Publish tagged Levana releases to the Google Play internal testing track through
GitHub Actions.

## Scope

- Keep the existing tag-triggered GitHub Release APK artifact.
- Build the signed Android App Bundle (`.aab`) from the same release build.
- Upload that bundle to the Play internal testing track using the encrypted
  `PLAY_SERVICE_ACCOUNT_JSON` GitHub Actions secret.
- Do not automate production rollout.

## Security

The workflow reads its upload keystore and Google service-account credential
only from Actions secrets. Neither credential is committed to the repository.

## Verification

Validate the workflow YAML and run the release bundle Gradle task locally where
the Android SDK and signing credentials are available.

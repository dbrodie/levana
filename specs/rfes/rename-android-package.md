# Rename Android application ID to `ninja.doskey.app.levana`

## Summary

Rename Levana's Kotlin namespace and Android application ID from
`com.levana.app` to `ninja.doskey.app.levana`. This is intentionally a new-app
installation: existing data under the prior ID is not migrated.

## Changes

- Update the Gradle `namespace` and `applicationId`.
- Move main and test Kotlin sources to the matching package hierarchy and
  update package declarations/imports.
- Update ProGuard rules and notification alarm action identifiers.
- Keep the existing contact custom MIME type for compatibility with contacts
  previously written by the app.
- Update developer-facing documentation that records the package name.

## Validation

- Run unit tests and assemble the debug APK.
- Confirm the merged debug manifest has `ninja.doskey.app.levana` as its
  package.
- Install the debug APK on an available emulator and verify application launch,
  declared receivers, launcher aliases, and the Quick Settings tile component.

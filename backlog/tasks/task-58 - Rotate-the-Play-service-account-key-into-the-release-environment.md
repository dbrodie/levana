---
id: TASK-58
title: Rotate the Play service-account key into the release environment
status: To Do
assignee: []
created_date: '2026-11-01T14:36:00Z'
updated_date: '2026-11-04T13:52:00Z'
labels: []
dependencies: []
priority: low
type: chore
ordinal: 94000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up to TASK-53.3 (2026-11-01). The release job now runs in the GitHub environment `release`, which only `v*.*.*` tags can deploy to. The owner is moving the four signing secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) there. `PLAY_SERVICE_ACCOUNT_JSON` stays a repo-level secret for now: the owner doesn't have the original JSON file, and GitHub never reveals secret values.

Steps (owner, in consoles; credentials never go in the repo):
1. Play Console → Setup → API access (or Users and permissions): identify the linked Google Cloud service account used for uploads.
2. Google Cloud Console → IAM & Admin → Service accounts → that account → Keys → Add key → Create new key → JSON.
3. GitHub → Settings → Environments → release: add the downloaded file as the environment secret `PLAY_SERVICE_ACCOUNT_JSON`; then delete the repo-level secret of the same name.
4. In Cloud Console, delete the old key so the previous credential stops working. Delete the downloaded JSON from the local disk, or store it only in the password manager.
5. Verify with the next tagged release: the "Upload App Bundle to Google Play internal testing" step succeeds.

Also confirm the service account has only the Play permissions the upload needs (release to testing tracks), nothing broader.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new service-account JSON key is stored only as the release-environment secret PLAY_SERVICE_ACCOUNT_JSON; the repo-level secret and the old Cloud key are deleted
- [ ] #2 The next tagged release uploads to Play successfully; the service account's permissions are limited to what the upload needs
- [ ] #3 The service account can also update the store listing (TASK-55's listing upload), and nothing broader
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Owner direction 2026-11-03 (TASK-55)
- TASK-55 adds Play listing uploads from release.yml: fastlane `supply` uploads text and images, and only when `fastlane/` changed since the last upload. Play "What's new" goes through the existing r0adkll step (`whatsNewDirectory`).
- **For now, both use the existing repo-level `PLAY_SERVICE_ACCOUNT_JSON`.** The rotation is not a prerequisite.
- When rotating: in addition to releasing to testing tracks, the service account needs permission to **edit the store listing** (Play Console → Users and permissions → the service account → app permissions, the store-presence/listing permission). Otherwise the listing job fails while the AAB upload still succeeds. If the current account lacks it, the first listing upload after TASK-55 will show it; grant it then.
- When the secret moves to the `release` environment, the listing job must run in that environment too (it runs in release.yml on tags, so `environment: release` works).

2026-11-04: TASK-64 removed the CI store-listing job. The Play listing is now uploaded by hand with scripts/upload-store-listing.sh, which reads a local key via PLAY_SERVICE_ACCOUNT_JSON_PATH. The new service-account key is needed both locally (for that script) and as the PLAY_SERVICE_ACCOUNT_JSON secret (for the release AAB upload).
<!-- SECTION:NOTES:END -->

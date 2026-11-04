---
id: TASK-64
title: Fix the Play listing upload and decouple the reproducibility check from it
status: To Do
assignee: []
created_date: '2026-11-03T22:55:00Z'
updated_date: '2026-11-04T10:16:00Z'
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
- [ ] #1 The listing job uploads the approved listing to Play successfully (verified in Play Console) and moves store-listing-published
- [ ] #2 verify-release runs whenever the APK release job succeeded, even if the listing upload fails; a listing failure is visible
- [ ] #3 The listing can be uploaded or retried on demand without a new release, within the release environment's protections (documented)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Owner decision 2026-11-04: CI keeps uploading releases to the internal track only. Promotion to closed and, later, production is done manually in Play Console; no promote job. Note: the store listing (text, screenshots, feature graphic) is app-wide, not per track, so the listing upload doesn't need to follow promotion. Release notes ('What's new') travel with the release when it's promoted in the Console. The SUPPLY_TRACK/VERSION_CODE setting in this task only satisfies fastlane's release lookup.
<!-- SECTION:NOTES:END -->

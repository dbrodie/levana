---
id: TASK-43
title: Show release notes or a what's-new walkthrough after an app update
status: To Do
assignee: []
created_date: '2026-10-29T20:55:00Z'
labels: []
dependencies: []
priority: low
type: feature
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner request 2026-10-29. When the user opens the app for the first time after updating to a new version, show a short popup with that version's user-facing changes, e.g. a dialog or a few-step walkthrough for bigger features. It must work fully offline: the notes ship with the app as localized resources (English and Hebrew), not fetched from the network.

Design questions to settle in the plan:
- Detection: store the last-seen versionCode in DataStore and compare it on launch. Fresh installs see no popup (record the current version silently).
- Skipped versions: when a user jumps several versions, show the combined notes or only the latest.
- Content source: one resource per release, kept in sync with the release process (specs/rfes/release-process.md, play-publishing-workflow.md). The release workflow (.github/workflows/release.yml) and Play release notes could share the same text, so it's written once.
- Format: a simple dialog for small releases, an optional multi-page walkthrough for headline features; no popup when a release has no user-facing notes.
- A way to see the notes again later, e.g. a 'What's new' entry in About.
- Debug builds and the developer settings need a way to trigger it for testing.

Related: the existing in-app update checker (update/UpdateChecker.kt) and the About screen.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After an update, the first launch shows that version's notes once; fresh installs and same-version launches show nothing
- [ ] #2 Notes are bundled and localized (English/Hebrew, RTL); the behavior for skipped versions and releases without notes is defined and tested
- [ ] #3 Release notes have a single source, documented in the release process; the notes can be reopened from About; developer settings can trigger the popup
- [ ] #4 Unit tests for version detection; emulator verification of an upgrade from an older build
<!-- AC:END -->

---
id: TASK-55
title: >-
  Single source for standout features, store text and auto-generated screenshots
  (Play, F-Droid, GitHub, walkthrough)
status: To Do
assignee: []
created_date: '2026-10-31T18:34:00Z'
labels: []
dependencies: []
priority: high
type: feature
ordinal: 68000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner request 2026-10-31: formalize how Levana presents its features **everywhere** (Play Store listing, F-Droid listing, GitHub README/releases, and the in-app walkthrough) from **one source of truth**, with a way to **regenerate screenshots automatically** when the screens they show change. **This blocks the F-Droid publishing work (TASK-53):** no store text or screenshots go out until this is done and the owner approves the result.

**Owner status (2026-10-31):** the owner has **not** approved the listing text or the screenshots. TASK-53.4's Fastlane files on master (`fastlane/metadata/android/en-US/`: text based on the Play listing, 8 hand-captured screenshots, changelogs 401 and 402) are a **draft input** for this task, not approved content. The Play listing, still in Play Console (not published), is another input.

**Current state to build on:**
- **Walkthrough:** `ui/walkthrough/WalkthroughRegistry` holds the in-app feature screens (stable IDs, title/body strings, live mini-UI illustrations, optional permission step); see ARCHITECTURE_SPEC "Feature Walkthrough". There are 9 screens today, including `agenda`.
- **Screenshot tooling:** a Roborazzi harness with a fixed date, seeded data, EN/HE × light/dark and foldable sizes (TASK-12.1, TASK-52.1/52.2), plus emulator capture via the android-debug skill.
- **Store assets:**
  - **Play:** text and 3 framed screenshots (Oct 18) in Play Console; originals in `~/projects/levana/.claude/store-assets/`. Play limits screenshots to a 2:1 aspect ratio, so 1080×2400 raw captures need framing or cropping.
  - **F-Droid:** the draft Fastlane metadata above. F-Droid prefers plain captures; changelogs ≤ 500 chars, enforced in release.yml.
  - **GitHub:** README.md (TASK-53.1) has no screenshots yet.

**Scope:**
1. **Standout-feature catalog.** A single committed definition, a data file or Kotlin registry, of Levana's standout features. For each one: a stable ID, a name, short marketing copy (one line) and long copy (a few lines), and which surfaces it appears on with its order on each: walkthrough screen, Play screenshot slot, F-Droid screenshot slot, GitHub README, and the store description bullets. Link it to the walkthrough registry so a feature's walkthrough screen and its store presence come from the same entry; don't keep two lists that drift apart. The **owner decides** which features go in the store screenshots and which in the walkthrough; prepare a proposal for approval.
2. **Store text.** Generate or derive the Play full and short description, F-Droid's title, short and full description, and the GitHub README feature section from the catalog. Respect each store's limits (Play: 30/80/4000; F-Droid: 50/80/4000). Keep it accurate to PROJECT_SPEC; no invented claims. The owner approves the final wording.
3. **Automated screenshots.**
   - A reproducible generator for the store screenshots of the chosen features: deterministic data and date, English (Hebrew optional per owner), the light theme plus any agreed variants, and the right device sizes (phone; tablet or foldable optional).
   - Output plain captures for F-Droid and GitHub, and framed or captioned 2:1-compliant images for Play.
   - Prefer reusing the Roborazzi/Compose harness, so screenshots regenerate in CI or locally with one command. Use real-emulator capture only where Compose can't render it, e.g. home-screen widgets or the Quick Settings tile.
   - **Change detection:** when a screen used in a store screenshot changes (e.g. its Roborazzi baseline changes), the build or CI flags that the store images are stale and need regenerating and re-approval.
4. **Publishing flow (documented, optionally automated).**
   - Fastlane files are the source for F-Droid; the README embeds GitHub images.
   - For Play, document the manual upload, or propose automating it (e.g. `fastlane supply` or the Play Developer API from the release workflow using the existing service-account secret). Automation needs owner sign-off.
   - Releases must not publish unapproved store content.
5. **Changelogs:** decide whether per-version changelogs (F-Droid `changelogs/<versionCode>.txt`, GitHub release notes, Play "what's new") also come from one source.

**Process:** propose the catalog, surface mapping and screenshot list first (with mock-ups or captures), and get owner approval. Then implement the generator and text, regenerate, and get owner approval of the final assets. Only after that can TASK-53.5/53.6 use them.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An owner-approved standout-feature catalog, linked to the walkthrough registry, maps each feature to its surfaces (walkthrough, Play, F-Droid and GitHub screenshots and description bullets)
- [ ] #2 Play, F-Droid and GitHub texts come from the catalog within each store's limits; the owner approves the final wording
- [ ] #3 Store screenshots regenerate with one documented command (deterministic data; plain for F-Droid/GitHub, 2:1-compliant framed for Play); CI or the build flags stale store images when their source screens change
- [ ] #4 The publishing flow for each store is documented; no unapproved store content can be published; the owner approves the final assets
<!-- AC:END -->

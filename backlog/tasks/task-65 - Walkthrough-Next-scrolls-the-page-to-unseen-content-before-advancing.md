---
id: TASK-65
title: Walkthrough Next scrolls the page to unseen content before advancing
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-04T17:59:00Z'
updated_date: '2026-11-14T21:00:00Z'
labels:
  - walkthrough
dependencies: []
priority: medium
type: enhancement
ordinal: 107000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**Problem.** Each walkthrough page (`ui/walkthrough/WalkthroughScreen.kt`) is a vertically scrolling column (`verticalScroll(rememberScrollState())`, ~line 190) inside a `HorizontalPager`. On small screens, large font scales or landscape, part of a page sits below the fold. Tapping **Next** (bottom bar, ~line 260, `WalkthroughIntent.Next`) moves straight to the next page, so users skip content they never saw. On permission pages that content can include the action itself.

**Wanted behaviour.**
- If the current page can still scroll down, tapping **Next** scrolls it down, by about one viewport or to the end, whichever is less. It does not change page.
- Once the page is at the bottom, or doesn't scroll at all, **Next** advances as it does today.
- The same rule applies to the last page's **Get started** / **Got it**: scroll first, then finish.
- Reduced motion (already honoured by `PagerSync`) scrolls instantly instead of animating.
- Swiping between pages and **Back** are unchanged.

**Design notes.**
- Hoisting the per-page `ScrollState` is the likely approach. Either keep it in the page composable and expose "can scroll forward" to the bottom bar, or keep the decision in the UI layer. It's transient UI state, so it doesn't need to go through the ViewModel unless that turns out simpler under MVI.
- Consider whether the button should hint that there's more, for example a label or icon change, or a subtle fade at the bottom edge. If the visible design changes, update the Figma walkthrough frames (specs/FIGMA.md).
- TalkBack: Next must still be reachable and must never get stuck, i.e. it always makes progress. Check that the scroll is announced or that focus lands sensibly.

**Related:**
- TASK-43, the feature walkthrough.
- TASK-57.20, onboarding fixes from the design review.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 On a walkthrough page with content below the fold, Next (and Get started / Got it on the last page) scrolls the page down instead of advancing; once at the bottom it advances as before
- [ ] #2 Pages that don't scroll behave exactly as today; swipe and Back are unchanged; reduced motion scrolls without animation
- [ ] #3 TalkBack can always progress with Next; unit/Roborazzi tests cover a scrollable page (e.g. large font scale) and a non-scrollable one; verified on an emulator with a small screen or large font
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. WalkthroughScreen.kt: each page keeps its own rememberScrollState (saved with the pager item, as today) and registers it in a hoisted snapshot map keyed by screen id (DisposableEffect), so the bottom bar can see the current page's ScrollState.
2. Next / Get started / Got it go through one UI-layer helper: if the current page's ScrollState.canScrollForward, scroll down by one viewport less a small overlap (min of that and the remaining distance; animateScrollTo, or scrollTo under reduced motion) and do not send an intent; otherwise send Next / Finish as today. ViewModel/MVI unchanged (transient UI state).
3. TalkBack: each tap either scrolls a finite distance or advances, so Next always progresses. While the button would scroll, its click action label becomes "Show more" (TalkBack: 'double tap to show more'), so the change is announced without a visible design change. New string walkthrough_show_more (en + iw).
4. No visible design change, so no Figma update.
5. Tests: a Robolectric Compose test on WalkthroughContent with a recording onIntent: small height/large font page -> Next scrolls (no intent) until bottom, then sends Next; last page Get started scrolls then sends Finish; a non-scrolling page sends Next on the first tap; reduced motion scrolls instantly; Back unaffected. Roborazzi capture of a scrolled page. Keep WalkthroughScreenshotTest.walkThrough robust (tap Next until the next page shows).
6. Build/test/lint + verifyRoborazziDebug; emulator check at large font, en + iw, TalkBack pass; evidence in /Users/daniel/.paseo/evidence/task-65/.
<!-- SECTION:PLAN:END -->

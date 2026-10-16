# Claude Design UI refresh

## Intent and reference
Adapt the existing native Compose / Material 3 app to the user's Claude Design project, Jewish Calendar App Design (fe15c347-4eef-410e-9639-d561a1ff21b0), `Jewish Calendar App.dc.html`, etag 1788938696112158. The exact 118707-byte source was downloaded through the connector with SHA-256 576a3e3b8888e2674ec7c76605d90f2b918d36fa1a75d632ca2f4baea3c6a329. Local inspection copy: design-reference/claude-design-reference.html. The whole-project export failed on thumbnail size validation; the primary design source downloaded successfully.

## Design context and reuse manifest
- Reference layouts and inline styles: adapt to native Compose. Ivory #FAF9F7, slate #3B4A55, ink #1B1C1E, muted #45484C, blue-gray selection #D6E1E8, warm sheet #ECE9E5, divider #DAD6D1.
- Typography: use the platform default font for all text, keeping the redesign's type scale. Bundled Manrope and Frank Ruhl Libre were dropped because Manrope has no Hebrew, which broke Hebrew weights (see `system-font.md`).
- Reference components: adapt calendar, day sheet, drawer, event and settings groups to existing Material 3 primitives; HTML components cannot be imported into Android.
- AndroidDevice frame/support.js: not applicable to native implementation; presentation scaffolding only.
- Existing MVI, Koin, KosherJava, Room/DataStore and notifications: preserve. No fabricated calendar facts or inert decorative controls.

## Implementation plan
1. Shared theme, typography, shapes, navigation and transition polish.
2. Calendar grid, mode controls, date picker and day-detail sheet fidelity.
3. Zmanim, events, onboarding, settings and ancillary screen consistency.
4. Independent Terra Medium critic iterates on compiled emulator screenshots AND accessibility XML. Report actionable issues to owners and repeat until accepted.
5. Root review for gaps, regressions, Hebrew/RTL, font scaling, dark mode, scrolling, click semantics, contrast, and motion.

## Acceptance
Clear hierarchy and consistent spacing, restrained motion, legible numeric times, no clipping or overlapping controls, accessible labels and native touch targets. Match reference visual language while keeping real data and complete existing flows. Build debug APK; run meaningful existing tests and appropriate checks; record emulator screen/XML evidence and findings. Do not claim perfect fidelity or test coverage without evidence. Feature merge requires user approval per repository workflow.

## Coordination
All implementation happens in this feature worktree. Agents have non-overlapping ownership, communicate cross-cutting needs, and loop through implementation, verification, criticism and refinement. A separate verification agent owns emulator interaction. No recurring automation is needed for the requested iterative loop.

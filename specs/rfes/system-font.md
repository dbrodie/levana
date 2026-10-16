# System Font Across the App

**Status:** Prototype for owner review (not approved)

## Problem

The redesign bundles two fonts: Frank Ruhl Libre (display) and Manrope (body). Manrope has no Hebrew glyphs, so all Hebrew body text falls back to the system Hebrew font. That fallback ignores the requested weight (SemiBold/Bold Hebrew renders regular, e.g. Settings section headers and zmanim headers), the Hebrew typeface varies by device, and mixed Hebrew/Latin lines switch typeface mid-line.

## Proposal

Use the platform default font (`FontFamily.Default`) for all text, display and body, instead of the bundled fonts. Android pairs its default Latin font with a matching-weight Hebrew font, so weights and mixed-script lines render consistently, and the app respects device and user font choices.

- `LevanaDisplayFontFamily` and `LevanaBodyFontFamily` become `FontFamily.Default`. The typography scale (sizes, line heights, weights) is unchanged.
- The bundled font files stay in the tree for this prototype so the change is easy to compare and revert. If approved, a follow-up removes `res/font/` and the font licenses, and updates `claude-design-refresh.md`'s typography line.

## Review plan

Capture the same screens from the current build and from this build, in English and Hebrew: calendar month, expanded day panel, full zmanim screen, Settings, and My events / Add event. The owner compares them before deciding.

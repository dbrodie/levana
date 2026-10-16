# System Font Across the App

**Status:** Approved by the owner after a side-by-side review (English and Hebrew)

## Problem

The redesign bundles two fonts: Frank Ruhl Libre (display) and Manrope (body). Manrope has no Hebrew glyphs, so all Hebrew body text falls back to the system Hebrew font. That fallback ignores the requested weight (SemiBold/Bold Hebrew renders regular, e.g. Settings section headers and zmanim headers), the Hebrew typeface varies by device, and mixed Hebrew/Latin lines switch typeface mid-line.

## Proposal

Use the platform default font (`FontFamily.Default`) for all text, display and body, instead of the bundled fonts. Android pairs its default Latin font with a matching-weight Hebrew font, so weights and mixed-script lines render consistently, and the app respects device and user font choices.

- All text uses the platform default font. The typography scale (sizes, line heights, weights) is unchanged.
- `LevanaDisplayFontFamily` and `LevanaBodyFontFamily` are removed, along with the explicit font family on the calendar's day numbers.
- The bundled fonts (`res/font/frank_ruhl_libre.ttf`, `res/font/manrope.ttf`) and their OFL license files (`assets/licenses/`) are removed. Nothing else referenced them.
- `claude-design-refresh.md`'s typography line is updated to match.

## Review plan

Capture the same screens from the current build and from this build, in English and Hebrew: calendar month, expanded day panel, full zmanim screen, Settings, and My events / Add event. The owner compares them before deciding.

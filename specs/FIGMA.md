# Figma

Design source for the UI: [Levana — Android App UI](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd). Design UI changes in Figma first, then implement them in Compose.

Copy on all pages follows [Terminology and formatting](TERMINOLOGY.md), including full and compact halachic time names and Hebrew-only labels in Hebrew screens.

## File layout

- **Foundations / variables**: tokens. Each variable's Android code syntax names its theme value (e.g. `MaterialTheme.colorScheme.primary`).
- **Components**: one per shared composable. The description names the Compose source and its defaults.
- **Screens — Light / Dark / Hebrew (RTL)**: one section per feature. Dark and Hebrew show expected results only; Compose derives them from the theme and layout direction.
- **Onboarding**: reference pages plus a spec card per page. **Widgets**: Glance widgets, tile, launcher icon.

## Implementing a design

1. Read the frame through the Figma MCP (`get_design_context`, with the `figma-design-to-code` skill).
2. Map variables and text styles to `ui/theme/` and add missing tokens there first. Never hardcode values.
3. Reuse the composable behind each component instance. Build a new composable only if it's a new component.
4. Strings go in `values` and `values-iw`; the Hebrew frames show the expected text.
5. Verify on the emulator (`android-debug` skill) in light, dark and Hebrew against the frames.
6. If the implementation must deviate, update Figma so it stays accurate.

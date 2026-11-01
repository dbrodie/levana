# Figma

The Figma file mirrors the shipped UI: [Levana — Android App UI](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd). Code is the source of truth.

## When the UI changes

1. **Tokens** (`ui/theme/`): update the matching variable or text style. Colours have Light and Dark modes.
2. **Shared UI**: update the component on the Components page. Mirror the change in its `Direction=RTL` variant.
3. **Screens**: update the affected frames. Dark copies are in Screens — Dark, and key screens have Hebrew copies in Screens — Hebrew (RTL).
4. **New screens**: build them from components, place them in their feature section, then add Dark and Hebrew copies.
5. **Verify**: compare against emulator screenshots (`android-debug` skill), including the Hebrew and dark versions if affected.

## Rules

- Editable layers only; no screenshots inside frames.
- Bind colours, spacing and radii to variables, and text to text styles.
- New onboarding pages: add a spec card on the Onboarding page.

# Collapsing Calendar Header on Event Scroll

## Status: Implemented

## Context

The month view has two sections: the calendar grid (ElevatedCard, fixed) and the day detail events (scrollable). The goal is a unified, Material Design 3-style experience where the calendar grid collapses as the user scrolls down through events — giving more room to content — and re-expands when the user scrolls back to the top. This mirrors the pattern used in Google Calendar and other MD3 apps.

## Behavior

- **Expanded state** (default): Full month grid is visible. Events are at the bottom.
- **Collapsed state**: When the user scrolls down in the events panel, the `ElevatedCard` containing the month grid collapses to zero height and fades out.
- **Re-expansion triggers**:
  - Events scroll position returns to 0 (user scrolls back up past top of events list)
  - User taps a different day (calendar expands, both panels animate back)
  - User begins swiping left/right on the month pager (calendar expands immediately)
- **Animation**: `spring(stiffness = Spring.StiffnessMediumLow)` on calendar height; `animateScrollTo(0)` on events — both run concurrently.

## Implementation

### Approach: `NestedScrollConnection`

The initial attempt used `scrollState.value == 0` to drive collapse. This caused a feedback loop: collapsing the calendar expanded the events panel, which made content fit without scrolling, resetting scroll to 0 and re-expanding immediately.

The correct approach is a `NestedScrollConnection` on the outer `Column`, which intercepts the gesture itself:
- `onPreScroll` (scrolling down): consumes scroll to collapse the calendar **before** the events list starts scrolling
- `onPostScroll` (scrolling up): catches unconsumed scroll after events reach the top, expanding the calendar

This is gesture-driven, not layout-feedback-driven, so there is no loop.

### Key state

```kotlin
val calendarHeightPx = remember { mutableIntStateOf(0) }    // natural height, captured on first layout
val calendarHiddenPx = remember { mutableFloatStateOf(0f) } // pixels currently hidden (0 = expanded)
val isResetting = remember { booleanArrayOf(false) }         // suppresses onPostScroll during animateExpand()
```

`calendarFraction` is `derivedStateOf { 1f - (calendarHiddenPx / calendarHeightPx).coerceIn(0,1) }` and drives both the `layout` modifier (shrinks reported height) and `.alpha()`.

`calendarHeightPx` is captured once inside the `layout` modifier on the first frame (`if (calendarHeightPx == 0 && placeable.height > 0)`). Setting it from within `layout` causes one extra layout pass on the first frame; subsequent frames are a no-op due to the guard.

### Animated expand

```kotlin
suspend fun animateExpand() = coroutineScope {
    launch {
        Animatable(calendarHiddenPx.floatValue).animateTo(
            targetValue = 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) { calendarHiddenPx.floatValue = value }
    }
    launch {
        eventsScrollState.animateScrollTo(0)
    }
}
```

`isResetting[0]` is set to `true` around `animateExpand()` calls to prevent `onPostScroll` from also driving `calendarHiddenPx` during the animation (they would fight).

### Triggers

```kotlin
// Day tap
LaunchedEffect(state.selectedDate) {
    isResetting[0] = true
    try { animateExpand() } finally { isResetting[0] = false }
}

// Month pager swipe begins
LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.isScrollInProgress }
        .collect { isScrolling ->
            if (isScrolling) {
                isResetting[0] = true
                try { animateExpand() } finally { isResetting[0] = false }
            }
        }
}
```

### Files changed

- `app/src/main/java/com/levana/app/ui/calendar/CalendarScreen.kt` — main changes (both `GregorianCalendarContent` and `HebrewCalendarContent`)
- `app/src/main/java/com/levana/app/ui/daydetail/DayDetailScreen.kt` — `scrollState` parameter hoisted out of `DayDetailContent` so callers can pass their own

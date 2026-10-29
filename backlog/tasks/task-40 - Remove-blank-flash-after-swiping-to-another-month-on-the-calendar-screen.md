---
id: TASK-40
title: Remove blank flash after swiping to another month on the calendar screen
status: To Do
assignee: []
created_date: '2026-10-29T11:02:00Z'
labels:
  - calendar
  - ui
dependencies: []
references:
  - app/src/main/java/ninja/doskey/app/levana/ui/calendar/CalendarScreen.kt
  - app/src/main/java/ninja/doskey/app/levana/ui/calendar/CalendarViewModel.kt
priority: medium
type: bug
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
After swiping left/right to another month on the main calendar screen (Gregorian or Hebrew mode), the grid briefly blanks/blinks once the swipe settles, even though the month was already visible during the swipe.

Root cause (research, commit abddf8c):
- CalendarScreen.kt GregorianCalendarContent/HebrewCalendarContent: on pagerState.settledPage change they send CalendarIntent.LoadMonth / LoadHebrewMonth.
- CalendarViewModel.loadMonth / loadHebrewMonth (keepContent=false) synchronously set isLoading=true, currentMonth/hebrewYearMonth=target and monthDays=emptyList().
- The pager page renders state.monthDays when pageMonth == currentMonth (otherwise the cache). So the page that was drawn from gregorianMonthCache/hebrewMonthCache during the swipe switches to the now-empty monthDays -> empty Box (+ spinner) -> repopulated when loadJob finishes. That is the flash.
- The header subtitle (CalendarHeaderFormatter.hebrewMonthRange / gregorianRange over state.monthDays) also blanks for the same frame(s).
- The loadJob then replaces the cache with only mapOf(target to days), dropping neighbours until adjacentJob finishes, so a quick second swipe can land on an empty page too.

Suggested direction: when the target month is already in the cache, set monthDays from the cache immediately (no isLoading, no clearing) and keep existing cache entries while the reload/adjacent prefetch runs; only show the empty/loading state when there is genuinely no data. Optionally make the pager page fall back to the cache when monthDays is empty.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Swiping to an adjacent month in Gregorian mode shows no blank/flash of the grid or header subtitle after the swipe settles
- [ ] #2 Same for Hebrew mode
- [ ] #3 Rapid consecutive swipes do not land on an empty page when neighbour data was already loaded
- [ ] #4 Go-to-date / Today jumps to an uncached month still show a loading state and then the correct month
- [ ] #5 ViewModel unit test covers: loading a cached month keeps monthDays non-empty and isLoading=false throughout
<!-- AC:END -->

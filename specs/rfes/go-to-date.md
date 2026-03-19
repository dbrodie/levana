# RFE: Go to Date

## Summary

Add a "Go to Date" toolbar button that lets users jump directly to any date in the calendar.

## Motivation

The calendar currently only supports "Go to Today". Users have no way to jump to an arbitrary date without scrolling month-by-month.

## Product Decisions

- **Toolbar placement**: New icon button (`Icons.Filled.EditCalendar`) inserted between "Go to Today" and "Add Event" in both `GregorianMonthHeader` and `HebrewMonthHeader`.
- **Dialog**: Two sections — Hebrew spinner picker + Gregorian spinner picker — both always editable; editing either updates the other in real time.
- **Navigation result**: Scrolls calendar to the target month AND selects/highlights that day.
- **Default section order**: Hebrew section first in Hebrew mode; Gregorian section first in Gregorian mode.
- **Gregorian picker style**: Same +/− spinner pattern as the existing `HebrewDatePicker`.
- **Invalid dates**: Auto-clamp (day capped to the last valid day of the selected month).
- **Year ranges**: Hebrew 5700–5900, Gregorian 1940–2140.

## Implementation

### New intents (`CalendarIntent`)
- `OpenGoToDateDialog`
- `CloseGoToDateDialog`
- `GoToDate(date: LocalDate)`

### New state field (`CalendarState`)
- `showGoToDateDialog: Boolean = false`

### ViewModel handlers
- `OpenGoToDateDialog` → set `showGoToDateDialog = true`
- `CloseGoToDateDialog` → set `showGoToDateDialog = false`
- `GoToDate(date)` → set `selectedDate = date`, `showGoToDateDialog = false`, load month for that date in the active mode

### New composable: `GoToDateDialog`
Material3 `AlertDialog` with:
- Hebrew spinner row (day / month name / year)
- Divider
- Gregorian spinner row (day / month name / year)
- Real-time bidirectional sync via `JewishDate`
- "Go to Date" confirm button, "Cancel" dismiss button

### CalendarScreen changes
- Add `onGoToDate` parameter to both header composables
- Insert `IconButton(EditCalendar)` between Today and Add Event in each header
- Show `GoToDateDialog` when `state.showGoToDateDialog`

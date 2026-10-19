# Fix: System Calendar Event Indicators

## Problem

`SystemCalendarRepository` converts every `Instances.BEGIN`/`END` through the
device time zone and marks only the start date of each instance:

- All-day events are stored by Android as UTC midnight boundaries. In zones
  west of UTC they appear on the previous day; east of UTC the day-detail query
  can also pick up the neighbouring day's all-day event.
- Multi-day events (all-day or timed) show a dot only on their first day.
- Events that began before the queried range (e.g. last month) never show a
  dot inside the range, because their start date is outside it.

## Behaviour

- **All-day instances** use UTC date semantics: they cover the UTC dates
  `[date(BEGIN), date(END))`; `END` is exclusive. A zero/negative-length
  all-day instance covers just its start date. Their `startTime`/`endTime` are
  the UTC wall-clock values (midnight of the calendar dates), independent of
  device zone.
- **Timed instances** stay time-zone aware: converted with the device zone and
  cover every local date from `date(BEGIN)` through `date(END)`, except that an
  end at exactly local midnight is exclusive (an event ending 00:00 on day N
  does not mark day N). A zero-length event covers its start date.
- `getEventColorsForDateRange(start, end)` marks every date in `[start, end]`
  covered by an instance, including instances starting before `start` or ending
  after `end`. Colors per date remain de-duplicated and capped at `MAX_DOTS`.
- `getEventsForDate(date)` returns exactly the instances covering `date`.
- The provider query window is widened by one day on each side so UTC-anchored
  all-day instances are not missed; results are then filtered by coverage.
- Recurring events are unaffected: each instance row is handled separately.

## Implementation

- Add a pure internal helper computing the covered date range of an instance
  (`allDay`, begin/end millis, zone), used by both public methods.
- No changes to the domain model, ViewModels or UI.

## Tests

- JVM unit tests for the coverage helper (all-day in negative/positive zones,
  multi-day, midnight-exclusive timed end, overnight timed, zero-length).
- Robolectric tests for the repository with a fake `CalendarContract` provider:
  month-range dots for multi-day/prior-month/all-day/recurring instances, and
  `getEventsForDate` filtering of neighbouring all-day events.

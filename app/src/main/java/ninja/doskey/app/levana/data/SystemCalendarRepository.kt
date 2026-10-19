package ninja.doskey.app.levana.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import ninja.doskey.app.levana.domain.model.DeviceCalendar
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class SystemCalendarRepository(private val context: Context) {

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getDeviceCalendars(): List<DeviceCalendar> {
        if (!hasCalendarPermission()) return emptyList()

        val calendars = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndex(
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )
            val accountIdx = cursor.getColumnIndex(
                CalendarContract.Calendars.ACCOUNT_NAME
            )
            val colorIdx = cursor.getColumnIndex(
                CalendarContract.Calendars.CALENDAR_COLOR
            )

            while (cursor.moveToNext()) {
                calendars.add(
                    DeviceCalendar(
                        id = cursor.getLong(idIdx),
                        name = cursor.getString(nameIdx) ?: "",
                        accountName = cursor.getString(accountIdx) ?: "",
                        color = cursor.getInt(colorIdx)
                    )
                )
            }
        }

        return calendars
    }

    fun getEventsForDate(date: LocalDate, calendarIds: Set<Long>): List<SystemCalendarEvent> {
        if (!hasCalendarPermission() || calendarIds.isEmpty()) {
            return emptyList()
        }

        val zone = ZoneId.systemDefault()
        return queryInstances(date, date, calendarIds, zone)
            .filter { it.coveredDates.overlaps(date, date) }
            .map { it.event }
    }

    fun getEventColorsForDateRange(
        start: LocalDate,
        end: LocalDate,
        calendarIds: Set<Long>
    ): Map<LocalDate, List<Int>> {
        if (!hasCalendarPermission() || calendarIds.isEmpty()) {
            return emptyMap()
        }

        val zone = ZoneId.systemDefault()
        val instances = queryInstances(start, end, calendarIds, zone)

        val result = mutableMapOf<LocalDate, MutableSet<Int>>()
        for (instance in instances) {
            val dates = instance.coveredDates
            var day = maxOf(dates.start, start)
            val last = minOf(dates.endInclusive, end)
            while (!day.isAfter(last)) {
                result.getOrPut(day) { linkedSetOf() }
                    .add(instance.event.calendarColor)
                day = day.plusDays(1)
            }
        }

        return result.mapValues { it.value.toList().take(MAX_DOTS) }
    }

    private fun queryInstances(
        start: LocalDate,
        end: LocalDate,
        calendarIds: Set<Long>,
        zone: ZoneId
    ): List<CalendarInstance> {
        val events = mutableListOf<CalendarInstance>()

        // Widen by a day on each side: all-day instances are anchored to UTC
        // midnight, which can fall outside the local-day window. Results are
        // filtered by their covered dates afterwards.
        val startMillis = start.minusDays(1).atStartOfDay(zone).toInstant()
            .toEpochMilli()
        val endMillis = end.plusDays(2).atStartOfDay(zone).toInstant()
            .toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_COLOR
        )

        val calendarIdList = calendarIds.joinToString(",")
        val selection =
            "${CalendarContract.Instances.CALENDAR_ID} IN ($calendarIdList)"

        context.contentResolver.query(
            builder.build(),
            projection,
            selection,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val eventIdIdx = cursor.getColumnIndex(
                CalendarContract.Instances.EVENT_ID
            )
            val titleIdx = cursor.getColumnIndex(
                CalendarContract.Instances.TITLE
            )
            val beginIdx = cursor.getColumnIndex(
                CalendarContract.Instances.BEGIN
            )
            val endIdx = cursor.getColumnIndex(
                CalendarContract.Instances.END
            )
            val allDayIdx = cursor.getColumnIndex(
                CalendarContract.Instances.ALL_DAY
            )
            val calIdIdx = cursor.getColumnIndex(
                CalendarContract.Instances.CALENDAR_ID
            )
            val colorIdx = cursor.getColumnIndex(
                CalendarContract.Instances.CALENDAR_COLOR
            )

            while (cursor.moveToNext()) {
                val calId = cursor.getLong(calIdIdx)
                if (calId !in calendarIds) continue

                val beginMs = cursor.getLong(beginIdx)
                val endMs = cursor.getLong(endIdx)
                val allDay = cursor.getInt(allDayIdx) == 1
                // All-day instances are stored as UTC midnight boundaries.
                val eventZone = if (allDay) ZoneOffset.UTC else zone

                val event = SystemCalendarEvent(
                    eventId = cursor.getLong(eventIdIdx),
                    title = cursor.getString(titleIdx) ?: "",
                    startTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(beginMs),
                        eventZone
                    ),
                    endTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(endMs),
                        eventZone
                    ),
                    allDay = allDay,
                    calendarId = calId,
                    calendarColor = cursor.getInt(colorIdx)
                )
                events.add(
                    CalendarInstance(
                        event,
                        coveredDates(beginMs, endMs, allDay, zone)
                    )
                )
            }
        }

        return events
    }

    private class CalendarInstance(
        val event: SystemCalendarEvent,
        val coveredDates: ClosedRange<LocalDate>
    )

    companion object {
        const val MAX_DOTS = 3

        /**
         * The calendar dates an instance occupies. All-day instances use UTC
         * dates with an exclusive end; timed instances use [zone] dates, with
         * an end at exactly midnight treated as exclusive.
         */
        internal fun coveredDates(
            beginMillis: Long,
            endMillis: Long,
            allDay: Boolean,
            zone: ZoneId
        ): ClosedRange<LocalDate> {
            val dateZone = if (allDay) ZoneOffset.UTC else zone
            val begin = Instant.ofEpochMilli(beginMillis).atZone(dateZone)
            val first = begin.toLocalDate()
            if (endMillis <= beginMillis) return first..first

            val end = Instant.ofEpochMilli(endMillis).atZone(dateZone)
            val last = if (end.toLocalTime() == LocalTime.MIDNIGHT) {
                end.toLocalDate().minusDays(1)
            } else {
                end.toLocalDate()
            }
            return first..maxOf(first, last)
        }

        private fun ClosedRange<LocalDate>.overlaps(
            from: LocalDate,
            to: LocalDate
        ): Boolean = !start.isAfter(to) && !endInclusive.isBefore(from)
    }
}

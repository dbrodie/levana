package com.levana.app.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.levana.app.domain.model.DeviceCalendar
import com.levana.app.domain.model.SystemCalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant()
            .toEpochMilli()

        return queryInstances(startMillis, endMillis, calendarIds, zone)
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
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant()
            .toEpochMilli()

        val events = queryInstances(startMillis, endMillis, calendarIds, zone)

        val result = mutableMapOf<LocalDate, MutableSet<Int>>()
        for (event in events) {
            val eventDate = event.startTime.toLocalDate()
            result.getOrPut(eventDate) { mutableSetOf() }
                .add(event.calendarColor)
        }

        return result.mapValues { it.value.toList().take(MAX_DOTS) }
    }

    private fun queryInstances(
        startMillis: Long,
        endMillis: Long,
        calendarIds: Set<Long>,
        zone: ZoneId
    ): List<SystemCalendarEvent> {
        val events = mutableListOf<SystemCalendarEvent>()

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

                events.add(
                    SystemCalendarEvent(
                        eventId = cursor.getLong(eventIdIdx),
                        title = cursor.getString(titleIdx) ?: "",
                        startTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(beginMs),
                            zone
                        ),
                        endTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(endMs),
                            zone
                        ),
                        allDay = cursor.getInt(allDayIdx) == 1,
                        calendarId = calId,
                        calendarColor = cursor.getInt(colorIdx)
                    )
                )
            }
        }

        return events
    }

    companion object {
        const val MAX_DOTS = 3
    }
}

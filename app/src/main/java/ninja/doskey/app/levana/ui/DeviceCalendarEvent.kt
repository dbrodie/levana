package ninja.doskey.app.levana.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

/** Opens a device-calendar event in the system calendar app (day panel, agenda). */
fun openDeviceCalendarEvent(context: Context, eventId: Long) {
    val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
    context.startActivity(Intent(Intent.ACTION_VIEW).setData(uri))
}

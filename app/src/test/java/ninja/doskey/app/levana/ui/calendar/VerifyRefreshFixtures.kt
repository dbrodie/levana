package ninja.doskey.app.levana.ui.calendar

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.GregorianCalendar
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao

/** Verifier fixtures; deliberately only uses APIs that exist on the baseline too. */

fun jewish(date: LocalDate) =
    JewishDate(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))

fun personalEventOn(title: String, date: LocalDate, id: Long = 0): PersonalEvent {
    val jd = jewish(date)
    return PersonalEvent(
        id = id,
        title = title,
        hebrewDay = jd.jewishDayOfMonth,
        hebrewMonth = jd.jewishMonth,
        hebrewYear = jd.jewishYear
    )
}

/** Counts full reads of the personal_events table (one per month/day load). */
class ReadCountingDao(private val dao: PersonalEventDao) : PersonalEventDao by dao {
    val reads = AtomicInteger()
    override suspend fun getAllOnce(): List<PersonalEvent> {
        reads.incrementAndGet()
        return dao.getAllOnce()
    }
}

/**
 * A minimal contacts provider serving the Levana Hebrew-birthday data rows. Mutations notify
 * a descendant of the authority URI, like the real provider does.
 */
class FakeContactsProvider : ContentProvider() {
    data class Row(val lookup: String, val name: String, val day: Int, val month: Int, val year: Int)

    override fun onCreate() = true

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        queries.incrementAndGet()
        val cursor = MatrixCursor(
            arrayOf(
                ContactsContract.Data.LOOKUP_KEY,
                ContactsContract.Data.DISPLAY_NAME_PRIMARY,
                ContactsContract.Data.PHOTO_THUMBNAIL_URI,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3
            )
        )
        if (selectionArgs?.firstOrNull() == ContactBirthdayRepository.MIME_TYPE) {
            rows.forEach {
                cursor.addRow(arrayOf(it.lookup, it.name, null, "${it.day}", "${it.month}", "${it.year}"))
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ) = 0

    companion object {
        val rows = CopyOnWriteArrayList<Row>()
        val queries = AtomicInteger()

        fun reset() {
            rows.clear()
            queries.set(0)
        }

        fun birthdayOn(name: String, date: LocalDate): Row {
            val jd = jewish(date)
            return Row("lk-$name", name, jd.jewishDayOfMonth, jd.jewishMonth, jd.jewishYear)
        }
    }
}

/** A minimal calendar provider serving Instances for the requested window. */
class FakeCalendarProvider : ContentProvider() {
    data class Instance(
        val eventId: Long, val title: String, val calendarId: Long, val color: Int,
        val begin: Long, val end: Long, val allDay: Boolean
    )

    override fun onCreate() = true

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        queries.incrementAndGet()
        val cursor = MatrixCursor(
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.CALENDAR_COLOR
            )
        )
        val segments = uri.pathSegments
        // instances/when/<start>/<end>
        if (segments.size >= 4 && segments[0] == "instances") {
            val start = segments[2].toLong()
            val end = segments[3].toLong()
            instances.filter { it.begin < end && it.end > start }.forEach {
                cursor.addRow(
                    arrayOf(
                        it.eventId, it.title, it.begin, it.end,
                        if (it.allDay) 1 else 0, it.calendarId, it.color
                    )
                )
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ) = 0

    companion object {
        val instances = CopyOnWriteArrayList<Instance>()
        val queries = AtomicInteger()

        fun reset() {
            instances.clear()
            queries.set(0)
        }

        /** A timed one-hour instance at 10:00 local time on [date]. */
        fun timedOn(id: Long, title: String, calendarId: Long, color: Int, date: LocalDate): Instance {
            val begin = date.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return Instance(id, title, calendarId, color, begin, begin + 3_600_000, false)
        }

        fun allDayOn(id: Long, title: String, calendarId: Long, color: Int, date: LocalDate): Instance {
            val begin = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            return Instance(id, title, calendarId, color, begin, begin + 86_400_000, true)
        }
    }
}

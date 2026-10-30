package ninja.doskey.app.levana.domain.agenda

import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import java.time.LocalDate
import java.time.LocalTime

/**
 * One entry of the agenda. [time] is the clock time it is listed at, null for an all-day
 * entry; candle lighting and havdalah are in the selected city's clock, device events in the
 * device's.
 */
sealed interface AgendaItem {
    val time: LocalTime?

    /** A holiday, fast or Rosh Chodesh; [roshChodeshOf] is the month a Rosh Chodesh begins. */
    data class HolidayItem(val holiday: Holiday, val roshChodeshOf: HebrewMonth? = null) : AgendaItem {
        override val time: LocalTime? = null
    }

    /** Candle lighting on the evening that begins Shabbat or Yom Tov. */
    data class CandleLighting(override val time: LocalTime) : AgendaItem

    /** A Shabbat with its weekly reading, and havdalah when it ends that evening. */
    data class Shabbat(
        val parsha: String,
        val parshaHebrew: String?,
        val specialShabbat: String?,
        val havdalah: LocalTime?
    ) : AgendaItem {
        override val time: LocalTime? = null
    }

    /** Havdalah at the end of Yom Tov (or a Shabbat without a weekly reading). */
    data class Havdalah(override val time: LocalTime) : AgendaItem

    /** A local personal event; a yahrzeit when [PersonalEvent.useYahrzeitRules]. */
    data class PersonalEventItem(val event: PersonalEvent, val label: OccurrenceLabel?) : AgendaItem {
        override val time: LocalTime? = null
        val isYahrzeit: Boolean get() = event.useYahrzeitRules
    }

    /** A contact's Hebrew birthday. */
    data class BirthdayItem(val birthday: ContactBirthday, val label: OccurrenceLabel?) : AgendaItem {
        override val time: LocalTime? = null
    }

    /** A selected device-calendar event; [time] is its start on the day it starts, else null. */
    data class DeviceEventItem(val event: SystemCalendarEvent, override val time: LocalTime?) : AgendaItem
}

/** A day of the agenda: its Hebrew date and its entries, in display order (never empty). */
data class AgendaDay(
    val date: LocalDate,
    val hebrewDay: HebrewDay,
    val items: List<AgendaItem>
)

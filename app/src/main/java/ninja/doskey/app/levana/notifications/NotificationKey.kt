package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import java.time.LocalDate

/**
 * Identity of a posted reminder. Android identifies a notification by `(tag, id)`: [tag] is the
 * full reminder identity (type, ISO date and, where needed, the observance or the persistent
 * event / contact identity) and [id] is a constant per reminder type. Tags hold no localized
 * text, so reposting the same reminder — after an app-language change too — replaces it.
 *
 * [legacyId] is the untagged id the previous version used for the same reminder; it is
 * cancelled when the reminder is posted so an upgrade does not leave a duplicate behind.
 */
internal data class NotificationKey(val tag: String, val id: Int, val legacyId: Int)

internal object NotificationKeys {

    const val ID_CANDLE_LIGHTING = 1
    const val ID_HOLIDAY = 2
    const val ID_FAST = 3
    const val ID_PERSONAL_EVENT = 4
    const val ID_BIRTHDAY = 5
    const val ID_OMER_TZAIT = 6
    const val ID_OMER_MORNING = 7

    fun candleLighting(date: LocalDate) =
        NotificationKey("candle_lighting/$date", ID_CANDLE_LIGHTING, legacyDayId(0, date))

    /** Keyed on [Holiday.name], a constant English name, so observances on one date differ. */
    fun holiday(date: LocalDate, holiday: Holiday) =
        NotificationKey("holiday/$date/${holiday.name}", ID_HOLIDAY, legacyDayId(1, date))

    fun fast(date: LocalDate, fast: Holiday) =
        NotificationKey("fast/$date/${fast.name}", ID_FAST, legacyDayId(2, date))

    /** Keyed on the event's database id, so events with the same title stay separate. */
    fun personalEvent(date: LocalDate, eventId: Long, title: String, label: OccurrenceLabel?) =
        NotificationKey(
            "personal_event/$date/$eventId",
            ID_PERSONAL_EVENT,
            legacyDayId(3, date) + "event|$title|$label".hashCode() % 1000
        )

    /**
     * Keyed on the contact's lookup key, so contacts with the same name stay separate; the
     * name is only used if the contact has no lookup key.
     */
    fun birthday(
        date: LocalDate,
        contactLookupKey: String,
        contactName: String,
        label: OccurrenceLabel?
    ): NotificationKey {
        val contact = if (contactLookupKey.isNotEmpty()) {
            "key:$contactLookupKey"
        } else {
            "name:$contactName"
        }
        return NotificationKey(
            "birthday/$date/$contact",
            ID_BIRTHDAY,
            legacyDayId(3, date) + "birthday|$contactName|$label".hashCode() % 1000
        )
    }

    fun omerTzait(date: LocalDate) =
        NotificationKey("omer_tzait/$date", ID_OMER_TZAIT, legacyDayId(4, date))

    fun omerMorning(date: LocalDate) =
        NotificationKey("omer_morning/$date", ID_OMER_MORNING, legacyDayId(4, date) + 1)

    /** The previous `channelOrdinal * 10000 + dayOfYear` scheme. */
    private fun legacyDayId(channelOrdinal: Int, date: LocalDate) =
        channelOrdinal * 10000 + date.dayOfYear
}

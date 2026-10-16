package ninja.doskey.app.levana.notifications

import android.content.Context
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.ui.events.occurrenceTitle

/** Text of a Hebrew birthday notification. [eventTitle] includes the occurrence label, if any. */
data class BirthdayNotificationText(
    val title: String,
    val body: String,
    val eventTitle: String
)

/** Builds the birthday notification text in [context]'s language (see [withAppLocale]). */
fun birthdayNotificationText(
    context: Context,
    contactName: String,
    label: OccurrenceLabel?
): BirthdayNotificationText {
    val eventTitle = occurrenceTitle(
        context,
        context.getString(R.string.notification_birthday_event, contactName),
        label
    )
    return BirthdayNotificationText(
        title = context.getString(R.string.notification_birthday_title),
        body = context.getString(R.string.notification_birthday_body, eventTitle),
        eventTitle = eventTitle
    )
}

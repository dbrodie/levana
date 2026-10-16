package ninja.doskey.app.levana.domain.model

/** A contact birthday observed on a given day; [label] is set when it was moved there. */
data class BirthdayOccurrence(
    val birthday: ContactBirthday,
    val label: OccurrenceLabel? = null
)

package ninja.doskey.app.levana.ui.events

import ninja.doskey.app.levana.data.db.PersonalEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class EventsImportTest {

    @Test
    fun deduplicateImportedEventsSkipsExistingAndInFileDuplicates() {
        val existing = listOf(
            event(title = "Existing", day = 1, month = 7, year = 5785)
        )
        val newEvent = event(title = "New", day = 2, month = 7, year = 5785)
        val parsed = listOf(
            existing.first(),
            newEvent,
            newEvent.copy(notes = "Different notes")
        )

        val plan = deduplicateImportedEvents(existing = existing, parsed = parsed)

        assertEquals(listOf(newEvent), plan.events)
        assertEquals(2, plan.skipped)
    }

    private fun event(title: String, day: Int, month: Int, year: Int) = PersonalEvent(
        title = title,
        hebrewDay = day,
        hebrewMonth = month,
        hebrewYear = year
    )
}

package ninja.doskey.app.levana.data

import ninja.doskey.app.levana.data.db.PersonalEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EventSerializerTest {

    @Test
    fun jsonRoundTripOmitsRoomIdAndPreservesFields() {
        val events = listOf(
            PersonalEvent(
                id = 42,
                title = "Yahrzeit",
                hebrewDay = 10,
                hebrewMonth = 12,
                hebrewYear = 5784,
                notes = "Light candle",
                useYahrzeitRules = true
            )
        )

        val json = EventSerializer.toJson(events)
        val parsed = EventSerializer.fromJson(json)

        assertFalse(json.contains("\"id\""))
        assertEquals(events.map { it.copy(id = 0) }, parsed)
    }

    @Test
    fun csvRoundTripPreservesCommasQuotesAndNewlines() {
        val events = listOf(
            PersonalEvent(
                id = 7,
                title = "Dinner, \"quoted\"",
                hebrewDay = 15,
                hebrewMonth = 1,
                hebrewYear = 5784,
                notes = "Line one\nLine two, with comma and \"quote\"",
                useYahrzeitRules = false
            ),
            PersonalEvent(
                id = 8,
                title = "Yahrzeit",
                hebrewDay = 2,
                hebrewMonth = 13,
                hebrewYear = 5784,
                notes = "",
                useYahrzeitRules = true
            )
        )

        val csv = EventSerializer.toCsv(events)
        val parsed = EventSerializer.fromCsv(csv)

        assertEquals(events.map { it.copy(id = 0) }, parsed)
    }

    @Test
    fun csvImportAcceptsCrLfLineEndingsAndSkipsBlankLines() {
        val csv = """
            title,hebrewDay,hebrewMonth,hebrewYear,notes,useYahrzeitRules
            Birthday,1,7,5785,,false

            Yahrzeit,2,8,5785,Note,true
        """.trimIndent().replace("\n", "\r\n")

        val parsed = EventSerializer.fromCsv(csv)

        assertEquals(
            listOf(
                PersonalEvent(
                    title = "Birthday",
                    hebrewDay = 1,
                    hebrewMonth = 7,
                    hebrewYear = 5785
                ),
                PersonalEvent(
                    title = "Yahrzeit",
                    hebrewDay = 2,
                    hebrewMonth = 8,
                    hebrewYear = 5785,
                    notes = "Note",
                    useYahrzeitRules = true
                )
            ),
            parsed
        )
    }

    @Test
    fun csvImportRejectsUnexpectedHeader() {
        val csv = """
            name,hebrewDay,hebrewMonth,hebrewYear,notes,useYahrzeitRules
            Event,1,1,5785,,false
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            EventSerializer.fromCsv(csv)
        }
    }

    @Test
    fun csvImportRejectsUnclosedQuotedField() {
        val csv = """
            title,hebrewDay,hebrewMonth,hebrewYear,notes,useYahrzeitRules
            "Broken,1,1,5785,,false
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            EventSerializer.fromCsv(csv)
        }
    }

    // ── Bad dates are rejected on import ─────────────────────────────────────

    private fun json(day: Int = 10, month: Int = 9, year: Any = 5785) = """
        [{"title":"Bad","hebrewDay":$day,"hebrewMonth":$month,"hebrewYear":$year}]
    """.trimIndent()

    private fun csv(day: Int = 10, month: Int = 9, year: Any = 5785) =
        "title,hebrewDay,hebrewMonth,hebrewYear,notes,useYahrzeitRules\n" +
            "Bad,$day,$month,$year,,false\n"

    private fun assertRejected(expectedInMessage: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            assertTrue(
                "message was: ${e.message}",
                e.message.orEmpty().contains(expectedInMessage)
            )
            return
        }
        fail("Expected the import to be rejected")
    }

    @Test
    fun importRejectsYearsTheCalendarCannotHandle() {
        for (year in listOf(1, -1, 3761, 10000, 99999)) {
            assertRejected("Invalid Hebrew year $year") { EventSerializer.fromJson(json(year = year)) }
            assertRejected("Invalid Hebrew year $year") { EventSerializer.fromCsv(csv(year = year)) }
        }
    }

    @Test
    fun importRejectsYearsThatDoNotFitAnInt() {
        assertRejected("") { EventSerializer.fromJson(json(year = "99999999999999")) }
        assertRejected("") { EventSerializer.fromCsv(csv(year = "99999999999999")) }
    }

    @Test
    fun importErrorNamesTheEvent() {
        assertRejected("\"Bad\"") { EventSerializer.fromJson(json(year = 1)) }
    }

    @Test
    fun importAcceptsUnknownYearAndBoundaryYears() {
        for (year in listOf(0, 3762, 5650, 5785, 9999)) {
            assertEquals(year, EventSerializer.fromJson(json(year = year)).single().hebrewYear)
            assertEquals(year, EventSerializer.fromCsv(csv(year = year)).single().hebrewYear)
        }
    }

    @Test
    fun importRejectsImpossibleMonthsAndDays() {
        for (month in listOf(0, -1, 14)) {
            assertRejected("Invalid Hebrew month $month") { EventSerializer.fromJson(json(month = month)) }
            assertRejected("Invalid Hebrew month $month") { EventSerializer.fromCsv(csv(month = month)) }
        }
        for (day in listOf(0, -1, 31)) {
            assertRejected("Invalid Hebrew day $day") { EventSerializer.fromJson(json(day = day)) }
            assertRejected("Invalid Hebrew day $day") { EventSerializer.fromCsv(csv(day = day)) }
        }
        assertEquals(13, EventSerializer.fromJson(json(month = 13, day = 30)).single().hebrewMonth)
    }

    @Test
    fun oneBadEventRejectsTheWholeFile() {
        val content = """
            [{"title":"Good","hebrewDay":1,"hebrewMonth":7,"hebrewYear":5785},
             {"title":"Bad","hebrewDay":1,"hebrewMonth":7,"hebrewYear":-1}]
        """.trimIndent()
        assertRejected("Invalid Hebrew year -1") { EventSerializer.fromJson(content) }
    }
}

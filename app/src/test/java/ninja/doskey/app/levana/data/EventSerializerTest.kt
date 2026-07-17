package ninja.doskey.app.levana.data

import ninja.doskey.app.levana.data.db.PersonalEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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
}

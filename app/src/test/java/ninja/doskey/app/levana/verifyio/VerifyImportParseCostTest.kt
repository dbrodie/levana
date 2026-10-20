package ninja.doskey.app.levana.verifyio

import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.EventSerializer
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.ui.events.deduplicateImportedEvents
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsing and de-duplication still run on the caller (main) after the bounded
 * read. Measures that work at the 1 MB import limit; prints the timings.
 */
class VerifyImportParseCostTest {

    private fun eventsFilling(limit: Int, encode: (List<PersonalEvent>) -> String): Pair<String, Int> {
        var n = 1000
        while (true) {
            val events = (1..n).map {
                PersonalEvent(title = "Imported event number $it", hebrewDay = 1 + it % 28,
                    hebrewMonth = 1 + it % 12, hebrewYear = 5600 + it % 400, notes = "note $it")
            }
            val text = encode(events)
            if (text.toByteArray().size > limit) {
                var keep = n
                while (encode(events.take(keep)).toByteArray().size > limit) keep -= n / 50
                return encode(events.take(keep)) to keep
            }
            n += n / 2
        }
    }

    private fun ms(block: () -> Unit): Long { val s = System.nanoTime(); block(); return (System.nanoTime() - s) / 1_000_000 }

    @Test
    fun parseCostAtTheImportLimit() {
        val limit = EventFileRepository.MAX_IMPORT_BYTES
        for ((label, enc, dec) in listOf(
            Triple("json", EventSerializer::toJson, EventSerializer::fromJson),
            Triple("csv", EventSerializer::toCsv, EventSerializer::fromCsv)
        )) {
            val (text, count) = eventsFilling(limit, enc)
            assertTrue(text.toByteArray().size <= limit)
            lateinit var parsed: List<PersonalEvent>
            val cold = ms { parsed = dec(text) }
            val dedupe = ms { deduplicateImportedEvents(existing = parsed.take(500), parsed = parsed) }
            repeat(3) { dec(text) }
            val warm = ms { dec(text) }
            println("VERIFY-PARSE $label bytes=${text.toByteArray().size} events=$count coldParseMs=$cold warmParseMs=$warm dedupeMs=$dedupe")
        }
    }
}

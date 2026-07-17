package ninja.doskey.app.levana.data

import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.PersonalEventDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EventSerializer {

    private val json = Json { prettyPrint = true }

    // ── JSON ─────────────────────────────────────────────────────────────────

    fun toJson(events: List<PersonalEvent>): String {
        val dtos = events.map { it.toDto() }
        return json.encodeToString(dtos)
    }

    fun fromJson(content: String): List<PersonalEvent> {
        val dtos = json.decodeFromString<List<PersonalEventDto>>(content)
        return dtos.map { it.toEntity() }
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    private const val CSV_HEADER =
        "title,hebrewDay,hebrewMonth,hebrewYear,notes,useYahrzeitRules"

    fun toCsv(events: List<PersonalEvent>): String {
        val sb = StringBuilder(CSV_HEADER).append('\n')
        for (event in events) {
            sb.append(csvQuote(event.title)).append(',')
            sb.append(event.hebrewDay).append(',')
            sb.append(event.hebrewMonth).append(',')
            sb.append(event.hebrewYear).append(',')
            sb.append(csvQuote(event.notes)).append(',')
            sb.append(event.useYahrzeitRules).append('\n')
        }
        return sb.toString()
    }

    fun fromCsv(content: String): List<PersonalEvent> {
        val rows = parseCsv(content)
        if (rows.isEmpty()) return emptyList()

        val header = rows.first().joinToString(",")
        require(header == CSV_HEADER) {
            "Unexpected CSV header: $header"
        }

        return rows.drop(1).map { parseCsvRow(it) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun PersonalEvent.toDto() = PersonalEventDto(
        title = title,
        hebrewDay = hebrewDay,
        hebrewMonth = hebrewMonth,
        hebrewYear = hebrewYear,
        notes = notes,
        useYahrzeitRules = useYahrzeitRules
    )

    private fun PersonalEventDto.toEntity() = PersonalEvent(
        title = title,
        hebrewDay = hebrewDay,
        hebrewMonth = hebrewMonth,
        hebrewYear = hebrewYear,
        notes = notes,
        useYahrzeitRules = useYahrzeitRules
    )

    /** RFC 4180: wrap in double-quotes if value contains comma, quote, or newline. */
    private fun csvQuote(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            return value
        }
        return "\"${value.replace("\"", "\"\"")}\""
    }

    /**
     * Parses RFC 4180 rows, including quoted fields containing commas, quotes,
     * CRLF line endings, and embedded newlines.
     */
    private fun parseCsv(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun finishRow() {
            row.add(field.toString())
            field.clear()
            if (row.any { it.isNotEmpty() }) {
                rows.add(row.toList())
            }
            row.clear()
        }

        while (i < content.length) {
            val ch = content[i]
            when {
                ch == '"' && inQuotes && i + 1 < content.length && content[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                ch == '"' -> {
                    inQuotes = !inQuotes
                }
                ch == ',' && !inQuotes -> {
                    row.add(field.toString())
                    field.clear()
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++
                    }
                    finishRow()
                }
                else -> field.append(ch)
            }
            i++
        }

        require(!inQuotes) { "Unclosed quoted field" }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            finishRow()
        }

        return rows
    }

    private fun parseCsvRow(fields: List<String>): PersonalEvent {
        require(fields.size == 6) { "Expected 6 fields, got ${fields.size}" }
        val yahrzeitRules = fields[5].trim().lowercase()
        require(yahrzeitRules == "true" || yahrzeitRules == "false") {
            "Invalid useYahrzeitRules value: ${fields[5]}"
        }
        return PersonalEvent(
            title = fields[0],
            hebrewDay = fields[1].toInt(),
            hebrewMonth = fields[2].toInt(),
            hebrewYear = fields[3].toInt(),
            notes = fields[4],
            useYahrzeitRules = yahrzeitRules == "true"
        )
    }
}

package com.levana.app.data

import com.levana.app.data.db.PersonalEvent
import com.levana.app.domain.model.PersonalEventDto
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
        val lines = content.lines()
        if (lines.isEmpty()) return emptyList()

        // Validate header
        val header = lines.first().trim()
        require(header == CSV_HEADER) {
            "Unexpected CSV header: $header"
        }

        return lines.drop(1)
            .filter { it.isNotBlank() }
            .map { parseCsvRow(it) }
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
     * Parse a single RFC 4180 CSV row into a [PersonalEvent].
     * Handles quoted fields containing commas and escaped double-quotes.
     */
    private fun parseCsvRow(line: String): PersonalEvent {
        val fields = splitCsvRow(line)
        require(fields.size == 6) { "Expected 6 fields, got ${fields.size} in: $line" }
        return PersonalEvent(
            title = fields[0],
            hebrewDay = fields[1].toInt(),
            hebrewMonth = fields[2].toInt(),
            hebrewYear = fields[3].toInt(),
            notes = fields[4],
            useYahrzeitRules = fields[5].trim().lowercase() == "true"
        )
    }

    private fun splitCsvRow(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && !inQuotes -> inQuotes = true
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++ // skip second quote
                }
                ch == '"' && inQuotes -> inQuotes = false
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}

package com.levana.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.levana.app.domain.model.EventType

@Entity(tableName = "personal_events")
data class PersonalEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val eventType: EventType,
    val customTitle: String? = null,
    val hebrewDay: Int,
    val hebrewMonth: Int,
    val hebrewYear: Int,
    val notes: String = ""
) {
    val displayTitle: String
        get() = when (eventType) {
            EventType.BIRTHDAY -> "$name's Birthday"
            EventType.YAHRZEIT -> "$name's Yahrzeit"
            EventType.CUSTOM -> customTitle ?: name
        }
}

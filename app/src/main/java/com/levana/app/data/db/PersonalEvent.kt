package com.levana.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_events")
data class PersonalEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val hebrewDay: Int,
    val hebrewMonth: Int,
    val hebrewYear: Int,
    val notes: String = "",
    val useYahrzeitRules: Boolean = false
)

package com.levana.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PersonalEvent::class],
    version = 1,
    exportSchema = false
)
abstract class LevanaDatabase : RoomDatabase() {
    abstract fun personalEventDao(): PersonalEventDao
}

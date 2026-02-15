package com.levana.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PersonalEvent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LevanaDatabase : RoomDatabase() {
    abstract fun personalEventDao(): PersonalEventDao
}

package ninja.doskey.app.levana.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** A later app version whose schema bump ships without a migration from version 1. */
@Database(entities = [PersonalEvent::class], version = 2, exportSchema = false)
abstract class FutureLevanaDatabase : RoomDatabase()

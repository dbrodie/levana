package ninja.doskey.app.levana.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Holds the user's personal events, so upgrades must never lose it.
 *
 * Migration policy: any schema change bumps [VERSION], adds an explicit
 * `Migration(n, n + 1)` to [MIGRATIONS], commits the newly exported
 * `app/schemas/.../<n + 1>.json` (shipped schema files are never edited) and
 * adds a MigrationTestHelper test from every shipped version. There is no
 * destructive fallback: a missing migration fails the open and leaves the
 * file intact.
 */
@Database(
    entities = [PersonalEvent::class],
    version = LevanaDatabase.VERSION,
    exportSchema = true
)
abstract class LevanaDatabase : RoomDatabase() {
    abstract fun personalEventDao(): PersonalEventDao

    companion object {
        const val NAME = "levana-database"
        const val VERSION = 1

        /** Every supported upgrade step. Version 1 is the only shipped schema so far. */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun build(context: Context): LevanaDatabase = withUpgradePolicy(
            Room.databaseBuilder(context, LevanaDatabase::class.java, NAME)
        ).build()

        /** Explicit migrations only; shared with tests that open the file as a future version. */
        internal fun <T : RoomDatabase> withUpgradePolicy(builder: RoomDatabase.Builder<T>) =
            builder.addMigrations(*MIGRATIONS)
    }
}

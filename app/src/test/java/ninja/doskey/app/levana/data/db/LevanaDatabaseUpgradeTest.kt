package ninja.doskey.app.levana.data.db

import android.app.Application
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LevanaDatabaseUpgradeTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevanaDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val app get() = RuntimeEnvironment.getApplication()
    private val dbFile get() = app.getDatabasePath(LevanaDatabase.NAME).path
    private val opened = mutableListOf<RoomDatabase>()

    private val shipped = listOf(
        PersonalEvent(
            id = 1,
            title = "Yahrzeit אבא",
            hebrewDay = 30,
            hebrewMonth = 6,
            hebrewYear = 5760,
            notes = "Kaddish",
            useYahrzeitRules = true
        ),
        PersonalEvent(id = 2, title = "Anniversary", hebrewDay = 14, hebrewMonth = 13, hebrewYear = 0)
    )

    @Before
    fun clean() {
        app.deleteDatabase(LevanaDatabase.NAME)
    }

    @After
    fun close() {
        opened.forEach { it.close() }
    }

    private fun production() = LevanaDatabase.build(app).also { opened += it }

    private fun events(db: LevanaDatabase) =
        runBlocking { db.personalEventDao().getAllOnce() }.sortedBy { it.id }

    /** Writes [shipped] with raw SQL into a database created from the committed version-1 schema. */
    private fun createShippedV1Database() {
        helper.createDatabase(LevanaDatabase.NAME, 1).use { db ->
            for (e in shipped) {
                db.execSQL(
                    "INSERT INTO personal_events (id, title, hebrewDay, hebrewMonth, hebrewYear, " +
                        "notes, useYahrzeitRules) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(
                        e.id,
                        e.title,
                        e.hebrewDay,
                        e.hebrewMonth,
                        e.hebrewYear,
                        e.notes,
                        if (e.useYahrzeitRules) 1 else 0
                    )
                )
            }
        }
    }

    private fun rawSqlite(flags: Int) = SQLiteDatabase.openDatabase(dbFile, null, flags)

    @Test
    fun `shipped v1 events survive opening and reopening with the production database`() {
        createShippedV1Database()
        val added = PersonalEvent(title = "Added", hebrewDay = 1, hebrewMonth = 7, hebrewYear = 5785)

        val first = production()
        assertEquals(shipped, events(first))
        val addedId = runBlocking { first.personalEventDao().insert(added) }
        first.close()

        assertEquals(shipped + added.copy(id = addedId), events(production()))
    }

    @Test
    fun `every shipped version has a committed schema and a migration path to the current version`() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        for (version in 1..LevanaDatabase.VERSION) {
            assets.open("${LevanaDatabase::class.java.name}/$version.json").close()
        }
        for (version in 1 until LevanaDatabase.VERSION) {
            assertTrue(
                "missing Migration($version, ${version + 1})",
                LevanaDatabase.MIGRATIONS.any {
                    it.startVersion == version && it.endVersion == version + 1
                }
            )
        }

        createShippedV1Database()
        helper.runMigrationsAndValidate(
            LevanaDatabase.NAME,
            LevanaDatabase.VERSION,
            true,
            *LevanaDatabase.MIGRATIONS
        ).close()
        assertEquals(shipped, events(production()))
    }

    @Test
    fun `an upgrade without a migration fails instead of wiping events`() {
        createShippedV1Database()

        val future = LevanaDatabase.withUpgradePolicy(
            Room.databaseBuilder(app, FutureLevanaDatabase::class.java, LevanaDatabase.NAME)
        ).build()
        opened += future
        val error = assertThrows(IllegalStateException::class.java) {
            future.openHelper.writableDatabase
        }
        assertTrue(error.message, error.message!!.contains("migration from 1 to 2", ignoreCase = true))
        future.close()

        assertEquals(shipped, events(production()))
    }

    @Test
    fun `a downgrade fails instead of wiping events`() {
        createShippedV1Database()
        rawSqlite(SQLiteDatabase.OPEN_READWRITE).use { it.version = LevanaDatabase.VERSION + 1 }

        val db = production()
        assertThrows(IllegalStateException::class.java) { db.openHelper.writableDatabase }
        db.close()

        val remaining = rawSqlite(SQLiteDatabase.OPEN_READONLY).use {
            DatabaseUtils.queryNumEntries(it, "personal_events")
        }
        assertEquals(shipped.size.toLong(), remaining)
    }
}

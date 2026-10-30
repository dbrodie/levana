package ninja.doskey.app.levana.data

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.domain.agenda.AgendaDay
import ninja.doskey.app.levana.domain.agenda.AgendaItem
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The agenda gathers every source with the existing rules, at the selected city's times. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AgendaRepositoryTest {

    private class FakeDao(private val events: List<PersonalEvent>) : PersonalEventDao {
        override fun getAll(): Flow<List<PersonalEvent>> = flowOf(events)
        override suspend fun getAllOnce(): List<PersonalEvent> = events
        override suspend fun getById(id: Long): PersonalEvent? = events.firstOrNull { it.id == id }
        override suspend fun getByHebrewDate(month: Int, day: Int): List<PersonalEvent> = error("not used")
        override suspend fun insert(event: PersonalEvent): Long = 0
        override suspend fun update(event: PersonalEvent) = Unit
        override suspend fun delete(event: PersonalEvent) = Unit
    }

    private val app get() = RuntimeEnvironment.getApplication()
    private lateinit var savedZone: TimeZone

    private val newYork = Location(40.7128, -74.006, 10.0, "America/New_York", "New York", "United States")
    private val yahrzeit = PersonalEvent(id = 1, title = "Moshe ben Avraham", hebrewDay = 23, hebrewMonth = JewishDate.CHESHVAN, hebrewYear = 5770, useYahrzeitRules = true)
    private val adarEvent = PersonalEvent(id = 2, title = "Purim party", hebrewDay = 14, hebrewMonth = JewishDate.ADAR, hebrewYear = 0)
    private val birthday = ContactBirthday("sarah", "Sarah", null, 30, JewishDate.CHESHVAN, 0)

    private val friday = LocalDate.of(2026, 10, 30)

    @Before
    fun setUp() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        Robolectric.setupContentProvider(AgendaCalendarProvider::class.java, CalendarContract.AUTHORITY)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(savedZone)
    }

    private fun repository(birthdays: List<ContactBirthday> = listOf(birthday)) = AgendaRepository(
        CalendarRepository(),
        ZmanimRepository(),
        PersonalEventRepository(FakeDao(listOf(yahrzeit, adarEvent))),
        object : ContactBirthdayRepository(app) {
            override suspend fun getAll(): List<ContactBirthday> = birthdays
        },
        SystemCalendarRepository(app),
        Dispatchers.Unconfined
    )

    /** The device in New York with Jerusalem selected, in the Diaspora, with the test calendar chosen. */
    private fun prefs(selectJerusalem: Boolean = true, inIsrael: Boolean = false) = UserPreferences(
        currentLocation = newYork,
        savedLocations = listOf(SavedLocation("jlm", Location.JERUSALEM)),
        activeLocationId = "jlm".takeIf { selectJerusalem },
        isInIsrael = inIsrael,
        selectedCalendarIds = setOf(CAL_ID)
    )

    private fun load(from: LocalDate, to: LocalDate, prefs: UserPreferences = prefs()) =
        runBlocking { repository().load(from, to, prefs) }

    private fun List<AgendaDay>.on(date: LocalDate) = first { it.date == date }.items

    @Test
    fun `candle lighting and havdalah are the selected city's`() {
        val zmanim = ZmanimRepository()
        val jerusalem = load(friday, friday.plusDays(1))
        val candles = jerusalem.days.on(friday).filterIsInstance<AgendaItem.CandleLighting>().single()
        assertEquals(zmanim.getShabbatInfo(friday, Location.JERUSALEM).candleLightingTime, candles.time)
        val shabbat = jerusalem.days.on(friday.plusDays(1)).filterIsInstance<AgendaItem.Shabbat>().single()
        assertEquals("Vayera", shabbat.parsha)
        assertEquals(zmanim.getShabbatInfo(friday.plusDays(1), Location.JERUSALEM).havdalahTime, shabbat.havdalah)

        val nyCandles = load(friday, friday, prefs(selectJerusalem = false)).days.on(friday)
            .filterIsInstance<AgendaItem.CandleLighting>().single()
        assertNotEquals(candles.time, nyCandles.time)
    }

    @Test
    fun `events and birthdays recur by their Hebrew dates`() {
        val days = load(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 12, 31)).days
        // 23 Cheshvan of 5787 and 5788
        assertEquals(
            listOf(LocalDate.of(2026, 11, 3), JewishDate(5788, JewishDate.CHESHVAN, 23).localDate),
            days.filter { day -> day.items.any { it is AgendaItem.PersonalEventItem && it.isYahrzeit } }.map { it.date }
        )
        // 30 Cheshvan: kept where Cheshvan has 30 days (5787), else 1 Kislev
        val birthdayDays = days.filter { day -> day.items.any { it is AgendaItem.BirthdayItem } }
        assertEquals(LocalDate.of(2026, 11, 10), birthdayDays.first().date)
        val in5788 = if (HebrewDateMatcher.daysInMonth(5788, JewishDate.CHESHVAN) == 30) {
            JewishDate(5788, JewishDate.CHESHVAN, 30)
        } else {
            JewishDate(5788, JewishDate.KISLEV, 1)
        }
        assertEquals(in5788.localDate, birthdayDays.last().date)
        // 5787 is a leap year: an unknown-year Adar falls in Adar II
        val purimParty = days.first { day -> day.items.any { (it as? AgendaItem.PersonalEventItem)?.event == adarEvent } }
        assertEquals(JewishDate(5787, JewishDate.ADAR_II, 14).localDate, purimParty.date)
    }

    @Test
    fun `holidays and Rosh Chodesh follow Israel or the Diaspora`() {
        val roshChodesh = load(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11)).days
        assertTrue(roshChodesh.all { day -> day.items.any { (it as? AgendaItem.HolidayItem)?.holiday?.name == "Rosh Chodesh" } })

        val secondDayShavuot = JewishDate(5787, JewishDate.SIVAN, 7).localDate
        fun shavuotOn(inIsrael: Boolean) = load(secondDayShavuot, secondDayShavuot, prefs(inIsrael = inIsrael)).days
            .flatMap { it.items }.any { (it as? AgendaItem.HolidayItem)?.holiday?.name == "Shavuot" }
        assertTrue(shavuotOn(inIsrael = false))
        assertFalse(shavuotOn(inIsrael = true))
    }

    @Test
    fun `without the calendar permission device events are left out and reported`() {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)
        val range = load(friday, friday)
        assertFalse(range.calendarPermission)
        assertTrue(range.days.on(friday).none { it is AgendaItem.DeviceEventItem })
    }

    @Test
    fun `selected device events are listed with their place, in time order`() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        val range = load(friday, friday)
        assertTrue(range.calendarPermission)
        val items = range.days.on(friday)
        val dinner = items.filterIsInstance<AgendaItem.DeviceEventItem>().single()
        assertEquals("Shabbat dinner at the Levis", dinner.event.title)
        assertEquals("Rechavia", dinner.event.location)
        assertEquals(LocalTime.of(19, 0), dinner.time)
        // Candle lighting (afternoon) comes before the 19:00 dinner
        assertTrue(items.indexOfFirst { it is AgendaItem.CandleLighting } < items.indexOf(dinner))

        // A calendar that isn't selected is not shown
        val none = runBlocking { repository().load(friday, friday, prefs().copy(selectedCalendarIds = setOf(99L))) }
        assertTrue(none.days.on(friday).none { it is AgendaItem.DeviceEventItem })
    }

    /** One selected calendar with a 19:00 dinner on Friday 30 October 2026, New York time. */
    class AgendaCalendarProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val start = LocalDate.of(2026, 10, 30).atTime(19, 0).atZone(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
            val values = mapOf(
                CalendarContract.Instances.EVENT_ID to 5L,
                CalendarContract.Instances.TITLE to "Shabbat dinner at the Levis",
                CalendarContract.Instances.BEGIN to start,
                CalendarContract.Instances.END to start + 2 * 3_600_000L,
                CalendarContract.Instances.ALL_DAY to 0,
                CalendarContract.Instances.CALENDAR_ID to CAL_ID,
                CalendarContract.Instances.CALENDAR_COLOR to 0xFF336699.toInt(),
                CalendarContract.Instances.EVENT_LOCATION to "Rechavia"
            )
            val columns = projection!!
            return MatrixCursor(columns).apply { addRow(columns.map { values[it] }) }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }

    private companion object {
        const val CAL_ID = 3L
    }
}

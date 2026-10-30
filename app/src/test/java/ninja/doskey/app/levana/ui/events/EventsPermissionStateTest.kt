package ninja.doskey.app.levana.ui.events

import android.Manifest
import android.app.Application
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.domain.model.ContactBirthday
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Add on the Birthdays tab opens the editor when contacts are allowed. The permission is known
 * before the contacts query returns, so a tap during a slow query does not ask for it again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EventsPermissionStateTest {

    private object NoEvents : PersonalEventDao {
        override fun getAll(): Flow<List<PersonalEvent>> = flowOf(emptyList())
        override suspend fun getAllOnce(): List<PersonalEvent> = emptyList()
        override suspend fun getById(id: Long): PersonalEvent? = null
        override suspend fun getByHebrewDate(month: Int, day: Int): List<PersonalEvent> = emptyList()
        override suspend fun insert(event: PersonalEvent): Long = 0
        override suspend fun update(event: PersonalEvent) = Unit
        override suspend fun delete(event: PersonalEvent) = Unit
    }

    @Test
    fun `the contacts permission is known while the birthdays are still loading`() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        val neverReturns = CompletableDeferred<List<ContactBirthday>>()
        val birthdays = object : ContactBirthdayRepository(app) {
            override suspend fun getAll(): List<ContactBirthday> = neverReturns.await()
        }
        val vm = EventsViewModel(PersonalEventRepository(NoEvents), birthdays, EventFileRepository(app))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(vm.state.value.hasContactsPermission)
    }
}

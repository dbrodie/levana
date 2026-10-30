package ninja.doskey.app.levana.screenshots

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.PickedContact
import ninja.doskey.app.levana.domain.model.ContactBirthday
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Adding a birthday for a contact that already has one switches to editing it (TASK-51). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class ContactBirthdayScreenshotTest(variant: Variant) : ScreenshotTest(variant) {

    override fun testModules(): List<Module> = listOf(
        module {
            single<ContactBirthdayRepository> {
                object : ContactBirthdayRepository(androidContext()) {
                    override suspend fun resolveContact(contactUri: Uri) = PickedContact(KEY, NAME, null)
                    override suspend fun getBirthdayForContact(contactLookupKey: String) =
                        ContactBirthday(KEY, NAME, null, 8, JewishDate.KISLEV, 5750)
                }
            }
        }
    )

    @Test
    fun pickingAContactWithABirthdayEditsIt() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        awaitText(string(R.string.events_tab_birthdays))
        compose.onNodeWithContentDescription(string(R.string.events_add)).performClick()
        awaitText(string(R.string.design_add_birthday))

        // The editor opened the contact picker; it returns the contact
        onActivity { activity ->
            val shadow = shadowOf(activity)
            val picker = checkNotNull(shadow.nextStartedActivityForResult) { "contact picker not launched" }
            shadow.receiveResult(picker.intent, Activity.RESULT_OK, Intent().setData(Uri.parse(CONTACT_URI)))
        }
        awaitText(string(R.string.design_edit_birthday))
        await(hasText(string(R.string.event_update)))
        capture("app_contact_birthday_existing")
    }

    companion object {
        private const val KEY = "sarah"
        private const val NAME = "Sarah Cohen"
        private const val CONTACT_URI = "content://com.android.contacts/contacts/lookup/sarah/1"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}

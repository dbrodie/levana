package ninja.doskey.app.levana.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationSwitchTest {

    @Test
    fun grantedEnablesOnlyThePendingSwitch() {
        assertEquals(
            SettingsIntent.SetNotifyCandleLighting(true),
            notificationPermissionResultIntent(NotificationSwitch.CandleLighting.name, granted = true)
        )
        assertEquals(
            SettingsIntent.SetNotifyFasts(true),
            notificationPermissionResultIntent(NotificationSwitch.Fasts.name, granted = true)
        )
    }

    @Test
    fun deniedEnablesNothing() {
        for (switch in NotificationSwitch.entries) {
            assertNull(notificationPermissionResultIntent(switch.name, granted = false))
        }
    }

    @Test
    fun nothingPendingOrUnknownEnablesNothing() {
        assertNull(notificationPermissionResultIntent(null, granted = true))
        assertNull(notificationPermissionResultIntent("NotASwitch", granted = true))
    }

    @Test
    fun everySwitchMapsToItsIntent() {
        val expected = mapOf(
            NotificationSwitch.CandleLighting to SettingsIntent.SetNotifyCandleLighting(true),
            NotificationSwitch.Holidays to SettingsIntent.SetNotifyHolidays(true),
            NotificationSwitch.Fasts to SettingsIntent.SetNotifyFasts(true),
            NotificationSwitch.PersonalEvents to SettingsIntent.SetNotifyPersonalEvents(true),
            NotificationSwitch.Omer to SettingsIntent.SetNotifyOmer(true),
            NotificationSwitch.OmerTzait to SettingsIntent.SetNotifyOmerTzait(true),
            NotificationSwitch.OmerMorning to SettingsIntent.SetNotifyOmerMorning(true)
        )
        assertEquals(NotificationSwitch.entries.toSet(), expected.keys)
        for ((switch, intent) in expected) {
            assertEquals(intent, notificationPermissionResultIntent(switch.name, granted = true))
            assertEquals(intent, switch.intent(true))
        }
        assertEquals(SettingsIntent.SetNotifyOmer(false), NotificationSwitch.Omer.intent(false))
    }
}

package ninja.doskey.app.levana.ui.settings

/** Every switch in Settings → Notifications that needs POST_NOTIFICATIONS to be useful. */
enum class NotificationSwitch {
    CandleLighting,
    Holidays,
    Fasts,
    PersonalEvents,
    Omer,
    OmerTzait,
    OmerMorning;

    fun intent(enabled: Boolean): SettingsIntent = when (this) {
        CandleLighting -> SettingsIntent.SetNotifyCandleLighting(enabled)
        Holidays -> SettingsIntent.SetNotifyHolidays(enabled)
        Fasts -> SettingsIntent.SetNotifyFasts(enabled)
        PersonalEvents -> SettingsIntent.SetNotifyPersonalEvents(enabled)
        Omer -> SettingsIntent.SetNotifyOmer(enabled)
        OmerTzait -> SettingsIntent.SetNotifyOmerTzait(enabled)
        OmerMorning -> SettingsIntent.SetNotifyOmerMorning(enabled)
    }
}

/**
 * Decides what to send once the notification permission request returns.
 * [pendingName] is the [NotificationSwitch.name] the user tried to turn on.
 * Returns the intent that turns that switch on, or null if denied or nothing is pending.
 */
fun notificationPermissionResultIntent(pendingName: String?, granted: Boolean): SettingsIntent? {
    if (!granted || pendingName == null) return null
    val switch = NotificationSwitch.entries.firstOrNull { it.name == pendingName } ?: return null
    return switch.intent(true)
}

package ninja.doskey.app.levana.domain.model

data class UserPreferences(
    /**
     * The device position: the last current-location fix, or the device time zone's fallback
     * city without one. Read it through [ninja.doskey.app.levana.device.DeviceNow.position].
     */
    val currentLocation: Location = Location.JERUSALEM,
    val savedLocations: List<SavedLocation> = emptyList(),
    val activeLocationId: String? = null,
    val candleLightingOffset: Double = 18.0,
    val isInIsrael: Boolean = false,
    val showModernIsraeliHolidays: Boolean = true,
    val calendarHebrewMode: Boolean = false,
    val dynamicHolidayTheme: Boolean = true,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val showDeveloperSettings: Boolean = true,
    /**
     * TEST ONLY — the developer override: whole days the device clock is shifted by, 0 when
     * off. Applied solely by [ninja.doskey.app.levana.device.DeviceContext].
     */
    val devDayShift: Long = 0,
    val devForceHolidayTheme: String? = null,
    val notifyCandleLighting: Boolean = false,
    val candleLightingNotifyMode: String = "morning",
    val candleLightingMorningTime: Int = 480,
    val candleLightingHoursBefore: Int = 2,
    val notifyHolidays: Boolean = false,
    val holidayNotifyDaysBefore: Int = 1,
    val notifyFasts: Boolean = false,
    val notifyPersonalEvents: Boolean = false,
    val notifyOmer: Boolean = false,
    val notifyOmerTzait: Boolean = true,
    val notifyOmerMorning: Boolean = false,
    val notifyOmerMorningTime: Int = 420,
    val selectedZmanim: Set<String> = setOf("Sunrise", "Sunset", "Nightfall"),
    val yahrzeitRules: YahrzeitRules = YahrzeitRules()
)

/**
 * The selected (display) city: the saved city chosen in the sidebar, or the current location
 * when none is. It is a display lens only — read it for the Zmanim screen, the day panel's
 * times and the city pickers, never for dates, notifications or Israel/Diaspora, which follow
 * the device ([ninja.doskey.app.levana.device.DeviceContext]).
 */
val UserPreferences.displayCity: Location
    get() = savedLocations.find { it.id == activeLocationId }?.location ?: currentLocation

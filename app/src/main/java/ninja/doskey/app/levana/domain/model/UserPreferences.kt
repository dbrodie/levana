package ninja.doskey.app.levana.domain.model

data class UserPreferences(
    /**
     * The device position: the last current-location fix, always in the device's current time
     * zone, or the device time zone's fallback city without one. Read it through
     * [ninja.doskey.app.levana.device.DeviceNow.position].
     */
    val currentLocation: Location = Location.JERUSALEM,
    /** Whether [currentLocation] is a real location fix rather than the zone's fallback city. */
    val currentLocationIsFix: Boolean = false,
    val savedLocations: List<SavedLocation> = emptyList(),
    val activeLocationId: String? = null,
    val candleLightingOffset: Double = 18.0,
    /** The Israel / Diaspora setting; [isInIsrael] is its result for the device. */
    val israelMode: IsraelMode = IsraelMode.AUTO,
    /** Whether Israel's calendar applies, resolved by [ninja.doskey.app.levana.device.resolveIsInIsrael]. */
    val isInIsrael: Boolean = false,
    val showModernIsraeliHolidays: Boolean = true,
    val calendarHebrewMode: Boolean = false,
    val dynamicHolidayTheme: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You: wallpaper colors, when the device supports them and no holiday theme applies. */
    val materialYou: Boolean = false,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val showDeveloperSettings: Boolean = true,
    /**
     * TEST ONLY — the developer override: the fixed date the device acts as if it is on, null
     * when off. Applied solely by [ninja.doskey.app.levana.device.DeviceContext].
     */
    val devDateOverride: java.time.LocalDate? = null,
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

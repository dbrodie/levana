package ninja.doskey.app.levana.domain.model

/** Settings → Theme. [SYSTEM] follows the device's light/dark setting. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    /** Whether the app is dark while the device is dark when [systemDark]. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

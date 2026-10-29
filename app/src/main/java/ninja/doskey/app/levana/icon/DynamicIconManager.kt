package ninja.doskey.app.levana.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.util.GregorianCalendar

/**
 * Switches the active launcher icon alias to reflect the current Hebrew day of the month.
 *
 * Android does not support changing launcher icon bitmaps at runtime. Instead the manifest
 * declares 30 <activity-alias> entries (MainActivityDay01..MainActivityDay30), each carrying
 * its own pre-baked icon. This object enables the correct alias and disables all others.
 *
 * The default component (LauncherActivity) carries the fallback icon (א / day 1) and starts
 * enabled; update() replaces it with the correct day alias on first run. MainActivity is never
 * toggled: Android finishes activities started through a component when it is disabled, so
 * toggling the UI activity would close the app.
 *
 * Call sites: MainActivity (onResume), BootReceiver, DateChangeReceiver.
 */
object DynamicIconManager {

    /**
     * Older versions toggled MainActivity itself, so upgraded installs may have it saved as
     * disabled — which would make every launch fail. Call before anything starts MainActivity.
     */
    fun restoreMainActivity(context: Context) {
        val mainActivity = ComponentName(context.packageName, "${context.packageName}.MainActivity")
        val pm = context.packageManager
        if (pm.getComponentEnabledSetting(mainActivity) !=
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        ) {
            pm.setComponentEnabledSetting(
                mainActivity,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    /** Shows the Hebrew day of the device's [today] ([ninja.doskey.app.levana.device.DeviceContext]). */
    fun update(context: Context, today: LocalDate) {
        val jc = JewishCalendar(
            GregorianCalendar(today.year, today.monthValue - 1, today.dayOfMonth)
        )
        val day = jc.jewishDayOfMonth  // 1..30

        val pkg = context.packageName
        val pm = context.packageManager

        val defaultComponent = ComponentName(pkg, "$pkg.LauncherActivity")
        val dayAliases = (1..30).map { d ->
            ComponentName(pkg, "$pkg.MainActivityDay${d.toString().padStart(2, '0')}")
        }
        val target = dayAliases[day - 1]

        // Enable the matching alias; disable the default entry and all other aliases.
        (listOf(defaultComponent) + dayAliases).forEach { component ->
            val desired = if (component == target)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(component, desired, PackageManager.DONT_KILL_APP)
        }
    }
}

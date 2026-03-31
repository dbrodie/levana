package com.levana.app.icon

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
 * The default component (MainActivity) carries the fallback icon (א / day 1) and starts
 * enabled; update() replaces it with the correct day alias on first run.
 *
 * Call sites: MainActivity (onResume), BootReceiver, DateChangeReceiver.
 */
object DynamicIconManager {

    fun update(context: Context, devDateOverride: LocalDate? = null) {
        val today = devDateOverride ?: LocalDate.now()
        val jc = JewishCalendar(
            GregorianCalendar(today.year, today.monthValue - 1, today.dayOfMonth)
        )
        val day = jc.jewishDayOfMonth  // 1..30

        val pkg = context.packageName
        val pm = context.packageManager

        val defaultComponent = ComponentName(pkg, "$pkg.MainActivity")
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

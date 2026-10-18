package ninja.doskey.app.levana.icon

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.util.GregorianCalendar
import ninja.doskey.app.levana.LauncherActivity
import ninja.doskey.app.levana.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DynamicIconManagerTest {

    @Test
    fun updatesLauncherComponentsWithoutDisablingMainActivity() {
        val context = RuntimeEnvironment.getApplication()
        val packageName = context.packageName
        val date = LocalDate.of(2026, 7, 21)
        val expectedDay = JewishCalendar(
            GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        ).jewishDayOfMonth
        val expectedAlias = ComponentName(
            packageName,
            "$packageName.MainActivityDay${expectedDay.toString().padStart(2, '0')}"
        )

        DynamicIconManager.update(context, date)

        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            context.packageManager.getComponentEnabledSetting(
                ComponentName(context, MainActivity::class.java)
            )
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            context.packageManager.getComponentEnabledSetting(
                ComponentName(context, LauncherActivity::class.java)
            )
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            context.packageManager.getComponentEnabledSetting(expectedAlias)
        )
    }

    @Test
    fun restoresMainActivityDisabledByOlderVersions() {
        val context = RuntimeEnvironment.getApplication()
        val mainActivity = ComponentName(context, MainActivity::class.java)
        context.packageManager.setComponentEnabledSetting(
            mainActivity,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        DynamicIconManager.restoreMainActivity(context)

        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            context.packageManager.getComponentEnabledSetting(mainActivity)
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun datedAliasesTargetLauncherActivity() {
        val context = RuntimeEnvironment.getApplication()
        val packageName = context.packageName
        val alias = ComponentName(packageName, "$packageName.MainActivityDay01")

        val info = context.packageManager.getActivityInfo(
            alias,
            PackageManager.MATCH_DISABLED_COMPONENTS
        )

        assertEquals(LauncherActivity::class.java.name, info.targetActivity)
    }
}

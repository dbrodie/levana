package ninja.doskey.app.levana.notifications

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration

/**
 * This context with the app language applied (Settings → App language).
 *
 * The setting is the per-app locale in [LocaleManager.applicationLocales]: `iw` for Hebrew,
 * empty for System default. Background work (workers, receivers) may run with a context that
 * does not carry it, so resolve it explicitly. With System default the context is returned
 * unchanged and the device language decides.
 */
fun Context.withAppLocale(): Context {
    val appLocales = getSystemService(LocaleManager::class.java)?.applicationLocales
    if (appLocales == null || appLocales.isEmpty) return this
    if (resources.configuration.locales == appLocales) return this
    val config = Configuration(resources.configuration).apply { setLocales(appLocales) }
    return createConfigurationContext(config)
}

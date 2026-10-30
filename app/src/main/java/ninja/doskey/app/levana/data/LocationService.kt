package ninja.doskey.app.levana.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.device.DeviceContext
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationService(
    private val context: Context,
    private val geocoder: ReverseGeocoder = AndroidReverseGeocoder(context),
    private val fixer: LocationFixer = LocationFixer(PlatformLocationSource(context))
) {

    fun hasPermission(): Boolean =
        PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    /**
     * A recent or fresh fix, or null when permission is missing, no provider gives one, or it
     * takes over 15s (see [LocationFixer]). The fix is named off the main thread; a failed
     * lookup keeps the coordinates under [FALLBACK_PLACE_NAME].
     */
    suspend fun getCurrentLocationOrNull(): Location? {
        if (!hasPermission()) return null
        val fix = fixer.fixOrNull() ?: return null
        return Location(
            latitude = fix.latitude,
            longitude = fix.longitude,
            elevation = fix.altitude,
            // A fix is in the device zone (TASK-5 case 10); it is re-zoned on every read (TASK-46)
            timezoneId = DeviceContext.realZone().id,
            name = resolvePlaceName(geocoder, fix.latitude, fix.longitude),
            country = ""
        )
    }

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

/** [LocationSource] over the platform [LocationManager]. */
@SuppressLint("MissingPermission")
class PlatformLocationSource(context: Context) : LocationSource {

    private val manager = context.getSystemService(LocationManager::class.java)

    override fun providers(): List<String> = manager.allProviders

    override fun isEnabled(provider: String): Boolean = manager.isProviderEnabled(provider)

    override fun lastKnown(provider: String): Fix? = manager.getLastKnownLocation(provider)?.toFix()

    override fun elapsedRealtimeNanos(): Long = SystemClock.elapsedRealtimeNanos()

    override suspend fun current(provider: String): Fix? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        manager.getCurrentLocation(provider, signal, Runnable::run) { loc -> cont.resume(loc?.toFix()) }
    }

    private fun android.location.Location.toFix() =
        Fix(latitude, longitude, altitude, elapsedRealtimeNanos)
}

package ninja.doskey.app.levana.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.device.DeviceContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class LocationService(
    private val context: Context,
    private val geocoder: ReverseGeocoder = AndroidReverseGeocoder(context)
) {

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    /**
     * A fresh fix, or null when permission is missing, the fix fails, or it takes over 15s.
     * The fix is named off the main thread; a failed lookup keeps the coordinates
     * under [FALLBACK_PLACE_NAME].
     */
    suspend fun getCurrentLocationOrNull(): Location? {
        if (!hasPermission()) return null
        val fix = try {
            withTimeoutOrNull(15_000L) { getCurrentFix() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null
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

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentFix(): android.location.Location = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                cont.resume(loc)
            } else {
                cont.resumeWithException(
                    Exception("Could not determine location")
                )
            }
        }.addOnFailureListener { e ->
            cont.resumeWithException(e)
        }
    }

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

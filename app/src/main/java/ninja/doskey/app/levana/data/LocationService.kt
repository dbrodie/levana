package ninja.doskey.app.levana.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import ninja.doskey.app.levana.domain.model.Location
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class LocationService(private val context: Context) {

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    /** A fresh fix, or null when permission is missing, the fix fails, or it takes over 15s. */
    suspend fun getCurrentLocationOrNull(): Location? {
        if (!hasPermission()) return null
        return try {
            withTimeoutOrNull(15_000L) { getCurrentLocation() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                val name = reverseGeocode(loc.latitude, loc.longitude)
                cont.resume(
                    Location(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        elevation = loc.altitude,
                        timezoneId = TimeZone.getDefault().id,
                        name = name,
                        country = ""
                    )
                )
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

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    ?: "Current Location"
            } else {
                "Current Location"
            }
        } catch (_: Exception) {
            "Current Location"
        }
    }
}

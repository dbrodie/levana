package com.levana.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.levana.app.domain.model.Location
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationService(private val context: Context) {

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { cont ->
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

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    ?: "%.4f, %.4f".format(lat, lon)
            } else {
                "%.4f, %.4f".format(lat, lon)
            }
        } catch (_: Exception) {
            "%.4f, %.4f".format(lat, lon)
        }
    }
}

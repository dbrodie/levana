package ninja.doskey.app.levana.data

import android.app.Application
import android.location.LocationManager
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.device.DeviceContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLocationManager

/** [LocationService] over the platform [LocationManager], with no Play services. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LocationServiceTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(LocationManager::class.java)
    private val shadow: ShadowLocationManager = shadowOf(manager)
    private val service = LocationService(app, geocoder = { _, _ -> "Ukiah" })

    @Before
    fun setUp() {
        shadow.setLocationEnabled(true)
        LocationFixer.PROVIDER_ORDER.forEach { shadow.setProviderEnabled(it, true) }
    }

    private fun location(provider: String, ageMillis: Long = 0) = android.location.Location(provider).apply {
        latitude = 39.15
        longitude = -123.21
        altitude = 190.0
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() - ageMillis * 1_000_000
    }

    @Test
    fun `no permission gives null`() = runBlocking {
        shadowOf(app).denyPermissions(*LocationService.PERMISSIONS)
        shadow.setLastKnownLocation(LocationManager.GPS_PROVIDER, location(LocationManager.GPS_PROVIDER))
        assertNull(service.getCurrentLocationOrNull())
    }

    @Test
    fun `recent last known fix is named and stamped with the device zone`() = runBlocking {
        shadowOf(app).grantPermissions(*LocationService.PERMISSIONS)
        shadow.setLastKnownLocation(LocationManager.NETWORK_PROVIDER, location(LocationManager.NETWORK_PROVIDER))
        val result = service.getCurrentLocationOrNull()!!
        assertEquals(39.15, result.latitude, 1e-9)
        assertEquals(-123.21, result.longitude, 1e-9)
        assertEquals(190.0, result.elevation, 1e-9)
        assertEquals("Ukiah", result.name)
        assertEquals(DeviceContext.realZone().id, result.timezoneId)
    }

    @Test
    fun `fresh fix comes from the preferred provider`() = runBlocking {
        shadowOf(app).grantPermissions(*LocationService.PERMISSIONS)
        val provider = LocationFixer(PlatformLocationSource(app)).usableProviders().first()
        assertEquals(LocationManager.FUSED_PROVIDER, provider)
        val request = async(start = CoroutineStart.UNDISPATCHED) { service.getCurrentLocationOrNull() }
        shadow.simulateLocation(provider, location(provider))
        assertEquals(39.15, request.await()!!.latitude, 1e-9)
    }
}

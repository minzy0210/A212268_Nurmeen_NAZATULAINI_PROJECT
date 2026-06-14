package com.example.a212268_nazatulaini_lab1

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.math.*

data class UserLocation(val latitude: Double, val longitude: Double)
object DistanceUtils {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun formatDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val metres = EARTH_RADIUS_M * c
        return if (metres < 1000) "${metres.toInt()}m"
        else "%.1fkm".format(metres / 1000.0)
    }
}
class LocationHelper(private val context: Context) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocation? {
        return try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let { UserLocation(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reverse-geocodes the given coordinates into a human-readable place name
     * (e.g. "Bangi, Selangor" or "Kuala Lumpur").
     * Falls back to the raw coordinates if geocoding fails or returns nothing.
     */
    suspend fun getPlaceName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())

            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+ uses an async callback API
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        cont.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }

            if (address != null) {
                // Prefer a locality (city/town), fall back to sub-admin area or admin area
                val locality = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                val area = address.adminArea

                when {
                    locality != null && area != null && locality != area -> "$locality, $area"
                    locality != null -> locality
                    area != null -> area
                    else -> "%.5f, %.5f".format(latitude, longitude)
                }
            } else {
                "%.5f, %.5f".format(latitude, longitude)
            }
        } catch (e: Exception) {
            // Geocoder can throw if no network/geocoder service available
            "%.5f, %.5f".format(latitude, longitude)
        }
    }

    fun formatDistance(
        userLat: Double, userLon: Double,
        targetLat: Double, targetLon: Double
    ): String {
        val metres = haversineMetres(userLat, userLon, targetLat, targetLon)
        return if (metres < 1000) "${metres.toInt()}m"
        else "%.1fkm".format(metres / 1000.0)
    }

    fun haversineMetres(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
package com.example.a212268_nazatulaini_lab1

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.math.*

data class UserLocation(val latitude: Double, val longitude: Double)

class LocationHelper(private val context: Context) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Requests a FRESH, high-accuracy GPS fix.
     * Uses getCurrentLocation() which actively polls the sensor,
     * unlike lastLocation which may return a stale cached value.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocation? {
        return try {
            val cancellationTokenSource =
                com.google.android.gms.tasks.CancellationTokenSource()

            val location = fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                .await()

            location?.let { UserLocation(it.latitude, it.longitude) }
        } catch (e: Exception) {
            // Fall back to last known fix if fresh request fails
            try {
                val last = fusedLocationClient.lastLocation.await()
                last?.let { UserLocation(it.latitude, it.longitude) }
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Reverse-geocodes [latitude]/[longitude] to a readable place name
     * e.g. "Bangi, Selangor".
     */
    suspend fun getPlaceName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+: async listener-based API
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val name = addresses.firstOrNull()?.let { buildPlaceName(it) }
                            ?: "%.5f, %.5f".format(latitude, longitude)
                        cont.resume(name)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { buildPlaceName(it) }
                    ?: "%.5f, %.5f".format(latitude, longitude)
            }
        } catch (e: Exception) {
            "%.5f, %.5f".format(latitude, longitude)
        }
    }

    /** Builds a concise "Suburb, City" label from an Address. */
    private fun buildPlaceName(address: android.location.Address): String {
        val parts = listOfNotNull(
            address.subLocality ?: address.thoroughfare,
            address.locality ?: address.subAdminArea,
            address.adminArea
        ).filter { it.isNotBlank() }.distinct()
        return if (parts.isNotEmpty()) parts.take(2).joinToString(", ")
        else address.countryName ?: "Unknown location"
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
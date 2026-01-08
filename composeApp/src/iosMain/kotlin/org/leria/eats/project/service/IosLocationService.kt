package org.leria.eats.project.service


import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosLocationService : LocationService {

    private val locationManager = CLLocationManager()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCurrentAddress(): String? {
        return suspendCoroutine { continuation ->
            locationManager.requestWhenInUseAuthorization()

            val location = locationManager.location

            if (location == null) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            val geocoder = CLGeocoder()

            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                if (error != null || placemarks == null) {
                    continuation.resume(null)
                } else {
                    val placemark = placemarks.firstOrNull() as? CLPlacemark
                    if (placemark != null) {
                        val street = placemark.thoroughfare ?: ""
                        val number = placemark.subThoroughfare ?: ""
                        val city = placemark.locality ?: ""

                        val fullAddress = if (street.isNotEmpty()) "$street, $number - $city" else city
                        continuation.resume(fullAddress)
                    } else {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
}
package org.leria.eats.project.service

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.*
import platform.Foundation.NSThread
import platform.darwin.*
import platform.darwin.NSObject
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosLocationService : LocationService {

    private val locationManager = CLLocationManager()

    // ── Permission helper ──────────────────────────────────────────────────────
    /**
     * Requests location permission and waits up to 5 seconds for the user's
     * response before continuing. Returns true if authorized.
     */
    @OptIn(ExperimentalAtomicApi::class)
    private fun requestAndWaitForLocationPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        // kCLAuthorizationStatusAuthorizedWhenInUse = 4, kCLAuthorizationStatusAuthorizedAlways = 3
        if (status == 3 || status == 4) return true
        // kCLAuthorizationStatusDenied = 2, kCLAuthorizationStatusRestricted = 1
        if (status == 1 || status == 2) return false

        // Not determined (0) — show the dialog and wait
        val done = AtomicInt(0)

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(
                manager: CLLocationManager,
                didChangeAuthorizationStatus: CLAuthorizationStatus
            ) {
                if (didChangeAuthorizationStatus != 0) { // 0 = NotDetermined
                    done.store(1)
                }
            }
        }

        locationManager.delegate = delegate
        locationManager.requestWhenInUseAuthorization()

        var waited = 0
        while (done.load() == 0 && waited < 500) {
            NSThread.sleepForTimeInterval(0.01)
            waited++
        }

        val newStatus = CLLocationManager.authorizationStatus()
        return newStatus == 3 || newStatus == 4
    }

    // ── getCurrentAddress ──────────────────────────────────────────────────────
    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun getCurrentAddress(): String? {
        return suspendCoroutine { continuation ->
            val authorized = requestAndWaitForLocationPermission()
            if (!authorized) {
                continuation.resume(null)
                return@suspendCoroutine
            }

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

    // ── getAddressFromCoordinates ──────────────────────────────────────────────
    // IMPORTANT: This function must be called from a background thread
    // (e.g. via withContext(Dispatchers.IO) in a coroutine).
    // It dispatches the CLGeocoder call onto the main thread (required by iOS)
    // and spin-waits on the calling (background) thread for the result.
    @OptIn(ExperimentalAtomicApi::class)
    override fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        var result: String? = null
        val done = AtomicInt(0)

        val location = CLLocation(latitude = latitude, longitude = longitude)
        val geocoder = CLGeocoder()

        // CLGeocoder must be called on the main thread on iOS.
        // Since CartScreen calls this inside withContext(Dispatchers.IO),
        // the spin-wait below runs on a background thread and is safe.
        val mainQueue = dispatch_get_main_queue()
        dispatch_async(mainQueue) {
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                if (error == null && placemarks != null) {
                    val placemark = placemarks.firstOrNull() as? CLPlacemark
                    if (placemark != null) {
                        val street = placemark.thoroughfare ?: ""
                        val number = placemark.subThoroughfare ?: ""
                        val city = placemark.locality ?: ""
                        val postalCode = placemark.postalCode ?: ""
                        result = buildString {
                            if (street.isNotEmpty()) {
                                append(street)
                                if (number.isNotEmpty()) append(", $number")
                            }
                            if (city.isNotEmpty()) {
                                if (isNotEmpty()) append(" - ")
                                append(city)
                            }
                            if (postalCode.isNotEmpty()) {
                                if (isNotEmpty()) append(", ")
                                append(postalCode)
                            }
                        }.ifEmpty { "$latitude, $longitude" }
                    } else {
                        result = "$latitude, $longitude"
                    }
                } else {
                    result = "$latitude, $longitude"
                }
                done.store(1)
            }
        }

        // Spin-wait on the calling background thread (max ~5 seconds).
        // The main thread is free to deliver the geocoder callback.
        var waited = 0
        while (done.load() == 0 && waited < 500) {
            NSThread.sleepForTimeInterval(0.01)
            waited++
        }

        return result
    }

    // ── getCurrentCoordinates ──────────────────────────────────────────────────
    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun getCurrentCoordinates(): Pair<Double, Double>? {
        return try {
            val authorized = requestAndWaitForLocationPermission()
            if (!authorized) return null
            val location = locationManager.location ?: return null
            val lat = location.coordinate.useContents { latitude }
            val lng = location.coordinate.useContents { longitude }
            Pair(lat, lng)
        } catch (_: Exception) {
            null
        }
    }
}
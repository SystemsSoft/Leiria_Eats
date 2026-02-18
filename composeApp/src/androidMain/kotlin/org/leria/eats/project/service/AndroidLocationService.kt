package org.leria.eats.project.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidLocationService(private val context: Context) : LocationService {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentAddress(): String? {
        return suspendCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            // 1. TENTA PEGAR A LOCALIZAÇÃO ATUAL (Rápida/Wi-Fi)
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->

                if (location != null) {
                    // Sucesso na primeira tentativa
                    val address = getAddressFromLocation(location)
                    continuation.resume(address)
                } else {
                    // 2. FALHOU? TENTA O CACHE (Última localização conhecida)
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            val address = getAddressFromLocation(lastLocation)
                            continuation.resume(address)
                        } else {
                            // 3. FALHOU TUDO
                            continuation.resume("Localização não encontrada. Ative o GPS.")
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener { exception ->
                // Se der erro na api de localização
                exception.printStackTrace()
                continuation.resume(null)
            }
        }
    }

    override fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare ?: address.featureName ?: "Rua sem nome"
                val number = address.subThoroughfare ?: "S/N"
                val district = address.subLocality ?: address.subAdminArea ?: ""

                if (district.isNotEmpty()) {
                    "$street, $number - $district"
                } else {
                    "$street, $number"
                }
            } else {
                "Endereço não identificado"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Erro de conexão (Internet)"
        }
    }

    // Função auxiliar para traduzir GPS -> Texto
    private fun getAddressFromLocation(location: Location): String {
        return getAddressFromCoordinates(location.latitude, location.longitude) ?: "Endereço não encontrado"
    }
}
package org.leria.eats.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PaymentIntentRequest(
    val amount_euros: Double,
    val restaurant_id: Int,
)

@Serializable
data class PaymentIntentResponse(
    val url: String,
)

class LeriaApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val baseUrl = "http://192.168.29.3:8080"
    suspend fun searchRestaurants(text: String): SearchResponse {
        val response = client.post("$baseUrl/search") {
            contentType(ContentType.Application.Json)
            setBody(SearchRequest(query = text))
        }
        return response.body()
    }

    suspend fun createPaymentIntent(request: PaymentIntentRequest): PaymentIntentResponse? {
        return try {
            val response = client.post("$baseUrl/checkout/create-session") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun sendOrder(request: OrderRequest): Boolean {
        try {
            val response = client.post("$baseUrl/orders") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.value == 422) {
                val errorBody: String = response.body()
                println("❌ Erro 422 no envio do pedido: $errorBody")
            }
            
            return response.status.value in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun getCustomerOrders(userId: String): List<Order> {
        return try {
            val response = client.get("$baseUrl/orders/customer/$userId")
            
            if (response.status.value == 200) {
                response.body()
            } else {
                val errorBody: String = response.body()
                println("⚠️ Erro ${response.status.value} ao buscar pedidos: $errorBody")
                emptyList()
            }
        } catch (e: Exception) {
            println("🚨 Falha crítica no GET: ${e.message}")
            emptyList()
        }
    }
}
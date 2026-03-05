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
data class PaymentIntentResponse(
    val url: String?,
    val auto_paid: Boolean = false,
    val order_id: Int? = null,
    val payment_intent_id: String? = null
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

    private val baseUrl = "https://api.leiriaeats.com"
    suspend fun searchRestaurants(text: String): SearchResponse {
        val response = client.post("$baseUrl/search") {
            contentType(ContentType.Application.Json)
            setBody(SearchRequest(query = text))
        }
        return response.body()
    }

    suspend fun initiateCheckout(request: OrderRequest): PaymentIntentResponse? {
        return try {
            val response = client.post("$baseUrl/orders/initiate-checkout") {
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

    suspend fun getCompanyById(idCompany: Int): CompanyResponse? {
        return try {
            val response = client.get("$baseUrl/companies/$idCompany")

            if (response.status.value == 200) {
                response.body()
            } else {
                val errorBody: String = response.body()
                println("⚠️ Erro ${response.status.value} ao buscar empresa: $errorBody")
                null
            }
        } catch (e: Exception) {
            println("🚨 Falha crítica no GET: ${e.message}")
            null
        }
    }
}
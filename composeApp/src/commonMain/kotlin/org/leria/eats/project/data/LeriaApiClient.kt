package org.leria.eats.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000 // 2 minutos
            connectTimeoutMillis = 30_000  // 30 segundos
            socketTimeoutMillis = 120_000  // 2 minutos
        }
    }

    private val baseUrl = "http://192.168.29.31:8000"
    private val urlLocal = "http://192.168.29.31:8000"

    // Session ID para contexto conversacional com IA
    private var sessionId: String? = null

    private fun generateSessionId(): String {
        // Gera um ID único baseado em valores aleatórios
        val part1 = (100000..999999).random()
        val part2 = (100000..999999).random()
        val part3 = (1000..9999).random()
        return "session_${part1}_${part2}_${part3}"
    }

    private fun getOrCreateSessionId(): String {
        if (sessionId == null) {
            sessionId = generateSessionId()
        }
        return sessionId!!
    }

    // NOVO: Endpoint com IA Generativa
    suspend fun sendChatMessage(text: String, restaurantGid: String? = null): ChatResponse {
        val response = client.post("$baseUrl/chat/sales") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                message = text,
                restaurantGid = restaurantGid,
                sessionId = getOrCreateSessionId()
            ))
        }
        return response.body()
    }

    // NOVO: Verificar status do servidor de IA
    suspend fun checkChatStatus(): Boolean {
        return try {
            val response = client.get("$baseUrl/chat/status")
            response.status.value == 200
        } catch (e: Exception) {
            println("⚠️ Erro ao verificar status da IA: ${e.message}")
            false
        }
    }

    // Mantido para compatibilidade (chama o novo endpoint)
    @Deprecated("Use sendChatMessage instead", ReplaceWith("sendChatMessage(text, null)"))
    suspend fun searchRestaurants(text: String): SearchResponse {
        val chatResponse = sendChatMessage(text, null)
        // Converte ChatResponse para SearchResponse para compatibilidade
        return SearchResponse(
            reply = chatResponse.response,
            intent = chatResponse.intent,
            restaurantResults = chatResponse.restaurantResults,
            productResults = if (chatResponse.products.isNotEmpty())
                chatResponse.products
            else
                chatResponse.productResults
        )
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

    suspend fun getCompanyByGid(gidCompany: String): CompanyResponse? {
        return try {
            val response = client.get("$baseUrl/companies/$gidCompany")

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

    /** Busca todos os restaurantes da API (/restaurants) e converte para o modelo local Restaurant. */
    suspend fun getAllRestaurants(): List<Restaurant> {
        return try {
            val response = client.get("$baseUrl/restaurants")
            if (response.status.value == 200) {
                // A API do backend retorna uma lista de CompanyResponse
                val companies: List<CompanyResponse> = response.body()
                companies.map { company ->
                    Restaurant(
                        gid = company.gid,
                        name = company.name,
                        category = company.category,
                        image_url = company.imageUrl,
                        products = company.products,
                        plan = company.plan,
                        latitude = company.latitude,
                        longitude = company.longitude
                    )
                }
            } else {
                val errorBody: String = response.body()
                println("⚠️ Erro ${response.status.value} ao buscar restaurantes: $errorBody")
                emptyList()
            }
        } catch (e: Exception) {
            println("🚨 Falha ao buscar restaurantes: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSavedPaymentMethods(userId: String): SavedPaymentMethodsResponse? {
        return try {
            val response = client.get("$baseUrl/users/$userId/saved-payment-methods")

            if (response.status.value == 200) {
                response.body()
            } else {
                val errorBody: String = response.body()
                println("⚠️ Erro ${response.status.value} ao buscar métodos de pagamento: $errorBody")
                null
            }
        } catch (e: Exception) {
            println("🚨 Falha ao buscar métodos de pagamento: ${e.message}")
            null
        }
    }

    suspend fun submitRatings(request: RatingRequest): RatingResponse? {
        return try {
            val response = client.post("$baseUrl/orders/ratings") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) {
                response.body()
            } else {
                val errorBody: String = response.body()
                println("⚠️ Erro ${response.status.value} ao enviar avaliações: $errorBody")
                null
            }
        } catch (e: Exception) {
            println("🚨 Falha ao enviar avaliações: ${e.message}")
            null
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Boolean {
        return try {
            val response = client.put("$baseUrl/orders/$orderId/status") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("status" to newStatus))
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            println("🚨 Falha ao atualizar status do pedido: ${e.message}")
            false
        }
    }

    suspend fun getDeliveryFee(request: DeliveryFeeRequest): DeliveryFeeResponse {
        val response = client.post("$baseUrl/orders/delivery-fee") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value in 200..299) {
            return response.body()
        } else {
            val errorBody: String = response.body()
            val detail = try {
                Json.parseToJsonElement(errorBody).jsonObject["detail"]?.jsonPrimitive?.content
            } catch (e: Exception) { null }
            throw Exception(detail ?: "Endereço fora da área de entrega.")
        }
    }
}
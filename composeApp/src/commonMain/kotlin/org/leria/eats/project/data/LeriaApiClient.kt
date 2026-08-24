package org.leria.eats.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.*
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class PaymentIntentResponse(
    val url: String? = null,
    val auto_paid: Boolean = false,
    val order_id: Int? = null,
    val payment_intent_id: String? = null,
    val publishableKey: String? = null,
    val clientSecret: String? = null,
    val customerId: String? = null,
    val ephemeralKey: String? = null
)

class LeriaApiClient {
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000 // 2 minutos
            connectTimeoutMillis = 30_000  // 30 segundos
            socketTimeoutMillis = 120_000  // 2 minutos
        }
    }

    private val baseUrl = "https://api.leiriaeats.com"
    private val urlLocal = "http://192.168.29.31:8000"
    private val urlprod = "https://api.leiriaeats.com"

    private fun normalizeUrl(url: String?): String? {
        if (url == null || url.isBlank()) return null
        return if (url.startsWith("http")) url else "$baseUrl$url"
    }

    private fun normalizeProductUrl(product: Product): Product {
        return product.copy(image_url = normalizeUrl(product.image_url))
    }

    private fun normalizeRestaurantUrls(restaurant: Restaurant): Restaurant {
        return restaurant.copy(
            image_url = normalizeUrl(restaurant.image_url),
            products = restaurant.products.map { normalizeProductUrl(it) }
        )
    }

    private fun normalizeChatResponse(response: ChatResponse): ChatResponse {
        return response.copy(
            products = response.products.map { normalizeProductUrl(it) },
            productResults = response.productResults.map { normalizeProductUrl(it) },
            restaurantResults = response.restaurantResults.map { normalizeRestaurantUrls(it) }
        )
    }

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

    // NOVO: Endpoint com IA Generativa (Síncrono)
    suspend fun sendChatMessage(text: String, restaurantGid: String? = null): ChatResponse {
        val response = client.post("$baseUrl/chat/sales") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                message = text,
                restaurantGid = restaurantGid,
                sessionId = getOrCreateSessionId()
            ))
        }
        val chatResponse: ChatResponse = response.body()
        return normalizeChatResponse(chatResponse)
    }

    // NOVO: Endpoint com IA Generativa (Streaming)
    fun sendChatMessageStream(text: String, restaurantGid: String? = null): Flow<ChatResponse> = flow {
        val request = ChatRequest(
            message = text,
            restaurantGid = restaurantGid,
            sessionId = getOrCreateSessionId()
        )
        
        client.preparePost("$baseUrl/chat/sales/stream") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.substring(6).trim()
                    if (data.isNotBlank()) {
                        try {
                            val chunk = json.decodeFromString<ChatResponse>(data)
                            emit(normalizeChatResponse(chunk))
                        } catch (e: Exception) {
                            println("⚠️ Erro ao decodificar chunk: ${e.message}")
                            println("JSON input: $data")
                        }
                    }
                }
            }
        }
    }

    // NOVO: Confirmar a sessão do pedido (Limpeza de estado no servidor)
    suspend fun confirmOrderSession(sessionId: String): Boolean {
        return try {
            val response = client.post("$baseUrl/chat/session/$sessionId/confirm") {
                contentType(ContentType.Application.Json)
            }
            response.status.value == 200
        } catch (e: Exception) {
            println("⚠️ Erro ao confirmar sessão da IA: ${e.message}")
            false
        }
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
                val orders: List<Order> = response.body()
                orders.map { order ->
                    order.copy(
                        subOrders = order.subOrders.map { sub ->
                            sub.copy(
                                restaurantImageUrl = normalizeUrl(sub.restaurantImageUrl),
                                items = sub.items.map { item ->
                                    item.copy(imageUrl = normalizeUrl(item.imageUrl) ?: "")
                                }
                            )
                        }
                    )
                }
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
                val company: CompanyResponse = response.body()
                company.copy(
                    imageUrl = normalizeUrl(company.imageUrl) ?: "",
                    products = company.products.map { normalizeProductUrl(it) }
                )
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
                    normalizeRestaurantUrls(Restaurant(
                        gid = company.gid,
                        name = company.name,
                        category = company.category,
                        image_url = company.imageUrl,
                        products = company.products,
                        plan = company.plan,
                        latitude = company.latitude,
                        longitude = company.longitude
                    ))
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
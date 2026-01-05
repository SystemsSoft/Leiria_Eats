package org.leria.eats.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LeriaApiClient {

    // Configura o Ktor
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "http://192.168.29.45:8000"

    suspend fun sendChat(text: String): AIResponse {
        val response = client.post("$baseUrl/chat") {
            contentType(ContentType.Application.Json)
            setBody(UserRequest(text = text))
        }
        return response.body()
    }
}
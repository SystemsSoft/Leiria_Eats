package org.leria.eats.project.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.leria.eats.project.domain.model.ChatMessage
import org.leria.eats.project.domain.repository.ChatHistoryRepository

class ChatHistoryRepositoryImpl(private val dataStore: DataStore<Preferences>) : ChatHistoryRepository {

    private val CHAT_MESSAGES_KEY = stringPreferencesKey("chat_messages")

    override val messages: Flow<List<ChatMessage>> = dataStore.data.map { preferences ->
        val json = preferences[CHAT_MESSAGES_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<ChatMessage>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun save(messages: List<ChatMessage>) {
        dataStore.edit { preferences ->
            // Mantém apenas as últimas 20 mensagens
            val limitedMessages = messages.takeLast(20)
            preferences[CHAT_MESSAGES_KEY] = Json.encodeToString(limitedMessages)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(CHAT_MESSAGES_KEY)
        }
    }
}
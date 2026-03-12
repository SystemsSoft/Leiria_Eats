package org.leria.eats.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileRepository(private val dataStore: DataStore<Preferences>) {

    private val ID_KEY = stringPreferencesKey("user_id")
    private val NAME_KEY = stringPreferencesKey("user_name")
    private val EMAIL_KEY = stringPreferencesKey("user_email")
    private val PHONE_KEY = stringPreferencesKey("user_phone")
    private val ADDRESSES_KEY = stringPreferencesKey("user_addresses")
    private val FAVORITE_ORDERS_KEY = stringSetPreferencesKey("favorite_orders")
    private val FAVORITE_ORDER_NICKNAMES_KEY = stringPreferencesKey("favorite_order_nicknames")
    private val ORDER_SEARCH_QUERIES_KEY = stringPreferencesKey("order_search_queries")
    private val PAYMENT_METHODS_KEY = stringPreferencesKey("saved_payment_methods")

    val userProfileFlow: Flow<UserProfile> = dataStore.data.map { preferences ->
        val addressesJson = preferences[ADDRESSES_KEY] ?: "[]"
        val addresses = try {
            Json.decodeFromString<List<Address>>(addressesJson)
        } catch (e: Exception) {
            emptyList()
        }
        val paymentMethodsJson = preferences[PAYMENT_METHODS_KEY] ?: "[]"
        val paymentMethods = try {
            Json.decodeFromString<List<SavedPaymentMethod>>(paymentMethodsJson)
        } catch (e: Exception) {
            emptyList()
        }
        UserProfile(
            id = preferences[ID_KEY] ?: "",
            name = preferences[NAME_KEY] ?: "",
            email = preferences[EMAIL_KEY] ?: "",
            phone = preferences[PHONE_KEY] ?: "",
            addresses = addresses,
            savedPaymentMethods = paymentMethods
        )
    }

    suspend fun saveProfile(
        id: String,
        name: String,
        email: String,
        phone: String,
        addresses: List<Address>, ) {
        dataStore.edit { preferences ->
            val addressesJson = Json.encodeToString(addresses)
            preferences[ID_KEY] = id
            preferences[NAME_KEY] = name
            preferences[EMAIL_KEY] = email
            preferences[PHONE_KEY] = phone
            preferences[ADDRESSES_KEY] = addressesJson
        }
    }

    val favoriteOrderIdsFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[FAVORITE_ORDERS_KEY] ?: emptySet()
    }

    suspend fun saveFavoriteOrderIds(ids: Set<String>) {
        dataStore.edit { preferences ->
            preferences[FAVORITE_ORDERS_KEY] = ids
        }
    }

    val favoriteOrderNicknamesFlow: Flow<Map<String, String>> = dataStore.data.map { preferences ->
        val json = preferences[FAVORITE_ORDER_NICKNAMES_KEY] ?: "{}"
        try {
            Json.decodeFromString<Map<String, String>>(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveFavoriteOrderNickname(orderId: String, nickname: String) {
        dataStore.edit { preferences ->
            val current = try {
                Json.decodeFromString<Map<String, String>>(preferences[FAVORITE_ORDER_NICKNAMES_KEY] ?: "{}")
            } catch (e: Exception) {
                emptyMap()
            }
            val updated = if (nickname.isBlank()) current - orderId else current + (orderId to nickname)
            preferences[FAVORITE_ORDER_NICKNAMES_KEY] = Json.encodeToString(updated)
        }
    }

    val orderSearchQueriesFlow: Flow<Map<String, String>> = dataStore.data.map { preferences ->
        val json = preferences[ORDER_SEARCH_QUERIES_KEY] ?: "{}"
        try {
            Json.decodeFromString<Map<String, String>>(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveOrderSearchQuery(orderId: String, searchQuery: String) {
        dataStore.edit { preferences ->
            val current = try {
                Json.decodeFromString<Map<String, String>>(preferences[ORDER_SEARCH_QUERIES_KEY] ?: "{}")
            } catch (e: Exception) {
                emptyMap()
            }
            val updated = current + (orderId to searchQuery)
            preferences[ORDER_SEARCH_QUERIES_KEY] = Json.encodeToString(updated)
        }
    }

    suspend fun savePaymentMethods(methods: List<SavedPaymentMethod>) {
        dataStore.edit { preferences ->
            val methodsJson = Json.encodeToString(methods)
            preferences[PAYMENT_METHODS_KEY] = methodsJson
        }
    }
}
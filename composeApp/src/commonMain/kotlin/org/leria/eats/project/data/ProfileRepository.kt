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
    private val ORDER_ITEM_RATINGS_KEY = stringPreferencesKey("order_item_ratings")
    private val ORDER_PRODUCT_IDS_KEY = stringPreferencesKey("order_product_ids")
    private val ORDER_RESTAURANT_IDS_KEY = stringPreferencesKey("order_restaurant_ids")
    private val ALLERGIES_KEY = stringPreferencesKey("user_allergies")
    private val LIFESTYLES_KEY = stringPreferencesKey("user_lifestyles")

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
            savedPaymentMethods = paymentMethods,
            allergies = preferences[ALLERGIES_KEY] ?: "",
            lifestyles = preferences[LIFESTYLES_KEY] ?: ""
        )
    }

    suspend fun saveProfile(
        id: String,
        name: String,
        email: String,
        phone: String,
        addresses: List<Address>,
        allergies: String = "",
        lifestyles: String = ""
    ) {
        dataStore.edit { preferences ->
            val addressesJson = Json.encodeToString(addresses)
            preferences[ID_KEY] = id
            preferences[NAME_KEY] = name
            preferences[EMAIL_KEY] = email
            preferences[PHONE_KEY] = phone
            preferences[ADDRESSES_KEY] = addressesJson
            preferences[ALLERGIES_KEY] = allergies
            preferences[LIFESTYLES_KEY] = lifestyles
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

    // ─── Order item ratings — key = "orderId::productName", value = 1..5 ──────
    val orderItemRatingsFlow: Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        val json = preferences[ORDER_ITEM_RATINGS_KEY] ?: "{}"
        try {
            Json.decodeFromString<Map<String, String>>(json).mapValues { it.value.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveOrderItemRating(orderId: String, productName: String, rating: Int) {
        dataStore.edit { preferences ->
            val current = try {
                Json.decodeFromString<Map<String, String>>(preferences[ORDER_ITEM_RATINGS_KEY] ?: "{}")
            } catch (e: Exception) {
                emptyMap()
            }
            val key = "$orderId::$productName"
            val updated = current + (key to rating.toString())
            preferences[ORDER_ITEM_RATINGS_KEY] = Json.encodeToString(updated)
        }
    }

    // ─── Product IDs — key = "orderId::productName", value = productId ────────
    val orderProductIdsFlow: Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        val json = preferences[ORDER_PRODUCT_IDS_KEY] ?: "{}"
        try {
            Json.decodeFromString<Map<String, String>>(json).mapValues { it.value.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveOrderProductIds(orderId: String, items: List<Pair<String, Int>>) {
        dataStore.edit { preferences ->
            val current = try {
                Json.decodeFromString<Map<String, String>>(preferences[ORDER_PRODUCT_IDS_KEY] ?: "{}")
            } catch (e: Exception) {
                emptyMap()
            }
            val newEntries = items.associate { (productName, productId) ->
                "$orderId::$productName" to productId.toString()
            }
            preferences[ORDER_PRODUCT_IDS_KEY] = Json.encodeToString(current + newEntries)
        }
    }

    // ─── Restaurant IDs — key = orderId, value = restaurantId ────────────────
    val orderRestaurantIdsFlow: Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        val json = preferences[ORDER_RESTAURANT_IDS_KEY] ?: "{}"
        try {
            Json.decodeFromString<Map<String, String>>(json).mapValues { it.value.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveOrderRestaurantId(orderId: String, restaurantId: Int) {
        dataStore.edit { preferences ->
            val current = try {
                Json.decodeFromString<Map<String, String>>(preferences[ORDER_RESTAURANT_IDS_KEY] ?: "{}")
            } catch (e: Exception) {
                emptyMap()
            }
            preferences[ORDER_RESTAURANT_IDS_KEY] = Json.encodeToString(current + (orderId to restaurantId.toString()))
        }
    }
}
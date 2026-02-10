package org.leria.eats.project.data


import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val dataStore: DataStore<Preferences>) {

    // Chaves para salvar os dados
    private val ID_KEY = stringPreferencesKey("user_id")
    private val NAME_KEY = stringPreferencesKey("user_name")
    private val ADDRESS_KEY = stringPreferencesKey("user_address")
    private val PHONE_KEY = stringPreferencesKey("user_phone")
    private val FAVORITE_ORDERS_KEY = stringSetPreferencesKey("favorite_orders")


    val userProfileFlow: Flow<UserProfile> = dataStore.data.map { preferences ->
        UserProfile(
            id = preferences[ID_KEY] ?: "",
            name = preferences[NAME_KEY] ?: "",
            phone = preferences[PHONE_KEY] ?: "",
            address = preferences[ADDRESS_KEY] ?: ""
        )
    }

    suspend fun saveProfile(id: String, name: String, phone: String, address: String) {
        dataStore.edit { preferences ->
            preferences[ID_KEY] = id
            preferences[NAME_KEY] = name
            preferences[PHONE_KEY] = phone
            preferences[ADDRESS_KEY] = address
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
}
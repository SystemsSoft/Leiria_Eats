package org.leria.eats.project.data


import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val dataStore: DataStore<Preferences>) {

    // Chaves para salvar os dados
    private val NAME_KEY = stringPreferencesKey("user_name")
    private val ADDRESS_KEY = stringPreferencesKey("user_address")

    private val PHONE_KEY = stringPreferencesKey("user_phone")

    val userProfileFlow: Flow<UserProfile> = dataStore.data.map { preferences ->
        UserProfile(
            name = preferences[NAME_KEY] ?: "",
            phone = preferences[PHONE_KEY] ?: "", // <--- Novo
            address = preferences[ADDRESS_KEY] ?: ""
        )
    }

    suspend fun saveProfile(name: String, phone: String, address: String) {
        dataStore.edit { preferences ->
            preferences[NAME_KEY] = name
            preferences[PHONE_KEY] = phone // <--- Novo
            preferences[ADDRESS_KEY] = address
        }
    }
}
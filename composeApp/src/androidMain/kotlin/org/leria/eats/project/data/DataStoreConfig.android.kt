// Arquivo: composeApp/src/androidMain/kotlin/.../data/DataStoreConfig.android.kt
package org.leria.eats.project.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

private lateinit var appContext: Context

fun initAndroidDataStore(context: Context) {
    appContext = context
}

actual fun getDataStore(): DataStore<Preferences> {
    if (!::appContext.isInitialized) {
        throw IllegalStateException("❌ ERRO CRÍTICO: Chame initAndroidDataStore(context) na MainActivity antes de usar o Koin!")
    }

    return createDataStore(
        producePath = {
            appContext.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
        }
    )
}
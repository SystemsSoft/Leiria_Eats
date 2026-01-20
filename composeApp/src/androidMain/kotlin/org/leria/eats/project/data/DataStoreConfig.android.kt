package org.leria.eats.project.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

// Variável global ou injetada para segurar o contexto
lateinit var appContext: Context

actual fun getDataStore(): DataStore<Preferences> {
    return createDataStore(
        producePath = {
            // No Android, salvamos na pasta de arquivos interna
            appContext.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
        }
    )
}
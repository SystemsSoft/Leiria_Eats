package org.leria.eats.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun getDataStore(): DataStore<Preferences>
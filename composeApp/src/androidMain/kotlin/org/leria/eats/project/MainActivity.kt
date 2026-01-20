package org.leria.eats.project

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.leria.eats.project.data.initAndroidDataStore


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAndroidDataStore(applicationContext)

        setContent {
            App()
        }
    }
}
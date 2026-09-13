package org.leria.eats.project

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.android.ext.android.inject
import org.leria.eats.project.data.initAndroidDataStore
import org.leria.eats.project.payment.StripePaymentManager
import org.leria.eats.project.presentation.VideoSplashScreen

class MainActivity : ComponentActivity() {

    private val stripePaymentManager: StripePaymentManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHolder.activity = this
        initAndroidDataStore(applicationContext)
        org.leria.eats.project.data.setApplicationContext(applicationContext)

        // Registrar o Stripe PaymentSheet antes de iniciar o conteúdo (IMPORTANTE)
        stripePaymentManager.register(this)

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                VideoSplashScreen(onFinished = { showSplash = false })
            } else {
                App()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityHolder.activity = null
    }
}

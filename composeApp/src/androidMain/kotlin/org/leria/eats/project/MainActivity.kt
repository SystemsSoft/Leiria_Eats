package org.leria.eats.project

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.android.ext.android.inject
import org.leria.eats.project.data.initAndroidDataStore
import org.leria.eats.project.payment.StripePaymentManager

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
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityHolder.activity = null
    }
}

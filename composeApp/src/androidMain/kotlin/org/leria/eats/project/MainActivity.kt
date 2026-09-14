package org.leria.eats.project

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import org.koin.android.ext.android.inject
import org.leria.eats.project.data.initAndroidDataStore
import org.leria.eats.project.payment.StripePaymentManager
import org.leria.eats.project.presentation.SplashTransitionHost
import org.leria.eats.project.theme.KomaTopBarGreenStart

class MainActivity : ComponentActivity() {

    private val stripePaymentManager: StripePaymentManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(KomaTopBarGreenStart.toArgb())
        )

        ActivityHolder.activity = this
        initAndroidDataStore(applicationContext)
        org.leria.eats.project.data.setApplicationContext(applicationContext)

        // Registrar o Stripe PaymentSheet antes de iniciar o conteúdo (IMPORTANTE)
        stripePaymentManager.register(this)

        setContent {
            SplashTransitionHost {
                App()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityHolder.activity = null
    }
}

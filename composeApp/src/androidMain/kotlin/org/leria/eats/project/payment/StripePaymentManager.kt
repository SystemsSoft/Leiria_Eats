package org.leria.eats.project.payment

import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import androidx.activity.ComponentActivity

actual class StripePaymentManager actual constructor() {
    private var paymentSheet: PaymentSheet? = null
    private var onPaymentResult: ((StripePaymentResult) -> Unit)? = null

    /**
     * REGISTRO OBRIGATÓRIO: Deve ser chamado no onCreate da Activity.
     */
    fun register(activity: ComponentActivity) {
        paymentSheet = PaymentSheet(activity) { result ->
            when (result) {
                is PaymentSheetResult.Completed -> {
                    onPaymentResult?.invoke(StripePaymentResult.Completed)
                }
                is PaymentSheetResult.Canceled -> {
                    onPaymentResult?.invoke(StripePaymentResult.Canceled)
                }
                is PaymentSheetResult.Failed -> {
                    onPaymentResult?.invoke(StripePaymentResult.Failed(result.error.localizedMessage ?: "Erro desconhecido"))
                }
            }
        }
    }

    actual fun init(publishableKey: String) {
        val activity = org.leria.eats.project.ActivityHolder.activity ?: return
        PaymentConfiguration.init(activity, publishableKey)
    }

    actual fun presentPaymentSheet(
        paymentIntentClientSecret: String,
        customerId: String?,
        ephemeralKeySecret: String?,
        onResult: (StripePaymentResult) -> Unit
    ) {
        this.onPaymentResult = onResult
        
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "KOMAAI",
            customer = if (customerId != null && ephemeralKeySecret != null) {
                PaymentSheet.CustomerConfiguration(
                    id = customerId,
                    ephemeralKeySecret = ephemeralKeySecret
                )
            } else null
        )

        paymentSheet?.presentWithPaymentIntent(
            paymentIntentClientSecret,
            configuration
        )
    }
}
package org.leria.eats.project.payment

import org.leria.eats.project.data.PaymentIntentResponse

/**
 * Interface para gerenciar pagamentos via SDK nativo da Stripe.
 */
expect class StripePaymentManager() {
    /**
     * Inicializa o SDK da Stripe (Publishable Key).
     */
    fun init(publishableKey: String)

    /**
     * Apresenta o PaymentSheet (interface de pagamento nativa).
     */
    fun presentPaymentSheet(
        paymentIntentClientSecret: String,
        customerId: String?,
        ephemeralKeySecret: String?,
        onResult: (StripePaymentResult) -> Unit
    )
}

sealed class StripePaymentResult {
    object Completed : StripePaymentResult()
    object Canceled : StripePaymentResult()
    data class Failed(val error: String) : StripePaymentResult()
}
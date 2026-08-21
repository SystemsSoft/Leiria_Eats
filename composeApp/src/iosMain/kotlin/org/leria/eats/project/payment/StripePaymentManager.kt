package org.leria.eats.project.payment

actual class StripePaymentManager actual constructor() {
    actual fun init(publishableKey: String) {
        // TODO: Implementar usando Stripe iOS SDK (via CocoaPods ou SPM)
    }

    actual fun presentPaymentSheet(
        paymentIntentClientSecret: String,
        customerId: String?,
        ephemeralKeySecret: String?,
        onResult: (StripePaymentResult) -> Unit
    ) {
        // TODO: Implementar usando Stripe iOS SDK
        println("Stripe iOS não implementado ainda")
        onResult(StripePaymentResult.Failed("Stripe iOS não implementado"))
    }
}
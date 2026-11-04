package org.leria.eats.project.payment

/**
 * Ponte para o SDK StripePaymentSheet, que só existe do lado Swift (Swift Package Manager).
 * Implementada em iosApp/iosApp/StripeBridge.swift e registrada no init do iOSApp.
 */
interface StripeIosBridge {
    fun configure(publishableKey: String)

    fun presentPaymentSheet(
        paymentIntentClientSecret: String,
        customerId: String?,
        ephemeralKeySecret: String?,
        onCompleted: () -> Unit,
        onCanceled: () -> Unit,
        onFailed: (String) -> Unit
    )
}

object StripeIosBridgeHolder {
    var bridge: StripeIosBridge? = null
}

actual class StripePaymentManager actual constructor() {
    actual fun init(publishableKey: String) {
        StripeIosBridgeHolder.bridge?.configure(publishableKey)
    }

    actual fun presentPaymentSheet(
        paymentIntentClientSecret: String,
        customerId: String?,
        ephemeralKeySecret: String?,
        onResult: (StripePaymentResult) -> Unit
    ) {
        val bridge = StripeIosBridgeHolder.bridge
        if (bridge == null) {
            onResult(StripePaymentResult.Failed("Pagamento indisponível: Stripe não registrada no iOS"))
            return
        }
        bridge.presentPaymentSheet(
            paymentIntentClientSecret = paymentIntentClientSecret,
            customerId = customerId,
            ephemeralKeySecret = ephemeralKeySecret,
            onCompleted = { onResult(StripePaymentResult.Completed) },
            onCanceled = { onResult(StripePaymentResult.Canceled) },
            onFailed = { error -> onResult(StripePaymentResult.Failed(error)) }
        )
    }
}

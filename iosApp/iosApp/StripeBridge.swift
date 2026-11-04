import UIKit
import ComposeApp
import StripePaymentSheet

/// Implementa a ponte StripeIosBridge (Kotlin) com o PaymentSheet nativo da Stripe.
final class StripeBridge: NSObject, StripeIosBridge {
    /// Precisa casar com o esquema "komaai" do CFBundleURLTypes no Info.plist.
    static let returnURL = "komaai://stripe-redirect"

    private var paymentSheet: PaymentSheet?

    func configure(publishableKey: String) {
        STPAPIClient.shared.publishableKey = publishableKey
    }

    func presentPaymentSheet(
        paymentIntentClientSecret: String,
        customerId: String?,
        ephemeralKeySecret: String?,
        onCompleted: @escaping () -> Void,
        onCanceled: @escaping () -> Void,
        onFailed: @escaping (String) -> Void
    ) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                onFailed("Não foi possível abrir o pagamento")
                return
            }

            var configuration = PaymentSheet.Configuration()
            configuration.merchantDisplayName = "KOMAAI"
            configuration.returnURL = Self.returnURL
            if let customerId, let ephemeralKeySecret {
                configuration.customer = .init(id: customerId, ephemeralKeySecret: ephemeralKeySecret)
            }

            let sheet = PaymentSheet(
                paymentIntentClientSecret: paymentIntentClientSecret,
                configuration: configuration
            )
            self.paymentSheet = sheet
            sheet.present(from: presenter) { result in
                self.paymentSheet = nil
                switch result {
                case .completed:
                    onCompleted()
                case .canceled:
                    onCanceled()
                case .failed(let error):
                    onFailed(error.localizedDescription)
                }
            }
        }
    }

    private static func topViewController() -> UIViewController? {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

import SwiftUI
import ComposeApp // O nome do seu módulo compartilhado (pode variar)
import StripePaymentSheet

@main
struct iOSApp: App {

    init() {
        KoinHelperKt.doInitKoin()
        StripeIosBridgeHolder.shared.bridge = StripeBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Retorno de pagamentos com redirecionamento (ex.: MB WAY)
                    _ = StripeAPI.handleURLCallback(with: url)
                }
        }
    }
}

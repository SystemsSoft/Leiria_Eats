import SwiftUI
import ComposeApp // O nome do seu módulo compartilhado (pode variar)

@main
struct iOSApp: App {

    init() {
        KoinHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
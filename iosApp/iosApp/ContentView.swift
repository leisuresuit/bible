import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

enum AppView {
    case compose
    case native
}

struct ContentView: View {
    @State private var selectedUI: AppView = .compose
    
    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch selectedUI {
                case .compose:
                    ComposeView()
                        .ignoresSafeArea()
                case .native:
                    BibleView()
                }
            }
            
            // UI Switcher
            Picker("UI Mode", selection: $selectedUI) {
                Text("Compose").tag(AppView.compose)
                Text("Native").tag(AppView.native)
            }
            .pickerStyle(.segmented)
            .padding()
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding()
        }
    }
}

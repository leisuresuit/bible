import SwiftUI
import ComposeApp

struct SettingsView: View {
    @State private var localTheme: AppTheme
    @State private var localDisplayMode: DisplayMode
    @State private var localShowWordsOfJesus: Bool
    
    private let onThemeChange: (AppTheme) -> Void
    private let onDisplayModeChange: (DisplayMode) -> Void
    private let onShowWordsOfJesusChange: (Bool) -> Void
    private let onDismiss: () -> Void
    let isSidePanel: Bool
    
    init(
        theme: AppTheme,
        displayMode: DisplayMode,
        showWordsOfJesus: Bool,
        onThemeChange: @escaping (AppTheme) -> Void,
        onDisplayModeChange: @escaping (DisplayMode) -> Void,
        onShowWordsOfJesusChange: @escaping (Bool) -> Void,
        onDismiss: @escaping () -> Void,
        isSidePanel: Bool = false
    ) {
        self._localTheme = State(initialValue: theme)
        self._localDisplayMode = State(initialValue: displayMode)
        self._localShowWordsOfJesus = State(initialValue: showWordsOfJesus)
        self.onThemeChange = onThemeChange
        self.onDisplayModeChange = onDisplayModeChange
        self.onShowWordsOfJesusChange = onShowWordsOfJesusChange
        self.onDismiss = onDismiss
        self.isSidePanel = isSidePanel
    }
    
    var body: some View {
        if isSidePanel {
            content
        } else {
            NavigationView {
                content
            }
        }
    }
    
    private var content: some View {
        Form {
            Section(header: Text(NSLocalizedString("appearance", comment: ""))) {
                Picker(NSLocalizedString("theme", comment: ""), selection: $localTheme) {
                    Text(NSLocalizedString("theme_system", comment: "")).tag(AppTheme.system)
                    Text(NSLocalizedString("theme_light", comment: "")).tag(AppTheme.light)
                    Text(NSLocalizedString("theme_dark", comment: "")).tag(AppTheme.dark)
                }
            }
            
            Section(header: Text(NSLocalizedString("reading_experience", comment: ""))) {
                Picker(NSLocalizedString("display_mode", comment: ""), selection: $localDisplayMode) {
                    Text(NSLocalizedString("mode_single", comment: "")).tag(DisplayMode.singleChapter)
                    Text(NSLocalizedString("mode_contiguous", comment: "")).tag(DisplayMode.contiguous)
                }
                
                Toggle(NSLocalizedString("show_words_of_jesus", comment: ""), isOn: $localShowWordsOfJesus)
            }
        }
        .navigationTitle(NSLocalizedString("settings", comment: ""))
        .onChange(of: localTheme) { _, newValue in
            onThemeChange(newValue)
        }
        .onChange(of: localDisplayMode) { _, newValue in
            onDisplayModeChange(newValue)
        }
        .onChange(of: localShowWordsOfJesus) { _, newValue in
            onShowWordsOfJesusChange(newValue)
        }
        .preferredColorScheme(localColorScheme)
        .overlay(alignment: .top) {
            if isSidePanel {
                VStack(spacing: 0) {
                    HStack {
                        Text(NSLocalizedString("settings", comment: ""))
                            .font(.headline)
                        Spacer()
                    }
                    .padding()
                    Divider()
                }
                .background(Color(.systemBackground))
            }
        }
    }
    
    private var localColorScheme: ColorScheme? {
        switch localTheme {
        case .light: return .light
        case .dark: return .dark
        default: return nil
        }
    }
}

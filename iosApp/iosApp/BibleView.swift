import SwiftUI
import ComposeApp

struct BibleView: View {
    @State private var viewModel: BibleViewModel
    @State private var state: BibleState
    @State private var searchQuery: String = ""
    
    @State private var showError: Bool = false
    @State private var errorMessage: String = ""
    @State private var errorActionLabel: String? = nil
    @State private var errorAction: (() -> Void)? = nil
    
    @Environment(\.horizontalSizeClass) var horizontalSizeClass
    @Environment(\.verticalSizeClass) var verticalSizeClass
    
    var useSidePanel: Bool {
        horizontalSizeClass != .compact || verticalSizeClass == .compact
    }

    init() {
        let vm = KoinHelper().getBibleViewModel()
        self._viewModel = State(initialValue: vm)
        self._state = State(initialValue: vm.state.value)
        self._searchQuery = State(initialValue: vm.state.value.searchQuery)
    }
    
    private var navigationTitleText: String {
        "\(state.currentBook.localizedName) \(state.currentChapter)"
    }

    var body: some View {
        ZStack {
            if useSidePanel {
                landscapeLayout
            } else {
                portraitLayout
            }
        }
        .task {
            for await newState in viewModel.state {
                self.state = newState
                self.searchQuery = newState.searchQuery
            }
        }
        .task {
            for await effect in viewModel.effects {
                if let snackbar = effect as? BibleEffect.ShowSnackbar {
                    self.errorMessage = snackbar.message
                    self.errorActionLabel = snackbar.actionLabel
                    self.errorAction = { snackbar.onAction?() }
                    self.showError = true
                }
            }
        }
        .alert(errorMessage, isPresented: $showError) {
            if let label = errorActionLabel {
                Button(label) {
                    errorAction?()
                }
            }
            Button("OK", role: .cancel) { }
        }
        .preferredColorScheme(colorScheme)
    }
    
    private var portraitLayout: some View {
        NavigationView {
            verseList
                .navigationTitle(navigationTitleText)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        chapterNavigationButtons
                    }
                    
                    ToolbarItemGroup(placement: .bottomBar) {
                        bottomBarButtons
                    }
                }
        }
        .sheet(item: Binding(
            get: { state.activeSheet.map { IdentifiableSheet(sheet: $0) } },
            set: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: $0?.sheet)) }
        )) { wrapper in
            sheetContentView(sheet: wrapper.sheet, isSidePanel: false)
        }
    }
    
    private var landscapeLayout: some View {
        HStack(spacing: 0) {
            // Navigation Rail
            VStack(spacing: 20) {
                Button(action: {
                    viewModel.onIntent(intent: BibleIntent.ToggleSheet(sheet: ActiveSheet.PassageSelection(initialPage: 0)))
                }) {
                    VStack {
                        Text(state.currentBook.localizedName)
                            .font(.caption)
                        Text("\(state.currentChapter)")
                            .font(.headline)
                            .bold()
                    }
                    .foregroundColor(.primary)
                }
                .padding(.top, 40)
                
                Button(action: {
                    viewModel.onIntent(intent: BibleIntent.ToggleSheet(sheet: ActiveSheet.VersionSelection()))
                }) {
                    Text(selectedVersionAbbreviation)
                        .font(.caption)
                        .bold()
                        .padding(8)
                        .background(Color.accentColor.opacity(0.1))
                        .cornerRadius(8)
                }

                Spacer()
                
                railIconButton(systemName: "magnifyingglass", title: NSLocalizedString("search", comment: ""), sheet: ActiveSheet.Search())
                railIconButton(systemName: "clock", title: NSLocalizedString("history", comment: ""), sheet: ActiveSheet.History())
                railIconButton(systemName: "gearshape", title: NSLocalizedString("settings", comment: ""), sheet: ActiveSheet.Settings())
                
                Spacer()
                    .frame(height: 20)
            }
            .frame(width: 80)
            .background(Color(.systemBackground))
            
            Divider()
            
            // Side Panel
            if let activeSheet = state.activeSheet {
                ZStack(alignment: .topTrailing) {
                    sheetContentView(sheet: activeSheet, isSidePanel: true)
                        .frame(width: 320)
                    
                    Button(action: {
                        viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil))
                    }) {
                        Image(systemName: "xmark")
                            .padding()
                            .contentShape(Rectangle())
                    }
                    .padding(8)
                    .zIndex(1)
                }
                Divider()
            }
            
            // Main Content
            verseList
                .safeAreaInset(edge: .top) {
                    VStack(spacing: 0) {
                        HStack {
                            Spacer()
                            Text(navigationTitleText)
                                .font(.headline)
                            Spacer()
                            chapterNavigationButtons
                        }
                        .padding(.horizontal)
                        .frame(height: 50)
                        .background(.ultraThinMaterial)
                        
                        Divider()
                    }
                }
        }
    }
    
    private var verseList: some View {
        List {
            ForEach(state.verses, id: \.number) { verse in
                formatVerse(verse)
                    .font(.system(.body, design: .serif))
                    .lineSpacing(4)
                    .padding(.vertical, 3)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
                    .textSelection(.enabled)
            }
            
            Color.clear
                .frame(height: 150)
                .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .environment(\.defaultMinListRowHeight, 0)
    }
    
    private var chapterNavigationButtons: some View {
        HStack {
            Button(action: { viewModel.onIntent(intent: BibleIntent.PreviousChapter()) }) {
                Image(systemName: "chevron.left")
            }
            Button(action: { viewModel.onIntent(intent: BibleIntent.NextChapter()) }) {
                Image(systemName: "chevron.right")
            }
        }
    }
    
    private var bottomBarButtons: some View {
        Group {
            Button(action: {
                viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.PassageSelection(initialPage: 0)))
            }) {
                Label(navigationTitleText, systemImage: "chevron.down")
            }
            Spacer()
            Button(action: {
                viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.VersionSelection()))
            }) {
                Label(NSLocalizedString("versions", comment: ""), systemImage: "books.vertical")
            }
            Spacer()
            Button(action: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.Search())) }) {
                Image(systemName: "magnifyingglass")
            }
            Spacer()
            Button(action: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.History())) }) {
                Image(systemName: "clock")
            }
            Spacer()
            Button(action: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.Settings())) }) {
                Image(systemName: "gearshape")
            }
        }
    }
    
    private func railIconButton(systemName: String, title: String, sheet: ActiveSheet) -> some View {
        let isSelected = state.activeSheet != nil && type(of: state.activeSheet!) == type(of: sheet)
        return Button(action: {
            viewModel.onIntent(intent: BibleIntent.ToggleSheet(sheet: sheet))
        }) {
            Image(systemName: systemName)
                .font(.title3)
                .foregroundColor(isSelected ? .accentColor : .secondary)
                .frame(width: 44, height: 44)
                .background(isSelected ? Color.accentColor.opacity(0.1) : Color.clear)
                .cornerRadius(12)
        }
        .accessibilityLabel(title)
    }
    
    @ViewBuilder
    private func sheetContentView(sheet: ActiveSheet, isSidePanel: Bool) -> some View {
        switch sheet {
        case let selection as ActiveSheet.PassageSelection:
            PassageSelectionView(
                allBooks: state.allBooks,
                selectedBook: state.currentBook,
                selectedChapter: Int(state.currentChapter),
                initialPage: Int(selection.initialPage),
                onSelectPassage: { book, chapter, verse in
                    viewModel.onIntent(intent: BibleIntent.SelectPassage(book: book, chapter: Int32(chapter), verse: Int32(verse)))
                    viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil))
                },
                onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) },
                isSidePanel: isSidePanel
            )
        case is ActiveSheet.Settings:
            SettingsView(
                theme: state.theme,
                displayMode: state.displayMode,
                showWordsOfJesus: state.showWordsOfJesus,
                onThemeChange: { viewModel.onIntent(intent: BibleIntent.UpdateTheme(theme: $0)) },
                onDisplayModeChange: { viewModel.onIntent(intent: BibleIntent.UpdateDisplayMode(mode: $0)) },
                onShowWordsOfJesusChange: { viewModel.onIntent(intent: BibleIntent.UpdateShowWordsOfJesus(enabled: $0)) },
                onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) },
                isSidePanel: isSidePanel
            )
        case is ActiveSheet.History:
            HistoryView(
                history: state.history,
                currentBook: state.currentBook,
                currentChapter: state.currentChapter,
                currentVerse: state.currentVerse,
                onItemClick: { item in
                    viewModel.onIntent(intent: BibleIntent.NavigateToHistoryItem(item: item))
                    if !isSidePanel {
                        viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil))
                    }
                },
                onClear: { viewModel.onIntent(intent: BibleIntent.ClearHistory()) },
                isSidePanel: isSidePanel
            )
        case is ActiveSheet.VersionSelection:
            VersionSelectionView(
                versions: state.versions,
                selectedVersions: state.selectedVersions,
                onToggleVersion: { viewModel.onIntent(intent: BibleIntent.ToggleParallelVersion(version: $0)) },
                onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) },
                isSidePanel: isSidePanel
            )
        case is ActiveSheet.Search:
            SearchView(
                searchQuery: Binding(
                    get: { searchQuery },
                    set: { newValue in
                        searchQuery = newValue
                        viewModel.onIntent(intent: BibleIntent.UpdateSearchQuery(query: newValue))
                    }
                ),
                searchResults: state.searchResults,
                searchSort: state.searchSort,
                isLoading: state.isLoading,
                onSearchQueryChange: { viewModel.onIntent(intent: BibleIntent.UpdateSearchQuery(query: $0)) },
                onSearchSortChange: { viewModel.onIntent(intent: BibleIntent.UpdateSearchSort(sort: $0)) },
                onResultClick: { result in
                    viewModel.onIntent(intent: BibleIntent.SelectPassage(book: result.book, chapter: result.chapterNumber, verse: result.verseNumber))
                    if !isSidePanel {
                        viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil))
                    }
                },
                onBack: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) },
                isSidePanel: isSidePanel
            )
        default:
            EmptyView()
        }
    }
    
    private var selectedVersionAbbreviation: String {
        if state.selectedVersions.count > 1 {
            return NSLocalizedString("versions", comment: "")
        } else {
            return state.selectedVersions.first?.abbreviation ?? NSLocalizedString("versions", comment: "")
        }
    }
    
    private var colorScheme: ColorScheme? {
        switch state.theme {
        case .light: return .light
        case .dark: return .dark
        default: return nil
        }
    }

    private func formatVerse(_ verse: Verse) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(verse.elements.enumerated()), id: \.offset) { index, element in
                if let textElement = element as? VerseElementText {
                    let combinedText = textElement.spans.reduce(Text("")) { result, span in
                        var t = Text(span.text)
                        let style = span.style
                        
                        if style == .bold || style == .wordsOfJesusBold || style == .wordsOfJesusItalicBold || style == .italicBold {
                            t = t.bold()
                        }
                        if style == .italic || style == .wordsOfJesusItalic || style == .wordsOfJesusItalicBold || style == .italicBold {
                            t = t.italic()
                        }
                        
                        let isJesus = style == .wordsOfJesus || style == .wordsOfJesusBold ||
                                     style == .wordsOfJesusItalic || style == .wordsOfJesusItalicBold
                        
                        if isJesus && state.showWordsOfJesus {
                            t = t.foregroundColor(.red)
                        }
                        return result + t
                    }
                    
                    // Show verse number only on the first Text element
                    if index == verse.elements.firstIndex(where: { $0 is VerseElementText }) {
                        (Text("\(verse.number)   ").bold() + combinedText)
                    } else {
                        combinedText
                    }
                } else if let headingElement = element as? VerseElementHeading {
                    let headingText = headingElement.spans.map { $0.text }.joined()
                    Text(headingText)
                        .bold()
                        .font(.headline)
                        .padding(.top, index == 0 ? 0 : 8)
                }
            }
        }
    }
}

// Wrapper to avoid extension-of-imported-type Identifiable conformance error
struct IdentifiableSheet: Identifiable {
    let sheet: ActiveSheet
    var id: String {
        switch sheet {
        case is ActiveSheet.VersionSelection: return "version"
        case let p as ActiveSheet.PassageSelection: return "passage-\(p.initialPage)"
        case is ActiveSheet.Settings: return "settings"
        case is ActiveSheet.History: return "history"
        case is ActiveSheet.Search: return "search"
        default: return "none"
        }
    }
}

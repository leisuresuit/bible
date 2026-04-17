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
    
    init() {
        let vm = KoinHelper().getBibleViewModel()
        self._viewModel = State(initialValue: vm)
        self._state = State(initialValue: vm.state.value)
        self._searchQuery = State(initialValue: vm.state.value.searchQuery)
    }
    
    var body: some View {
        NavigationView {
            VStack {
                List {
                    let verses = state.verses
                    ForEach(verses, id: \.number) { verse in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(verse.number)")
                                .font(.system(.caption, design: .serif))
                                .foregroundColor(.secondary)
                                .frame(width: 24, alignment: .trailing)
                                .padding(.top, 4)
                            
                            formatVerse(verse)
                                .font(.system(.body, design: .serif))
                                .lineSpacing(4)
                        }
                        .padding(.vertical, 4)
                        .listRowSeparator(.hidden)
                    }
                }
                .listStyle(.plain)
                .navigationTitle("\(state.currentBook?.localizedName ?? "") \(state.currentChapter)")
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: ActiveDialog.PassageSelection(initialPage: 0)))
                        }) {
                            HStack(spacing: 4) {
                                Text("\(state.currentBook?.localizedName ?? "") \(state.currentChapter)")
                                    .font(.headline)
                                Image(systemName: "chevron.down")
                                    .font(.caption.bold())
                            }
                            .foregroundColor(.primary)
                        }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        HStack {
                            Button(action: {
                                viewModel.onIntent(intent: BibleIntent.PreviousChapter())
                            }) {
                                Image(systemName: "chevron.left")
                            }
                            Button(action: {
                                viewModel.onIntent(intent: BibleIntent.NextChapter())
                            }) {
                                Image(systemName: "chevron.right")
                            }
                        }
                    }
                    
                    ToolbarItemGroup(placement: .bottomBar) {
                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: ActiveDialog.VersionSelection()))
                        }) {
                            Label(NSLocalizedString("versions", comment: ""), systemImage: "books.vertical")
                        }
                        Spacer()
                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.SetSearchMode(enabled: true))
                        }) {
                            Label(NSLocalizedString("search", comment: ""), systemImage: "magnifyingglass")
                        }
                        Spacer()
                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: ActiveDialog.History()))
                        }) {
                            Label(NSLocalizedString("history", comment: ""), systemImage: "clock")
                        }
                        Spacer()
                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: ActiveDialog.Settings()))
                        }) {
                            Label(NSLocalizedString("settings", comment: ""), systemImage: "gearshape")
                        }
                    }
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
            .sheet(isPresented: Binding(
                get: { state.activeDialog is ActiveDialog.PassageSelection },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) } }
            )) {
                if let selection = state.activeDialog as? ActiveDialog.PassageSelection {
                    PassageSelectionView(
                        allBooks: state.allBooks,
                        selectedBook: state.currentBook,
                        selectedChapter: Int(state.currentChapter),
                        initialPage: Int(selection.initialPage),
                        onSelectPassage: { book, chapter, verse in
                            viewModel.onIntent(intent: BibleIntent.SelectPassage(book: book, chapter: Int32(chapter), verse: Int32(verse)))
                        },
                        onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) }
                    )
                }
            }
            .sheet(isPresented: Binding(
                get: { state.activeDialog is ActiveDialog.Settings },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) } }
            )) {
                SettingsView(
                    theme: state.theme,
                    displayMode: state.displayMode,
                    showWordsOfJesus: state.showWordsOfJesus,
                    onThemeChange: { viewModel.onIntent(intent: BibleIntent.UpdateTheme(theme: $0)) },
                    onDisplayModeChange: { viewModel.onIntent(intent: BibleIntent.UpdateDisplayMode(mode: $0)) },
                    onShowWordsOfJesusChange: { viewModel.onIntent(intent: BibleIntent.UpdateShowWordsOfJesus(enabled: $0)) },
                    onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) }
                )
            }
            .sheet(isPresented: Binding(
                get: { state.activeDialog is ActiveDialog.History },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) } }
            )) {
                HistoryView(
                    history: state.history,
                    currentBook: state.currentBook,
                    currentChapter: state.currentChapter,
                    currentVerse: state.currentVerse,
                    onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) },
                    onItemClick: { item in
                        viewModel.onIntent(intent: BibleIntent.NavigateToHistoryItem(item: item))
                        viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil))
                    },
                    onClear: { viewModel.onIntent(intent: BibleIntent.ClearHistory()) }
                )
            }
            .sheet(isPresented: Binding(
                get: { state.activeDialog is ActiveDialog.VersionSelection },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) } }
            )) {
                VersionSelectionView(
                    versions: state.versions,
                    selectedVersions: state.selectedVersions,
                    onToggleVersion: { viewModel.onIntent(intent: BibleIntent.ToggleParallelVersion(version: $0)) },
                    onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowDialog(dialog: nil)) }
                )
            }
            .fullScreenCover(isPresented: Binding(
                get: { state.isSearchMode },
                set: { viewModel.onIntent(intent: BibleIntent.SetSearchMode(enabled: $0)) }
            )) {
                SearchView(
                    searchQuery: $searchQuery,
                    searchResults: state.searchResults,
                    searchSort: state.searchSort,
                    isLoading: state.isLoading,
                    onSearchQueryChange: { viewModel.onIntent(intent: BibleIntent.UpdateSearchQuery(query: $0)) },
                    onSearchSortChange: { viewModel.onIntent(intent: BibleIntent.UpdateSearchSort(sort: $0)) },
                    onResultClick: { result in
                        viewModel.onIntent(intent: BibleIntent.SelectPassage(book: result.book, chapter: result.chapterNumber, verse: result.verseNumber))
                        viewModel.onIntent(intent: BibleIntent.SetSearchMode(enabled: false))
                    },
                    onBack: { viewModel.onIntent(intent: BibleIntent.SetSearchMode(enabled: false)) }
                )
            }
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
        var views: [Text] = []
        
        for element in verse.elements {
            if let textElement = element as? VerseElementText {
                for span in textElement.spans {
                    var t = Text(span.text)
                    if span.style == .bold { t = t.bold() }
                    if span.style == .italic { t = t.italic() }
                    if span.style == .wordsOfJesus && state.showWordsOfJesus {
                        t = t.foregroundColor(.red)
                    }
                    views.append(t)
                }
            } else if let headingElement = element as? VerseElementHeading {
                // Simplified heading for inline display
                let headingText = headingElement.spans.map { $0.text }.joined()
                views.append(Text("\n" + headingText + "\n").bold().font(.headline))
            }
        }
        
        // Reduce into a single Text view to allow wrapping
        return views.reduce(Text(""), +)
    }
}

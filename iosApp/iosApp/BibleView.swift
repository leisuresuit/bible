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
    
    private var navigationTitleText: String {
        "\(state.currentBook.localizedName) \(state.currentChapter)"
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
                        .padding(.vertical, 0)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
                    }
                }
                .navigationTitle(navigationTitleText)
                .toolbar {
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

                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.Search()))
                        }) {
                            Label(NSLocalizedString("search", comment: ""), systemImage: "magnifyingglass")
                        }
                        .labelStyle(.iconOnly)

                        Spacer()

                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.History()))
                        }) {
                            Label(NSLocalizedString("history", comment: ""), systemImage: "clock")
                        }
                        .labelStyle(.iconOnly)

                        Spacer()

                        Button(action: {
                            viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.Settings()))
                        }) {
                            Label(NSLocalizedString("settings", comment: ""), systemImage: "gearshape")
                        }
                        .labelStyle(.iconOnly)
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
                get: {
                    let active = state.activeSheet
                    return active is ActiveSheet.PassageSelection
                },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) } }
            )) {
                if let selection = state.activeSheet as? ActiveSheet.PassageSelection {
                    PassageSelectionView(
                        allBooks: state.allBooks,
                        selectedBook: state.currentBook,
                        selectedChapter: Int(state.currentChapter),
                        initialPage: Int(selection.initialPage),
                        onSelectPassage: { book, chapter, verse in
                            viewModel.onIntent(intent: BibleIntent.SelectPassage(book: book, chapter: Int32(chapter), verse: Int32(verse)))
                        },
                        onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) }
                    )
                }
            }
            .sheet(isPresented: Binding(
                get: {
                    let active = state.activeSheet
                    return active is ActiveSheet.Settings
                },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) } }
            )) {
                SettingsView(
                    theme: state.theme,
                    displayMode: state.displayMode,
                    showWordsOfJesus: state.showWordsOfJesus,
                    onThemeChange: { viewModel.onIntent(intent: BibleIntent.UpdateTheme(theme: $0)) },
                    onDisplayModeChange: { viewModel.onIntent(intent: BibleIntent.UpdateDisplayMode(mode: $0)) },
                    onShowWordsOfJesusChange: { viewModel.onIntent(intent: BibleIntent.UpdateShowWordsOfJesus(enabled: $0)) },
                    onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) }
                )
            }
            .sheet(isPresented: Binding(
                get: {
                    let active = state.activeSheet
                    return active is ActiveSheet.History
                },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) } }
            )) {
                HistoryView(
                    history: state.history,
                    currentBook: state.currentBook,
                    currentChapter: state.currentChapter,
                    currentVerse: state.currentVerse,
                    onItemClick: { item in
                        viewModel.onIntent(intent: BibleIntent.NavigateToHistoryItem(item: item))
                        viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil))
                    },
                    onClear: { viewModel.onIntent(intent: BibleIntent.ClearHistory()) }
                )
            }
            .sheet(isPresented: Binding(
                get: {
                    let active = state.activeSheet
                    return active is ActiveSheet.VersionSelection
                },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) } }
            )) {
                VersionSelectionView(
                    versions: state.versions,
                    selectedVersions: state.selectedVersions,
                    onToggleVersion: { viewModel.onIntent(intent: BibleIntent.ToggleParallelVersion(version: $0)) },
                    onDismiss: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) }
                )
            }
            .sheet(isPresented: Binding(
                get: {
                    let active = state.activeSheet
                    return active is ActiveSheet.Search
                },
                set: { if !$0 { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) } }
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
                        viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil))
                    },
                    onBack: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: nil)) }
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

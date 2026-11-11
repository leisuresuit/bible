import SwiftUI
import ComposeApp

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
    
    // MARK: - Portrait Layout
    private var portraitLayout: some View {
        NavigationView {
            readingAreaContent
                .padding(.horizontal, 8)
                .navigationTitle(navigationTitleText)
                .navigationBarTitleDisplayMode(.inline)
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
    
    // MARK: - Landscape / iPad / Mac Layout
    private var landscapeLayout: some View {
        HStack(spacing: 0) {
            // Navigation Rail / Sidebar
            VStack(spacing: 20) {
                Button(action: {
                    viewModel.onIntent(intent: BibleIntent.ToggleSheet(sheet: ActiveSheet.PassageSelection(initialPage: 0)))
                }) {
                    VStack(spacing: 2) {
                        Text(state.currentBook.localizedName)
                            .font(.caption2)
                            .lineLimit(1)
                        Text("\(state.currentChapter)")
                            .font(.headline)
                            .bold()
                    }
                    .foregroundColor(.primary)
                }
                .padding(.top, 40)
                
                Button(action: {
                    viewModel.onIntent(intent: BibleIntent.ToggleSheet(sheet: ActiveSheet.VersionSelection.shared))
                }) {
                    Text(selectedVersionAbbreviation)
                        .font(.caption)
                        .bold()
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(Color.accentColor.opacity(0.1))
                        .cornerRadius(8)
                }

                Spacer()
                
                railIconButton(systemName: "magnifyingglass", title: NSLocalizedString("search", comment: ""), sheet: ActiveSheet.Search.shared)
                railIconButton(systemName: "clock", title: NSLocalizedString("history", comment: ""), sheet: ActiveSheet.History.shared)
                railIconButton(systemName: "gearshape", title: NSLocalizedString("settings", comment: ""), sheet: ActiveSheet.Settings.shared)
                
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
                        .frame(width: 340)
                }
                Divider()
            }
            
            // Main Content Area
            VStack(spacing: 0) {
                // Top Navigation Bar for Landscape
                HStack {
                    Spacer()
                    Text(navigationTitleText)
                        .font(.headline)
                    Spacer()
                    chapterNavigationButtons
                }
                .padding(.horizontal)
                .frame(height: 48)
                .background(.ultraThinMaterial)
                
                Divider()
                
                readingAreaContent
                    .padding(.horizontal, 8)
                    .frame(maxWidth: 650)
            }
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Reading Area (Single Chapter vs Contiguous)
    @ViewBuilder
    private var readingAreaContent: some View {
        if state.displayMode == .singleChapter {
            SingleChapterHostView(state: state, viewModel: viewModel)
        } else {
            contiguousView
        }
    }

    // MARK: - Contiguous Mode (Continuous Scroll)
    private var contiguousView: some View {
        ContiguousVerseListView(
            currentBook: state.currentBook,
            currentChapter: Int(state.currentChapter),
            currentVerse: Int(state.currentVerse),
            selectionEventId: state.selectionEventId,
            chaptersVerses: chaptersVersesMap,
            showWordsOfJesus: state.showWordsOfJesus,
            onUpdateVisiblePassage: { book, chapter, verse in
                viewModel.onIntent(intent: BibleIntent.UpdateVisiblePassage(book: book, chapter: Int32(chapter), verse: Int32(verse)))
            },
            onLoadChapterVerses: { book, chapter, globalIndex in
                viewModel.onIntent(intent: BibleIntent.LoadChapterVerses(book: book, chapter: Int32(chapter), globalIndex: Int32(globalIndex)))
            },
            onShowPassageSelection: {
                viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.PassageSelection(initialPage: 0)))
            }
        )
    }

    // MARK: - Navigation & Bottom Bar Items
    private var chapterNavigationButtons: some View {
        HStack(spacing: 8) {
            Button(action: { viewModel.onIntent(intent: BibleIntent.PreviousChapter.shared) }) {
                Image(systemName: "chevron.left")
                    .font(.body.weight(.semibold))
            }
            Button(action: { viewModel.onIntent(intent: BibleIntent.NextChapter.shared) }) {
                Image(systemName: "chevron.right")
                    .font(.body.weight(.semibold))
            }
        }
    }
    
    private var bottomBarButtons: some View {
        Group {
            Button(action: {
                viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.PassageSelection(initialPage: 0)))
            }) {
                HStack(spacing: 4) {
                    Text(navigationTitleText)
                        .bold()
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.caption2)
                }
            }
            Spacer()
            Button(action: {
                viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.VersionSelection.shared))
            }) {
                Label(selectedVersionAbbreviation, systemImage: "books.vertical")
            }
            Spacer()
            Button(action: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.Search.shared)) }) {
                Image(systemName: "magnifyingglass")
            }
            Spacer()
            Button(action: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.History.shared)) }) {
                Image(systemName: "clock")
            }
            Spacer()
            Button(action: { viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.Settings.shared)) }) {
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

    // MARK: - Helper Methods
    private var chaptersVersesMap: [Int: [Verse]] {
        var map: [Int: [Verse]] = [:]
        for (key, value) in state.chaptersVerses {
            let intKey: Int?
            if let kInt = key as? KotlinInt {
                intKey = Int(kInt.intValue)
            } else if let num = key as? NSNumber {
                intKey = num.intValue
            } else {
                intKey = nil
            }
            
            if let gIdx = intKey {
                if let vList = value as? [Verse] {
                    map[gIdx] = vList
                } else if let nsArray = value as? NSArray {
                    var verses: [Verse] = []
                    for item in nsArray {
                        if let v = item as? Verse {
                            verses.append(v)
                        }
                    }
                    map[gIdx] = verses
                } else if let anyArray = value as? [Any] {
                    var verses: [Verse] = []
                    for item in anyArray {
                        if let v = item as? Verse {
                            verses.append(v)
                        }
                    }
                    map[gIdx] = verses
                }
            }
        }
        return map
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

    // MARK: - Sheet Content View
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
                onClear: { viewModel.onIntent(intent: BibleIntent.ClearHistory.shared) },
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
}

// MARK: - Single Chapter Host View (Synced Paged TabView)
struct SingleChapterHostView: View {
    let state: BibleState
    let viewModel: BibleViewModel
    
    @State private var selectedIndex: Int
    @State private var isExternalUpdate = false

    init(state: BibleState, viewModel: BibleViewModel) {
        self.state = state
        self.viewModel = viewModel
        self._selectedIndex = State(initialValue: Book.getGlobalIndex(book: state.currentBook, chapter: Int(state.currentChapter)))
    }

    var body: some View {
        TabView(selection: $selectedIndex) {
            ForEach(max(0, selectedIndex - 2)...min(1188, selectedIndex + 2), id: \.self) { globalIndex in
                let (book, chapter) = Book.getPassageFromGlobalIndex(globalIndex)
                let verses = versesForGlobalIndex(globalIndex)
                
                SingleChapterPage(
                    book: book,
                    chapter: chapter,
                    verses: verses,
                    targetVerse: globalIndex == selectedIndex ? Int(state.currentVerse) : nil,
                    showWordsOfJesus: state.showWordsOfJesus,
                    onLoadVerses: {
                        viewModel.onIntent(intent: BibleIntent.LoadChapterVerses(book: book, chapter: Int32(chapter), globalIndex: Int32(globalIndex)))
                    },
                    onShowPassageSelection: {
                        viewModel.onIntent(intent: BibleIntent.ShowSheet(sheet: ActiveSheet.PassageSelection(initialPage: 0)))
                    }
                )
                .tag(globalIndex)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .onChange(of: Book.getGlobalIndex(book: state.currentBook, chapter: Int(state.currentChapter))) { _, newIdx in
            if selectedIndex != newIdx {
                isExternalUpdate = true
                selectedIndex = newIdx
                DispatchQueue.main.async {
                    isExternalUpdate = false
                }
            }
        }
        .onChange(of: selectedIndex) { _, newIdx in
            if !isExternalUpdate {
                let (b, c) = Book.getPassageFromGlobalIndex(newIdx)
                if b != state.currentBook || c != Int(state.currentChapter) {
                    viewModel.onIntent(intent: BibleIntent.UpdateVisiblePassage(book: b, chapter: Int32(c), verse: 1))
                }
            }
        }
    }

    private var chaptersVersesMap: [Int: [Verse]] {
        var map: [Int: [Verse]] = [:]
        for (key, value) in state.chaptersVerses {
            let intKey: Int?
            if let kInt = key as? KotlinInt {
                intKey = Int(kInt.intValue)
            } else if let num = key as? NSNumber {
                intKey = num.intValue
            } else {
                intKey = nil
            }
            
            if let gIdx = intKey {
                if let vList = value as? [Verse] {
                    map[gIdx] = vList
                } else if let nsArray = value as? NSArray {
                    var verses: [Verse] = []
                    for item in nsArray {
                        if let v = item as? Verse {
                            verses.append(v)
                        }
                    }
                    map[gIdx] = verses
                } else if let anyArray = value as? [Any] {
                    var verses: [Verse] = []
                    for item in anyArray {
                        if let v = item as? Verse {
                            verses.append(v)
                        }
                    }
                    map[gIdx] = verses
                }
            }
        }
        return map
    }

    private func versesForGlobalIndex(_ globalIndex: Int) -> [Verse]? {
        return chaptersVersesMap[globalIndex]
    }
}

// MARK: - Single Chapter Page Component
struct SingleChapterPage: View {
    let book: Book
    let chapter: Int
    let verses: [Verse]?
    let targetVerse: Int?
    let showWordsOfJesus: Bool
    let onLoadVerses: () -> Void
    let onShowPassageSelection: () -> Void

    var body: some View {
        Group {
            if let verses = verses {
                ScrollViewReader { proxy in
                    List {
                        ChapterHeaderView(book: book, chapter: chapter, onClick: onShowPassageSelection)
                            .id(0)
                        
                        ForEach(verses, id: \.number) { verse in
                            VerseItemView(verse: verse, showWordsOfJesus: showWordsOfJesus)
                                .id(Int(verse.number))
                                .listRowSeparator(.hidden)
                                .listRowInsets(EdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12))
                        }
                    }
                    .listStyle(.plain)
                    .onAppear {
                        if let target = targetVerse, target > 1 {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                                proxy.scrollTo(target, anchor: .top)
                            }
                        }
                    }
                    .onChange(of: targetVerse) { _, newTarget in
                        if let target = newTarget, target > 1 {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                                proxy.scrollTo(target, anchor: .top)
                            }
                        }
                    }
                }
            } else {
                ChapterPlaceholderView(book: book, chapter: chapter, onClick: onShowPassageSelection)
            }
        }
        .onAppear {
            onLoadVerses()
        }
    }
}

// MARK: - Contiguous Mode Continuous Scroll View
struct ContiguousVerseListView: View {
    let currentBook: Book
    let currentChapter: Int
    let currentVerse: Int
    let selectionEventId: Int64
    let chaptersVerses: [Int: [Verse]]
    let showWordsOfJesus: Bool
    let onUpdateVisiblePassage: (Book, Int, Int) -> Void
    let onLoadChapterVerses: (Book, Int, Int) -> Void
    let onShowPassageSelection: () -> Void

    @State private var lastProcessedEventId: Int64 = -1

    init(
        currentBook: Book,
        currentChapter: Int,
        currentVerse: Int,
        selectionEventId: Int64,
        chaptersVerses: [Int: [Verse]],
        showWordsOfJesus: Bool,
        onUpdateVisiblePassage: @escaping (Book, Int, Int) -> Void,
        onLoadChapterVerses: @escaping (Book, Int, Int) -> Void,
        onShowPassageSelection: @escaping () -> Void
    ) {
        self.currentBook = currentBook
        self.currentChapter = currentChapter
        self.currentVerse = currentVerse
        self.selectionEventId = selectionEventId
        self.chaptersVerses = chaptersVerses
        self.showWordsOfJesus = showWordsOfJesus
        self.onUpdateVisiblePassage = onUpdateVisiblePassage
        self.onLoadChapterVerses = onLoadChapterVerses
        self.onShowPassageSelection = onShowPassageSelection
    }

    private var targetItemIndex: Int {
        ContiguousIndex.shared.getItemIndex(book: currentBook, chapter: currentChapter, verse: currentVerse)
    }

    var body: some View {
        ScrollViewReader { proxy in
            List {
                ForEach(0..<ContiguousIndex.shared.totalItems, id: \.self) { index in
                    let location = ContiguousIndex.shared.getItemLocation(index)
                    let chapterInfo = location.chapterInfo
                    
                    if location.isHeader {
                        ChapterHeaderView(book: chapterInfo.book, chapter: chapterInfo.chapter, onClick: onShowPassageSelection)
                            .id(index)
                            .listRowSeparator(.hidden)
                            .onAppear {
                                onLoadChapterVerses(chapterInfo.book, chapterInfo.chapter, chapterInfo.globalChapterIndex)
                                if chapterInfo.book != currentBook || chapterInfo.chapter != currentChapter {
                                    DispatchQueue.main.async {
                                        onUpdateVisiblePassage(chapterInfo.book, chapterInfo.chapter, 1)
                                    }
                                }
                            }
                    } else if let vNum = location.verseNumber {
                        if let verses = chaptersVerses[chapterInfo.globalChapterIndex] {
                            let verseMatches = verses.filter { Int($0.number) == vNum }
                            if !verseMatches.isEmpty {
                                ContiguousVerseRow(verses: verseMatches, showWordsOfJesus: showWordsOfJesus)
                                    .id(index)
                                    .listRowSeparator(.hidden)
                                    .listRowInsets(EdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12))
                            }
                        } else {
                            ProgressView()
                                .frame(maxWidth: .infinity, minHeight: 32)
                                .id(index)
                                .listRowSeparator(.hidden)
                        }
                    }
                }
            }
            .listStyle(.plain)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                    proxy.scrollTo(targetItemIndex, anchor: .top)
                }
            }
            .onChange(of: selectionEventId) { _, eventId in
                if eventId > lastProcessedEventId {
                    lastProcessedEventId = eventId
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        proxy.scrollTo(targetItemIndex, anchor: .top)
                    }
                }
            }
        }
    }
}

// MARK: - Contiguous Verse Row Component
struct ContiguousVerseRow: View {
    let verses: [Verse]
    let showWordsOfJesus: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(Array(verses.enumerated()), id: \.offset) { _, verse in
                VerseItemView(verse: verse, showWordsOfJesus: showWordsOfJesus)
            }
        }
    }
}

// MARK: - Chapter Header View
struct ChapterHeaderView: View {
    let book: Book
    let chapter: Int
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text("\(book.localizedName) \(chapter)")
                .font(.system(.title, design: .serif))
                .bold()
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Single Spinner Chapter Placeholder View (Matches KMP VerseListPlaceholder)
struct ChapterPlaceholderView: View {
    let book: Book
    let chapter: Int
    let onClick: () -> Void

    var body: some View {
        VStack {
            ChapterHeaderView(book: book, chapter: chapter, onClick: onClick)
            Spacer()
            ProgressView()
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Verse Item View
struct VerseItemView: View {
    let verse: Verse
    let showWordsOfJesus: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let abbr = verse.versionAbbreviation, !abbr.isEmpty {
                Text(abbr)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

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
                        
                        if isJesus && showWordsOfJesus {
                            t = t.foregroundColor(.red)
                        }
                        return result + t
                    }
                    
                    if index == verse.elements.firstIndex(where: { $0 is VerseElementText }) {
                        (Text("\(verse.number)  ").bold() + combinedText)
                            .font(.system(.body, design: .serif))
                            .lineSpacing(4)
                            .textSelection(.enabled)
                    } else {
                        combinedText
                            .font(.system(.body, design: .serif))
                            .lineSpacing(4)
                            .textSelection(.enabled)
                    }
                } else if let headingElement = element as? VerseElementHeading {
                    let headingText = headingElement.spans.map { $0.text }.joined()
                    Text(headingText)
                        .bold()
                        .font(.headline)
                        .padding(.top, index == 0 ? 4 : 16)
                        .padding(.bottom, 6)
                }
            }
        }
    }
}

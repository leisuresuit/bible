import SwiftUI
import ComposeApp

enum PassageStep: Int, CaseIterable, Identifiable {
    case book = 0
    case chapter = 1
    case verse = 2
    
    var id: Int { rawValue }
    
    var title: String {
        switch self {
        case .book: return NSLocalizedString("book", comment: "")
        case .chapter: return NSLocalizedString("chapters", comment: "")
        case .verse: return NSLocalizedString("verses", comment: "")
        }
    }
}

struct PassageSelectionView: View {
    let allBooks: [Book]
    let selectedBook: Book?
    let selectedChapter: Int
    let initialPage: Int
    let onSelectPassage: (Book, Int, Int) -> Void
    let onDismiss: () -> Void
    let isSidePanel: Bool
    
    @State private var selectedStep: PassageStep
    @State private var internalBook: Book
    @State private var internalChapter: Int
    @State private var internalVerse: Int
    @State private var searchQuery: String = ""
    
    init(
        allBooks: [Book],
        selectedBook: Book?,
        selectedChapter: Int,
        initialPage: Int,
        onSelectPassage: @escaping (Book, Int, Int) -> Void,
        onDismiss: @escaping () -> Void,
        isSidePanel: Bool = false
    ) {
        self.allBooks = allBooks.isEmpty ? Book.allBooksList : allBooks
        self.selectedBook = selectedBook
        self.selectedChapter = selectedChapter
        self.initialPage = initialPage
        self.onSelectPassage = onSelectPassage
        self.onDismiss = onDismiss
        self.isSidePanel = isSidePanel
        
        let initialStep = PassageStep(rawValue: initialPage) ?? .book
        let defaultBook = selectedBook ?? .genesis
        self._selectedStep = State(initialValue: initialStep)
        self._internalBook = State(initialValue: defaultBook)
        self._internalChapter = State(initialValue: selectedChapter)
        self._internalVerse = State(initialValue: 1)
    }
    
    private var filteredBooks: [Book] {
        let q = searchQuery.trimmingCharacters(in: .whitespaces)
        if q.isEmpty {
            return allBooks
        } else {
            return allBooks.filter { $0.localizedName.localizedCaseInsensitiveContains(q) }
        }
    }
    
    private var oldTestamentBooks: [Book] {
        filteredBooks.filter { $0.isOldTestament }
    }
    
    private var newTestamentBooks: [Book] {
        filteredBooks.filter { !$0.isOldTestament }
    }
    
    private var titleText: String {
        switch selectedStep {
        case .book:
            return NSLocalizedString("book", comment: "")
        case .chapter:
            return internalBook.localizedName
        case .verse:
            return "\(internalBook.localizedName) \(internalChapter)"
        }
    }

    var body: some View {
        if isSidePanel {
            mainContent
        } else {
            NavigationView {
                mainContent
            }
        }
    }
    
    private var mainContent: some View {
        VStack(spacing: 0) {
            if isSidePanel {
                HStack {
                    Text(titleText)
                        .font(.headline)
                    Spacer()
                    stepNavigationButtons
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.body.weight(.semibold))
                            .foregroundColor(.secondary)
                    }
                    .padding(.leading, 8)
                }
                .padding()
                Divider()
            }
            
            // Segmented Step Picker
            Picker("Step", selection: $selectedStep) {
                ForEach(PassageStep.allCases) { step in
                    Text(step.title).tag(step)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            .padding(.top, 12)
            .padding(.bottom, 8)
            
            // Search / Filter Field
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                TextField(searchPlaceholder, text: $searchQuery)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                if !searchQuery.isEmpty {
                    Button(action: { searchQuery = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(8)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
            .padding(.horizontal)
            .padding(.bottom, 8)
            
            Divider()
            
            // Step Content
            switch selectedStep {
            case .book:
                bookSelectionContent
            case .chapter:
                chapterSelectionContent
            case .verse:
                verseSelectionContent
            }
        }
        .navigationTitle(titleText)
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                if !isSidePanel {
                    stepNavigationButtons
                }
            }
        }
        .onChange(of: selectedStep) { _, _ in
            searchQuery = ""
        }
    }
    
    private var searchPlaceholder: String {
        switch selectedStep {
        case .book: return NSLocalizedString("search", comment: "")
        case .chapter: return NSLocalizedString("chapters", comment: "")
        case .verse: return NSLocalizedString("verses", comment: "")
        }
    }
    
    private var stepNavigationButtons: some View {
        HStack(spacing: 12) {
            Button(action: {
                if let prev = PassageStep(rawValue: selectedStep.rawValue - 1) {
                    selectedStep = prev
                }
            }) {
                Image(systemName: "chevron.left")
            }
            .disabled(selectedStep == .book)

            Button(action: {
                if let next = PassageStep(rawValue: selectedStep.rawValue + 1) {
                    selectedStep = next
                }
            }) {
                Image(systemName: "chevron.right")
            }
            .disabled(selectedStep == .verse)
        }
    }
    
    // MARK: - Book Selection
    private var bookSelectionContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                if !oldTestamentBooks.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("OLD TESTAMENT")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.secondary)
                            .padding(.horizontal)
                        
                        bookGrid(books: oldTestamentBooks)
                    }
                }
                
                if !newTestamentBooks.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("NEW TESTAMENT")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.secondary)
                            .padding(.horizontal)
                        
                        bookGrid(books: newTestamentBooks)
                    }
                }
            }
            .padding(.vertical)
        }
    }
    
    private func bookGrid(books: [Book]) -> some View {
        let columns = [GridItem(.adaptive(minimum: 100, maximum: 160), spacing: 8)]
        return LazyVGrid(columns: columns, spacing: 8) {
            ForEach(books, id: \.name) { book in
                let isSelected = internalBook == book
                Button(action: {
                    internalBook = book
                    internalChapter = 1
                    internalVerse = 1
                    if book.chaptersCount == 1 {
                        selectedStep = .verse
                    } else {
                        selectedStep = .chapter
                    }
                }) {
                    Text(book.localizedName)
                        .font(.subheadline)
                        .fontWeight(isSelected ? .bold : .regular)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .background(isSelected ? Color.accentColor : Color(.secondarySystemGroupedBackground))
                        .foregroundColor(isSelected ? .white : .primary)
                        .cornerRadius(8)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal)
    }
    
    // MARK: - Chapter Selection
    private var chapterSelectionContent: some View {
        let totalChapters = Int(internalBook.chaptersCount)
        let chapters = Array(1...totalChapters)
        let filtered = searchQuery.isEmpty ? chapters : chapters.filter { "\($0)".contains(searchQuery) }
        let columns = [GridItem(.adaptive(minimum: 56, maximum: 72), spacing: 8)]
        
        return ScrollView {
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(filtered, id: \.self) { chapter in
                    let isSelected = internalChapter == chapter
                    Button(action: {
                        internalChapter = chapter
                        internalVerse = 1
                        selectedStep = .verse
                    }) {
                        Text("\(chapter)")
                            .font(.callout)
                            .fontWeight(isSelected ? .bold : .regular)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(isSelected ? Color.accentColor : Color(.secondarySystemGroupedBackground))
                            .foregroundColor(isSelected ? .white : .primary)
                            .cornerRadius(8)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding()
        }
    }
    
    // MARK: - Verse Selection
    private var verseSelectionContent: some View {
        let cIdx = max(0, min(internalChapter - 1, Int(internalBook.chaptersCount) - 1))
        let verseCount = Int(truncating: internalBook.versesInChapters[cIdx])
        let verses = Array(1...max(1, verseCount))
        let filtered = searchQuery.isEmpty ? verses : verses.filter { "\($0)".contains(searchQuery) }
        let columns = [GridItem(.adaptive(minimum: 56, maximum: 72), spacing: 8)]
        
        return ScrollView {
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(filtered, id: \.self) { verse in
                    let isSelected = internalVerse == verse
                    Button(action: {
                        internalVerse = verse
                        onSelectPassage(internalBook, internalChapter, verse)
                    }) {
                        Text("\(verse)")
                            .font(.callout)
                            .fontWeight(isSelected ? .bold : .regular)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(isSelected ? Color.accentColor : Color(.secondarySystemGroupedBackground))
                            .foregroundColor(isSelected ? .white : .primary)
                            .cornerRadius(8)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding()
        }
    }
}

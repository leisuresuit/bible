import SwiftUI
import ComposeApp

struct PassageSelectionView: View {
    let allBooks: [Book]
    let selectedBook: Book?
    let selectedChapter: Int
    let initialPage: Int
    let onSelectPassage: (Book, Int, Int) -> Void
    let onDismiss: () -> Void
    
    @State private var currentPage: Int
    @State private var internalSelectedBook: Book?
    @State private var internalSelectedChapter: Int?
    @State private var internalSelectedVerse: Int?
    @State private var searchQuery: String = ""
    
    var filteredBooks: [Book] {
        if searchQuery.isEmpty {
            return allBooks
        } else {
            return allBooks.filter { $0.localizedName.localizedCaseInsensitiveContains(searchQuery) }
        }
    }
    
    private var navigationTitleText: String {
        switch currentPage {
        case 0:
            return NSLocalizedString("book", comment: "")
        case 1:
            return internalSelectedBook?.localizedName ?? NSLocalizedString("chapter", comment: "")
        case 2:
            if let bookName = internalSelectedBook?.localizedName {
                if let chapter = internalSelectedChapter {
                    return "\(bookName) \(chapter)"
                }
                return bookName
            }
            return NSLocalizedString("verse", comment: "")
        default:
            return ""
        }
    }

    private var searchPlaceholder: String {
        switch currentPage {
        case 0: return NSLocalizedString("search", comment: "")
        case 1: return NSLocalizedString("chapter", comment: "")
        case 2: return NSLocalizedString("verse", comment: "")
        default: return ""
        }
    }
    
    init(allBooks: [Book], selectedBook: Book?, selectedChapter: Int, initialPage: Int, onSelectPassage: @escaping (Book, Int, Int) -> Void, onDismiss: @escaping () -> Void) {
        self.allBooks = allBooks
        self.selectedBook = selectedBook
        self.selectedChapter = selectedChapter
        self.initialPage = initialPage
        self.onSelectPassage = onSelectPassage
        self.onDismiss = onDismiss
        self._currentPage = State(initialValue: initialPage)
        self._internalSelectedBook = State(initialValue: selectedBook)
        self._internalSelectedChapter = State(initialValue: selectedChapter)
        self._internalSelectedVerse = State(initialValue: 1)
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField(searchPlaceholder, text: $searchQuery)
                    if !searchQuery.isEmpty {
                        Button(action: {
                            searchQuery = ""
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.gray)
                        }
                    }
                }
                .padding(10)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top, 16)
                .padding(.bottom, 8)
                
                Divider()
                
                if currentPage == 0 {
                    BookGridView(allBooks: filteredBooks, selectedBook: internalSelectedBook) { book in
                        if internalSelectedBook != book {
                            internalSelectedBook = book
                            internalSelectedChapter = 1
                            internalSelectedVerse = 1
                        }
                        if book.chaptersCount == 1 {
                            currentPage = 2
                        } else {
                            currentPage = 1
                        }
                    }
                } else if currentPage == 1 {
                    ChapterGridView(book: internalSelectedBook ?? .genesis, searchQuery: searchQuery, selectedChapter: internalSelectedChapter) { chapter in
                        if internalSelectedChapter != chapter {
                            internalSelectedChapter = chapter
                            internalSelectedVerse = 1
                        }
                        currentPage = 2
                    }
                } else {
                    VerseGridView(book: internalSelectedBook ?? .genesis, chapter: internalSelectedChapter ?? 1, searchQuery: searchQuery, selectedVerse: internalSelectedVerse) { verse in
                        internalSelectedVerse = verse
                        if let book = internalSelectedBook, let chapter = internalSelectedChapter {
                            onSelectPassage(book, chapter, verse)
                        }
                    }
                }
            }
            .navigationTitle(navigationTitleText)
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button(action: {
                        if currentPage > 0 {
                            currentPage -= 1
                        }
                    }) {
                        Image(systemName: "chevron.left")
                    }
                    .disabled(currentPage == 0)

                    Button(action: {
                        if currentPage < 2 {
                            currentPage += 1
                        }
                    }) {
                        Image(systemName: "chevron.right")
                    }
                    .disabled(!((currentPage == 0 && internalSelectedBook != nil) || (currentPage == 1 && internalSelectedChapter != nil)))
                }
            }
            .onChange(of: currentPage) { _, _ in
                searchQuery = ""
            }
        }
    }
}

struct BookGridView: View {
    let allBooks: [Book]
    let selectedBook: Book?
    let onBookSelect: (Book) -> Void
    
    let columns = [GridItem(.adaptive(minimum: 100))]
    
    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(allBooks, id: \.name) { book in
                    Button(action: { onBookSelect(book) }) {
                        Text(book.localizedName)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(selectedBook == book ? Color.accentColor : Color.secondary.opacity(0.1))
                            .foregroundColor(selectedBook == book ? .white : .primary)
                            .cornerRadius(8)
                    }
                }
            }
            .padding()
        }
    }
}

struct ChapterGridView: View {
    let book: Book
    let searchQuery: String
    let selectedChapter: Int?
    let onChapterSelect: (Int) -> Void
    
    let columns = [GridItem(.adaptive(minimum: 50))]
    
    var body: some View {
        let chapters = Array(1...Int(book.chaptersCount))
        let filteredChapters = searchQuery.isEmpty ? chapters : chapters.filter { "\($0)".contains(searchQuery) }
        
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(filteredChapters, id: \.self) { chapter in
                    Button(action: { onChapterSelect(chapter) }) {
                        Text("\(chapter)")
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(selectedChapter == chapter ? Color.accentColor : Color.secondary.opacity(0.1))
                            .foregroundColor(selectedChapter == chapter ? .white : .primary)
                            .cornerRadius(8)
                    }
                }
            }
            .padding()
        }
    }
}

struct VerseGridView: View {
    let book: Book
    let chapter: Int
    let searchQuery: String
    let selectedVerse: Int?
    let onVerseSelect: (Int) -> Void
    
    let columns = [GridItem(.adaptive(minimum: 50))]
    
    var body: some View {
        let versesCount = Int(truncating: book.versesInChapters[chapter - 1])
        let verses = Array(1...versesCount)
        let filteredVerses = searchQuery.isEmpty ? verses : verses.filter { "\($0)".contains(searchQuery) }
        
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(filteredVerses, id: \.self) { verse in
                    Button(action: { onVerseSelect(verse) }) {
                        Text("\(verse)")
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(selectedVerse == verse ? Color.accentColor : Color.secondary.opacity(0.1))
                            .foregroundColor(selectedVerse == verse ? .white : .primary)
                            .cornerRadius(8)
                    }
                }
            }
            .padding()
        }
    }
}

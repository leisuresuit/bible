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
            VStack {
                Picker("Selection", selection: $currentPage) {
                    Text(NSLocalizedString("books", comment: "")).tag(0)
                    Text(NSLocalizedString("chapters", comment: "")).tag(1)
                    Text(NSLocalizedString("verses", comment: "")).tag(2)
                }
                .pickerStyle(.segmented)
                .padding()
                
                if currentPage == 0 {
                    BookGridView(allBooks: allBooks, selectedBook: internalSelectedBook) { book in
                        internalSelectedBook = book
                        currentPage = 1
                    }
                } else if currentPage == 1 {
                    ChapterGridView(book: internalSelectedBook ?? .genesis, selectedChapter: internalSelectedChapter) { chapter in
                        internalSelectedChapter = chapter
                        currentPage = 2
                    }
                } else {
                    VerseGridView(book: internalSelectedBook ?? .genesis, chapter: internalSelectedChapter ?? 1, selectedVerse: internalSelectedVerse) { verse in
                        internalSelectedVerse = verse
                        if let book = internalSelectedBook, let chapter = internalSelectedChapter {
                            onSelectPassage(book, chapter, verse)
                        }
                    }
                }
            }
            .navigationTitle(NSLocalizedString("select_passage", comment: ""))
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
    let selectedChapter: Int?
    let onChapterSelect: (Int) -> Void
    
    let columns = [GridItem(.adaptive(minimum: 50))]
    
    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(1...Int(book.chaptersCount), id: \.self) { chapter in
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
    let selectedVerse: Int?
    let onVerseSelect: (Int) -> Void
    
    let columns = [GridItem(.adaptive(minimum: 50))]
    
    var body: some View {
        ScrollView {
            let versesCount = Int(truncating: book.versesInChapters[chapter - 1] as! NSNumber)
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(1...versesCount, id: \.self) { verse in
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

import SwiftUI
import ComposeApp

struct HistoryView: View {
    let history: [HistoryItem]
    let currentBook: Book?
    let currentChapter: Int32
    let currentVerse: Int32?
    let onItemClick: (HistoryItem) -> Void
    let onClear: () -> Void

    private var currentIndex: Int? {
        history.firstIndex(where: {
            $0.book == currentBook && $0.chapter == currentChapter && $0.verse == currentVerse
        })
    }

    var body: some View {
        NavigationView {
            List {
                ForEach(history, id: \.self) { item in
                    let isSameBook = item.book == currentBook
                    let isSameChapter = item.chapter == currentChapter
                    let isSameVerse = item.verse == currentVerse
                    let isSelected = isSameBook && isSameChapter && isSameVerse
                    
                    Button(action: { onItemClick(item) }) {
                        HStack {
                            let passage = "\(item.book.localizedName) \(item.chapter):\(item.verse)"
                            Text(passage)
                                .fontWeight(isSelected ? .bold : .regular)
                                .foregroundColor(isSelected ? .accentColor : .primary)
                            Spacer()
                            if isSelected {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }
            .navigationTitle(NSLocalizedString("history", comment: ""))
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button(NSLocalizedString("clear", comment: "")) {
                        onClear()
                    }
                    
                    Button(action: {
                        if let index = currentIndex, index < history.count - 1 {
                            onItemClick(history[index + 1])
                        }
                    }) {
                        Image(systemName: "chevron.left")
                    }
                    .disabled(currentIndex == nil || currentIndex == history.count - 1)

                    Button(action: {
                        if let index = currentIndex, index > 0 {
                            onItemClick(history[index - 1])
                        }
                    }) {
                        Image(systemName: "chevron.right")
                    }
                    .disabled(currentIndex == nil || currentIndex == 0)
                }
            }
        }
    }
}

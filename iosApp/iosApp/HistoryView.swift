import SwiftUI
import ComposeApp

struct HistoryView: View {
    let history: [HistoryItem]
    let currentBook: Book?
    let currentChapter: Int32
    let currentVerse: Int32?
    let onDismiss: () -> Void
    let onItemClick: (HistoryItem) -> Void
    let onClear: () -> Void

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
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(NSLocalizedString("clear", comment: "")) {
                        onClear()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(NSLocalizedString("close", comment: "")) {
                        onDismiss()
                    }
                }
            }
        }
    }
}

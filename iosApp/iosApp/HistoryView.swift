import SwiftUI
import ComposeApp

struct HistoryView: View {
    let history: [HistoryItem]
    let currentBook: Book?
    let currentChapter: Int32
    let currentVerse: Int32?
    let onItemClick: (HistoryItem) -> Void
    let onClear: () -> Void
    let isSidePanel: Bool

    private var currentIndex: Int? {
        history.firstIndex(where: {
            $0.book == currentBook && $0.chapter == currentChapter && $0.verse == currentVerse
        })
    }
    
    init(history: [HistoryItem], currentBook: Book?, currentChapter: Int32, currentVerse: Int32?, onItemClick: @escaping (HistoryItem) -> Void, onClear: @escaping () -> Void, isSidePanel: Bool = false) {
        self.history = history
        self.currentBook = currentBook
        self.currentChapter = currentChapter
        self.currentVerse = currentVerse
        self.onItemClick = onItemClick
        self.onClear = onClear
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
        List {
            ForEach(history, id: \.self) { item in
                let isSelected = item.book == currentBook && item.chapter == currentChapter && item.verse == currentVerse
                
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
                if !isSidePanel {
                    headerActions
                }
            }
        }
        .overlay(alignment: .top) {
            if isSidePanel {
                VStack(spacing: 0) {
                    HStack {
                        Text(NSLocalizedString("history", comment: ""))
                            .font(.headline)
                        Spacer()
                        headerActions
                    }
                    .padding()
                    Divider()
                }
                .background(Color(.systemBackground))
            }
        }
    }
    
    private var headerActions: some View {
        HStack {
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

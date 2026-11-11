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
    
    init(
        history: [HistoryItem],
        currentBook: Book?,
        currentChapter: Int32,
        currentVerse: Int32?,
        onItemClick: @escaping (HistoryItem) -> Void,
        onClear: @escaping () -> Void,
        isSidePanel: Bool = false
    ) {
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
                    Text(NSLocalizedString("history", comment: ""))
                        .font(.headline)
                    Spacer()
                    headerActions
                }
                .padding()
                Divider()
            }

            if history.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "clock")
                        .font(.system(size: 48))
                        .foregroundColor(.secondary)
                    Text(NSLocalizedString("history", comment: ""))
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            } else {
                List {
                    ForEach(history, id: \.self) { item in
                        let isSelected = item.book == currentBook && item.chapter == currentChapter && (currentVerse == nil || item.verse == currentVerse)
                        
                        Button(action: { onItemClick(item) }) {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("\(item.book.localizedName) \(item.chapter):\(item.verse)")
                                        .font(.body)
                                        .fontWeight(isSelected ? .bold : .regular)
                                        .foregroundColor(isSelected ? .accentColor : .primary)
                                }
                                Spacer()
                                if isSelected {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.accentColor)
                                        .font(.system(size: 14, weight: .bold))
                                }
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .listStyle(.plain)
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
    }
    
    private var headerActions: some View {
        HStack(spacing: 12) {
            Button(NSLocalizedString("clear", comment: "")) {
                onClear()
            }
            .foregroundColor(.red)
            .disabled(history.isEmpty)
            
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

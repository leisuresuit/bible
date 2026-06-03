import SwiftUI
import ComposeApp

struct SearchView: View {
    @Binding var searchQuery: String
    let searchResults: [SearchResult]
    let searchSort: SearchSort
    let isLoading: Bool
    let onSearchQueryChange: (String) -> Void
    let onSearchSortChange: (SearchSort) -> Void
    let onResultClick: (SearchResult) -> Void
    let onBack: () -> Void
    let isSidePanel: Bool

    init(searchQuery: Binding<String>, searchResults: [SearchResult], searchSort: SearchSort, isLoading: Bool, onSearchQueryChange: @escaping (String) -> Void, onSearchSortChange: @escaping (SearchSort) -> Void, onResultClick: @escaping (SearchResult) -> Void, onBack: @escaping () -> Void, isSidePanel: Bool = false) {
        self._searchQuery = searchQuery
        self.searchResults = searchResults
        self.searchSort = searchSort
        self.isLoading = isLoading
        self.onSearchQueryChange = onSearchQueryChange
        self.onSearchSortChange = onSearchSortChange
        self.onResultClick = onResultClick
        self.onBack = onBack
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
        VStack(spacing: 0) {
            if isSidePanel {
                HStack {
                    Text(NSLocalizedString("search", comment: ""))
                        .font(.headline)
                    Spacer()
                }
                .padding()
            }
            
            VStack {
                HStack {
                    if !isSidePanel {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                    }
                    
                    TextField(NSLocalizedString("search", comment: ""), text: $searchQuery)
                        .onChange(of: searchQuery) { _, newValue in
                            onSearchQueryChange(newValue)
                        }
                    if isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                    } else if !searchQuery.isEmpty {
                        Button(action: {
                            searchQuery = ""
                            onSearchQueryChange("")
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

                Divider()

                Picker("Sort", selection: Binding(
                    get: { searchSort },
                    set: { onSearchSortChange($0) }
                )) {
                    Text(NSLocalizedString("sort_relevance", comment: "")).tag(SearchSort.relevance)
                    Text(NSLocalizedString("sort_canonical", comment: "")).tag(SearchSort.canonical)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                List(searchResults, id: \.id) { result in
                    Button(action: { onResultClick(result) }) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(result.book.localizedName) \(result.chapterNumber):\(result.verseNumber)")
                                .font(.headline)
                                .foregroundColor(.secondary)
                            highlightedText(text: result.text, query: searchQuery)
                                .font(.body)
                                .lineLimit(5)
                        }
                    }
                }
            }
        }
        .navigationTitle(NSLocalizedString("search", comment: ""))
    }
    
    private func highlightedText(text: String, query: String) -> Text {
        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedQuery.isEmpty, trimmedQuery.count >= 3 else {
            return Text(text)
        }
        
        var attributedString = AttributedString(text)
        let lowercasedText = text.lowercased()
        let lowercasedQuery = trimmedQuery.lowercased()
        
        var searchRange = lowercasedText.startIndex..<lowercasedText.endIndex
        while let range = lowercasedText.range(of: lowercasedQuery, range: searchRange) {
            if let start = AttributedString.Index(range.lowerBound, within: attributedString),
               let end = AttributedString.Index(range.upperBound, within: attributedString) {
                attributedString[start..<end].backgroundColor = Color.accentColor.opacity(0.2)
                attributedString[start..<end].inlinePresentationIntent = .stronglyEmphasized
            }
            searchRange = range.upperBound..<lowercasedText.endIndex
        }
        
        return Text(attributedString)
    }
}

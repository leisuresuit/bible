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

    init(
        searchQuery: Binding<String>,
        searchResults: [SearchResult],
        searchSort: SearchSort,
        isLoading: Bool,
        onSearchQueryChange: @escaping (String) -> Void,
        onSearchSortChange: @escaping (SearchSort) -> Void,
        onResultClick: @escaping (SearchResult) -> Void,
        onBack: @escaping () -> Void,
        isSidePanel: Bool = false
    ) {
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
                    Text(NSLocalizedString("search", comment: ""))
                        .font(.headline)
                    Spacer()
                    Button(action: onBack) {
                        Image(systemName: "xmark")
                            .font(.body.weight(.semibold))
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                Divider()
            }
            
            VStack(spacing: 8) {
                // Search Field
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    
                    TextField(NSLocalizedString("search", comment: ""), text: $searchQuery)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
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
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(8)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top, 12)

                // Sort Segmented Control
                Picker("Sort", selection: Binding(
                    get: { searchSort },
                    set: { onSearchSortChange($0) }
                )) {
                    Text(NSLocalizedString("sort_relevance", comment: "")).tag(SearchSort.relevance)
                    Text(NSLocalizedString("sort_canonical", comment: "")).tag(SearchSort.canonical)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                Divider()

                if searchResults.isEmpty && !isLoading && searchQuery.trimmingCharacters(in: .whitespaces).count >= 3 {
                    VStack(spacing: 12) {
                        Spacer()
                        Image(systemName: "doc.text.magnifyingglass")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text(NSLocalizedString("no_results_found", comment: ""))
                            .font(.headline)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                } else {
                    List(searchResults, id: \.id) { result in
                        Button(action: { onResultClick(result) }) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(result.book.localizedName) \(result.chapterNumber):\(result.verseNumber)")
                                    .font(.subheadline)
                                    .fontWeight(.bold)
                                    .foregroundColor(.accentColor)
                                
                                highlightedText(text: result.text, query: searchQuery)
                                    .font(.body)
                                    .lineLimit(5)
                                    .multilineTextAlignment(.leading)
                            }
                            .padding(.vertical, 4)
                        }
                        .buttonStyle(.plain)
                    }
                    .listStyle(.plain)
                }
            }
        }
        .navigationTitle(NSLocalizedString("search", comment: ""))
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if !isSidePanel {
                    Button(NSLocalizedString("done", comment: "")) {
                        onBack()
                    }
                    .bold()
                }
            }
        }
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

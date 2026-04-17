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

    var body: some View {
        NavigationView {
            VStack {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField(NSLocalizedString("search", comment: ""), text: $searchQuery)
                        .onChange(of: searchQuery) {
                            onSearchQueryChange(searchQuery)
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
                            Text(result.text)
                                .font(.body)
                                .lineLimit(3)
                        }
                    }
                }
            }
            .navigationTitle(NSLocalizedString("search", comment: ""))
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                        Text(NSLocalizedString("back", comment: ""))
                    }
                }
            }
        }
    }
}

import SwiftUI
import ComposeApp

struct VersionSelectionView: View {
    let versions: [BibleVersion]
    let selectedVersions: [BibleVersion]
    let onToggleVersion: (BibleVersion) -> Void
    let onDismiss: () -> Void

    @State private var searchQuery: String = ""

    var filteredVersions: [BibleVersion] {
        if searchQuery.isEmpty {
            return versions.sorted {
                $0.abbreviation < $1.abbreviation
            }
        } else {
            return versions.filter {
                $0.name.localizedCaseInsensitiveContains(searchQuery) ||
                    $0.abbreviation.localizedCaseInsensitiveContains(searchQuery)
            }
            .sorted {
                $0.abbreviation < $1.abbreviation
            }
        }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField(NSLocalizedString("search", comment: ""), text: $searchQuery)
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
                .padding()

                Divider()

                List {
                    ForEach(filteredVersions, id: \.id) { version in
                        let isSelected = selectedVersions.contains(where: { $0.id == version.id })

                        Button(action: {
                            onToggleVersion(version)
                        }) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(version.abbreviation)
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    Text(version.name)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
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
                .navigationTitle(NSLocalizedString("versions", comment: ""))
            }
        }
    }
}

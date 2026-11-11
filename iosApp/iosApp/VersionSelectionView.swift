import SwiftUI
import ComposeApp

struct VersionSelectionView: View {
    let versions: [BibleVersion]
    let selectedVersions: [BibleVersion]
    let onToggleVersion: (BibleVersion) -> Void
    let onDismiss: () -> Void
    let isSidePanel: Bool

    @State private var searchQuery: String = ""

    var filteredVersions: [BibleVersion] {
        let sorted = versions.sorted { $0.abbreviation < $1.abbreviation }
        let q = searchQuery.trimmingCharacters(in: .whitespaces)
        if q.isEmpty {
            return sorted
        } else {
            return sorted.filter {
                $0.name.localizedCaseInsensitiveContains(q) ||
                $0.abbreviation.localizedCaseInsensitiveContains(q)
            }
        }
    }
    
    init(
        versions: [BibleVersion],
        selectedVersions: [BibleVersion],
        onToggleVersion: @escaping (BibleVersion) -> Void,
        onDismiss: @escaping () -> Void,
        isSidePanel: Bool = false
    ) {
        self.versions = versions
        self.selectedVersions = selectedVersions
        self.onToggleVersion = onToggleVersion
        self.onDismiss = onDismiss
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
                    Text(NSLocalizedString("versions", comment: ""))
                        .font(.headline)
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.body.weight(.semibold))
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                Divider()
            }

            // Search Bar
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                TextField(NSLocalizedString("search", comment: ""), text: $searchQuery)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                if !searchQuery.isEmpty {
                    Button(action: { searchQuery = "" }) {
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
            .padding(.bottom, 8)

            Divider()

            List {
                ForEach(filteredVersions, id: \.id) { version in
                    let isSelected = selectedVersions.contains(where: { $0.id == version.id })

                    Button(action: {
                        onToggleVersion(version)
                    }) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
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
            .listStyle(.plain)
        }
        .navigationTitle(NSLocalizedString("versions", comment: ""))
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if !isSidePanel {
                    Button(NSLocalizedString("done", comment: "")) {
                        onDismiss()
                    }
                    .bold()
                }
            }
        }
    }
}

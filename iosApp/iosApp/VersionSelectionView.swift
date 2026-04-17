import SwiftUI
import ComposeApp

struct VersionSelectionView: View {
    let versions: [BibleVersion]
    let selectedVersions: [BibleVersion]
    let onToggleVersion: (BibleVersion) -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        NavigationView {
            List {
                ForEach(versions, id: \.id) { version in
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
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(NSLocalizedString("done", comment: "")) {
                        onDismiss()
                    }
                }
            }
        }
    }
}

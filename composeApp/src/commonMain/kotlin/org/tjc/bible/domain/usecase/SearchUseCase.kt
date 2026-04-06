package org.tjc.bible.domain.usecase

import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.repository.BibleRepository

class SearchUseCase(private val repository: BibleRepository) {
    suspend operator fun invoke(versionId: String, query: String): Result<List<SearchResult>> {
        return repository.search(versionId, query)
    }
}

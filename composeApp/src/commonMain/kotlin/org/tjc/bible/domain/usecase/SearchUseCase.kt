package org.tjc.bible.domain.usecase

import org.tjc.bible.domain.model.SearchResult
import org.tjc.bible.domain.repository.BibleRepository

import org.tjc.bible.domain.model.SearchResponse
import org.tjc.bible.domain.model.SearchSort

class SearchUseCase(private val repository: BibleRepository) {
    suspend operator fun invoke(
        versionId: String,
        query: String,
        offset: Int = 0,
        limit: Int = 20,
        sort: SearchSort = SearchSort.RELEVANCE
    ): Result<SearchResponse> {
        return repository.search(versionId, query, offset, limit, sort)
    }
}

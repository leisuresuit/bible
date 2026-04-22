package org.tjc.bible.domain.usecase

import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.repository.BibleRepository

class GetBibleVersionsUseCase(private val repository: BibleRepository) {
    suspend operator fun invoke(languages: List<String> = emptyList()): Result<List<BibleVersion>> {
        return repository.getVersions(languages)
    }
}

package org.tjc.bible.domain.usecase

import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.Verse
import org.tjc.bible.domain.repository.BibleRepository

class GetVersesUseCase(private val repository: BibleRepository) {
    suspend operator fun invoke(versionId: String, book: Book, chapter: Int): List<Verse> {
        return repository.getVerses(versionId, book, chapter)
    }
}

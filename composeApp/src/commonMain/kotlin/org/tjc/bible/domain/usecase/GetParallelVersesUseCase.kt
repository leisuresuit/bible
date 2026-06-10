package org.tjc.bible.domain.usecase

import org.tjc.bible.domain.model.BibleVersion
import org.tjc.bible.domain.model.Book
import org.tjc.bible.domain.model.Verse

class GetParallelVersesUseCase(private val getVersesUseCase: GetVersesUseCase) {
    suspend operator fun invoke(selectedVersions: List<BibleVersion>, book: Book, chapter: Int): Result<List<Verse>> {
        val allVerses = mutableListOf<Verse>()
        if (selectedVersions.size > 1) {
            val results = selectedVersions.map { version ->
                getVersesUseCase(version.id, book, chapter).map { verses ->
                    verses.map { it.copy(versionAbbreviation = version.abbreviation) }
                }
            }

            results.find { it.isFailure }?.let { return it }

            val versesByVersion = results.map { it.getOrThrow() }

            if (versesByVersion.isNotEmpty()) {
                val maxVerseCount = versesByVersion.maxOf { it.size }
                for (i in 0 until maxVerseCount) {
                    versesByVersion.forEach { versionVerses ->
                        if (i < versionVerses.size) {
                            allVerses.add(versionVerses[i])
                        }
                    }
                }
            }
        } else if (selectedVersions.isNotEmpty()) {
            val result = getVersesUseCase(selectedVersions.first().id, book, chapter)
            if (result.isFailure) return result
            allVerses.addAll(result.getOrThrow())
        }
        return Result.success(allVerses)
    }
}

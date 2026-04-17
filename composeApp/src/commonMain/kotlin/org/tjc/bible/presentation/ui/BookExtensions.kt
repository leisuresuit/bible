package org.tjc.bible.presentation.ui

import bible.composeapp.generated.resources.Res
import bible.composeapp.generated.resources.book_1chronicles
import bible.composeapp.generated.resources.book_1corinthians
import bible.composeapp.generated.resources.book_1john
import bible.composeapp.generated.resources.book_1kings
import bible.composeapp.generated.resources.book_1peter
import bible.composeapp.generated.resources.book_1samuel
import bible.composeapp.generated.resources.book_1thessalonians
import bible.composeapp.generated.resources.book_1timothy
import bible.composeapp.generated.resources.book_2chronicles
import bible.composeapp.generated.resources.book_2corinthians
import bible.composeapp.generated.resources.book_2john
import bible.composeapp.generated.resources.book_2kings
import bible.composeapp.generated.resources.book_2peter
import bible.composeapp.generated.resources.book_2samuel
import bible.composeapp.generated.resources.book_2thessalonians
import bible.composeapp.generated.resources.book_2timothy
import bible.composeapp.generated.resources.book_3john
import bible.composeapp.generated.resources.book_acts
import bible.composeapp.generated.resources.book_amos
import bible.composeapp.generated.resources.book_colossians
import bible.composeapp.generated.resources.book_daniel
import bible.composeapp.generated.resources.book_deuteronomy
import bible.composeapp.generated.resources.book_ecclesiastes
import bible.composeapp.generated.resources.book_ephesians
import bible.composeapp.generated.resources.book_esther
import bible.composeapp.generated.resources.book_exodus
import bible.composeapp.generated.resources.book_ezekiel
import bible.composeapp.generated.resources.book_ezra
import bible.composeapp.generated.resources.book_galatians
import bible.composeapp.generated.resources.book_genesis
import bible.composeapp.generated.resources.book_habakkuk
import bible.composeapp.generated.resources.book_haggai
import bible.composeapp.generated.resources.book_hebrews
import bible.composeapp.generated.resources.book_hosea
import bible.composeapp.generated.resources.book_isaiah
import bible.composeapp.generated.resources.book_james
import bible.composeapp.generated.resources.book_jeremiah
import bible.composeapp.generated.resources.book_job
import bible.composeapp.generated.resources.book_joel
import bible.composeapp.generated.resources.book_john
import bible.composeapp.generated.resources.book_jonah
import bible.composeapp.generated.resources.book_joshua
import bible.composeapp.generated.resources.book_jude
import bible.composeapp.generated.resources.book_judges
import bible.composeapp.generated.resources.book_lamentations
import bible.composeapp.generated.resources.book_leviticus
import bible.composeapp.generated.resources.book_luke
import bible.composeapp.generated.resources.book_malachi
import bible.composeapp.generated.resources.book_mark
import bible.composeapp.generated.resources.book_matthew
import bible.composeapp.generated.resources.book_micah
import bible.composeapp.generated.resources.book_nahum
import bible.composeapp.generated.resources.book_nehemiah
import bible.composeapp.generated.resources.book_numbers
import bible.composeapp.generated.resources.book_obadiah
import bible.composeapp.generated.resources.book_philemon
import bible.composeapp.generated.resources.book_philippians
import bible.composeapp.generated.resources.book_proverbs
import bible.composeapp.generated.resources.book_psalms
import bible.composeapp.generated.resources.book_revelation
import bible.composeapp.generated.resources.book_romans
import bible.composeapp.generated.resources.book_ruth
import bible.composeapp.generated.resources.book_song_of_solomon
import bible.composeapp.generated.resources.book_titus
import bible.composeapp.generated.resources.book_zechariah
import bible.composeapp.generated.resources.book_zephaniah
import org.jetbrains.compose.resources.StringResource
import org.tjc.bible.domain.model.Book

val Book.nameResource: StringResource
    get() = when (this) {
        Book.Genesis -> Res.string.book_genesis
        Book.Exodus -> Res.string.book_exodus
        Book.Leviticus -> Res.string.book_leviticus
        Book.Numbers -> Res.string.book_numbers
        Book.Deuteronomy -> Res.string.book_deuteronomy
        Book.Joshua -> Res.string.book_joshua
        Book.Judges -> Res.string.book_judges
        Book.Ruth -> Res.string.book_ruth
        Book.Samuel1 -> Res.string.book_1samuel
        Book.Samuel2 -> Res.string.book_2samuel
        Book.Kings1 -> Res.string.book_1kings
        Book.Kings2 -> Res.string.book_2kings
        Book.Chronicles1 -> Res.string.book_1chronicles
        Book.Chronicles2 -> Res.string.book_2chronicles
        Book.Ezra -> Res.string.book_ezra
        Book.Nehemiah -> Res.string.book_nehemiah
        Book.Esther -> Res.string.book_esther
        Book.Job -> Res.string.book_job
        Book.Psalms -> Res.string.book_psalms
        Book.Proverbs -> Res.string.book_proverbs
        Book.Ecclesiastes -> Res.string.book_ecclesiastes
        Book.SongOfSolomon -> Res.string.book_song_of_solomon
        Book.Isaiah -> Res.string.book_isaiah
        Book.Jeremiah -> Res.string.book_jeremiah
        Book.Lamentations -> Res.string.book_lamentations
        Book.Ezekiel -> Res.string.book_ezekiel
        Book.Daniel -> Res.string.book_daniel
        Book.Hosea -> Res.string.book_hosea
        Book.Joel -> Res.string.book_joel
        Book.Amos -> Res.string.book_amos
        Book.Obadiah -> Res.string.book_obadiah
        Book.Jonah -> Res.string.book_jonah
        Book.Micah -> Res.string.book_micah
        Book.Nahum -> Res.string.book_nahum
        Book.Habakkuk -> Res.string.book_habakkuk
        Book.Zephaniah -> Res.string.book_zephaniah
        Book.Haggai -> Res.string.book_haggai
        Book.Zechariah -> Res.string.book_zechariah
        Book.Malachi -> Res.string.book_malachi
        Book.Matthew -> Res.string.book_matthew
        Book.Mark -> Res.string.book_mark
        Book.Luke -> Res.string.book_luke
        Book.John -> Res.string.book_john
        Book.Acts -> Res.string.book_acts
        Book.Romans -> Res.string.book_romans
        Book.Corinthians1 -> Res.string.book_1corinthians
        Book.Corinthians2 -> Res.string.book_2corinthians
        Book.Galatians -> Res.string.book_galatians
        Book.Ephesians -> Res.string.book_ephesians
        Book.Philippians -> Res.string.book_philippians
        Book.Colossians -> Res.string.book_colossians
        Book.Thessalonians1 -> Res.string.book_1thessalonians
        Book.Thessalonians2 -> Res.string.book_2thessalonians
        Book.Timothy1 -> Res.string.book_1timothy
        Book.Timothy2 -> Res.string.book_2timothy
        Book.Titus -> Res.string.book_titus
        Book.Philemon -> Res.string.book_philemon
        Book.Hebrews -> Res.string.book_hebrews
        Book.James -> Res.string.book_james
        Book.Peter1 -> Res.string.book_1peter
        Book.Peter2 -> Res.string.book_2peter
        Book.John1 -> Res.string.book_1john
        Book.John2 -> Res.string.book_2john
        Book.John3 -> Res.string.book_3john
        Book.Jude -> Res.string.book_jude
        Book.Revelation -> Res.string.book_revelation
    }

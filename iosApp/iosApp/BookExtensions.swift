import SwiftUI
import ComposeApp

extension Book {
    static var allBooksList: [Book] {
        return [
            .genesis, .exodus, .leviticus, .numbers, .deuteronomy, .joshua, .judges, .ruth,
            .samuel1, .samuel2, .kings1, .kings2, .chronicles1, .chronicles2, .ezra, .nehemiah,
            .esther, .job, .psalms, .proverbs, .ecclesiastes, .songOfSolomon, .isaiah, .jeremiah,
            .lamentations, .ezekiel, .daniel, .hosea, .joel, .amos, .obadiah, .jonah, .micah,
            .nahum, .habakkuk, .zephaniah, .haggai, .zechariah, .malachi, .matthew, .mark,
            .luke, .john, .acts, .romans, .corinthians1, .corinthians2, .galatians, .ephesians,
            .philippians, .colossians, .thessalonians1, .thessalonians2, .timothy1, .timothy2,
            .titus, .philemon, .hebrews, .james, .peter1, .peter2, .john1, .john2, .john3,
            .jude, .revelation
        ]
    }

    var localizedName: String {
        let resourceKey: String
        switch self {
        case .genesis: resourceKey = "book_genesis"
        case .exodus: resourceKey = "book_exodus"
        case .leviticus: resourceKey = "book_leviticus"
        case .numbers: resourceKey = "book_numbers"
        case .deuteronomy: resourceKey = "book_deuteronomy"
        case .joshua: resourceKey = "book_joshua"
        case .judges: resourceKey = "book_judges"
        case .ruth: resourceKey = "book_ruth"
        case .samuel1: resourceKey = "book_1samuel"
        case .samuel2: resourceKey = "book_2samuel"
        case .kings1: resourceKey = "book_1kings"
        case .kings2: resourceKey = "book_2kings"
        case .chronicles1: resourceKey = "book_1chronicles"
        case .chronicles2: resourceKey = "book_2chronicles"
        case .ezra: resourceKey = "book_ezra"
        case .nehemiah: resourceKey = "book_nehemiah"
        case .esther: resourceKey = "book_esther"
        case .job: resourceKey = "book_job"
        case .psalms: resourceKey = "book_psalms"
        case .proverbs: resourceKey = "book_proverbs"
        case .ecclesiastes: resourceKey = "book_ecclesiastes"
        case .songOfSolomon: resourceKey = "book_song_of_solomon"
        case .isaiah: resourceKey = "book_isaiah"
        case .jeremiah: resourceKey = "book_jeremiah"
        case .lamentations: resourceKey = "book_lamentations"
        case .ezekiel: resourceKey = "book_ezekiel"
        case .daniel: resourceKey = "book_daniel"
        case .hosea: resourceKey = "book_hosea"
        case .joel: resourceKey = "book_joel"
        case .amos: resourceKey = "book_amos"
        case .obadiah: resourceKey = "book_obadiah"
        case .jonah: resourceKey = "book_jonah"
        case .micah: resourceKey = "book_micah"
        case .nahum: resourceKey = "book_nahum"
        case .habakkuk: resourceKey = "book_habakkuk"
        case .zephaniah: resourceKey = "book_zephaniah"
        case .haggai: resourceKey = "book_haggai"
        case .zechariah: resourceKey = "book_zechariah"
        case .malachi: resourceKey = "book_malachi"
        case .matthew: resourceKey = "book_matthew"
        case .mark: resourceKey = "book_mark"
        case .luke: resourceKey = "book_luke"
        case .john: resourceKey = "book_john"
        case .acts: resourceKey = "book_acts"
        case .romans: resourceKey = "book_romans"
        case .corinthians1: resourceKey = "book_1corinthians"
        case .corinthians2: resourceKey = "book_2corinthians"
        case .galatians: resourceKey = "book_galatians"
        case .ephesians: resourceKey = "book_ephesians"
        case .philippians: resourceKey = "book_philippians"
        case .colossians: resourceKey = "book_colossians"
        case .thessalonians1: resourceKey = "book_1thessalonians"
        case .thessalonians2: resourceKey = "book_2thessalonians"
        case .timothy1: resourceKey = "book_1timothy"
        case .timothy2: resourceKey = "book_2timothy"
        case .titus: resourceKey = "book_titus"
        case .philemon: resourceKey = "book_philemon"
        case .hebrews: resourceKey = "book_hebrews"
        case .james: resourceKey = "book_james"
        case .peter1: resourceKey = "book_1peter"
        case .peter2: resourceKey = "book_2peter"
        case .john1: resourceKey = "book_1john"
        case .john2: resourceKey = "book_2john"
        case .john3: resourceKey = "book_3john"
        case .jude: resourceKey = "book_jude"
        case .revelation: resourceKey = "book_revelation"
        @unknown default:
            return self.name
        }
        return NSLocalizedString(resourceKey, comment: "")
    }

    var isOldTestament: Bool {
        return self.ordinal < Book.matthew.ordinal
    }

    static func getGlobalIndex(book: Book, chapter: Int) -> Int {
        let books = Book.allBooksList
        var sum = 0
        for b in books {
            if b == book { break }
            sum += Int(b.chaptersCount)
        }
        return sum + (chapter - 1)
    }

    static func getPassageFromGlobalIndex(_ globalIndex: Int) -> (Book, Int) {
        let books = Book.allBooksList
        var cumulativeSum = 0
        var cumulative: [Int] = []
        for b in books {
            cumulative.append(cumulativeSum)
            cumulativeSum += Int(b.chaptersCount)
        }
        
        let clampedIndex = max(0, min(globalIndex, max(0, cumulativeSum - 1)))
        var bookIndex = 0
        for (idx, start) in cumulative.enumerated() {
            if start <= clampedIndex {
                bookIndex = idx
            }
        }
        let book = books.indices.contains(bookIndex) ? books[bookIndex] : Book.genesis
        let start = cumulative.indices.contains(bookIndex) ? cumulative[bookIndex] : 0
        let chapter = clampedIndex - start + 1
        return (book, chapter)
    }
}

struct ContiguousChapterInfo {
    let book: Book
    let chapter: Int
    let globalChapterIndex: Int
    let startItemIndex: Int
    let verseCount: Int
}

struct ContiguousItemLocation {
    let chapterInfo: ContiguousChapterInfo
    let verseNumber: Int?
    let isHeader: Bool
}

class ContiguousIndex {
    static let shared = ContiguousIndex()
    
    let chapters: [ContiguousChapterInfo]
    let totalItems: Int
    
    private init() {
        var list: [ContiguousChapterInfo] = []
        var globalChapterIndex = 0
        var currentItemIndex = 0
        
        let books = Book.allBooksList
        for book in books {
            for cIndex in 0..<Int(book.chaptersCount) {
                let chapterNumber = cIndex + 1
                let verseCount = Int(truncating: book.versesInChapters[cIndex])
                let info = ContiguousChapterInfo(
                    book: book,
                    chapter: chapterNumber,
                    globalChapterIndex: globalChapterIndex,
                    startItemIndex: currentItemIndex,
                    verseCount: verseCount
                )
                list.append(info)
                globalChapterIndex += 1
                currentItemIndex += 1 + verseCount
            }
        }
        self.chapters = list
        self.totalItems = currentItemIndex
    }
    
    func getChapterInfo(_ globalChapterIndex: Int) -> ContiguousChapterInfo {
        let idx = max(0, min(globalChapterIndex, max(0, chapters.count - 1)))
        return chapters[idx]
    }
    
    func findChapterForGlobalIndex(book: Book, chapter: Int) -> ContiguousChapterInfo {
        let globalIndex = Book.getGlobalIndex(book: book, chapter: chapter)
        return getChapterInfo(globalIndex)
    }
    
    func getItemIndex(book: Book, chapter: Int, verse: Int? = nil) -> Int {
        let info = findChapterForGlobalIndex(book: book, chapter: chapter)
        let v = min(max(verse ?? 1, 1), info.verseCount)
        return info.startItemIndex + v
    }
    
    func getItemLocation(_ itemIndex: Int) -> ContiguousItemLocation {
        guard !chapters.isEmpty else {
            return ContiguousItemLocation(
                chapterInfo: ContiguousChapterInfo(book: .genesis, chapter: 1, globalChapterIndex: 0, startItemIndex: 0, verseCount: 31),
                verseNumber: 1,
                isHeader: false
            )
        }
        let clampedIndex = max(0, min(itemIndex, max(0, totalItems - 1)))
        var low = 0
        var high = chapters.count - 1
        var foundChapter = chapters[0]
        
        while low <= high {
            let mid = (low + high) / 2
            let info = chapters[mid]
            let endItemIndex = info.startItemIndex + info.verseCount
            if clampedIndex < info.startItemIndex {
                high = mid - 1
            } else if clampedIndex > endItemIndex {
                low = mid + 1
            } else {
                foundChapter = info
                break
            }
        }
        
        let offset = clampedIndex - foundChapter.startItemIndex
        let isHeader = (offset == 0)
        let verseNumber = isHeader ? nil : offset
        return ContiguousItemLocation(chapterInfo: foundChapter, verseNumber: verseNumber, isHeader: isHeader)
    }
}

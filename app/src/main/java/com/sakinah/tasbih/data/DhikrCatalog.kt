package com.sakinah.tasbih.data

import java.text.Normalizer

data class DhikrEntry(
    val id: String,
    val collectionId: String,
    val text: String,
    val repetitions: Int,
    val reference: String,
)

data class DhikrCollection(
    val id: String,
    val order: Int,
    val title: String,
    val audioUrl: String?,
    val entries: List<DhikrEntry>,
)

data class HisnCatalog(
    val languageTag: String,
    val collections: List<DhikrCollection>,
) {
    val totalDhikr: Int = collections.sumOf { it.entries.size }

    fun collection(id: String): DhikrCollection? = collections.firstOrNull { it.id == id }

    fun search(query: String): List<DhikrCollection> {
        val normalizedQuery = normalizeArabicForSearch(query)
        if (normalizedQuery.isBlank()) return collections

        return collections.filter { collection ->
            normalizeArabicForSearch(collection.title).contains(normalizedQuery) ||
                collection.entries.any { entry ->
                    normalizeArabicForSearch(entry.text).contains(normalizedQuery) ||
                        normalizeArabicForSearch(entry.reference).contains(normalizedQuery)
                }
        }
    }

    companion object {
        val Empty = HisnCatalog(languageTag = "ar", collections = emptyList())
    }
}

data class ReadingProgress(
    val entryIndex: Int = 0,
    val repetitionCounts: Map<Int, Int> = emptyMap(),
    val completedEntryIndices: Set<Int> = emptySet(),
    val completed: Boolean = false,
) {
    val repetitionCount: Int
        get() = repetitionCountFor(entryIndex)

    fun repetitionCountFor(index: Int): Int = repetitionCounts[index]?.coerceAtLeast(0) ?: 0

    fun isEntryCompleted(index: Int, collection: DhikrCollection): Boolean {
        if (completed) return index in collection.entries.indices
        val entry = collection.entries.getOrNull(index) ?: return false
        return index in completedEntryIndices || repetitionCountFor(index) >= entry.repetitions
    }

    fun completedEntries(collection: DhikrCollection): Int = collection.entries.indices.count { index ->
        isEntryCompleted(index, collection)
    }

    fun fraction(collection: DhikrCollection): Float {
        if (collection.entries.isEmpty()) return 0f
        if (completed) return 1f

        val completedFraction = collection.entries.indices.fold(0f) { total, index ->
            val entry = collection.entries[index]
            val entryFraction = if (isEntryCompleted(index, collection)) {
                1f
            } else {
                repetitionCountFor(index).toFloat() / entry.repetitions.coerceAtLeast(1)
            }
            total + entryFraction.coerceIn(0f, 1f)
        }
        return (completedFraction / collection.entries.size).coerceIn(0f, 1f)
    }
}

data class TasbihPhrase(
    val id: String,
    val text: String,
    val defaultGoal: Int,
    val isCustom: Boolean = false,
)

enum class DhikrGroup(val firstOrder: Int, val lastOrder: Int) {
    DailyLife(1, 15),
    Prayer(16, 26),
    MorningEvening(27, 34),
    ReliefAndWellbeing(35, 61),
    Occasions(62, 95),
    Travel(96, 107),
    SocialAndVirtues(108, 115),
    HajjAndUmrah(116, 122),
    GeneralGood(123, 133),
    ;

    companion object {
        fun forOrder(order: Int): DhikrGroup = entries.firstOrNull {
            order in it.firstOrder..it.lastOrder
        } ?: GeneralGood
    }
}

object DhikrCatalog {
    val builtInTasbihPhrases = listOf(
        TasbihPhrase(id = "subhan_allah", text = "سُبْحَانَ اللَّهِ", defaultGoal = 33),
        TasbihPhrase(id = "alhamdulillah", text = "الْحَمْدُ لِلَّهِ", defaultGoal = 33),
        TasbihPhrase(id = "allahu_akbar", text = "اللَّهُ أَكْبَرُ", defaultGoal = 34),
        TasbihPhrase(
            id = "la_ilaha_illa_allah",
            text = "لَا إِلَهَ إِلَّا اللَّهُ",
            defaultGoal = 100,
        ),
        TasbihPhrase(id = "astaghfirullah", text = "أَسْتَغْفِرُ اللَّهَ", defaultGoal = 100),
        TasbihPhrase(
            id = "hawqala",
            text = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            defaultGoal = 100,
        ),
    )

    fun phraseFor(id: String, customPhrases: List<TasbihPhrase>): TasbihPhrase =
        (builtInTasbihPhrases + customPhrases).firstOrNull { it.id == id }
            ?: builtInTasbihPhrases.first()
}

/**
 * Removes the vowel marks and the tatweel for reading without tashkeel. The hamza and madda
 * are part of their letters and stay: \u0623 \u0625 \u0622 \u0624 \u0626 are kept whole, and so are a hamza or madda
 * written as a separate mark (U+0653 to U+0655).
 */
fun stripArabicDiacritics(text: String): String = text.filterNot(::isArabicDiacritic)

private fun isArabicDiacritic(character: Char): Boolean = when (character) {
    '\u0640', // tatweel
    in '\u0610'..'\u061a', // honorific and Quranic signs
    in '\u064b'..'\u0652', // tanween, the short vowels, shadda and sukun
    in '\u0656'..'\u065f',
    '\u0670', // the dagger alif
    in '\u06d6'..'\u06dc', // Quranic annotation marks
    in '\u06df'..'\u06e4',
    '\u06e7', '\u06e8',
    in '\u06ea'..'\u06ed',
    -> true
    else -> false
}

/** For matching only: every mark goes, the hamza and madda with them, so \u00ab\u0627\u0643\u0628\u0631\u00bb finds \u00ab\u0623\u064e\u0643\u0652\u0628\u064e\u0631\u064f\u00bb. */
private fun stripAllArabicMarks(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .filterNot { character ->
        Character.getType(character) == Character.NON_SPACING_MARK.toInt() ||
            character == '\u0640'
    }

fun normalizeArabicForSearch(text: String): String = stripAllArabicMarks(text)
    .lowercase()
    .replace(Regex("[أإآٱ]"), "ا")
    .replace('ى', 'ي')
    .replace('ؤ', 'و')
    .replace('ئ', 'ي')
    .replace('ة', 'ه')
    .replace(Regex("[^\u0621-\u063A\u0641-\u064A0-9 ]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

fun displayArabic(text: String, showDiacritics: Boolean): String =
    if (showDiacritics) text else stripArabicDiacritics(text)

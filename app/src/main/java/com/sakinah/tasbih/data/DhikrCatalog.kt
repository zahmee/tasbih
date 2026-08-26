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
    val repetitionCount: Int = 0,
    val completed: Boolean = false,
) {
    fun completedEntries(collection: DhikrCollection): Int = when {
        completed -> collection.entries.size
        collection.entries.isEmpty() -> 0
        else -> entryIndex.coerceIn(0, collection.entries.lastIndex)
    }

    fun fraction(collection: DhikrCollection): Float {
        if (collection.entries.isEmpty()) return 0f
        if (completed) return 1f

        val safeIndex = entryIndex.coerceIn(0, collection.entries.lastIndex)
        val entry = collection.entries[safeIndex]
        val entryFraction = repetitionCount.toFloat() / entry.repetitions.coerceAtLeast(1)
        return ((safeIndex + entryFraction.coerceIn(0f, 1f)) / collection.entries.size)
            .coerceIn(0f, 1f)
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

fun stripArabicDiacritics(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .filterNot { character ->
        Character.getType(character) == Character.NON_SPACING_MARK.toInt() ||
            character == '\u0640'
    }

fun normalizeArabicForSearch(text: String): String = stripArabicDiacritics(text)
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

package com.sakinah.tasbih.data

data class DhikrSearchResult(
    val collection: DhikrCollection,
    val entry: DhikrEntry,
    val excerpt: String,
    val matchedReference: Boolean = false,
)

fun HisnCatalog.searchEntries(query: String, group: DhikrGroup? = null): List<DhikrSearchResult> {
    val needle = normalizeArabicForSearch(query)
    return collections.asSequence()
        .filter { group == null || DhikrGroup.forOrder(it.order) == group }
        .flatMap { collection ->
            val titleMatches = needle.isNotEmpty() && normalizeArabicForSearch(collection.title).contains(needle)
            collection.entries.asSequence().mapNotNull { entry ->
                val textMatches = needle.isEmpty() || normalizeArabicForSearch(entry.text).contains(needle)
                val referenceMatches = needle.isNotEmpty() && normalizeArabicForSearch(entry.reference).contains(needle)
                if (!textMatches && !referenceMatches && !titleMatches) null else {
                    val referenceOnly = referenceMatches && !textMatches && !titleMatches
                    DhikrSearchResult(collection, entry, dhikrExcerpt(if (referenceOnly) entry.reference else entry.text, query), referenceOnly)
                }
            }
        }.toList()
}

/** Keeps original Arabic and diacritics, locating the excerpt by normalized words. */
fun dhikrExcerpt(text: String, query: String = "", length: Int = 180): String {
    if (text.length <= length) return text
    val normalized = StringBuilder()
    val originalOffsets = mutableListOf<Int>()
    Regex("\\S+").findAll(text).forEach { word ->
        val value = normalizeArabicForSearch(word.value)
        if (value.isNotBlank()) {
            if (normalized.isNotEmpty()) {
                normalized.append(' ')
                originalOffsets.add(word.range.first)
            }
            normalized.append(value)
            repeat(value.length) { originalOffsets.add(word.range.first) }
        }
    }
    val needle = normalizeArabicForSearch(query)
    val match = if (needle.isBlank()) 0 else normalized.indexOf(needle).coerceAtLeast(0)
    val matchOffset = originalOffsets.getOrElse(match) { 0 }
    val roughStart = (matchOffset - 35).coerceAtLeast(0)
    val start = if (roughStart == 0) 0 else text.lastIndexOf(' ', roughStart).coerceAtLeast(0)
    val end = (start + maxOf(length, query.length + 70)).coerceAtMost(text.length)
    return (if (start > 0) "… " else "") + text.substring(start, end).trim() + if (end < text.length) " …" else ""
}

package com.sakinah.tasbih.data

import org.junit.Assert.*
import org.junit.Test

class DhikrSearchTest {
    private val one = DhikrEntry("one", "hisn_001", "الحمد لله الذي أحيانا", 1, "البخاري")
    private val two = DhikrEntry("two", "hisn_027", "أَذْكَارُ الصَّبَاحِ وما بعدها", 3, "مسلم")
    private val catalog = HisnCatalog("ar", listOf(
        DhikrCollection("hisn_001", 1, "الاستيقاظ", null, listOf(one)),
        DhikrCollection("hisn_027", 27, "الصباح", null, listOf(two)),
    ))
    @Test fun resultNamesTheExactEntryAndRespectsScope() {
        assertEquals("two", catalog.searchEntries("اذكار الصباح").single().entry.id)
        assertTrue(catalog.searchEntries("اذكار الصباح", DhikrGroup.DailyLife).isEmpty())
        assertEquals("one", catalog.searchEntries("الاستيقاظ").single().entry.id)
    }
    @Test fun referenceOnlyMatchIsExplained() {
        val result = catalog.searchEntries("البخاري").single()
        assertTrue(result.matchedReference)
        assertEquals("البخاري", result.excerpt)
    }
    @Test fun excerptFindsLateMatchWithoutStrippingTheOriginalArabic() {
        val text = "الحمد لله ".repeat(55) + "وَسُبْحَانَ اللهِ وبحمده " + "الحمد لله ".repeat(30)
        val excerpt = dhikrExcerpt(text, "وسبحان الله")
        assertTrue(excerpt.contains("وَسُبْحَانَ اللهِ"))
        assertTrue(excerpt.startsWith("…"))
        assertTrue(excerpt.length < text.length)
        assertEquals("نص قصير", dhikrExcerpt("نص قصير"))
    }
    @Test fun undoReceiptRoundTripsCountsAndRejectsCorruptValues() {
        val progress = ReadingProgress(2, mapOf(0 to 1, 2 to 8), setOf(0), false)
        val receipt = ReaderUndo(progress, 42, 43, "2026-09-11")
        val restored = decodeReaderUndo(setOf(receipt.encode("hisn_001"), "broken", "a|b|-2|0|invalid"))
        assertEquals(mapOf("hisn_001" to receipt), restored)
    }
}

package com.sakinah.tasbih.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DhikrCatalogTest {
    @Test
    fun `bundled hisn catalog contains the complete 133 chapters and 302 entries`() {
        val source = File("src/main/assets/hisn_ar.json").readText()

        assertEquals(133, Regex("\\\"Audio\\\"").findAll(source).count())
        assertEquals(302, Regex("\\\"Text\\\"").findAll(source).count())
        assertEquals(302, Regex("\\\"Count\\\"").findAll(source).count())
        assertEquals(302, Regex("\\\"Reference\\\"").findAll(source).count())
    }

    @Test
    fun `arabic search ignores diacritics and alef forms`() {
        assertEquals("اذكار الصباح", normalizeArabicForSearch("أَذْكَارُ الصَّبَاحِ"))
        assertEquals(
            normalizeArabicForSearch("إلى المسجد"),
            normalizeArabicForSearch("الى المَسْجِدِ"),
        )
    }

    @Test
    fun `all book orders map to a focused library group`() {
        val mapped = (1..133).map(DhikrGroup::forOrder)

        assertEquals(133, mapped.size)
        assertTrue(mapped.all { it in DhikrGroup.entries })
        assertEquals(DhikrGroup.MorningEvening, DhikrGroup.forOrder(27))
        assertEquals(DhikrGroup.HajjAndUmrah, DhikrGroup.forOrder(120))
    }

    @Test
    fun `built in tasbih phrases have unique ids and valid goals`() {
        val phrases = DhikrCatalog.builtInTasbihPhrases

        assertEquals(phrases.size, phrases.map(TasbihPhrase::id).distinct().size)
        assertTrue(phrases.all { it.text.isNotBlank() && it.defaultGoal > 0 })
    }

    @Test
    fun `reading progress accounts for repetitions inside an entry`() {
        val collection = DhikrCollection(
            id = "test",
            order = 1,
            title = "اختبار",
            audioUrl = null,
            entries = listOf(
                DhikrEntry("one", "test", "ذكر", 2, "مرجع"),
                DhikrEntry("two", "test", "ذكر", 1, "مرجع"),
            ),
        )

        assertEquals(0.25f, ReadingProgress(repetitionCount = 1).fraction(collection), 0.001f)
        assertEquals(1f, ReadingProgress(completed = true).fraction(collection), 0.001f)
    }
}

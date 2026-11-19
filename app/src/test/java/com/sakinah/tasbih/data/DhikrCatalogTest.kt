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

        assertEquals(
            0.25f,
            ReadingProgress(repetitionCounts = mapOf(0 to 1)).fraction(collection),
            0.001f,
        )
        assertEquals(1f, ReadingProgress(completed = true).fraction(collection), 0.001f)
    }

    @Test
    fun `manual navigation does not count skipped entries as completed`() {
        val collection = DhikrCollection(
            id = "test",
            order = 1,
            title = "اختبار",
            audioUrl = null,
            entries = List(3) { index ->
                DhikrEntry("entry_$index", "test", "ذكر", 1, "")
            },
        )

        val navigatedProgress = ReadingProgress(entryIndex = 2)

        assertEquals(0, navigatedProgress.completedEntries(collection))
        assertEquals(0f, navigatedProgress.fraction(collection), 0.001f)
        assertEquals(0, navigatedProgress.repetitionCount)
    }

    @Test
    fun `completion follows counted entries even when read out of order`() {
        val collection = DhikrCollection(
            id = "test",
            order = 1,
            title = "اختبار",
            audioUrl = null,
            entries = List(3) { index ->
                DhikrEntry("entry_$index", "test", "ذكر", 1, "")
            },
        )
        val progress = ReadingProgress(
            entryIndex = 2,
            repetitionCounts = mapOf(2 to 1),
            completedEntryIndices = setOf(2),
        )

        assertEquals(1, progress.completedEntries(collection))
        assertEquals(1f / 3f, progress.fraction(collection), 0.001f)
        assertTrue(progress.isEntryCompleted(2, collection))
    }

    @Test
    fun `hiding the tashkeel keeps the hamza and the madda`() {
        assertEquals("الله أكبر", stripArabicDiacritics("اللَّهُ أَكْبَرُ"))
        assertEquals("لا إله إلا الله", stripArabicDiacritics("لَا إِلَٰهَ إِلَّا اللَّهُ"))
        assertEquals("القرآن", stripArabicDiacritics("الْقُرْآنِ"))
        assertEquals("يؤمنون بالله وملائكته", stripArabicDiacritics("يُؤْمِنُونَ بِاللَّهِ وَمَلَائِكَتِهِ"))
        assertEquals("سبحان الله", stripArabicDiacritics("سُبْحَـانَ اللهِ"))
        // A hamza written as a separate mark is still a letter's hamza.
        assertEquals("ا\u0654كبر", stripArabicDiacritics("ا\u0654\u064Eكبر"))
        assertEquals("أَكْبَرُ", displayArabic("أَكْبَرُ", showDiacritics = true))
        // Search still ignores the hamza, so a query typed without it finds the text.
        assertEquals("اكبر", normalizeArabicForSearch("أَكْبَرُ"))
    }
}

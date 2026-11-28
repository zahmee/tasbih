package com.sakinah.tasbih.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each prayer widget's own look, and the one look of before kept for the widgets already placed. */
class PerWidgetStyleTest {
    /** A store in memory: DataStore's own file cannot be renamed over on Windows outside Android. */
    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val lock = Mutex()
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            lock.withLock { transform(state.value).also { state.value = it } }
    }

    private val store = MemoryStore()
    private val repository = PrayerWidgetStyleRepository(store)

    private val navy = PrayerWidgetStyle(surface = WidgetSurface.NAVY, accent = WidgetAccent.GOLD, lines = listOf(WidgetLine(LineItem.WEEKDAY)))
    private val rose = PrayerWidgetStyle(surface = WidgetSurface.ROSE, opacity = 40, ornament = WidgetOrnament.WINGS)

    @Test fun `each widget keeps a look of its own`() = runBlocking {
        repository.save(7, navy)
        repository.save(9, rose)
        assertEquals(navy, repository.current(7))
        assertEquals(rose, repository.current(9))
        assertEquals(setOf(7, 9), repository.styledIds())
    }

    @Test fun `the look of before is kept for the widgets already placed`() = runBlocking {
        // As saved by 2.0.4, when all widgets shared one look under keys without an id.
        store.edit {
            it[stringPreferencesKey("day_surface")] = "NAVY"
            it[stringPreferencesKey("day_accent")] = "GOLD"
            it[stringPreferencesKey("day_lines")] = "WEEKDAY|NONE|AUTO"
        }
        assertEquals(navy, repository.current(3))
        val styles = repository.stylesFor(listOf(3, 4))
        assertEquals(navy, styles[3])
        assertEquals(navy, styles[4])
        assertEquals(setOf(3, 4), repository.styledIds())
        // Each is now its own: a later look for one, or a new default, leaves the other as it was.
        repository.save(3, rose)
        assertEquals(rose, repository.current(3))
        assertEquals(navy, repository.current(4))
    }

    @Test fun `a widget placed later starts from the look edited last`() = runBlocking {
        repository.stylesFor(listOf(1))
        repository.save(1, rose)
        assertEquals(rose, repository.current())
        assertEquals(rose, repository.stylesFor(listOf(1, 2))[2])
    }

    @Test fun `with nothing saved every widget has the default look`() = runBlocking {
        assertEquals(PrayerWidgetStyle(), repository.current(5))
        assertEquals(PrayerWidgetStyle(), repository.stylesFor(listOf(5))[5])
    }

    @Test fun `a widget taken off the home screen takes its look with it`() = runBlocking {
        repository.save(7, navy)
        repository.save(8, rose)
        repository.remove(listOf(7))
        assertEquals(setOf(8), repository.styledIds())
        // Its id read again falls back to the default, the look edited last.
        assertEquals(rose, repository.current(7))
    }

    @Test fun `applying to all gives every widget the same look`() = runBlocking {
        repository.save(1, navy)
        repository.save(2, PrayerWidgetStyle())
        repository.saveAll(listOf(1, 2), rose)
        assertEquals(rose, repository.current(1))
        assertEquals(rose, repository.current(2))
    }

    @Test fun `after a restore each look moves to its widget's new id`() = runBlocking {
        repository.save(10, navy)
        repository.save(11, rose)
        repository.save(null, PrayerWidgetStyle())
        // The ids swap and shift: an old id that is also another widget's new one is not lost.
        repository.remap(intArrayOf(10, 11, 12), intArrayOf(11, 20, 21))
        assertEquals(navy, repository.current(11))
        assertEquals(rose, repository.current(20))
        assertEquals(setOf(11, 20), repository.styledIds())
        // The old id 12 had no look of its own, so its new widget takes the default.
        assertEquals(PrayerWidgetStyle(), repository.current(21))
        assertTrue(10 !in repository.styledIds())
    }
}

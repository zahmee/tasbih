package com.sakinah.tasbih.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TasbihPhraseOrderTest {
    @Test fun existingPreferencesKeepTheirOriginalOrderUntilTheUserChangesIt() {
        val custom = TasbihPhrase("custom_existing", "ذكر خاص", 33, true)
        val preferences = AppPreferences(customPhrases = listOf(custom))
        assertEquals(DhikrCatalog.builtInTasbihPhrases + custom, preferences.tasbihPhrases)
    }

    @Test fun staleAndDuplicateOrderIdsDoNotHideOrDuplicateVisiblePhrases() {
        val builtIn = DhikrCatalog.builtInTasbihPhrases
        val preferences = AppPreferences(
            hiddenBuiltInPhraseIds = setOf(builtIn[0].id),
            tasbihPhraseOrder = listOf("deleted_phrase", builtIn[1].id, builtIn[1].id, builtIn[0].id),
        )
        assertEquals(builtIn.drop(1), preferences.tasbihPhrases)
    }

    @Test fun newlyAvailablePhrasesFollowTheSavedOrder() {
        val builtIn = DhikrCatalog.builtInTasbihPhrases
        val custom = TasbihPhrase("custom_new", "ذكر جديد", 7, true)
        val preferences = AppPreferences(
            customPhrases = listOf(custom),
            tasbihPhraseOrder = builtIn.reversed().map(TasbihPhrase::id),
        )
        assertEquals(builtIn.reversed() + custom, preferences.tasbihPhrases)
    }
}

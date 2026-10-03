package com.sakinah.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.sakinah.tasbih.data.DhikrCollection
import com.sakinah.tasbih.data.DhikrEntry
import com.sakinah.tasbih.data.HisnCatalog
import com.sakinah.tasbih.data.ReadingProgress
import com.sakinah.tasbih.ui.ReaderScreen
import com.sakinah.tasbih.ui.SakinahUiState
import com.sakinah.tasbih.ui.theme.SakinahTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReaderAutomaticAdvanceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val collection = DhikrCollection(
        id = "evening_test",
        order = 28,
        title = "أذكار المساء",
        audioUrl = null,
        entries = listOf(
            DhikrEntry("evening_first", "evening_test", "سبحان الله", 3, ""),
            DhikrEntry("evening_second", "evening_test", "الحمد لله", 3, ""),
        ),
    )
    private val state = mutableStateOf(SakinahUiState())
    private var advanceRequests = 0
    private var finishOnAdvance = false

    private fun openReader(focusMode: Boolean, autoAdvance: Boolean, lastEntry: Boolean = false) {
        state.value = SakinahUiState(
            isLoading = false,
            catalog = HisnCatalog("ar", listOf(collection)),
            hapticsEnabled = false,
            autoAdvanceDhikrEnabled = autoAdvance,
            readingProgress = mapOf(
                collection.id to if (lastEntry) {
                    ReadingProgress(entryIndex = 1, repetitionCounts = mapOf(0 to 3, 1 to 2))
                } else {
                    ReadingProgress(repetitionCounts = mapOf(0 to 2))
                },
            ),
        )
        rule.setContent {
            val uiState = state.value
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SakinahTheme(uiState.dynamicColorEnabled, uiState.themeMode, uiState.arabicFontStyle) {
                    ReaderScreen(
                        state = uiState,
                        collectionId = collection.id,
                        isFocusMode = focusMode,
                        onFocusModeChange = {},
                        onBack = {},
                        onIncrement = {
                            val progress = state.value.progressFor(collection.id)
                            updateProgress(progress.copy(
                                repetitionCounts = progress.repetitionCounts +
                                    (progress.entryIndex to progress.repetitionCount + 1),
                            ))
                        },
                        onAdvance = {
                            advanceRequests++
                            if (finishOnAdvance) {
                                updateProgress(state.value.progressFor(collection.id).copy(completed = true))
                            }
                            // Otherwise hold the completed entry to simulate slow storage.
                        },
                        onNavigateDhikr = {},
                        onRestart = {},
                        onToggleFavorite = {},
                        onAddToTasbih = {},
                        onSetShowReferenceByDefault = {},
                        onSetTextScale = {},
                        onSetDhikrCompletionSoundEnabled = {},
                        onSetAutoAdvanceDhikrEnabled = {},
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    private fun updateProgress(progress: ReadingProgress) {
        state.value = state.value.copy(readingProgress = mapOf(collection.id to progress))
    }

    private fun assertDirectAdvance(focusMode: Boolean, autoAdvance: Boolean) {
        openReader(focusMode, autoAdvance)
        val counterBounds = rule.onNodeWithTag("reader_count_button").fetchSemanticsNode().boundsInRoot
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("reader_count_button").performClick()
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle { assertEquals(1, advanceRequests) }
        rule.onNodeWithTag("reader_advance").assertDoesNotExist()
        rule.onNodeWithText(rule.activity.getString(R.string.reader_dhikr_done)).assertDoesNotExist()
        rule.onNodeWithTag("reader_count_button").assertIsDisplayed().assertIsNotEnabled()
        assertEquals(counterBounds, rule.onNodeWithTag("reader_count_button").fetchSemanticsNode().boundsInRoot)

        // The transition stays quiet even if persistence takes longer than the former delay.
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithTag("reader_advance").assertDoesNotExist()
        rule.onNodeWithText(rule.activity.getString(R.string.reader_dhikr_done)).assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(1, advanceRequests)
            updateProgress(state.value.progressFor(collection.id).copy(entryIndex = 1))
        }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("reader_repetition_count", useUnmergedTree = true).assertTextEquals("0")
        rule.onNodeWithText("الحمد لله").assertIsDisplayed()
    }

    @Test fun automaticAdvanceDoesNotFlashCompletionControls() {
        assertDirectAdvance(focusMode = false, autoAdvance = true)
    }

    @Test fun focusModeAdvancesDirectlyWithAutomaticAdvancePreferenceOff() {
        assertDirectAdvance(focusMode = true, autoAdvance = false)
    }

    @Test fun normalModeWithAutomaticAdvanceOffKeepsManualNextAction() {
        openReader(focusMode = false, autoAdvance = false)
        rule.onNodeWithTag("reader_count_button").performClick()
        rule.onNodeWithTag("reader_advance").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, advanceRequests) }
        rule.onNodeWithTag("reader_advance").performClick()
        rule.runOnIdle { assertEquals(1, advanceRequests) }
    }

    @Test fun finalDhikrStillShowsCollectionCompletion() {
        finishOnAdvance = true
        openReader(focusMode = true, autoAdvance = true, lastEntry = true)
        rule.onNodeWithTag("reader_count_button").performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.session_complete)).assertIsDisplayed()
        rule.onNodeWithTag("reader_advance").assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, advanceRequests) }
    }
}

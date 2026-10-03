package com.sakinah.tasbih

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.sakinah.tasbih.ui.SakinahViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DesignImprovementsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: SakinahViewModel
    private fun ready() {
        waitTag("home_featured")
        rule.activityRule.scenario.onActivity { activity -> vm = ViewModelProvider(activity)[SakinahViewModel::class.java] }
    }
    private fun waitTag(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun waitState(check: () -> Boolean) = rule.waitUntil(10_000, check)
    private fun back() {
        rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
    private fun openFirstCollection() {
        rule.onNodeWithTag("nav_library").performClick()
        waitTag("library_group_DailyLife")
        rule.onNodeWithTag("library_group_DailyLife").performClick()
        waitTag("collection_hisn_001")
        rule.onNodeWithTag("collection_hisn_001").performClick()
        waitTag("reader_focus_mode")
    }

    @Test fun favoriteSearchAndRecentOpenExactEntryWithoutErasingOtherCounts() {
        ready()
        rule.activityRule.scenario.onActivity { vm.restartCollection("hisn_001"); vm.setAutoAdvanceDhikrEnabled(true) }
        openFirstCollection()
        rule.onNodeWithTag("reader_count_button").performClick()
        waitState { vm.uiState.value.progressFor("hisn_001").entryIndex == 1 }
        rule.onNodeWithTag("reader_next_dhikr").performClick()
        waitState { vm.uiState.value.progressFor("hisn_001").entryIndex == 2 }
        val entry = vm.uiState.value.catalog.collection("hisn_001")!!.entries[2]
        if (entry.id !in vm.uiState.value.favoriteEntryIds) {
            rule.onNodeWithTag("reader_actions").performClick()
            rule.onNodeWithTag("reader_favorite").performClick()
            waitState { entry.id in vm.uiState.value.favoriteEntryIds }
        }
        back()
        waitTag("library_filter_favorites")
        rule.onNodeWithTag("library_filter_favorites").performClick()
        rule.onNodeWithTag("library_list").performScrollToNode(hasTestTag("library_entry_${entry.id}"))
        rule.onNodeWithTag("library_entry_${entry.id}").performClick()
        waitTag("reader_focus_mode")
        assertEquals(2, vm.uiState.value.progressFor("hisn_001").entryIndex)
        assertEquals(1, vm.uiState.value.progressFor("hisn_001").repetitionCountFor(0))
        rule.onNodeWithTag("reader_focus_mode").performClick()
        waitTag("reader_focus_screen")
        rule.activityRule.scenario.recreate()
        waitTag("reader_focus_screen")
        assertEquals(2, vm.uiState.value.progressFor("hisn_001").entryIndex)
        back(); waitTag("reader_focus_mode"); back()
        waitTag("library_filter_recent")
        rule.onNodeWithTag("library_filter_recent").performClick()
        rule.onNodeWithTag("library_list").performScrollToNode(hasTestTag("library_entry_${entry.id}"))
        rule.onNodeWithTag("library_entry_${entry.id}").assertIsDisplayed()
        rule.onNodeWithTag("library_list").performScrollToNode(hasTestTag("library_filter_all"))
        rule.onNodeWithTag("library_filter_all").performClick()
        rule.onNodeWithTag("library_search").performTextInput(entry.text.take(22))
        rule.onNodeWithTag("library_list").performScrollToNode(hasTestTag("library_entry_${entry.id}"))
        rule.onNodeWithTag("library_entry_${entry.id}").performClick()
        waitTag("reader_focus_mode")
        assertEquals(2, vm.uiState.value.progressFor("hisn_001").entryIndex)
        assertEquals(1, vm.uiState.value.progressFor("hisn_001").repetitionCountFor(0))
    }

    @Test fun readerUndoAfterAutomaticAdvanceAndRecreationReturnsToPreviousDhikr() {
        ready()
        rule.activityRule.scenario.onActivity { vm.restartCollection("hisn_001"); vm.setAutoAdvanceDhikrEnabled(true) }
        openFirstCollection()
        rule.onNodeWithTag("reader_count_button").performClick()
        waitState { vm.uiState.value.progressFor("hisn_001").entryIndex == 1 }
        rule.activityRule.scenario.recreate()
        waitTag("reader_undo")
        rule.onNodeWithTag("reader_undo").assertIsEnabled().performClick()
        waitState { vm.uiState.value.progressFor("hisn_001").entryIndex == 0 && vm.uiState.value.progressFor("hisn_001").repetitionCount == 0 }
        rule.onNodeWithTag("reader_undo").assertIsNotEnabled()
    }

    @Test fun longTasbihScrollDoesNotCountAndEditorExplainsItsLimit() {
        ready()
        val longText = "سبحان الله والحمد لله ".repeat(22) + "نهاية الذكر"
        rule.activityRule.scenario.onActivity { vm.addCustomPhrase(longText, 33); vm.setTasbihTextScale(1.6f) }
        waitState { vm.uiState.value.selectedPhrase.text == longText }
        rule.onNodeWithTag("nav_tasbih").performClick()
        waitTag("tasbih_counter")
        val totalBefore = vm.uiState.value.tasbihCount
        rule.onNodeWithTag("tasbih_focus_mode").performClick()
        waitTag("tasbih_focus_screen")
        val dialBefore = rule.onNodeWithTag("tasbih_counter").fetchSemanticsNode().boundsInRoot
        val text = rule.onNodeWithTag("tasbih_phrase_text")
        text.performTouchInput { swipeUp() }
        waitState { text.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f }
        assertEquals(totalBefore, vm.uiState.value.tasbihCount)
        assertEquals(dialBefore, rule.onNodeWithTag("tasbih_counter").fetchSemanticsNode().boundsInRoot)
        rule.activityRule.scenario.recreate()
        waitTag("tasbih_focus_screen")
        assertEquals(totalBefore, vm.uiState.value.tasbihCount)
        rule.onNodeWithTag("tasbih_exit_focus_mode").performClick()
        rule.onNodeWithTag("tasbih_actions").performClick()
        rule.onNodeWithTag("tasbih_manage_phrases").performClick()
        waitTag("tasbih_manager_add")
        rule.onNodeWithTag("tasbih_manager_add").performClick()
        waitTag("custom_text")
        rule.onNodeWithTag("custom_text").performTextInput("س".repeat(501))
        rule.onNodeWithTag("custom_save").assertIsNotEnabled()
        rule.onNodeWithTag("phrase_character_count", useUnmergedTree = true).performScrollTo()
        rule.onNodeWithTag("phrase_character_count", useUnmergedTree = true).assertTextContains("501", substring = true)
        rule.onNodeWithText("النص أطول من 500 حرف. اختصره ليتم الحفظ.", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("custom_text").performTextClearance()
        rule.onNodeWithTag("custom_text").performTextInput("ذكر قصير")
        rule.onNodeWithTag("custom_goal").performScrollTo().performTextClearance()
        rule.onNodeWithTag("custom_goal").performTextInput("10000")
        rule.onNodeWithTag("custom_save").assertIsNotEnabled()
        rule.onNodeWithTag("custom_goal").performTextClearance()
        rule.onNodeWithTag("custom_goal").performTextInput("33")
        rule.onNodeWithTag("custom_save").assertIsEnabled()
        back()
        rule.activityRule.scenario.onActivity { vm.setTasbihTextScale(1f) }
    }
    @Test fun importedLongDhikrAllowsGoalChangesWithoutTruncatingItsText() {
        ready()
        val entry = vm.uiState.value.catalog.collection("hisn_001")!!.entries.first { it.text.length > 500 }
        rule.activityRule.scenario.onActivity { vm.addEntryToTasbih(entry) }
        waitState { vm.uiState.value.selectedPhrase.text == entry.text }
        rule.onNodeWithTag("nav_tasbih").performClick()
        waitTag("tasbih_actions")
        rule.onNodeWithTag("tasbih_actions").performClick()
        waitTag("tasbih_manage_phrases")
        rule.onNodeWithTag("tasbih_manage_phrases").performClick()
        waitTag("tasbih_phrase_manager_list")
        rule.onNodeWithTag("tasbih_phrase_manager_list")
            .performScrollToNode(hasTestTag("tasbih_phrase_actions_selected"))
        rule.onNodeWithTag("tasbih_phrase_actions_selected").performScrollTo().performClick()
        rule.onNodeWithTag("tasbih_phrase_edit_selected").performClick()
        waitTag("custom_goal")
        rule.onNodeWithTag("custom_goal").performScrollTo().performTextReplacement("77")
        rule.onNodeWithTag("custom_save").assertIsEnabled().performClick()
        waitState { vm.uiState.value.selectedPhrase.defaultGoal == 77 }
        assertEquals(entry.text, vm.uiState.value.selectedPhrase.text)
    }

    @Test fun savedCompletedEntryAlwaysOffersAnExplicitFinishActionOnReentry() {
        ready()
        rule.activityRule.scenario.onActivity { vm.restartCollection("hisn_001"); vm.setAutoAdvanceDhikrEnabled(true) }
        waitState { vm.uiState.value.progressFor("hisn_001").repetitionCounts.isEmpty() }
        val collection = vm.uiState.value.catalog.collection("hisn_001")!!
        // Count every entry without advancing the final one, representing leaving during the completion delay.
        collection.entries.forEachIndexed { index, entry ->
            rule.activityRule.scenario.onActivity { vm.openReaderEntry(entry) }
            waitState { vm.uiState.value.progressFor(collection.id).entryIndex == index }
            repeat(entry.repetitions) { count ->
                rule.activityRule.scenario.onActivity { vm.incrementDhikr(collection.id) }
                waitState { vm.uiState.value.progressFor(collection.id).repetitionCount == count + 1 }
            }
        }
        openFirstCollection()
        waitTag("reader_advance")
        rule.onNodeWithTag("reader_advance").assertIsEnabled().performClick()
        waitState { vm.uiState.value.progressFor(collection.id).completed }
        waitTag("reader_review_completed")
        rule.onNodeWithTag("reader_review_completed").performScrollTo().performClick()
        waitTag("reader_focus_mode")
        assertTrue(vm.uiState.value.progressFor(collection.id).completed)
        rule.onNodeWithTag("reader_undo").performClick()
        waitState { !vm.uiState.value.progressFor(collection.id).completed }
    }

}

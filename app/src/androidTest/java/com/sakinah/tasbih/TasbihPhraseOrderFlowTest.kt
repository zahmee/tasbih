package com.sakinah.tasbih

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.sakinah.tasbih.ui.SakinahViewModel
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TasbihPhraseOrderFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun waitTag(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        rule.waitForIdle()
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val prefix = InstrumentationRegistry.getArguments().getString("gallery", "reorder")!!
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "reorder-evidence").apply { mkdirs() }
        File(directory, "$prefix-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun changingPositionKeepsTheSelectedCounterAndChangesCarouselOrderAfterRecreation() {
        waitTag("home_featured")
        lateinit var vm: SakinahViewModel
        rule.activityRule.scenario.onActivity { vm = ViewModelProvider(it)[SakinahViewModel::class.java] }
        val theme = if (InstrumentationRegistry.getArguments().getString("gallery", "").contains("dark")) ThemeMode.Dark else ThemeMode.Light
        rule.activityRule.scenario.onActivity {
            vm.setThemeMode(theme)
            vm.setArabicFontStyle(ArabicFontStyle.Sakinah)
        }
        rule.waitUntil(10_000) { vm.uiState.value.themeMode == theme && vm.uiState.value.arabicFontStyle == ArabicFontStyle.Sakinah }
        val first = vm.uiState.value.tasbihPhrases[0]
        val second = vm.uiState.value.tasbihPhrases[1]
        rule.activityRule.scenario.onActivity { vm.selectPhrase(first.id) }
        rule.waitUntil(10_000) { vm.uiState.value.selectedPhrase.id == first.id }
        rule.activityRule.scenario.onActivity { vm.resetTasbih() }
        rule.waitUntil(10_000) { vm.uiState.value.tasbihCount == 0 }
        repeat(3) { index ->
            rule.activityRule.scenario.onActivity { vm.incrementTasbih() }
            rule.waitUntil(10_000) { vm.uiState.value.tasbihCount == index + 1 }
        }
        val targetBefore = vm.uiState.value.tasbihTarget
        rule.onNodeWithTag("nav_tasbih").performClick()
        waitTag("tasbih_actions")
        rule.onNodeWithTag("tasbih_actions").performClick()
        waitTag("tasbih_manage_phrases")
        rule.onNodeWithTag("tasbih_manage_phrases").performClick()
        waitTag("tasbih_phrase_manager_list")
        rule.onNodeWithTag("tasbih_phrase_manager_list").performScrollToNode(hasTestTag("tasbih_phrase_actions_${second.id}"))
        rule.onNodeWithTag("tasbih_phrase_actions_${second.id}").performClick()
        waitTag("tasbih_phrase_reorder_${second.id}")
        capture("menu")
        rule.onNodeWithTag("tasbih_phrase_reorder_${second.id}").performClick()
        waitTag("phrase_order_editor")
        rule.onNodeWithTag("phrase_order_input").performScrollTo().performTextReplacement("0")
        rule.onNodeWithTag("phrase_order_save").assertIsNotEnabled()
        rule.onNodeWithTag("phrase_order_input").performTextReplacement("٢")
        rule.onNodeWithTag("phrase_order_up").performScrollTo().performClick()
        rule.onNodeWithTag("phrase_order_input").assertTextContains("١", substring = true)
        rule.onNodeWithTag("phrase_order_up").assertIsNotEnabled()
        rule.onNodeWithTag("phrase_order_save").performScrollTo().assertIsDisplayed()
        capture("editor")
        rule.onNodeWithTag("phrase_order_save").performClick()
        rule.waitUntil(10_000) { vm.uiState.value.tasbihPhrases.first().id == second.id }
        assertEquals(first.id, vm.uiState.value.selectedPhrase.id)
        assertEquals(3, vm.uiState.value.tasbihCount)
        assertEquals(targetBefore, vm.uiState.value.tasbihTarget)
        rule.activityRule.scenario.recreate()
        waitTag("tasbih_phrase_manager_list")
        assertEquals(listOf(second.id, first.id), vm.uiState.value.tasbihPhrases.take(2).map { it.id })
        rule.onNodeWithTag("tasbih_phrase_manager_list").performScrollToIndex(1)
        capture("saved")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitTag("tasbih_counter")
        rule.onNodeWithTag("tasbih_previous_phrase").assertIsEnabled().performClick()
        rule.waitUntil(10_000) { vm.uiState.value.selectedPhrase.id == second.id }
        rule.onNodeWithTag("tasbih_previous_phrase").assertIsNotEnabled()
        rule.onNodeWithTag("tasbih_next_phrase").performClick()
        rule.waitUntil(10_000) { vm.uiState.value.selectedPhrase.id == first.id }
        assertEquals(3, vm.uiState.value.tasbihCount)
        rule.activityRule.scenario.onActivity { vm.moveTasbihPhrase(second.id, 1) }
        rule.waitUntil(10_000) { vm.uiState.value.tasbihPhrases.first().id == first.id }
    }
}

package com.sakinah.tasbih

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.SakinahViewModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TasbihHeaderFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitTag(tag: String) = rule.waitUntil(10_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun menuAction(tag: String) {
        rule.onNodeWithTag("tasbih_actions").performClick()
        waitTag(tag)
        rule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        rule.waitForIdle()
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val prefix = InstrumentationRegistry.getArguments().getString("gallery", "normal")!!
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "tasbih-header-evidence").apply { mkdirs() }
        File(directory, "$prefix-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun overflowEditsCurrentPhraseAndUndoesOneCountWithoutLosingProgress() {
        waitTag("nav_tasbih")
        lateinit var vm: SakinahViewModel
        val theme = if (InstrumentationRegistry.getArguments().getString("gallery", "").contains("dark")) ThemeMode.Dark else ThemeMode.Light
        rule.activityRule.scenario.onActivity {
            vm = ViewModelProvider(it)[SakinahViewModel::class.java]
            vm.setThemeMode(theme)
            vm.setArabicFontStyle(ArabicFontStyle.Sakinah)
            vm.setTasbihTextScale(1f)
            vm.addCustomPhrase("الحمد لله", 33)
        }
        rule.waitUntil(10_000) { vm.uiState.value.selectedPhrase.isCustom && vm.uiState.value.selectedPhrase.text == "الحمد لله" }
        val phraseId = vm.uiState.value.selectedPhrase.id
        rule.onNodeWithTag("nav_tasbih").performClick()
        waitTag("tasbih_counter")
        rule.onNodeWithTag("tasbih_manage_phrases").assertDoesNotExist()
        rule.onNodeWithTag("tasbih_undo").assertDoesNotExist()
        rule.onNodeWithTag("tasbih_edit").assertDoesNotExist()
        rule.onNodeWithTag("tasbih_focus_mode").assertIsDisplayed()
        rule.onNodeWithTag("tasbih_actions").assertIsDisplayed()

        val previous = rule.onNodeWithTag("tasbih_previous_phrase").fetchSemanticsNode().boundsInRoot
        val next = rule.onNodeWithTag("tasbih_next_phrase").fetchSemanticsNode().boundsInRoot
        val text = rule.onNodeWithTag("tasbih_phrase_text").fetchSemanticsNode().boundsInRoot
        assertTrue(previous.left > next.right)
        assertEquals(previous.center.y, next.center.y, 1f)
        assertTrue(text.top >= maxOf(previous.bottom, next.bottom))

        rule.onNodeWithTag("tasbih_actions").performClick()
        waitTag("tasbih_undo")
        rule.onNodeWithTag("tasbih_undo").assertIsNotEnabled()
        pressBack()
        repeat(3) { index ->
            rule.onNodeWithTag("tasbih_counter").performClick()
            rule.waitUntil(10_000) { vm.uiState.value.tasbihCount == index + 1 }
        }
        capture("screen")
        rule.onNodeWithTag("tasbih_actions").performClick()
        waitTag("tasbih_edit")
        capture("menu")
        rule.onNodeWithTag("tasbih_undo").performScrollTo().performClick()
        rule.waitUntil(10_000) { vm.uiState.value.tasbihCount == 2 }

        menuAction("tasbih_edit")
        waitTag("custom_text")
        rule.onNodeWithTag("custom_text").performScrollTo().assertTextContains("الحمد لله")
        rule.onNodeWithTag("custom_text").performTextReplacement("الحمد لله رب العالمين")
        rule.onNodeWithTag("custom_goal").performScrollTo().performTextReplacement("99")
        rule.onNodeWithTag("custom_save").assertIsDisplayed().performClick()
        rule.waitUntil(10_000) { vm.uiState.value.selectedPhrase.text == "الحمد لله رب العالمين" && vm.uiState.value.tasbihTarget == 99 }
        assertEquals(phraseId, vm.uiState.value.selectedPhrase.id)
        assertEquals(2, vm.uiState.value.tasbihCount)

        menuAction("tasbih_manage_phrases")
        waitTag("tasbih_phrase_manager_list")
        rule.onNodeWithTag("tasbih_phrase_manager_list").performScrollToNode(hasTestTag("tasbih_phrase_actions_selected"))
        rule.onNodeWithTag("tasbih_phrase_actions_selected").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitTag("tasbih_focus_mode")
        rule.onNodeWithTag("tasbih_focus_mode").performClick()
        waitTag("tasbih_focus_screen")
        rule.onNodeWithTag("tasbih_actions").assertDoesNotExist()
        capture("focus")
        rule.onNodeWithTag("tasbih_exit_focus_mode").performClick()
        rule.activityRule.scenario.recreate()
        waitTag("tasbih_counter")
        assertEquals(phraseId, vm.uiState.value.selectedPhrase.id)
        assertEquals(2, vm.uiState.value.tasbihCount)
        assertEquals(99, vm.uiState.value.tasbihTarget)
        capture("edited")
        rule.activityRule.scenario.onActivity { vm.deleteCustomPhrase(phraseId) }
        rule.waitUntil(10_000) { vm.uiState.value.tasbihPhrases.none { it.id == phraseId } }
    }
}

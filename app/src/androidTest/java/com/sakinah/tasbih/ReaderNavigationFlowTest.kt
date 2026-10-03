package com.sakinah.tasbih

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.SakinahViewModel
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReaderNavigationFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val gallery = InstrumentationRegistry.getArguments().getString("gallery", "standard")!!
    private lateinit var vm: SakinahViewModel

    private fun waitTag(tag: String) = rule.waitUntil(10_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun ready() {
        waitTag("home_featured")
        rule.activityRule.scenario.onActivity { activity ->
            vm = ViewModelProvider(activity)[SakinahViewModel::class.java]
            vm.setThemeMode(if (gallery.contains("dark")) ThemeMode.Dark else ThemeMode.Light)
            vm.setArabicFontStyle(ArabicFontStyle.Sakinah)
        }
        rule.waitUntil(10_000) {
            vm.uiState.value.themeMode == (if (gallery.contains("dark")) ThemeMode.Dark else ThemeMode.Light) &&
                vm.uiState.value.arabicFontStyle == ArabicFontStyle.Sakinah
        }
    }

    private fun assertNormalChrome(selectedTag: String) {
        waitTag("reader_top_bar")
        rule.onNodeWithTag("reader_top_bar").assertIsDisplayed()
        rule.onNodeWithTag("reader_back").assertIsDisplayed()
        rule.onNodeWithTag("reader_title").assertIsDisplayed()
        rule.onNodeWithTag("reader_focus_mode").assertIsDisplayed()
        rule.onNodeWithTag("reader_actions").assertIsDisplayed()
        rule.onNodeWithTag("reader_dhikr_text").assertIsDisplayed()
        listOf("nav_home", "nav_library", "nav_tasbih", "nav_settings").forEach {
            rule.onNodeWithTag(it).assertIsDisplayed()
        }
        rule.onNodeWithTag(selectedTag).assertIsSelected()
        rule.onNodeWithTag("reader_exit_focus_mode").assertDoesNotExist()
        if (rule.onAllNodesWithTag("app_navigation_bar").fetchSemanticsNodes().isNotEmpty()) {
            val bar = rule.onNodeWithTag("app_navigation_bar").fetchSemanticsNode().boundsInRoot
            val counter = rule.onNodeWithTag("reader_count_button").fetchSemanticsNode().boundsInRoot
            assertTrue("The counter must remain above the navigation bar", counter.bottom <= bar.top + 1f)
        }
        val back = rule.onNodeWithTag("reader_back").fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithTag("reader_title").fetchSemanticsNode().boundsInRoot
        val focus = rule.onNodeWithTag("reader_focus_mode").fetchSemanticsNode().boundsInRoot
        val actions = rule.onNodeWithTag("reader_actions").fetchSemanticsNode().boundsInRoot
        assertTrue(back.center.x > title.center.x && title.center.x > focus.center.x && focus.center.x > actions.center.x)
    }

    private fun assertFocusChrome() {
        waitTag("reader_focus_screen")
        rule.onNodeWithTag("reader_top_bar").assertDoesNotExist()
        rule.onNodeWithTag("reader_actions").assertDoesNotExist()
        listOf("nav_home", "nav_library", "nav_tasbih", "nav_settings").forEach {
            rule.onNodeWithTag(it).assertDoesNotExist()
        }
        rule.onNodeWithTag("reader_count_button").assertIsDisplayed()
        rule.onNodeWithTag("reader_exit_focus_mode").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        val label = rule.onNodeWithText("إلغاء التوسيع", useUnmergedTree = true)
        label.assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse("Exit text must not be truncated vertically", layouts.first().didOverflowHeight)
        assertFalse(
            "Exit text must fit its available width: font=${vm.uiState.value.arabicFontStyle}, " +
                "size=${layouts.first().size}, paragraphWidth=${layouts.first().multiParagraph.width}",
            layouts.first().didOverflowWidth,
        )
        val exitBounds = rule.onNodeWithTag("reader_exit_focus_mode").fetchSemanticsNode().boundsInRoot
        val labelBounds = label.fetchSemanticsNode().boundsInRoot
        assertTrue(labelBounds.top >= exitBounds.top && labelBounds.bottom <= exitBounds.bottom)
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        instrumentation.waitForIdleSync()
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "reader-navigation-evidence")
        directory.mkdirs()
        File(directory, "$gallery-$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

    @Test fun readerOpenedFromHomeKeepsNavigationUntilFocusIsEnabled() {
        ready()
        rule.onNodeWithTag("home_resume_wird").performScrollTo().performClick()
        assertNormalChrome("nav_home")
        val counts = vm.uiState.value.readingProgress
        capture("reader-normal")
        rule.onNodeWithTag("reader_focus_mode").performClick()
        assertFocusChrome()
        capture("reader-focus")
        rule.activityRule.scenario.recreate()
        assertFocusChrome()
        rule.onNodeWithTag("reader_exit_focus_mode").performClick()
        assertNormalChrome("nav_home")
        assertEquals(counts, vm.uiState.value.readingProgress)
    }

    @Test fun readerOpenedFromLibraryKeepsItsMenuAndNavigationWithoutCounting() {
        ready()
        rule.activityRule.scenario.onActivity { vm.restartCollection("hisn_001") }
        rule.onNodeWithTag("nav_library").performClick()
        waitTag("library_list")
        rule.onNodeWithTag("library_list").performScrollToNode(hasTestTag("library_group_DailyLife"))
        rule.onNodeWithTag("library_group_DailyLife").performClick()
        rule.onNodeWithTag("library_list").performScrollToNode(hasTestTag("collection_hisn_001"))
        rule.onNodeWithTag("collection_hisn_001").performClick()
        assertNormalChrome("nav_library")
        val progress = vm.uiState.value.progressFor("hisn_001")
        rule.onNodeWithTag("reader_actions").performClick()
        rule.onNodeWithTag("reader_settings").assertIsDisplayed()
        Espresso.pressBack()
        assertNormalChrome("nav_library")
        rule.onNodeWithTag("nav_tasbih").performClick()
        waitTag("tasbih_counter")
        assertEquals(progress, vm.uiState.value.progressFor("hisn_001"))
    }

    @Test fun focusExitLabelFitsAllArabicFontsAndBackRestoresNavigation() {
        ready()
        rule.onNodeWithTag("home_resume_wird").performScrollTo().performClick()
        assertNormalChrome("nav_home")
        rule.onNodeWithTag("reader_focus_mode").performClick()
        for (font in ArabicFontStyle.entries) {
            rule.activityRule.scenario.onActivity { vm.setArabicFontStyle(font) }
            rule.waitUntil(10_000) { vm.uiState.value.arabicFontStyle == font }
            assertFocusChrome()
            capture("focus-${font.name.lowercase()}")
        }
        rule.activityRule.scenario.onActivity { vm.setArabicFontStyle(ArabicFontStyle.Sakinah) }
        Espresso.pressBack()
        assertNormalChrome("nav_home")
    }
}

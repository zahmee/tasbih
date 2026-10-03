package com.sakinah.tasbih

import android.graphics.Bitmap
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.SakinahViewModel
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Exercises real navigation and exports evidence under the test device's current display settings. */
class AdaptiveDesignFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val arguments = InstrumentationRegistry.getArguments()
    private val prefix = arguments.getString("gallery", "standard")!!
    private fun waitTag(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun tap(tag: String) { waitTag(tag); rule.onNodeWithTag(tag).performClick() }
    private fun scrollTap(list: String, tag: String) {
        waitTag(list); rule.onNodeWithTag(list).performScrollToNode(hasTestTag(tag)); tap(tag)
    }
    private fun back() {
        rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
    private fun capture(name: String) {
        rule.waitForIdle()
        instrumentation.waitForIdleSync()
        Thread.sleep(350) // Allow the rendered frame and native navigation transition to settle.
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "design-evidence").apply { mkdirs() }
        File(directory, "$prefix-$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
    @Test fun pagesRemainUsableAtCurrentWidthAndFontSize() {
        waitTag("home_featured")
        lateinit var vm: SakinahViewModel
        rule.activityRule.scenario.onActivity { activity ->
            vm = ViewModelProvider(activity)[SakinahViewModel::class.java]
            vm.setThemeMode(if (prefix.contains("dark")) ThemeMode.Dark else ThemeMode.Light)
            vm.setArabicFontStyle(ArabicFontStyle.Sakinah)
            vm.setShowDiacritics(true)
            vm.setDynamicColor(prefix.contains("dynamic"))
            vm.setTextScale(if (prefix.contains("large")) 1.4f else 1f)
            vm.setTasbihTextScale(if (prefix.contains("large")) 1.6f else 1f)
            vm.restartCollection("hisn_001")
        }
        val expectedTheme = if (prefix.contains("dark")) ThemeMode.Dark else ThemeMode.Light
        rule.waitUntil(10_000) {
            vm.uiState.value.themeMode == expectedTheme &&
            vm.uiState.value.arabicFontStyle == ArabicFontStyle.Sakinah &&
                vm.uiState.value.progressFor("hisn_001").repetitionCounts.isEmpty()
        }
        capture("home")
        tap("nav_library"); waitTag("library_search"); capture("library")
        scrollTap("library_list", "library_group_DailyLife")
        scrollTap("library_list", "collection_hisn_001")
        waitTag("reader_focus_mode"); capture("reader")
        tap("reader_focus_mode"); waitTag("reader_focus_screen")
        rule.onNodeWithTag("reader_count_button").assertIsDisplayed()
        capture("reader-focus")
        tap("reader_exit_focus_mode"); back()
        tap("nav_settings"); waitTag("settings_list"); capture("more")
        scrollTap("settings_list", "open_reading_settings"); waitTag("settings_detail_list"); capture("reading-settings"); back()
        scrollTap("settings_list", "open_appearance_settings"); waitTag("settings_detail_list"); capture("appearance"); back()
        scrollTap("settings_list", "open_sound_settings"); waitTag("sound_settings_screen"); capture("sound"); back()
        scrollTap("settings_list", "open_achievements"); waitTag("achievements_list"); capture("activity")
        scrollTap("achievements_list", "activity_expand_hours")
        rule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("hourly_activity"))
        capture("hours")
        scrollTap("achievements_list", "hourly_values")
        rule.onNodeWithTag("hourly_value_list").assertExists()
        capture("hour-values")
        scrollTap("achievements_list", "activity_expand_calendar")
        rule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_calendar"))
        rule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_calendar_grid"))
        capture("calendar"); back()
        scrollTap("settings_list", "open_about"); waitTag("about_screen"); capture("about"); back()
        val longText = "سبحان الله والحمد لله ".repeat(22) + "نهاية الذكر"
        rule.activityRule.scenario.onActivity { vm.addCustomPhrase(longText, 33) }
        rule.waitUntil(10_000) { vm.uiState.value.selectedPhrase.text == longText }
        tap("nav_tasbih"); waitTag("tasbih_counter"); capture("tasbih")
        tap("tasbih_focus_mode"); waitTag("tasbih_focus_screen")
        rule.onNodeWithTag("tasbih_counter").assertIsDisplayed(); capture("tasbih-focus")
        tap("tasbih_exit_focus_mode"); tap("tasbih_actions"); tap("tasbih_manage_phrases")
        waitTag("tasbih_phrase_manager_list"); capture("manager")
        tap("tasbih_manager_add"); waitTag("custom_text")
        rule.onNodeWithTag("custom_text").performClick().performTextInput("ذكر تجريبي")
        instrumentation.uiAutomation.executeShellCommand("settings put secure show_ime_with_hard_keyboard 1").close()
        rule.onNodeWithTag("custom_text").performClick()
        val service = instrumentation.uiAutomation.serviceInfo
        service.flags = service.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        instrumentation.uiAutomation.serviceInfo = service
        rule.waitUntil(5_000) {
            instrumentation.uiAutomation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        }
        rule.onNodeWithTag("custom_save").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("custom_text").assertIsDisplayed()
        capture("editor-keyboard")
        back()
    }
}

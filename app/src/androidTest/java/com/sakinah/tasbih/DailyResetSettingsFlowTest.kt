package com.sakinah.tasbih

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.DailyResetSettings
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.QuranAppStoreUrl
import com.sakinah.tasbih.ui.SakinahViewModel
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DailyResetSettingsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun waitTag(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun scrollSetting(tag: String): SemanticsNodeInteraction {
        rule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag(tag))
        return rule.onNodeWithTag(tag).performScrollTo()
    }
    private fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() } }
    private fun capture(name: String) {
        rule.waitForIdle()
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val prefix = InstrumentationRegistry.getArguments().getString("gallery", "daily-reset")!!
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "daily-reset-evidence").apply { mkdirs() }
        File(directory, "$prefix-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun settingsPersistWithoutResettingCountsAndOurQuranOpensItsOwnStorePage() {
        waitTag("nav_settings")
        lateinit var vm: SakinahViewModel
        rule.activityRule.scenario.onActivity { vm = ViewModelProvider(it)[SakinahViewModel::class.java] }
        val theme = if (InstrumentationRegistry.getArguments().getString("gallery", "").contains("dark")) ThemeMode.Dark else ThemeMode.Light
        rule.activityRule.scenario.onActivity {
            vm.setThemeMode(theme)
            vm.setArabicFontStyle(ArabicFontStyle.Sakinah)
            vm.setTasbihDailyReset(true)
            vm.setAdhkarDailyReset(true)
            vm.setDailyResetTime(0)
        }
        rule.waitUntil(10_000) { vm.uiState.value.dailyReset == DailyResetSettings() && vm.uiState.value.themeMode == theme && vm.uiState.value.arabicFontStyle == ArabicFontStyle.Sakinah }
        val beforeIncrement = vm.uiState.value.tasbihCount
        rule.activityRule.scenario.onActivity { vm.incrementTasbih() }
        rule.waitUntil(10_000) { vm.uiState.value.tasbihCount == beforeIncrement + 1 }
        val before = vm.uiState.value.tasbihCount
        rule.onNodeWithTag("nav_settings").performClick()
        rule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("open_daily_reset_settings"))
        rule.onNodeWithTag("open_daily_reset_settings").performClick()
        waitTag("settings_detail_dailyreset")
        scrollSetting("setting_daily_reset_tasbih").assertIsOn().performClick()
        rule.waitUntil(10_000) { !vm.uiState.value.dailyReset.tasbihEnabled }
        scrollSetting("setting_daily_reset_adhkar").assertIsOn().performClick()
        rule.waitUntil(10_000) { !vm.uiState.value.dailyReset.adhkarEnabled }
        scrollSetting("daily_reset_summary").assertTextContains("متوقف", substring = true)
        capture("disabled")
        scrollSetting("daily_reset_time").performClick()
        scrollSetting("daily_reset_hour").performTextReplacement("24")
        rule.onNodeWithTag("daily_reset_time_save").assertIsNotEnabled()
        scrollSetting("daily_reset_hour").performTextReplacement("٠٤")
        rule.onNodeWithTag("daily_reset_minute").performTextReplacement("60")
        rule.onNodeWithTag("daily_reset_time_save").assertIsNotEnabled()
        rule.onNodeWithTag("daily_reset_minute").performTextReplacement("٣٠")
        rule.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(rule.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        rule.onNodeWithTag("daily_reset_minute").assertIsFocused()
        scrollSetting("daily_reset_time_save").assertIsEnabled()
        capture("time-editor")
        rule.onNodeWithTag("daily_reset_time_save").performClick()
        rule.waitUntil(10_000) { vm.uiState.value.dailyReset.minuteOfDay == 270 }
        assertEquals(before, vm.uiState.value.tasbihCount)
        rule.activityRule.scenario.recreate()
        waitTag("settings_detail_dailyreset")
        scrollSetting("setting_daily_reset_tasbih").assertIsOff().performClick()
        scrollSetting("setting_daily_reset_adhkar").assertIsOff().performClick()
        rule.waitUntil(10_000) { vm.uiState.value.dailyReset == DailyResetSettings(minuteOfDay = 270) }
        scrollSetting("daily_reset_summary").assertTextContains("٠٤:٣٠", substring = true)
        capture("saved")
        // Cancelling a draft must keep the saved time.
        scrollSetting("daily_reset_time").performClick()
        scrollSetting("daily_reset_hour").performTextReplacement("12")
        scrollSetting("daily_reset_time_cancel").performClick()
        assertEquals(270, vm.uiState.value.dailyReset.minuteOfDay)
        back()
        waitTag("settings_list")
        rule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("open_about"))
        rule.onNodeWithTag("open_about").performClick()
        rule.onNodeWithTag("about_list").performScrollToNode(hasTestTag("about_quran_store"))
        rule.onNodeWithTag("about_quran_store").assertIsDisplayed()
        capture("our-apps")
        var openedUrl: String? = null
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_VIEW) return null
                openedUrl = intent.dataString
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            rule.onNodeWithTag("about_quran_store").performClick()
            rule.waitUntil(5_000) { openedUrl != null }
            assertEquals(QuranAppStoreUrl, openedUrl)
            assertEquals("https://play.google.com/store/apps/details?id=com.mushaf.reader", openedUrl)
        } finally {
            instrumentation.removeMonitor(monitor)
            rule.activityRule.scenario.onActivity { vm.setDailyResetTime(0) }
            rule.waitUntil(10_000) { vm.uiState.value.dailyReset == DailyResetSettings() }
        }
    }
}

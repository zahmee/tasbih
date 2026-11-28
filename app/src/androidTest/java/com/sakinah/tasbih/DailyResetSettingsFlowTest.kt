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
    companion object {
        /**
         * These steps type and read Hindi digits. Android's backup may bring back a phone's
         * choice of the Arabic digits when the app is installed, so the test sets them first.
         */
        @JvmStatic
        @org.junit.BeforeClass
        fun hindiDigits() {
            com.sakinah.tasbih.data.DigitStyle.save(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
                western = false,
            )
        }
    }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun waitTag(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    /** The settings page in front; each tab's settings list their options under their own tag. */
    private var list = "dhikr_settings_options"
    private fun scrollSetting(tag: String): SemanticsNodeInteraction {
        rule.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
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
        waitTag("nav_tasbih")
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
        // The reset is one, for the tasbih and the adhkar, in their shared section: the tasbih's
        // gear opens it on the tasbih, and the adhkar's on reading.
        rule.onNodeWithTag("nav_tasbih").performClick()
        waitTag("tasbih_open_settings")
        rule.onNodeWithTag("tasbih_open_settings").performClick()
        waitTag("dhikr_settings")
        rule.onNodeWithTag("dhikr_tab_reset").performClick()
        scrollSetting("setting_daily_reset_tasbih").assertIsOn().performClick()
        rule.waitUntil(10_000) { !vm.uiState.value.dailyReset.tasbihEnabled }
        back()
        rule.onNodeWithTag("nav_library").performClick()
        waitTag("library_open_settings")
        rule.onNodeWithTag("library_open_settings").performClick()
        waitTag("dhikr_settings")
        rule.onNodeWithTag("dhikr_tab_reset").performClick()
        // Both switches sit together, so the tasbih's shows what was set from its own gear.
        scrollSetting("setting_daily_reset_tasbih").assertIsOff()
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
        waitTag("dhikr_settings")
        scrollSetting("setting_daily_reset_adhkar").assertIsOff().performClick()
        rule.activityRule.scenario.onActivity { vm.setTasbihDailyReset(true) }
        rule.waitUntil(10_000) { vm.uiState.value.dailyReset == DailyResetSettings(minuteOfDay = 270) }
        scrollSetting("daily_reset_summary").assertTextContains("٠٤:٣٠", substring = true)
        capture("saved")
        // Cancelling a draft must keep the saved time.
        scrollSetting("daily_reset_time").performClick()
        scrollSetting("daily_reset_hour").performTextReplacement("12")
        scrollSetting("daily_reset_time_cancel").performClick()
        assertEquals(270, vm.uiState.value.dailyReset.minuteOfDay)
        back()
        // About the app is in the settings tree, behind the gear on the day's page.
        rule.onNodeWithTag("nav_home").performClick()
        waitTag("home_open_settings")
        rule.onNodeWithTag("home_open_settings").performClick()
        waitTag("settings_hub")
        rule.onNodeWithTag("settings_hub_list").performScrollToNode(hasTestTag("open_about"))
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

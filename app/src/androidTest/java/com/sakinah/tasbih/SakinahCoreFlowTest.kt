package com.sakinah.tasbih

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.sakinah.tasbih.ui.SakinahViewModel
import com.sakinah.tasbih.data.CompletionSound
import org.junit.Rule
import org.junit.Test

class SakinahCoreFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeKeepsTheCompactGreetingAndMovesTheFullBookToTheLibrary() {
        waitForTag("home_featured")

        composeRule.onNodeWithTag("home_greeting").assertIsDisplayed()
        composeRule.onNodeWithText("السلام عليكم").assertIsDisplayed()
        waitForAbsentText("حصن المسلم كاملًا")
        composeRule.onNodeWithTag("nav_library").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_library").performClick()
        waitForTag("library_group_DailyLife")
        composeRule.onNodeWithTag("library_group_DailyLife").performClick()
        waitForTag("collection_hisn_001")
    }

    @Test
    fun libraryReaderAndCustomTasbihSurviveRecreation() {
        waitForTag("home_featured")
        composeRule.activityRule.scenario.onActivity { activity ->
            ViewModelProvider(activity)[SakinahViewModel::class.java]
                .restartCollection("hisn_001")
        }

        composeRule.onNodeWithTag("nav_library").performClick()
        waitForTag("library_group_DailyLife")
        composeRule.onNodeWithTag("library_group_DailyLife").performClick()
        waitForTag("collection_hisn_001")
        composeRule.onNodeWithText("15 نتيجة").assertIsDisplayed()

        composeRule.onNodeWithTag("collection_hisn_001").performClick()
        waitForTag("reader_count_button")
        waitForText("الذكر 1 من 4")
        composeRule.onNodeWithTag("reader_next_dhikr").performClick()
        waitForText("الذكر 2 من 4")
        composeRule.activityRule.scenario.onActivity { activity ->
            val progress = ViewModelProvider(activity)[SakinahViewModel::class.java]
                .uiState.value.progressFor("hisn_001")
            assertEquals(0, progress.repetitionCounts.values.sum())
        }
        composeRule.onNodeWithTag("reader_previous_dhikr").performClick()
        waitForText("الذكر 1 من 4")
        composeRule.onNodeWithTag("reader_count_area").performClick()
        waitForText("الذكر 2 من 4")

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForTag("nav_tasbih")
        composeRule.onNodeWithTag("nav_tasbih").performClick()
        waitForTag("tasbih_counter")

        composeRule.onNodeWithTag("tasbih_actions").performClick()
        composeRule.onNodeWithTag("tasbih_add").performClick()
        waitForTag("custom_text")
        composeRule.onNodeWithTag("custom_text").performTextInput("ذِكْرٌ خَاصّ")
        composeRule.onNodeWithTag("custom_goal").performTextClearance()
        composeRule.onNodeWithTag("custom_goal").performTextInput("7")
        composeRule.onNodeWithTag("custom_save").performClick()
        waitForText("ذِكْرٌ خَاصّ")
        waitForText("الدورة الحالية 0 من 7")

        composeRule.onNodeWithTag("tasbih_phrase_card").assertHasNoClickAction()
        composeRule.onNodeWithTag("tasbih_phrase_text").performTouchInput { click(center) }
        waitForAbsentTag("tasbih_phrase_editor")
        composeRule.onNodeWithTag("tasbih_phrase_text").performTouchInput { longClick() }
        waitForAbsentTag("tasbih_phrase_editor")
        composeRule.activityRule.scenario.onActivity { activity ->
            ViewModelProvider(activity)[SakinahViewModel::class.java].resetTasbih()
        }
        waitForText("الدورة الحالية 0 من 7")
        composeRule.onNodeWithTag("tasbih_actions").performClick()
        composeRule.onNodeWithTag("tasbih_manage_phrases").performClick()
        waitForTag("tasbih_phrase_manager_screen")
        composeRule.onNodeWithTag("tasbih_phrase_manager_list")
            .performScrollToNode(hasTestTag("tasbih_phrase_actions_selected"))
        composeRule.onNodeWithTag("tasbih_phrase_actions_selected").performClick()
        composeRule.onNodeWithTag("tasbih_phrase_edit_selected").performClick()
        waitForText("تعديل الذكر")
        composeRule.onNodeWithTag("custom_text").performTextClearance()
        composeRule.onNodeWithTag("custom_text").performTextInput("ذِكْرٌ مُعَدَّل")
        composeRule.onNodeWithTag("custom_goal").performTextClearance()
        composeRule.onNodeWithTag("custom_goal").performTextInput("9")
        composeRule.onNodeWithTag("custom_save").performClick()
        waitForText("ذِكْرٌ مُعَدَّل")
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForTag("tasbih_counter")
        waitForText("الدورة الحالية 0 من 9")

        composeRule.onNodeWithTag("tasbih_counter").performClick()
        waitForText("الدورة الحالية 1 من 9")

        composeRule.onNodeWithTag("tasbih_actions").performClick()
        composeRule.onNodeWithTag("tasbih_phrase_statistics").performClick()
        waitForTag("tasbih_phrase_statistics_sheet")
        composeRule.onNodeWithText("إحصائيات هذا الذكر").assertIsDisplayed()
        composeRule.onNodeWithText("إجمالي مرات التسبيح").assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_phrase_statistics_close").performClick()
        waitForTag("tasbih_counter")

        composeRule.activityRule.scenario.recreate()
        waitForTag("tasbih_counter")
        waitForText("ذِكْرٌ مُعَدَّل")
        waitForText("الدورة الحالية 1 من 9")

        composeRule.onNodeWithTag("nav_settings").performClick()
        waitForTag("open_achievements")
        composeRule.onNodeWithTag("open_appearance_settings").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("theme_dark"))
        composeRule.onNodeWithTag("theme_dark").performClick()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("font_baqiyat"))
        composeRule.onNodeWithTag("font_baqiyat").performClick()
        waitForSelectedTag("font_baqiyat")
        composeRule.onNodeWithTag("font_baqiyat").assertIsSelected()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("open_achievements"))
        composeRule.onNodeWithTag("open_achievements").performScrollTo().performClick()
        waitForTag("achievements_screen")

        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_expand_sources"))
        composeRule.onNodeWithTag("activity_expand_sources").performClick()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("source_statistics"))
        composeRule.onNodeWithTag("source_statistics").assertIsDisplayed()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_expand_hours"))
        composeRule.onNodeWithTag("activity_expand_hours").performClick()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("hourly_activity"))
        composeRule.onNodeWithTag("hourly_activity").assertIsDisplayed()
        composeRule.onNodeWithTag("hour_range_Year").performClick().assertIsSelected()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_expand_trends"))
        composeRule.onNodeWithTag("activity_expand_trends").performClick()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("long_term_activity"))
        composeRule.onNodeWithTag("long_term_activity").assertIsDisplayed()
        composeRule.onNodeWithTag("trend_mode_Yearly").performClick().assertIsSelected()

        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("weekly_values"))
        composeRule.onNodeWithTag("weekly_values").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("weekly_value_list").assertIsDisplayed()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_expand_calendar"))
        composeRule.onNodeWithTag("activity_expand_calendar").performClick()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_calendar"))
        composeRule.onNodeWithTag("activity_calendar").assertIsDisplayed()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasTestTag("activity_expand_history"))
        composeRule.onNodeWithTag("activity_expand_history").performClick()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasText("تسبيح"))
        composeRule.onAllNodesWithText("تسبيح")[0].assertIsDisplayed()
        composeRule.onNodeWithTag("achievements_list").performScrollToNode(hasText("ذكر من حصن المسلم"))
        composeRule.onAllNodesWithText("ذكر من حصن المسلم")[0].assertIsDisplayed()
    }

    @Test
    fun aboutScreenShowsOfficialContactChannels() {
        waitForTag("home_featured")

        composeRule.onNodeWithTag("nav_settings").performClick()
        waitForTag("settings_list")
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("open_about"))
        composeRule.onNodeWithTag("open_about").performClick()

        waitForTag("about_screen")
        composeRule.onNodeWithTag("about_list").performScrollToNode(hasTestTag("about_whatsapp"))
        composeRule.onNodeWithTag("about_whatsapp").assertIsDisplayed()
        composeRule.onNodeWithTag("about_list").performScrollToNode(hasTestTag("about_email"))
        composeRule.onNodeWithTag("about_email").assertIsDisplayed()
        composeRule.onNodeWithTag("about_list").performScrollToNode(hasTestTag("about_contact_page"))
        composeRule.onNodeWithTag("about_contact_page").assertIsDisplayed()
    }

    @Test
    fun tasbihPhraseManagerEditsAndDeletesCustomPhrasesAndStopsAtFirstPhrase() {
        val originalPhrase = "ذِكْرُ اختبارِ الجدول"
        val updatedPhrase = "ذِكْرُ اختبارِ الجدول المعدّل"

        waitForTag("home_featured")
        composeRule.onNodeWithTag("nav_tasbih").performClick()
        waitForTag("tasbih_counter")
        composeRule.onNodeWithTag("tasbih_actions").performClick()
        composeRule.onNodeWithTag("tasbih_manage_phrases").performClick()
        waitForTag("tasbih_phrase_manager_screen")

        composeRule.onNodeWithTag("tasbih_phrase_row_subhan_allah").performClick()
        waitForSelectedTag("tasbih_phrase_row_subhan_allah")
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForTag("tasbih_counter")
        composeRule.onNodeWithTag("tasbih_previous_phrase").assertIsNotEnabled()
        composeRule.onNodeWithTag("tasbih_next_phrase").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_previous_phrase").assertIsEnabled()
            }.isSuccess
        }

        composeRule.onNodeWithTag("tasbih_actions").performClick()
        composeRule.onNodeWithTag("tasbih_manage_phrases").performClick()
        waitForTag("tasbih_phrase_manager_screen")
        composeRule.onNodeWithTag("tasbih_manager_add").performClick()
        waitForTag("tasbih_phrase_editor")
        composeRule.onNodeWithTag("custom_text").performTextInput(originalPhrase)
        composeRule.onNodeWithTag("custom_save").performClick()
        composeRule.onNodeWithTag("tasbih_phrase_manager_list")
            .performScrollToNode(hasTestTag("tasbih_phrase_actions_selected"))
        waitForText(originalPhrase)

        composeRule.onNodeWithTag("tasbih_phrase_actions_selected").performClick()
        composeRule.onNodeWithTag("tasbih_phrase_edit_selected").performClick()
        waitForTag("tasbih_phrase_editor")
        composeRule.onNodeWithTag("custom_text").performTextClearance()
        composeRule.onNodeWithTag("custom_text").performTextInput(updatedPhrase)
        composeRule.onNodeWithTag("custom_save").performClick()
        waitForText(updatedPhrase)

        composeRule.onNodeWithTag("tasbih_phrase_actions_selected").performClick()
        composeRule.onNodeWithTag("tasbih_phrase_delete_selected").performClick()
        waitForText("حذف الذكر؟")
        composeRule.onNodeWithText("حذف الذكر").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(updatedPhrase).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun tasbihCountsSurvivePhraseAndTabNavigationAndRecreation() {
        waitForTag("home_featured")
        composeRule.onNodeWithTag("nav_tasbih").performClick()
        waitForTag("tasbih_counter")
        lateinit var viewModel: SakinahViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[SakinahViewModel::class.java]
            viewModel.selectPhrase("subhan_allah")
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.selectedPhrase.id == "subhan_allah"
        }
        composeRule.activityRule.scenario.onActivity { viewModel.resetTasbih() }
        waitForTasbihTotal(0)
        repeat(2) { composeRule.onNodeWithTag("tasbih_counter").performClick() }
        waitForTasbihTotal(2)

        composeRule.onNodeWithTag("tasbih_next_phrase").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.selectedPhrase.id != "subhan_allah"
        }
        composeRule.activityRule.scenario.onActivity { viewModel.resetTasbih() }
        waitForTasbihTotal(0)
        repeat(3) { composeRule.onNodeWithTag("tasbih_counter").performClick() }
        waitForTasbihTotal(3)

        composeRule.onNodeWithTag("tasbih_previous_phrase").performClick()
        waitForTasbihTotal(2)
        composeRule.onNodeWithTag("nav_home").performClick()
        waitForTag("home_featured")
        composeRule.onNodeWithTag("nav_tasbih").performClick()
        waitForTasbihTotal(2)
        composeRule.activityRule.scenario.recreate()
        waitForTag("tasbih_counter")
        waitForTasbihTotal(2)

        composeRule.onNodeWithTag("tasbih_next_phrase").performClick()
        waitForTasbihTotal(3)
        composeRule.onNodeWithTag("tasbih_actions").performClick()
        composeRule.onNodeWithText("تصفير").performClick()
        waitForText("تصفير المسبحة؟")
        composeRule.onNodeWithText("تصفير").performClick()
        waitForTasbihTotal(0)
        composeRule.onNodeWithTag("tasbih_previous_phrase").performClick()
        waitForTasbihTotal(2)
    }

    @Test
    fun tasbihFocusModeHidesNavigationAndCyclesProgressWithoutResettingTotal() {
        waitForTag("home_featured")
        composeRule.onNodeWithTag("nav_tasbih").performClick()
        waitForTag("tasbih_counter")

        composeRule.activityRule.scenario.onActivity { activity ->
            ViewModelProvider(activity)[SakinahViewModel::class.java].apply {
                resetTasbih()
                setTasbihTarget(1)
            }
        }
        waitForText("الدورة الحالية 0 من 1")

        composeRule.onNodeWithTag("tasbih_focus_mode").performClick()
        waitForTag("tasbih_focus_screen")
        waitForAbsentTag("nav_home")
        waitForAbsentTag("tasbih_add")
        composeRule.onNodeWithTag("tasbih_focus_phrase_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_exit_focus_mode").assertIsDisplayed()
        composeRule.onNodeWithText("إلغاء التوسيع").assertIsDisplayed()
        val previous = composeRule.onNodeWithTag("tasbih_previous_phrase").fetchSemanticsNode().boundsInRoot
        val exit = composeRule.onNodeWithTag("tasbih_exit_focus_mode").fetchSemanticsNode().boundsInRoot
        val next = composeRule.onNodeWithTag("tasbih_next_phrase").fetchSemanticsNode().boundsInRoot
        val phrase = composeRule.onNodeWithTag("tasbih_phrase_text").fetchSemanticsNode().boundsInRoot
        val header = composeRule.onNodeWithTag("tasbih_focus_phrase_header").fetchSemanticsNode().boundsInRoot
        assertTrue(previous.center.x > exit.center.x && exit.center.x > next.center.x)
        assertEquals(previous.center.y, exit.center.y, 1f)
        assertEquals(next.center.y, exit.center.y, 1f)
        assertTrue(phrase.top >= maxOf(previous.bottom, exit.bottom, next.bottom))
        assertEquals(header.width, phrase.width, 1f)
        composeRule.onNodeWithTag("tasbih_counter").assertIsDisplayed().performClick()

        waitForText("الدورة الحالية 1 من 1")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("الدورة الحالية 0 من 1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(
            testTag = "tasbih_total_count",
            useUnmergedTree = true,
        ).assertTextEquals("1")

        composeRule.onNodeWithTag("tasbih_exit_focus_mode").performClick()
        waitForTag("tasbih_standard_screen")
        waitForTag("nav_home")
        waitForAbsentTag("tasbih_exit_focus_mode")
        composeRule.onNodeWithTag(
            testTag = "tasbih_total_count",
            useUnmergedTree = true,
        ).assertTextEquals("1")
    }

    @Test
    fun completionSoundSettingsCanBePreviewedAndSurviveRecreation() {
        waitForTag("home_featured")
        composeRule.onNodeWithTag("nav_settings").performClick()
        waitForTag("settings_list")
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("open_sound_settings"))
        composeRule.onNodeWithTag("open_sound_settings").performClick()
        waitForTag("sound_settings_screen")

        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("sound_choice_triple_pulse"))
        composeRule.onNodeWithTag("sound_choice_triple_pulse").performClick()
        waitForSelectedTag("sound_choice_triple_pulse")
        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("sound_volume"))
        composeRule.onNodeWithTag("sound_volume").performSemanticsAction(SemanticsActions.SetProgress) { it(0.7f) }
        waitForText("70٪")
        CompletionSound.entries.forEach { sound ->
            composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("sound_preview_${sound.storageId}"))
            composeRule.onNodeWithTag("sound_preview_${sound.storageId}").performClick()
        }
        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("tasbih_sound_enabled"))
        composeRule.onNodeWithTag("tasbih_sound_enabled").performClick()
        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("reader_sound_enabled"))
        composeRule.onNodeWithTag("reader_sound_enabled").performClick()

        composeRule.activityRule.scenario.recreate()
        waitForTag("sound_settings_screen")
        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("tasbih_sound_enabled"))
        composeRule.onNodeWithTag("tasbih_sound_enabled").assertIsOff()
        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("reader_sound_enabled"))
        composeRule.onNodeWithTag("reader_sound_enabled").assertIsOff()
        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("sound_choice_triple_pulse"))
        composeRule.onNodeWithTag("sound_choice_triple_pulse").assertIsSelected()
        composeRule.onNodeWithTag("sound_settings_list").performScrollToNode(hasTestTag("sound_volume_value"))
        waitForText("70٪")

        composeRule.activityRule.scenario.onActivity { activity ->
            ViewModelProvider(activity)[SakinahViewModel::class.java].apply {
                setTasbihCompletionSoundEnabled(true)
                setDhikrCompletionSoundEnabled(true)
            }
        }
    }

    @Test
    fun readerFocusModeHidesReaderControlsAndExitsWithoutIncrementing() {
        waitForTag("home_featured")
        lateinit var viewModel: SakinahViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[SakinahViewModel::class.java].apply {
                restartCollection("hisn_001")
                setAutoAdvanceDhikrEnabled(false)
            }
        }

        composeRule.onNodeWithTag("nav_library").performClick()
        waitForTag("library_group_DailyLife")
        composeRule.onNodeWithTag("library_group_DailyLife").performClick()
        waitForTag("collection_hisn_001")
        composeRule.onNodeWithTag("collection_hisn_001").performClick()
        waitForTag("reader_count_button")
        composeRule.onNodeWithTag("reader_focus_mode").performClick()

        waitForTag("reader_focus_screen")
        waitForAbsentTag("reader_settings")
        waitForAbsentTag("reader_focus_mode")
        waitForAbsentTag("reader_session_header")
        waitForAbsentTag("reader_reference_toggle")
        composeRule.onNodeWithTag("reader_count_button").assertIsDisplayed()
        composeRule.onNodeWithText("إلغاء التوسيع").assertIsDisplayed()
        val previous = composeRule.onNodeWithTag("reader_previous_dhikr").fetchSemanticsNode().boundsInRoot
        val exit = composeRule.onNodeWithTag("reader_exit_focus_mode").fetchSemanticsNode().boundsInRoot
        val next = composeRule.onNodeWithTag("reader_next_dhikr").fetchSemanticsNode().boundsInRoot
        val text = composeRule.onNodeWithTag("reader_dhikr_text").fetchSemanticsNode().boundsInRoot
        val dock = composeRule.onNodeWithTag("reader_count_button").fetchSemanticsNode().boundsInRoot
        assertTrue(previous.center.x > exit.center.x && exit.center.x > next.center.x)
        assertEquals(previous.center.y, exit.center.y, 1f)
        assertEquals(next.center.y, exit.center.y, 1f)
        assertTrue(text.top > exit.bottom && text.bottom < dock.top)
        assertTrue(text.width > dock.width * 0.9f)
        composeRule.onNodeWithText("المتبقي 1", useUnmergedTree = true).assertIsDisplayed()

        // Navigation remains navigation in focus mode and must not count a repetition.
        composeRule.onNodeWithTag("reader_next_dhikr").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.progressFor("hisn_001").entryIndex == 1
        }
        composeRule.activityRule.scenario.onActivity {
            val progress = viewModel.uiState.value.progressFor("hisn_001")
            assertEquals(0, progress.repetitionCountFor(0))
            assertEquals(0, progress.repetitionCountFor(1))
        }
        composeRule.onNodeWithTag("reader_previous_dhikr").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.progressFor("hisn_001").entryIndex == 0
        }

        // The scrollable text canvas counts exactly once, then focus mode advances even with
        // the ordinary reader auto-advance preference disabled.
        composeRule.onNodeWithTag("reader_dhikr_text").performTouchInput { click(center) }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.progressFor("hisn_001").repetitionCountFor(0) == 1
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.progressFor("hisn_001").entryIndex == 1
        }
        composeRule.activityRule.scenario.onActivity {
            val progress = viewModel.uiState.value.progressFor("hisn_001")
            assertEquals(1, progress.repetitionCountFor(0))
            assertEquals(0, progress.repetitionCountFor(1))
        }
        composeRule.onNodeWithTag("reader_exit_focus_mode").assertIsDisplayed().performClick()

        waitForTag("reader_focus_mode")
        waitForTag("reader_session_header")
        waitForAbsentTag("reader_exit_focus_mode")
        composeRule.onNodeWithTag(
            testTag = "reader_repetition_count",
            useUnmergedTree = true,
        ).assertTextEquals("0")

        composeRule.onNodeWithTag("reader_focus_mode").performClick()
        waitForTag("reader_focus_screen")
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForTag("reader_focus_mode")
        composeRule.onNodeWithTag(
            testTag = "reader_repetition_count",
            useUnmergedTree = true,
        ).assertTextEquals("0")

        composeRule.activityRule.scenario.onActivity {
            viewModel.setAutoAdvanceDhikrEnabled(true)
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.autoAdvanceDhikrEnabled
        }
    }

    @Test
    fun readerNavigationStartsTheNextTextAtTheTopAfterScrolling() {
        waitForTag("home_featured")
        lateinit var viewModel: SakinahViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[SakinahViewModel::class.java].apply {
                restartCollection("hisn_001")
                setTextScale(1.4f)
            }
        }
        composeRule.onNodeWithTag("nav_library").performClick()
        waitForTag("library_group_DailyLife")
        composeRule.onNodeWithTag("library_group_DailyLife").performClick()
        waitForTag("collection_hisn_001")
        composeRule.onNodeWithTag("collection_hisn_001").performClick()
        waitForTag("reader_focus_mode")
        composeRule.onNodeWithTag("reader_focus_mode").performClick()
        repeat(3) { index ->
            composeRule.onNodeWithTag("reader_next_dhikr").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.uiState.value.progressFor("hisn_001").entryIndex == index + 1
            }
        }
        val scrollNode = composeRule.onNodeWithTag("reader_dhikr_scroll", useUnmergedTree = true)
        scrollNode.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 600f) }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            scrollNode.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f
        }
        composeRule.onNodeWithTag("reader_previous_dhikr").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.progressFor("hisn_001").entryIndex == 2
        }
        composeRule.waitForIdle()
        assertEquals(0f, scrollNode.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value(), 0.5f)
        composeRule.onNodeWithTag("reader_count_button").assertIsDisplayed()
        assertEquals(0, viewModel.uiState.value.progressFor("hisn_001").repetitionCount)
        composeRule.activityRule.scenario.onActivity { viewModel.setTextScale(1f) }
    }

    private fun waitForTasbihTotal(count: Int) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_total_count", useUnmergedTree = true)
                    .assertTextEquals(count.toString())
            }.isSuccess
        }
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForAbsentTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForAbsentText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForSelectedTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsSelected()
            }.isSuccess
        }
    }
}

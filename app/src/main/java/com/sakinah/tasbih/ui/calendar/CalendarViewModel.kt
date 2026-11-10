package com.sakinah.tasbih.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakinah.tasbih.calendar.CalendarSettings
import com.sakinah.tasbih.calendar.HijriMonth
import com.sakinah.tasbih.calendar.PrimaryCalendar
import com.sakinah.tasbih.calendar.WeekStart
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.widget.CalendarWidgetUpdater
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A request to show the month that holds [date]. Counted, so asking twice still moves there. */
data class CalendarJump(val date: LocalDate, val serial: Int)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarPreferencesRepository(application)
    private val widgets = CalendarWidgetUpdater(application)

    /** Null until the saved settings have been read once. */
    val settings: StateFlow<CalendarSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _jump = MutableStateFlow<CalendarJump?>(null)
    val jump: StateFlow<CalendarJump?> = _jump.asStateFlow()

    fun showDate(date: LocalDate) {
        _jump.value = CalendarJump(date, (_jump.value?.serial ?: 0) + 1)
    }

    fun setPrimary(primary: PrimaryCalendar) = save { repository.setPrimary(primary) }

    fun setWeekStart(start: WeekStart) = save { repository.setWeekStart(start) }

    fun setHijriOffset(days: Int) = save { repository.setHijriOffset(days) }

    fun setShowOtherOccasions(show: Boolean) = save { repository.setShowOtherOccasions(show) }

    fun setShowWhiteDays(show: Boolean) = save { repository.setShowWhiteDays(show) }

    fun setCorrection(month: HijriMonth, days: Int) = save { repository.setCorrection(month, days) }

    /** Opening the app also brings the widgets up to date, in case an alarm was held back. */
    fun refreshWidgets() {
        viewModelScope.launch { widgets.updateAll() }
    }

    /** Saved for good, then every calendar widget is drawn again with it. */
    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            widgets.updateAll()
        }
    }
}

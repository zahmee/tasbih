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

/** [granted] is the phone's permission; the lists are empty while the agenda is off. */
data class AgendaState(
    val granted: Boolean = false,
    val calendars: List<com.sakinah.tasbih.calendar.DeviceCalendar> = emptyList(),
    val events: Map<LocalDate, List<com.sakinah.tasbih.calendar.AgendaEvent>> = emptyMap(),
)

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

    /** The calendar has moved there, so opening it again must not move it once more. */
    fun jumpHandled(jump: CalendarJump) {
        _jump.compareAndSet(jump, null)
    }

    fun setPrimary(primary: PrimaryCalendar) = save { repository.setPrimary(primary) }

    fun setWeekStart(start: WeekStart) = save { repository.setWeekStart(start) }

    fun toggleWeekendDay(day: java.time.DayOfWeek) = save {
        val current = settings.value?.weekendDays ?: com.sakinah.tasbih.calendar.DEFAULT_WEEKEND
        repository.setWeekendDays(if (day in current) current - day else current + day)
    }

    private val _agenda = MutableStateFlow(AgendaState())

    /** The phone's calendars and their events around the month on screen, while the user shows them. */
    val agenda: StateFlow<AgendaState> = _agenda.asStateFlow()

    private var agendaAround: LocalDate = LocalDate.now()
    private var agendaJob: kotlinx.coroutines.Job? = null

    /** Reads the phone's calendars for two months on either side of [around], off the main thread. */
    fun loadAgenda(around: LocalDate = agendaAround) {
        agendaAround = around
        agendaJob?.cancel()
        agendaJob = viewModelScope.launch {
            val app = getApplication<Application>()
            val on = (settings.value ?: repository.current()).agendaEnabled
            val granted = com.sakinah.tasbih.calendar.DeviceAgenda.granted(app)
            if (!on || !granted) {
                _agenda.value = AgendaState(granted = granted)
                return@launch
            }
            val (calendars, events) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.sakinah.tasbih.calendar.DeviceAgenda.calendars(app) to
                    com.sakinah.tasbih.calendar.DeviceAgenda.events(app, around.minusMonths(2), around.plusMonths(2))
            }
            _agenda.value = AgendaState(granted = true, calendars = calendars, events = events)
        }
    }

    fun setAgendaEnabled(on: Boolean) = viewModelScope.launch {
        repository.setAgendaEnabled(on)
        loadAgenda()
        widgets.updateAll()
    }

    fun setAgendaColor(calendarId: Long, color: com.sakinah.tasbih.calendar.DayColor) = save { repository.setAgendaColor(calendarId, color) }

    fun setAgendaShown(calendarId: Long, shown: Boolean) = save { repository.setAgendaShown(calendarId, shown) }

    fun setHijriColor(color: com.sakinah.tasbih.calendar.DayColor) = save { repository.setHijriColor(color) }

    fun setGregorianColor(color: com.sakinah.tasbih.calendar.DayColor) = save { repository.setGregorianColor(color) }

    fun setGridStyle(style: com.sakinah.tasbih.calendar.GridStyle) = save { repository.setGridStyle(style) }

    fun setHijriOffset(days: Int) = save { repository.setHijriOffset(days) }


    fun setShowWhiteDays(show: Boolean) = save { repository.setShowWhiteDays(show) }

    fun addPersonalOccasion(name: String, month: Int, day: Int) = save {
        repository.addPersonalOccasion(
            com.sakinah.tasbih.calendar.PersonalOccasion(java.util.UUID.randomUUID().toString().take(8), name, month, day),
        )
    }

    fun removePersonalOccasion(id: String) = save { repository.removePersonalOccasion(id) }

    /** The prayer alarm also wakes at Maghrib for the date widget while this is on. */
    fun setHijriDayFromMaghrib(on: Boolean) = save {
        repository.setHijriDayFromMaghrib(on)
        com.sakinah.tasbih.alerts.PrayerAlarms.schedule(getApplication())
    }

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

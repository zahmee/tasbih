package com.sakinah.tasbih.ui.wird

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakinah.tasbih.data.AppPreferencesRepository
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.data.PrayerPreferencesRepository
import com.sakinah.tasbih.data.WirdRepository
import com.sakinah.tasbih.wird.WirdBoard
import com.sakinah.tasbih.wird.WirdEnvironment
import com.sakinah.tasbih.wird.WirdItem
import com.sakinah.tasbih.wird.WirdPreset
import com.sakinah.tasbih.wird.WirdSync
import com.sakinah.tasbih.wird.WirdTask
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the wird's page and the home screen's line show. */
data class WirdState(
    val board: WirdBoard,
    val tasks: List<WirdTask>,
    /** Suggestions not yet on the list. */
    val presets: List<WirdPreset>,
    /** Without a place there are no prayer times, so tasks set by a prayer have no time yet. */
    val hasPlace: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WirdViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WirdRepository(application)
    private val now = MutableStateFlow(Instant.now())

    private val calendar = combine(
        CalendarPreferencesRepository(application).settings,
        PrayerPreferencesRepository(application).settings,
        AppPreferencesRepository(application).preferences.map { it.dailyReset }.distinctUntilChanged(),
    ) { calendarSettings, prayerSettings, reset ->
        WirdEnvironment.calendar(calendarSettings, prayerSettings, reset) to (prayerSettings.place != null)
    }

    /** The readings are read from the start of the history the page shows, which moves with the day. */
    private val readings = combine(calendar, now) { (calendar, _), instant -> WirdBoard.readingsFrom(instant, calendar) }
        .distinctUntilChanged()
        .flatMapLatest(repository::readingsSince)

    val state: StateFlow<WirdState?> = combine(repository.tasks, repository.ticked, readings, calendar, now) { tasks, ticked, readings, (calendar, hasPlace), instant ->
        WirdState(
            board = WirdBoard.build(tasks, ticked, readings, instant, calendar),
            tasks = tasks,
            presets = WirdPreset.entries.filter { preset -> tasks.none { it.presetId == preset.id } },
            hasPlace = hasPlace,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.prune(LocalDate.now())
        }
        // The day turns and a task's time comes while the page stays open.
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                now.value = Instant.now()
            }
        }
    }

    /** After the app comes back: the day may have turned, a reading may be done, an alarm may be lost. */
    fun refresh() {
        now.value = Instant.now()
        viewModelScope.launch { WirdSync.changed(getApplication()) }
    }

    fun setDone(item: WirdItem, done: Boolean) = viewModelScope.launch {
        repository.setDone(item.task, item.day, done)
        WirdSync.refreshViews(getApplication())
    }

    fun save(task: WirdTask) = viewModelScope.launch {
        repository.save(task)
        WirdSync.changed(getApplication())
    }

    fun addPreset(preset: WirdPreset) = viewModelScope.launch {
        repository.addPreset(preset)
        WirdSync.changed(getApplication())
    }

    fun delete(task: WirdTask) = viewModelScope.launch {
        repository.delete(task.id)
        com.sakinah.tasbih.wird.WirdNotifier.cancel(getApplication(), task.id)
        WirdSync.changed(getApplication())
    }
}

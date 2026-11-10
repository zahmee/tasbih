package com.sakinah.tasbih.ui.prayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakinah.tasbih.R
import com.sakinah.tasbih.alerts.PrayerAlarms
import com.sakinah.tasbih.prayer.AlertSettings
import com.sakinah.tasbih.prayer.PrayerAlert
import com.sakinah.tasbih.widget.PrayerWidgetUpdater
import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.data.PrayerPreferencesRepository
import com.sakinah.tasbih.prayer.AsrChoice
import com.sakinah.tasbih.prayer.DeviceLocation
import com.sakinah.tasbih.prayer.HighLatitudeChoice
import com.sakinah.tasbih.prayer.PlaceSearch
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerMethod
import com.sakinah.tasbih.prayer.PrayerPlace
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.prayer.PrayerSettings
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException

/** What the place screen shows while a search or a position is on its way. */
data class PlaceSearchState(
    val results: List<PrayerPlace> = emptyList(),
    val searched: Boolean = false,
    val searching: Boolean = false,
    val searchFailed: Boolean = false,
    val locating: Boolean = false,
    val locationFailed: Boolean = false,
)

/** The saved settings and, once a place is chosen, the schedule worked out from them. */
data class PrayerState(val settings: PrayerSettings, val schedule: PrayerSchedule?)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PrayerPreferencesRepository(application)
    private val calendar = CalendarPreferencesRepository(application)
    private val placeSearch = PlaceSearch()
    private var searchJob: Job? = null

    /** Null until the saved settings have been read once. */
    val state: StateFlow<PrayerState?> = combine(repository.settings, calendar.settings) { settings, calendarSettings ->
        // Ramadan, for the Umm al-Qura rule, follows the user's own Hijri corrections.
        PrayerState(settings, settings.place?.let { PrayerSchedule(it, settings, HijriCalendar.from(calendarSettings)) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null until the saved alert settings have been read once. */
    val alerts: StateFlow<AlertSettings?> = repository.alerts.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _search = MutableStateFlow(PlaceSearchState())
    val search: StateFlow<PlaceSearchState> = _search.asStateFlow()

    fun search(query: String) {
        searchJob?.cancel()
        if (query.trim().length < PlaceSearch.MIN_QUERY_LENGTH) return
        searchJob = viewModelScope.launch {
            _search.update { it.copy(searching = true, searchFailed = false) }
            val results = try {
                placeSearch.search(query)
            } catch (_: IOException) {
                null
            } catch (_: JSONException) {
                null
            }
            _search.update {
                it.copy(results = results.orEmpty(), searched = true, searching = false, searchFailed = results == null)
            }
        }
    }

    fun choose(place: PrayerPlace) = save { repository.setPlace(place) }

    /** Called once the location permission has been granted. */
    fun useDeviceLocation() {
        viewModelScope.launch {
            _search.update { it.copy(locating = true, locationFailed = false) }
            val app = getApplication<Application>()
            val place = DeviceLocation.currentPlace(app, app.getString(R.string.prayer_my_location))
            if (place != null) {
                repository.setPlace(place)
                refreshAlarms()
            }
            _search.update { it.copy(locating = false, locationFailed = place == null) }
        }
    }

    fun setMethod(method: PrayerMethod?) = save { repository.setMethod(method) }

    fun setAsr(choice: AsrChoice) = save { repository.setAsr(choice) }

    fun setHighLatitude(choice: HighLatitudeChoice) = save { repository.setHighLatitude(choice) }

    fun setAdjustment(prayer: Prayer, minutes: Int) = save { repository.setAdjustment(prayer, minutes) }

    fun setAlert(prayer: Prayer, alert: PrayerAlert) = save { repository.setAlert(prayer, alert) }

    fun setTone(tone: String) = save { repository.setTone(tone) }

    fun setAdhan(adhan: String, fajr: Boolean) = save { repository.setAdhan(adhan, fajr) }

    fun setCustomAdhan(uri: String, fajr: Boolean) = save { repository.setCustomAdhan(uri, fajr) }

    fun testAlert() {
        val next = state.value?.schedule?.next()?.prayer ?: Prayer.DHUHR
        PrayerAlarms.scheduleTest(getApplication(), next)
    }

    /** After the screen comes back, as a permission may have been granted meanwhile. */
    fun refresh() {
        viewModelScope.launch { refreshAlarms() }
    }

    /** Every change can move a time, so the alarms and the widgets are set again after each. */
    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            refreshAlarms()
        }
    }

    private suspend fun refreshAlarms() {
        val app = getApplication<Application>()
        PrayerAlarms.schedule(app)
        PrayerWidgetUpdater(app).updateAll()
    }
}

package com.sakinah.tasbih.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sakinah.tasbih.calendar.CalendarSettings
import com.sakinah.tasbih.calendar.DEFAULT_WEEKEND
import com.sakinah.tasbih.calendar.DayColor
import com.sakinah.tasbih.calendar.GridStyle
import java.time.DayOfWeek
import com.sakinah.tasbih.calendar.HijriMonth
import com.sakinah.tasbih.calendar.PrimaryCalendar
import com.sakinah.tasbih.calendar.SavedMaps
import com.sakinah.tasbih.calendar.WeekStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// A file of its own, so the calendar can never damage the remembrance and tasbih progress.
private val Context.calendarDataStore by preferencesDataStore(name = "calendar_preferences")

class CalendarPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.calendarDataStore)

    val settings: Flow<CalendarSettings> = dataStore.data.map(::read)

    suspend fun current(): CalendarSettings = settings.first()

    suspend fun setPrimary(primary: PrimaryCalendar) = dataStore.edit { it[Keys.primary] = primary.name }

    suspend fun setWeekStart(start: WeekStart) = dataStore.edit { it[Keys.weekStart] = start.name }

    suspend fun setWeekendDays(days: Set<DayOfWeek>) = dataStore.edit { it[Keys.weekendDays] = days.mapTo(mutableSetOf()) { d -> d.name } }

    suspend fun setHijriColor(color: DayColor) = dataStore.edit { it[Keys.hijriColor] = color.name }

    suspend fun setGregorianColor(color: DayColor) = dataStore.edit { it[Keys.gregorianColor] = color.name }

    suspend fun setGridStyle(style: GridStyle) = dataStore.edit { it[Keys.gridStyle] = style.name }

    suspend fun setAgendaEnabled(on: Boolean) = dataStore.edit { it[Keys.agendaEnabled] = on }

    suspend fun setAgendaColor(calendarId: Long, color: DayColor) = dataStore.edit { preferences ->
        val colors = readAgendaColors(preferences[Keys.agendaColors])
        val updated = if (color == DayColor.THEME) colors - calendarId else colors + (calendarId to color)
        preferences[Keys.agendaColors] = updated.mapTo(mutableSetOf()) { (id, c) -> "$id=${c.name}" }
    }

    suspend fun setAgendaShown(calendarId: Long, shown: Boolean) = dataStore.edit { preferences ->
        val hidden = preferences[Keys.agendaHidden].orEmpty()
        preferences[Keys.agendaHidden] = if (shown) hidden - calendarId.toString() else hidden + calendarId.toString()
    }

    private fun readAgendaColors(lines: Set<String>?): Map<Long, DayColor> = lines.orEmpty().mapNotNull { line ->
        val id = line.substringBefore('=').toLongOrNull()
        val color = DayColor.entries.firstOrNull { it.name == line.substringAfter('=', "") }
        if (id == null || color == null) null else id to color
    }.toMap()

    suspend fun setHijriOffset(days: Int) = dataStore.edit { it[Keys.hijriOffset] = days }


    suspend fun setShowWhiteDays(show: Boolean) = dataStore.edit { it[Keys.showWhiteDays] = show }

    suspend fun setHijriDayFromMaghrib(on: Boolean) = dataStore.edit { it[Keys.hijriDayFromMaghrib] = on }

    suspend fun addPersonalOccasion(occasion: com.sakinah.tasbih.calendar.PersonalOccasion) = dataStore.edit {
        val saved = com.sakinah.tasbih.calendar.PersonalOccasions.decode(it[Keys.personalOccasions].orEmpty())
        it[Keys.personalOccasions] = com.sakinah.tasbih.calendar.PersonalOccasions.encode(saved + occasion)
    }

    suspend fun removePersonalOccasion(id: String) = dataStore.edit {
        val saved = com.sakinah.tasbih.calendar.PersonalOccasions.decode(it[Keys.personalOccasions].orEmpty())
        it[Keys.personalOccasions] = com.sakinah.tasbih.calendar.PersonalOccasions.encode(saved.filterNot { o -> o.id == id })
    }

    /** Zero clears the correction, so the month follows the computed calendar again. */
    suspend fun setCorrection(month: HijriMonth, days: Int) = dataStore.edit { preferences ->
        val corrections = SavedMaps.decodeStrings(preferences[Keys.monthCorrections])
        val updated = if (days == 0) corrections - month.key else corrections + (month.key to days)
        preferences[Keys.monthCorrections] = SavedMaps.encode(updated)
    }

    /** Moves a month widget by [direction] months, or back to the current month for zero. */
    suspend fun turnMonthWidget(appWidgetId: Int, direction: Int, today: Long) {
        dataStore.edit { preferences ->
            // Pages turned on an earlier day no longer count, for this widget or any other.
            val pages = if (preferences[Keys.monthWidgetPagesDay] == today) {
                SavedMaps.decodeInts(preferences[Keys.monthWidgetPages])
            } else {
                emptyMap()
            }
            val page = if (direction == 0) 0 else (pages[appWidgetId] ?: 0) + direction
            val updated = if (page == 0) pages - appWidgetId else pages + (appWidgetId to page)
            preferences[Keys.monthWidgetPages] = SavedMaps.encode(updated)
            preferences[Keys.monthWidgetPagesDay] = today
        }
    }

    suspend fun removeMonthWidget(appWidgetId: Int) = dataStore.edit { preferences ->
        val pages = SavedMaps.decodeInts(preferences[Keys.monthWidgetPages]) - appWidgetId
        preferences[Keys.monthWidgetPages] = SavedMaps.encode(pages)
    }

    private fun read(preferences: Preferences) = CalendarSettings(
        primary = enumOrDefault(preferences[Keys.primary], PrimaryCalendar.HIJRI),
        hijriOffsetDays = preferences[Keys.hijriOffset] ?: 0,
        monthCorrections = SavedMaps.decodeStrings(preferences[Keys.monthCorrections]),
        weekStart = enumOrDefault(preferences[Keys.weekStart], WeekStart.AUTO),
        // Never saved means the default; a saved empty set means the user keeps no day off.
        weekendDays = preferences[Keys.weekendDays]
            ?.mapNotNullTo(mutableSetOf()) { name -> DayOfWeek.entries.firstOrNull { it.name == name } }
            ?: DEFAULT_WEEKEND,
        hijriColor = enumOrDefault(preferences[Keys.hijriColor], DayColor.THEME),
        gregorianColor = enumOrDefault(preferences[Keys.gregorianColor], DayColor.THEME),
        gridStyle = enumOrDefault(preferences[Keys.gridStyle], GridStyle.TABLE),
        agendaEnabled = preferences[Keys.agendaEnabled] ?: false,
        agendaColors = readAgendaColors(preferences[Keys.agendaColors]),
        agendaHidden = preferences[Keys.agendaHidden].orEmpty().mapNotNullTo(mutableSetOf()) { it.toLongOrNull() },
        showWhiteDays = preferences[Keys.showWhiteDays] ?: true,
        hijriDayFromMaghrib = preferences[Keys.hijriDayFromMaghrib] ?: false,
        personalOccasions = com.sakinah.tasbih.calendar.PersonalOccasions.decode(preferences[Keys.personalOccasions].orEmpty()),
        monthWidgetPages = SavedMaps.decodeInts(preferences[Keys.monthWidgetPages]),
        monthWidgetPagesDay = preferences[Keys.monthWidgetPagesDay] ?: 0,
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val primary = stringPreferencesKey("primary_calendar")
        val hijriOffset = intPreferencesKey("hijri_offset_days")
        val monthCorrections = stringSetPreferencesKey("hijri_month_corrections")
        val weekStart = stringPreferencesKey("week_start")
        val weekendDays = stringSetPreferencesKey("weekend_days")
        val hijriColor = stringPreferencesKey("hijri_day_color")
        val gregorianColor = stringPreferencesKey("gregorian_day_color")
        val gridStyle = stringPreferencesKey("grid_style")
        val agendaEnabled = booleanPreferencesKey("agenda_enabled")
        val agendaColors = stringSetPreferencesKey("agenda_colors")
        val agendaHidden = stringSetPreferencesKey("agenda_hidden")
        val showWhiteDays = booleanPreferencesKey("show_white_days")
        val hijriDayFromMaghrib = booleanPreferencesKey("hijri_day_from_maghrib")
        val personalOccasions = stringSetPreferencesKey("personal_occasions")
        val monthWidgetPages = stringSetPreferencesKey("month_widget_pages")
        val monthWidgetPagesDay = longPreferencesKey("month_widget_pages_day")
    }
}

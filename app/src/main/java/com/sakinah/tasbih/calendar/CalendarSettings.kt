package com.sakinah.tasbih.calendar

/** Which calendar leads: its month fills the grid and its date is written larger. */
enum class PrimaryCalendar { HIJRI, GREGORIAN }

/** [AUTO] follows the phone's region, since Arabic weeks start on Saturday, Sunday or Monday. */
enum class WeekStart { AUTO, SATURDAY, SUNDAY, MONDAY }

/** A colour the user may give a calendar's day numbers. [THEME] keeps the screen's own ink. */
enum class DayColor { THEME, GREEN, COPPER, BLUE, RED, PURPLE }

/** [TABLE] draws every day in a square of its own; [PLAIN] leaves the days on the page. */
enum class GridStyle { PLAIN, TABLE }

data class CalendarSettings(
    val primary: PrimaryCalendar = PrimaryCalendar.HIJRI,
    /** Moves every Hijri day, from -2 to +2. */
    val hijriOffsetDays: Int = 0,
    /** Hijri month, as [HijriMonth.key], to how many days later than computed it began. */
    val monthCorrections: Map<String, Int> = emptyMap(),
    val weekStart: WeekStart = WeekStart.AUTO,
    /** The days off the user keeps, marked in the month. Friday and Saturday unless chosen; may be none. */
    val weekendDays: Set<java.time.DayOfWeek> = DEFAULT_WEEKEND,
    /** The colour of the Hijri day number in the month, large or small. */
    val hijriColor: DayColor = DayColor.THEME,
    /** The colour of the Gregorian day number in the month, large or small. */
    val gregorianColor: DayColor = DayColor.THEME,
    /** The table since November 20, 2026, at the user's wish. */
    val gridStyle: GridStyle = GridStyle.TABLE,
    /** Shows the events of the phone's own calendars. Off until the user turns it on. */
    val agendaEnabled: Boolean = false,
    /** A phone calendar's id to the colour the user gave it; [DayColor.THEME] keeps its own. */
    val agendaColors: Map<Long, DayColor> = emptyMap(),
    /** Phone calendars the user chose not to show here. */
    val agendaHidden: Set<Long> = emptySet(),
    val showWhiteDays: Boolean = true,
    /** The Islamic day begins with its night, so the user may let the Hijri date turn at Maghrib. */
    val hijriDayFromMaghrib: Boolean = false,
    /** The user's own occasions, each on a Hijri day that returns every year. */
    val personalOccasions: List<PersonalOccasion> = emptyList(),
    /** Month widget to how many months it was moved from the current one, on [monthWidgetPagesDay]. */
    val monthWidgetPages: Map<Int, Int> = emptyMap(),
    /** The day, as an epoch day, the pages were last turned. On any other day they start again. */
    val monthWidgetPagesDay: Long = 0,
) {
    fun monthWidgetPage(appWidgetId: Int, today: Long): Int =
        if (monthWidgetPagesDay == today) monthWidgetPages[appWidgetId] ?: 0 else 0
}

val DEFAULT_WEEKEND: Set<java.time.DayOfWeek> = setOf(java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY)

/**
 * Maps are saved as sets of "key=value" lines, which Preferences can hold. Anything that does
 * not read back as a number is dropped, so a damaged line can never stop the calendar opening.
 */
internal object SavedMaps {
    fun encode(map: Map<*, Int>): Set<String> = map.mapTo(mutableSetOf()) { (key, value) -> "$key=$value" }

    fun decodeStrings(lines: Set<String>?): Map<String, Int> = lines.orEmpty().mapNotNull { line ->
        val key = line.substringBefore('=', missingDelimiterValue = "")
        val value = line.substringAfter('=', missingDelimiterValue = "").toIntOrNull()
        if (key.isEmpty() || value == null) null else key to value
    }.toMap()

    fun decodeInts(lines: Set<String>?): Map<Int, Int> = decodeStrings(lines).mapNotNull { (key, value) ->
        key.toIntOrNull()?.let { it to value }
    }.toMap()
}

/**
 * The day whose Hijri date is today's. After [maghribToday], when the user lets the Hijri day
 * begin at sunset, it is already tomorrow's. Without a known Maghrib, as before a place is
 * chosen for prayer times, the date turns at midnight as usual.
 */
fun hijriDayFor(today: java.time.LocalDate, now: java.time.Instant, maghribToday: java.time.Instant?, fromMaghrib: Boolean): java.time.LocalDate =
    if (fromMaghrib && maghribToday != null && !now.isBefore(maghribToday)) today.plusDays(1) else today

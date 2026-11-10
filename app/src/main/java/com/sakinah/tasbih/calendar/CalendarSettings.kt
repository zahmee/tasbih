package com.sakinah.tasbih.calendar

/** Which calendar leads: its month fills the grid and its date is written larger. */
enum class PrimaryCalendar { HIJRI, GREGORIAN }

/** [AUTO] follows the phone's region, since Arabic weeks start on Saturday, Sunday or Monday. */
enum class WeekStart { AUTO, SATURDAY, SUNDAY, MONDAY }

data class CalendarSettings(
    val primary: PrimaryCalendar = PrimaryCalendar.HIJRI,
    /** Moves every Hijri day, from -2 to +2. */
    val hijriOffsetDays: Int = 0,
    /** Hijri month, as [HijriMonth.key], to how many days later than computed it began. */
    val monthCorrections: Map<String, Int> = emptyMap(),
    val weekStart: WeekStart = WeekStart.AUTO,
    /** Occasions that not all Muslims mark, such as the Mawlid. Off unless chosen. */
    val showOtherOccasions: Boolean = false,
    val showWhiteDays: Boolean = true,
    /** Month widget to how many months it was moved from the current one, on [monthWidgetPagesDay]. */
    val monthWidgetPages: Map<Int, Int> = emptyMap(),
    /** The day, as an epoch day, the pages were last turned. On any other day they start again. */
    val monthWidgetPagesDay: Long = 0,
) {
    fun monthWidgetPage(appWidgetId: Int, today: Long): Int =
        if (monthWidgetPagesDay == today) monthWidgetPages[appWidgetId] ?: 0 else 0
}

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

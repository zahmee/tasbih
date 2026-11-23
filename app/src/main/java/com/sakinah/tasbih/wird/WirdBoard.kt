package com.sakinah.tasbih.wird

import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * A task on one of its days. [auto] is set when the reading of its adhkar in the app, and not a
 * tick, made it done. [late] marks a task of one time whose day has gone by undone.
 */
data class WirdItem(
    val task: WirdTask,
    val day: LocalDate,
    val time: ZonedDateTime?,
    val done: Boolean,
    val auto: Boolean = false,
    val late: Boolean = false,
)

/** One day gone by, with each of its tasks done or not. */
data class WirdDay(val day: LocalDate, val items: List<WirdItem>) {
    val doneCount: Int get() = items.count(WirdItem::done)
}

/** A collection of adhkar read to its end, at [at]. */
data class WirdReading(val collectionId: String, val at: Instant)

/**
 * The wird as its page shows it: today's tasks, each task's next day, the tasks of one time that
 * are done, and the days just gone by. Built from what is stored, with nothing kept of its own.
 */
data class WirdBoard(
    val today: LocalDate,
    val todayItems: List<WirdItem>,
    val upcoming: List<WirdItem>,
    val finished: List<WirdItem>,
    val history: List<WirdDay>,
) {
    val doneToday: Int get() = todayItems.count(WirdItem::done)
    val totalToday: Int get() = todayItems.size

    /** The first task still to do today, which the home screen and the widget name. */
    val nextToDo: WirdItem? get() = todayItems.firstOrNull { !it.done }

    companion object {
        const val HISTORY_DAYS = 7L

        fun build(
            tasks: List<WirdTask>,
            /** Each task's days ticked done: a recurring task's own days, a task of one time's date. */
            ticked: Map<Long, Set<LocalDate>>,
            readings: List<WirdReading>,
            now: Instant,
            calendar: WirdCalendar,
        ): WirdBoard {
            val today = calendar.today(now)
            fun item(task: WirdTask, day: LocalDate, late: Boolean = false): WirdItem {
                val byHand = tickedOn(task, day, ticked)
                val read = !byHand && readOn(task, day, readings, calendar)
                return WirdItem(task, day, WirdPlanner.timeOn(task, day, calendar), byHand || read, auto = read, late = late && !byHand && !read)
            }
            fun created(task: WirdTask): LocalDate =
                if (task.createdAtMillis <= 0) LocalDate.MIN else calendar.today(Instant.ofEpochMilli(task.createdAtMillis))

            val todayItems = tasks.mapNotNull { task ->
                when {
                    task.repeat == WirdRepeat.ONCE -> {
                        val date = task.date ?: return@mapNotNull null
                        when {
                            date == today -> item(task, today)
                            // A day gone by undone stays in sight until it is done or removed.
                            date.isBefore(today) -> item(task, date, late = true).takeIf { !it.done }
                            else -> null
                        }
                    }
                    WirdPlanner.occursOn(task, today, calendar.hijri) -> item(task, today)
                    else -> null
                }
            }.sortedWith(itemOrder)

            val upcoming = tasks.mapNotNull { task ->
                WirdPlanner.nextDay(task, today, calendar.hijri)?.let { day -> item(task, day) }
            }.filterNot { it.task.repeat == WirdRepeat.ONCE && it.done }
                .sortedWith(compareBy<WirdItem> { it.day }.then(itemOrder))

            val finished = tasks.filter { it.repeat == WirdRepeat.ONCE && ticked[it.id].orEmpty().isNotEmpty() }
                .mapNotNull { task -> task.date?.let { item(task, it) } }
                .sortedByDescending { it.day }

            val history = (1L..HISTORY_DAYS).map(today::minusDays).mapNotNull { day ->
                val items = tasks.filter { task ->
                    if (task.repeat == WirdRepeat.ONCE) task.date == day
                    else !day.isBefore(created(task)) && WirdPlanner.occursOn(task, day, calendar.hijri)
                }.map { item(it, day) }.sortedWith(itemOrder)
                items.takeIf { it.isNotEmpty() }?.let { WirdDay(day, it) }
            }
            return WirdBoard(today, todayItems, upcoming, finished, history)
        }

        /** Ticked by hand: a recurring task on that day, a task of one time at all. */
        fun tickedOn(task: WirdTask, day: LocalDate, ticked: Map<Long, Set<LocalDate>>): Boolean =
            ticked[task.id].orEmpty().let { days -> if (task.repeat == WirdRepeat.ONCE) days.isNotEmpty() else day in days }

        /** Its adhkar read to their end within the day, as the app's day runs from its turning hour. */
        fun readOn(task: WirdTask, day: LocalDate, readings: List<WirdReading>, calendar: WirdCalendar): Boolean {
            val id = task.collectionId ?: return false
            val (start, end) = calendar.window(day)
            return readings.any { it.collectionId == id && !it.at.isBefore(start) && it.at.isBefore(end) }
        }

        fun doneOn(task: WirdTask, day: LocalDate, ticked: Map<Long, Set<LocalDate>>, readings: List<WirdReading>, calendar: WirdCalendar): Boolean =
            tickedOn(task, day, ticked) || readOn(task, day, readings, calendar)

        /** The readings the board can need: the history's days and today. */
        fun readingsFrom(now: Instant, calendar: WirdCalendar): Instant =
            calendar.window(calendar.today(now).minusDays(HISTORY_DAYS + 1)).first

        /** Undone before done; then by time, the untimed after, in the user's order. */
        private val itemOrder: Comparator<WirdItem> = compareBy<WirdItem> { it.done }
            .thenBy { it.time == null }
            .thenBy { it.time?.toInstant() }
            .thenBy { it.task.sortOrder }
            .thenBy { it.task.id }
    }
}

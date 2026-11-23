package com.sakinah.tasbih.wird

import com.sakinah.tasbih.prayer.Prayer
import java.time.DayOfWeek
import java.time.LocalDate

/** How often a task comes round. */
enum class WirdRepeat { ONCE, DAILY, WEEKLY, HIJRI_MONTHLY }

/** When in its day a task falls: at no set time, at a clock time, or before or after a prayer. */
enum class WirdTimeKind { NONE, CLOCK, PRAYER }

/**
 * One task of the user's wird, as on November 23, 2026 the user asked for: the day's acts of
 * worship and whatever else the user adds. A task tied to a collection of adhkar, as the
 * morning's, counts as done once that collection is read to its end in the app.
 */
data class WirdTask(
    val id: Long = 0,
    val title: String,
    /** The Hisn collection whose reading completes the task, or null for a task ticked by hand. */
    val collectionId: String? = null,
    /** The suggestion the task was made from, so it is not suggested twice. */
    val presetId: String? = null,
    val repeat: WirdRepeat = WirdRepeat.DAILY,
    /** For [WirdRepeat.WEEKLY]. */
    val weekdays: Set<DayOfWeek> = emptySet(),
    /** For [WirdRepeat.HIJRI_MONTHLY]: days of the Hijri month, 1 to 30. */
    val hijriDays: Set<Int> = emptySet(),
    /** For [WirdRepeat.ONCE]. */
    val date: LocalDate? = null,
    val timeKind: WirdTimeKind = WirdTimeKind.NONE,
    /** For [WirdTimeKind.CLOCK]: minutes after midnight. */
    val minuteOfDay: Int = 0,
    /** For [WirdTimeKind.PRAYER], with [offsetMinutes] after it, or before it when negative. */
    val prayer: Prayer? = null,
    val offsetMinutes: Int = 0,
    /** A notification at the task's time. Off unless the user turns it on. */
    val remind: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = 0,
) {
    val hasTime: Boolean get() = timeKind == WirdTimeKind.CLOCK || (timeKind == WirdTimeKind.PRAYER && prayer != null)

    /** A reminder needs a time to ring at. */
    val reminds: Boolean get() = remind && hasTime

    val recurring: Boolean get() = repeat != WirdRepeat.ONCE

    companion object {
        const val MAX_TITLE = 80
        const val LAST_HIJRI_DAY = 30

        /** The minutes a task may sit before or after its prayer. */
        val OFFSET_CHOICES = listOf(0, 5, 10, 15, 20, 30, 45, 60, 90, 120)
    }
}

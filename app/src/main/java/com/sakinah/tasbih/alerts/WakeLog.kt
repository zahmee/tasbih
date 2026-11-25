package com.sakinah.tasbih.alerts

import android.content.Context

/**
 * One wake the app asked Android for, and what became of it: the alarm rang at [seenAt], or by
 * [seenAt] the app found that it never had.
 */
data class WakeRecord(val plannedAt: Long, val seenAt: Long, val rang: Boolean) {
    /** How late the alarm rang, in whole minutes. */
    val lateMinutes: Int get() = ((seenAt - plannedAt) / 60_000L).coerceAtLeast(0).toInt()
}

/**
 * Remembers the last wakes, so the reliability screen can show whether the phone delivers
 * Anaa's alarms. Without it a phone that drops them looks the same as one that never rang, and
 * neither the user nor the developer can tell which of the phone's settings is to blame.
 */
object WakeLog {
    const val KEEP = 40

    /** An alarm due this recently may still be on its way. */
    const val GRACE_MS = 2 * 60_000L
    private const val KEY_LOG = "wake_log"

    /**
     * [log] with what this run learnt. The alarm for [rangFor] rang at [now]. Each of [planned],
     * the wakes set by the run before this one, that is past and is not the one ringing was
     * never delivered, since each alarm that rings plans the wakes anew. [judgeMissed] is off
     * after a restart, an update or a change of the clock, which clear or move the alarms.
     */
    fun update(log: List<WakeRecord>, planned: List<Long>, rangFor: Long?, now: Long, judgeMissed: Boolean): List<WakeRecord> {
        val byTime = log.associateByTo(mutableMapOf()) { it.plannedAt }
        if (judgeMissed) {
            planned.filter { it != rangFor && it < now - GRACE_MS && it !in byTime }
                .forEach { byTime[it] = WakeRecord(it, now, rang = false) }
        }
        // Without exact alarms two alarms stand for one wake: the first to ring is the one kept.
        if (rangFor != null && byTime[rangFor]?.rang != true) byTime[rangFor] = WakeRecord(rangFor, now, rang = true)
        return byTime.values.sortedBy { it.plannedAt }.takeLast(KEEP)
    }

    fun encode(log: List<WakeRecord>): String = log.joinToString("\n") { "${it.plannedAt};${it.seenAt};${if (it.rang) 1 else 0}" }

    fun decode(text: String?): List<WakeRecord> = text.orEmpty().lines().mapNotNull { line ->
        val parts = line.split(';')
        val planned = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val seen = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
        WakeRecord(planned, seen, rang = parts.getOrNull(2) == "1")
    }

    /** The last wakes, the newest first. */
    fun read(context: Context): List<WakeRecord> =
        decode(context.getSharedPreferences(PrayerAlarms.STORE, Context.MODE_PRIVATE).getString(KEY_LOG, null)).asReversed()

    internal fun record(context: Context, planned: List<Long>, rangFor: Long?, now: Long, judgeMissed: Boolean) {
        val store = context.getSharedPreferences(PrayerAlarms.STORE, Context.MODE_PRIVATE)
        val before = decode(store.getString(KEY_LOG, null))
        val after = update(before, planned, rangFor, now, judgeMissed)
        if (after != before) store.edit().putString(KEY_LOG, encode(after)).apply()
    }
}

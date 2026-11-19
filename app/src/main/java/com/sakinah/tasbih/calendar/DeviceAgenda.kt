package com.sakinah.tasbih.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** One of the phone's own calendars, as an account's or a shared one. */
data class DeviceCalendar(val id: Long, val name: String, val account: String, val color: Int)

/** One showing of an event; a repeating event has one for every time it falls. */
data class AgendaEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    val location: String,
    /** The colour the phone's calendar app gives it, the event's own or its calendar's. */
    val color: Int,
)

/**
 * Reads the phone's calendars, never writes to them. Nothing read leaves the phone: the events
 * are kept in memory for the months on screen and drawn in the calendar.
 */
object DeviceAgenda {
    const val PERMISSION = Manifest.permission.READ_CALENDAR

    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun calendars(context: Context): List<DeviceCalendar> {
        if (!granted(context)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                "${CalendarContract.Calendars.ACCOUNT_NAME}, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME}",
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            DeviceCalendar(
                                id = cursor.getLong(0),
                                name = cursor.getString(1).orEmpty(),
                                account = cursor.getString(2).orEmpty(),
                                color = cursor.getInt(3),
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** Every showing that touches a day from [from] to [to], both included, by day. */
    fun events(context: Context, from: LocalDate, to: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, List<AgendaEvent>> {
        if (!granted(context)) return emptyMap()
        // A day wider on each side, since all-day events are kept in UTC.
        val begin = from.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = to.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, begin)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        val events = runCatching {
            context.contentResolver.query(
                uri,
                projection,
                "${CalendarContract.Instances.VISIBLE} = 1",
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            AgendaEvent(
                                eventId = cursor.getLong(0),
                                calendarId = cursor.getLong(1),
                                title = cursor.getString(2).orEmpty(),
                                begin = Instant.ofEpochMilli(cursor.getLong(3)),
                                end = Instant.ofEpochMilli(cursor.getLong(4)),
                                allDay = cursor.getInt(5) == 1,
                                location = cursor.getString(6).orEmpty(),
                                color = cursor.getInt(7),
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
        return byDay(events, from, to, zone)
    }

    /**
     * The days each event covers. An all-day event is stored from midnight to midnight in UTC,
     * so its days are read in UTC; a timed one covers each local day it overlaps, and one that
     * ends exactly at midnight does not spill into the next day.
     */
    internal fun byDay(events: List<AgendaEvent>, from: LocalDate, to: LocalDate, zone: ZoneId): Map<LocalDate, List<AgendaEvent>> {
        val days = sortedMapOf<LocalDate, MutableList<AgendaEvent>>()
        events.forEach { event ->
            val dayZone = if (event.allDay) ZoneOffset.UTC else zone
            val first = event.begin.atZone(dayZone).toLocalDate()
            val endExclusive = if (event.end.isAfter(event.begin)) event.end.minusMillis(1) else event.begin
            val last = endExclusive.atZone(dayZone).toLocalDate()
            var day = maxOf(first, from)
            val stop = minOf(last, to)
            while (!day.isAfter(stop)) {
                days.getOrPut(day) { mutableListOf() }.add(event)
                day = day.plusDays(1)
            }
        }
        // All-day events lead their day, then the rest by when they begin.
        return days.mapValues { (_, list) -> list.sortedWith(compareBy({ !it.allDay }, { it.begin })) }
    }
}

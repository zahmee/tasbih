package com.sakinah.tasbih.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sakinah.tasbih.MainActivity
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.AlertKind
import com.sakinah.tasbih.prayer.AlertStyle
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.ui.prayer.PrayerText
import java.time.ZonedDateTime

/**
 * Shows prayer notifications. A channel's sound cannot change once it exists, so every tone has
 * a channel of its own, and choosing another tone only means posting to another channel.
 */
object AlertNotifier {
    const val CHANNEL_SILENT = "prayer_silent"
    const val CHANNEL_REMINDER = "prayer_reminder"
    const val CHANNEL_ADHAN = "prayer_adhan_playing"
    private const val TONE_PREFIX = "prayer_tone_"
    private const val NOTIFICATION_ID = 4100

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** True when the user has silenced one of the channels a prayer alert uses. */
    fun anyChannelMuted(context: Context, toneId: String): Boolean {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        return listOf(CHANNEL_SILENT, CHANNEL_REMINDER, CHANNEL_ADHAN, TONE_PREFIX + toneId)
            .mapNotNull { manager.getNotificationChannel(it) }
            .any { it.importance == NotificationManager.IMPORTANCE_NONE }
    }

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val group = context.getString(R.string.alert_channel_group)
        manager.createNotificationChannelGroup(android.app.NotificationChannelGroup(GROUP, group))
        val channels = mutableListOf(
            NotificationChannel(CHANNEL_SILENT, context.getString(R.string.alert_channel_silent), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
            },
            NotificationChannel(CHANNEL_REMINDER, context.getString(R.string.alert_channel_reminder), NotificationManager.IMPORTANCE_DEFAULT),
            // The adhan itself plays from the service; its notification only carries the stop button.
            NotificationChannel(CHANNEL_ADHAN, context.getString(R.string.alert_channel_adhan), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
            },
        )
        PrayerSounds.tones.forEach { tone ->
            channels += NotificationChannel(
                TONE_PREFIX + tone.id,
                context.getString(R.string.alert_channel_tone, context.getString(tone.label)),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(
                    PrayerSounds.uri(context, tone.raw),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }
        channels.forEach { it.group = GROUP }
        manager.createNotificationChannels(channels)
    }

    /** A reminder before a prayer, or a prayer's own alert that is not the full adhan. */
    fun post(context: Context, prayer: Prayer, kind: AlertKind, style: AlertStyle, prayerTime: ZonedDateTime, toneId: String) {
        if (!canPost(context)) return
        ensureChannels(context)
        val text = PrayerText(context)
        val name = text.name(prayer, prayerTime)
        val channel = when {
            kind == AlertKind.REMINDER -> CHANNEL_REMINDER
            style == AlertStyle.SOUND || style == AlertStyle.ADHAN -> TONE_PREFIX + PrayerSounds.tone(toneId).id
            else -> CHANNEL_SILENT
        }
        val title = when {
            kind == AlertKind.REMINDER -> context.getString(R.string.alert_reminder_title, name, text.time(prayerTime))
            prayer == Prayer.SUNRISE -> context.getString(R.string.alert_sunrise_title)
            else -> context.getString(R.string.alert_prayer_title, name)
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.alert_prayer_text, text.time(prayerTime)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPrayerTimes(context))
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + prayer.ordinal, notification)
    }

    fun openPrayerTimes(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        MainActivity.prayerIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private const val GROUP = "prayer_alerts"
}

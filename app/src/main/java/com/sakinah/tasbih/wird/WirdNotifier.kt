package com.sakinah.tasbih.wird

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sakinah.tasbih.MainActivity
import com.sakinah.tasbih.R
import com.sakinah.tasbih.alerts.AlertNotifier
import java.time.LocalDate

/**
 * The wird's reminders, on a channel of their own, apart from the prayer alerts, so silencing
 * one leaves the other. Each carries «done» and «remind me later», and a tap opens the task, or
 * the adhkar themselves when the task is their reading.
 */
object WirdNotifier {
    const val CHANNEL = "wird_reminder"
    private const val GROUP = "wird"
    private const val BASE_ID = 5000

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannelGroup(NotificationChannelGroup(GROUP, context.getString(R.string.wird_title)))
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.wird_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                group = GROUP
                description = context.getString(R.string.wird_channel_description)
            },
        )
    }

    /** True when the user has silenced the wird's channel in the phone's settings. */
    fun muted(context: Context): Boolean {
        ensureChannel(context)
        return context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE
    }

    fun post(context: Context, task: WirdTask, day: LocalDate) {
        if (!AlertNotifier.canPost(context)) return
        ensureChannel(context)
        val id = notificationId(task.id)
        val text = context.getString(if (task.collectionId != null) R.string.wird_notify_read else R.string.wird_notify_text)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(task.title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open(context, task))
            .addAction(0, context.getString(R.string.wird_action_done), action(context, WirdReceiver.ACTION_DONE, task.id, day, 1))
            .addAction(0, context.getString(R.string.wird_action_later), action(context, WirdReceiver.ACTION_SNOOZE, task.id, day, 2))
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId))
    }

    private fun notificationId(taskId: Long): Int = BASE_ID + (taskId % 100_000).toInt()

    private fun open(context: Context, task: WirdTask): PendingIntent = PendingIntent.getActivity(
        context,
        notificationId(task.id),
        MainActivity.wirdIntent(context, task.collectionId).setData(Uri.parse("anaa://wird/${task.id}")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun action(context: Context, action: String, taskId: Long, day: LocalDate, slot: Int): PendingIntent {
        val intent = Intent(context, WirdReceiver::class.java)
            .setAction(action)
            // Each task and day its own intent, so one notification's buttons never act for another.
            .setData(Uri.parse("anaa://wird/$action/$taskId/$day"))
            .putExtra(WirdReceiver.EXTRA_TASK, taskId)
            .putExtra(WirdReceiver.EXTRA_DAY, day.toString())
        return PendingIntent.getBroadcast(
            context,
            notificationId(taskId) * 8 + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

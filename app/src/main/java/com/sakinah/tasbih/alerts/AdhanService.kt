package com.sakinah.tasbih.alerts

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.AlertKind
import com.sakinah.tasbih.prayer.AlertStyle
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.ui.prayer.PrayerText
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Plays the full adhan. It is started by the prayer's exact alarm, which Android allows to start
 * a foreground service from the background, and it plays on the alarm stream: on Android 17 that
 * pairing is what lets an app play sound while it is not on screen. It stops by itself at the
 * end, when the user taps "stop", or when a call begins.
 *
 * From Android 15, a service an alarm started in the background is refused audio focus, so the
 * focus is asked for but not waited on, and a call is noticed through the phone's audio mode.
 */
class AdhanService : Service() {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var modeListener: AudioManager.OnModeChangedListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var prayer = Prayer.DHUHR
    private var time: ZonedDateTime = ZonedDateTime.now()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAll(leaveNotice = false)
            return START_NOT_STICKY
        }
        prayer = Prayer.entries.getOrNull(intent?.getIntExtra(EXTRA_PRAYER, -1) ?: -1) ?: Prayer.DHUHR
        time = Instant.ofEpochMilli(intent?.getLongExtra(EXTRA_TIME, System.currentTimeMillis()) ?: System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
        val source = intent?.getStringExtra(EXTRA_SOURCE)?.let(Uri::parse)
        AlertNotifier.ensureChannels(this)
        val text = PrayerText(this)
        val notification = NotificationCompat.Builder(this, AlertNotifier.CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(getString(R.string.alert_prayer_title, text.name(prayer, time)))
            .setContentText(getString(R.string.alert_adhan_playing))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(AlertNotifier.openPrayerTimes(this))
            .addAction(0, getString(R.string.alert_stop), stopIntent(this))
            .setDeleteIntent(stopIntent(this))
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        // A last guard, so a broken file can never keep the service alive.
        handler.postDelayed({ stopAll(leaveNotice = true) }, MAX_PLAY_MS)
        play(source ?: PrayerSounds.uri(this, PrayerSounds.adhans.first().raw))
        return START_NOT_STICKY
    }

    private fun play(uri: Uri) {
        stopPlayer()
        val audio = getSystemService(AudioManager::class.java)
        // In a call, the adhan would sound in the caller's ear: the prayer's notice is enough.
        if (inCall(audio.mode)) {
            stopAll(leaveNotice = true)
            return
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                // Another alarm or a call: the adhan gives way and does not come back.
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stopAll(leaveNotice = true)
                }
            }
            .build()
        if (audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) focus = request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val listener = AudioManager.OnModeChangedListener { mode -> if (inCall(mode)) stopAll(leaveNotice = true) }
            audio.addOnModeChangedListener(ContextCompat.getMainExecutor(this), listener)
            modeListener = listener
        }
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(applicationContext, uri)
                setOnCompletionListener { stopAll(leaveNotice = true) }
                setOnErrorListener { _, _, _ ->
                    stopAll(leaveNotice = true)
                    true
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            // A file that was moved or deleted on the phone: the user still gets the notice.
            stopAll(leaveNotice = true)
            null
        }
    }

    private fun stopPlayer() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
    }

    private fun releaseAudio() {
        val audio = getSystemService(AudioManager::class.java)
        focus?.let(audio::abandonAudioFocusRequest)
        focus = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) modeListener?.let(audio::removeOnModeChangedListener)
        modeListener = null
    }

    /**
     * Ends the adhan. Unless the user stopped it, a plain notice stays behind, so the prayer's
     * time is still there to see once the sound is over or a call cut it short.
     */
    private fun stopAll(leaveNotice: Boolean) {
        handler.removeCallbacksAndMessages(null)
        stopPlayer()
        releaseAudio()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (leaveNotice) AlertNotifier.post(this, prayer, AlertKind.PRAYER, AlertStyle.SILENT, time, "")
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopPlayer()
        releaseAudio()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.sakinah.tasbih.action.STOP_ADHAN"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_SOURCE = "source"
        private const val NOTIFICATION_ID = 4200
        private const val MAX_PLAY_MS = 10 * 60 * 1000L

        private fun inCall(mode: Int): Boolean =
            mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE

        fun start(context: Context, prayer: Prayer, timeMillis: Long, source: Uri) {
            val intent = Intent(context, AdhanService::class.java)
                .putExtra(EXTRA_PRAYER, prayer.ordinal)
                .putExtra(EXTRA_TIME, timeMillis)
                .putExtra(EXTRA_SOURCE, source.toString())
            ContextCompat.startForegroundService(context, intent)
        }

        private fun stopIntent(context: Context): PendingIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

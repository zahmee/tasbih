package com.sakinah.tasbih

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sakinah.tasbih.ui.AppOpenRequest
import com.sakinah.tasbih.ui.SakinahApp
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    /** Set by a tap on a calendar widget, and cleared once the app has moved there. */
    private var openRequest by mutableStateOf<AppOpenRequest?>(null)

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after a rotation the screen already shows what was asked for.
        if (savedInstanceState == null) openRequest = requestFrom(intent)
        setContent {
            SakinahApp(openRequest = openRequest, onOpenRequestHandled = { openRequest = null })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestFrom(intent)?.let { openRequest = it }
    }

    private fun requestFrom(intent: Intent?): AppOpenRequest? {
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_PRAYER) return AppOpenRequest.Prayer
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_QIBLA) return AppOpenRequest.Qibla
        if (intent?.getStringExtra(EXTRA_OPEN) != OPEN_CALENDAR) return null
        val day = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        val date = if (day == Long.MIN_VALUE) LocalDate.now() else LocalDate.ofEpochDay(day)
        return AppOpenRequest.Calendar(date, showOccasions = intent.getBooleanExtra(EXTRA_OCCASIONS, false))
    }

    companion object {
        private const val EXTRA_OPEN = "open"
        private const val OPEN_CALENDAR = "calendar"
        private const val OPEN_PRAYER = "prayer"
        private const val OPEN_QIBLA = "qibla"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_OCCASIONS = "occasions"

        /** Opens the prayer times, from a widget or a prayer notification. */
        fun prayerIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_PRAYER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** Opens the Qibla compass, from its widget. */
        fun qiblaIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_QIBLA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** Opens the calendar on the month that holds [date], or on the coming occasions. */
        fun calendarIntent(context: Context, date: LocalDate, showOccasions: Boolean = false): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_CALENDAR)
                .putExtra(EXTRA_DATE, date.toEpochDay())
                .putExtra(EXTRA_OCCASIONS, showOccasions)
                // Reaches the open app as a new request instead of starting a second copy of it.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

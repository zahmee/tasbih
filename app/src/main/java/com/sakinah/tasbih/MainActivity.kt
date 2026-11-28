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
        externalRequest(intent?.action, intent?.getStringExtra(EXTRA_DESTINATION), LocalDate.now())?.let { return it }
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_PRAYER) return AppOpenRequest.Prayer
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_QIBLA) return AppOpenRequest.Qibla
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_CALENDAR_SETTINGS) return AppOpenRequest.CalendarSettings
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_WIRD) return AppOpenRequest.Wird(intent.getStringExtra(EXTRA_COLLECTION))
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_WIDGET_STYLE) {
            val id = intent.getIntExtra(EXTRA_WIDGET_ID, 0).takeIf { it != 0 }
            return AppOpenRequest.WidgetStyle(next = intent.getBooleanExtra(EXTRA_NEXT, false), appWidgetId = id)
        }
        if (intent?.getStringExtra(EXTRA_OPEN) != OPEN_CALENDAR) return null
        val day = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        val date = if (day == Long.MIN_VALUE) LocalDate.now() else LocalDate.ofEpochDay(day)
        return AppOpenRequest.Calendar(date, showOccasions = intent.getBooleanExtra(EXTRA_OCCASIONS, false))
    }

    companion object {
        // A contract with other apps (Natlu, com.mushaf.reader) that send it. Once published, these
        // names and values never change and are never removed; new destinations may be added.
        // Kept apart from EXTRA_OPEN below, which only the app's own widgets and notifications use.
        const val ACTION_OPEN = "com.sakinah.tasbih.action.OPEN"
        const val EXTRA_DESTINATION = "com.sakinah.tasbih.extra.DESTINATION"
        const val DESTINATION_TODAY = "today"
        const val DESTINATION_PRAYER = "prayer"
        const val DESTINATION_CALENDAR = "calendar"
        const val DESTINATION_ADHKAR = "adhkar"
        const val DESTINATION_TASBIH = "tasbih"
        const val DESTINATION_QIBLA = "qibla"

        /**
         * The screen another app asked for with [ACTION_OPEN], or null for any other action.
         * A missing or unknown destination opens the day.
         */
        fun externalRequest(action: String?, destination: String?, today: LocalDate): AppOpenRequest? {
            if (action != ACTION_OPEN) return null
            return when (destination) {
                DESTINATION_PRAYER -> AppOpenRequest.Prayer
                DESTINATION_QIBLA -> AppOpenRequest.Qibla
                DESTINATION_CALENDAR -> AppOpenRequest.Calendar(today)
                DESTINATION_ADHKAR -> AppOpenRequest.Adhkar
                DESTINATION_TASBIH -> AppOpenRequest.Tasbih
                else -> AppOpenRequest.Today
            }
        }

        private const val EXTRA_OPEN = "open"
        private const val OPEN_CALENDAR = "calendar"
        private const val OPEN_PRAYER = "prayer"
        private const val OPEN_QIBLA = "qibla"
        private const val OPEN_WIDGET_STYLE = "widget_style"
        private const val OPEN_CALENDAR_SETTINGS = "calendar_settings"
        private const val OPEN_WIRD = "wird"
        private const val EXTRA_NEXT = "next"
        private const val EXTRA_WIDGET_ID = "widget_id"
        private const val EXTRA_COLLECTION = "collection"
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

        /**
         * Opens the prayer widgets' customisation on the look of the widget [appWidgetId], from its
         * own «customise»; without an id, on the default look, the next prayer's widget when [next].
         */
        fun widgetStyleIntent(context: Context, next: Boolean, appWidgetId: Int? = null): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_WIDGET_STYLE)
                .putExtra(EXTRA_NEXT, next)
                .apply { if (appWidgetId != null) putExtra(EXTRA_WIDGET_ID, appWidgetId) }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /**
         * Opens the wird, from its widget or a reminder, and on [collectionId]'s adhkar when the
         * task is their reading, so a tap goes straight to reading them.
         */
        fun wirdIntent(context: Context, collectionId: String? = null): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_WIRD)
                .apply { if (collectionId != null) putExtra(EXTRA_COLLECTION, collectionId) }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** Opens the calendar's settings, from a calendar widget's own «customise». */
        fun calendarSettingsIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_CALENDAR_SETTINGS)
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

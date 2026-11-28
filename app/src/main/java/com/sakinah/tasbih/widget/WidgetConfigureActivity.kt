package com.sakinah.tasbih.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.sakinah.tasbih.MainActivity

/**
 * What Android opens for a widget's «customise»: from Android 12 a long press on the widget
 * offers it. It says yes at once and hands over to the app: a prayer widget to the widgets'
 * customisation, on the widget it came from, a calendar widget to the calendar's settings. A
 * screen of its own would repeat the app's theme and data.
 *
 * Placing a widget no longer opens it from Android 12, where the widgets say their customising
 * is optional. Until November 25, 2026 it did, and the launcher waited for this screen's
 * answer behind the app: leaving the app with the home button then cancelled the answer, and
 * the launcher took the new widget off the home screen again, as seen on Android 15's own
 * launcher. Before Android 12 placing still opens it, since Android has no other way there.
 */
class WidgetConfigureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className
        val calendar = CalendarWidgetKind.entries.any { it.receiver.name == provider }
        startActivity(
            if (calendar) {
                MainActivity.calendarSettingsIntent(this)
            } else {
                MainActivity.widgetStyleIntent(
                    this,
                    next = provider == PrayerNextWidget::class.java.name,
                    appWidgetId = id.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID },
                )
            },
        )
        finish()
    }
}

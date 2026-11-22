package com.sakinah.tasbih.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.sakinah.tasbih.MainActivity

/**
 * What Android opens for a prayer widget's «customise»: from Android 12 a long press on the
 * widget offers it, and placing one from the launcher's list opens it too. It says yes at once,
 * so the widget is placed whatever happens next, and hands over to the app's own customisation,
 * on the widget it came from. A screen of its own would repeat the app's theme and data.
 */
class WidgetConfigureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className
        startActivity(MainActivity.widgetStyleIntent(this, next = provider == PrayerNextWidget::class.java.name))
        finish()
    }
}

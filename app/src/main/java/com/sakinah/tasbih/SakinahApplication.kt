package com.sakinah.tasbih

import android.app.Application
import com.sakinah.tasbih.data.DigitStyle

/** Reads, before anything is drawn, what every screen, widget and notification writes with. */
class SakinahApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DigitStyle.load(this)
    }
}

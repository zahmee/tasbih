package com.sakinah.tasbih.alerts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** What stands between a prayer's alarm and the user hearing it, read fresh each time. */
data class AlertHealth(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val batteryUnrestricted: Boolean,
    val channelsOn: Boolean,
) {
    val allGood: Boolean get() = notifications && exactAlarms && batteryUnrestricted && channelsOn

    companion object {
        fun read(context: Context, toneId: String): AlertHealth = AlertHealth(
            notifications = AlertNotifier.canPost(context),
            exactAlarms = PrayerAlarms.canScheduleExact(context),
            batteryUnrestricted = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
            channelsOn = !AlertNotifier.anyChannelMuted(context, toneId),
        )

        /** The system screen where this app may set exact alarms; Android 12 and later only. */
        fun exactAlarmSettings(context: Context): Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            } else {
                null
            }

        fun notificationSettings(context: Context): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

        /**
         * The list of battery optimisations, where the user can exempt the app. The direct request
         * is not used: Google Play allows it only for a few kinds of app, and prayer apps are not one.
         */
        fun batterySettings(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}

/** Phone makers whose battery managers are known to stop alarms, and how to tell. */
enum class PhoneMaker { HUAWEI, SAMSUNG, XIAOMI, OPPO, OTHER;

    companion object {
        fun current(): PhoneMaker {
            val maker = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
            return when {
                "huawei" in maker || "honor" in maker -> HUAWEI
                "samsung" in maker -> SAMSUNG
                "xiaomi" in maker || "redmi" in maker || "poco" in maker -> XIAOMI
                "oppo" in maker || "realme" in maker || "oneplus" in maker || "vivo" in maker -> OPPO
                else -> OTHER
            }
        }
    }
}

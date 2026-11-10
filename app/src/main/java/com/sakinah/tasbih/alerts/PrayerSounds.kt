package com.sakinah.tasbih.alerts

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.AlertSettings

/** A sound the app ships. [credit] names the source a licence asks to be named, if any. */
data class PrayerSound(val id: String, @RawRes val raw: Int, @StringRes val label: Int, val credit: String? = null)

/**
 * Every sound in one place, so a recording is added or removed here and nowhere else. The
 * recordings come from Wikimedia Commons under the licences below; THIRD_PARTY_NOTICES.md lists
 * them in full with what was changed. Each was trimmed to the adhan and levelled to one loudness.
 */
object PrayerSounds {
    /** Short sounds a notification channel plays in full, under half a minute each. */
    val tones = listOf(
        PrayerSound(
            "takbir_makkah",
            R.raw.prayer_tone_takbir_makkah,
            R.string.alert_tone_takbir_makkah,
            "Seyfula Islam – CC BY 3.0 – مقتطع من أذان المسجد الحرام",
        ),
        // Original tones made by tools/generate_prayer_tones.py, free of any third-party rights.
        PrayerSound("chime", R.raw.prayer_tone_chime, R.string.alert_tone_chime),
        PrayerSound("bell", R.raw.prayer_tone_bell, R.string.alert_tone_bell),
        PrayerSound("soft", R.raw.prayer_tone_soft, R.string.alert_tone_soft),
    )

    /** Whole adhans, and a tone for those who want no voice at all. */
    val adhans: List<PrayerSound> = listOf(
        PrayerSound(
            "makkah_maghrib",
            R.raw.adhan_makkah_maghrib,
            R.string.alert_adhan_makkah_maghrib,
            "3omar Faruq – CC BY 3.0 – مقتطع ومضبوط الصوت",
        ),
        PrayerSound("recording_1", R.raw.adhan_recording_1, R.string.alert_adhan_recording_1, "Aishatu98 – ملك عام (CC0)"),
        PrayerSound("recording_2", R.raw.adhan_recording_2, R.string.alert_adhan_recording_2, "Adam-synagda – ملك عام (CC0)"),
        PrayerSound("recording_3", R.raw.adhan_recording_3, R.string.alert_adhan_recording_3, "Andrewler – CC BY-SA 4.0 – مضبوط الصوت"),
        PrayerSound("recording_4", R.raw.adhan_recording_4, R.string.alert_adhan_recording_4, "Isaacayodele32 – CC BY-SA 4.0 – مضبوط الصوت"),
        PrayerSound(AlertSettings.TONE_ADHAN, R.raw.prayer_tone_chime, R.string.alert_adhan_tone),
    )

    fun tone(id: String): PrayerSound = tones.firstOrNull { it.id == id } ?: tones.first()

    fun adhan(id: String): PrayerSound = adhans.firstOrNull { it.id == id } ?: adhans.first()

    fun uri(context: Context, @RawRes raw: Int): Uri = Uri.parse("android.resource://${context.packageName}/$raw")
}

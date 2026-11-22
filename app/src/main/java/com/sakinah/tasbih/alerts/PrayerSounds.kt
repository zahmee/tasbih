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
 * recordings come from Wikimedia Commons and the tones from Android's own source, under the
 * licences below; THIRD_PARTY_NOTICES.md lists them in full with what was changed. Each adhan
 * was trimmed to the adhan and levelled to one loudness.
 */
object PrayerSounds {
    private const val ANDROID_CREDIT = "Android Open Source Project – Apache 2.0"

    /** Short sounds a notification channel plays in full, under half a minute each. */
    val tones = listOf(
        PrayerSound(
            "takbir_makkah",
            R.raw.prayer_tone_takbir_makkah,
            R.string.alert_tone_takbir_makkah,
            "Seyfula Islam – CC BY 3.0 – مقتطع من أذان المسجد الحرام",
        ),
        // Android's own Material notification tones, Apache 2.0, made mono and a touch quieter.
        PrayerSound("ceres", R.raw.prayer_tone_ceres, R.string.alert_tone_ceres, ANDROID_CREDIT),
        PrayerSound("tethys", R.raw.prayer_tone_tethys, R.string.alert_tone_tethys, ANDROID_CREDIT),
        PrayerSound("rhea", R.raw.prayer_tone_rhea, R.string.alert_tone_rhea, ANDROID_CREDIT),
    )

    /**
     * The tones that came before, chosen by the user on November 23, 2026 to give way to these.
     * A saved choice of one of them carries over to the tone that took its place.
     */
    private val RETIRED = mapOf("chime" to "ceres", "bell" to "tethys", "soft" to "rhea")

    /** A saved tone as it stands today: a retired one becomes the tone that replaced it. */
    fun currentToneId(id: String): String = RETIRED[id] ?: id

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
        PrayerSound(AlertSettings.TONE_ADHAN, R.raw.prayer_tone_ceres, R.string.alert_adhan_tone, ANDROID_CREDIT),
    )

    fun tone(id: String): PrayerSound = currentToneId(id).let { current -> tones.firstOrNull { it.id == current } ?: tones.first() }

    fun adhan(id: String): PrayerSound = adhans.firstOrNull { it.id == id } ?: adhans.first()

    /**
     * By the file's name, not its number: a notification channel keeps the address it was made
     * with for good, and the numbers shift whenever a sound is added to or removed from the app.
     */
    fun uri(context: Context, @RawRes raw: Int): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/${context.resources.getResourceEntryName(raw)}")
}

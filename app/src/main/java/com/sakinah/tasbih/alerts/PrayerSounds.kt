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
 * takbir and the second adhan come from Wikimedia Commons, the tones from Pixabay, and the other
 * adhans from files the user gave; THIRD_PARTY_NOTICES.md lists them with what was changed.
 * Each was trimmed of its silences and levelled to one loudness.
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
        // Added by the user on November 23, 2026, from Pixabay; made mono and levelled.
        PrayerSound("clear", R.raw.prayer_tone_clear, R.string.alert_tone_clear, "koiroylers – Pixabay"),
        PrayerSound("light", R.raw.prayer_tone_light, R.string.alert_tone_light, "Universfield – Pixabay"),
        PrayerSound("simple", R.raw.prayer_tone_simple, R.string.alert_tone_simple, "Universfield – Pixabay"),
    )

    /**
     * Tones the app shipped before, the first three until November 23, 2026 and Android's own
     * after them until the user chose on the same day which sounds stay. A saved choice of one
     * of them carries over to a tone that stays.
     */
    private val RETIRED_TONES = mapOf(
        "chime" to "clear", "bell" to "light", "soft" to "simple",
        "ceres" to "clear", "tethys" to "light", "rhea" to "simple",
    )

    /** A saved tone as it stands today: a retired one becomes the tone that replaced it. */
    fun currentToneId(id: String): String = RETIRED_TONES[id] ?: id

    /**
     * Whole adhans. The first is the default. The muezzin is in the name and the place below it;
     * where the four the user added came from, and under what licence, is still to be confirmed.
     */
    val adhans: List<PrayerSound> = listOf(
        PrayerSound(AlertSettings.DEFAULT_ADHAN, R.raw.adhan_ali_mulla, R.string.alert_adhan_ali_mulla, "الحرم المكي، 2006"),
        PrayerSound("ahmed_alamadi", R.raw.adhan_ahmed_alamadi, R.string.alert_adhan_ahmed_alamadi, "قطر"),
        PrayerSound("nasser_alqatami", R.raw.adhan_nasser_alqatami, R.string.alert_adhan_nasser_alqatami, "السعودية"),
        PrayerSound("hamad_aldaghriri", R.raw.adhan_hamad_aldaghriri, R.string.alert_adhan_hamad_aldaghriri),
        PrayerSound("recording_2", R.raw.adhan_recording_2, R.string.alert_adhan_recording_2, "Adam-synagda – ملك عام (CC0)"),
    )

    /**
     * Adhans the user took out on November 23, 2026, the tone among them. A saved choice of one
     * of them carries over to the default, so the phone still hears an adhan at the prayer.
     */
    private val RETIRED_ADHANS = setOf("makkah_maghrib", "recording_1", "recording_3", "recording_4", "tone")

    /** A saved adhan as it stands today: a retired one becomes the default. */
    fun currentAdhanId(id: String): String = if (id in RETIRED_ADHANS) AlertSettings.DEFAULT_ADHAN else id

    fun tone(id: String): PrayerSound = currentToneId(id).let { current -> tones.firstOrNull { it.id == current } ?: tones.first() }

    fun adhan(id: String): PrayerSound = currentAdhanId(id).let { current -> adhans.firstOrNull { it.id == current } ?: adhans.first() }

    /**
     * By the file's name, not its number: a notification channel keeps the address it was made
     * with for good, and the numbers shift whenever a sound is added to or removed from the app.
     */
    fun uri(context: Context, @RawRes raw: Int): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/${context.resources.getResourceEntryName(raw)}")
}

package com.sakinah.tasbih.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sakinah.tasbih.calendar.SavedMaps
import com.sakinah.tasbih.prayer.AlertSettings
import com.sakinah.tasbih.prayer.AlertStyle
import com.sakinah.tasbih.prayer.PrayerAlert
import com.sakinah.tasbih.prayer.AsrChoice
import com.sakinah.tasbih.prayer.HighLatitudeChoice
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerMethod
import com.sakinah.tasbih.prayer.PrayerPlace
import com.sakinah.tasbih.prayer.PrayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Its own file: the place stays on the phone, apart from the remembrance and tasbih progress.
private val Context.prayerDataStore by preferencesDataStore(name = "prayer_preferences")

class PrayerPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.prayerDataStore)

    val settings: Flow<PrayerSettings> = dataStore.data.map(::read)

    val alerts: Flow<AlertSettings> = dataStore.data.map(::readAlerts)

    suspend fun currentAlerts(): AlertSettings = alerts.first()

    /** Saved as "FAJR=ADHAN:10" lines: the style, then the reminder's minutes. */
    suspend fun setAlert(prayer: Prayer, alert: PrayerAlert) = dataStore.edit { preferences ->
        val lines = preferences[Keys.alerts].orEmpty().filterNot { it.startsWith("${prayer.name}=") }
        preferences[Keys.alerts] = (lines + "${prayer.name}=${alert.style.name}:${alert.reminderMinutes}").toSet()
    }

    suspend fun setTone(tone: String) = dataStore.edit { it[Keys.tone] = tone }

    suspend fun setAdhan(adhan: String, fajr: Boolean) = dataStore.edit { it[if (fajr) Keys.fajrAdhan else Keys.adhan] = adhan }

    /** A file from the phone, kept by its address; the app holds a lasting permission to read it. */
    suspend fun setCustomAdhan(uri: String, fajr: Boolean) = dataStore.edit {
        it[Keys.customAdhanUri] = uri
        it[if (fajr) Keys.fajrAdhan else Keys.adhan] = AlertSettings.CUSTOM_ADHAN
    }

    suspend fun current(): PrayerSettings = settings.first()

    suspend fun setPlace(place: PrayerPlace) = dataStore.edit {
        it[Keys.name] = place.name
        it[Keys.region] = place.region
        it[Keys.country] = place.country
        it[Keys.countryCode] = place.countryCode
        it[Keys.latitude] = place.latitude
        it[Keys.longitude] = place.longitude
        it[Keys.timeZone] = place.timeZone
        it[Keys.fromDevice] = place.fromDevice
    }

    /** Null goes back to the method of the place's country. */
    suspend fun setMethod(method: PrayerMethod?) = dataStore.edit {
        if (method == null) it.remove(Keys.method) else it[Keys.method] = method.name
    }

    suspend fun setAsr(choice: AsrChoice) = dataStore.edit { it[Keys.asr] = choice.name }

    suspend fun setHighLatitude(choice: HighLatitudeChoice) = dataStore.edit { it[Keys.highLatitude] = choice.name }

    suspend fun setAdjustment(prayer: Prayer, minutes: Int) = dataStore.edit { preferences ->
        val current = SavedMaps.decodeStrings(preferences[Keys.adjustments])
        val value = minutes.coerceIn(-PrayerSettings.MAX_ADJUSTMENT, PrayerSettings.MAX_ADJUSTMENT)
        val updated = if (value == 0) current - prayer.name else current + (prayer.name to value)
        preferences[Keys.adjustments] = SavedMaps.encode(updated)
    }

    private fun read(preferences: Preferences): PrayerSettings {
        val latitude = preferences[Keys.latitude]
        val longitude = preferences[Keys.longitude]
        val place = if (latitude != null && longitude != null) {
            PrayerPlace(
                name = preferences[Keys.name].orEmpty(),
                region = preferences[Keys.region].orEmpty(),
                country = preferences[Keys.country].orEmpty(),
                countryCode = preferences[Keys.countryCode].orEmpty(),
                latitude = latitude,
                longitude = longitude,
                timeZone = preferences[Keys.timeZone].orEmpty(),
                fromDevice = preferences[Keys.fromDevice] ?: false,
            )
        } else {
            null
        }
        return PrayerSettings(
            place = place,
            method = PrayerMethod.entries.firstOrNull { it.name == preferences[Keys.method] },
            asr = AsrChoice.entries.firstOrNull { it.name == preferences[Keys.asr] } ?: AsrChoice.AUTO,
            highLatitude = HighLatitudeChoice.entries.firstOrNull { it.name == preferences[Keys.highLatitude] }
                ?: HighLatitudeChoice.AUTO,
            adjustments = SavedMaps.decodeStrings(preferences[Keys.adjustments]).mapNotNull { (name, minutes) ->
                Prayer.entries.firstOrNull { it.name == name }?.let { it to minutes }
            }.toMap(),
        )
    }

    private fun readAlerts(preferences: Preferences) = AlertSettings(
        alerts = preferences[Keys.alerts].orEmpty().mapNotNull { line ->
            val prayer = Prayer.entries.firstOrNull { it.name == line.substringBefore('=') } ?: return@mapNotNull null
            val value = line.substringAfter('=')
            val style = AlertStyle.entries.firstOrNull { it.name == value.substringBefore(':') } ?: return@mapNotNull null
            val minutes = value.substringAfter(':', "0").toIntOrNull()?.takeIf { it in AlertSettings.REMINDER_CHOICES } ?: 0
            prayer to PrayerAlert(style, minutes)
        }.toMap(),
        tone = preferences[Keys.tone] ?: AlertSettings.DEFAULT_TONE,
        adhan = preferences[Keys.adhan] ?: AlertSettings.DEFAULT_ADHAN,
        fajrAdhan = preferences[Keys.fajrAdhan] ?: AlertSettings.DEFAULT_ADHAN,
        customAdhanUri = preferences[Keys.customAdhanUri],
    )

    private object Keys {
        val name = stringPreferencesKey("place_name")
        val region = stringPreferencesKey("place_region")
        val country = stringPreferencesKey("place_country")
        val countryCode = stringPreferencesKey("place_country_code")
        val latitude = doublePreferencesKey("place_latitude")
        val longitude = doublePreferencesKey("place_longitude")
        val timeZone = stringPreferencesKey("place_time_zone")
        val fromDevice = booleanPreferencesKey("place_from_device")
        val method = stringPreferencesKey("method")
        val asr = stringPreferencesKey("asr")
        val highLatitude = stringPreferencesKey("high_latitude")
        val adjustments = stringSetPreferencesKey("adjustments")
        val alerts = stringSetPreferencesKey("alerts")
        val tone = stringPreferencesKey("alert_tone")
        val adhan = stringPreferencesKey("alert_adhan")
        val fajrAdhan = stringPreferencesKey("alert_fajr_adhan")
        val customAdhanUri = stringPreferencesKey("alert_custom_adhan_uri")
    }
}

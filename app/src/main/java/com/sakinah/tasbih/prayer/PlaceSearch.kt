package com.sakinah.tasbih.prayer

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Finds a city or village by name with the Open-Meteo geocoding service, whose data comes from
 * GeoNames. This is the app's only use of the internet: the typed words go out, and the place's
 * name, coordinates and time zone come back. Nothing else is sent, and the times are then worked
 * out on the phone.
 */
class PlaceSearch(private val endpoint: String = ENDPOINT) {

    /** Throws [IOException] when the service cannot be reached or answers with an error. */
    suspend fun search(query: String): List<PrayerPlace> = withContext(Dispatchers.IO) {
        val text = query.trim()
        if (text.length < MIN_QUERY_LENGTH) return@withContext emptyList()
        val url = URL(
            "$endpoint?name=${URLEncoder.encode(text, "UTF-8")}&count=$MAX_RESULTS&language=ar&format=json",
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Place search answered ${connection.responseCode}")
            }
            parse(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search"
        private const val MAX_RESULTS = 20
        private const val TIMEOUT_MS = 15_000
        const val MIN_QUERY_LENGTH = 2

        /** Direction marks the service leaves in some names, which would only confuse a search. */
        private val DIRECTION_MARKS = Regex("[‎‏‪-‮⁦-⁩]")

        /**
         * Places without a time zone are dropped, as their times could not be shown correctly, and
         * so are airports, bases and other features where nobody lives: GeoNames marks the places
         * people live in, from capitals to villages, with codes that begin with "PPL".
         */
        fun parse(json: String): List<PrayerPlace> {
            val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
            return (0 until results.length()).mapNotNull { index ->
                val item = results.optJSONObject(index) ?: return@mapNotNull null
                val feature = item.optString("feature_code")
                if (feature.isNotEmpty() && !feature.startsWith("PPL")) return@mapNotNull null
                val zone = item.optString("timezone").clean()
                if (zone.isEmpty() || !item.has("latitude") || !item.has("longitude")) return@mapNotNull null
                PrayerPlace(
                    name = item.optString("name").clean(),
                    region = item.optString("admin1").clean(),
                    country = item.optString("country").clean(),
                    countryCode = item.optString("country_code").clean().uppercase(),
                    latitude = item.getDouble("latitude"),
                    longitude = item.getDouble("longitude"),
                    timeZone = zone,
                )
            }.distinctBy { listOf(it.name, it.region, it.countryCode) }
        }

        private fun String.clean(): String = DIRECTION_MARKS.replace(this, "").trim()
    }
}

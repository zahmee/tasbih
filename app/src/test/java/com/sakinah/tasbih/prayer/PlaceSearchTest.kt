package com.sakinah.tasbih.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSearchTest {
    /** Shaped like the service's answer to «الرياض» on 2026-11-11, trimmed to what is read. */
    private val answer = """
        {"results":[
          {"id":108410,"name":"الرياض","latitude":24.68773,"longitude":46.72185,"country_code":"SA",
           "timezone":"Asia/Riyadh","population":4205961,"country":"المملكة العربية السعودية","admin1":"منطقة الرياض‎"},
          {"id":1,"name":"الرياض","latitude":35.26964,"longitude":43.91067,"country_code":"IQ",
           "timezone":"Asia/Baghdad","country":"العراق","admin1":"محافظة كركوك"},
          {"id":2,"name":"بلا منطقة زمنية","latitude":1.0,"longitude":2.0,"country_code":"XX"},
          {"id":4,"name":"قاعدة الرياض الجوية","latitude":24.7,"longitude":46.7,"country_code":"SA",
           "timezone":"Asia/Riyadh","feature_code":"AIRB","country":"المملكة العربية السعودية"},
          {"id":3,"name":"الرياض","latitude":24.6878,"longitude":46.7219,"country_code":"SA",
           "timezone":"Asia/Riyadh","country":"المملكة العربية السعودية","admin1":"منطقة الرياض"}
        ],"generationtime_ms":0.4}
    """.trimIndent()

    @Test
    fun `places are read with their zone, and marks and repeats are dropped`() {
        val places = PlaceSearch.parse(answer)
        assertEquals(2, places.size)
        val riyadh = places.first()
        assertEquals("الرياض", riyadh.name)
        assertEquals("منطقة الرياض", riyadh.region)
        assertEquals("SA", riyadh.countryCode)
        assertEquals("Asia/Riyadh", riyadh.timeZone)
        assertEquals(24.68773, riyadh.latitude, 1e-6)
        assertEquals("IQ", places[1].countryCode)
    }

    @Test
    fun `an answer with no results is an empty list`() {
        assertTrue(PlaceSearch.parse("""{"generationtime_ms":0.2}""").isEmpty())
    }
}

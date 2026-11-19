package com.sakinah.tasbih.prayer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyPlacesTest {
    private fun near(latitude: Double, longitude: Double, lines: List<String>) = NearbyPlaces.pick(lines.asSequence(), latitude, longitude)

    private val sample = listOf(
        "17.3700\t42.5400\tبيش\tجازان\tSA\tt",
        "17.3740\t42.5370\tحي النهضة\tجازان\tSA\tx",
        "17.4300\t42.6000\tقرية بعيدة\t\tSA\t",
        "17.3900\t42.5500\tمزرعة\t\tSA\t",
    )

    @Test
    fun `a district is joined to its town`() {
        val name = near(17.3742, 42.5361, sample)!!
        assertEquals("حي النهضة، بيش", name.name)
        assertEquals("جازان", name.region)
        assertEquals("السعودية", name.country)
        assertEquals("SA", name.countryCode)
    }

    @Test
    fun `a town close by is preferred to a nearer hamlet, and a far one is not`() {
        assertEquals("بيش", near(17.3880, 42.5490, sample)!!.name)
        val village = near(17.4310, 42.6010, sample)!!
        assertEquals("قرية بعيدة", village.name)
        // A place without a region of its own takes its neighbours'.
        assertEquals("جازان", village.region)
    }

    @Test
    fun `nothing in reach gives no name, and a broken line is passed over`() {
        assertNull(near(37.4220, -122.0840, sample))
        assertNull(near(17.9, 42.5, sample))
        assertEquals("بيش", near(17.3700, 42.5400, listOf("", "x\ty", "17.37\t42.54", sample[0]))!!.name)
    }

    @Test
    fun `the bundled list names the towns of Jazan`() {
        val lines = File("src/main/assets/places_ar.tsv").readLines()
        assertTrue(lines.size > 100_000)
        assertTrue(near(17.3742, 42.5361, lines)!!.name.endsWith("بيش"))
        assertTrue(near(17.1100, 42.7770, lines)!!.name.endsWith("ضمد"))
        assertEquals("السعودية", near(16.8892, 42.5511, lines)!!.country)
    }
}

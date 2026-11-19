package com.sakinah.tasbih.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceNamerTest {
    @Test
    fun `a district and its town make the name, with the region beside it`() {
        val name = PlaceNamer.compose(
            listOf(
                // The nearest address often lacks the district, which a later one carries.
                AddressParts(town = "بيش", region = "منطقة جازان", country = "السعودية", countryCode = "sa"),
                AddressParts(district = "حي الروضة", town = "بيش", region = "منطقة جازان", country = "السعودية", countryCode = "SA"),
            ),
        )!!
        assertEquals("حي الروضة، بيش", name.name)
        assertEquals("منطقة جازان", name.region)
        assertEquals("السعودية", name.country)
        assertEquals("SA", name.countryCode)
    }

    @Test
    fun `a town alone is the name, and a village falls back to its county`() {
        assertEquals("بيش", PlaceNamer.compose(listOf(AddressParts(town = "بيش", region = "منطقة جازان")))!!.name)
        val village = PlaceNamer.compose(listOf(AddressParts(county = "محافظة ضمد", region = "منطقة جازان")))!!
        assertEquals("محافظة ضمد", village.name)
        assertEquals("منطقة جازان", village.region)
    }

    @Test
    fun `a district named like its town is not repeated, nor a region named like the place`() {
        val name = PlaceNamer.compose(listOf(AddressParts(district = "جازان", town = "جازان", region = "جازان")))!!
        assertEquals("جازان", name.name)
        assertEquals("", name.region)
    }

    @Test
    fun `the region stands in where nothing nearer is known, and nothing gives no name`() {
        assertEquals("منطقة جازان", PlaceNamer.compose(listOf(AddressParts(region = "منطقة جازان", country = "السعودية")))!!.name)
        assertNull(PlaceNamer.compose(listOf(AddressParts(country = "السعودية", district = "  "))))
        assertNull(PlaceNamer.compose(emptyList()))
    }
}

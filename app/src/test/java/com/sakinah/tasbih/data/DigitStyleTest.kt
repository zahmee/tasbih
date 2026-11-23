package com.sakinah.tasbih.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitStyleTest {
    @After
    fun backToHindi() = DigitStyle.use(western = false)

    @Test
    fun numbersAreInHindiDigitsUnlessTheUserChoseArabic() {
        assertEquals("١٢٣", arabicNumber(123))
        assertEquals("٠٥", twoDigits(5))
        DigitStyle.use(western = true)
        assertEquals("123", arabicNumber(123))
        assertEquals("05", twoDigits(5))
        assertEquals("12", twoDigits(12))
    }
}

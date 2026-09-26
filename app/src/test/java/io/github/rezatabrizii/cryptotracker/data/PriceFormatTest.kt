package io.github.rezatabrizii.cryptotracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFormatTest {

    @Test
    fun full_groupsThousands() {
        assertEquals("112,350", PriceFormat.full(112_350))
        assertEquals("1,234,567", PriceFormat.full(1_234_567))
    }

    @Test
    fun compact_fitsSmallComplications() {
        assertEquals("9,999", PriceFormat.compact(9_999))
        assertEquals("112.4K", PriceFormat.compact(112_350))
        assertEquals("98K", PriceFormat.compact(98_000))
        assertEquals("1.23M", PriceFormat.compact(1_234_567))
    }
}

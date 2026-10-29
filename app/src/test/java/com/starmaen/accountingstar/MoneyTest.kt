package com.starmaen.accountingstar

import com.starmaen.accountingstar.util.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun `round keeps two decimals`() {
        assertEquals(10.13, Money.round(10.125), 0.0001)
        assertEquals(0.0, Money.round(0.0), 0.0001)
    }

    @Test
    fun `format adds thousands separator`() {
        assertEquals("1,234.50", Money.format(1234.5))
        assertEquals("0.00", Money.format(0.0))
    }

    @Test
    fun `parse accepts arabic decimal separator`() {
        assertEquals(12.5, Money.parse("12,5"), 0.0001)
        assertEquals(0.0, Money.parse("غير رقم"), 0.0001)
    }

    @Test
    fun `percent handles zero total`() {
        assertEquals(0.0, Money.percent(10.0, 0.0), 0.0001)
        assertEquals(25.0, Money.percent(25.0, 100.0), 0.0001)
    }
}

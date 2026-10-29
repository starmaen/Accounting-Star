package com.starmaen.accountingstar

import com.starmaen.accountingstar.data.InvoiceType
import com.starmaen.accountingstar.data.PaymentType
import com.starmaen.accountingstar.print.ReceiptInfo
import com.starmaen.accountingstar.print.ReceiptLine
import com.starmaen.accountingstar.print.ReceiptText
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptTextTest {
    private fun sample() = ReceiptInfo(
        shopName = "نجم المحاسبة",
        address = "الرياض",
        phone = "0500000000",
        taxNumber = "300000000000003",
        invoiceNo = "S-0001",
        dateText = "2026-10-29 10:00",
        invoiceType = InvoiceType.SALE,
        paymentType = PaymentType.CASH,
        partyLabel = "العميل",
        partyName = "عميل نقدي",
        cashier = "المدير",
        lines = listOf(ReceiptLine("قميص قطني", 2.0, 50.0, 100.0)),
        subtotal = 100.0,
        discount = 5.0,
        taxAmount = 7.13,
        total = 102.13,
        paid = 102.13,
        remaining = 0.0,
        footer = "شكراً لتعاملكم معنا",
        currency = "ر.س"
    )

    @Test
    fun `receipt contains invoice number and totals`() {
        val text = ReceiptText.build(sample())
        assertTrue(text.contains("S-0001"))
        assertTrue(text.contains("نجم المحاسبة"))
        assertTrue(text.contains("قميص قطني"))
        assertTrue(text.contains("102.13"))
        assertTrue(text.contains("شكراً لتعاملكم معنا"))
    }

    @Test
    fun `receipt width is respected`() {
        val narrow = ReceiptText.build(sample(), ReceiptText.WIDTH_58)
        assertTrue(narrow.lines().all { it.length <= 58 })
    }
}

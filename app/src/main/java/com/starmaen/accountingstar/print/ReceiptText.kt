package com.starmaen.accountingstar.print

import com.starmaen.accountingstar.data.InvoiceType
import com.starmaen.accountingstar.data.PaymentType
import com.starmaen.accountingstar.util.Money

data class ReceiptLine(
    val name: String,
    val qty: Double,
    val price: Double,
    val total: Double
)

data class ReceiptInfo(
    val shopName: String,
    val address: String,
    val phone: String,
    val taxNumber: String,
    val invoiceNo: String,
    val dateText: String,
    val invoiceType: String,
    val paymentType: String,
    val partyLabel: String,
    val partyName: String,
    val cashier: String,
    val lines: List<ReceiptLine>,
    val subtotal: Double,
    val discount: Double,
    val taxAmount: Double,
    val total: Double,
    val paid: Double,
    val remaining: Double,
    val footer: String,
    val currency: String
) {
    val typeLabel: String
        get() = when (invoiceType) {
            InvoiceType.SALE -> "فاتورة بيع"
            InvoiceType.PURCHASE -> "فاتورة شراء"
            InvoiceType.SALE_RETURN -> "مرتجع بيع"
            else -> "مرتجع شراء"
        }

    val paymentLabel: String
        get() = when (paymentType) {
            PaymentType.CASH -> "نقدي"
            PaymentType.CREDIT -> "آجل (دين)"
            else -> "تقسيط"
        }
}

object ReceiptText {
    const val WIDTH_58 = 32
    const val WIDTH_80 = 42

    fun build(info: ReceiptInfo, width: Int = WIDTH_58): String {
        val sb = StringBuilder()
        fun center(text: String) {
            if (text.length >= width) sb.appendLine(text) else {
                val pad = (width - text.length) / 2
                sb.appendLine(" ".repeat(pad) + text)
            }
        }
        fun line(char: Char = '-') = sb.appendLine(char.toString().repeat(width))
        fun row(right: String, left: String) {
            val space = width - right.length - left.length
            if (space <= 0) sb.appendLine("$right $left") else sb.appendLine(right + " ".repeat(space) + left)
        }

        center(info.shopName)
        if (info.address.isNotBlank()) center(info.address)
        if (info.phone.isNotBlank()) center("هاتف: ${info.phone}")
        if (info.taxNumber.isNotBlank()) center("الرقم الضريبي: ${info.taxNumber}")
        line('=')
        center(info.typeLabel)
        row("رقم: ${info.invoiceNo}", info.dateText)
        row("الدفع: ${info.paymentLabel}", "الكاشير: ${info.cashier}")
        if (info.partyName.isNotBlank()) sb.appendLine("${info.partyLabel}: ${info.partyName}")
        line()
        info.lines.forEach { item ->
            sb.appendLine(item.name)
            row("  ${Money.format(item.qty)} × ${Money.format(item.price)}", Money.format(item.total))
        }
        line()
        row("المجموع", Money.format(info.subtotal))
        if (info.discount > 0) row("الخصم", "-${Money.format(info.discount)}")
        if (info.taxAmount > 0) row("الضريبة", Money.format(info.taxAmount))
        row("الإجمالي", Money.format(info.total))
        row("المدفوع", Money.format(info.paid))
        if (info.remaining > 0) row("المتبقي", Money.format(info.remaining))
        line('=')
        center(info.footer)
        sb.appendLine()
        sb.appendLine()
        return sb.toString()
    }
}

package com.starmaen.accountingstar.print

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.ContextCompat
import com.starmaen.accountingstar.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object ReceiptPrinter {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun canUseBluetooth(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    suspend fun print(
        context: Context,
        info: ReceiptInfo,
        macAddress: String,
        paperWidth: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        if (macAddress.isBlank()) {
            AndroidPrinter.print(context, info, paperWidth)
            return@withContext Result.success("تم إرسال الفاتورة إلى خدمة الطباعة")
        }
        runCatching {
            check(canUseBluetooth(context)) { "يلزم منح إذن البلوتوث للطباعة الحرارية" }
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: error("الجهاز لا يدعم البلوتوث")
            check(adapter.isEnabled) { "البلوتوث غير مفعّل" }
            val device = adapter.getRemoteDevice(macAddress.trim())
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            try {
                socket.connect()
                val dots = if (paperWidth >= 80) 576 else 384
                val bitmap = renderBitmap(info, dots)
                val out = socket.outputStream
                out.write(byteArrayOf(0x1B, 0x40))
                out.write(rasterBytes(bitmap))
                out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
                out.flush()
            } finally {
                runCatching { socket.close() }
            }
            "تمت الطباعة على الطابعة الحرارية"
        }
    }

    fun preview(info: ReceiptInfo, paperWidth: Int = 58): String =
        ReceiptText.build(info, if (paperWidth >= 80) ReceiptText.WIDTH_80 else ReceiptText.WIDTH_58)

    fun testInfo(shopName: String, footer: String): ReceiptInfo = ReceiptInfo(
        shopName = shopName,
        address = "عنوان المتجر",
        phone = "0500000000",
        taxNumber = "",
        invoiceNo = "TEST-0001",
        dateText = com.starmaen.accountingstar.util.Dates.formatDateTime(System.currentTimeMillis()),
        invoiceType = "SALE",
        paymentType = "CASH",
        partyLabel = "العميل",
        partyName = "عميل نقدي",
        cashier = "المدير",
        lines = listOf(
            ReceiptLine("صنف تجريبي", 2.0, 10.0, 20.0),
            ReceiptLine("فاتورة اختبار الطباعة", 1.0, 5.0, 5.0)
        ),
        subtotal = 25.0,
        discount = 0.0,
        taxAmount = 0.0,
        total = 25.0,
        paid = 25.0,
        remaining = 0.0,
        footer = footer,
        currency = "ر.س"
    )

    private fun renderBitmap(info: ReceiptInfo, width: Int): Bitmap {
        val padding = 10f
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val bold = Paint(normal).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        val big = Paint(bold).apply { textSize = 27f }
        val small = Paint(normal).apply { textSize = 17f }
        val rows = buildRows(info, normal, bold, big, small, width - padding * 2)
        val lineHeight = 25f
        var height = padding * 2
        rows.forEach { height += it.lines.size * lineHeight + 3f }
        val bitmap = Bitmap.createBitmap(width, height.toInt().coerceAtLeast(100), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = padding
        rows.forEach { row ->
            row.lines.forEachIndexed { index, text ->
                val paint = row.paint
                val baseline = y + index * lineHeight + lineHeight * 0.75f
                when {
                    row.center -> canvas.drawText(text, (width - paint.measureText(text)) / 2f, baseline, paint)
                    row.leftOnly -> canvas.drawText(text, padding, baseline, paint)
                    index == 0 -> {
                        canvas.drawText(text, padding, baseline, paint)
                        val value = row.left
                        canvas.drawText(value, width - padding - paint.measureText(value), baseline, paint)
                    }
                    else -> canvas.drawText(text, padding, baseline, paint)
                }
            }
            y += row.lines.size * lineHeight + 3f
        }
        return bitmap
    }

    private data class DrawRow(
        val lines: List<String>,
        val left: String = "",
        val paint: Paint,
        val center: Boolean = false,
        val leftOnly: Boolean = false
    )

    private fun buildRows(
        info: ReceiptInfo,
        normal: Paint,
        bold: Paint,
        big: Paint,
        small: Paint,
        maxWidth: Float
    ): List<DrawRow> {
        val rows = mutableListOf<DrawRow>()
        fun center(text: String, paint: Paint = bold) {
            if (text.isNotBlank()) rows.add(DrawRow(wrap(text, paint, maxWidth), paint = paint, center = true))
        }
        fun row(right: String, left: String, paint: Paint = normal) {
            rows.add(DrawRow(wrap(right, paint, maxWidth * 0.62f), left, paint))
        }
        fun divider() = rows.add(DrawRow(listOf("-".repeat(46)), paint = small, leftOnly = true))

        center(info.shopName, big)
        center(info.address, small)
        if (info.phone.isNotBlank()) center("هاتف: ${info.phone}", small)
        if (info.taxNumber.isNotBlank()) center("الرقم الضريبي: ${info.taxNumber}", small)
        divider()
        center(info.typeLabel)
        row("رقم: ${info.invoiceNo}", info.dateText)
        row("الدفع: ${info.paymentLabel}", "الكاشير: ${info.cashier}")
        if (info.partyName.isNotBlank()) center("${info.partyLabel}: ${info.partyName}", normal)
        divider()
        info.lines.forEach { item ->
            center(item.name, normal)
            row("${Money.format(item.qty)} × ${Money.format(item.price)}", Money.format(item.total))
        }
        divider()
        row("المجموع", Money.format(info.subtotal))
        if (info.discount > 0) row("الخصم", "-${Money.format(info.discount)}")
        if (info.taxAmount > 0) row("الضريبة", Money.format(info.taxAmount))
        row("الإجمالي", Money.format(info.total), bold)
        row("المدفوع", Money.format(info.paid))
        if (info.remaining > 0) row("المتبقي", Money.format(info.remaining), bold)
        divider()
        center(info.footer, small)
        center("العملة: ${info.currency}", small)
        return rows
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(' ')
        val result = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) result.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) result.add(current)
        return result.ifEmpty { listOf(text) }
    }

    private fun rasterBytes(bitmap: Bitmap): ByteArray {
        val widthBytes = (bitmap.width + 7) / 8
        val height = bitmap.height
        val data = ByteArray(widthBytes * height)
        val pixels = IntArray(bitmap.width)
        for (y in 0 until height) {
            bitmap.getPixels(pixels, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in 0 until bitmap.width) {
                val pixel = pixels[x]
                val luminance = ((Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000)
                if (luminance < 150) {
                    val index = y * widthBytes + (x / 8)
                    data[index] = (data[index].toInt() or (0x80 ushr (x % 8))).toByte()
                }
            }
        }
        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (widthBytes and 0xFF).toByte(),
            ((widthBytes shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(),
            ((height shr 8) and 0xFF).toByte()
        )
        return header + data
    }
}

object AndroidPrinter {
    fun print(context: Context, info: ReceiptInfo, paperWidth: Int) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "فاتورة ${info.invoiceNo}"
        printManager.print(jobName, ReceiptPrintAdapter(info, paperWidth), null)
    }

    private class ReceiptPrintAdapter(
        private val info: ReceiptInfo,
        private val paperWidth: Int
    ) : PrintDocumentAdapter() {
        private var document: PdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }
            val builder = PrintDocumentInfo.Builder("receipt-${info.invoiceNo}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
            callback?.onLayoutFinished(builder.build(), true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            if (destination == null) {
                callback?.onWriteFailed(null)
                return
            }
            try {
                val pdf = PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = pdf.startPage(pageInfo)
                drawReceipt(page.canvas, info, pageWidth)
                pdf.finishPage(page)
                val out = java.io.FileOutputStream(destination.fileDescriptor)
                pdf.writeTo(out)
                out.flush()
                out.close()
                pdf.close()
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (t: Throwable) {
                callback?.onWriteFailed(t.message)
            }
        }

        override fun onFinish() {
            document?.close()
        }
    }

    private fun drawReceipt(canvas: Canvas, info: ReceiptInfo, pageWidth: Int) {
        val margin = 40f
        val maxWidth = pageWidth - margin * 2
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val bold = Paint(normal).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        var y = 56f
        fun center(text: String, paint: Paint = normal, step: Float = 16f) {
            if (text.isBlank()) return
            canvas.drawText(text, pageWidth / 2f, y, paint)
            y += step
        }
        fun row(right: String, left: String, paint: Paint = normal) {
            canvas.drawText(right, pageWidth - margin, y, paint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(left, margin, y, paint.apply { textAlign = Paint.Align.LEFT })
            y += 15f
        }
        center(info.shopName, title, 26f)
        center(info.address)
        if (info.phone.isNotBlank()) center("هاتف: ${info.phone}")
        if (info.taxNumber.isNotBlank()) center("الرقم الضريبي: ${info.taxNumber}")
        y += 4f
        center(info.typeLabel, bold)
        row("رقم: ${info.invoiceNo}", info.dateText)
        row("الدفع: ${info.paymentLabel}", "الكاشير: ${info.cashier}")
        if (info.partyName.isNotBlank()) center("${info.partyLabel}: ${info.partyName}")
        y += 4f
        canvas.drawLine(margin, y, pageWidth - margin, y, normal)
        y += 16f
        info.lines.forEach { item ->
            val namePaint = Paint(normal)
            canvas.drawText(item.name, pageWidth - margin, y, namePaint.apply { textAlign = Paint.Align.RIGHT })
            y += 15f
            row("${Money.format(item.qty)} × ${Money.format(item.price)}", Money.format(item.total))
        }
        canvas.drawLine(margin, y, pageWidth - margin, y, normal)
        y += 18f
        row("المجموع", Money.format(info.subtotal))
        if (info.discount > 0) row("الخصم", "-${Money.format(info.discount)}")
        if (info.taxAmount > 0) row("الضريبة", Money.format(info.taxAmount))
        row("الإجمالي", Money.format(info.total), bold)
        row("المدفوع", Money.format(info.paid))
        if (info.remaining > 0) row("المتبقي", Money.format(info.remaining), bold)
        y += 10f
        center(info.footer)
        center("العملة: ${info.currency}")
    }
}

package com.starmaen.accountingstar.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Money {
    private val format = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun round(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    fun format(value: Double): String = format.format(round(value))

    fun format(value: Double, currency: String): String = "${format.format(round(value))} $currency"

    fun parse(text: String): Double = text.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

    fun percent(part: Double, total: Double): Double =
        if (abs(total) < 0.000001) 0.0 else (part / total) * 100.0
}

object Dates {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    fun dayStart(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun dayEnd(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun monthStart(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return dayStart(cal.timeInMillis)
    }

    fun addDays(millis: Long, days: Int): Long = millis + days.toLong() * 24L * 60L * 60L * 1000L

    fun addMonths(millis: Long, months: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    fun format(millis: Long): String = dateFormat.format(Date(millis))
    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))
    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))

    fun parse(text: String): Long? = runCatching { dateFormat.parse(text.trim())?.time }.getOrNull()
}

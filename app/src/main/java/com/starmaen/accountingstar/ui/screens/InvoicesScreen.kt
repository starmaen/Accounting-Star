package com.starmaen.accountingstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.starmaen.accountingstar.data.InvoiceEntity
import com.starmaen.accountingstar.data.InvoiceStatus
import com.starmaen.accountingstar.data.InvoiceType
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.DividerLine
import com.starmaen.accountingstar.ui.components.EmptyState
import com.starmaen.accountingstar.ui.components.KeyValueRow
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.ui.theme.SuccessGreen
import com.starmaen.accountingstar.util.Dates
import com.starmaen.accountingstar.util.Money

fun invoiceTypeLabel(type: String): String = when (type) {
    InvoiceType.SALE -> "بيع"
    InvoiceType.PURCHASE -> "شراء"
    InvoiceType.SALE_RETURN -> "مرتجع بيع"
    else -> "مرتجع شراء"
}

fun invoiceStatusLabel(status: String): String = when (status) {
    InvoiceStatus.PAID -> "مدفوعة"
    InvoiceStatus.PARTIAL -> "جزئية"
    else -> "غير مدفوعة"
}

@Composable
fun InvoicesScreen(vm: AppViewModel, state: AppUiState) {
    val currency = state.currentProject?.currency ?: "ر.س"
    val context = LocalContext.current
    var filter by remember { mutableStateOf("") }
    var paying by remember { mutableStateOf(false) }
    val invoices = remember(state.invoices, filter) {
        if (filter.isEmpty()) state.invoices else state.invoices.filter { it.type == filter }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFFBF5))) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "" to "الكل",
                InvoiceType.SALE to "بيع",
                InvoiceType.PURCHASE to "شراء",
                InvoiceType.SALE_RETURN to "مرتجع بيع",
                InvoiceType.PURCHASE_RETURN to "مرتجع شراء"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = filter == type,
                    onClick = { filter = type },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
        if (invoices.isEmpty()) {
            EmptyState("لا توجد فواتير في هذا التصنيف")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(invoices, key = { it.id }) { invoice ->
                    InvoiceCard(invoice, currency) { vm.openInvoice(invoice.id) }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    state.invoiceDetail?.let { detail ->
        Dialog(
            onDismissRequest = { vm.closeInvoice() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(Modifier.fillMaxSize(), color = Color(0xFFFFFBF5)) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().background(BrandGreen).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "فاتورة ${detail.invoice.invoiceNo}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "${invoiceTypeLabel(detail.invoice.type)} • ${Dates.formatDateTime(detail.invoice.date)}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { vm.closeInvoice() }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                KeyValueRow("الطرف", detail.invoice.partyName.ifBlank { "عميل نقدي" })
                                KeyValueRow("طريقة الدفع", paymentTypeLabel(detail.invoice.paymentType))
                                KeyValueRow("الحالة", invoiceStatusLabel(detail.invoice.status))
                                if (detail.invoice.notes.isNotBlank()) KeyValueRow("ملاحظات", detail.invoice.notes)
                            }
                        }
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("الأصناف", fontWeight = FontWeight.Bold, color = BrandGreen)
                                Spacer(Modifier.height(6.dp))
                                detail.items.forEach { item ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${Money.format(item.qty)} ${item.unit} × ${Money.format(item.unitPrice)}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF77736B)
                                            )
                                        }
                                        Text(Money.format(item.lineTotal, currency), fontSize = 13.sp)
                                    }
                                    DividerLine()
                                }
                            }
                        }
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                KeyValueRow("المجموع", Money.format(detail.invoice.subtotal, currency))
                                KeyValueRow("الخصم", Money.format(detail.invoice.discount, currency))
                                KeyValueRow("الضريبة", Money.format(detail.invoice.taxAmount, currency))
                                KeyValueRow("الإجمالي", Money.format(detail.invoice.total, currency), bold = true)
                                KeyValueRow("المدفوع", Money.format(detail.invoice.paid, currency), color = SuccessGreen)
                                KeyValueRow(
                                    "المتبقي",
                                    Money.format(detail.invoice.remaining, currency),
                                    bold = true,
                                    color = if (detail.invoice.remaining > 0) DangerRed else SuccessGreen
                                )
                            }
                        }
                        if (detail.payments.isNotEmpty()) {
                            Card(shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("الدفعات", fontWeight = FontWeight.Bold, color = BrandGreen)
                                    detail.payments.forEach { payment ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(Dates.format(payment.date), fontSize = 12.sp)
                                            Text("${Money.format(payment.amount, currency)} • ${payment.method}", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                        if (detail.installments.isNotEmpty()) {
                            Card(shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("جدول الأقساط", fontWeight = FontWeight.Bold, color = BrandGreen)
                                    detail.installments.forEach { installment ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "قسط ${installment.seq} • ${Dates.format(installment.dueDate)}",
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                "${Money.format(installment.paidAmount)} / ${Money.format(installment.amount, currency)}",
                                                fontSize = 12.sp,
                                                color = if (installment.status == InvoiceStatus.PAID) SuccessGreen else DangerRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Surface(color = Color.White, shadowElevation = 8.dp) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { vm.showReceipt(detail.invoice.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("معاينة")
                            }
                            Button(
                                onClick = { vm.printInvoice(context, detail.invoice.id) },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandGold)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("طباعة")
                            }
                            if (detail.invoice.remaining > 0) {
                                Button(
                                    onClick = { paying = true },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("دفعة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (paying && state.invoiceDetail != null) {
        val invoice = state.invoiceDetail!!.invoice
        var amount by remember(invoice.id) { mutableStateOf(invoice.remaining.toString()) }
        var method by remember { mutableStateOf("CASH") }
        AlertDialog(
            onDismissRequest = { paying = false },
            title = { Text("تسجيل دفعة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المتبقي: ${Money.format(invoice.remaining, currency)}", fontWeight = FontWeight.Bold)
                    LabeledField("المبلغ", amount, { amount = it }, numeric = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH" to "نقدي", "BANK" to "تحويل", "CARD" to "شبكة").forEach { (m, label) ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.payInvoice(invoice.id, Money.parse(amount), method)
                    paying = false
                }) { Text("تسجيل") }
            },
            dismissButton = { TextButton(onClick = { paying = false }) { Text("إلغاء") } }
        )
    }

    state.receiptPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { vm.closeReceipt() },
            title = { Text("معاينة الفاتورة") },
            text = {
                Column(Modifier.height(380.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        preview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                val invoiceId = state.invoiceDetail?.invoice?.id
                if (invoiceId != null) {
                    Button(onClick = {
                        vm.closeReceipt()
                        vm.printInvoice(context, invoiceId)
                    }) { Text("طباعة") }
                }
            },
            dismissButton = { TextButton(onClick = { vm.closeReceipt() }) { Text("إغلاق") } }
        )
    }
}

private fun paymentTypeLabel(type: String): String = when (type) {
    "CASH" -> "نقدي"
    "CREDIT" -> "آجل / دين"
    "INSTALLMENT" -> "تقسيط"
    else -> type
}

@Composable
private fun InvoiceCard(invoice: InvoiceEntity, currency: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(invoice.invoiceNo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandGreen)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        invoiceTypeLabel(invoice.type),
                        fontSize = 10.sp,
                        color = Color(0xFF77736B)
                    )
                }
                Text(
                    invoice.partyName.ifBlank { "عميل نقدي" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(Dates.formatDateTime(invoice.date), fontSize = 11.sp, color = Color(0xFF77736B))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(invoice.total, currency), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    invoiceStatusLabel(invoice.status),
                    fontSize = 11.sp,
                    color = when (invoice.status) {
                        InvoiceStatus.PAID -> SuccessGreen
                        InvoiceStatus.PARTIAL -> BrandGold
                        else -> DangerRed
                    }
                )
                if (invoice.remaining > 0) {
                    Text(
                        "متبقي ${Money.format(invoice.remaining, currency)}",
                        fontSize = 10.sp,
                        color = DangerRed
                    )
                }
            }
        }
    }
}

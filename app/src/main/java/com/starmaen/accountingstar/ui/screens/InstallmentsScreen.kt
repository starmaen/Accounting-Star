package com.starmaen.accountingstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmaen.accountingstar.data.InstallmentRow
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.EmptyState
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.util.Dates
import com.starmaen.accountingstar.util.Money

@Composable
fun InstallmentsScreen(vm: AppViewModel, state: AppUiState) {
    val currency = state.currentProject?.currency ?: "ر.س"
    var paying by remember { mutableStateOf<InstallmentRow?>(null) }
    val due = state.dueInstallments

    Column(Modifier.fillMaxSize().background(Color(0xFFFFFBF5))) {
        Card(
            Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGold.copy(alpha = 0.14f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("الأقساط المستحقة", fontWeight = FontWeight.Bold, color = Color(0xFF6B5300))
                Text(
                    "عدد الأقساط التي حان موعدها: ${due.size} — الدفع هنا يُسجّل تلقائياً على الفاتورة الأصلية.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B5300)
                )
            }
        }
        if (due.isEmpty()) {
            EmptyState("لا توجد أقساط مستحقة حالياً")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(due, key = { it.id }) { installment ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(installment.customerName.ifBlank { "عميل نقدي" }, fontWeight = FontWeight.Bold)
                                Text(
                                    "فاتورة ${installment.invoiceNo} • قسط ${installment.seq}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF77736B)
                                )
                                Text(
                                    "الاستحقاق: ${Dates.format(installment.dueDate)}",
                                    fontSize = 11.sp,
                                    color = DangerRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    Money.format(installment.amount - installment.paidAmount, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                                Text("المتبقي من القسط", fontSize = 10.sp, color = Color(0xFF77736B))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { paying = installment }) {
                                Icon(Icons.Default.Payments, contentDescription = null)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    paying?.let { installment ->
        var amount by remember(installment.id) {
            mutableStateOf((installment.amount - installment.paidAmount).toString())
        }
        var method by remember { mutableStateOf("CASH") }
        AlertDialog(
            onDismissRequest = { paying = null },
            title = { Text("تحصيل قسط") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("العميل: ${installment.customerName}", fontWeight = FontWeight.Medium)
                    Text("المتبقي من القسط: ${Money.format(installment.amount - installment.paidAmount, currency)}")
                    LabeledField("المبلغ المحصل", amount, { amount = it }, numeric = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH" to "نقدي", "BANK" to "تحويل", "CARD" to "شبكة").forEach { (m, label) ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.payInvoice(installment.invoiceId, Money.parse(amount), method)
                    paying = null
                }) { Text("تحصيل") }
            },
            dismissButton = { TextButton(onClick = { paying = null }) { Text("إلغاء") } }
        )
    }
}

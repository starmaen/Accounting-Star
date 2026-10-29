package com.starmaen.accountingstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.DividerLine
import com.starmaen.accountingstar.ui.components.EmptyState
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.components.SectionCard
import com.starmaen.accountingstar.ui.components.StatCard
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.ui.theme.SuccessGreen
import com.starmaen.accountingstar.util.Dates
import com.starmaen.accountingstar.util.Money

@Composable
fun ReportsScreen(vm: AppViewModel, state: AppUiState) {
    val currency = state.currentProject?.currency ?: "ر.س"
    var from by remember { mutableStateOf(Dates.format(Dates.monthStart())) }
    var to by remember { mutableStateOf(Dates.format(System.currentTimeMillis())) }
    var showExpense by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentProject?.id) {
        vm.loadReport(Dates.monthStart(), Dates.dayEnd())
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF5))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الفترة", fontWeight = FontWeight.Bold, color = BrandGreen)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            from = Dates.format(System.currentTimeMillis())
                            to = from
                            Dates.parse(from)?.let { vm.loadReport(it, Dates.dayEnd(it)) }
                        },
                        label = { Text("اليوم", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            from = Dates.format(Dates.monthStart())
                            to = Dates.format(System.currentTimeMillis())
                            vm.loadReport(Dates.monthStart(), Dates.dayEnd())
                        },
                        label = { Text("هذا الشهر", fontSize = 11.sp) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("من", from, { from = it }, Modifier.weight(1f))
                    LabeledField("إلى", to, { to = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val start = Dates.parse(from)?.let { Dates.dayStart(it) }
                            val end = Dates.parse(to)?.let { Dates.dayEnd(it) }
                            if (start == null || end == null) vm.message("صيغة التاريخ يجب أن تكون yyyy-MM-dd")
                            else vm.loadReport(start, end)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("تطبيق") }
                    OutlinedButton(onClick = { showExpense = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("مصروف")
                    }
                }
            }
        }

        val report = state.report
        if (report == null) {
            EmptyState("جارٍ تحميل التقرير...")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("المبيعات", Money.format(report.sales, currency), Modifier.weight(1f))
                StatCard("المشتريات", Money.format(report.purchases, currency), Modifier.weight(1f), BrandGold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("الربح الإجمالي", Money.format(report.profit, currency), Modifier.weight(1f), SuccessGreen)
                StatCard("المصروفات", Money.format(report.expenses, currency), Modifier.weight(1f), DangerRed)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    "صافي الربح",
                    Money.format(report.profit - report.expenses, currency),
                    Modifier.weight(1f),
                    if (report.profit - report.expenses >= 0) SuccessGreen else DangerRed,
                    "بعد المصروفات"
                )
                StatCard("المرتجعات", Money.format(report.returns, currency), Modifier.weight(1f), Color(0xFFD97706))
            }

            SectionCard("الأصناف الأكثر مبيعاً") {
                if (report.topProducts.isEmpty()) {
                    Text("لا توجد مبيعات في الفترة المحددة", fontSize = 13.sp, color = Color(0xFF77736B))
                } else {
                    report.topProducts.forEachIndexed { index, row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}.", fontWeight = FontWeight.Bold, color = BrandGold, modifier = Modifier.width(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text(row.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("الكمية: ${Money.format(row.qty)}", fontSize = 11.sp, color = Color(0xFF77736B))
                            }
                            Text(Money.format(row.total, currency), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (index < report.topProducts.lastIndex) DividerLine()
                    }
                }
            }

            SectionCard("أصناف تحت حد التنبيه") {
                if (report.lowStock.isEmpty()) {
                    Text("لا توجد أصناف منخفضة", fontSize = 13.sp, color = SuccessGreen)
                } else {
                    report.lowStock.take(12).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.name, fontSize = 12.sp)
                            Text(
                                "${Money.format(row.quantity)} • ${row.warehouseName}",
                                fontSize = 11.sp,
                                color = DangerRed
                            )
                        }
                    }
                }
            }

            SectionCard("ملخص الفترة") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("من ${Dates.format(report.from)}")
                    Text("إلى ${Dates.format(report.to)}")
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("تكلفة البضاعة المباعة", fontSize = 12.sp)
                    Text(Money.format(report.sales - report.profit, currency), fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("هامش الربح", fontSize = 12.sp)
                    Text(
                        Money.format(Money.percent(report.profit, report.sales), "%"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showExpense) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("تشغيلية") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExpense = false },
            title = { Text("تسجيل مصروف") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("البيان *", title, { title = it })
                    LabeledField("المبلغ *", amount, { amount = it }, numeric = true)
                    Text("التصنيف", fontSize = 12.sp, color = Color(0xFF77736B))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("تشغيلية", "إيجار", "رواتب", "تسويق", "أخرى").forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(c, fontSize = 10.sp) }
                            )
                        }
                    }
                    LabeledField("ملاحظات", notes, { notes = it })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.addExpense(title, category, Money.parse(amount), notes)
                        showExpense = false
                    },
                    enabled = title.isNotBlank() && Money.parse(amount) > 0
                ) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showExpense = false }) { Text("إلغاء") } }
        )
    }
}

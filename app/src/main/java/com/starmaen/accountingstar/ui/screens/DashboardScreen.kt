package com.starmaen.accountingstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.Dest
import com.starmaen.accountingstar.ui.components.DividerLine
import com.starmaen.accountingstar.ui.components.KeyValueRow
import com.starmaen.accountingstar.ui.components.SectionCard
import com.starmaen.accountingstar.ui.components.SmallTag
import com.starmaen.accountingstar.ui.components.StatCard
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.ui.theme.SuccessGreen
import com.starmaen.accountingstar.util.Dates
import com.starmaen.accountingstar.util.Money

@Composable
fun DashboardScreen(vm: AppViewModel, state: AppUiState, onNavigate: (Dest) -> Unit) {
    val currency = state.currentProject?.currency ?: "ر.س"
    val stats = state.stats
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFFFFBF5)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreen)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(
                        "أهلاً ${state.currentUser?.fullName ?: ""}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "ملخص أعمال ${state.currentProject?.name ?: ""} — ${Dates.format(System.currentTimeMillis())}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallTag(state.activeBranchName.ifBlank { "فرع رئيسي" }, BrandGold)
                        SmallTag(state.activeWarehouseName.ifBlank { "المستودع" }, Color.White)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("مبيعات اليوم", Money.format(stats.todaySales, currency), Modifier.weight(1f))
                StatCard("مبيعات الشهر", Money.format(stats.monthSales, currency), Modifier.weight(1f), BrandGold)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    "ربح الشهر",
                    Money.format(stats.monthProfit, currency),
                    Modifier.weight(1f),
                    SuccessGreen,
                    "بعد خصم التكلفة"
                )
                StatCard(
                    "مصروفات الشهر",
                    Money.format(stats.expenses, currency),
                    Modifier.weight(1f),
                    DangerRed
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    "ديون العملاء",
                    Money.format(stats.receivables, currency),
                    Modifier.weight(1f),
                    DangerRed,
                    "مستحقة لك"
                )
                StatCard(
                    "مستحقات الموردين",
                    Money.format(stats.payables, currency),
                    Modifier.weight(1f),
                    Color(0xFFD97706),
                    "عليك"
                )
            }
        }
        item {
            SectionCard("وصول سريع") {
                val actions = listOf(
                    Triple(Dest.POS, Icons.Default.PointOfSale, "نقطة البيع"),
                    Triple(Dest.PURCHASE, Icons.Default.ShoppingCart, "شراء"),
                    Triple(Dest.PRODUCTS, Icons.Default.Inventory2, "المنتجات"),
                    Triple(Dest.INVENTORY, Icons.Default.Warehouse, "المخزون"),
                    Triple(Dest.CUSTOMERS, Icons.Default.People, "العملاء"),
                    Triple(Dest.REPORTS, Icons.Default.BarChart, "التقارير")
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (dest, icon, label) ->
                                QuickAction(icon, label, Modifier.weight(1f)) { onNavigate(dest) }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(
                "تنبيهات المخزون",
                action = {
                    if (stats.lowStockCount > 0) SmallTag("${stats.lowStockCount} صنف", DangerRed)
                }
            ) {
                if (state.lowStock.isEmpty()) {
                    Text("كل الأصناف ضمن الحد الآمن", fontSize = 13.sp, color = SuccessGreen)
                } else {
                    state.lowStock.take(5).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${Money.format(row.quantity)} ${row.warehouseName}",
                                fontSize = 12.sp,
                                color = DangerRed
                            )
                        }
                    }
                }
            }
        }
        if (stats.dueInstallments > 0) {
            item {
                SectionCard("أقساط مستحقة", action = { SmallTag("${stats.dueInstallments} قسط", BrandGold) }) {
                    Text(
                        "توجد أقساط حان موعدها. افتح شاشة الأقساط لمتابعة التحصيل.",
                        fontSize = 13.sp
                    )
                }
            }
        }
        item {
            SectionCard("أحدث الفواتير") {
                if (state.invoices.isEmpty()) {
                    Text("لا توجد فواتير بعد. ابدأ من نقطة البيع.", fontSize = 13.sp, color = Color(0xFF77736B))
                } else {
                    state.invoices.take(5).forEach { invoice ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${invoice.invoiceNo} • ${invoice.partyName.ifBlank { "عميل نقدي" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(Dates.formatDateTime(invoice.date), fontSize = 11.sp, color = Color(0xFF77736B))
                            }
                            Text(
                                Money.format(invoice.total, currency),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (invoice.remaining > 0) DangerRed else SuccessGreen
                            )
                        }
                        DividerLine()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F1E7))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(BrandGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = BrandGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

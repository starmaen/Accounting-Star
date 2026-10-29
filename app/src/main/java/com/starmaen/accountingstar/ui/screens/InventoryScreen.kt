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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.starmaen.accountingstar.data.StockRow
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.EmptyState
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.util.Money

@Composable
fun InventoryScreen(vm: AppViewModel, state: AppUiState) {
    val currency = state.currentProject?.currency ?: "ر.س"
    var query by remember { mutableStateOf("") }
    var adjusting by remember { mutableStateOf<StockRow?>(null) }
    var transferring by remember { mutableStateOf<StockRow?>(null) }
    val filtered = remember(state.stockRows, query) {
        val q = query.trim()
        state.stockRows.filter {
            q.isEmpty() || it.name.contains(q, true) || it.barcode.contains(q)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFFBF5))) {
        if (state.warehouses.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = state.warehouses.indexOfFirst { it.id == state.activeWarehouseId }.coerceAtLeast(0),
                edgePadding = 8.dp,
                containerColor = Color.White
            ) {
                state.warehouses.forEach { warehouse ->
                    Tab(
                        selected = warehouse.id == state.activeWarehouseId,
                        onClick = { vm.setActiveWarehouse(warehouse.id) },
                        text = { Text(warehouse.name, fontSize = 12.sp) }
                    )
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("بحث في مخزون ${state.activeWarehouseName}") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        if (state.lowStock.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGold.copy(alpha = 0.15f))
            ) {
                Text(
                    "يوجد ${state.lowStock.size} صنفاً عند حد التنبيه أو أقل — راجع الكميات أدناه",
                    Modifier.padding(10.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF6B5300)
                )
            }
        }
        if (filtered.isEmpty()) {
            EmptyState("المخزون فارغ أو لا توجد نتائج")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.productId }) { row ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(row.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    listOf(row.barcode, row.unit, row.categoryName).filter { it.isNotBlank() }.joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = Color(0xFF77736B)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "الكمية: ${Money.format(row.quantity)} ${row.unit}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (row.quantity <= row.minQty) DangerRed else BrandGreen
                                    )
                                    Text("التكلفة: ${Money.format(row.costPrice, currency)}", fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { adjusting = row }) {
                                Icon(Icons.Default.Tune, contentDescription = "تعديل الكمية", tint = BrandGreen)
                            }
                            IconButton(
                                onClick = { transferring = row },
                                enabled = state.warehouses.size > 1
                            ) {
                                Icon(Icons.Default.CompareArrows, contentDescription = "نقل", tint = BrandGold)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    adjusting?.let { row ->
        AdjustStockDialog(row, currency, onDismiss = { adjusting = null }) { qty ->
            vm.adjustStock(row.productId, qty)
            adjusting = null
        }
    }

    transferring?.let { row ->
        TransferDialog(
            row = row,
            warehouses = state.warehouses.filter { it.id != state.activeWarehouseId },
            onDismiss = { transferring = null }
        ) { toId, qty ->
            vm.transferStock(row.productId, toId, qty)
            transferring = null
        }
    }
}

@Composable
private fun AdjustStockDialog(
    row: StockRow,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var value by remember { mutableStateOf(row.quantity.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل كمية المخزون") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.name, fontWeight = FontWeight.Bold)
                Text("الكمية الحالية: ${Money.format(row.quantity)} ${row.unit}", fontSize = 13.sp)
                Text("التكلفة: ${Money.format(row.costPrice, currency)}", fontSize = 12.sp)
                LabeledField("الكمية الجديدة", value, { value = it }, numeric = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(Money.parse(value)) }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun TransferDialog(
    row: StockRow,
    warehouses: List<com.starmaen.accountingstar.data.WarehouseEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, Double) -> Unit
) {
    var target by remember { mutableStateOf(warehouses.firstOrNull()?.id ?: 0L) }
    var qty by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نقل بين المستودعات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.name, fontWeight = FontWeight.Bold)
                Text("الرصيد الحالي: ${Money.format(row.quantity)} ${row.unit}", fontSize = 13.sp)
                Text("إلى المستودع:", fontSize = 12.sp)
                warehouses.forEach { warehouse ->
                    FilterChip(
                        selected = target == warehouse.id,
                        onClick = { target = warehouse.id },
                        label = { Text(warehouse.name, fontSize = 12.sp) }
                    )
                }
                LabeledField("الكمية", qty, { qty = it }, numeric = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(target, Money.parse(qty)) },
                enabled = target > 0 && Money.parse(qty) > 0
            ) { Text("نقل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

package com.starmaen.accountingstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.starmaen.accountingstar.data.InvoiceLineInput
import com.starmaen.accountingstar.data.ProductEntity
import com.starmaen.accountingstar.data.SupplierEntity
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.DividerLine
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.util.Money
import kotlin.math.max

private data class PurchaseLine(
    val product: ProductEntity,
    val qty: Double,
    val unitPrice: Double
) {
    val total: Double get() = Money.round(qty * unitPrice)
}

@Composable
fun PurchaseScreen(vm: AppViewModel, state: AppUiState) {
    val currency = state.currentProject?.currency ?: "ر.س"
    var lines by remember { mutableStateOf<List<PurchaseLine>>(emptyList()) }
    var supplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var paid by remember { mutableStateOf("0") }
    var discount by remember { mutableStateOf("0") }
    var taxRate by remember { mutableStateOf((state.currentProject?.taxRate ?: 0.0).toString()) }

    val subtotal = Money.round(lines.sumOf { it.total })
    val discountValue = Money.parse(discount)
    val afterDiscount = max(0.0, subtotal - discountValue)
    val taxAmount = Money.round(afterDiscount * Money.parse(taxRate) / 100.0)
    val total = Money.round(afterDiscount + taxAmount)

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF5))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            Modifier.fillMaxWidth().clickable { showSupplierPicker = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreen)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("المورد", fontSize = 11.sp, color = Color(0xFF77736B))
                    Text(supplier?.name ?: "اختر المورد", fontWeight = FontWeight.Medium)
                }
                Text(if (supplier == null) "اختيار" else "تغيير", color = BrandGreen, fontSize = 12.sp)
            }
        }

        Button(onClick = { showProductPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("إضافة صنف للفاتورة")
        }

        if (lines.isEmpty()) {
            Text(
                "لا توجد أصناف. أضف الأصناف المشتراة لتحديث المخزون تلقائياً.",
                fontSize = 13.sp,
                color = Color(0xFF77736B)
            )
        } else {
            lines.forEach { line ->
                PurchaseLineEditor(
                    line = line,
                    currency = currency,
                    onChange = { qty, price ->
                        lines = lines.map {
                            if (it.product.id == line.product.id) it.copy(qty = qty, unitPrice = price) else it
                        }
                    },
                    onDelete = { lines = lines.filter { it.product.id != line.product.id } }
                )
            }
        }

        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("الخصم", discount, { discount = it }, Modifier.weight(1f), numeric = true)
                    LabeledField("الضريبة %", taxRate, { taxRate = it }, Modifier.weight(1f), numeric = true)
                }
                LabeledField("المدفوع للمورد الآن", paid, { paid = it }, numeric = true)
                DividerLine()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المجموع", fontSize = 13.sp)
                    Text(Money.format(subtotal, currency))
                }
                if (taxAmount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الضريبة", fontSize = 13.sp)
                        Text(Money.format(taxAmount, currency))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي", fontWeight = FontWeight.Bold)
                    Text(Money.format(total, currency), fontWeight = FontWeight.Bold, color = BrandGreen, fontSize = 18.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المتبقي للمورد", fontSize = 13.sp)
                    Text(
                        Money.format(max(0.0, total - Money.parse(paid)), currency),
                        color = DangerRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Button(
            onClick = {
                vm.createPurchase(
                    supplier = supplier,
                    lines = lines.map {
                        InvoiceLineInput(
                            productId = it.product.id,
                            name = it.product.name,
                            unit = it.product.unit,
                            qty = it.qty,
                            unitPrice = it.unitPrice,
                            costPrice = it.unitPrice
                        )
                    },
                    paid = Money.parse(paid),
                    discount = discountValue,
                    taxRate = Money.parse(taxRate)
                )
                lines = emptyList()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = lines.isNotEmpty() && supplier != null,
            shape = RoundedCornerShape(12.dp)
        ) { Text("حفظ فاتورة الشراء", fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(20.dp))
    }

    if (showSupplierPicker) {
        AlertDialog(
            onDismissRequest = { showSupplierPicker = false },
            title = { Text("اختيار المورد") },
            text = {
                if (state.suppliers.isEmpty()) {
                    Text("لا يوجد موردون. أضفهم من شاشة الموردين.")
                } else {
                    LazyColumn(Modifier.height(300.dp)) {
                        items(state.suppliers, key = { it.id }) { item ->
                            Column(
                                Modifier.fillMaxWidth().clickable {
                                    supplier = item
                                    showSupplierPicker = false
                                }.padding(12.dp)
                            ) {
                                Text(item.name, fontWeight = FontWeight.Medium)
                                Text(item.phone, fontSize = 11.sp, color = Color(0xFF77736B))
                            }
                            DividerLine()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSupplierPicker = false }) { Text("إغلاق") } }
        )
    }

    if (showProductPicker) {
        ProductPickerDialog(
            products = state.products,
            currency = currency,
            existing = lines.map { it.product.id }.toSet(),
            onDismiss = { showProductPicker = false }
        ) { product ->
            lines = lines + PurchaseLine(product, 1.0, product.costPrice)
            showProductPicker = false
        }
    }
}

@Composable
private fun ProductPickerDialog(
    products: List<ProductEntity>,
    currency: String,
    existing: Set<Long>,
    onDismiss: () -> Unit,
    onPick: (ProductEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        products.filter {
            it.id !in existing && (query.isBlank() || it.name.contains(query, true) || it.barcode.contains(query))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار صنف") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                LazyColumn(Modifier.height(320.dp)) {
                    items(filtered, key = { it.id }) { product ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(product) }.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(product.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(Money.format(product.costPrice, currency), fontSize = 12.sp)
                        }
                        DividerLine()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
private fun PurchaseLineEditor(
    line: PurchaseLine,
    currency: String,
    onChange: (Double, Double) -> Unit,
    onDelete: () -> Unit
) {
    var qtyText by remember(line.product.id) { mutableStateOf(line.qty.toString()) }
    var priceText by remember(line.product.id) { mutableStateOf(line.unitPrice.toString()) }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.product.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(
                        "التكلفة السابقة: ${Money.format(line.product.costPrice, currency)}",
                        fontSize = 10.sp,
                        color = Color(0xFF77736B)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("الكمية", qtyText, {
                    qtyText = it
                    onChange(Money.parse(it), Money.parse(priceText))
                }, Modifier.weight(1f), numeric = true)
                LabeledField("سعر الشراء", priceText, {
                    priceText = it
                    onChange(Money.parse(qtyText), Money.parse(it))
                }, Modifier.weight(1f), numeric = true)
            }
            Text(
                "الإجمالي: ${Money.format(line.total, currency)}",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = BrandGreen,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

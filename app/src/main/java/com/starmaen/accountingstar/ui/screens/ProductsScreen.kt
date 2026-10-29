package com.starmaen.accountingstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import com.starmaen.accountingstar.data.CategoryEntity
import com.starmaen.accountingstar.data.PriceTier
import com.starmaen.accountingstar.data.ProductEntity
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.ConfirmDialog
import com.starmaen.accountingstar.ui.components.EmptyState
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.util.Money

@Composable
fun ProductsScreen(vm: AppViewModel, state: AppUiState) {
    val currency = state.currentProject?.currency ?: "ر.س"
    var query by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var deleting by remember { mutableStateOf<ProductEntity?>(null) }
    val stock = remember(state.stockRows) { state.stockRows.associateBy { it.productId } }
    val products = remember(state.products, query) {
        val q = query.trim()
        state.products.filter {
            q.isEmpty() || it.name.contains(q, true) || it.barcode.contains(q) || it.sku.contains(q, true)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFFFFBF5))) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("بحث في المنتجات") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            if (products.isEmpty()) {
                EmptyState("لا توجد منتجات. أضف أول منتج بالزر +")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        listOf(product.barcode, product.sku, product.unit)
                                            .filter { it.isNotBlank() }.joinToString(" • "),
                                        fontSize = 11.sp,
                                        color = Color(0xFF77736B)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("قطعة ${Money.format(product.retailPrice)}", fontSize = 11.sp)
                                        Text("نصف ${Money.format(product.halfWholesalePrice)}", fontSize = 11.sp)
                                        Text("جملة ${Money.format(product.wholesalePrice)}", fontSize = 11.sp)
                                    }
                                    Text(
                                        "المخزون: ${Money.format(stock[product.id]?.quantity ?: 0.0)} ${product.unit}",
                                        fontSize = 11.sp,
                                        color = if ((stock[product.id]?.quantity ?: 0.0) <= product.minQty) DangerRed else BrandGreen
                                    )
                                }
                                IconButton(onClick = { editing = product; showForm = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = BrandGreen)
                                }
                                IconButton(onClick = { deleting = product }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = null; showForm = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            containerColor = BrandGreen,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
        }
    }

    if (showForm) {
        ProductFormDialog(
            product = editing,
            projectId = state.currentProject?.id ?: 0,
            categories = state.categories,
            currency = currency,
            onDismiss = { showForm = false },
            onSave = { product, opening ->
                vm.saveProduct(product, opening)
                showForm = false
            }
        )
    }

    deleting?.let { product ->
        ConfirmDialog(
            title = "تعطيل المنتج",
            text = "سيتم إخفاء المنتج من البيع مع الاحتفاظ بفواتيره القديمة. هل تريد المتابعة؟",
            confirmText = "تعطيل",
            onConfirm = {
                vm.deleteProduct(product.id)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ProductFormDialog(
    product: ProductEntity?,
    projectId: Long,
    categories: List<CategoryEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, Double) -> Unit
) {
    val isNew = product == null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "قطعة") }
    var cost by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var retail by remember { mutableStateOf(product?.retailPrice?.toString() ?: "") }
    var half by remember { mutableStateOf(product?.halfWholesalePrice?.toString() ?: "") }
    var wholesale by remember { mutableStateOf(product?.wholesalePrice?.toString() ?: "") }
    var minQty by remember { mutableStateOf(product?.minQty?.toString() ?: "0") }
    var opening by remember { mutableStateOf("0") }
    var categoryId by remember { mutableStateOf(product?.categoryId) }
    var showCategories by remember { mutableStateOf(false) }
    val categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "بدون تصنيف"

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.82f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (isNew) "إضافة منتج" else "تعديل المنتج",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
                LabeledField("اسم المنتج *", name, { name = it })
                LabeledField("الباركود", barcode, { barcode = it })
                LabeledField("رمز المنتج SKU", sku, { sku = it })
                LabeledField("الوحدة", unit, { unit = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("سعر التكلفة", cost, { cost = it }, Modifier.weight(1f), numeric = true)
                    LabeledField("سعر القطعة", retail, { retail = it }, Modifier.weight(1f), numeric = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("نصف الجملة", half, { half = it }, Modifier.weight(1f), numeric = true)
                    LabeledField("الجملة", wholesale, { wholesale = it }, Modifier.weight(1f), numeric = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("حد التنبيه", minQty, { minQty = it }, Modifier.weight(1f), numeric = true)
                    if (isNew) {
                        LabeledField("كمية افتتاحية", opening, { opening = it }, Modifier.weight(1f), numeric = true)
                    }
                }
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {},
                    label = { Text("التصنيف") },
                    modifier = Modifier.fillMaxWidth().clickable { showCategories = true },
                    readOnly = true,
                    enabled = false,
                    shape = RoundedCornerShape(12.dp)
                )
                TextButton(onClick = { showCategories = true }) { Text("اختيار التصنيف") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                    Button(
                        onClick = {
                            onSave(
                                ProductEntity(
                                    id = product?.id ?: 0,
                                    projectId = projectId,
                                    name = name.trim(),
                                    barcode = barcode.trim(),
                                    sku = sku.trim(),
                                    categoryId = categoryId,
                                    unit = unit.ifBlank { "قطعة" },
                                    costPrice = Money.parse(cost),
                                    retailPrice = Money.parse(retail),
                                    halfWholesalePrice = Money.parse(half),
                                    wholesalePrice = Money.parse(wholesale),
                                    minQty = Money.parse(minQty),
                                    active = true,
                                    createdAt = product?.createdAt ?: System.currentTimeMillis()
                                ),
                                Money.parse(opening)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank()
                    ) { Text("حفظ") }
                }
                Text("العملة: $currency", fontSize = 11.sp, color = Color(0xFF77736B))
            }
        }
    }

    if (showCategories) {
        AlertDialog(
            onDismissRequest = { showCategories = false },
            title = { Text("التصنيف") },
            text = {
                LazyColumn(Modifier.height(260.dp)) {
                    item {
                        Text(
                            "بدون تصنيف",
                            Modifier.fillMaxWidth().clickable {
                                categoryId = null
                                showCategories = false
                            }.padding(12.dp)
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        Text(
                            category.name,
                            Modifier.fillMaxWidth().clickable {
                                categoryId = category.id
                                showCategories = false
                            }.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

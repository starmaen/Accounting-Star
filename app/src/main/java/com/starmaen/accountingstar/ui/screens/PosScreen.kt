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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.starmaen.accountingstar.data.InvoiceStatus
import com.starmaen.accountingstar.data.PaymentType
import com.starmaen.accountingstar.data.PriceTier
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.CartItem
import com.starmaen.accountingstar.ui.components.DividerLine
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.util.Money
import kotlin.math.max

@Composable
fun PosScreen(vm: AppViewModel, state: AppUiState) {
    val context = LocalContext.current
    val currency = state.currentProject?.currency ?: "ر.س"
    var query by remember { mutableStateOf("") }
    var showCart by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var discount by remember { mutableStateOf("0") }
    var taxRate by remember { mutableStateOf((state.currentProject?.taxRate ?: 0.0).toString()) }
    var paid by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("3") }

    val stockByProduct = remember(state.stockRows) { state.stockRows.associateBy { it.productId } }
    val filtered = remember(state.products, query, state.priceTier) {
        val q = query.trim()
        state.products.filter {
            q.isEmpty() || it.name.contains(q, true) || it.barcode.contains(q) || it.sku.contains(q, true)
        }
    }

    fun addByBarcode() {
        val code = query.trim()
        if (code.isEmpty()) return
        val product = state.products.firstOrNull { it.barcode == code || it.sku == code }
            ?: filtered.firstOrNull()
        if (product != null) {
            vm.addToCart(product)
            query = ""
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFFBF5))) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث بالاسم أو امسح الباركود") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Text),
                keyboardActions = KeyboardActions(onSearch = { addByBarcode() })
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    PriceTier.RETAIL to "قطعة",
                    PriceTier.HALF_WHOLESALE to "نصف جملة",
                    PriceTier.WHOLESALE to "جملة"
                ).forEach { (tier, label) ->
                    FilterChip(
                        selected = state.priceTier == tier,
                        onClick = { vm.setPriceTier(tier) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "لا توجد منتجات مطابقة.\nأضف منتجات من شاشة المنتجات أو عدّل البحث.",
                    color = Color(0xFF77736B),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { product ->
                    val stock = stockByProduct[product.id]
                    val qty = stock?.quantity ?: 0.0
                    Card(
                        modifier = Modifier.clickable { vm.addToCart(product) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.height(36.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Money.format(product.priceFor(state.priceTier), currency),
                                color = BrandGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "المخزون: ${Money.format(qty)} ${product.unit}",
                                fontSize = 10.sp,
                                color = if (qty <= 0) DangerRed else Color(0xFF77736B)
                            )
                        }
                    }
                }
            }
        }

        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("${Money.format(state.cartCount)} وحدة", fontSize = 12.sp, color = Color(0xFF77736B))
                    Text(
                        Money.format(state.cartTotal, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandGreen
                    )
                }
                Button(
                    onClick = { showCart = state.cart.isNotEmpty() },
                    enabled = state.cart.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("إتمام البيع (${state.cart.size})")
                }
            }
        }
    }

    if (showCart) {
        val subtotal = state.cartTotal
        val discountValue = Money.parse(discount)
        val afterDiscount = max(0.0, subtotal - discountValue)
        val taxValue = Money.parse(taxRate)
        val taxAmount = Money.round(afterDiscount * taxValue / 100.0)
        val total = Money.round(afterDiscount + taxAmount)
        val remaining = Money.round(total - if (state.paymentType == PaymentType.CASH) total else Money.parse(paid))

        Dialog(
            onDismissRequest = { showCart = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(Modifier.fillMaxSize(), color = Color(0xFFFFFBF5)) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().background(BrandGreen).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("سلة البيع", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showCart = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.cart, key = { it.product.id }) { item ->
                            CartLineCard(item, currency, vm)
                        }
                        item {
                            Card(shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth().clickable { showCustomerPicker = true },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreen)
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text("العميل", fontSize = 11.sp, color = Color(0xFF77736B))
                                                Text(
                                                    state.selectedCustomer?.name ?: "عميل نقدي",
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Text("تغيير", color = BrandGreen, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text("طريقة الدفع", fontSize = 12.sp, color = Color(0xFF77736B))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            PaymentType.CASH to "نقدي",
                                            PaymentType.CREDIT to "دين",
                                            PaymentType.INSTALLMENT to "تقسيط"
                                        ).forEach { (type, label) ->
                                            FilterChip(
                                                selected = state.paymentType == type,
                                                onClick = { vm.setPaymentType(type) },
                                                label = { Text(label, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                    if (state.paymentType != PaymentType.CASH) {
                                        Spacer(Modifier.height(8.dp))
                                        LabeledField(
                                            "المبلغ المدفوع الآن",
                                            paid,
                                            { paid = it },
                                            numeric = true
                                        )
                                        if (state.paymentType == PaymentType.INSTALLMENT) {
                                            Spacer(Modifier.height(8.dp))
                                            LabeledField(
                                                "عدد الأقساط الشهرية",
                                                installments,
                                                { installments = it },
                                                numeric = true
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LabeledField("الخصم", discount, { discount = it }, Modifier.weight(1f), numeric = true)
                                        LabeledField("الضريبة %", taxRate, { taxRate = it }, Modifier.weight(1f), numeric = true)
                                    }
                                }
                            }
                        }
                    }
                    Surface(color = Color.White, shadowElevation = 10.dp) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المجموع الفرعي", fontSize = 13.sp)
                                Text(Money.format(subtotal, currency), fontWeight = FontWeight.Medium)
                            }
                            if (discountValue > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الخصم", fontSize = 13.sp)
                                    Text("-${Money.format(discountValue, currency)}", color = DangerRed)
                                }
                            }
                            if (taxAmount > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الضريبة", fontSize = 13.sp)
                                    Text(Money.format(taxAmount, currency))
                                }
                            }
                            DividerLine()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الإجمالي", fontWeight = FontWeight.Bold)
                                Text(Money.format(total, currency), fontWeight = FontWeight.Bold, color = BrandGreen, fontSize = 18.sp)
                            }
                            if (state.paymentType != PaymentType.CASH) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المتبقي", fontSize = 13.sp)
                                    Text(
                                        Money.format(max(0.0, remaining), currency),
                                        color = if (remaining > 0) DangerRed else BrandGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    vm.checkout(
                                        paid = if (state.paymentType == PaymentType.CASH) total else Money.parse(paid),
                                        discount = discountValue,
                                        taxRate = taxValue,
                                        installmentCount = installments.toIntOrNull() ?: 0
                                    )
                                    showCart = false
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = state.cart.isNotEmpty()
                            ) {
                                Text("حفظ الفاتورة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("اختيار العميل") },
            text = {
                LazyColumn(Modifier.height(300.dp)) {
                    item {
                        Text(
                            "عميل نقدي (بدون حساب)",
                            Modifier.fillMaxWidth().clickable {
                                vm.setCustomer(null)
                                showCustomerPicker = false
                            }.padding(12.dp),
                            fontWeight = FontWeight.Medium,
                            color = BrandGreen
                        )
                    }
                    items(state.customers, key = { it.id }) { customer ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                vm.setCustomer(customer)
                                showCustomerPicker = false
                            }.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(customer.name, fontWeight = FontWeight.Medium)
                                Text(customer.phone, fontSize = 11.sp, color = Color(0xFF77736B))
                            }
                            if (customer.balance > 0) {
                                Text(Money.format(customer.balance, currency), color = DangerRed, fontSize = 12.sp)
                            }
                        }
                        DividerLine()
                    }
                }
            },
            confirmButton = {}
        )
    }

    state.pendingPrintId?.let { id ->
        AlertDialog(
            onDismissRequest = { vm.consumePendingPrint() },
            title = { Text("تم حفظ الفاتورة") },
            text = { Text("هل تريد طباعة الفاتورة الآن؟ يمكنك أيضاً طباعتها لاحقاً من شاشة الفواتير.") },
            confirmButton = {
                Button(onClick = {
                    vm.consumePendingPrint()
                    vm.printInvoice(context, id)
                }) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("طباعة")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.consumePendingPrint() }) { Text("لاحقاً") }
            }
        )
    }
}

@Composable
private fun CartLineCard(item: CartItem, currency: String, vm: AppViewModel) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.product.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        "${Money.format(item.unitPrice, currency)} × ${Money.format(item.qty)}",
                        fontSize = 11.sp,
                        color = Color(0xFF77736B)
                    )
                }
                IconButton(onClick = { vm.removeFromCart(item.product.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.updateCartQty(item.product.id, item.qty - 1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "تقليل", tint = BrandGreen)
                    }
                    Text(Money.format(item.qty), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { vm.updateCartQty(item.product.id, item.qty + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "زيادة", tint = BrandGreen)
                    }
                }
                Text(
                    Money.format(item.total, currency),
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
            }
        }
    }
}

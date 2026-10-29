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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.starmaen.accountingstar.data.CustomerEntity
import com.starmaen.accountingstar.data.SupplierEntity
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.EmptyState
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.DangerRed
import com.starmaen.accountingstar.ui.theme.SuccessGreen
import com.starmaen.accountingstar.util.Money

@Composable
fun ContactsScreen(vm: AppViewModel, state: AppUiState, startWithCustomers: Boolean) {
    val currency = state.currentProject?.currency ?: "ر.س"
    var showCustomers by remember { mutableStateOf(startWithCustomers) }
    var query by remember { mutableStateOf("") }
    var customerForm by remember { mutableStateOf<CustomerEntity?>(null) }
    var supplierForm by remember { mutableStateOf<SupplierEntity?>(null) }
    var addCustomer by remember { mutableStateOf(false) }
    var addSupplier by remember { mutableStateOf(false) }

    val customers = remember(state.customers, query) {
        state.customers.filter {
            query.isBlank() || it.name.contains(query, true) || it.phone.contains(query)
        }
    }
    val suppliers = remember(state.suppliers, query) {
        state.suppliers.filter {
            query.isBlank() || it.name.contains(query, true) || it.phone.contains(query)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFFBF5))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showCustomers,
                onClick = { showCustomers = true; query = "" },
                label = { Text("العملاء (${state.customers.size})") }
            )
            FilterChip(
                selected = !showCustomers,
                onClick = { showCustomers = false; query = "" },
                label = { Text("الموردون (${state.suppliers.size})") }
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            placeholder = { Text("بحث بالاسم أو الجوال") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.08f))
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text("إجمالي ديون العملاء", fontSize = 11.sp, color = Color(0xFF77736B))
                    Text(Money.format(state.stats.receivables, currency), fontWeight = FontWeight.Bold, color = DangerRed)
                }
            }
            Card(
                Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGold.copy(alpha = 0.12f))
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text("مستحقات الموردين", fontSize = 11.sp, color = Color(0xFF77736B))
                    Text(Money.format(state.stats.payables, currency), fontWeight = FontWeight.Bold, color = Color(0xFF8A6100))
                }
            }
        }
        Button(
            onClick = {
                if (showCustomers) {
                    customerForm = null
                    addCustomer = true
                } else {
                    supplierForm = null
                    addSupplier = true
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (showCustomers) "إضافة عميل جديد" else "إضافة مورد جديد")
        }
        Spacer(Modifier.height(8.dp))
        if (showCustomers) {
            if (customers.isEmpty()) {
                EmptyState("لا يوجد عملاء. أضف عميلاً بالزر +")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        listOf(customer.phone, customer.address).filter { it.isNotBlank() }.joinToString(" • "),
                                        fontSize = 11.sp,
                                        color = Color(0xFF77736B)
                                    )
                                    Text(
                                        if (customer.type == "WHOLESALE") "جملة" else "قطعة",
                                        fontSize = 10.sp,
                                        color = BrandGreen
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        Money.format(customer.balance, currency),
                                        fontWeight = FontWeight.Bold,
                                        color = if (customer.balance > 0) DangerRed else SuccessGreen
                                    )
                                    Text("الرصيد", fontSize = 10.sp, color = Color(0xFF77736B))
                                }
                                IconButton(onClick = { customerForm = customer }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = BrandGreen)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        } else {
            if (suppliers.isEmpty()) {
                EmptyState("لا يوجد موردون. أضف مورداً بالزر +")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suppliers, key = { it.id }) { supplier ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(supplier.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        listOf(supplier.phone, supplier.address).filter { it.isNotBlank() }.joinToString(" • "),
                                        fontSize = 11.sp,
                                        color = Color(0xFF77736B)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        Money.format(supplier.balance, currency),
                                        fontWeight = FontWeight.Bold,
                                        color = if (supplier.balance > 0) Color(0xFF8A6100) else SuccessGreen
                                    )
                                    Text("مستحق له", fontSize = 10.sp, color = Color(0xFF77736B))
                                }
                                IconButton(onClick = { supplierForm = supplier }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = BrandGreen)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (addCustomer) {
        CustomerDialog(
            customer = customerForm,
            projectId = state.currentProject?.id ?: 0,
            onDismiss = { addCustomer = false; customerForm = null }
        ) { customer ->
            vm.saveCustomer(customer)
            addCustomer = false
            customerForm = null
        }
    }

    if (addSupplier) {
        SupplierDialog(
            supplier = supplierForm,
            projectId = state.currentProject?.id ?: 0,
            onDismiss = { addSupplier = false; supplierForm = null }
        ) { supplier ->
            vm.saveSupplier(supplier)
            addSupplier = false
            supplierForm = null
        }
    }

    customerForm?.let { customer ->
        CustomerDialog(
            customer = customer,
            projectId = state.currentProject?.id ?: 0,
            onDismiss = { customerForm = null }
        ) { updated ->
            vm.saveCustomer(updated)
            customerForm = null
        }
    }

    supplierForm?.let { supplier ->
        SupplierDialog(
            supplier = supplier,
            projectId = state.currentProject?.id ?: 0,
            onDismiss = { supplierForm = null }
        ) { updated ->
            vm.saveSupplier(updated)
            supplierForm = null
        }
    }
}

@Composable
private fun CustomerDialog(
    customer: CustomerEntity?,
    projectId: Long,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    val isNew = customer == null
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var type by remember { mutableStateOf(customer?.type ?: "RETAIL") }
    var creditLimit by remember { mutableStateOf(customer?.creditLimit?.toString() ?: "0") }
    var opening by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "إضافة عميل" else "تعديل العميل") },
        text = {
            Column(
                Modifier.height(420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabeledField("الاسم *", name, { name = it })
                LabeledField("الجوال", phone, { phone = it })
                LabeledField("العنوان", address, { address = it })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = type == "RETAIL", onClick = { type = "RETAIL" }, label = { Text("قطعة", fontSize = 11.sp) })
                    FilterChip(selected = type == "WHOLESALE", onClick = { type = "WHOLESALE" }, label = { Text("جملة", fontSize = 11.sp) })
                }
                LabeledField("حد الائتمان", creditLimit, { creditLimit = it }, numeric = true)
                if (isNew) {
                    LabeledField("رصيد افتتاحي (دين سابق)", opening, { opening = it }, numeric = true)
                }
                LabeledField("ملاحظات", notes, { notes = it }, singleLine = false)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        (customer ?: CustomerEntity(projectId = projectId, name = "")).copy(
                            projectId = projectId,
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            type = type,
                            creditLimit = Money.parse(creditLimit),
                            openingBalance = if (isNew) Money.parse(opening) else customer!!.openingBalance,
                            notes = notes.trim(),
                            active = true
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun SupplierDialog(
    supplier: SupplierEntity?,
    projectId: Long,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    val isNew = supplier == null
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var opening by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "إضافة مورد" else "تعديل المورد") },
        text = {
            Column(
                Modifier.height(340.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabeledField("اسم المورد *", name, { name = it })
                LabeledField("الجوال", phone, { phone = it })
                LabeledField("العنوان", address, { address = it })
                if (isNew) {
                    LabeledField("رصيد افتتاحي (مستحق له)", opening, { opening = it }, numeric = true)
                }
                LabeledField("ملاحظات", notes, { notes = it }, singleLine = false)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        (supplier ?: SupplierEntity(projectId = projectId, name = "")).copy(
                            projectId = projectId,
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            openingBalance = if (isNew) Money.parse(opening) else supplier!!.openingBalance,
                            notes = notes.trim(),
                            active = true
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

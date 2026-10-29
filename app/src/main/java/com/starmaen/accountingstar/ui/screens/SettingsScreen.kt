package com.starmaen.accountingstar.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.starmaen.accountingstar.data.Roles
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.DividerLine
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.components.SectionCard
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.SuccessGreen
import com.starmaen.accountingstar.util.Money

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, state: AppUiState) {
    val context = LocalContext.current
    val project = state.currentProject
    var showBranch by remember { mutableStateOf(false) }
    var showWarehouse by remember { mutableStateOf(false) }
    var showUser by remember { mutableStateOf(false) }

    var shopName by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var phone by remember(project?.id) { mutableStateOf(project?.phone ?: "") }
    var address by remember(project?.id) { mutableStateOf(project?.address ?: "") }
    var currency by remember(project?.id) { mutableStateOf(project?.currency ?: "ر.س") }
    var taxRate by remember(project?.id) { mutableStateOf((project?.taxRate ?: 0.0).toString()) }

    var mac by remember { mutableStateOf(vm.session.thermalMac) }
    var paper by remember { mutableStateOf(vm.session.paperWidth) }
    var footer by remember { mutableStateOf(vm.session.receiptFooter) }
    var taxNumber by remember { mutableStateOf(vm.session.taxNumber) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.printTest(context) else vm.message("لم يُمنح إذن البلوتوث")
    }

    fun doTestPrint() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            mac.isNotBlank() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            vm.savePrinterSettings(mac, paper, footer, taxNumber)
            vm.printTest(context)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF5))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard("بيانات النشاط التجاري") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("اسم المتجر", shopName, { shopName = it })
                LabeledField("الجوال", phone, { phone = it })
                LabeledField("العنوان", address, { address = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("العملة", currency, { currency = it }, Modifier.weight(1f))
                    LabeledField("الضريبة %", taxRate, { taxRate = it }, Modifier.weight(1f), numeric = true)
                }
                Button(
                    onClick = {
                        val p = project ?: return@Button
                        vm.updateProject(
                            p.copy(
                                name = shopName.trim().ifBlank { p.name },
                                phone = phone.trim(),
                                address = address.trim(),
                                currency = currency.ifBlank { "ر.س" },
                                taxRate = Money.parse(taxRate)
                            )
                        )
                    },
                    enabled = project != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.padding(3.dp))
                    Text("حفظ بيانات النشاط")
                }
            }
        }

        SectionCard("الفروع والمستودعات") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الفرع الحالي", fontSize = 12.sp, color = Color(0xFF77736B))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.branches.forEach { branch ->
                        FilterChip(
                            selected = branch.id == state.activeBranchId,
                            onClick = { vm.setActiveBranch(branch.id) },
                            label = { Text(branch.name, fontSize = 11.sp) }
                        )
                    }
                }
                Text("المستودع الحالي", fontSize = 12.sp, color = Color(0xFF77736B))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.warehouses.forEach { warehouse ->
                        FilterChip(
                            selected = warehouse.id == state.activeWarehouseId,
                            onClick = { vm.setActiveWarehouse(warehouse.id) },
                            label = { Text(warehouse.name, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showBranch = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("فرع")
                    }
                    OutlinedButton(onClick = { showWarehouse = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("مستودع")
                    }
                }
            }
        }

        SectionCard("الطباعة الحرارية والفواتير") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("عنوان MAC للطابعة (اتركه فارغاً لطباعة النظام/PDF)", mac, { mac = it })
                Text("عرض الورق", fontSize = 12.sp, color = Color(0xFF77736B))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = paper == 58, onClick = { paper = 58 }, label = { Text("58mm", fontSize = 11.sp) })
                    FilterChip(selected = paper == 80, onClick = { paper = 80 }, label = { Text("80mm", fontSize = 11.sp) })
                }
                LabeledField("تذييل الفاتورة", footer, { footer = it })
                LabeledField("الرقم الضريبي (اختياري)", taxNumber, { taxNumber = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.savePrinterSettings(mac, paper, footer, taxNumber) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text("حفظ")
                    }
                    Button(
                        onClick = { doTestPrint() },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandGold)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Text("اختبار")
                    }
                }
                Text(
                    "اربط الطابعة من إعدادات بلوتوث في الجوال أولاً، ثم أدخل عنوان MAC. عند تركه فارغاً ستُطبع الفاتورة عبر خدمة الطباعة في أندرويد (PDF أو طابعة شبكة).",
                    fontSize = 10.sp,
                    color = Color(0xFF8A867D)
                )
            }
        }

        SectionCard(
            "المستخدمون",
            action = {
                if (vm.canManage()) {
                    TextButton(onClick = { showUser = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("إضافة")
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.users.forEach { user ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(user.fullName.ifBlank { user.username }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("@${user.username}", fontSize = 10.sp, color = Color(0xFF77736B))
                        }
                        Text(
                            roleLabel(user.role),
                            fontSize = 11.sp,
                            color = BrandGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    DividerLine()
                }
            }
        }

        SectionCard("النسخ الاحتياطي والبيانات") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "كل البيانات محفوظة محلياً داخل قاعدة بيانات الجهاز. أنشئ نسخة احتياطية دورياً وشاركها إلى التخزين السحابي.",
                    fontSize = 11.sp,
                    color = Color(0xFF615F58)
                )
                Button(onClick = { vm.backup(context) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(Modifier.padding(3.dp))
                    Text("تصدير نسخة احتياطية من قاعدة البيانات")
                }
            }
        }

        SectionCard("حول التطبيق") {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("نجم المحاسبة — الإصدار 1.0", fontWeight = FontWeight.Bold, color = BrandGreen)
                Text("نظام محاسبي ونقاط بيع متكامل يعمل دون إنترنت.", fontSize = 11.sp)
                Text("يدعم: قطعة / نصف جملة / جملة • نقدي / دين / تقسيط • مستودعات وفروع متعددة • مشاريع منفصلة.", fontSize = 11.sp)
                Text("المستخدم الحالي: ${state.currentUser?.fullName} (${roleLabel(state.currentUser?.role ?: "")})", fontSize = 11.sp, color = Color(0xFF77736B))
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showBranch) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBranch = false },
            title = { Text("إضافة فرع") },
            text = { LabeledField("اسم الفرع", name, { name = it }) },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    vm.addBranch(name)
                    showBranch = false
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showBranch = false }) { Text("إلغاء") } }
        )
    }

    if (showWarehouse) {
        var name by remember { mutableStateOf("") }
        var branchId by remember { mutableStateOf(state.activeBranchId) }
        AlertDialog(
            onDismissRequest = { showWarehouse = false },
            title = { Text("إضافة مستودع") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("اسم المستودع", name, { name = it })
                    Text("الفرع", fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.branches.forEach { branch ->
                            FilterChip(
                                selected = branchId == branch.id,
                                onClick = { branchId = branch.id },
                                label = { Text(branch.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    vm.addWarehouse(name, branchId)
                    showWarehouse = false
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showWarehouse = false }) { Text("إلغاء") } }
        )
    }

    if (showUser) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(Roles.CASHIER) }
        AlertDialog(
            onDismissRequest = { showUser = false },
            title = { Text("إضافة مستخدم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("الاسم الكامل", fullName, { fullName = it })
                    LabeledField("اسم المستخدم", username, { username = it })
                    LabeledField("كلمة المرور", password, { password = it })
                    Text("الصلاحية", fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Roles.CASHIER, Roles.MANAGER, Roles.ADMIN).forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(roleLabel(r), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = username.isNotBlank() && password.length >= 4,
                    onClick = {
                        vm.addUser(username, password, fullName, role)
                        showUser = false
                    }
                ) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showUser = false }) { Text("إلغاء") } }
        )
    }
}

fun roleLabel(role: String): String = when (role) {
    Roles.ADMIN -> "مدير نظام"
    Roles.MANAGER -> "مدير"
    else -> "كاشير"
}

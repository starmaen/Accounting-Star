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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.components.LabeledField
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.ui.theme.SuccessGreen
import com.starmaen.accountingstar.util.Money

@Composable
fun ProjectsScreen(vm: AppViewModel, state: AppUiState) {
    var showAdd by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF5))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "يمكنك إنشاء أكثر من نشاط تجاري مستقل (متجر ألبسة، بقالة، مستحضرات تجميل...) وكل مشروع له بياناته ومخزونه وفواتيره وعملاؤه بشكل منفصل تماماً.",
            fontSize = 12.sp,
            color = Color(0xFF615F58)
        )
        state.projects.forEach { project ->
            val active = project.id == state.currentProject?.id
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (active) BrandGreen.copy(alpha = 0.08f) else Color.White
                )
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandGreen)
                    Spacer(Modifier.padding(6.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(project.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (active) {
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "نشط",
                                    tint = SuccessGreen,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        Text(
                            "${businessTypeLabel(project.businessType)} • ${project.currency} • ضريبة ${project.taxRate}%",
                            fontSize = 11.sp,
                            color = Color(0xFF77736B)
                        )
                        if (project.phone.isNotBlank() || project.address.isNotBlank()) {
                            Text(
                                listOf(project.phone, project.address).filter { it.isNotBlank() }.joinToString(" • "),
                                fontSize = 10.sp,
                                color = Color(0xFF8A867D)
                            )
                        }
                    }
                    if (!active) {
                        OutlinedButton(onClick = { vm.selectProject(project.id) }) { Text("فتح") }
                    } else {
                        Text("المشروع الحالي", fontSize = 11.sp, color = SuccessGreen)
                    }
                }
            }
        }
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("إنشاء مشروع جديد")
        }
        Text(
            "ملاحظة: العملات والضرائب تُضبط لكل مشروع على حدة، ويمكنك إضافة فروع ومستودعات متعددة من الإعدادات.",
            fontSize = 11.sp,
            color = Color(0xFF8A867D)
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("GENERAL") }
        var currency by remember { mutableStateOf("ر.س") }
        var tax by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("مشروع تجاري جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("اسم المشروع *", name, { name = it })
                    Text("نوع النشاط", fontSize = 12.sp, color = Color(0xFF77736B))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "GENERAL" to "عام",
                            "CLOTHING" to "ألبسة",
                            "GROCERY" to "بقالة",
                            "COSMETICS" to "تجميل"
                        ).forEach { (t, label) ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                    LabeledField("العملة", currency, { currency = it })
                    LabeledField("نسبة الضريبة %", tax, { tax = it }, numeric = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        vm.addProject(name, type, currency, Money.parse(tax))
                        showAdd = false
                    }
                ) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("إلغاء") } }
        )
    }
}

fun businessTypeLabel(type: String): String = when (type) {
    "CLOTHING" -> "ألبسة"
    "GROCERY" -> "بقالة"
    "COSMETICS" -> "مستحضرات تجميل"
    "MALL" -> "مول / متعدد الأقسام"
    else -> "تجاري عام"
}

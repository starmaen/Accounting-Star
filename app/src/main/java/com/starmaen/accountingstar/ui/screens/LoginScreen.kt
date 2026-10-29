package com.starmaen.accountingstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen

@Composable
fun LoginScreen(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B2E22), BrandGreen, Color(0xFF14503C))
                )
            )
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(92.dp)
                    .background(BrandGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "نجم المحاسبة",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "نظام محاسبي ونقاط بيع متكامل",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "تسجيل الدخول",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (state.message != null) {
                        Text(state.message ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { vm.login(username, password) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                    ) {
                        Text("دخول", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF6F1E7), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "الدخول الأول: admin / admin123 — يمكن تغييره من الإعدادات لاحقاً",
                            fontSize = 11.sp,
                            color = Color(0xFF615F58),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "يعمل دون إنترنت • نسخ احتياطي محلي • دعم الطباعة الحرارية",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

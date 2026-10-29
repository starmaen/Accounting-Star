package com.starmaen.accountingstar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import com.starmaen.accountingstar.util.Money

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = BrandGreen,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF615F58))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = Color(0xFF77736B))
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                action?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = Color(0xFF8A867D))
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text
        )
    )
}

@Composable
fun KeyValueRow(label: String, value: String, bold: Boolean = false, color: Color = Color.Unspecified) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF615F58))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun MoneyText(amount: Double, currency: String, color: Color = BrandGreen) {
    Text(
        Money.format(amount) + " " + currency,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "تأكيد",
    dismissText: String = "إلغاء",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } }
    )
}

@Composable
fun GoldBadge(text: String) {
    Box(
        Modifier
            .background(BrandGold.copy(alpha = 0.18f), RoundedCornerShape(30.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = Color(0xFF6B5300), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PagePadding(content: @Composable () -> Unit) {
    Box(Modifier.padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))) { content() }
}

@Composable
fun IconCircle(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(42.dp)
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun DividerLine() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFEDE8DE))
    )
}

@Composable
fun SmallTag(text: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 12.sp, color = color)
    }
}

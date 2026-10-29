package com.starmaen.accountingstar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection

val BrandGreen = Color(0xFF0F3D2E)
val BrandGreenLight = Color(0xFF1E6B52)
val BrandGold = Color(0xFFD4A72C)
val BrandSand = Color(0xFFF6F1E7)
val DangerRed = Color(0xFFB3261E)
val SuccessGreen = Color(0xFF1B7F4B)

private val colors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8DF),
    onPrimaryContainer = BrandGreen,
    secondary = BrandGold,
    onSecondary = Color(0xFF2C2300),
    secondaryContainer = Color(0xFFF7E8B8),
    onSecondaryContainer = Color(0xFF3B3000),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1C1C18),
    surface = Color.White,
    onSurface = Color(0xFF1C1C18),
    surfaceVariant = BrandSand,
    onSurfaceVariant = Color(0xFF494740),
    error = DangerRed,
    outline = Color(0xFFBDB8AC)
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun AccountingStarTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            content = content
        )
    }
}

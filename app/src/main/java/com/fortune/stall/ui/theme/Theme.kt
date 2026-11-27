package com.fortune.stall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 问心处 · 深空星幕配色（源自 fortune-stall 的 :root CSS 变量） */
object AppColors {
    val Sky0 = Color(0xFF07090F)
    val Sky1 = Color(0xFF10131D)
    val Ink = Color(0xFF141412)
    val Shop = Color(0xFF1C1C22)
    val Shop2 = Color(0xFF232330)
    val Paper = Color(0xFFF4F3EF)
    val Muted = Color(0xFF8A857C)
    val Dim = Color(0xFF5C5952)
    val Lantern = Color(0xFFD9A441)   // 灯笼金
    val Ember = Color(0xFFC96442)     // 余烬红
    val Jade = Color(0xFF8FAE9A)      // 青玉
    val Wind = Color(0xFF9DB4C9)      // 风蓝
    val GoldLine = Color(0x52D9A441)  // rgba(217,164,65,.32)
    val Line = Color(0x14FFFFFF)      // rgba(255,255,255,.08)
}

private val DarkColors = darkColorScheme(
    primary = AppColors.Lantern,
    onPrimary = AppColors.Ink,
    primaryContainer = AppColors.Shop2,
    onPrimaryContainer = AppColors.Paper,
    secondary = AppColors.Jade,
    onSecondary = AppColors.Ink,
    tertiary = AppColors.Ember,
    background = AppColors.Sky0,
    onBackground = AppColors.Paper,
    surface = AppColors.Shop,
    onSurface = AppColors.Paper,
    surfaceVariant = AppColors.Shop2,
    onSurfaceVariant = AppColors.Muted,
    outline = AppColors.GoldLine,
    error = AppColors.Ember
)

@Composable
fun FortuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}

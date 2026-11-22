package com.fortune.stall.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 无衬线：正文；衬线感：标题（用系统 Serif 字体族模拟 fortune-stall 的 --serif） */
private val SansFont = FontFamily.SansSerif
private val SerifFont = FontFamily.Serif

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Normal, fontSize = 44.sp, letterSpacing = 5.sp),
    headlineLarge = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Normal, fontSize = 28.sp, letterSpacing = 3.sp),
    headlineMedium = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Normal, fontSize = 21.sp, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, letterSpacing = 2.sp),
    titleMedium = TextStyle(fontFamily = SansFont, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = SansFont, fontSize = 14.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = SansFont, fontSize = 12.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = SansFont, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = SansFont, fontSize = 15.sp, letterSpacing = 4.sp),
    labelMedium = TextStyle(fontFamily = SansFont, fontSize = 11.5.sp, letterSpacing = 2.sp)
)

package com.fortune.stall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 深空背景 —— 已按要求去掉星空（星星、流星、无限动画），仅保留静态深色渐变底色。
 * 无动画驱动时 Canvas 只在尺寸变化时绘制一次，不再逐帧重绘，降低绘制开销。
 */
@Composable
fun Starfield(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF07090F), Color(0xFF10131D), Color(0xFF141412)),
                startY = 0f,
                endY = size.height
            )
        )
    }
}

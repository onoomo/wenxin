package com.fortune.stall.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/**
 * 深空星幕 —— 复刻 fortune-stall 的 Starfield：
 * 两层星尘缓漂 + 闪烁 + 偶发流星。用 Compose 无限动画驱动，随 Compose 生命周期自动省电（页面不可见时停）。
 */
@Composable
fun Starfield(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "starfield")
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    // 固定种子生成星星，避免重组时闪烁跳变
    val stars = remember {
        val r = Random(20240610)
        (0..110).map {
            Star(
                x = r.nextFloat(), y = r.nextFloat(),
                radius = if (r.nextFloat() < 0.68f) (0.4f + r.nextFloat() * 0.7f) else (0.9f + r.nextFloat() * 1.1f),
                speed = if (r.nextFloat() < 0.68f) (0.004f + r.nextFloat() * 0.008f) else (0.012f + r.nextFloat() * 0.02f),
                alpha = 0.25f + r.nextFloat() * 0.6f,
                twinkle = 1.5f + r.nextFloat() * 3.5f,
                phase = r.nextFloat() * 7f,
                far = r.nextFloat() < 0.68f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // 背景渐变：深空三段色
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF07090F), Color(0xFF10131D), Color(0xFF141412)),
                startY = 0f, endY = h
            )
        )
        val phase = t * 60f  // 秒
        stars.forEach { s ->
            val driftX = ((s.x - phase * s.speed) % 1f + 1f) % 1f
            val tw = 0.72f + 0.28f * kotlin.math.sin(phase * (6.28f / s.twinkle) + s.phase)
            drawCircle(
                color = if (s.far) Color(0xFFC9CFDD) else Color(0xFFF4F3EF),
                radius = s.radius,
                center = Offset(driftX * w, s.y * h),
                alpha = s.alpha * tw
            )
        }
        // 流星：一个周期里出现两次，斜向划过
        drawMeteor(phase, w, h, offset = 0f)
        drawMeteor(phase, w, h, offset = 31f)
    }
}

private data class Star(
    val x: Float, val y: Float, val radius: Float, val speed: Float,
    val alpha: Float, val twinkle: Float, val phase: Float, val far: Boolean
)

private fun DrawScope.drawMeteor(phase: Float, w: Float, h: Float, offset: Float) {
    val cycle = 26f
    val local = ((phase + offset) % cycle)
    if (local > 1.4f) return  // 只在短暂窗口出现
    val progress = local / 1.4f
    val startX = w * 0.85f - progress * w * 0.5f
    val startY = h * 0.12f + progress * h * 0.28f
    val tailLen = 120f
    val alpha = (1f - progress) * 0.8f
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color(0x00F4F3EF), Color(0xFFF4F3EF)),
            start = Offset(startX - tailLen, startY - tailLen * 0.45f),
            end = Offset(startX, startY)
        ),
        start = Offset(startX - tailLen, startY - tailLen * 0.45f),
        end = Offset(startX, startY),
        strokeWidth = 1.4f,
        alpha = alpha
    )
}

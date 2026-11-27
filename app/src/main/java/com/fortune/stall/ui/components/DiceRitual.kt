package com.fortune.stall.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 骰子点数 → 圆点相对坐标（0..2 网格） */
private val PIP_XY = mapOf(
    1 to listOf(1 to 1),
    2 to listOf(0 to 0, 2 to 2),
    3 to listOf(0 to 0, 1 to 1, 2 to 2),
    4 to listOf(0 to 0, 0 to 2, 2 to 0, 2 to 2),
    5 to listOf(0 to 0, 0 to 2, 1 to 1, 2 to 0, 2 to 2),
    6 to listOf(0 to 0, 0 to 2, 1 to 0, 1 to 2, 2 to 0, 2 to 2)
)

/**
 * 单枚骰子。pips=null 表示摇动中（显示 ? 并抖动），否则显示对应点数。
 */
@Composable
fun Die(pips: Int?, rolling: Boolean, modifier: Modifier = Modifier) {
    val shake = rememberInfiniteTransition(label = "die")
    val angle by shake.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Restart),
        label = "shake"
    )
    Canvas(modifier = modifier.size(62.dp)) {
        val s = size.width
        val rad = s * 0.18f
        // 骰子底色（象牙白渐变 + 阴影）
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(0xFFF7F6F2), Color(0xFFDDD9D0)),
                start = Offset(0f, 0f), end = Offset(s, s)
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(rad),
            alpha = 1f
        )
        if (pips == null || rolling) {
            // 摇动中：轻微位移
            val dx = kotlin.math.sin(angle * Math.PI.toFloat() * 4f) * 3f
            val dy = kotlin.math.cos(angle * Math.PI.toFloat() * 4f) * 2f
            // 问号用中心小点阵近似：画一个「?」的意象——中心菱形
            drawCircle(Color(0xFF8A857C), radius = s * 0.06f, center = center + androidx.compose.ui.geometry.Offset(dx, dy))
            drawCircle(Color(0xFF8A857C), radius = s * 0.03f, center = center + androidx.compose.ui.geometry.Offset(dx, dy + s * 0.16f))
        } else {
            PIP_XY[pips]?.forEach { (gx, gy) ->
                val cx = s * (0.24f + gx * 0.26f)
                val cy = s * (0.24f + gy * 0.26f)
                drawCircle(Color(0xFF23211E), radius = s * 0.075f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
        }
    }
}

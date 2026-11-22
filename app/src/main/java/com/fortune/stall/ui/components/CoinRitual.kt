package com.fortune.stall.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 方孔铜钱：外圆内方。背=浅铜素面，字=深铜铸「元亨利貞」。
 * zi=true 表示字面（有字），false 表示背面（素面）。
 */
@Composable
fun Coin(zi: Boolean, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.size(36.dp)) {
        val s = size.width
        val c = Offset(s / 2, s / 2)
        val outer = s * 0.46f
        val body = if (zi) Brush.radialGradient(
            listOf(Color(0xFF7D6647), Color(0xFF5A4732), Color(0xFF2F2415)),
            center = Offset(s * 0.32f, s * 0.28f), radius = s
        ) else Brush.radialGradient(
            listOf(Color(0xFFECD08A), Color(0xFFCAA25C), Color(0xFF715428)),
            center = Offset(s * 0.32f, s * 0.28f), radius = s
        )
        drawCircle(body, radius = outer, center = c)
        drawCircle(
            Color(0x55_46_32_20), radius = outer, center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
        val hole = s * 0.11f
        drawRect(Color(0xFF0B0C11), topLeft = Offset(c.x - hole, c.y - hole), size = Size(hole * 2, hole * 2))
        if (zi) {
            val glyphs = listOf(
                "元" to Offset(c.x, c.y - outer * 0.58f),
                "亨" to Offset(c.x, c.y + outer * 0.58f),
                "利" to Offset(c.x + outer * 0.58f, c.y),
                "貞" to Offset(c.x - outer * 0.58f, c.y)
            )
            glyphs.forEach { (g, pos) ->
                val layout = textMeasurer.measure(g, TextStyle(fontSize = 8.sp, color = Color(0xFFE6D1A0)))
                drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2, pos.y - layout.size.height / 2))
            }
        }
    }
}

@Composable
private fun YaoLine(solid: Boolean, moving: Boolean) {
    Canvas(modifier = Modifier.size(width = 84.dp, height = 14.dp)) {
        val w = size.width; val h = size.height
        val color = Color(0xFFF4F3EF)
        if (solid) {
            drawRect(color, Offset(0f, h * 0.36f), Size(w, h * 0.26f))
            if (moving) drawCircle(Color(0xFFD9A441), radius = 3.5f, center = Offset(w + 9f, h / 2))
        } else {
            val gap = w * 0.1f
            drawRect(color, Offset(0f, h * 0.36f), Size(w / 2 - gap, h * 0.26f))
            drawRect(color, Offset(w / 2 + gap, h * 0.36f), Size(w / 2 - gap, h * 0.26f))
            if (moving) drawCircle(Color(0xFFC96442), radius = 3.5f, center = Offset(w + 9f, h / 2))
        }
    }
}

/** 爻象符号：老阳○动 / 少阳 / 少阴 / 老阴×动 */
@Composable
fun YaoGlyph(value: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        when (value) {
            9 -> YaoLine(true, true)
            7 -> YaoLine(true, false)
            8 -> YaoLine(false, false)
            6 -> YaoLine(false, true)
        }
    }
}

package com.fortune.stall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.Tarot
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.TarotImageLoader
import com.fortune.stall.util.TarotSettings

/**
 * 塔罗牌：点击切换牌面/牌背（无动画，直接显示）。
 * 牌背为星幕同源的 Canvas 绘制；牌面受「显示塔罗牌面」开关控制，
 * 从本地缓存读取（需在设置里点「下载塔罗牌」下载过），未下载时用文字牌面兜底。
 */
@Composable
fun TarotCard(
    drawn: Tarot.Drawn,
    flipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp
) {
    val height = width * 1.62f
    Box(
        modifier = modifier
            .size(width, height)
            .clickable(onClick = onFlip)
    ) {
        if (flipped) {
            // 逆位牌整体旋转 180°；无翻牌动画，直接呈现
            CardFace(drawn, Modifier.graphicsLayer {
                rotationZ = if (drawn.reversed) 180f else 0f
            })
        } else {
            CardBack(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CardBack(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.linearGradient(
            colors = listOf(Color(0xFF12162A), Color(0xFF0A0C16), Color(0xFF0E1120)),
            start = Offset(0f, 0f), end = Offset(w, h)
        ))
        drawRect(
            Color(0x38D9A441),
            topLeft = Offset(7f, 7f),
            size = androidx.compose.ui.geometry.Size(w - 14f, h - 14f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
        val r = kotlin.random.Random(77)
        repeat(14) {
            drawCircle(Color(0xAAF4F3EF), radius = r.nextFloat() * 1.2f + 0.4f,
                center = Offset(r.nextFloat() * w, r.nextFloat() * h))
        }
        // 中心星芒（四向光芒线 + 中心点）
        val c = Offset(w / 2, h / 2)
        val R = kotlin.math.min(w, h) * 0.32f
        val gold = Color(0x8CD9A441)
        drawLine(gold, Offset(c.x, c.y - R), Offset(c.x, c.y + R), strokeWidth = 1.5f)
        drawLine(gold, Offset(c.x - R * 0.55f, c.y - R * 0.55f), Offset(c.x + R * 0.55f, c.y + R * 0.55f), strokeWidth = 1.2f)
        drawLine(gold, Offset(c.x - R, c.y), Offset(c.x + R, c.y), strokeWidth = 1.5f)
        drawLine(gold, Offset(c.x - R * 0.55f, c.y + R * 0.55f), Offset(c.x + R * 0.55f, c.y - R * 0.55f), strokeWidth = 1.2f)
        drawCircle(gold, radius = R * 0.18f, center = c)
    }
}

@Composable
private fun CardFace(drawn: Tarot.Drawn, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val showImage = TarotSettings.showCardImage.value
    // 只读本地缓存，不联网；缓存版本变化时刷新（下载完成后版本号+1）
    val version = TarotSettings.imageVersion.value
    val bitmap = remember(drawn.card.name, showImage, version) {
        if (showImage) TarotImageLoader.loadCached(context, drawn.card) else null
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = drawn.card.cn,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            CardFaceFallback(drawn)
        }
    }
}

@Composable
private fun CardFaceFallback(drawn: Tarot.Drawn, loading: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(Brush.verticalGradient(
                colors = listOf(Color(0xFF191C28), Color(0xFF111420), Color(0xFF141828))
            ))
            drawRect(
                Color(0x66D9A441),
                topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                drawn.card.number.ifEmpty { "✦" },
                color = AppColors.Lantern, fontSize = 20.sp, fontFamily = FontFamily.Serif
            )
            Text(
                drawn.card.cn,
                color = AppColors.Paper, fontSize = 12.sp, fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                if (loading) "取牌中…" else if (drawn.reversed) "逆 位" else "正 位",
                color = AppColors.Muted, fontSize = 9.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

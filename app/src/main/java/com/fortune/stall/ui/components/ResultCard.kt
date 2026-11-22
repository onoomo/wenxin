package com.fortune.stall.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.FortuneEngine
import com.fortune.stall.engine.Tarot
import com.fortune.stall.ui.theme.AppColors

/** 结果卡片：印章落款 + 塔罗牌阵(可翻) + 卦面全文折叠 + 复制 */
@Composable
fun ResultCard(output: FortuneEngine.CastOutput, onReset: () -> Unit) {
    val context = LocalContext.current
    var expanded = remember { mutableStateOf(false) }
    val firstLine = output.face.lines().firstOrNull()?.trim() ?: ""
    val warn = Regex("⚠[^\\n]*").find(output.face)?.value
    val cards = remember(output) { parseTarotCards(output.extras["cards"]) }
    val xlrCells = remember(output) {
        if (output.method == FortuneEngine.Method.XIAOLIUREN) parseXlrCells(output.extras["xlrGongs"]) else null
    }
    var pickedCell by remember { mutableStateOf<XlrCell?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xCC1E1C1A), Color(0xDD161514))))
    ) {
        Box(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)) {
            Text(firstLine, color = AppColors.Lantern, fontSize = 12.5.sp, lineHeight = 20.sp)
        }
        if (cards != null && cards.isNotEmpty()) {
            TarotSpread(cards, Modifier.padding(vertical = 8.dp))
        }
        // 小六壬：jishiyu 风格三宫格（白底黑线，点宫弹术语）
        if (xlrCells != null && xlrCells.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                XlrGrid(xlrCells) { pickedCell = it }
            }
        }
        if (warn != null) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0x33C96442)).padding(8.dp)
            ) { Text(warn, color = AppColors.Ember, fontSize = 12.sp) }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded.value = !expanded.value }
                .animateContentSize()
        ) {
            Text("卦 面 全 文", color = AppColors.Muted, fontSize = 10.5.sp, letterSpacing = 4.sp)
            Text(
                output.face, color = AppColors.Paper, fontSize = 12.5.sp, lineHeight = 22.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = if (expanded.value) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text("点开 / 收起", color = AppColors.Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionButton("复制卦面 · 拿去给 AI 断", gold = true, modifier = Modifier.weight(1.2f)) {
                copyText(context, output.face)
                Toast.makeText(context, "卦面已复制——拿去给你的 AI 断吧", Toast.LENGTH_SHORT).show()
            }
            ActionButton("再问一事", gold = false, modifier = Modifier.weight(0.8f), onClick = onReset)
        }
    }
    pickedCell?.let { XlrExplainDialog(it) { pickedCell = null } }
}

/** 点宫弹出的术语解释（jishiyu 口径） */
@Composable
private fun XlrExplainDialog(cell: XlrCell, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text("${cell.position}宫 · ${cell.gong}", color = AppColors.Lantern, fontSize = 17.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            val rows = listOf(
                "吉凶" to cell.jixiong, "五行" to cell.wuxing, "六神" to cell.shen,
                "五星" to cell.xing, "地支" to "${cell.zhi}(${cell.zhiWx})", "六亲" to cell.liuqin
            )
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(k, color = AppColors.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(v, color = AppColors.Paper, fontSize = 13.sp, modifier = Modifier.weight(2f))
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, gold: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(if (gold) Color(0x1AD9A441) else Color(0x10FFFFFF))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (gold) AppColors.Lantern else AppColors.Paper, fontSize = 11.5.sp, textAlign = TextAlign.Center)
    }
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("卦面", text))
}

private fun parseTarotCards(raw: String?): List<Tarot.Drawn>? {
    if (raw.isNullOrBlank()) return null
    return try {
        val list = raw.split("|").mapNotNull { seg ->
            val p = seg.split(";")
            if (p.size >= 7) {
                Tarot.Drawn(
                    card = Tarot.Card(
                        name = p[1], cn = p[0], number = p[2],
                        type = if (p[6] == "1") "Major" else "Minor",
                        suitCn = p[3].ifEmpty { null },
                        suit = p.getOrNull(7)?.ifEmpty { null },
                        rank = p.getOrNull(8)?.ifEmpty { null }
                    ),
                    reversed = p[4] == "1",
                    position = p[5]
                )
            } else null
        }
        list.ifEmpty { null }
    } catch (e: Exception) { null }
}

@Composable
private fun TarotSpread(cards: List<Tarot.Drawn>, modifier: Modifier = Modifier) {
    // 默认翻开显示牌面图片；点击可翻回牌背再翻开
    val flipped = remember(cards) { Array(cards.size) { mutableStateOf(true) } }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (cards.size == 10) {
            CelticCross(cards, flipped)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                cards.forEachIndexed { i, c ->
                    TarotCard(c, flipped[i].value, { flipped[i].value = !flipped[i].value })
                }
            }
        }
    }
}

@Composable
private fun CelticCross(cards: List<Tarot.Drawn>, flipped: Array<androidx.compose.runtime.MutableState<Boolean>>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(230.dp)) {
            Box(Modifier.align(Alignment.Center)) { TarotCard(cards[0], flipped[0].value, { flipped[0].value = !flipped[0].value }) }
            Box(Modifier.align(Alignment.Center).graphicsLayer { rotationZ = 90f }) { TarotCard(cards[1], flipped[1].value, { flipped[1].value = !flipped[1].value }) }
            Box(Modifier.align(Alignment.CenterStart)) { TarotCard(cards[2], flipped[2].value, { flipped[2].value = !flipped[2].value }) }
            Box(Modifier.align(Alignment.CenterEnd)) { TarotCard(cards[5], flipped[5].value, { flipped[5].value = !flipped[5].value }) }
            Box(Modifier.align(Alignment.TopCenter)) { TarotCard(cards[4], flipped[4].value, { flipped[4].value = !flipped[4].value }) }
            Box(Modifier.align(Alignment.BottomCenter)) { TarotCard(cards[3], flipped[3].value, { flipped[3].value = !flipped[3].value }) }
        }
        Spacer(Modifier.height(18.dp))
        FlowRow(horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 6..9) TarotCard(cards[i], flipped[i].value, { flipped[i].value = !flipped[i].value })
        }
    }
}

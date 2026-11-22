package com.fortune.stall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// jishiyu 吉时雨 · 小六壬宫格视觉规范（纯 Compose 还原，非 HTML）
private val THEME = Color(0xFFD12824)          // --theme-color
private val GRID_LINE = Color(0xFF000000)      // 黑框
private val GONG_BG = Color(0xFFFFFFFF)        // 白底
private val MARK_GRAY = Color(0xFF999999)      // 打标灰

// 五行色（jishiyu --jin/--shui/--mu/--huo/--tu）
private val WX = mapOf(
    "金" to Color(0xFFFFA500), "水" to Color(0xFF0074E4), "木" to Color(0xFF00A879),
    "火" to Color(0xFFED4D49), "土" to Color(0xFFA64B00)
)
private fun wx(w: String) = WX[w] ?: Color(0xFF4B3521)

/** 单个宫格数据（从 extras 解析） */
data class XlrCell(
    val gong: String, val position: String, val shen: String, val xing: String,
    val zhi: String, val zhiWx: String, val liuqin: String, val wuxing: String,
    val jixiong: String, val mark: String
)

/**
 * jishiyu 风格小六壬宫格：三宫（天/地/人）横排。
 * 每宫内部 2列×4行：六神|五星 · 地支(五行)|六亲 · 宫名标签|打标 · 底部五行色带。
 */
@Composable
fun XlrGrid(cells: List<XlrCell>, onCell: (XlrCell) -> Unit) {
    // 白色底 + 细黑框整体容器
    Row(Modifier.fillMaxWidth().background(GONG_BG).border(1.dp, GRID_LINE)) {
        cells.forEachIndexed { i, c ->
            if (i > 0) Box(Modifier.width(1.dp).height(148.dp).background(GRID_LINE))
            XlrCellCard(c, Modifier.weight(1f), onCell)
        }
    }
}

@Composable
private fun XlrCellCard(cell: XlrCell, modifier: Modifier, onCell: (XlrCell) -> Unit) {
    Column(modifier.height(148.dp).background(GONG_BG).clickable { onCell(cell) }) {
        // 行1：六神（左） / 五星（右，五行色）
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp, vertical = 4.dp)) {
            Text(cell.shen, color = Color(0xFF222222), fontSize = 13.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(cell.xing, color = wx(cell.wuxing), fontSize = 13.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        // 行2：地支(五行)（左，五行色） / 六亲（右）
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp, vertical = 2.dp)) {
            Text("${cell.zhi}(${cell.zhiWx})", color = wx(cell.zhiWx), fontSize = 14.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(cell.liuqin, color = Color(0xFF222222), fontSize = 13.sp, fontFamily = FontFamily.Serif,
                textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        // 行3：宫名标签（左，主题色描边圆角） / 打标（右，灰）
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.border(1.dp, THEME, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                Text(cell.gong, color = THEME, fontSize = 13.sp, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text(cell.mark, color = MARK_GRAY, fontSize = 11.sp, textAlign = TextAlign.End)
        }
        // 行4：底部五行色带
        Box(Modifier.fillMaxWidth().height(10.dp).background(wx(cell.wuxing)))
    }
}

/** 解析 extras["xlrGongs"]：字段 ; 分隔，宫 | 分隔 */
fun parseXlrCells(raw: String?): List<XlrCell>? {
    if (raw.isNullOrBlank()) return null
    return try {
        raw.split("|").mapNotNull { seg ->
            val p = seg.split(";")
            if (p.size < 8) null else XlrCell(
                gong = p[0], position = p[1], shen = p[2], xing = p[3], zhi = p[4],
                zhiWx = p[5], liuqin = p[6], wuxing = p[7],
                jixiong = if (p.size > 8) p[8] else "", mark = if (p.size > 9) p[9] else p[1]
            )
        }.ifEmpty { null }
    } catch (e: Exception) { null }
}

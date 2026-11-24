package com.fortune.stall.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.jishiyu.Taiyi72
import kotlin.math.cos
import kotlin.math.sin

/**
 * 太乙神数 · 传统圆形盘（1:1 仿参考图）。
 * 结构（自中心向外）：
 *   ① 中心空白圆
 *   ② 内环 8 格：九宫数 二七六一八三四九（顺时针自正上：午坤酉乾子艮卯巽）
 *   ③ 中环 16 格：地支+四维（顺时针自正上：午未坤申酉戌乾亥子丑艮寅卯辰巽巳）
 *   ④ 外环 16 格：太乙十六神固定名（地主…大义，与 POSITIONS 对应）
 *   ⑤ 最外：17 星将落点（带颜色：太乙绿 / 文昌黄 / 始击红 / 其余黑）
 *
 * 说明：参考图为"上南下北"，故正上方=午位。以 16 方位等分，每格 22.5°。
 */
private val TY_POS16 = listOf("午", "未", "坤", "申", "酉", "戌", "乾", "亥", "子", "丑", "艮", "寅", "卯", "辰", "巽", "巳")
// 十六神：按 Taiyi72.POSITIONS(子丑艮寅卯辰巽巳午未坤申酉戌乾亥) 顺序，映射到 TY_POS16
private val TY_GOD_BY_POS = mapOf(
    "子" to "地主", "丑" to "阳德", "艮" to "和德", "寅" to "吕申",
    "卯" to "高丛", "辰" to "太阳", "巽" to "大炅", "巳" to "大神",
    "午" to "大威", "未" to "天道", "坤" to "大武", "申" to "武德",
    "酉" to "太簇", "戌" to "阴主", "亥" to "阴德", "乾" to "大义"
)
// 九宫数落位：1乾 2午 3艮 4卯 5中 6酉 7坤 8子 9巽（取八宫，去掉中5）
private val TY_NUM_BY_POS = mapOf(
    "午" to "二", "坤" to "七", "酉" to "六", "乾" to "一",
    "子" to "八", "艮" to "三", "卯" to "四", "巽" to "九"
)
private val TY_INK = Color(0xFF2A2620)
private val TY_LINE = Color(0xFF555555)
private val TY_GREEN = Color(0xFF2E7D32)
private val TY_YELLOW = Color(0xFFC8912A)
private val TY_RED = Color(0xFFC0392B)

@Composable
fun TaiyiRoundBoard(r: Taiyi72.Result, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val sizeDp = 340.dp
    Box(modifier.fillMaxWidth().height(sizeDp + 16.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(sizeDp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val R = minOf(w, h) / 2f * 0.97f
            // 半径分层
            val rCenter = R * 0.20f   // 中心空白圆
            val rNumOut = R * 0.40f   // 内环数字外径
            val rZhiOut = R * 0.64f   // 中环地支外径
            val rGodOut = R * 0.86f   // 外环十六神外径
            val rLand = R * 0.93f     // 星将落点半径

            // ---- 同心圆 ----
            for (rr in listOf(rCenter, rNumOut, rZhiOut, rGodOut, R)) {
                drawCircle(color = TY_LINE, radius = rr, center = Offset(cx, cy), style = Stroke(width = 1.6f))
            }
            // ---- 16 放射线（贯穿数字环外到最外）----
            // 使数字环居中于方位格：数字环8格对应偶数位，放射线仍按16格
            for (i in 0 until 16) {
                val ang = Math.toRadians(i * 22.5 - 90.0)
                val dx = cos(ang).toFloat(); val dy = sin(ang).toFloat()
                drawLine(color = TY_LINE, start = Offset(cx + dx * rCenter, cy + dy * rCenter),
                    end = Offset(cx + dx * R, cy + dy * R), strokeWidth = 1.4f)
            }
            // ---- 数字环 8 格：位于偶数方位格中心（午坤酉乾子艮卯巽）----
            // 正上=午=索引0，每隔2格。方位格中心角 = (i+0.5)*22.5 - 90 会偏；参考图数字正对放射线区间中点
            for (i in 0 until 16 step 2) {
                val pos = TY_POS16[i]
                val num = TY_NUM_BY_POS[pos] ?: continue
                val ang = Math.toRadians((i) * 22.5 - 90.0)
                val rr = (rCenter + rNumOut) / 2f
                val x = cx + cos(ang).toFloat() * rr
                val y = cy + sin(ang).toFloat() * rr
                val tr = measurer.measure(num, style = TextStyle(fontSize = 19.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = TY_INK))
                drawText(textMeasurer = measurer, text = num, topLeft = Offset(x - tr.size.width / 2f, y - tr.size.height / 2f),
                    style = TextStyle(fontSize = 19.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = TY_INK))
            }
            // ---- 中环 16 地支 ----
            for (i in 0 until 16) {
                val pos = TY_POS16[i]
                val ang = Math.toRadians(i * 22.5 - 90.0)
                val rr = (rNumOut + rZhiOut) / 2f
                val x = cx + cos(ang).toFloat() * rr
                val y = cy + sin(ang).toFloat() * rr
                val color = if (pos in TY_NUM_BY_POS) TY_RED else TY_INK // 八宫方位地支标红（参考图）
                val tr = measurer.measure(pos, style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = color))
                drawText(textMeasurer = measurer, text = pos, topLeft = Offset(x - tr.size.width / 2f, y - tr.size.height / 2f),
                    style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = color))
            }
            // ---- 外环 16 神 ----
            for (i in 0 until 16) {
                val pos = TY_POS16[i]
                val god = TY_GOD_BY_POS[pos] ?: ""
                val ang = Math.toRadians(i * 22.5 - 90.0)
                val rr = (rZhiOut + rGodOut) / 2f
                val x = cx + cos(ang).toFloat() * rr
                val y = cy + sin(ang).toFloat() * rr
                val tr = measurer.measure(god, style = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.Serif, color = TY_INK))
                drawText(textMeasurer = measurer, text = god, topLeft = Offset(x - tr.size.width / 2f, y - tr.size.height / 2f),
                    style = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.Serif, color = TY_INK))
            }
            // ---- 最外 17 星将落点：同一方位多星纵向排开 ----
            val byPos = r.starLandings.groupBy { it.position }
            for (i in 0 until 16) {
                val pos = TY_POS16[i]
                val list = byPos[pos] ?: continue
                val ang = Math.toRadians(i * 22.5 - 90.0)
                // 方位越靠上/下，文字行越需收拢；以径向基线 + 切向微移实现多行
                val nx = cos(ang).toFloat(); val ny = sin(ang).toFloat()
                // 切向单位向量（垂直径向）
                val tx = -ny; val ty = nx
                val n = list.size
                list.forEachIndexed { k, s ->
                    val color = when (s.tone) { 1 -> TY_GREEN; 2 -> TY_YELLOW; 3 -> TY_RED; else -> TY_INK }
                    // 径向位置随行数略微外扩，切向散开
                    val radial = rGodOut + (rLand - rGodOut) * (0.35f + 0.65f * (k + 1) / n)
                    val tangentOffset = (k - (n - 1) / 2f) * 13.dp.toPx()
                    val x = cx + nx * radial + tx * tangentOffset
                    val y = cy + ny * radial + ty * tangentOffset
                    val fs = 11.sp
                    val tr = measurer.measure(s.name, style = TextStyle(fontSize = fs, fontFamily = FontFamily.Serif, color = color))
                    drawText(textMeasurer = measurer, text = s.name, topLeft = Offset(x - tr.size.width / 2f, y - tr.size.height / 2f),
                        style = TextStyle(fontSize = fs, fontFamily = FontFamily.Serif, color = color))
                }
            }
        }
    }
}

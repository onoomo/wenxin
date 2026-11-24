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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.jishiyu.Taiyi72
import kotlin.math.cos
import kotlin.math.sin

/**
 * 太乙神数 · 传统圆形盘（仿参考图）。
 * 由内到外严格五层：心(空圆) → 8 格(九宫数) → 16 格(地支) → 16 格(十六神) → 16 格(星将)。
 * 16 条放射线落在相邻文字之间的分界，所有文字端正落在格子中央、不压线。
 * 星将竖排（字沿半径方向堆叠），同格多将沿切向并排，不越界、不重叠。
 * 正上方 = 午位（上南下北）；16 方位每格 22.5°，九宫数取其中 8 个偶数位、每 45°。
 */
private val TY_POS16 = listOf("午", "未", "坤", "申", "酉", "戌", "乾", "亥", "子", "丑", "艮", "寅", "卯", "辰", "巽", "巳")
private val TY_GOD_BY_POS = mapOf(
    "午" to "大威", "未" to "天道", "坤" to "大武", "申" to "武德",
    "酉" to "太簇", "戌" to "阴主", "乾" to "大义", "亥" to "阴德",
    "子" to "地主", "丑" to "阳德", "艮" to "和德", "寅" to "吕申",
    "卯" to "高从", "辰" to "太阳", "巽" to "大炅", "巳" to "大神"
)
// 九宫数：1乾 2午 3艮 4卯 5中 6酉 7坤 8子 9巽（去中5，取八宫）
private val TY_NUM_BY_POS = mapOf(
    "午" to "二", "坤" to "七", "酉" to "六", "乾" to "一",
    "子" to "八", "艮" to "三", "卯" to "四", "巽" to "九"
)
private val TY_INK = Color(0xFF2A2620)
private val TY_LINE = Color(0xFF66605A)
private val TY_GREEN = Color(0xFF2E7D32)
private val TY_YELLOW = Color(0xFFC8912A)
private val TY_RED = Color(0xFFC0392B)

@Composable
fun TaiyiRoundBoard(r: Taiyi72.Result, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val sizeDp = 360.dp
    Box(modifier.fillMaxWidth().height(sizeDp + 16.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(sizeDp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val R = minOf(size.width, size.height) / 2f * 0.985f

            // 五层半径：心 / 8格 / 16地支 / 16神 / 16星将
            val rCenter = R * 0.17f
            val rNumOut = R * 0.38f
            val rZhiOut = R * 0.58f
            val rGodOut = R * 0.78f
            val rStar = (rGodOut + R) / 2f   // 星将文字所在圆弧（固定半径，同格沿切向排开）

            // ---- 同心圆 ----
            for (rr in listOf(rCenter, rNumOut, rZhiOut, rGodOut, R)) {
                drawCircle(color = TY_LINE, radius = rr, center = Offset(cx, cy), style = Stroke(width = 1.5f))
            }
            // ---- 16 条放射线：落在相邻文字之间的分界，不穿过文字中心 ----
            for (i in 0 until 16) {
                val ang = Math.toRadians(i * 22.5 + 11.25 - 90.0)
                val dx = cos(ang).toFloat(); val dy = sin(ang).toFloat()
                drawLine(color = TY_LINE,
                    start = Offset(cx + dx * rCenter, cy + dy * rCenter),
                    end = Offset(cx + dx * R, cy + dy * R), strokeWidth = 1.3f)
            }

            // 工具：在角度/半径处居中绘制文字
            fun drawAt(text: String, angDeg: Double, radius: Float, fs: TextUnit,
                       color: Color, bold: Boolean = false) {
                val a = Math.toRadians(angDeg)
                val x = cx + cos(a).toFloat() * radius
                val y = cy + sin(a).toFloat() * radius
                val style = TextStyle(fontSize = fs, fontFamily = FontFamily.Serif,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color)
                val tr = measurer.measure(text, style = style)
                drawText(textMeasurer = measurer, text = text,
                    topLeft = Offset(x - tr.size.width / 2f, y - tr.size.height / 2f), style = style)
            }

            // ② 8 格九宫数：偶数方位，每 45°
            for (k in 0 until 8) {
                val pos = TY_POS16[k * 2]
                val num = TY_NUM_BY_POS[pos] ?: continue
                drawAt(num, k * 45.0 - 90.0, (rCenter + rNumOut) / 2f, 19.sp, TY_INK, bold = true)
            }
            // ③ 16 格地支（八宫方位标红）
            for (i in 0 until 16) {
                val pos = TY_POS16[i]
                val color = if (pos in TY_NUM_BY_POS) TY_RED else TY_INK
                drawAt(pos, i * 22.5 - 90.0, (rNumOut + rZhiOut) / 2f, 16.sp, color, bold = true)
            }
            // ④ 16 格太乙神
            for (i in 0 until 16) {
                val god = TY_GOD_BY_POS[TY_POS16[i]] ?: continue
                drawAt(god, i * 22.5 - 90.0, (rZhiOut + rGodOut) / 2f, 15.sp, TY_INK)
            }
            // ⑤ 16 格星将：每个将名竖排（字沿半径方向堆叠），同格多将沿切向并排，全部留在格外环内、不越界
            val byPos = r.starLandings.groupBy { if (it.position == "中") "午" else it.position }
            for (i in 0 until 16) {
                val pos = TY_POS16[i]
                val list = byPos[pos] ?: continue
                val angDeg = i * 22.5 - 90.0   // 字仍在格子中央角度（线已移到分界）
                val ang = Math.toRadians(angDeg)
                val nx = cos(ang).toFloat(); val ny = sin(ang).toFloat()
                val tx = -ny; val ty = nx          // 切向单位向量（并排方向）
                val n = list.size
                val fs = 11.sp
                val charStep = 11.dp.toPx()        // 竖排字距（径向）
                val colStep = 13.dp.toPx()         // 并排列距（切向）
                val rOuter = R - 6.dp.toPx()       // 将名首字贴近最外圈
                list.forEachIndexed { k, s ->
                    val color = when (s.tone) { 1 -> TY_GREEN; 2 -> TY_YELLOW; 3 -> TY_RED; else -> TY_INK }
                    val style = TextStyle(fontSize = fs, fontFamily = FontFamily.Serif, color = color)
                    val tangent = (k - (n - 1) / 2f) * colStep
                    val chars = s.name.toCharArray()
                    for (j in chars.indices) {
                        val rr = rOuter - j * charStep
                        if (rr < rGodOut + 4.dp.toPx()) break   // 不侵入内一环
                        val x = cx + nx * rr + tx * tangent
                        val y = cy + ny * rr + ty * tangent
                        val tr = measurer.measure(chars[j].toString(), style = style)
                        drawText(textMeasurer = measurer, text = chars[j].toString(),
                            topLeft = Offset(x - tr.size.width / 2f, y - tr.size.height / 2f), style = style)
                    }
                }
            }
        }
    }
}

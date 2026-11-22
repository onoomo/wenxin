package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.Ziwei
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 紫微斗数专用起卦页 —— 对齐 ji.js.cn 原版。
 * 起卦表单：出生公历时间 / 性别 / 时辰索引(留空自动)。
 * 详情页：4×4 十二宫格(中宫信息) + 底部大限流年条。
 * 点击大限/流年/宫格：画三方四正虚线并高亮选中宫。
 */
@Composable
fun ZiweiCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<Ziwei.Result?>(null) }

    if (result == null) {
        JiMingpanCastForm(title = "紫 微 斗 数", onBack = onBack) { f ->
            val r = Ziwei.paipan(f.dateTime, f.isMan, -1)
            result = r
            if (!f.noSave) {
                try {
                    val id = db.insert(f.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        f.name.ifBlank { "紫微斗数 · ${if (f.isMan) "男" else "女"}" }, "ziwei", "紫微斗数",
                        ziweiFaceText(r), "紫微命盘")
                    Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { }
            }
        }
    } else {
        ZiweiDetailScreen(result!!, onBack)
    }
}

private val STAR_MAJOR = Color(0xFFD94A3D)   // 主星 红
private val STAR_MINOR = Color(0xFF3B7DBF)   // 辅星 蓝
private val STAR_ADJ = Color(0xFF5A6B7A)     // 杂曜 灰蓝
private val MUT_COLOR = listOf(Color(0xFF2E8B57), Color(0xFFD2691E), Color(0xFF4169E1), Color(0xFFC0392B)) // 禄权科忌
private val LINE_DASH = Color(0xFFC9A24B)    // 三方四正虚线
private val HL_BG = Color(0x5531A5B5)        // 选中宫高亮底色

// 选中信息：地支绝对序号(0子..11亥) + 显示文案 + 来源类型(区分大限/流年,避免联动)
private enum class ZwSelType { PALACE, DECADAL, YEARLY }
private data class ZwSel(val zhi: Int, val label: String, val type: ZwSelType)

@Composable
private fun ZiweiDetailScreen(r: Ziwei.Result, onBack: () -> Unit) {
    val scroll = rememberScrollState()
    var sel by remember { mutableStateOf<ZwSel?>(null) }
    Box(Modifier.fillMaxSize().background(Color(0xFF0E0C0A))) {
        Column(Modifier.fillMaxSize()) {
            BoardNavBar("紫 微 斗 数", onBack)
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).padding(8.dp)) {
                // 顶部信息条
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BoardTheme.CARD)
                    .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("命主 ${r.soul} · 身主 ${r.body}", color = AppColors.Muted, fontSize = 11.sp,
                        modifier = Modifier.weight(1f))
                    Text(r.juName, color = BoardTheme.GOLD, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))

                // 选中提示条：点击大限/流年/宫格后显示三方四正信息
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(
                        if (sel != null) Color(0x1FC9A24B) else BoardTheme.CARD)
                    .padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sel?.label ?: "点击大限 / 流年 / 宫格，查看三方四正连线",
                        color = if (sel != null) LINE_DASH else AppColors.Dim, fontSize = 11.sp,
                        modifier = Modifier.weight(1f))
                    if (sel != null) {
                        Text("✕ 清除", color = AppColors.Muted, fontSize = 10.sp,
                            modifier = Modifier.clickable { sel = null })
                    }
                }
                Spacer(Modifier.height(6.dp))

                // 4x4 宫格：地支固定位置 + 三方四正连线覆盖层
                // 行0: 巳(5) 午(6) 未(7) 申(8)
                // 行1: 辰(4) [中央x2] 酉(9)
                // 行2: 卯(3) [中央x2] 戌(10)
                // 行3: 寅(2) 丑(1) 子(0) 亥(11)
                val byZhi = r.palaces.associateBy { ziToIdx(it.zhi) }
                val lineW = 1.dp
                val rowH = 112.dp
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().border(lineW, BoardTheme.LINE)) {
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi[5], 5, sel, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi[6], 6, sel, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi[7], 7, sel, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi[8], 8, sel, Modifier.weight(1f)) { sel = it }
                        }
                        ZWDivider()
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi[4], 4, sel, Modifier.weight(1f)) { sel = it }
                            Box(Modifier.weight(2f).border(lineW, BoardTheme.LINE)) { ZiweiCenter(r) }
                            ZwCell(byZhi[9], 9, sel, Modifier.weight(1f)) { sel = it }
                        }
                        ZWDivider()
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi[3], 3, sel, Modifier.weight(1f)) { sel = it }
                            Box(Modifier.weight(2f).border(lineW, BoardTheme.LINE)) { ZiweiCenter2(r) }
                            ZwCell(byZhi[10], 10, sel, Modifier.weight(1f)) { sel = it }
                        }
                        ZWDivider()
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi[2], 2, sel, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi[1], 1, sel, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi[0], 0, sel, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi[11], 11, sel, Modifier.weight(1f)) { sel = it }
                        }
                    }
                    // 三方四正虚线（覆盖在宫格上层，不拦截点击）
                    if (sel != null) {
                        Canvas(Modifier.fillMaxSize()) {
                            val border = 1.dp.toPx()
                            val rh = rowH.toPx()
                            val div = 1.dp.toPx()
                            val cw = (size.width - 2 * border) / 4f
                            fun center(z: Int): FloatArray {
                                val (rr, cc) = zhiToRC(z)
                                return floatArrayOf(border + cc * cw + cw / 2f, border + rr * (rh + div) + rh / 2f)
                            }
                            val z = sel!!.zhi
                            val opp = (z + 6) % 12          // 对宫(冲)
                            val s1 = (z + 4) % 12           // 三合一
                            val s2 = (z + 8) % 12           // 三合二
                            val cOpp = center(opp); val cSel = center(z); val cS1 = center(s1); val cS2 = center(s2)
                            val path = Path().apply {
                                moveTo(cOpp[0], cOpp[1])
                                lineTo(cSel[0], cSel[1])
                                lineTo(cS1[0], cS1[1])
                                lineTo(cS2[0], cS2[1])
                                lineTo(cSel[0], cSel[1])
                            }
                            drawPath(path, LINE_DASH, style = Stroke(
                                width = 1.6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f)),
                                cap = StrokeCap.Round))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                // 底部大限条
                Text("大限 / 流年", color = BoardTheme.GOLD, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    val sorted = r.palaces.sortedBy { it.decadal.first }
                    sorted.forEach { p ->
                        val z = ziToIdx(p.zhi)
                        val active = sel?.type == ZwSelType.DECADAL && sel?.zhi == z
                        Column(Modifier.width(62.dp).border(if (active) 1.5.dp else 0.5.dp,
                                if (active) LINE_DASH else BoardTheme.LINE, RoundedCornerShape(4.dp))
                            .background(if (active) Color(0x2FC9A24B) else Color.Transparent, RoundedCornerShape(4.dp))
                            .clickable {
                                sel = ZwSel(z, "大限 ${p.decadal.first}-${p.decadal.second} · ${p.name}${p.gan}${p.zhi}", ZwSelType.DECADAL)
                            }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${p.decadal.first}-${p.decadal.second}", color = AppColors.Muted, fontSize = 9.sp)
                            Text("${p.gan}${p.zhi}", color = BoardTheme.GOLD, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(p.name, color = AppColors.Paper, fontSize = 9.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 流年条（带干支，可点击画线）—— 与原版一致：虚岁 = 流年 - 出生年 + 1
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    val birthYear = r.solarDate.substringBefore("-").toIntOrNull()
                        ?: java.time.LocalDate.now().year
                    val nowYear = java.time.LocalDate.now().year
                    val yearCount = maxOf(100, nowYear - birthYear + 30)  // 覆盖一生及当前年之后
                    for (k in 0 until yearCount) {
                        val year = birthYear + k
                        val age = k + 1                       // 虚岁：出生年 1 岁，逐年 +1
                        val gz = ganZhiOfYear(year)
                        val z = ziToIdx(gz[1].toString())
                        val active = sel?.type == ZwSelType.YEARLY && sel?.zhi == z
                        Column(Modifier.width(46.dp).border(if (active) 1.5.dp else 0.5.dp,
                                if (active) LINE_DASH else BoardTheme.LINE, RoundedCornerShape(4.dp))
                            .background(if (active) Color(0x2FC9A24B) else Color.Transparent, RoundedCornerShape(4.dp))
                            .clickable {
                                sel = ZwSel(z, "流年 $year · $gz（${p1(z)}）", ZwSelType.YEARLY)
                            }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${year}", color = AppColors.Muted, fontSize = 8.sp)
                            Text(gz, color = BoardTheme.GOLD, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${age}岁", color = AppColors.Paper, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ZWDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(BoardTheme.LINE))

@Composable
private fun ZwCell(p: Ziwei.Palace?, z: Int, sel: ZwSel?, modifier: Modifier, onSelect: (ZwSel) -> Unit) {
    val active = sel?.zhi == z
    val palaceName = p?.name ?: ZHI_CN[z]
    Box(modifier.border(1.dp, if (active) LINE_DASH else BoardTheme.LINE)
        .background(if (active) HL_BG else Color(0xFF14110E))
        .clickable { onSelect(ZwSel(z, "选中 ${p?.gan ?: ""}${ZHI_CN[z]} · ${palaceName}（三方四正）", ZwSelType.PALACE)) }
        .padding(3.dp)) {
        if (p == null) return@Box
        Column(Modifier.fillMaxSize()) {
            // 星曜区
            Column(Modifier.weight(1f)) {
                p.major.forEach { StarLine(it, STAR_MAJOR) }
                p.minor.forEach { StarLine(it, STAR_MINOR) }
                p.adj.chunked(2).forEach { pair ->
                    Text(pair.joinToString(" "), color = STAR_ADJ, fontSize = 9.sp, lineHeight = 11.sp)
                }
            }
            // 底部：长生/宫名/地支 + 博士
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(p.changsheng, color = Color(0xFF7FA05A), fontSize = 9.sp, lineHeight = 10.sp)
                    Text("${p.gan}${p.zhi}", color = AppColors.Muted, fontSize = 9.sp, lineHeight = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(p.boshi, color = Color(0xFF9B7BBE), fontSize = 9.sp, lineHeight = 10.sp)
                    Text(if (p.isBody) "${p.name}·身" else p.name, color = BoardTheme.GOLD,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StarLine(s: Ziwei.Star, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(s.name, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
        if (s.brightness.isNotEmpty()) Text(s.brightness, color = Color(0xFF888888), fontSize = 8.sp, lineHeight = 11.sp)
        if (s.mutagen.isNotEmpty()) {
            val c = MUT_COLOR[when (s.mutagen) { "禄" -> 0; "权" -> 1; "科" -> 2; else -> 3 }]
            Text(s.mutagen, color = c, fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
        }
    }
}

@Composable
private fun ZiweiCenter(r: Ziwei.Result) {
    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("吉时雨 · 紫微", color = BoardTheme.RED, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("${if (r.soul.isNotEmpty()) "阳" else ""}男 · ${r.juName}", color = BoardTheme.GOLD, fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text("公历：${r.solarDate} ${r.timeRange}", color = AppColors.Paper, fontSize = 9.sp)
        Text("农历：${r.lunarDate}", color = AppColors.Paper, fontSize = 9.sp)
        Text("属相：${r.zodiac}  星座：${r.sign}", color = AppColors.Paper, fontSize = 9.sp)
        Text("命宫：${r.ebSoul}  身宫：${r.ebBody}", color = AppColors.Paper, fontSize = 9.sp)
    }
}

@Composable
private fun ZiweiCenter2(r: Ziwei.Result) {
    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("命主：${r.soul}  身主：${r.body}", color = BoardTheme.GOLD, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text("四柱：${r.chineseDate}", color = AppColors.Paper, fontSize = 9.sp)
        Spacer(Modifier.height(2.dp))
        Text("自化 →禄 →权 →科 →忌", color = Color(0xFF888888), fontSize = 9.sp)
    }
}

private val ZHI_CN = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
private val GAN_CN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")

private fun ziToIdx(zhi: String) = ZHI_CN.indexOf(zhi)

// 地支绝对序号 → (行, 列) 宫格坐标
private fun zhiToRC(z: Int): Pair<Int, Int> = when (z) {
    5 -> 0 to 0; 6 -> 0 to 1; 7 -> 0 to 2; 8 -> 0 to 3      // 巳午未申
    4 -> 1 to 0; 9 -> 1 to 3                                  // 辰 酉
    3 -> 2 to 0; 10 -> 2 to 3                                 // 卯 戌
    2 -> 3 to 0; 1 -> 3 to 1; 0 -> 3 to 2; 11 -> 3 to 3       // 寅丑子亥
    else -> 0 to 0
}

private fun p1(z: Int) = ZHI_CN.getOrNull(z) ?: ""

// 公历年 → 干支（4年为甲子）
private fun ganZhiOfYear(year: Int): String {
    val g = GAN_CN[Math.floorMod(year - 4, 10)]
    val z = ZHI_CN[Math.floorMod(year - 4, 12)]
    return g + z
}

private fun ziweiFaceText(r: Ziwei.Result): String = buildString {
    appendLine("【紫微斗数 · 命盘】")
    appendLine("公历：${r.solarDate} ${r.timeRange}")
    appendLine("农历：${r.lunarDate}　${r.chineseDate}")
    appendLine("命宫：${r.ebSoul}　身宫：${r.ebBody}　${r.juName}")
    appendLine("命主：${r.soul}　身主：${r.body}　属相：${r.zodiac}　星座：${r.sign}")
    appendLine()
    r.palaces.forEach { p ->
        val stars = (p.major + p.minor).joinToString(" ") { s ->
            s.name + (if (s.brightness.isNotEmpty()) "(${s.brightness})" else "") + (if (s.mutagen.isNotEmpty()) "[${s.mutagen}]" else "")
        }
        val adj = p.adj.joinToString("·")
        appendLine("${p.name}[${p.gan}${p.zhi}]${if (p.isBody) "(身宫)" else ""} 大限${p.decadal.first}-${p.decadal.second}：${stars}${if (adj.isNotEmpty()) " | $adj" else ""} | 长生${p.changsheng} 博士${p.boshi}")
    }
}

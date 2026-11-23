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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.Ziwei
import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.ui.theme.AppColors
import java.time.format.DateTimeFormatter

/**
 * 紫微斗数专用起卦页 —— 对齐参考视频（吉时雨·紫微风格）。
 * 起卦表单：出生公历时间 / 性别 / 时辰索引(留空自动)。
 * 详情页：白底红标题栏 + 4×4 十二宫格(中央信息区) + 底部大限流年条。
 * 点击大限/流年/宫格：飞星四化落宫标注 + 三方四正虚线连线。
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

// ===== 视频风格配色（白底 + 朱红标题 + 星曜分色）=====
private val ZW_RED = Color(0xFFD9342B)          // 标题栏朱红 / 宫名红
private val ZW_BG = Color(0xFFFFFFFF)           // 白底
private val ZW_LINE = Color(0xFFC9C2B6)         // 宫格线 浅褐灰
private val STAR_MAJOR = Color(0xFF2E5FA8)      // 主星 蓝
private val STAR_MINOR = Color(0xFF3C6E9E)      // 辅星 深蓝
private val STAR_ADJ = Color(0xFF6B6B6B)        // 杂曜 灰
private val STAR_LIGHT = Color(0xFF3E7A6B)      // 亮度 绿灰
private val MUT_COLOR = listOf(
    Color(0xFF1E9E4F),  // 禄 绿
    Color(0xFFD9342B),  // 权 红
    Color(0xFF2E6FD0),  // 科 蓝
    Color(0xFF8E44AD)   // 忌 紫
)
private val LINE_DASH = Color(0xFF7A7A7A)       // 三方四正虚线 灰
private val FLY_GREEN = Color(0xFF1E9E4F)       // 飞星标注 绿
private val HL_BG = Color(0x14D9342B)           // 选中宫高亮底色

// 选中信息：地支绝对序号(0子..11亥) + 显示文案 + 来源类型(区分大限/流年,避免联动) + 天干(算四化)
private enum class ZwSelType { PALACE, DECADAL, YEARLY }
private data class ZwSel(val zhi: Int, val label: String, val type: ZwSelType, val gan: String)

/** 飞星落宫标注：落宫地支绝对序号 -> 该宫的标注(可多个) */
private data class FlyAnno(val text: String, val color: Color)

@Composable
private fun ZiweiDetailScreen(r: Ziwei.Result, onBack: () -> Unit) {
    val scroll = rememberScrollState()
    var sel by remember { mutableStateOf<ZwSel?>(null) }

    // 计算飞星四化：选中宫的天干 -> 禄权科忌四星 -> 各星落宫 -> 落宫显示"禄/权/科/忌"标注
    val flyMap: Map<Int, List<FlyAnno>> = remember(sel, r) {
        val m = mutableMapOf<Int, MutableList<FlyAnno>>()
        sel?.let { s ->
            val sihua = Ziwei.sihua(s.gan) // [禄,权,科,忌] 星名
            val prefix = when (s.type) { ZwSelType.DECADAL -> "大"; ZwSelType.YEARLY -> "年"; else -> "" }
            sihua.forEachIndexed { i, star ->
                if (star.isBlank()) return@forEachIndexed
                val zhi = Ziwei.starZhi(r, star) ?: return@forEachIndexed
                val idx = ziToIdx(zhi)
                // 视频风格：落宫标注「大/年 + 落宫宫名首字」，四化类型用颜色区分
                val palaceName = r.palaces.firstOrNull { it.zhi == zhi }?.name ?: ""
                val text = prefix + palaceName.take(1)
                m.getOrPut(idx) { mutableListOf() }.add(FlyAnno(text, MUT_COLOR[i]))
            }
        }
        m
    }

    Box(Modifier.fillMaxSize().background(ZW_BG)) {
        Column(Modifier.fillMaxSize()) {
            ZwRedNavBar("紫微斗数", onBack)
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).padding(horizontal = 3.dp)) {
                // 4x4 宫格 + 三方四正连线 + 飞星连线覆盖层
                val lineW = 1.dp
                val rowH = 128.dp
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().border(lineW, ZW_LINE)) {
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[5], 5, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[6], 6, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[7], 7, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[8], 8, sel, flyMap, Modifier.weight(1f)) { sel = it }
                        }
                        ZWDivider()
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[4], 4, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            Box(Modifier.weight(2f).border(lineW, ZW_LINE)) { ZiweiCenter(r) }
                            ZwCell(byZhi(r)[9], 9, sel, flyMap, Modifier.weight(1f)) { sel = it }
                        }
                        ZWDivider()
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[3], 3, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            Box(Modifier.weight(2f).border(lineW, ZW_LINE)) { ZiweiCenter2(r) }
                            ZwCell(byZhi(r)[10], 10, sel, flyMap, Modifier.weight(1f)) { sel = it }
                        }
                        ZWDivider()
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[2], 2, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[1], 1, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[0], 0, sel, flyMap, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[11], 11, sel, flyMap, Modifier.weight(1f)) { sel = it }
                        }
                    }
                    // 三方四正 + 飞星连线（覆盖在宫格上层，不拦截点击）
                    if (sel != null) {
                        Canvas(Modifier.fillMaxSize()) {
                            val border = lineW.toPx()
                            val rh = rowH.toPx()
                            val div = lineW.toPx()
                            val cw = (size.width - 2 * border) / 4f
                            fun center(z: Int): FloatArray {
                                val (rr, cc) = zhiToRC(z)
                                return floatArrayOf(border + cc * cw + cw / 2f, border + rr * (rh + div) + rh / 2f)
                            }
                            val z = sel!!.zhi
                            val opp = (z + 6) % 12
                            val s1 = (z + 4) % 12
                            val s2 = (z + 8) % 12
                            val cOpp = center(opp); val cSel = center(z); val cS1 = center(s1); val cS2 = center(s2)
                            val path = Path().apply {
                                moveTo(cOpp[0], cOpp[1]); lineTo(cSel[0], cSel[1]); lineTo(cS1[0], cS1[1])
                                lineTo(cS2[0], cS2[1]); lineTo(cSel[0], cSel[1])
                            }
                            drawPath(path, LINE_DASH, style = Stroke(
                                width = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
                                cap = StrokeCap.Round))
                            // 飞星落宫连线（选中宫 -> 各落宫）
                            flyMap.keys.forEach { tz ->
                                val c = center(tz)
                                val fp = Path().apply { moveTo(cSel[0], cSel[1]); lineTo(c[0], c[1]) }
                                drawPath(fp, Color(0xFFBBBBBB), style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f))))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // 底部：大限 + 流年 两行表（视频样式），点击联动飞星
                ZwBottomTable(r, sel) { sel = it }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable private fun ZWDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(ZW_LINE))

private fun byZhi(r: Ziwei.Result) = r.palaces.associateBy { ziToIdx(it.zhi) }

@Composable
private fun ZwCell(
    p: Ziwei.Palace?, z: Int, sel: ZwSel?, fly: Map<Int, List<FlyAnno>>,
    modifier: Modifier, onSelect: (ZwSel) -> Unit
) {
    val active = sel?.zhi == z
    val gan = p?.gan ?: ""
    val zhi = ZHI_CN[z]
    Box(modifier.border(1.dp, if (active) ZW_RED else ZW_LINE)
        .background(if (active) HL_BG else ZW_BG)
        .clickable { onSelect(ZwSel(z, "选中 ${gan}${zhi} · ${p?.name ?: ""}", ZwSelType.PALACE, gan)) }
        .padding(horizontal = 3.dp, vertical = 2.dp)) {
        if (p == null) return@Box
        Column(Modifier.fillMaxSize()) {
            // 星曜区（主星/辅星/杂曜 + 亮度 + 四化）
            Column(Modifier.weight(1f)) {
                p.major.forEach { ZwStarLine(it) }
                p.minor.forEach { ZwStarLine(it) }
                p.adj.chunked(2).forEach { pair ->
                    Text(pair.joinToString(" "), color = STAR_ADJ, fontSize = 8.5.sp, lineHeight = 10.sp)
                }
            }
            // 飞星落宫标注（禄/权/科/忌）
            val annos = fly[z]
            if (annos != null && annos.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    annos.forEach { a ->
                        Text(a.text, color = a.color, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp)
                    }
                }
            }
            // 底部行：起运岁数 + 长生 + 宫名 + 地支
            Text("${p.decadal.first}-${p.decadal.second}", color = Color(0xFF9A9A9A), fontSize = 7.5.sp, lineHeight = 9.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(p.changsheng, color = STAR_LIGHT, fontSize = 8.sp, lineHeight = 9.sp)
                    Text(p.boshi, color = Color(0xFF8A7BA8), fontSize = 8.sp, lineHeight = 9.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (p.isBody) "${p.name}·身" else p.name, color = ZW_RED,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                    Text(gan + zhi, color = Color(0xFF8A8A8A), fontSize = 8.sp, lineHeight = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ZwStarLine(s: Ziwei.Star) {
    val color = when (s.type) { "major" -> STAR_MAJOR; else -> STAR_MINOR }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(s.name, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
        if (s.brightness.isNotEmpty()) Text(s.brightness, color = STAR_LIGHT, fontSize = 8.sp, lineHeight = 12.sp)
        if (s.mutagen.isNotEmpty()) {
            val c = MUT_COLOR[when (s.mutagen) { "禄" -> 0; "权" -> 1; "科" -> 2; else -> 3 }]
            Text(s.mutagen, color = c, fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun ZiweiCenter(r: Ziwei.Result) {
    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 5.dp)) {
        Text("吉时雨 · 紫微", color = Color(0xFF7B3FB5), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(3.dp))
        val yinYang = if (Ziweiyang(r.yearGan)) "阳" else "阴"
        val gender = if (r.isMan) "男" else "女"
        Text("$yinYang$gender · ${r.juName}", color = Color(0xFF7B3FB5), fontSize = 11.5.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text("公历：${r.solarDate} ${r.timeRange}", color = Color(0xFF444444), fontSize = 9.5.sp, lineHeight = 13.sp)
        Text("农历：${lunarCn(r)} ${zhiToShi(r)}", color = Color(0xFF444444), fontSize = 9.5.sp, lineHeight = 13.sp)
        Text("属相：${r.zodiac}   星座：${r.sign}", color = Color(0xFF444444), fontSize = 9.5.sp, lineHeight = 13.sp)
        Text("命主：${r.soul}   身主：${r.body}", color = Color(0xFF444444), fontSize = 9.5.sp, lineHeight = 13.sp)
        Text("命宫：${r.ebSoul}   身宫：${r.ebBody}", color = Color(0xFF444444), fontSize = 9.5.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun ZiweiCenter2(r: Ziwei.Result) {
    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 5.dp)) {
        // 四柱（天干一行彩色、地支一行）：丙 己 辛 丁 / 午 亥 卯 酉
        val ganZhi = r.chineseDate.split(" ").filter { it.length >= 2 }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ganZhi.forEach { gz ->
                Text(gz[0].toString(), color = Color(0xFF444444), fontSize = 19.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ganZhi.forEach { gz ->
                Text(gz[1].toString(), color = Color(0xFF444444), fontSize = 19.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("出生后 ${r.juName.replace("局","")} 起运 · 五行局 ${r.wuxingju}",
            color = Color(0xFF666666), fontSize = 8.5.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("自化  →禄 →权 →科 →忌", color = Color(0xFF888888), fontSize = 9.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ZwBottomTable(r: Ziwei.Result, sel: ZwSel?, onSelect: (ZwSel) -> Unit) {
    val birthYear = r.solarDate.substringBefore("-").toIntOrNull() ?: 2000
    // 两行共用一个滚动状态：滑动大限时流年表同步跟着动（1:1 原版联动）
    val sharedScroll = rememberScrollState()
    val cellW = 50.dp
    Column(Modifier.fillMaxWidth().border(1.dp, ZW_LINE).background(Color(0xFFFBFAF7))) {
        Row(Modifier.fillMaxWidth()) {
            // 左侧固定行标签列
            Column(Modifier.width(30.dp)) {
                Box(Modifier.fillMaxWidth().height(46.dp).background(Color(0xFFF2EEE6)).border(1.dp, ZW_LINE),
                    contentAlignment = Alignment.Center) {
                    Text("大\n限", color = Color(0xFF6B5B3A), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
                }
                Box(Modifier.fillMaxWidth().height(46.dp).background(Color(0xFFF2EEE6)).border(1.dp, ZW_LINE),
                    contentAlignment = Alignment.Center) {
                    Text("流\n年", color = Color(0xFF6B5B3A), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
                }
            }
            // 大限 / 流年两行放进同一个横向滚动容器，列宽一致、上下对齐、同步移动
            Column(Modifier.weight(1f).horizontalScroll(sharedScroll)) {
                // 大限行
                Row(Modifier.height(46.dp)) {
                    r.palaces.sortedBy { it.decadal.first }.forEach { p ->
                        val z = ziToIdx(p.zhi)
                        val active = sel?.type == ZwSelType.DECADAL && sel?.zhi == z
                        Column(Modifier.width(cellW).border(0.5.dp, ZW_LINE)
                            .background(if (active) Color(0xFFFFE0B2) else Color.Transparent)
                            .clickable { onSelect(ZwSel(z, "大限 ${p.decadal.first}-${p.decadal.second} · ${p.name}${p.gan}${p.zhi}", ZwSelType.DECADAL, p.gan)) }
                            .padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${p.decadal.first}-${p.decadal.second}", color = Color(0xFF888888), fontSize = 8.5.sp)
                            Text(p.gan + p.zhi, color = ganWuXingColor(p.gan), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // 流年行：与大限行共用滚动，从左到右逐年排列
                Row(Modifier.height(46.dp)) {
                    val count = 90
                    for (k in 0 until count) {
                        val year = birthYear + k
                        val age = k + 1
                        val gz = ganZhiOfYear(year)
                        val z = ziToIdx(gz[1].toString())
                        val active = sel?.type == ZwSelType.YEARLY && sel?.zhi == z
                        Column(Modifier.width(cellW).border(0.5.dp, ZW_LINE)
                            .background(if (active) Color(0xFFFFE0B2) else Color.Transparent)
                            .clickable { onSelect(ZwSel(z, "流年 $year · $gz（${age}岁）", ZwSelType.YEARLY, gz[0].toString())) }
                            .padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${year}年 ${age}岁", color = Color(0xFF888888), fontSize = 7.5.sp)
                            Text(gz, color = ganWuXingColor(gz[0].toString()), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** 天干五行配色：甲乙木绿、丙丁火红、戊己土黄褐、庚辛金橙、壬癸水蓝（对齐截图大限干支彩色） */
private fun ganWuXingColor(gan: String) = when (gan) {
    "甲" -> Color(0xFF3E9E5E); "乙" -> Color(0xFF5BAE72)
    "丙" -> Color(0xFFE0523F); "丁" -> Color(0xFFD9342B)
    "戊" -> Color(0xFFB88A2E); "己" -> Color(0xFFA67C2E)
    "庚" -> Color(0xFFD98A2B); "辛" -> Color(0xFFC97A1F)
    "壬" -> Color(0xFF2E6FD0); "癸" -> Color(0xFF4A86D8)
    else -> Color(0xFF444444)
}

@Composable
private fun ZwRedNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(ZW_RED).height(50.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = Color.White, fontSize = 30.sp)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.size(40.dp))
    }
}

private val ZHI_CN = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
private val GAN_CN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")

private fun ziToIdx(zhi: String) = ZHI_CN.indexOf(zhi).coerceAtLeast(0)

// 地支绝对序号 → (行, 列) 宫格坐标
private fun zhiToRC(z: Int): Pair<Int, Int> = when (z) {
    5 -> 0 to 0; 6 -> 0 to 1; 7 -> 0 to 2; 8 -> 0 to 3      // 巳午未申
    4 -> 1 to 0; 9 -> 1 to 3                                  // 辰 酉
    3 -> 2 to 0; 10 -> 2 to 3                                 // 卯 戌
    2 -> 3 to 0; 1 -> 3 to 1; 0 -> 3 to 2; 11 -> 3 to 3       // 寅丑子亥
    else -> 0 to 0
}

private fun Ziweiyang(gan: String) = GAN_CN.indexOf(gan) % 2 == 0  // 甲丙戊庚壬为阳

private fun lunarCn(r: Ziwei.Result): String {
    // 从 lunarDate "2026年11月23日" 提取中文农历
    val parts = Regex("(\\d+)年(\\d+)月(\\d+)日").find(r.lunarDate)?.destructured
    return if (parts != null) {
        val (y, m, d) = parts
        val yCn = y.map { LunarCalendar.cnNumber(it - '0') }.joinToString("")
        "农历${yCn}年${LunarCalendar.cnNumber(m.toInt())}月${LunarCalendar.cnNumber(d.toInt())}日"
    } else r.lunarDate
}

private fun zhiToShi(r: Ziwei.Result): String {
    // 由 timeRange 起始小时推时辰地支：17:00 -> 酉
    val h = r.timeRange.substringBefore(":").toIntOrNull() ?: 0
    return ZHI_CN[((h + 1) / 2) % 12] + "时"
}

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

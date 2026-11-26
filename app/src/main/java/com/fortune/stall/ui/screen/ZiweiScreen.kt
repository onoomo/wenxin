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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.fortune.stall.engine.jishiyu.ZiweiFortune
import com.fortune.stall.util.LunarCalendar
import java.time.Year
import java.time.format.DateTimeFormatter

/**
 * 紫微斗数专用起卦页。
 * 起卦表单沿用通用表单；详情页 1:1 移植 vvdevpro/ziwei-ds 的「深色金国风」盘面：
 * 顶部本命/大限/流年 TimeNav + 年份切换；深色 4×4 十二宫格 + ☯ 中宫；
 * 琥珀色主星（按亮度分档）+ 四化 badge + 大限/流年四化叠加；
 * 点击宫位画三方四正虚线；底部大限/流年联动表共用横向滚动。
 */
@Composable
fun ZiweiCastScreen(db: FortuneDb, onBack: () -> Unit, onAskAi: (String) -> Unit = {}, onHeming: () -> Unit = {}) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<Ziwei.Result?>(null) }
    // 保存起卦参数，供详情页切换亮度流派时即时重排
    var lastArgs by remember { mutableStateOf<Triple<java.time.LocalDateTime, Boolean, Double?>?>(null) }

    if (result == null) {
        JiMingpanCastForm(title = "紫 微 斗 数", onBack = onBack, showLongitude = true, dark = true) { f ->
            // 真太阳时开关开启且填了经度才做经度+均时差校正；夏令时按主流口径只提示、不自动减 1 小时
            val lon = if (f.trueSolar) f.longitude else null
            val r = Ziwei.paipan(f.dateTime, f.isMan, -1, lon, false)
            result = r
            lastArgs = Triple(f.dateTime, f.isMan, lon)
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
        ZiweiDetailScreen(result!!, onBack, onAskAi, onHeming, onSchoolChange = { school ->
            com.fortune.stall.util.ZiweiSettings.setSchool(school)
            lastArgs?.let { (dt, man, lon) ->
                result = Ziwei.paipan(dt, man, -1, lon, false)
            }
        })
    }
}

// ===== mingyu 深紫粉配色（对照 mingyu 紫微斗数结果界面）=====
private val ZW_BG        = Color(0xFF17131F)          // 深紫近黑主背景
private val ZW_SURFACE   = Color(0xFF241D2E)          // 卡片紫 surface
private val ZW_CARD      = Color(0xFF2E2538)          // 信息卡底（略亮紫）
private val ZW_BORDER    = Color(0x2AFFFFFF)          // 细边框
private val ZW_BORDER_H  = Color(0x40FFFFFF)          // 稍亮边框
private val ZW_GOLD      = Color(0xFFE7A0C8)          // 参考图主强调：粉（命主/标题/高亮）
private val ZW_GOLD_DIM  = Color(0xFFC97FB0)          // 粉暗
private val PINK         = Color(0xFFE17FB0)          // 强粉（历史/童限胶囊/分享）
private val PINK_BG      = Color(0x2AE17FB0)          // 粉底
private val ZW_T0        = Color(0xFFF4EFF7)          // 主文字 近白
private val ZW_T1        = Color(0xFFE6DDEE)          // 次文字
private val ZW_T2        = Color(0xFFB9ABCD)          // 次级灰紫
private val ZW_FAINT     = Color(0xFF8B7E9B)          // 淡字

// 星曜亮度色系（保留 amber 金， Mingyu 参考图星曜本就偏暖，与粉紫底对比）
private val STAR_BRIGHT  = Color(0xFFFCD34D)
private val STAR_NORMAL  = Color(0xFFF5B860)
private val STAR_DIM     = Color(0xFFB98A5A)
private val STAR_LUCKY   = Color(0xB27FD8E8)          // 吉星
private val STAR_SHA     = Color(0x99E87A7A)          // 煞星
private val STAR_ADJ     = Color(0x807B6E90)          // 杂曜 淡

// 四化语义色：禄绿 权蓝 科黄 忌红
private val SH_COLORS = listOf(Color(0xFF4ADE80), Color(0xFF60A5FA), Color(0xFFFACC15), Color(0xFFF87171))
private val SH_BG     = listOf(Color(0x1A4ADE80), Color(0x1A60A5FA), Color(0x1AFACC15), Color(0x1AF87171))
private fun shIdx(s: String) = when (s) { "禄" -> 0; "权" -> 1; "科" -> 2; else -> 3 }

// 选中/三方/大限底色（粉紫系）
private val SEL_BG      = Color(0x2EE17FB0)
private val SEL_LINE    = Color(0xB3E17FB0)
private val SANFANG_BG  = Color(0x14E17FB0)
private val DX_BG       = Color(0x149B6FD8)
private val DX_STRIP    = Color(0x809B6FD8)
private val MING_TINT   = Color(0x0CE17FB0)
private val GRID_LINE   = Color(0x24FFFFFF)
private val SF_STROKE   = Color(0x8CE17FB0)
private val FLY_STROKE  = Color(0x66B0BEC8)

private enum class ZwView { MINGPAN, DAXIAN, LIUNIAN }
// 选中信息：地支绝对序号(0子..11亥) + 文案 + 来源类型 + 天干(算四化)
private enum class ZwSelType { PALACE, DECADAL, YEARLY }
private data class ZwSel(val zhi: Int, val label: String, val type: ZwSelType, val gan: String)
private data class FlyAnno(val text: String, val color: Color)

@Composable
private fun ZiweiDetailScreen(
    r: Ziwei.Result, onBack: () -> Unit, onAskAi: (String) -> Unit, onHeming: () -> Unit,
    onSchoolChange: (String) -> Unit
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()
    var view by remember { mutableStateOf(ZwView.MINGPAN) }
    var liunianYear by remember { mutableStateOf(Year.now().value) }
    var sel by remember { mutableStateOf<ZwSel?>(null) }

    val nowYear = Year.now().value
    val (dxZhi, dxGan) = remember(r) { Ziwei.currentDaXian(r, nowYear) }
    val dxPalace = r.palaces.firstOrNull { ziToIdx(it.zhi) == dxZhi }
    val dxRange = dxPalace?.decadal

    // 当前 view 的四化叠加：星名 -> [禄/权/科/忌]
    val overlay: Map<String, String> = remember(view, dxGan, liunianYear, r) {
        val gan = when (view) {
            ZwView.MINGPAN -> null
            ZwView.DAXIAN  -> dxGan
            ZwView.LIUNIAN -> Ziwei.yearGan(liunianYear)
        }
        if (gan == null) emptyMap() else {
            val s = Ziwei.sihua(gan)
            val types = listOf("禄", "权", "科", "忌")
            s.mapIndexedNotNull { i, name -> if (name.isBlank()) null else name to types[i] }.toMap()
        }
    }
    val overlayLabel = when (view) { ZwView.DAXIAN -> "限"; ZwView.LIUNIAN -> "年"; else -> null }
    val overlayGanName = when (view) {
        ZwView.DAXIAN  -> dxGan
        ZwView.LIUNIAN -> Ziwei.yearGan(liunianYear)
        else -> ""
    }

    // 飞星落宫：选中来源天干 -> 四化落宫（本命选中用出生年干）
    val flyMap: Map<Int, List<FlyAnno>> = remember(sel, r) {
        val m = mutableMapOf<Int, MutableList<FlyAnno>>()
        sel?.let { s ->
            val sihua = Ziwei.sihua(s.gan)
            val prefix = when (s.type) { ZwSelType.DECADAL -> "限"; ZwSelType.YEARLY -> "年"; else -> "" }
            sihua.forEachIndexed { i, star ->
                if (star.isBlank()) return@forEachIndexed
                val zhi = Ziwei.starZhi(r, star) ?: return@forEachIndexed
                val idx = ziToIdx(zhi)
                val palaceName = r.palaces.firstOrNull { it.zhi == zhi }?.name ?: ""
                m.getOrPut(idx) { mutableListOf() }
                    .add(FlyAnno(prefix + palaceName.take(1), SH_COLORS[i]))
            }
        }
        m
    }

    val sanFang = sel?.let { intArrayOf(it.zhi, (it.zhi + 6) % 12, (it.zhi + 4) % 12, (it.zhi + 8) % 12) }

    // 全盘自化（宫干四化星落本宫）
    val selfSihuaMap = remember(r) { Ziwei.allSelfSihua(r) }

    // 当前年龄/时限（童限：起运前；否则当前大限）
    val birthYear = r.solarDate.substringBefore("-").toIntOrNull() ?: nowYear
    val ageNow = (nowYear - birthYear + 1).coerceAtLeast(1)
    val firstLimit = r.palaces.minOfOrNull { it.decadal.first } ?: 6
    val limitLabel = if (ageNow < firstLimit) "童限" else "大限 ${dxRange?.first ?: 0}-${dxRange?.second ?: 0}岁"

    Box(Modifier.fillMaxSize().background(ZW_BG)) {
        Column(Modifier.fillMaxSize()) {
            ZwGoldNavBar("紫微斗数结果", onBack)
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).padding(horizontal = 12.dp)) {
                Spacer(Modifier.height(10.dp))
                // 子页签：紫微 / 命录（命录=合盘入口，对齐参考图布局）
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(ZW_SURFACE)
                    .border(1.dp, ZW_BORDER, RoundedCornerShape(20.dp)).padding(3.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(PINK_BG)
                        .padding(horizontal = 16.dp, vertical = 5.dp)) {
                        Text("紫微", color = PINK, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onHeming).padding(horizontal = 16.dp, vertical = 5.dp)) {
                        Text("命录", color = ZW_T2, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(14.dp))
                // 紫微总览 / 当前命盘
                Text("紫微总览", color = PINK, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("当前命盘", color = ZW_T0, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                // 胶囊标签：时限 / 时辰 / 性别
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZwOutlinePill(limitLabel)
                    ZwOutlinePill(r.timeRange)
                    ZwOutlinePill(if (r.isMan) "男" else "女")
                }
                Spacer(Modifier.height(12.dp))
                // 2×2 信息卡：命主 / 身主 / 五行局 / 当前时限
                ZwSummaryCard(r, limitLabel)
                Spacer(Modifier.height(16.dp))
                // 标题行
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("紫微斗数命盘", color = ZW_T0, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.Transparent)
                        .border(1.dp, ZW_BORDER_H, RoundedCornerShape(14.dp))
                        .clickable {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("紫微命盘", ziweiFaceText(r)))
                            Toast.makeText(ctx, "排盘已复制，可分享", Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("分享排盘", color = ZW_T1, fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(PINK_BG)
                        .border(1.dp, PINK.copy(0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(limitLabel, color = PINK, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZW_BORDER))
                Spacer(Modifier.height(10.dp))

                // TimeNav：本命 / 大限 / 流年 + 年份切换（保留增强功能）
                ZwTimeNav(view, liunianYear, dxRange, overlay, overlayGanName,
                    onView = { view = it }, onYear = { liunianYear = it })
                Spacer(Modifier.height(12.dp))

                // 4x4 分离圆角宫格卡 + 三方四正覆盖连线（mingyu 传统盘面）
                val rowH = 132.dp
                val cellPad = 2.dp
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        // 第一行：巳 午 未 申
                        Row(Modifier.fillMaxWidth()) {
                            listOf(5, 6, 7, 8).forEach { z ->
                                ZwCell(byZhi(r)[z], z, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi,
                                    Modifier.weight(1f).height(rowH).padding(cellPad)) { sel = it }
                            }
                        }
                        // 中段：左(辰/卯) + 中宫(跨2行) + 右(酉/戌)
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                ZwCell(byZhi(r)[4], 4, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                                ZwCell(byZhi(r)[3], 3, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                            }
                            Box(Modifier.weight(2f).height(rowH * 2).padding(cellPad)) {
                                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    .background(ZW_SURFACE).border(1.dp, ZW_BORDER, RoundedCornerShape(10.dp))) {
                                    ZwCenterInfo(r, dxRange, limitLabel)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                ZwCell(byZhi(r)[9], 9, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                                ZwCell(byZhi(r)[10], 10, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                            }
                        }
                        // 末行：寅 丑 子 亥
                        Row(Modifier.fillMaxWidth()) {
                            listOf(2, 1, 0, 11).forEach { z ->
                                ZwCell(byZhi(r)[z], z, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi,
                                    Modifier.weight(1f).height(rowH).padding(cellPad)) { sel = it }
                            }
                        }
                    }
                    if (sel != null) {
                        Canvas(Modifier.fillMaxSize()) {
                            val rh = rowH.toPx()
                            val cw = size.width / 4f
                            fun center(z: Int): FloatArray {
                                val (rr, cc) = zhiToRC(z)
                                return floatArrayOf(cc * cw + cw / 2f, rr * rh + rh / 2f)
                            }
                            val z = sel!!.zhi
                            val p0 = center(z); val p1 = center((z + 6) % 12)
                            val p2 = center((z + 4) % 12); val p3 = center((z + 8) % 12)
                            val style = Stroke(width = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)), cap = StrokeCap.Round)
                            drawPath(Path().apply { moveTo(p0[0], p0[1]); lineTo(p1[0], p1[1]) }, SF_STROKE, style = style)
                            drawPath(Path().apply { moveTo(p0[0], p0[1]); lineTo(p2[0], p2[1]); lineTo(p3[0], p3[1]); lineTo(p0[0], p0[1]) }, SF_STROKE, style = style)
                            listOf(p0, p1, p2, p3).forEachIndexed { i, p ->
                                drawCircle(SF_STROKE.copy(alpha = if (i == 0) 0.95f else 0.6f),
                                    radius = 3.dp.toPx(), center = Offset(p[0], p[1]))
                            }
                            flyMap.keys.forEach { tz ->
                                val c = center(tz)
                                drawPath(Path().apply { moveTo(p0[0], p0[1]); lineTo(c[0], c[1]) }, FLY_STROKE,
                                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f))))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // 操作行：问AI / 复制 / 合盘 / 流派
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(PINK_BG)
                        .border(1.dp, PINK.copy(0.5f), RoundedCornerShape(16.dp))
                        .clickable { onAskAi(ziweiFaceText(r)) }
                        .padding(horizontal = 8.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text("✦ 问 AI 解读（13 维）", color = PINK, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(Modifier.weight(0.5f).clip(RoundedCornerShape(16.dp)).background(Color(0x12FFFFFF))
                        .border(1.dp, GRID_LINE, RoundedCornerShape(16.dp))
                        .clickable {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("紫微命盘", ziweiFaceText(r)))
                            Toast.makeText(ctx, "命盘已复制", Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 8.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text("⧉ 复制", color = ZW_T1, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val curSchool = com.fortune.stall.util.ZiweiSettings.brightnessSchool.value
                    com.fortune.stall.engine.jishiyu.ZiweiSchools.NAMES.forEach { (label, key) ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (curSchool == key) PINK_BG else Color(0x10FFFFFF))
                            .border(1.dp, if (curSchool == key) PINK.copy(0.6f) else GRID_LINE, RoundedCornerShape(14.dp))
                            .clickable { if (curSchool != key) onSchoolChange(key) }
                            .padding(horizontal = 4.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
                            Text(label, color = if (curSchool == key) PINK else ZW_FAINT, fontSize = 9.5.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // 图例 + 提示
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    listOf("禄", "权", "科", "忌").forEach { s ->
                        ZwBadge(s, false, null)
                        Spacer(Modifier.width(5.dp))
                    }
                }
                Text("点击宫位看三方四正 · 切本命/大限/流年看四化叠加", color = ZW_FAINT,
                    fontSize = 8.5.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = TextAlign.Center)

                // 四化 · 自化与来因（保留增强）
                if (selfSihuaMap.isNotEmpty() || sel != null) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp))
                        .background(ZW_SURFACE).border(1.dp, ZW_BORDER, RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Text("四化 · 自化与来因", color = PINK, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        if (selfSihuaMap.isNotEmpty()) {
                            Spacer(Modifier.height(5.dp))
                            val parts = mutableListOf<String>()
                            selfSihuaMap.toSortedMap().forEach { (z, list) ->
                                val gong = Ziwei.gongNameAt(r, z)
                                list.forEach { (kind, star) -> parts.add("$gong·${star}自化$kind") }
                            }
                            Text(parts.chunked(3).joinToString("　") { it.joinToString("  ") },
                                color = ZW_T2, fontSize = 9.sp, lineHeight = 15.sp)
                        }
                        if (sel != null) {
                            val p = byZhi(r)[sel!!.zhi]
                            if (p != null) {
                                val selfHere = Ziwei.selfSihua(p)
                                val incoming = (p.major + p.minor).flatMap { st ->
                                    listOf("禄", "权", "科", "忌").mapNotNull { k ->
                                        val from = Ziwei.incomingPalaces(r, st.name, k)
                                        if (from.isNotEmpty()) "${st.name}化$k←${Ziwei.gongNameAt(r, from[0])}" else null
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                val detail = buildString {
                                    append("${Ziwei.gongNameAt(r, sel!!.zhi)}${p.gan}${p.zhi}：")
                                    if (selfHere.isNotEmpty()) append("自化${selfHere.joinToString("、") { "${it.second}${it.first}" }}")
                                    if (incoming.isNotEmpty()) {
                                        if (selfHere.isNotEmpty()) append("；")
                                        append("来因 ${incoming.take(3).joinToString("、")}")
                                    }
                                    if (selfHere.isEmpty() && incoming.isEmpty()) append("无自化 / 来因")
                                }
                                Text(detail, color = ZW_T1, fontSize = 9.sp, lineHeight = 14.sp)
                                val shenDetail = buildString {
                                    append("长生 ${p.changsheng.ifBlank { "—" }}　")
                                    append("博士 ${p.boshi.ifBlank { "—" }}")
                                    append("\n将前 ${p.jiangqian.ifBlank { "—" }}　")
                                    append("岁前 ${p.suiqian.ifBlank { "—" }}")
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(shenDetail, color = ZW_T2, fontSize = 9.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                // 底部：大限 + 流年 + 流月 + 流日 联动表
                ZwBottomTable(r, sel, birthYear, limitLabel) { sel = it }
                Spacer(Modifier.height(28.dp))
            }
        }
        // 右下角悬浮 AI 按钮（对齐参考图）
        Box(Modifier.align(Alignment.BottomEnd).padding(14.dp)
            .clip(RoundedCornerShape(18.dp)).background(PINK)
            .clickable { onAskAi(ziweiFaceText(r)) }
            .padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("AI", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 胶囊描边标签（参考图：童限 / 辰时 / 男）
@Composable
private fun ZwOutlinePill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0x10FFFFFF))
        .border(1.dp, ZW_BORDER_H, RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 5.dp)) {
        Text(text, color = ZW_T2, fontSize = 11.sp)
    }
}

// 2×2 命盘信息卡（参考图：命主/身主/五行局/当前时限）
@Composable
private fun ZwSummaryCard(r: Ziwei.Result, limitLabel: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(ZW_CARD).border(1.dp, ZW_BORDER, RoundedCornerShape(12.dp))) {
        Row(Modifier.fillMaxWidth()) {
            ZwSummaryCell("命主", r.soul, "命宫支 ${r.ebSoul}", Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(64.dp).background(ZW_BORDER))
            ZwSummaryCell("身主", r.body, "身宫支 ${r.ebBody}", Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZW_BORDER))
        Row(Modifier.fillMaxWidth()) {
            ZwSummaryCell("五行局", r.juName, r.zodiac, Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(64.dp).background(ZW_BORDER))
            ZwSummaryCell("当前时限", limitLabel, r.solarDate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ZwSummaryCell(label: String, value: String, sub: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label, color = ZW_T2, fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = ZW_T0, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = ZW_FAINT, fontSize = 10.sp)
    }
}

@Composable private fun ZwGridDivider(w: androidx.compose.ui.unit.Dp) =
    Box(Modifier.fillMaxWidth().height(w).background(GRID_LINE))

private fun byZhi(r: Ziwei.Result) = r.palaces.associateBy { ziToIdx(it.zhi) }

// ===== 宫格 =====
@Composable
private fun ZwCell(
    p: Ziwei.Palace?, z: Int, r: Ziwei.Result, sel: ZwSel?, fly: Map<Int, List<FlyAnno>>,
    sanFang: IntArray?, overlay: Map<String, String>, overlayLabel: String?, dxZhi: Int,
    modifier: Modifier, onSelect: (ZwSel) -> Unit
) {
    val active = sel?.zhi == z
    val isCurrentDx = dxZhi == z
    val isMing = z == Ziwei.mingZhiIdx(r)
    val isShen = p?.isBody == true
    val isSanFang = sanFang?.contains(z) == true && !active
    val gan = p?.gan ?: ""

    val bg = when {
        isCurrentDx -> DX_BG
        active -> SEL_BG
        isSanFang -> SANFANG_BG
        isMing || isShen -> MING_TINT
        else -> ZW_CARD
    }
    val borderColor = when {
        active -> SEL_LINE
        isSanFang -> PINK.copy(0.4f)
        else -> ZW_BORDER
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(bg)
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .clickable {
            if (active) Unit else onSelect(ZwSel(z, "选中 ${gan}${ZHI_CN[z]} · ${p?.name ?: ""}", ZwSelType.PALACE, gan))
        }
        .padding(horizontal = 5.dp, vertical = 4.dp)) {
        if (isCurrentDx) Box(Modifier.width(2.5.dp).fillMaxSize().background(DX_STRIP))
        if (p == null) return@Box
        Column(Modifier.fillMaxSize()) {
            // 顶部：左上干支 pill / 右上大限区间 pill（mingyu）
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x1AFFFFFF))
                    .padding(horizontal = 6.dp, vertical = 1.dp)) {
                    Text(gan + ZHI_CN[z], color = ZW_T0, fontSize = 9.sp, lineHeight = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF))
                    .padding(horizontal = 5.dp, vertical = 1.dp)) {
                    Text("${p.decadal.first}-${p.decadal.second}",
                        color = if (isCurrentDx) Color(0xFFD8B4FE) else ZW_T2,
                        fontSize = 8.sp, lineHeight = 11.sp)
                }
            }
            // "流年"小字 + 该宫岁数序列（mingyu 流年盘核心）
            Text("流年", color = ZW_FAINT, fontSize = 7.5.sp, lineHeight = 9.sp,
                modifier = Modifier.padding(top = 2.dp))
            if (p.ages.isNotEmpty()) {
                Text(p.ages.joinToString("·"), color = PINK.copy(0.85f), fontSize = 7.sp,
                    lineHeight = 9.sp, maxLines = 2)
            }
            Spacer(Modifier.height(1.dp))
            // 主星（保留专业数据，mingyu 原版此视图以流年为主，星曜作精简叠加）
            val majors = p.major
            majors.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, color = starBrightColor(s.brightness), fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold, lineHeight = 12.sp)
                    if (s.mutagen.isNotEmpty()) ZwBadge(s.mutagen, false, null)
                    overlay[s.name]?.let { ZwBadge(it, true, overlayLabel) }
                }
            }
            // 吉星 / 煞星（精简为一行）
            val minors = p.minor
            if (minors.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    minors.take(4).forEach { s ->
                        Text(s.name, color = if (s.type == "tough") STAR_SHA else STAR_LUCKY,
                            fontSize = 8.sp, lineHeight = 10.sp)
                    }
                }
            }
            // 飞星落宫标注
            val annos = fly[z]
            if (!annos.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    annos.forEach { Text(it.text, color = it.color, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                }
            }
            // 宫底 badge（对齐 ziweiTraditionalBoard footerBadges）：长生 / 博士 / 将前 / 岁前
            Spacer(Modifier.weight(1f))
            val footBadges = listOfNotNull(
                p.changsheng.takeIf { it.isNotBlank() }?.let { "长生 $it" },
                p.boshi.takeIf { it.isNotBlank() }?.let { "博士 $it" },
                p.jiangqian.takeIf { it.isNotBlank() }?.let { "将前 $it" },
                p.suiqian.takeIf { it.isNotBlank() }?.let { "岁前 $it" }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0x22FFFFFF))
                    .padding(horizontal = 4.dp, vertical = 1.dp)) {
                    Text(p.name, color = if (isMing) PINK else ZW_T0, fontSize = 8.sp, lineHeight = 10.sp)
                }
                footBadges.take(3).forEach { b ->
                    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0x18FFFFFF))
                        .padding(horizontal = 3.dp, vertical = 1.dp)) {
                        Text(b, color = ZW_T2, fontSize = 7.sp, lineHeight = 9.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZwBadge(sihua: String, overlay: Boolean, label: String?) {
    val i = shIdx(sihua); val c = SH_COLORS[i]
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(SH_BG[i])
            .border(0.5.dp, c.copy(alpha = if (overlay) 0.6f else 0.7f), RoundedCornerShape(3.dp))
            .padding(horizontal = 2.dp)) {
        if (overlay && label != null) Text(label, color = c.copy(0.75f), fontSize = 6.5.sp)
        Text(sihua, color = c, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
    }
}

private fun starBrightColor(b: String) = when (b) {
    "庙", "旺" -> STAR_BRIGHT
    "陷", "不" -> STAR_DIM
    else -> STAR_NORMAL
}

// ===== 中宫详情（mingyu：命盘/生肖/农历/时辰/四柱/当前 + 命主身主五行局命宫身宫 pill）=====
@Composable
private fun ZwCenterInfo(r: Ziwei.Result, dxRange: Pair<Int, Int>?, limitLabel: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 6.dp)) {
        // 顶部：当前时限 + 年龄
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(limitLabel, color = PINK, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            val birthYear = r.solarDate.substringBefore("-").toIntOrNull() ?: Year.now().value
            val age = (Year.now().value - birthYear + 1).coerceAtLeast(1)
            Text("${age}岁", color = ZW_T2, fontSize = 9.sp)
        }
        Spacer(Modifier.height(4.dp))
        // 命盘 / 生肖 / 农历 / 时辰 / 四柱 / 当前（参考图中宫字段）
        val solarParts = r.solarDate.split("-")
        val solarCn = if (solarParts.size == 3)
            "${solarParts[0]}年${solarParts[1].toIntOrNull() ?: 1}月${solarParts[2].toIntOrNull() ?: 1}日" else r.solarDate
        val gz = r.chineseDate.split(' ')
        val lines = listOf(
            "命盘" to "当前命盘 · ${if (r.isMan) "男" else "女"}",
            "生肖" to "${r.zodiac} · ${r.sign}  阳历 ${r.solarDate}",
            "农历" to lunarCn(r).removePrefix("农历"),
            "时辰" to r.timeRange,
            "四柱" to gz.joinToString(" "),
            "当前" to r.solarDate
        )
        lines.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth()) {
                Text(k, color = ZW_FAINT, fontSize = 8.5.sp, modifier = Modifier.width(30.dp))
                Text(v, color = ZW_T1, fontSize = 8.5.sp, modifier = Modifier.weight(1f), maxLines = 1)
            }
            Spacer(Modifier.height(1.dp))
        }
        Spacer(Modifier.weight(1f))
        // 底部 pill：命主 / 身主 / 五行局 / 命宫 / 身宫（两列）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ZwCenterPill("命主 ${r.soul}")
                ZwCenterPill(r.juName)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ZwCenterPill("身主 ${r.body}")
                ZwCenterPill("命宫 ${r.ebSoul} · 身宫 ${r.ebBody}")
            }
        }
    }
}

@Composable
private fun ZwCenterPill(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Color(0x12FFFFFF)).border(1.dp, ZW_BORDER, RoundedCornerShape(12.dp))
        .padding(horizontal = 7.dp, vertical = 4.dp)) {
        Text(text, color = ZW_T0, fontSize = 8.5.sp, maxLines = 1)
    }
}

// ===== TimeNav =====
@Composable
private fun ZwTimeNav(
    view: ZwView, year: Int, dxRange: Pair<Int, Int>?, overlay: Map<String, String>,
    overlayGanName: String, onView: (ZwView) -> Unit, onYear: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(ZW_SURFACE).border(1.dp, ZW_BORDER, RoundedCornerShape(10.dp)).padding(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ZwTab("本命", view == ZwView.MINGPAN, Modifier.weight(1f)) { onView(ZwView.MINGPAN) }
            Spacer(Modifier.width(4.dp))
            ZwTab(if (dxRange != null) "大限 ${dxRange.first}-${dxRange.second}" else "大限",
                view == ZwView.DAXIAN, Modifier.weight(1.4f)) { onView(ZwView.DAXIAN) }
            Spacer(Modifier.width(4.dp))
            // 流年 + 年份切换
            Row(Modifier.weight(1.6f).clip(RoundedCornerShape(8.dp))
                .background(if (view == ZwView.LIUNIAN) PINK_BG else Color.Transparent)
                .border(1.dp, if (view == ZwView.LIUNIAN) PINK.copy(0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                .padding(horizontal = 2.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("流年", color = if (view == ZwView.LIUNIAN) ZW_GOLD else ZW_FAINT,
                    fontSize = 9.sp, modifier = Modifier.clickable { onView(ZwView.LIUNIAN) })
                Spacer(Modifier.width(3.dp))
                Text("‹", color = ZW_FAINT, fontSize = 12.sp, modifier = Modifier.clickable { onYear(year - 1); onView(ZwView.LIUNIAN) })
                Text("$year", color = if (view == ZwView.LIUNIAN) ZW_GOLD else ZW_FAINT,
                    fontSize = 9.sp, modifier = Modifier.clickable { onView(ZwView.LIUNIAN) })
                Text("›", color = ZW_FAINT, fontSize = 12.sp, modifier = Modifier.clickable { onYear(year + 1); onView(ZwView.LIUNIAN) })
            }
        }
        if (view != ZwView.MINGPAN && overlay.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 5.dp).padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                val prefix = if (view == ZwView.DAXIAN) "大限" else "$year"
                Text("$prefix · $overlayGanName 四化：", color = ZW_FAINT, fontSize = 8.5.sp)
                listOf("禄", "权", "科", "忌").forEach { sh ->
                    val star = overlay.entries.firstOrNull { it.value == sh }?.key
                    if (star != null) {
                        Text("${star}化$sh", color = SH_COLORS[shIdx(sh)], fontSize = 8.5.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZwTab(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(text, color = if (active) ZW_GOLD else ZW_FAINT, fontSize = 9.5.sp,
        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
        textAlign = TextAlign.Center, modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(if (active) PINK_BG else Color.Transparent)
            .border(1.dp, if (active) PINK.copy(0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(vertical = 5.dp))
}

// ===== 底部四级联动运限表（严格对照 mingyu ZiweiFortuneSelector.tsx）=====
// 大限 → 流年 → 流月 → 流日 每级选中都会重算下级选项（对齐 useZiweiFortuneOptionsWorker）。
// 四行结构：竖排维度标签+竖线 | 横排格子(可滚) | 选中高亮。
@Composable
private fun ZwBottomTable(
    r: Ziwei.Result, sel: ZwSel?, birthYear: Int, limitLabel: String, onSelect: (ZwSel) -> Unit
) {
    val nowYear = java.time.Year.now().value
    val periods: List<ZiweiFortune.Period> = remember(r) { ZiweiFortune.buildPeriods(r) }
    // 默认选中「当前虚岁」所在的大限/童限
    val birthY = r.solarDate.substringBefore("-").toIntOrNull() ?: nowYear
    val curAge = (nowYear - birthY + 1).coerceAtLeast(1)
    val initialPeriod = periods.indexOfFirst { curAge in it.startAge..it.endAge }.coerceAtLeast(0)

    var pIdx by remember(r) { mutableStateOf(initialPeriod) }
    val period: ZiweiFortune.Period? = periods.getOrNull(pIdx) ?: periods.firstOrNull()

    val years: List<ZiweiFortune.Option> = remember(r, period) {
        if (period != null) ZiweiFortune.buildYears(r, period) else emptyList()
    }
    // 默认流年 = 当前虚岁所在项
    var yDate by remember(r, period) {
        mutableStateOf(years.firstOrNull { it.age == curAge }?.dateStr ?: years.firstOrNull()?.dateStr ?: "")
    }
    val year: ZiweiFortune.Option? = years.firstOrNull { it.dateStr == yDate } ?: years.firstOrNull()

    val months: List<ZiweiFortune.Option> = remember(r, year) {
        if (year != null) ZiweiFortune.buildMonths(r, year) else emptyList()
    }
    var mDate by remember(r, year) { mutableStateOf(months.firstOrNull()?.dateStr ?: "") }
    val month: ZiweiFortune.Option? = months.firstOrNull { it.dateStr == mDate } ?: months.firstOrNull()

    val days: List<ZiweiFortune.Option> = remember(r, month) {
        if (month != null) ZiweiFortune.buildDays(r, month) else emptyList()
    }
    var dDate by remember(r, month) { mutableStateOf(days.firstOrNull()?.dateStr ?: "") }

    val tagW = 24.dp
    val cardW = 52.dp
    val rowH = 72.dp
    val pScroll = rememberScrollState(); val yScroll = rememberScrollState()
    val mScroll = rememberScrollState(); val dScroll = rememberScrollState()

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .border(1.dp, ZW_BORDER, RoundedCornerShape(12.dp)).background(ZW_SURFACE)) {

        // —— 大限行：童限(每年) + 大限(每宫十年)，title + age + MM/DD 胶囊 ——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("大限", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(pScroll),
                verticalAlignment = Alignment.CenterVertically) {
                periods.forEachIndexed { i, p ->
                    val active = i == pIdx
                    ZwLimitCard(p.label,
                        if (p.startAge == p.endAge) "${p.startAge}岁" else "${p.startAge}-${p.endAge}岁",
                        p.dateStr.substring(5), active, cardW, rowH) {
                        pIdx = i
                        // 选大限 → 流年重置为该项首年（对齐 setSelectedYearDateStr）
                        val newYears = ZiweiFortune.buildYears(r, p)
                        yDate = newYears.firstOrNull()?.dateStr ?: ""
                        mDate = ""; dDate = ""
                        onSelect(ZwSel(ziToIdx(r.ebSoul), "${p.label} ${p.startAge}-${p.endAge}岁 · ${p.palaceName}", ZwSelType.DECADAL, r.yearGan))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
        ZwThinDivider()
        // —— 流年行：年份 + N岁 + 竖排干支（横排，选中该大限覆盖的整段）——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("流年", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(yScroll),
                verticalAlignment = Alignment.CenterVertically) {
                years.forEach { o ->
                    val active = o.dateStr == yDate
                    ZwGzCell(o.head, o.sub, o.ganZhi, active, cardW, rowH) {
                        yDate = o.dateStr
                        // 选流年 → 流月重置为该年首月
                        val newMonths = ZiweiFortune.buildMonths(r, o)
                        mDate = newMonths.firstOrNull()?.dateStr ?: ""
                        dDate = ""
                        onSelect(ZwSel(ziToIdx(o.ganZhi[1].toString()), "流年 ${o.head} · ${o.ganZhi}（${o.sub}）", ZwSelType.YEARLY, o.ganZhi[0].toString()))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
        ZwThinDivider()
        // —— 流月行：N月 + MM/DD + 竖排干支 ——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("流月", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(mScroll),
                verticalAlignment = Alignment.CenterVertically) {
                months.forEach { o ->
                    val active = o.dateStr == mDate
                    ZwGzCell(o.head, o.sub, o.ganZhi, active, cardW, rowH) {
                        mDate = o.dateStr
                        // 选流月 → 流日重置为该月首日
                        val newDays = ZiweiFortune.buildDays(r, o)
                        dDate = newDays.firstOrNull()?.dateStr ?: ""
                        onSelect(ZwSel(ziToIdx(o.ganZhi[1].toString()), "${o.head} · ${o.sub} · ${o.ganZhi}", ZwSelType.YEARLY, o.ganZhi[0].toString()))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
        ZwThinDivider()
        // —— 流日行：日 + MM/DD + 竖排干支 ——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("流日", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(dScroll),
                verticalAlignment = Alignment.CenterVertically) {
                days.forEach { o ->
                    val active = o.dateStr == dDate
                    ZwGzCell(o.head, o.sub, o.ganZhi, active, cardW, rowH) {
                        dDate = o.dateStr
                        onSelect(ZwSel(ziToIdx(o.ganZhi[1].toString()), "流日 ${o.sub} · ${o.ganZhi}", ZwSelType.YEARLY, o.ganZhi[0].toString()))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
    }
}

// 竖排维度标签 + 右侧竖线（对照参考图：无方块底，纯竖排字 + 粉色细竖线）
@Composable
private fun ZwRowTagV(text: String, h: androidx.compose.ui.unit.Dp, w: androidx.compose.ui.unit.Dp) {
    Row(Modifier.width(w).height(h), verticalAlignment = Alignment.CenterVertically) {
        Text(text.chunked(1).joinToString("\n"), color = ZW_T2, fontSize = 12.sp,
            fontWeight = FontWeight.Medium, lineHeight = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.width(3.dp))
        Box(Modifier.width(1.dp).height(h * 0.62f).background(PINK.copy(0.45f)))
    }
}

@Composable
private fun ZwThinDivider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZW_BORDER))
}

// 大限卡片：标题(童限/大限) + 岁数小字 + 胶囊日期；选中整块淡紫高亮
@Composable
private fun ZwLimitCard(
    title: String, age: String, date: String, active: Boolean,
    w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, onClick: () -> Unit
) {
    Column(Modifier.width(w).height(h).clip(RoundedCornerShape(10.dp))
        .background(if (active) PINK.copy(0.16f) else Color.Transparent)
        .border(1.dp, if (active) PINK.copy(0.5f) else Color.Transparent, RoundedCornerShape(10.dp))
        .clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = if (active) PINK else ZW_T0, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, lineHeight = 14.sp)
        Text(age, color = ZW_FAINT, fontSize = 8.5.sp, textAlign = TextAlign.Center, lineHeight = 10.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(PINK.copy(0.22f)).padding(horizontal = 2.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center) {
            Text(date, color = if (active) PINK else ZW_T1, fontSize = 10.sp, maxLines = 1)
        }
    }
}

// 流月/流日格子：头部(月/日) + 小字日期 + 竖排天干地支
@Composable
private fun ZwGzCell(
    head: String, date: String, gz: String, active: Boolean,
    w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, onClick: () -> Unit
) {
    Column(Modifier.width(w).height(h).clip(RoundedCornerShape(10.dp))
        .background(if (active) PINK.copy(0.14f) else Color.Transparent)
        .border(1.dp, if (active) PINK.copy(0.55f) else Color.Transparent, RoundedCornerShape(10.dp))
        .clickable(onClick = onClick).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(head, color = ZW_T0, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(date, color = ZW_FAINT, fontSize = 8.sp, maxLines = 1)
        Spacer(Modifier.weight(1f))
        if (gz.length >= 2) {
            Text(gz[0].toString(), color = ZW_T1, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
            Text(gz[1].toString(), color = ZW_T1, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun ZwGoldNavBar(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(ZW_SURFACE)) {
        Row(Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Text("‹", color = ZW_GOLD, fontSize = 30.sp)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(title, color = ZW_GOLD, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(40.dp))
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZW_BORDER))
    }
}

private val ZHI_CN = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
private val GAN_CN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
private fun ziToIdx(zhi: String) = ZHI_CN.indexOf(zhi).coerceAtLeast(0)
private fun zhiToRC(z: Int): Pair<Int, Int> = when (z) {
    5 -> 0 to 0; 6 -> 0 to 1; 7 -> 0 to 2; 8 -> 0 to 3
    4 -> 1 to 0; 9 -> 1 to 3
    3 -> 2 to 0; 10 -> 2 to 3
    2 -> 3 to 0; 1 -> 3 to 1; 0 -> 3 to 2; 11 -> 3 to 3
    else -> 0 to 0
}
private fun lunarCn(r: Ziwei.Result): String {
    val parts = Regex("(\\d+)年(\\d+)月(\\d+)日").find(r.lunarDate)?.destructured
    return if (parts != null) {
        val (y, m, d) = parts
        val yCn = y.map { LunarCalendar.cnNumber(it - '0') }.joinToString("")
        "农历${yCn}年${LunarCalendar.cnNumber(m.toInt())}月${LunarCalendar.cnNumber(d.toInt())}日"
    } else r.lunarDate
}
private fun ganZhiOfYear(year: Int): String =
    GAN_CN[Math.floorMod(year - 4, 10)] + ZHI_CN[Math.floorMod(year - 4, 12)]

private fun ziweiFaceText(r: Ziwei.Result) = buildString {
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
        appendLine("${p.name}[${p.gan}${p.zhi}]${if (p.isBody) "(身宫)" else ""} 大限${p.decadal.first}-${p.decadal.second}：${stars}${if (adj.isNotEmpty()) " | $adj" else ""} | 长生${p.changsheng} 博士${p.boshi} 将前${p.jiangqian} 岁前${p.suiqian}")
    }
}

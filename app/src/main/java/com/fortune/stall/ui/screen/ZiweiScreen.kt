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
fun ZiweiCastScreen(db: FortuneDb, onBack: () -> Unit, onAskAi: (String) -> Unit = {}) {
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
        ZiweiDetailScreen(result!!, onBack, onAskAi, onSchoolChange = { school ->
            com.fortune.stall.util.ZiweiSettings.setSchool(school)
            lastArgs?.let { (dt, man, lon) ->
                result = Ziwei.paipan(dt, man, -1, lon, false)
            }
        })
    }
}

// ===== 深色金国风配色（对照参考项目 dark 主题的 --t-* token）=====
private val ZW_BG        = Color(0xFF020810)          // --t-bg / --bg-0  深蓝黑底
private val ZW_SURFACE   = Color(0xFF0A1020)          // --t-surface / --bg-1
private val ZW_CARD      = Color(0xFF141E30)          // --bg-2
private val ZW_BORDER    = Color(0x1FFFFFFF)          // --t-border (white 12%)
private val ZW_BORDER_H  = Color(0x33FFFFFF)          // 稍亮边框
private val ZW_GOLD      = Color(0xFFD4A843)          // --t-gold 墨金
private val ZW_GOLD_DIM  = Color(0xFFE8C060)          // --ac-dim
private val ZW_T0        = Color(0xFFE8EEF6)          // --tx-0
private val ZW_T1        = Color(0xFFDCE4F0)          // --tx-1
private val ZW_T2        = Color(0xFF9DB0D0)          // --tx-2
private val ZW_FAINT     = Color(0xFF6A7A96)          // --t-faint

// 主星按亮度分档（参考：bright=amber-300 / normal=amber-500 / dim=amber-700）
private val STAR_BRIGHT  = Color(0xFFFCD34D)
private val STAR_NORMAL  = Color(0xFFF59E0B)
private val STAR_DIM     = Color(0xFFB45309)
private val STAR_LUCKY   = Color(0xB20EA5E9)          // 吉星 sky-500/70
private val STAR_SHA     = Color(0x99EF4444)          // 煞星 red-500/60
private val STAR_ADJ     = Color(0x806A7A96)          // 杂曜 淡

// 四化语义色：禄绿 权蓝 科黄 忌红（dark 下用 400 档）
private val SH_COLORS = listOf(Color(0xFF4ADE80), Color(0xFF60A5FA), Color(0xFFFACC15), Color(0xFFF87171))
private val SH_BG     = listOf(Color(0x1A4ADE80), Color(0x1A60A5FA), Color(0x1AFACC15), Color(0x1AF87171))
private fun shIdx(s: String) = when (s) { "禄" -> 0; "权" -> 1; "科" -> 2; else -> 3 }

// 选中/三方/大限底色
private val SEL_BG      = Color(0x2E2563EB)           // rgba(37,99,235,0.18)
private val SEL_LINE    = Color(0xB32563EB)
private val SANFANG_BG  = Color(0x172563EB)           // rgba(37,99,235,0.09)
private val DX_BG       = Color(0x14933EA)            // rgba(147,51,234,0.08)
private val DX_STRIP    = Color(0x80933EA)
private val MING_TINT   = Color(0x0AD4A843)           // rgba(212,168,67,0.04)
private val GRID_LINE   = Color(0x24FFFFFF)
private val SF_STROKE   = Color(0x8C2563EB)           // 三方四正线 rgba(37,99,235,0.55)
private val FLY_STROKE  = Color(0x66B0BEC8)           // 飞星落宫线

private enum class ZwView { MINGPAN, DAXIAN, LIUNIAN }
// 选中信息：地支绝对序号(0子..11亥) + 文案 + 来源类型 + 天干(算四化)
private enum class ZwSelType { PALACE, DECADAL, YEARLY }
private data class ZwSel(val zhi: Int, val label: String, val type: ZwSelType, val gan: String)
private data class FlyAnno(val text: String, val color: Color)

@Composable
private fun ZiweiDetailScreen(
    r: Ziwei.Result, onBack: () -> Unit, onAskAi: (String) -> Unit,
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

    Box(Modifier.fillMaxSize().background(ZW_BG)) {
        Column(Modifier.fillMaxSize()) {
            ZwGoldNavBar("紫微斗数", onBack)
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).padding(horizontal = 8.dp)) {
                Spacer(Modifier.height(8.dp))
                // TimeNav：本命 / 大限 / 流年 + 年份切换
                ZwTimeNav(view, liunianYear, dxRange, overlay, overlayGanName,
                    onView = { view = it }, onYear = { liunianYear = it })
                Spacer(Modifier.height(10.dp))
                // 标题
                Text("Z I   W E I   D O U   S H U", color = ZW_FAINT, fontSize = 8.sp,
                    letterSpacing = 3.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("${r.solarDate.substringBefore('-')} · 紫微斗数命盘", color = ZW_GOLD,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (r.solarInfo.isNotBlank()) {
                    Text(r.solarInfo, color = ZW_T2, fontSize = 8.5.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Center)
                }
                // 工具行：亮度流派切换 + 问 AI 解读（参考项目核心 13 维解读 + 流派切换）
                Spacer(Modifier.height(6.dp))
                val curSchool = com.fortune.stall.util.ZiweiSettings.brightnessSchool.value
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.fortune.stall.engine.jishiyu.ZiweiSchools.NAMES.forEach { (label, key) ->
                        Box(Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (curSchool == key) ZW_GOLD.copy(0.22f) else Color(0x10FFFFFF))
                            .border(1.dp, if (curSchool == key) ZW_GOLD else GRID_LINE, RoundedCornerShape(14.dp))
                            .clickable { if (curSchool != key) onSchoolChange(key) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)) {
                            Text(label, color = if (curSchool == key) ZW_GOLD else ZW_FAINT, fontSize = 9.5.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(ZW_GOLD.copy(alpha = 0.16f))
                    .border(1.dp, ZW_GOLD.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { onAskAi(ziweiFaceText(r)) }
                    .padding(horizontal = 18.dp, vertical = 6.dp)) {
                    Text("✦ 问 AI 解读命盘（13 维）", color = ZW_GOLD, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(6.dp))

                // 4x4 宫格 + 覆盖连线
                val lineW = 1.dp
                val rowH = 118.dp
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().border(lineW, GRID_LINE, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))) {
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[5], 5, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[6], 6, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[7], 7, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[8], 8, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                        }
                        ZwGridDivider(lineW)
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[4], 4, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            Box(Modifier.weight(2f).border(lineW, GRID_LINE)) { ZwCenterTop(r) }
                            ZwCell(byZhi(r)[9], 9, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                        }
                        ZwGridDivider(lineW)
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[3], 3, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            Box(Modifier.weight(2f).border(lineW, GRID_LINE)) { ZwCenterBottom(r, dxRange) }
                            ZwCell(byZhi(r)[10], 10, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                        }
                        ZwGridDivider(lineW)
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            ZwCell(byZhi(r)[2], 2, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[1], 1, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[0], 0, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                            ZwCell(byZhi(r)[11], 11, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, Modifier.weight(1f)) { sel = it }
                        }
                    }
                    if (sel != null) {
                        Canvas(Modifier.fillMaxSize()) {
                            val border = lineW.toPx(); val rh = rowH.toPx(); val div = lineW.toPx()
                            val cw = (size.width - 2 * border) / 4f
                            fun center(z: Int): FloatArray {
                                val (rr, cc) = zhiToRC(z)
                                return floatArrayOf(border + cc * cw + cw / 2f, border + rr * (rh + div) + rh / 2f)
                            }
                            val z = sel!!.zhi
                            val p0 = center(z); val p1 = center((z + 6) % 12)
                            val p2 = center((z + 4) % 12); val p3 = center((z + 8) % 12)
                            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                            val style = Stroke(width = 1.4.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round)
                            // 对宫直线 + 三合三角形
                            drawPath(Path().apply { moveTo(p0[0], p0[1]); lineTo(p1[0], p1[1]) }, SF_STROKE, style = style)
                            drawPath(Path().apply { moveTo(p0[0], p0[1]); lineTo(p2[0], p2[1]); lineTo(p3[0], p3[1]); lineTo(p0[0], p0[1]) }, SF_STROKE, style = style)
                            listOf(p0, p1, p2, p3).forEachIndexed { i, p ->
                                drawCircle(SF_STROKE.copy(alpha = if (i == 0) 0.95f else 0.6f),
                                    radius = 3.dp.toPx(), center = Offset(p[0], p[1]))
                            }
                            // 飞星落宫连线
                            flyMap.keys.forEach { tz ->
                                val c = center(tz)
                                drawPath(Path().apply { moveTo(p0[0], p0[1]); lineTo(c[0], c[1]) }, FLY_STROKE,
                                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f))))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                // 图例
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

                // 自化总览 + 选中宫来因宫（借鉴 ziwei-ds sihua.ts 自化/来因宫）
                if (selfSihuaMap.isNotEmpty() || sel != null) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp))
                        .background(ZW_SURFACE).border(1.dp, ZW_BORDER, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Text("四化 · 自化与来因", color = ZW_GOLD, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                                // 该宫的长生/博士/将前/岁前十二神（借鉴 ziwei-ds shier-shen.ts）
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

                Spacer(Modifier.height(12.dp))
                // 底部：大限 + 流年联动表（深色）
                ZwBottomTable(r, sel) { sel = it }
                Spacer(Modifier.height(20.dp))
            }
        }
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
        else -> ZW_BG
    }
    val borderColor = when {
        active -> SEL_LINE
        isSanFang -> Color(0x662563EB)
        else -> GRID_LINE
    }
    Box(modifier.background(bg).border(1.dp, borderColor)
        .clickable {
            if (active) Unit else onSelect(ZwSel(z, "选中 ${gan}${ZHI_CN[z]} · ${p?.name ?: ""}", ZwSelType.PALACE, gan))
        }
        .padding(horizontal = 3.dp, vertical = 2.dp)) {
        if (isCurrentDx) Box(Modifier.width(3.dp).fillMaxSize().background(DX_STRIP))
        if (p == null) return@Box
        Column(Modifier.fillMaxSize()) {
            // 宫名行（右上大限年龄）
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(p.name, color = when { isMing -> ZW_GOLD; isShen -> STAR_LUCKY; else -> ZW_FAINT },
                    fontSize = 10.sp, fontWeight = if (isMing || isShen) FontWeight.Medium else FontWeight.Normal,
                    lineHeight = 12.sp)
                if (isMing) Text("命", color = ZW_GOLD, fontSize = 7.sp, modifier = Modifier.padding(start = 2.dp))
                if (isShen) Text("身", color = STAR_LUCKY, fontSize = 7.sp, modifier = Modifier.padding(start = 2.dp))
                Spacer(Modifier.weight(1f))
                Text("${p.decadal.first}-${p.decadal.second}", color = if (isCurrentDx) Color(0xFFC084FC) else ZW_FAINT,
                    fontSize = 7.5.sp, lineHeight = 10.sp)
            }
            // 干支
            Text(gan + ZHI_CN[z], color = ZW_FAINT, fontSize = 8.sp, lineHeight = 10.sp)
            // 主星
            val majors = p.major
            if (majors.isEmpty()) Text("空宫", color = ZW_FAINT.copy(0.5f), fontSize = 9.sp)
            majors.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, color = starBrightColor(s.brightness), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, lineHeight = 13.sp)
                    if (s.mutagen.isNotEmpty()) ZwBadge(s.mutagen, false, null)
                    overlay[s.name]?.let { ZwBadge(it, true, overlayLabel) }
                }
            }
            // 吉星
            val luckies = p.minor.filter { it.type != "tough" }
            if (luckies.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    luckies.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, color = STAR_LUCKY, fontSize = 8.5.sp, lineHeight = 10.sp)
                            if (s.mutagen.isNotEmpty()) ZwBadge(s.mutagen, false, null)
                            overlay[s.name]?.let { ZwBadge(it, true, overlayLabel) }
                        }
                    }
                }
            }
            // 煞星
            val shas = p.minor.filter { it.type == "tough" }
            if (shas.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    shas.forEach { s -> Text(s.name, color = STAR_SHA, fontSize = 8.5.sp, lineHeight = 10.sp) }
                }
            }
            // 杂曜（淡色）
            if (p.adj.isNotEmpty()) {
                Text(p.adj.take(6).joinToString(" "), color = STAR_ADJ, fontSize = 7.sp, lineHeight = 9.sp,
                    maxLines = 1)
            }
            // 宫底神煞行：长生 + 博士十二神（参考项目金标准盘宫底第一行）
            Spacer(Modifier.weight(1f))
            val shenLine = listOfNotNull(
                p.changsheng.takeIf { it.isNotBlank() },
                p.boshi.takeIf { it.isNotBlank() }
            ).joinToString("·")
            if (shenLine.isNotBlank()) {
                Text(shenLine, color = Color(0xFF8A7A5C), fontSize = 7.sp, lineHeight = 8.sp, maxLines = 1)
            }
            // 飞星落宫标注
            val annos = fly[z]
            if (!annos.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    annos.forEach { Text(it.text, color = it.color, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
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

// ===== ☯ 中宫 =====
@Composable
private fun ZwCenterTop(r: Ziwei.Result) {
    Column(Modifier.fillMaxSize().background(ZW_BG).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("☯", color = ZW_GOLD.copy(0.18f), fontSize = 44.sp, lineHeight = 44.sp)
        Text("紫微斗数", color = ZW_GOLD, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp)
        Text("命宫 ${r.ebSoul}   身宫 ${r.ebBody}", color = ZW_T2, fontSize = 8.sp)
        Text(r.juName, color = ZW_GOLD.copy(0.75f), fontSize = 8.sp)
    }
}

@Composable
private fun ZwCenterBottom(r: Ziwei.Result, dxRange: Pair<Int, Int>?) {
    Column(Modifier.fillMaxSize().background(ZW_BG).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (dxRange != null) {
            Column(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x14933EA))
                .border(0.5.dp, Color(0x3A933EA), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("当前大限", color = Color(0xFFC084FC).copy(0.8f), fontSize = 7.sp)
                Text("${dxRange.first}–${dxRange.second} 岁", color = Color(0xFFC084FC), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        val lunar = lunarCn(r).removePrefix("农历")
        Text(lunar, color = ZW_FAINT, fontSize = 7.5.sp, modifier = Modifier.padding(top = 4.dp))
        Text("${r.chineseDate.split(' ').getOrNull(0) ?: ""} 年", color = ZW_FAINT, fontSize = 7.5.sp)
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
                .background(if (view == ZwView.LIUNIAN) Color(0x1AD4A843) else Color.Transparent)
                .border(1.dp, if (view == ZwView.LIUNIAN) Color(0x40D4A843) else Color.Transparent, RoundedCornerShape(8.dp))
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
            .background(if (active) Color(0x1AD4A843) else Color.Transparent)
            .border(1.dp, if (active) Color(0x40D4A843) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(vertical = 5.dp))
}

// ===== 底部大限/流年联动表（深色）—— 流年按每格大限分段 =====
@Composable
private fun ZwBottomTable(r: Ziwei.Result, sel: ZwSel?, onSelect: (ZwSel) -> Unit) {
    val birthYear = r.solarDate.substringBefore("-").toIntOrNull() ?: 2000
    val sharedScroll = rememberScrollState()
    val yearCellW = 26.dp              // 单个流年格宽
    val decadeW = yearCellW * 10       // 一格大限 = 10 个流年格宽（大限十年一宫）
    val dxH = 40.dp
    val yrH = 44.dp
    // 12 个大限宫，按起运岁排序
    val dxList = r.palaces.sortedBy { it.decadal.first }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .border(1.dp, ZW_BORDER, RoundedCornerShape(8.dp)).background(ZW_SURFACE)) {
        Row(Modifier.fillMaxWidth()) {
            // 左侧固定标签列
            Column(Modifier.width(30.dp)) {
                Box(Modifier.fillMaxWidth().height(dxH).background(ZW_CARD).border(0.5.dp, ZW_BORDER),
                    contentAlignment = Alignment.Center) {
                    Text("大\n限", color = ZW_GOLD, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
                }
                Box(Modifier.fillMaxWidth().height(yrH).background(ZW_CARD).border(0.5.dp, ZW_BORDER),
                    contentAlignment = Alignment.Center) {
                    Text("流\n年", color = ZW_GOLD, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
                }
            }
            // 大限 / 流年按段对齐，共用横向滚动：滑动大限，其下流年同步跟动
            Column(Modifier.weight(1f).horizontalScroll(sharedScroll)) {
                // 大限行：12 宫，每宫占 decadeW，与下方 10 个流年对齐
                Row(Modifier.height(dxH)) {
                    dxList.forEach { p ->
                        val z = ziToIdx(p.zhi)
                        val active = sel?.type == ZwSelType.DECADAL && sel.zhi == z
                        Box(Modifier.width(decadeW).border(0.5.dp, ZW_BORDER)
                            .background(if (active) Color(0x3A933EA) else Color(0x0A933EA))
                            .clickable { onSelect(ZwSel(z, "大限 ${p.decadal.first}-${p.decadal.second} · ${Ziwei.gongNameAt(r, z)}${p.gan}${p.zhi}", ZwSelType.DECADAL, p.gan)) }
                            .padding(2.dp), contentAlignment = Alignment.Center) {
                            Text("大限 ${p.decadal.first}-${p.decadal.second}岁 · ${p.gan}${p.zhi} ${Ziwei.gongNameAt(r, z)}",
                                color = if (active) Color(0xFFD8B4FE) else ZW_GOLD, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
                // 流年行：每个大限宫下挂它管辖的 10 个流年（最长 = 每格大限）
                Row(Modifier.height(yrH)) {
                    dxList.forEach { p ->
                        val zDx = ziToIdx(p.zhi)
                        val startAge = p.decadal.first
                        val dxActive = sel?.type == ZwSelType.DECADAL && sel.zhi == zDx
                        repeat(10) { i ->
                            val age = startAge + i
                            val year = birthYear + age - 1
                            val gz = ganZhiOfYear(year)
                            val zY = ziToIdx(gz[1].toString())
                            val yActive = sel?.type == ZwSelType.YEARLY && sel.label.contains("$year")
                            Column(Modifier.width(yearCellW).border(0.5.dp, ZW_BORDER)
                                .background(when {
                                    yActive -> Color(0x3AD4A843)
                                    dxActive || (sel?.type == ZwSelType.YEARLY && sel.zhi == zY) -> Color(0x12933EA)
                                    else -> Color.Transparent
                                })
                                .clickable { onSelect(ZwSel(zY, "流年 $year · $gz（${age}岁）", ZwSelType.YEARLY, gz[0].toString())) }
                                .padding(1.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(gz, color = ganWuXingColor(gz[0].toString()), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${age}岁", color = ZW_FAINT, fontSize = 7.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ganWuXingColor(gan: String) = when (gan) {
    "甲" -> Color(0xFF5BAE72); "乙" -> Color(0xFF86C79A)
    "丙" -> Color(0xFFF07A6A); "丁" -> Color(0xFFE86A5A)
    "戊" -> Color(0xFFD4AE5A); "己" -> Color(0xFFC4A04E)
    "庚" -> Color(0xFFE6A85A); "辛" -> Color(0xFFD89A4A)
    "壬" -> Color(0xFF5B9BF0); "癸" -> Color(0xFF6AA8F0)
    else -> ZW_T1
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

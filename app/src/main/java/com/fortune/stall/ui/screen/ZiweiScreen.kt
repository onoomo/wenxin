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
import com.fortune.stall.engine.jishiyu.ZiweiCenter
import com.fortune.stall.engine.jishiyu.ZiweiFortune
import com.fortune.stall.engine.jishiyu.ZiweiData
import com.fortune.stall.engine.jishiyu.JishiyuBase
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
        ZiweiCastForm(onBack = onBack) { f ->
            // 真太阳时开关开启且填了经度才做经度+均时差校正
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
private val FLY_TARGET_BG = Color(0x264ADE80)        // 点击宫位四化落宫高亮（视频：点一宫，落宫亮起）
private val FLY_TARGET_LINE = Color(0xA64ADE80)      // 落宫放射连线

private enum class ZwView { MINGPAN, DAXIAN, LIUNIAN }
// 选中信息：地支绝对序号(0子..11亥) + 文案 + 来源类型 + 天干(算四化)
private enum class ZwSelType { PALACE, DECADAL, YEARLY }
private data class ZwSel(val zhi: Int, val label: String, val type: ZwSelType, val gan: String, val level: String = "")
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
    // 默认叠加 7 层飞星（对齐原版图1：每宫一进来就显示 大/小/年/月/日/时/命 多个彩色方块）。
    // 命=本命四化，大=当前大限宫干，年/月/日/时=当前流年天干打底；
    // 点击底部运限行 / 中宫六标签会把对应层更新为该运限的真实天干。
    var flyGans by remember(r) {
        mutableStateOf(mapOf(
            "命" to r.yearGan,
            "大" to dxGan.ifBlank { r.yearGan },
            "小" to r.yearGan,
            "年" to Ziwei.yearGan(nowYear),
            "月" to Ziwei.yearGan(nowYear),
            "日" to Ziwei.yearGan(nowYear),
            "时" to Ziwei.yearGan(nowYear)
        ))
    }
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

    // 多层时限飞星落宫（对齐原版图1彩色方块）：每层天干四化 -> 落宫 -> 宫格显示「时限首字+宫名首字」方块
    // flyGans 常驻「命」层（本命四化），点击底部运限行追加 大/小/年/月/日/时 层
    val flyMap: Map<Int, List<FlyAnno>> = remember(flyGans, r) {
        val m = mutableMapOf<Int, MutableList<FlyAnno>>()
        flyGans.forEach { (level, gan) ->
            val sihua = Ziwei.sihua(gan)
            sihua.forEachIndexed { i, star ->
                if (star.isBlank()) return@forEachIndexed
                val zhi = Ziwei.starZhi(r, star) ?: return@forEachIndexed
                val idx = ziToIdx(zhi)
                val palaceName = r.palaces.firstOrNull { it.zhi == zhi }?.name ?: ""
                // 原版图1：本命层(命)显示两字完整宫名(如"田宅")；其余层显示「时限首字+宫名首字」(如"大田""时兄")
                val anno = FlyAnno(
                    if (level == "命") palaceName.take(2) else level + palaceName.take(1),
                    SH_COLORS[i])
                if (m.getOrPut(idx) { mutableListOf() }.none { it.text == anno.text }) m[idx]!!.add(anno)
            }
        }
        m
    }

    val sanFang = sel?.let { intArrayOf(it.zhi, (it.zhi + 6) % 12, (it.zhi + 4) % 12, (it.zhi + 8) % 12) }

    // 视频联动高亮：点击某个宫 -> 以该宫宫干起四化 -> 落宫高亮 + 从点击宫放射连线
    // 只针对点击宫格（PALACE），底部运限行点击沿用 flyMap 的时限方块，互不覆盖
    val clickFlyTargets: Set<Int> = remember(sel, r) {
        val s = sel
        if (s == null || s.type != ZwSelType.PALACE || s.gan.isBlank()) emptySet()
        else Ziwei.sihua(s.gan).mapNotNull { star ->
            if (star.isBlank()) null else Ziwei.starZhi(r, star)?.let { ziToIdx(it) }
        }.toSet()
    }

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
                Spacer(Modifier.height(2.dp))
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

                // 4x4 分离圆角宫格卡 + 三方四正覆盖连线（mingyu 传统盘面）
                val rowH = 132.dp
                val cellPad = 2.dp
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        // 第一行：巳 午 未 申
                        Row(Modifier.fillMaxWidth()) {
                            listOf(5, 6, 7, 8).forEach { z ->
                                ZwCell(byZhi(r)[z], z, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, clickFlyTargets,
                                    Modifier.weight(1f).height(rowH).padding(cellPad)) { sel = it }
                            }
                        }
                        // 中段：左(辰/卯) + 中宫(跨2行) + 右(酉/戌)
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                ZwCell(byZhi(r)[4], 4, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, clickFlyTargets,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                                ZwCell(byZhi(r)[3], 3, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, clickFlyTargets,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                            }
                            Box(Modifier.weight(2f).height(rowH * 2).padding(cellPad)) {
                                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    .background(ZW_SURFACE).border(1.dp, ZW_BORDER, RoundedCornerShape(10.dp))) {
                                    ZwCenterInfo(r, dxRange, limitLabel, liunianYear, dxGan,
                                        sel?.level ?: "", onLevel = { level, gan ->
                                            flyGans = flyGans + (level to gan)
                                            sel = ZwSel(sel?.zhi ?: Ziwei.mingZhiIdx(r),
                                                "飞星层: $level", ZwSelType.YEARLY, gan, level)
                                        })
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                ZwCell(byZhi(r)[9], 9, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, clickFlyTargets,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                                ZwCell(byZhi(r)[10], 10, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, clickFlyTargets,
                                    Modifier.fillMaxWidth().height(rowH).padding(cellPad)) { sel = it }
                            }
                        }
                        // 末行：寅 丑 子 亥
                        Row(Modifier.fillMaxWidth()) {
                            listOf(2, 1, 0, 11).forEach { z ->
                                ZwCell(byZhi(r)[z], z, r, sel, flyMap, sanFang, overlay, overlayLabel, dxZhi, clickFlyTargets,
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
                            // 视频联动：点击宫 -> 四化落宫的醒目的放射连线（实线箭头感）
                            clickFlyTargets.forEach { tz ->
                                val c = center(tz)
                                drawPath(Path().apply { moveTo(p0[0], p0[1]); lineTo(c[0], c[1]) }, FLY_TARGET_LINE,
                                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
                                drawCircle(FLY_TARGET_LINE, radius = 2.5.dp.toPx(), center = Offset(c[0], c[1]))
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
                Text("点击宫位看三方四正与飞星", color = ZW_FAINT,
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
                ZwBottomTable(r, sel, birthYear, limitLabel,
                    onLevel = { level, gan -> flyGans = flyGans + (level to gan) }) { sel = it }
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
// 星曜按五行/类别着色（对齐文墨天机原版：每颗星不同颜色）
private fun starNameColor(name: String): Color = when (name) {
    "紫微","天府","太阴","天相" -> Color(0xFF9AD9F5)   // 蓝
    "天机","贪狼","左辅","右弼" -> Color(0xFF8FE3A6)   // 绿
    "太阳" -> Color(0xFFFF9C7A)                        // 橙红
    "武曲","七杀","破军","廉贞" -> Color(0xFFC9A2F5)   // 紫
    "天同","巨门","天梁" -> Color(0xFFF2D06B)          // 黄
    "文昌","文曲","禄存","天马" -> Color(0xFFEFB6D8)   // 粉
    "擎羊","陀罗","火星","铃星","地空","地劫" -> Color(0xFFE88888) // 煞红
    else -> Color(0xFFDCCFE6)
}

// 流年虚岁序列：虚岁1落在出生年支宫，之后逐年顺行（对齐文墨天机流年行口径）
// 出生生肖→地支绝对序号；某宫z的起始虚岁 = ((z - birthIdx) mod 12) + 1
private val ZODIAC_CN = listOf("鼠","牛","虎","兔","龙","蛇","马","羊","猴","鸡","狗","猪")
private fun flowAgesOf(r: Ziwei.Result, z: Int): List<Int> {
    val birthIdx = ZODIAC_CN.indexOf(r.zodiac).coerceAtLeast(0) // 生肖与地支同序：鼠=子(0)..马=午(6)
    val start = ((z - birthIdx) % 12 + 12) % 12 + 1
    return (0 until 5).map { start + 12 * it }
}

@Composable
private fun ZwCell(
    p: Ziwei.Palace?, z: Int, r: Ziwei.Result, sel: ZwSel?, fly: Map<Int, List<FlyAnno>>,
    sanFang: IntArray?, overlay: Map<String, String>, overlayLabel: String?, dxZhi: Int,
    flyTargets: Set<Int> = emptySet(),
    modifier: Modifier, onSelect: (ZwSel) -> Unit
) {
    val active = sel?.zhi == z
    val isCurrentDx = dxZhi == z
    val isMing = z == Ziwei.mingZhiIdx(r)
    val isShen = p?.isBody == true
    val isSanFang = sanFang?.contains(z) == true && !active
    // 点击宫位的四化落宫高亮（视频「点一宫，相关宫亮起」）
    val isFlyTarget = z in flyTargets && !active
    val gan = p?.gan ?: ""

    val bg = when {
        active -> SEL_BG
        isFlyTarget -> FLY_TARGET_BG
        isSanFang -> SANFANG_BG
        isCurrentDx -> DX_BG
        isMing || isShen -> MING_TINT
        else -> ZW_CARD
    }
    val borderColor = when {
        active -> SEL_LINE
        isFlyTarget -> FLY_TARGET_LINE
        isSanFang -> PINK.copy(0.4f)
        else -> ZW_BORDER
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(bg)
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .clickable {
            if (active) Unit else onSelect(ZwSel(z, "选中 ${gan}${ZHI_CN[z]} · ${p?.name ?: ""}", ZwSelType.PALACE, gan))
        }
        .padding(horizontal = 3.dp, vertical = 3.dp)) {
        if (isCurrentDx) Box(Modifier.width(2.5.dp).fillMaxSize().background(DX_STRIP))
        if (p == null) return@Box

        // 图1原版详细宫格：星名竖排多列 → 流年小限 → 大限 → 左下博士/亡神/病符 + 右下长生宫干支 + 宫名红框
        // 身宫青色竖框（右上，竖排"身宫"两字）
        if (isShen) {
            Box(Modifier.align(Alignment.TopEnd)
                .clip(RoundedCornerShape(3.dp))
                .border(0.8.dp, Color(0xFF6FC7C0), RoundedCornerShape(3.dp))
                .background(Color(0x3317131F)).padding(horizontal = 1.dp, vertical = 1.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("身", color = Color(0xFF7FD8CF), fontSize = 7.sp, lineHeight = 8.sp)
                    Text("宫", color = Color(0xFF7FD8CF), fontSize = 7.sp, lineHeight = 8.sp)
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            // ① 星曜区：每颗星竖排两字成列，横排多列；列下方对齐亮度字 + 四化方块（对齐原版图2）
            val starCols = ((p.major + p.minor).map { it.name to it.brightness } +
                p.adj.map { it to (ZiweiData.ADJ_BRIGHTNESS[it] ?: "") }).take(9)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                starCols.take(9).forEach { (nm, br) ->
                    val mg = (p.major + p.minor).firstOrNull { it.name == nm }?.mutagen ?: ""
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        nm.forEach { ch ->
                            Text(ch.toString(), color = starNameColor(nm), fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold, lineHeight = 9.5.sp)
                        }
                        if (br.isNotBlank()) {
                            Text(br, color = ZW_T1, fontSize = 6.5.sp, lineHeight = 8.sp)
                        }
                        if (mg.isNotBlank()) {
                            Box(Modifier.clip(RoundedCornerShape(2.dp)).background(zwSiHuaBg(mg))
                                .padding(horizontal = 1.dp)) {
                                Text(mg, color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold,
                                    lineHeight = 8.sp)
                            }
                        }
                        overlay[nm]?.let { Text(it, color = Color(0xFFF0A0D0), fontSize = 6.sp, lineHeight = 7.sp) }
                    }
                }
            }
            Spacer(Modifier.height(1.dp))

            // ② 流年（原版图2：流年: 12,24,36,48,60）
            Text("流年: " + flowAgesOf(r, z).joinToString(","), color = ZW_T1,
                fontSize = 6.5.sp, lineHeight = 8.sp, maxLines = 1)
            // ③ 小限（原版图2：小限: 12,24,36,48,60）
            Text("小限: " + p.ages.take(5).joinToString(","), color = ZW_T1,
                fontSize = 6.5.sp, lineHeight = 8.sp, maxLines = 1)

            Spacer(Modifier.weight(1f))

            // ④ 飞星方块（对齐原版图1）：多层时限四化落宫，彩色底白字两字方块。
            //    有飞星方块时不再显示大限数字"2∽11"（用户的图1口径）。
            val annos = fly[z].orEmpty().take(8)
            if (annos.isNotEmpty()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    annos.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            pair.forEach { a ->
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(2.dp))
                                    .background(a.color.copy(alpha = 0.92f))
                                    .padding(horizontal = 2.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center) {
                                    Text(a.text, color = Color.White, fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold, lineHeight = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else {
                // 无飞星落宫时仍显示大限区间（保留原有信息）
                Text("${p.decadal.first}~${p.decadal.second}",
                    color = if (isCurrentDx) Color(0xFFD8B4FE) else ZW_T0,
                    fontSize = 11.sp, lineHeight = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            // 视频联动标记：落宫显示「源宫地支之落宫宫名」（如"子之田宅"）
            if (isFlyTarget && sel != null) {
                Text("${ZHI_CN[sel.zhi]}之${p.name}", color = Color(0xFF4ADE80),
                    fontSize = 7.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(1.dp))

            // ⑤ 底部行：左 博士/亡神/病符 ｜ 中 宫名红框 ｜ 右 长生+宫干支（对齐原版图2）
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                // 左下：博士 / 亡神 / 病符
                Column(Modifier.weight(1f)) {
                    if (p.boshi.isNotBlank()) Text(p.boshi, color = ZW_T1, fontSize = 6.5.sp, lineHeight = 8.sp, maxLines = 1)
                    if (p.jiangqian.isNotBlank()) Text(p.jiangqian, color = ZW_T1, fontSize = 6.5.sp, lineHeight = 8.sp, maxLines = 1)
                    if (p.suiqian.isNotBlank()) Text(p.suiqian, color = ZW_T1, fontSize = 6.5.sp, lineHeight = 8.sp, maxLines = 1)
                }
                // 宫名红框（居中）
                Box(Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFFC8102E))
                    .padding(horizontal = 3.dp, vertical = 1.dp)) {
                    Text(p.name, color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
                }
                // 右下：长生十二神竖排 + 宫干支大字
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (p.changsheng.isNotBlank()) {
                        p.changsheng.forEach { ch ->
                            Text(ch.toString(), color = ZW_T1, fontSize = 7.sp, lineHeight = 9.sp)
                        }
                    }
                    Text(p.gan, color = ZW_T0, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                    Text(p.zhi, color = ZW_T0, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                }
            }
        }
    }
}

// 宫格四化方块配色（对齐图1：化禄红底；权蓝 科黄 忌紫）
private fun zwSiHuaBg(mg: String): Color = when (mg) {
    "禄" -> Color(0xFFD0342C)
    "权" -> Color(0xFF2E6FBF)
    "科" -> Color(0xFFC99A20)
    else -> Color(0xFF8B4FC0)
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

// 天干地支五行 → 原版彩色字（木绿 火红 土橙 金白 水蓝）
private fun wxColor(ch: String): Color = when (JishiyuBase.GAN_WUXING[ch] ?: JishiyuBase.ZHI_WUXING[ch]) {
    "木" -> Color(0xFF4ADE80)
    "火" -> Color(0xFFF87171)
    "土" -> Color(0xFFFBBF24)
    "金" -> Color(0xFFE8E6EE)
    "水" -> Color(0xFF60A5FA)
    else -> ZW_T1
}

// ===== 中宫详情：1:1 对齐文墨天机专业版中宫（真太阳/钟表/农历/命主身主子斗/双四柱/八字起运/大运十神/岁序年份/日时导航/自化） =====
@Composable
private fun ZwCenterInfo(
    r: Ziwei.Result, dxRange: Pair<Int, Int>?, limitLabel: String,
    liunianYear: Int, dxGan: String, activeLevel: String, onLevel: (String, String) -> Unit
) {
    val info = remember(r) { ZiweiCenter.build(r) }
    // 流年干支 + 农历月（对齐原版中宫「流年: 2026 丙午年 农历正月」）
    val liunianGz = JishiyuBase.JIAZHI[((liunianYear - 4) % 60 + 60) % 60]
    val lunarMonthCn = Regex("(\\d+)月").find(r.lunarDate)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?.let { LunarCalendar.cnNumber(it) } ?: ""
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp)) {
        // 行1：姓名（匿名）+ 性别·五行局
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("姓名: 匿名", color = ZW_T0, fontSize = 9.sp, maxLines = 1)
            Text("${if (r.isMan) "男" else "女"}·${r.juName}", color = ZW_T0, fontSize = 9.sp, maxLines = 1)
        }
        // 行2-4：真太阳时 / 钟表时间 / 农历
        ZwCInfoLine("真太阳时", info.solarTime)
        ZwCInfoLine("钟表时间", info.clockTime)
        val gz = r.chineseDate.split(' ')
        val yearGz = gz.getOrNull(0) ?: ""
        val lunarParts = Regex("(\\d+)年(\\d+)月(\\d+)日").find(r.lunarDate)?.destructured
        val lunarLine = if (lunarParts != null) {
            val (_, m, d) = lunarParts
            "${yearGz}年${LunarCalendar.cnNumber(m.toInt())}月${LunarCalendar.cnNumber(d.toInt())}日 ${shiChenCn(r.timeRange)}"
        } else r.lunarDate
        ZwCInfoLine("农历", lunarLine)
        // 行5：命主 / 身主 / 子斗
        ZwCInfoLine("命主", "${r.soul} 身主:${r.body} 子斗:${info.ziDou}")

        Spacer(Modifier.height(2.dp))
        // 行6-8：节气四柱 + 非节气四柱（双列竖排彩色干支，对齐原版）
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("节气四柱", color = ZW_FAINT, fontSize = 7.5.sp)
                ZwPillarRow(info.jieqiPillars)
            }
            Column(Modifier.weight(1f)) {
                Text("非节气四柱", color = ZW_FAINT, fontSize = 7.5.sp)
                ZwPillarRow(info.nonJieqiPillars)
            }
        }
        Spacer(Modifier.height(2.dp))
        // 行9：八字起运
        Text(info.qiYunText, color = ZW_T0, fontSize = 9.sp, maxLines = 1)
        Spacer(Modifier.height(1.dp))
        // 行10：大运八列（干支彩色 + 天干十神全称）
        Row(Modifier.fillMaxWidth()) {
            info.dayun.forEach { dy ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dy.gan, color = wxColor(dy.gan), fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                    Text(dy.zhi, color = wxColor(dy.zhi), fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                    Text(dy.shishen, color = ZW_T2, fontSize = 6.5.sp, lineHeight = 7.sp, maxLines = 1)
                }
            }
        }
        // 行11-12：岁序 / 起运年份
        Text(info.ages.joinToString(" ") { "${it}岁" }, color = ZW_T2, fontSize = 7.5.sp, maxLines = 1)
        Text(info.years.joinToString(" "), color = ZW_T2, fontSize = 7.5.sp, maxLines = 1)

        Spacer(Modifier.weight(1f))
        // 流年行（对齐原版中宫「流年: 2026 丙午年 农历正月」）
        Text("流年: $liunianYear ${liunianGz}年 农历${lunarMonthCn}月", color = ZW_T0,
            fontSize = 9.sp, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        // 六层飞星标签：本/限/年/月/日/时（对齐原版中宫彩色标签，点击切换飞星层）
        // 本=本命四化常驻；限=大限；年=流年；月/日/时=对应层天干
        val lvDefs = listOf(
            Triple("本", "命", r.yearGan),
            Triple("限", "大", dxGan.ifBlank { Ziwei.yearGan(liunianYear) }),
            Triple("年", "年", Ziwei.yearGan(liunianYear)),
            Triple("月", "月", Ziwei.yearGan(liunianYear)),
            Triple("日", "日", Ziwei.yearGan(liunianYear)),
            Triple("时", "时", Ziwei.yearGan(liunianYear)),
        )
        val lvColors = listOf(Color(0xFF6B7280), Color(0xFF3FA96B), Color(0xFF3B82C4),
            Color(0xFFC8862A), Color(0xFF6B7280), Color(0xFF6B7280))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .background(Color(0x18FFFFFF)), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            lvDefs.forEachIndexed { i, (label, key, gan) ->
                val on = activeLevel == key || (label == "本" && activeLevel.isEmpty())
                Box(Modifier.weight(1f).clip(RoundedCornerShape(3.dp))
                    .background(if (on) lvColors[i] else lvColors[i].copy(alpha = 0.42f))
                    .clickable { onLevel(key, gan) }
                    .padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = Color.White, fontSize = 9.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ZwCInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = ZW_FAINT, fontSize = 8.sp, modifier = Modifier.width(42.dp), maxLines = 1)
        Text(value, color = ZW_T1, fontSize = 8.sp, modifier = Modifier.weight(1f), maxLines = 1)
    }
}

@Composable
private fun ZwPillarRow(pillars: List<String>) {
    Row(Modifier.fillMaxWidth()) {
        pillars.forEach { gz ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val gan = gz.take(1); val zhi = gz.drop(1).take(1)
                Text(gan, color = wxColor(gan), fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                Text(zhi, color = wxColor(zhi), fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
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
    r: Ziwei.Result, sel: ZwSel?, birthYear: Int, limitLabel: String,
    onLevel: (String, String) -> Unit, onSelect: (ZwSel) -> Unit
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
    val day: ZiweiFortune.Option? = days.firstOrNull { it.dateStr == dDate } ?: days.firstOrNull()

    // 流时（参考文墨天机底部第 5 行）：随选中流日重算
    val hours: List<ZiweiFortune.Option> = remember(r, day) {
        if (day != null) ZiweiFortune.buildHours(r, day) else emptyList()
    }
    // 流时索引（同一天 12 时辰 dateStr 相同，需用索引区分选中）
    var hIdx by remember(r, day) { mutableStateOf(0) }

    val tagW = 22.dp
    val cardW = 52.dp
    val rowH = 44.dp
    val pScroll = rememberScrollState(); val yScroll = rememberScrollState()
    val mScroll = rememberScrollState(); val dScroll = rememberScrollState(); val hScroll = rememberScrollState()

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .border(1.dp, ZW_BORDER, RoundedCornerShape(12.dp)).background(ZW_SURFACE)) {

        // —— 大限行：起限前(童限) + 每宫十年，title + age + MM/DD 胶囊 ——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("大限", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(pScroll),
                verticalAlignment = Alignment.CenterVertically) {
                periods.forEachIndexed { i, p ->
                    val active = i == pIdx
                    ZwLimitCard(if (i == 0 && p.kind == "童限") "起限前" else p.label,
                        if (p.startAge == p.endAge) "${p.startAge}岁" else "${p.startAge}-${p.endAge}岁",
                        p.dateStr.substring(5), active, cardW, rowH) {
                        pIdx = i
                        val newYears = ZiweiFortune.buildYears(r, p)
                        yDate = newYears.firstOrNull()?.dateStr ?: ""
                        mDate = ""; dDate = ""; hIdx = 0
                        val focus = ziToIdx(p.zhi.ifBlank { r.ebSoul })
                        val focusGan = p.gan.ifBlank { r.yearGan }
                        onLevel("大", focusGan)
                        onSelect(ZwSel(focus, "${p.label} ${p.startAge}-${p.endAge}岁 · ${p.palaceName}", ZwSelType.DECADAL, focusGan, "大"))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
            ZwRowArrow()
        }
        ZwThinDivider()
        // —— 流年小限行：2022年 / 壬寅2岁（参考图口径）——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("流年\n小限", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(yScroll),
                verticalAlignment = Alignment.CenterVertically) {
                years.forEach { o ->
                    val active = o.dateStr == yDate
                    ZwFlatCell("${o.head}年", "${o.ganZhi}${o.sub}", active, cardW, rowH) {
                        yDate = o.dateStr
                        val newMonths = ZiweiFortune.buildMonths(r, o)
                        mDate = newMonths.firstOrNull()?.dateStr ?: ""
                        dDate = ""; hIdx = 0
                        onLevel("年", o.ganZhi[0].toString())
                        onSelect(ZwSel(ziToIdx(o.ganZhi[1].toString()), "流年 ${o.head} · ${o.ganZhi}（${o.sub}）", ZwSelType.YEARLY, o.ganZhi[0].toString(), "年"))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
            ZwRowArrow()
        }
        ZwThinDivider()
        // —— 流月行：正月 / 二月 ...（中文，参考图口径）——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("流月", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(mScroll),
                verticalAlignment = Alignment.CenterVertically) {
                months.forEach { o ->
                    val active = o.dateStr == mDate
                    ZwFlatCell(o.display.ifBlank { o.head }, "", active, cardW, rowH) {
                        mDate = o.dateStr
                        val newDays = ZiweiFortune.buildDays(r, o)
                        dDate = newDays.firstOrNull()?.dateStr ?: ""
                        hIdx = 0
                        onLevel("月", o.ganZhi[0].toString())
                        onSelect(ZwSel(ziToIdx(o.ganZhi[1].toString()), "${o.head} · ${o.sub} · ${o.ganZhi}", ZwSelType.YEARLY, o.ganZhi[0].toString(), "月"))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
            ZwRowArrow()
        }
        ZwThinDivider()
        // —— 流日行：初一 / 初二 ...（中文，参考图口径）——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("流日", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(dScroll),
                verticalAlignment = Alignment.CenterVertically) {
                days.forEachIndexed { di, o ->
                    val active = o.dateStr == dDate
                    ZwFlatCell(o.display.ifBlank { o.head }, "", active, cardW, rowH) {
                        dDate = o.dateStr
                        hIdx = 0
                        onLevel("日", o.ganZhi[0].toString())
                        onSelect(ZwSel(ziToIdx(o.ganZhi[1].toString()), "流日 ${o.sub} · ${o.ganZhi}", ZwSelType.YEARLY, o.ganZhi[0].toString(), "日"))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
            ZwRowArrow()
        }
        ZwThinDivider()
        // —— 流时行：子时 / 丑时 ...（参考图新增第 5 行）——
        Row(Modifier.fillMaxWidth()) {
            ZwRowTagV("流时", rowH, tagW)
            Row(Modifier.weight(1f).height(rowH).horizontalScroll(hScroll),
                verticalAlignment = Alignment.CenterVertically) {
                hours.forEachIndexed { hi, o ->
                    ZwFlatCell(o.display.ifBlank { o.head }, o.ganZhi, hi == hIdx, cardW, rowH) {
                        hIdx = hi
                        onLevel("时", o.ganZhi[0].toString())
                        onSelect(ZwSel(ziToIdx(o.ganZhi[1].toString()), "流时 ${o.head} · ${o.ganZhi}", ZwSelType.YEARLY, o.ganZhi[0].toString(), "时"))
                    }
                    Spacer(Modifier.width(2.dp))
                }
            }
            ZwRowArrow()
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

// 行右侧展开箭头（参考图每行末尾的 ▷）
@Composable
private fun ZwRowArrow() {
    Box(Modifier.width(16.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Text("▷", color = ZW_FAINT, fontSize = 10.sp)
    }
}

// 紧凑格子：主文案(中文) + 可选副文案(干支/岁)，对照参考图底部表格扁平样式
@Composable
private fun ZwFlatCell(
    head: String, sub: String, active: Boolean,
    w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, onClick: () -> Unit
) {
    Column(Modifier.width(w).height(h).clip(RoundedCornerShape(8.dp))
        .background(if (active) PINK.copy(0.18f) else Color.Transparent)
        .border(1.dp, if (active) PINK.copy(0.55f) else Color.Transparent, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick).padding(vertical = 3.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(head, color = if (active) PINK else ZW_T0, fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1, lineHeight = 12.sp)
        if (sub.isNotBlank())
            Text(sub, color = ZW_FAINT, fontSize = 8.sp, maxLines = 1, lineHeight = 10.sp)
    }
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
        Text(title, color = if (active) PINK else ZW_T0, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, lineHeight = 12.sp)
        Text(age, color = ZW_FAINT, fontSize = 8.sp, textAlign = TextAlign.Center, lineHeight = 9.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(PINK.copy(0.22f)).padding(horizontal = 2.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center) {
            Text(date, color = if (active) PINK else ZW_T1, fontSize = 9.sp, maxLines = 1)
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
        Text(head, color = ZW_T0, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, lineHeight = 12.sp)
        Text(date, color = ZW_FAINT, fontSize = 7.5.sp, maxLines = 1, lineHeight = 9.sp)
        Spacer(Modifier.weight(1f))
        if (gz.length >= 2) {
            Text(gz[0].toString(), color = ZW_T1, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
            Text(gz[1].toString(), color = ZW_T1, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
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

// 从时辰区间取时辰中文（如 "11:00~13:00" → "午时"）
private fun shiChenCn(timeRange: String): String {
    val idx = listOf("00:00","01:00","03:00","05:00","07:00","09:00","11:00","13:00","15:00","17:00","19:00","21:00","23:00")
        .indexOf(timeRange.take(5))
    val zhi = if (idx < 0) 0 else idx
    return ZHI_CN[zhi] + "时"
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

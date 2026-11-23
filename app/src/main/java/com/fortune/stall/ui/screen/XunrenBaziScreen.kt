package com.fortune.stall.ui.screen

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.BaziTaibu
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 八字 · 寻壬观吉风格结果页（按 com.drone.nofeerty「八字排盘」反编译结构复刻）。
 * 深藏青 Header + 米色国风主题；三 Tab：基本信息 / 基本排盘 / 专业细盘。
 * 复用 BaziTaibu 引擎数据。
 */
// 主题色（反编译自原版）
private val XR_HEADER = Color(0xFF1A1A2E)      // HeaderDark
private val XR_GRID_HEAD = Color(0xFF24242C)   // 表头
private val XR_BG = Color(0xFFF5F2ED)          // 背景
private val XR_SURFACE = Color(0xFFFFFFFF)     // 卡片
private val XR_SURFACE_H = Color(0xFFF0EDE8)
private val XR_TEXT = Color(0xFF1A1A1A)
private val XR_TEXT2 = Color(0xFF6B6358)
private val XR_TEXT3 = Color(0xFF9A9085)
private val XR_BORDER = Color(0xFFDDD8D0)
private val XR_ACCENT = Color(0xFFC9A96E)      // 古铜金
private val XR_ACCENT_D = Color(0xFF8B7444)
private val XR_SHEN_SHA = Color(0xFFD2A679)

private val XR_WOOD = Color(0xFF4CAF50)
private val XR_FIRE = Color(0xFFF44336)
private val XR_EARTH = Color(0xFFFF8F00)
private val XR_METAL = Color(0xFFFFD700)
private val XR_WATER = Color(0xFF2196F3)

private fun xrWxColor(w: String) = when (w) {
    "木" -> XR_WOOD; "火" -> XR_FIRE; "土" -> XR_EARTH; "金" -> XR_METAL; "水" -> XR_WATER
    else -> XR_TEXT
}

@Composable
fun XunrenBaziScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var question by remember { mutableStateOf("案例1") }
    var isMan by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<BaziTaibu.Result?>(null) }

    if (result != null) {
        XrDetail(result!!, question, onBack)
        return
    }

    JiDialogShell(title = "八 字 排 盘", onBack = onBack, buttonText = "排 盘", onCast = {
        try {
            val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
            val r = BaziTaibu.paipan(dt, isMan, "midnight")
            val bazi = r.pillars.joinToString(" ") { it.ganZhi }
            val face = "${if (isMan) "乾造" else "坤造"} $bazi"
            db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                question, "xunrenbazi", "八字·寻壬", face, "")
            result = r
        } catch (e: Exception) {
            Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }) {
        JiRow("案例名称：") { JiInput(question, { question = it.take(20) }, "输入名称", Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(10.dp))
        JiRow("性别：") { JiChipLine("", listOf("男", "女"), if (isMan) 0 else 1) { isMan = it == 0 } }
        Spacer(Modifier.height(10.dp))
        JiRow("出生时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
    }
    if (showDate) BoardDateDialog(initial = try { LocalDate.parse(dtText.substring(0, 10)) } catch (e: Exception) { LocalDate.now() },
        onDismiss = { showDate = false },
        onConfirm = { d -> val t = dtText.substring(11).ifEmpty { "00:00" }; dtText = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"; showDate = false; showTime = true })
    if (showTime) BoardTimeDialog(initial = try { LocalTime.parse(dtText.substring(11)) } catch (e: Exception) { LocalTime.now() },
        onDismiss = { showTime = false },
        onConfirm = { t -> dtText = "${dtText.substring(0, 10)} ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"; showTime = false })
}

@Composable
private fun XrDetail(r: BaziTaibu.Result, name: String, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("基本信息", "基本排盘", "专业细盘")
    Column(Modifier.fillMaxSize().background(XR_BG)) {
        // 顶部：基本信息页保留复杂 Header；其它两页用简单 Header
        if (tab == 0) XrHeader(r, name, tab, tabs) { tab = it } else XrSimpleHeader(tabs, tab) { tab = it }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            when (tab) {
                0 -> XrBasicInfo(r, name)
                1 -> XrGridPage(r)
                2 -> XrProPage(r)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// 复杂 Header（藏青底 + 生肖圆章 + 案例名 + Tab）
@Composable
private fun XrHeader(r: BaziTaibu.Result, name: String, tab: Int, tabs: List<String>, onTab: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(XR_HEADER)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = Color.White, fontSize = 22.sp)
            Spacer(Modifier.width(6.dp))
            Text("八字排盘", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("⋮", color = Color.White, fontSize = 22.sp)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val zhi = r.pillars[0].zhi
            val emoji = shengXiaoEmoji(zhi)
            val wx = xrWxColor(BaziTaibu.ZHI_WX[zhi] ?: "")
            Box(Modifier.size(56.dp).clip(CircleShape)
                .background(wx.copy(alpha = 0.12f)).border(1.5.dp, wx.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 34.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(name.ifEmpty { "案例1" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(buildAnnotatedName(r), color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
                Text(r.solarText, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        XrTabRow(tabs, tab, onTab, dark = true)
    }
}

@Composable
private fun XrSimpleHeader(tabs: List<String>, tab: Int, onTab: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(XR_HEADER)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = Color.White, fontSize = 22.sp)
            Spacer(Modifier.width(6.dp))
            Text("八字排盘", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        XrTabRow(tabs, tab, onTab, dark = true)
    }
}

@Composable
private fun XrTabRow(tabs: List<String>, tab: Int, onTab: (Int) -> Unit, dark: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { i, t ->
            Column(Modifier.weight(1f).clickable { onTab(i) },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(t, color = if (tab == i) XR_ACCENT else Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 9.dp))
                Box(Modifier.width(if (tab == i) 28.dp else 0.dp).height(2.5.dp)
                    .clip(RoundedCornerShape(1.25.dp)).background(XR_ACCENT))
            }
        }
    }
}

private fun buildAnnotatedName(r: BaziTaibu.Result): String {
    val gz = r.pillars.joinToString(" ") { it.ganZhi }
    return "${if (r.gender == "男") "乾造" else "坤造"}  $gz"
}

// ===== Tab1：基本信息 =====
@Composable
private fun XrBasicInfo(r: BaziTaibu.Result, name: String) {
    Spacer(Modifier.height(4.dp))
    XrSection("时间信息") {
        XrInfoRow("阳历", r.solarText)
        XrInfoRow("农历", r.lunarText)
        XrInfoRow("性别", r.gender)
        XrInfoRow("属相", shengXiaoName(r.pillars[0].zhi))
    }
    XrSection("日主分析") {
        XrInfoRow("日主", "${r.dayMaster}（${BaziTaibu.GAN_WX[r.dayMaster] ?: ""}）", valueColor = xrWxColor(BaziTaibu.GAN_WX[r.dayMaster] ?: ""), valueSize = 22)
        XrInfoRow("旺衰", dayMasterStrength(r))
        XrInfoRow("命局格局", geju(r))
    }
    XrSection("五行统计") {
        val total = r.wuXing.values.sum().coerceAtLeast(1)
        for (w in listOf("木", "火", "土", "金", "水")) {
            val cnt = r.wuXing[w] ?: 0
            XrWuXingBar(w, cnt, total)
        }
        Spacer(Modifier.height(4.dp))
        Text("月令旺衰：" + r.wangShuai.joinToString(" · "), color = XR_TEXT2, fontSize = 12.sp)
    }
    XrSection("喜用忌神") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("喜用神", color = XR_TEXT2, fontSize = 15.sp, modifier = Modifier.width(56.dp))
            Row { for (w in yiYong(r)) XrWuXingChip(w, false) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("忌神", color = XR_TEXT2, fontSize = 15.sp, modifier = Modifier.width(56.dp))
            Row { for (w in jiShen(r)) XrWuXingChip(w, true) }
        }
    }
    XrSection("神煞") {
        val ss = (r.globalShenSha + r.pillars.flatMap { it.shenSha }).distinct()
        if (ss.isEmpty()) Text("—", color = XR_TEXT3, fontSize = 14.sp)
        else Text(ss.take(24).joinToString("  "), color = XR_SHEN_SHA, fontSize = 14.sp, lineHeight = 22.sp)
    }
    XrSection("刑冲合害") {
        val rel = (r.relations + r.banHe + r.sanHui + r.ganWuHe + r.ganChong).map { it.desc }
        if (rel.isEmpty()) Text("原局无明显刑冲合害", color = XR_TEXT3, fontSize = 14.sp)
        else Column { rel.take(12).forEach { Text("· $it", color = XR_TEXT2, fontSize = 13.sp, lineHeight = 20.sp) } }
    }
    XrSection("十二长生") {
        Row(Modifier.fillMaxWidth()) {
            r.pillars.forEach {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(it.name.replace("柱", ""), color = XR_TEXT3, fontSize = 12.sp)
                    Text(it.diShi, color = XR_ACCENT_D, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    XrSection("命宫·胎元·身宫") {
        Row(Modifier.fillMaxWidth()) {
            listOf("命宫" to r.mingGong to "精神", "胎元" to r.taiYuan to "先天", "身宫" to r.shenGong to "后天").forEach { (pair, desc) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pair.first, color = XR_TEXT3, fontSize = 12.sp)
                    Text(pair.second, color = XR_TEXT, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = XR_ACCENT, fontSize = 11.sp)
                }
            }
        }
    }
    XrSection("纳音·空亡") {
        for (p in r.pillars) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(p.name, color = XR_TEXT2, fontSize = 13.sp, modifier = Modifier.width(44.dp))
                Text("纳音 ${p.naYin}", color = XR_TEXT, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (p.kong) Text("空亡", color = XR_FIRE, fontSize = 12.sp)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(XR_ACCENT),
            contentAlignment = Alignment.Center) { Text("智能解读", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(XR_SURFACE_H)
            .border(1.dp, XR_BORDER, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text("专业排盘", color = XR_ACCENT_D, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ===== Tab2：基本排盘（四柱表）=====
@Composable
private fun XrGridPage(r: BaziTaibu.Result) {
    Spacer(Modifier.height(4.dp))
    Text("八字排盘", color = XR_TEXT, fontSize = 19.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(XR_SURFACE)
        .border(1.dp, XR_BORDER, RoundedCornerShape(8.dp))) {
        Column(Modifier.fillMaxWidth()) {
            val cols = listOf("", "年柱", "月柱", "日柱", "时柱")
            val colW = listOf(0.14f, 0.215f, 0.215f, 0.215f, 0.215f)
            // 表头
            Row(Modifier.fillMaxWidth().background(XR_GRID_HEAD)) {
                cols.forEachIndexed { i, h ->
                    Text(h, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(colW[i]).padding(vertical = 8.dp), textAlign = TextAlign.Center)
                }
            }
            XrGridDivider()
            // 十神
            XrGridRow("十神", colW, r.pillars) { i, p -> (if (i == 2) "日主" else tenGodShort(p.tenGod)) to XR_TEXT }
            XrGridDivider()
            // 天干（五行色 + 大字）
            XrGridRowBIG(colW, r.pillars, isGan = true)
            XrGridDivider()
            // 地支（五行色 + 大字）
            XrGridRowBIG(colW, r.pillars, isGan = false)
            XrGridDivider()
            // 藏干
            XrGridRow("藏干", colW, r.pillars) { _, p -> p.hidden.joinToString(" ") { it.stem } to XR_TEXT2 }
            XrGridDivider()
            // 纳音
            XrGridRow("纳音", colW, r.pillars) { _, p -> p.naYin to XR_TEXT2 }
            XrGridDivider()
            // 空亡
            XrGridRow("空亡", colW, r.pillars) { _, p -> (if (p.kong) "空" else "—") to XR_TEXT2 }
            // 神煞（简略）
            XrGridDivider()
            XrGridRow("神煞", colW, r.pillars) { _, p -> p.shenSha.take(2).joinToString(" ") to XR_SHEN_SHA }
        }
    }
}

// ===== Tab3：专业细盘 =====
@Composable
private fun XrProPage(r: BaziTaibu.Result) {
    Spacer(Modifier.height(4.dp))
    XrSection("格局用神") {
        XrInfoRow("格局", geju(r))
        XrInfoRow("日主强弱", dayMasterStrength(r))
        XrInfoRow("喜用神", yiYong(r).joinToString("、"))
        XrInfoRow("忌神", jiShen(r).joinToString("、"))
        XrInfoRow("调候", tiaoHou(r))
    }
    XrSection("干支关系") {
        val rel = (r.relations + r.banHe + r.sanHui).map { it.desc }
        if (rel.isEmpty()) Text("—", color = XR_TEXT3, fontSize = 14.sp)
        else Column { rel.forEach { Text("· $it", color = XR_TEXT2, fontSize = 13.sp, lineHeight = 20.sp) } }
    }
    XrSection("大运") {
        if (r.dayun.isEmpty()) Text("—", color = XR_TEXT3, fontSize = 14.sp)
        else Column {
            Text("起运：${r.qiYunText}", color = XR_ACCENT_D, fontSize = 13.sp)
            r.dayun.take(8).forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(it.ganZhi, color = xrWxColor(BaziTaibu.GAN_WX[it.ganZhi[0].toString()] ?: ""),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
                    Text("${it.startAge}岁 · ${it.startYear}年", color = XR_TEXT2, fontSize = 13.sp,
                        modifier = Modifier.weight(1f))
                    Text(it.tenGod, color = XR_TEXT2, fontSize = 12.sp)
                }
            }
        }
    }
}

// ===== 通用组件 =====
@Composable
private fun XrSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)
        .clip(RoundedCornerShape(10.dp)).background(XR_SURFACE)
        .border(1.dp, XR_BORDER, RoundedCornerShape(10.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 3.dp, height = 15.dp).clip(RoundedCornerShape(2.dp)).background(XR_ACCENT))
            Spacer(Modifier.width(7.dp))
            Text(title, color = XR_TEXT, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun XrInfoRow(label: String, value: String, valueColor: Color = XR_TEXT, valueSize: Int = 14) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = XR_TEXT2, fontSize = 14.sp, modifier = Modifier.width(70.dp))
        Text(value, color = valueColor, fontSize = valueSize.sp,
            fontWeight = if (valueSize >= 20) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun XrWuXingBar(w: String, cnt: Int, total: Int) {
    val c = xrWxColor(w)
    val pct = cnt * 100f / total
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(w, color = c, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(XR_SURFACE_H)) {
            Box(Modifier.fillMaxWidth(pct / 100f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(c))
        }
        Spacer(Modifier.width(8.dp))
        Text("$cnt", color = XR_TEXT2, fontSize = 12.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
        Text(String.format("%.1f%%", pct), color = XR_TEXT3, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun XrWuXingChip(w: String, isJi: Boolean) {
    val c = xrWxColor(w)
    Box(Modifier.padding(end = 6.dp).clip(RoundedCornerShape(12.dp))
        .background(c.copy(alpha = if (isJi) 0.12f else 0.18f))
        .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(w, color = c, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun XrGridDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(XR_BORDER))

@Composable
private fun XrGridRow(label: String, colW: List<Float>, pillars: List<BaziTaibu.Pillar>,
                     value: (Int, BaziTaibu.Pillar) -> Pair<String, Color>) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(colW[0]).background(XR_SURFACE_H).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = XR_TEXT2, fontSize = 11.sp)
        }
        pillars.forEachIndexed { i, p ->
            val (v, c) = value(i, p)
            Box(Modifier.weight(colW[i + 1]).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(v, color = c, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun XrGridRowBIG(colW: List<Float>, pillars: List<BaziTaibu.Pillar>, isGan: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(colW[0]).background(XR_SURFACE_H).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(if (isGan) "天干" else "地支", color = XR_TEXT2, fontSize = 11.sp)
        }
        pillars.forEachIndexed { i, p ->
            val ch = if (isGan) p.gan else p.zhi
            val c = xrWxColor(BaziTaibu.GAN_WX[ch] ?: BaziTaibu.ZHI_WX[ch] ?: "")
            Box(Modifier.weight(colW[i + 1]).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(ch, color = c, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ===== 工具 =====
private fun tenGodShort(tg: String) = when (tg) {
    "比肩" -> "比"; "劫财" -> "劫"; "食神" -> "食"; "伤官" -> "伤"
    "偏财" -> "才"; "正财" -> "财"; "七杀" -> "杀"; "正官" -> "官"
    "偏印" -> "枭"; "正印" -> "印"; "日主" -> "日主"; else -> tg
}
private fun shengXiaoName(zhi: String) = mapOf(
    "子" to "鼠", "丑" to "牛", "寅" to "虎", "卯" to "兔", "辰" to "龙", "巳" to "蛇",
    "午" to "马", "未" to "羊", "申" to "猴", "酉" to "鸡", "戌" to "狗", "亥" to "猪")[zhi] ?: "—"
private fun shengXiaoEmoji(zhi: String) = mapOf(
    "子" to "🐭", "丑" to "🐮", "寅" to "🐯", "卯" to "🐰", "辰" to "🐲", "巳" to "🐍",
    "午" to "🐴", "未" to "🐑", "申" to "🐵", "酉" to "🐔", "戌" to "🐶", "亥" to "🐷")[zhi] ?: "🐲"

private fun dayMasterStrength(r: BaziTaibu.Result): String {
    // 按日主五行在月令的旺相休囚死判断
    val dmWx = BaziTaibu.GAN_WX[r.dayMaster] ?: ""
    val wang = r.wangShuai.firstOrNull() ?: ""
    return when {
        dmWx == wang -> "旺（得令）"
        r.wangShuai.getOrNull(1) == dmWx -> "相（得生）"
        r.wangShuai.getOrNull(2) == dmWx -> "平和"
        else -> "弱（失令）"
    }
}
private fun geju(r: BaziTaibu.Result): String {
    // 简化格局：按月令本气十神
    val mTenGod = r.pillars[1].tenGod
    return when (mTenGod) {
        "正官" -> "正官格"; "七杀" -> "七杀格"; "正财" -> "正财格"; "偏财" -> "偏财格"
        "正印" -> "正印格"; "偏印" -> "偏印格"; "食神" -> "食神格"; "伤官" -> "伤官格"
        else -> "${mTenGod}格（月令）"
    }
}
// 调候用神（按月令季节），更贴近原版"夏月火旺土燥喜金水、忌木火"口径
private fun seasonYiJi(zhi: String): Pair<List<String>, List<String>> = when (zhi) {
    "寅", "卯", "辰" -> listOf("火", "水") to listOf("金", "土")
    "巳", "午", "未" -> listOf("金", "水") to listOf("木", "火")
    "申", "酉", "戌" -> listOf("水", "火") to listOf("金", "土")
    "亥", "子", "丑" -> listOf("火", "土") to listOf("金", "水")
    else -> listOf("金", "水") to listOf("木", "火")
}
private fun yiYong(r: BaziTaibu.Result): List<String> = seasonYiJi(r.pillars[1].zhi).first
private fun jiShen(r: BaziTaibu.Result): List<String> = seasonYiJi(r.pillars[1].zhi).second
private fun tiaoHou(r: BaziTaibu.Result): String = when (r.pillars[1].zhi) {
    "寅", "卯", "辰" -> "春月木旺，宜火泄金制"
    "巳", "午", "未" -> "夏月火旺，宜水调金生"
    "申", "酉", "戌" -> "秋月金旺，宜火制水泄"
    "亥", "子", "丑" -> "冬月水旺，宜火暖土制"
    else -> "平和"
}

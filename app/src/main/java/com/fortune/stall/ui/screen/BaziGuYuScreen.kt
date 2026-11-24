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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.BaziTaibu
import com.fortune.stall.util.BirthPlaces
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 八字 · 谷雨工作台（参考 ArtemisLin/guyu-bazi 界面风格，数据复用 BaziTaibu 引擎）。
 * 四柱九行明细 + 五行旺衰 + 神煞落点 + 大运流年逐级钻取 + 真太阳时选地。
 */

// ===== 谷雨浅色国风配色（对齐 guyu-bazi 五行色）=====
private val GY_BG = Color(0xFFF5F1E8)
private val GY_CARD = Color(0xFFFFFFFF)
private val GY_INK = Color(0xFF2A2620)
private val GY_SUB = Color(0xFF7A7368)
private val GY_DIM = Color(0xFFA9A197)
private val GY_LINE = Color(0xFFE4DED2)
private val GY_ACCENT = Color(0xFF8A6D3B)
private val GY_ACCENT_BG = Color(0xFFF3EBDC)
private val GY_RED = Color(0xFFB0392F)
private val GY_WOOD = Color(0xFF1E8E47)
private val GY_FIRE = Color(0xFFC93A28)
private val GY_EARTH = Color(0xFF8A5416)
private val GY_METAL = Color(0xFFA8791A)
private val GY_WATER = Color(0xFF1F6DBF)
private val GY_PURPLE = Color(0xFF7A5EA8)

private fun gyWx(c: String): Color = when (c) {
    "甲", "乙", "寅", "卯" -> GY_WOOD
    "丙", "丁", "巳", "午" -> GY_FIRE
    "戊", "己", "辰", "戌", "丑", "未" -> GY_EARTH
    "庚", "辛", "申", "酉" -> GY_METAL
    "壬", "癸", "亥", "子" -> GY_WATER
    else -> GY_INK
}

@Composable
fun BaziGuYuScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var name by remember { mutableStateOf("案例1") }
    var isMan by remember { mutableStateOf(true) }
    var boundary by remember { mutableStateOf(0) } // 0=午夜换日 1=子时换日
    var summer by remember { mutableStateOf(false) } // 夏令时（勾选减1小时）
    var showDate by remember { mutableStateOf(false) }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var pickLevel by remember { mutableStateOf(-1) } // -1关闭 0省 1市 2区
    var result by remember { mutableStateOf<BaziTaibu.Result?>(null) }
    var solarNote by remember { mutableStateOf("") }

    if (result != null) {
        GyDetail(result!!, name, solarNote, onBack)
        return
    }

    JiDialogShell(title = "八 字 · 谷 雨", onBack = onBack, buttonText = "排 盘", onCast = {
        try {
            var dt = LocalDateTime.parse(dtText.trim(), dtFmt)
            if (summer) dt = dt.minusHours(1) // 夏令时按标准时排
            val lon = BirthPlaces.lonOf(province, city, district)
            val r = BaziTaibu.paipan(dt, isMan, if (boundary == 0) "midnight" else "zi", lon)
            val face = baziTbFaceText(r, name, "八字排盘 · 谷雨")
            db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                name, "baziguyu", "八字·谷雨", face, "")
            solarNote = buildString {
                if (lon != null) append("真太阳时经度 ${"%.2f".format(lon)}°")
                if (summer) append(if (isEmpty()) "夏令时已校正（−1h）" else " · 夏令时已校正（−1h）")
            }
            result = r
        } catch (e: Exception) {
            Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }) {
        JiRow("案例名称：") { JiInput(name, { name = it.take(20) }, "输入名称", Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(10.dp))
        JiRow("性别：") { JiChipLine("", listOf("男", "女"), if (isMan) 0 else 1) { isMan = it == 0 } }
        Spacer(Modifier.height(10.dp))
        JiRow("出生时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(10.dp))
        PlacePicker3(province, city, district, listOf("省份", "城市", "区县"),
            JiForm.TEXT, JiForm.SUB, JiForm.LINE, JiForm.INPUT_BG,
            { pickLevel = 0 }, { pickLevel = 1 }, { pickLevel = 2 })
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            JiSwitch(summer, { summer = it }, "夏令时（1986–1991，勾选按标准时 −1h）")
        }
        Spacer(Modifier.height(8.dp))
        JiRow("换日界：") { JiChipLine("", listOf("午夜换日", "子时换日"), boundary) { boundary = it } }
    }

    if (showDate) BoardDateTimeDialog(
        initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
        onDismiss = { showDate = false },
        onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false })

    if (pickLevel >= 0) {
        val (title, opts, onPick) = when (pickLevel) {
            0 -> Triple("选择省份", BirthPlaces.PROVINCES.map { it.name }, { v: String ->
                province = v; city = ""; district = ""; pickLevel = 1 })
            1 -> {
                val ps = BirthPlaces.PROVINCES.firstOrNull { it.name == province }
                Triple("选择城市", ps?.cities?.map { it.name } ?: emptyList(), { v: String ->
                    city = v; district = ""; pickLevel = 2 })
            }
            else -> {
                val ps = BirthPlaces.PROVINCES.firstOrNull { it.name == province }
                val cs = ps?.cities?.firstOrNull { it.name == city }
                Triple("选择区县", cs?.districts?.map { it.name } ?: emptyList(), { v: String ->
                    district = v; pickLevel = -1 })
            }
        }
        if (opts.isEmpty()) {
            pickLevel = -1
        } else {
            JiOptionDialog(title, opts, false, { pickLevel = -1 }, onPick)
        }
    }
}

// ===== 结果页 =====
@Composable
private fun GyDetail(r: BaziTaibu.Result, name: String, solarNote: String, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("命盘", "神煞", "大运流年")
    Column(Modifier.fillMaxSize().background(GY_BG)) {
        BoardNavBar("八 字 · 谷 雨", onBack)
        Row(Modifier.fillMaxWidth().background(GY_CARD)) {
            tabs.forEachIndexed { i, t ->
                Box(Modifier.weight(1f).height(42.dp)
                    .background(if (tab == i) GY_ACCENT else Color.Transparent)
                    .clickable { tab = i }, contentAlignment = Alignment.Center) {
                    Text(t, color = if (tab == i) Color.White else GY_ACCENT,
                        fontSize = 14.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            when (tab) {
                0 -> GyMingpan(r, name, solarNote)
                1 -> GyShenSha(r)
                2 -> GyDaYun(r)
            }
            Spacer(Modifier.height(14.dp))
            com.fortune.stall.ui.components.ResultActionBar(faceText = baziTbFaceText(r, name, "八字排盘 · 谷雨"))
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ===== Tab 命盘：四柱九行明细 + 五行旺衰 =====
@Composable
private fun GyMingpan(r: BaziTaibu.Result, name: String, solarNote: String) {
    // 信息卡
    GyCard {
        Text(name, color = GY_ACCENT, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        GyKV("公历", r.solarText + " · " + r.gender)
        GyKV("农历", r.lunarText)
        if (solarNote.isNotBlank()) GyKV("校正", solarNote)
        GyKV("起运", r.qiYunText)
        GyKV("胎元 / 命宫", "${r.taiYuan} / ${r.mingGong}")
    }
    Spacer(Modifier.height(10.dp))
    // 九行明细表
    Text("四柱明细", color = GY_ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
    GyNineGrid(r)
    Spacer(Modifier.height(10.dp))
    // 五行旺衰
    GyCard {
        Text("五行旺相休囚死", color = GY_ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val monthZhi = r.pillars.getOrNull(1)?.zhi ?: ""
        GyWangBar(monthZhi)
        Spacer(Modifier.height(8.dp))
        Text("五行个数：" + r.wuXing.entries.sortedByDescending { it.value }
            .joinToString("  ") { "${it.key}${it.value}" }, color = GY_SUB, fontSize = 12.sp)
        if (r.wangShuai.isNotEmpty())
            Text("旺衰：" + r.wangShuai.joinToString("、"), color = GY_SUB, fontSize = 12.sp)
    }
}

/** 四柱九行明细：主星/天干/地支/藏干/星运/自坐/空亡/纳音/神煞 */
@Composable
private fun GyNineGrid(r: BaziTaibu.Result) {
    val cols = r.pillars
    val labels = listOf("主星", "天干", "地支", "藏干", "星运", "自坐", "空亡", "纳音", "神煞")
    val colW = 76.dp
    val labW = 42.dp
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        .background(GY_CARD).border(1.dp, GY_LINE, RoundedCornerShape(8.dp))) {
        // 表头：项目 + 四柱
        Box(Modifier.width(labW).background(GY_ACCENT_BG).padding(6.dp), contentAlignment = Alignment.Center) {
            Text("柱", color = GY_ACCENT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        cols.forEach { p ->
            Box(Modifier.width(colW).background(GY_ACCENT_BG).padding(6.dp), contentAlignment = Alignment.Center) {
                Text(p.name, color = GY_ACCENT, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        // 九行
        for (row in labels.indices) {
            val lab = labels[row]
            Box(Modifier.width(labW).background(GY_ACCENT_BG).border(0.5.dp, GY_LINE).padding(6.dp),
                contentAlignment = Alignment.Center) {
                Text(lab, color = GY_ACCENT, fontSize = 11.sp)
            }
            cols.forEach { p ->
                Box(Modifier.width(colW).border(0.5.dp, GY_LINE).padding(vertical = 6.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center) {
                    when (row) {
                        0 -> Text(shortTenGod(p.tenGod), color = GY_INK, fontSize = 12.sp)
                        1 -> Text(p.gan, color = gyWx(p.gan), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        2 -> Text(p.zhi, color = gyWx(p.zhi), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        3 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            p.hidden.forEach { h ->
                                Text("${h.stem} ${shortTenGod(h.tenGod)}", color = gyWx(h.stem), fontSize = 10.sp)
                            }
                        }
                        4 -> Text(p.diShi, color = GY_SUB, fontSize = 11.sp)
                        5 -> Text(BaziTaibu.diShi(p.gan, p.zhi), color = GY_SUB, fontSize = 11.sp)
                        6 -> Text(if (p.kong) "空亡" else "—", color = if (p.kong) GY_RED else GY_DIM, fontSize = 11.sp)
                        7 -> Text(p.naYin, color = GY_SUB, fontSize = 10.sp)
                        8 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (p.shenSha.isEmpty()) Text("—", color = GY_DIM, fontSize = 10.sp)
                            else p.shenSha.take(6).forEach { Text(it, color = GY_PURPLE, fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
    }
}

private fun shortTenGod(t: String) = t.replace("正", "").replace("偏", "")
    .let { if (it.length > 3) it.take(3) else it }

@Composable
private fun GyWangBar(monthZhi: String) {
    val ELEM = mapOf("寅" to "木", "卯" to "木", "巳" to "火", "午" to "火", "申" to "金", "酉" to "金",
        "亥" to "水", "子" to "水", "辰" to "土", "戌" to "土", "丑" to "土", "未" to "土")
    val SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    val KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")
    val ling = ELEM[monthZhi] ?: "木"
    val state = { e: String -> when {
        e == ling -> "旺"; SHENG[ling] == e -> "相"; SHENG[e] == ling -> "休"
        KE[e] == ling -> "囚"; else -> "死"
    } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("木", "火", "土", "金", "水").forEach { e ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(GY_ACCENT_BG)
                .padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("$e·${state(e)}", color = gyWx(e), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ===== Tab 神煞 =====
@Composable
private fun GyShenSha(r: BaziTaibu.Result) {
    GyCard {
        Text("全局神煞", color = GY_ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (r.globalShenSha.isEmpty()) Text("无", color = GY_DIM, fontSize = 12.sp)
        else Text(r.globalShenSha.joinToString("、"), color = GY_PURPLE, fontSize = 13.sp, lineHeight = 22.sp)
    }
    Spacer(Modifier.height(10.dp))
    GyCard {
        Text("各柱神煞落点", color = GY_ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        r.pillars.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("${p.name}(${p.ganZhi})", color = GY_INK, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp))
                Text(p.shenSha.joinToString("、").ifEmpty { "—" }, color = GY_PURPLE, fontSize = 12.sp)
            }
        }
    }
    val relations = r.relations + r.banHe + r.sanHui + r.ganWuHe + r.ganChong
    if (relations.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        GyCard {
            Text("刑冲合害", color = GY_ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            relations.forEach { rel ->
                Text("• ${rel.desc}", color = GY_SUB, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }
    }
}

// ===== Tab 大运流年（逐级钻取）=====
@Composable
private fun GyDaYun(r: BaziTaibu.Result) {
    var expanded by remember { mutableStateOf<Int?>(null) }
    var selYear by remember { mutableStateOf<Int?>(null) }
    GyCard {
        Text("起运：${r.qiYunText}", color = GY_ACCENT, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        if (r.xiaoYun.isNotEmpty()) Text("小运：" + r.xiaoYun.take(5).joinToString(" "), color = GY_SUB, fontSize = 11.sp)
    }
    Spacer(Modifier.height(10.dp))
    Text("大运（点开展开流年）", color = GY_ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
    if (r.dayun.isEmpty()) {
        GyCard { Text("无大运信息", color = GY_DIM, fontSize = 12.sp) }
        return
    }
    r.dayun.forEachIndexed { idx, dy ->
        GyCard {
            Row(Modifier.fillMaxWidth().clickable {
                expanded = if (expanded == idx) null else idx; selYear = null
            }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${dy.ganZhi}  ${dy.startYear}年起 · ${dy.startAge}岁",
                        color = GY_INK, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${dy.tenGod} · ${dy.naYin} · ${dy.diShi}", color = GY_SUB, fontSize = 11.sp)
                }
                Text(if (expanded == idx) "▲" else "▼", color = GY_ACCENT, fontSize = 13.sp)
            }
            if (expanded == idx) {
                Spacer(Modifier.height(8.dp))
                Text("十神：${dy.tenGod} / 支：${dy.branchTenGod}", color = GY_SUB, fontSize = 11.sp)
                if (dy.shenSha.isNotEmpty())
                    Text("神煞：${dy.shenSha.joinToString("、")}", color = GY_PURPLE, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                // 流年网格
                Text("流年（点选看详情）", color = GY_ACCENT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dy.liunian.forEach { ln ->
                        val sel = selYear == ln.year
                        Box(Modifier.width(66.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (sel) GY_ACCENT else GY_ACCENT_BG)
                            .clickable { selYear = if (sel) null else ln.year }
                            .padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                            Text("${ln.year % 100}\n${ln.ganZhi}", color = if (sel) Color.White else GY_INK,
                                fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
                        }
                    }
                }
                // 选中流年详情
                dy.liunian.firstOrNull { it.year == selYear }?.let { ln ->
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(GY_ACCENT_BG).padding(10.dp)) {
                        Column {
                            Text("${ln.year}年 ${ln.ganZhi} · ${ln.age}岁 · ${ln.tenGod}",
                                color = GY_ACCENT, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("纳音：${ln.naYin} · 星运：${ln.diShi}", color = GY_SUB, fontSize = 11.sp)
                            if (ln.taiSui.isNotEmpty())
                                Text("太岁：${ln.taiSui.joinToString("、")}", color = GY_RED, fontSize = 11.sp)
                            if (ln.shenSha.isNotEmpty())
                                Text("神煞：${ln.shenSha.joinToString("、")}", color = GY_PURPLE, fontSize = 11.sp)
                            if (ln.relations.isNotEmpty())
                                Text("应事：${ln.relations.joinToString("、")}", color = GY_SUB, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ===== 通用小组件 =====
@Composable
private fun GyCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(GY_CARD)
        .border(1.dp, GY_LINE, RoundedCornerShape(10.dp)).padding(12.dp), content = content)
}

@Composable
private fun GyKV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(k, color = GY_SUB, fontSize = 12.sp, modifier = Modifier.width(76.dp))
        Text(v, color = GY_INK, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

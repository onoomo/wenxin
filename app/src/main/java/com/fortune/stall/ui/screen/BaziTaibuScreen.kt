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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 八字 · 太卜口径独立模块（引擎 BaziTaibu，MIT taibu-core）。
 * 起卦弹窗 → 结果页（信息 + 四柱盘 + 关系 + 五行 + 神煞 + 大运流年级联）。
 */
@Composable
fun BaziTaibuScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var question by remember { mutableStateOf("") }
    var isMan by remember { mutableStateOf(true) }
    var boundary by remember { mutableStateOf(0) } // 0=午夜换日 1=子时换日
    var showDate by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<BaziTaibu.Result?>(null) }

    if (result != null) {
        BaziTbDetail(result!!, question, onBack)
        return
    }

    JiDialogShell(title = "八 字 · 太 卜", onBack = onBack, buttonText = "排 盘", onCast = {
        try {
            val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
            val r = BaziTaibu.paipan(dt, isMan, if (boundary == 0) "midnight" else "zi")
            val face = baziTbFaceText(r, question)
            db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                question, "bazitaibu", "八字·太卜", face, "")
            result = r
        } catch (e: Exception) {
            Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }) {
        JiRow("出生时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(12.dp))
        JiRow("性别：") { JiChipLine("", listOf("男", "女"), if (isMan) 0 else 1) { isMan = it == 0 } }
        Spacer(Modifier.height(10.dp))
        JiRow("换日界：") { JiChipLine("", listOf("午夜换日", "子时换日"), boundary) { boundary = it } }
        Spacer(Modifier.height(10.dp))
        JiRow("预测事项：") { JiInput(question, { question = it.take(30) }, "输入事项（选填）", Modifier.fillMaxWidth()) }
    }
    if (showDate) BoardDateTimeDialog(initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
        onDismiss = { showDate = false },
        onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false })
}

private val TB_LABEL = Color(0xFF4A5A8A)
private val TB_TEXT = Color(0xFF2A2620)
private val TB_RED = Color(0xFFB0392F)
private val TB_GOLD = Color(0xFF9A6B2F)

@Composable
private fun BaziTbDetail(r: BaziTaibu.Result, question: String, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("命盘", "关系神煞", "大运流年")
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F1E8))) {
        BoardNavBar("八 字 · 太 卜", onBack)
        // Tab 条
        Row(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 2.dp)) {
            tabs.forEachIndexed { i, t ->
                Box(Modifier.weight(1f).height(40.dp)
                    .background(if (tab == i) TB_LABEL else Color.Transparent)
                    .clickable { tab = i }, contentAlignment = Alignment.Center) {
                    Text(t, color = if (tab == i) Color.White else TB_LABEL, fontSize = 14.sp,
                        fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (tab) {
                0 -> TbMingpan(r, question)
                1 -> TbRelations(r)
                2 -> TbDayun(r)
            }
            Spacer(Modifier.height(16.dp))
            com.fortune.stall.ui.components.ResultActionBar(faceText = baziTbFaceText(r, question))
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ===== Tab 1：命盘 =====
@Composable
private fun TbMingpan(r: BaziTaibu.Result, question: String) {
    Spacer(Modifier.height(8.dp))
    // 信息区
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 6.dp)) {
        TbInfo("事项", question.ifEmpty { "—" }, question.isEmpty())
        TbInfo("时间", r.solarText + " · " + r.gender)
        TbInfo("农历", r.lunarText)
        TbInfo("日主", r.dayMaster + " · " + (BaziTaibu.GAN_WX[r.dayMaster] ?: ""))
    }
    Spacer(Modifier.height(10.dp))
    // 四柱盘：每柱一个竖卡
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        r.pillars.forEach { TbPillarCard(it, Modifier.weight(1f)) }
    }
    Spacer(Modifier.height(10.dp))
    // 胎元/命宫/胎息/身宫
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFDDD5BF), RoundedCornerShape(6.dp))
        .background(Color.White).padding(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("胎元" to r.taiYuan, "命宫" to r.mingGong, "胎息" to r.taiXi, "身宫" to r.shenGong).forEach {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(it.first, color = TB_LABEL, fontSize = 12.sp)
                    Text(it.second, color = TB_TEXT, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    // 五行 + 旺相休囚死
    TbWuXing(r)
    Spacer(Modifier.height(10.dp))
    // 空亡
    TbInfoCard("空亡", r.kongWang.xun + " · " + r.kongWang.kongZhi.joinToString(""))
}

@Composable
private fun TbPillarCard(p: BaziTaibu.Pillar, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(6.dp)).background(Color.White)
        .border(1.dp, Color(0xFFDDD5BF), RoundedCornerShape(6.dp)).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(p.name, color = TB_LABEL, fontSize = 12.sp)
        Text(p.tenGod, color = TB_GOLD, fontSize = 11.sp)
        Text(p.gan, color = TB_RED, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Text(p.zhi, color = TB_RED, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(2.dp))
        Text("纳音", color = Color(0xFF999999), fontSize = 10.sp)
        Text(p.naYin, color = TB_TEXT, fontSize = 11.sp)
        Text("地势 " + p.diShi, color = TB_TEXT, fontSize = 11.sp)
        if (p.kong) Text("空亡", color = TB_RED, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Text("藏干", color = Color(0xFF999999), fontSize = 10.sp)
        p.hidden.forEach { h ->
            Text("${h.stem}·${h.tenGod}", color = TB_TEXT, fontSize = 10.sp)
        }
        if (p.shenSha.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(p.shenSha.joinToString(" "), color = TB_GOLD, fontSize = 9.sp,
                textAlign = TextAlign.Center, lineHeight = 11.sp)
        }
    }
}

@Composable
private fun TbWuXing(r: BaziTaibu.Result) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFDDD5BF), RoundedCornerShape(6.dp))
        .background(Color.White).padding(8.dp)) {
        Text("五行统计（含藏干）", color = TB_LABEL, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("木", "火", "土", "金", "水").forEach { w ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(w, color = wxColor(w), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${r.wuXing[w] ?: 0}", color = TB_TEXT, fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("月令旺衰：旺 ${r.wangShuai[0]} · 相 ${r.wangShuai[1]} · 休 ${r.wangShuai[2]} · 囚 ${r.wangShuai[3]} · 死 ${r.wangShuai[4]}",
            color = TB_TEXT, fontSize = 11.sp)
    }
}
private fun wxColor(w: String) = when (w) {
    "木" -> Color(0xFF3B7A4A); "火" -> Color(0xFFB0392F); "土" -> Color(0xFF9A6B2F)
    "金" -> Color(0xFF8A8A8A); else -> Color(0xFF3B5A8A)
}

// ===== Tab 2：关系神煞 =====
@Composable
private fun TbRelations(r: BaziTaibu.Result) {
    Spacer(Modifier.height(8.dp))
    TbSection("地支刑冲合害") {
        val all = r.relations + r.banHe + r.sanHui
        if (all.isEmpty()) TbEmpty() else all.forEach { TbInfoCard("", "${it.desc}（${it.pillars.joinToString("·")}）") }
    }
    Spacer(Modifier.height(10.dp))
    TbSection("天干合冲") {
        val all = r.ganWuHe + r.ganChong
        if (all.isEmpty()) TbEmpty() else all.forEach { TbInfoCard("", "${it.desc}（${it.pillars.joinToString("·")}）") }
    }
    Spacer(Modifier.height(10.dp))
    TbSection("全局神煞") {
        if (r.globalShenSha.isEmpty()) TbEmpty()
        else Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Text(r.globalShenSha.joinToString("  "), color = TB_GOLD, fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
    TbSection("各柱神煞") {
        r.pillars.forEach { p ->
            if (p.shenSha.isNotEmpty()) TbInfoCard(p.name, p.shenSha.joinToString(" "))
        }
    }
}

// ===== Tab 3：大运流年（点击大运展开流年）=====
@Composable
private fun TbDayun(r: BaziTaibu.Result) {
    var selDy by remember { mutableStateOf(0) }
    var selLn by remember { mutableStateOf(-1) }
    Spacer(Modifier.height(8.dp))
    TbInfoCard("起运", r.qiYunText)
    if (r.xiaoYun.isNotEmpty()) TbInfoCard("小运", r.xiaoYun.joinToString(" "))
    Spacer(Modifier.height(10.dp))
    Text("大运（点击展开流年）", color = TB_LABEL, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
    // 大运横排
    Row(Modifier.fillMaxWidth().horizontalScrollCompat().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        r.dayun.forEachIndexed { i, d ->
            Column(Modifier.width(58.dp).clip(RoundedCornerShape(6.dp))
                .background(if (selDy == i) TB_LABEL else Color.White)
                .border(1.dp, Color(0xFFDDD5BF), RoundedCornerShape(6.dp))
                .clickable { selDy = i; selLn = -1 }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(d.ganZhi, color = if (selDy == i) Color.White else TB_RED,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(d.tenGod, color = if (selDy == i) Color(0xFFFFDDB0) else TB_GOLD, fontSize = 11.sp)
                Text("${d.startAge}岁", color = if (selDy == i) Color.White else Color(0xFF888888), fontSize = 10.sp)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    // 选中大运的详情
    if (r.dayun.isNotEmpty()) {
        val d = r.dayun[selDy]
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(6.dp)).background(Color.White).padding(10.dp)) {
            Text("${d.ganZhi} 运 · ${d.startYear}年起 · ${d.tenGod} · 地势${d.diShi}",
                color = TB_LABEL, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (d.relations.isNotEmpty()) Text("与原局：" + d.relations.joinToString("、"), color = TB_GOLD, fontSize = 11.sp)
            if (d.shenSha.isNotEmpty()) Text("神煞：" + d.shenSha.joinToString(" "), color = TB_GOLD, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("流年（点击看详情）", color = TB_LABEL, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp))
        // 流年网格
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            d.liunian.chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { ln ->
                        val idx = d.liunian.indexOf(ln)
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(4.dp))
                            .background(if (selLn == idx) TB_LABEL else Color.White)
                            .border(1.dp, Color(0xFFDDD5BF), RoundedCornerShape(4.dp))
                            .clickable { selLn = if (selLn == idx) -1 else idx }.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${ln.year}", color = if (selLn == idx) Color.White else Color(0xFF888888), fontSize = 10.sp)
                            Text(ln.ganZhi, color = if (selLn == idx) Color.White else TB_RED, fontSize = 15.sp, fontFamily = FontFamily.Serif)
                        }
                    }
                }
            }
        }
        if (selLn >= 0 && selLn < d.liunian.size) {
            val ln = d.liunian[selLn]
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(6.dp)).border(1.dp, TB_GOLD, RoundedCornerShape(6.dp))
                .background(Color(0xFFFFF9E8)).padding(10.dp)) {
                Text("${ln.year} ${ln.ganZhi} · ${ln.age}岁 · ${ln.tenGod} · ${ln.naYin} · 地势${ln.diShi}",
                    color = TB_LABEL, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (ln.taiSui.isNotEmpty()) Text("太岁：" + ln.taiSui.joinToString("、"), color = TB_RED, fontSize = 12.sp)
                if (ln.relations.isNotEmpty()) Text("刑冲合害：" + ln.relations.joinToString("、"), color = TB_GOLD, fontSize = 11.sp)
                if (ln.shenSha.isNotEmpty()) Text("神煞：" + ln.shenSha.joinToString(" "), color = TB_GOLD, fontSize = 11.sp)
            }
        }
    }
}

// 简单横向滚动修饰（避免额外 import 实验性 API）
@Composable
private fun Modifier.horizontalScrollCompat(): Modifier =
    this.horizontalScroll(rememberScrollState())

// ===== 通用小组件 =====
@Composable
private fun TbInfo(label: String, value: String, placeholder: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = TB_LABEL, fontSize = 14.sp, modifier = Modifier.width(52.dp))
        Text(value, color = if (placeholder) Color(0xFFAAAAAA) else TB_TEXT, fontSize = 14.sp)
    }
}
@Composable
private fun TbInfoCard(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
        if (label.isNotEmpty()) Text(label, color = TB_LABEL, fontSize = 13.sp, modifier = Modifier.width(52.dp))
        Text(value, color = TB_TEXT, fontSize = 13.sp)
    }
}
@Composable
private fun TbSection(title: String, content: @Composable () -> Unit) {
    Text(title, color = TB_LABEL, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFDDD5BF), RoundedCornerShape(6.dp))
        .background(Color.White).padding(vertical = 4.dp)) { content() }
}
@Composable
private fun TbEmpty() {
    Text("无", color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
}

/** 完整八字盘面文本：供存档/卦账复制/问AI 统一使用 */
internal fun baziTbFaceText(r: BaziTaibu.Result, question: String, title: String = "八字排盘 · 太卜") = buildString {
    appendLine("【$title】")
    if (question.isNotBlank()) appendLine("事项：$question")
    appendLine("公历：${r.solarText}　${r.gender}")
    appendLine("农历：${r.lunarText}")
    appendLine("日主：${r.dayMaster}（${com.fortune.stall.engine.jishiyu.BaziTaibu.GAN_WX[r.dayMaster] ?: ""}）")
    appendLine()
    appendLine("── 四柱 ──")
    r.pillars.forEach { p ->
        val hidden = p.hidden.joinToString("、") { "${it.stem}·${it.tenGod}" }
        appendLine("${p.name} ${p.gan}${p.zhi} [${p.tenGod}] 纳音${p.naYin} 地势${p.diShi}${if (p.kong) " 空亡" else ""}")
        appendLine("    藏干：${hidden.ifEmpty { "无" }}")
        if (p.shenSha.isNotEmpty()) appendLine("    神煞：${p.shenSha.joinToString(" ")}")
    }
    appendLine()
    appendLine("胎元：${r.taiYuan}　命宫：${r.mingGong}　胎息：${r.taiXi}　身宫：${r.shenGong}")
    appendLine("空亡：${r.kongWang.xun} · ${r.kongWang.kongZhi.joinToString("")}")
    appendLine("五行（含藏干）：木${r.wuXing["木"] ?: 0} 火${r.wuXing["火"] ?: 0} 土${r.wuXing["土"] ?: 0} 金${r.wuXing["金"] ?: 0} 水${r.wuXing["水"] ?: 0}")
    appendLine("旺相休囚死：旺${r.wangShuai.getOrNull(0) ?: ""} 相${r.wangShuai.getOrNull(1) ?: ""} 休${r.wangShuai.getOrNull(2) ?: ""} 囚${r.wangShuai.getOrNull(3) ?: ""} 死${r.wangShuai.getOrNull(4) ?: ""}")
    appendLine()
    val rel = (r.relations + r.banHe + r.sanHui).joinToString("；") { "${it.desc}(${it.pillars.joinToString("·")})" }
    val ganRel = (r.ganWuHe + r.ganChong).joinToString("；") { "${it.desc}(${it.pillars.joinToString("·")})" }
    if (rel.isNotBlank()) appendLine("地支刑冲合害：$rel")
    if (ganRel.isNotBlank()) appendLine("天干合冲：$ganRel")
    if (r.globalShenSha.isNotEmpty()) appendLine("全局神煞：${r.globalShenSha.joinToString(" ")}")
    appendLine()
    appendLine("起运：${r.qiYunText}")
    if (r.xiaoYun.isNotEmpty()) appendLine("小运：${r.xiaoYun.joinToString(" ")}")
    r.dayun.forEach { d ->
        appendLine("大运 ${d.ganZhi}（${d.startYear}年起 ${d.startAge}岁）${d.tenGod} 地势${d.diShi}${if (d.relations.isNotEmpty()) " 原局:${d.relations.joinToString("、")}" else ""}${if (d.shenSha.isNotEmpty()) " 神煞:${d.shenSha.joinToString(" ")}" else ""}")
        d.liunian.forEach { ln ->
            appendLine("　　${ln.year} ${ln.ganZhi} ${ln.age}岁 ${ln.tenGod} ${ln.naYin} 地势${ln.diShi}${if (ln.taiSui.isNotEmpty()) " 太岁:${ln.taiSui.joinToString("、")}" else ""}${if (ln.shenSha.isNotEmpty()) " 神煞:${ln.shenSha.joinToString(" ")}" else ""}")
        }
    }
}

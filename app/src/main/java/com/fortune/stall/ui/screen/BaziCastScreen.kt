package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.BaziCascade
import com.fortune.stall.engine.jishiyu.BaziChuangong
import com.fortune.stall.engine.jishiyu.BaziChart
import com.fortune.stall.engine.jishiyu.BaziFull
import com.fortune.stall.engine.jishiyu.BaziXiangpan
import com.fortune.stall.engine.jishiyu.JishiyuBase
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val BZ_RED = Color(0xFFB0392E)
private val BZ_GOLD = Color(0xFFD9A441)
private val BZ_LINE = Color(0x52D9A441)
private val BZ_CARD = Color(0x14FFFFFF)

/**
 * 八字排盘专用起卦页 —— 依据 ji.js.cn 八字排盘。
 * 起卦表单：姓名 / 性别 / 出生公历时间 / 出生地经度(真太阳时)；排盘后进入完整命盘详情页并自动存档。
 */
@Composable
fun BaziCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var chart by remember { mutableStateOf<BaziChart.Chart?>(null) }
    var fullData by remember { mutableStateOf<BaziFull.Data?>(null) }
    var xpData by remember { mutableStateOf<BaziXiangpan.Data?>(null) }
    if (chart == null) {
        JiMingpanCastForm(title = "八 字 排 盘", onBack = onBack) { f ->
            try {
                val c = BaziChart.build(f.dateTime, f.isMan)
                val fd = BaziFull.build(f.dateTime, f.isMan, c)
                fullData = fd
                xpData = BaziXiangpan.build(f.dateTime, f.isMan, c, fd)
                chart = c
                val q = f.name.ifBlank { "${f.dateTime.year}年出生 · 八字排盘" }
                val createdAt = f.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                if (!f.noSave) {
                    val id = db.insert(createdAt, q, "bazi", if (f.isMan) "男命" else "女命", "排盘中…", c.baziText)
                    db.updateFace(id, baziFaceText(c, q, createdAt))
                    Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    chart?.let { c -> BaziDetailScreen(chart = c, full = fullData, xp = xpData, onBack = { chart = null }) }
}

@Composable
private fun BaziDetailScreen(chart: BaziChart.Chart, full: BaziFull.Data?, xp: BaziXiangpan.Data?, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("基本", "命盘", "详盘")
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F1E8))) {
        // 顶栏（仿原版吉时雨八字 红色标题栏）
        Box(Modifier.fillMaxWidth().background(BZ_RED).padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text("吉时雨八字", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(28.dp))
            }
        }
        // Tab 行（浅灰底，选中项红字红下划线）
        Row(Modifier.fillMaxWidth().background(Color(0xFFEFEDE6))) {
            tabs.forEachIndexed { i, t ->
                val sel = tab == i
                Column(
                    Modifier.weight(1f).clickable { tab = i }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t, color = if (sel) BZ_RED else Color(0xFF666666),
                        fontSize = 16.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    if (sel) Box(Modifier.width(28.dp).height(2.dp).background(BZ_RED))
                }
            }
        }
        // 页面内容
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth()) {
                when (tab) {
                    0 -> BzBasicPage(chart, full)
                    1 -> BzMingpanPage(chart, full)
                    2 -> BzDetailPage(chart, full, xp)
                }
                Spacer(Modifier.height(12.dp))
                com.fortune.stall.ui.components.ResultActionBar(
                    faceText = baziFaceText(chart, chart.solar, "")
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/* ============================ 基本页（图1） ============================ */
@Composable
private fun BzBasicPage(c: BaziChart.Chart, f: BaziFull.Data?) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // 顶部：生肖图标 + 基本信息
        Row(Modifier.fillMaxWidth().background(Color.White).padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, BZ_RED, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Text(c.shengXiao, fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BzInfo("农历", (f?.nongliFull ?: c.lunar) + "  " + (if (c.gender == "男") "乾造" else "坤造"))
                BzInfo("北京时间", c.solar)
                BzInfo("真太阳时", c.solar)
                BzInfo("出生地区", f?.birthArea ?: "北京市")
            }
        }
        Spacer(Modifier.height(6.dp))
        // 出生节气 + 胎命身卦
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzInfo("出生节气", f?.jieqiFull ?: c.jieQi)
            BzInfo("出生生日", c.lunar)
            BzInfo("命卦", (f?.mingGuaDir ?: "") + " · " + c.mingGua)
        }
        Spacer(Modifier.height(6.dp))
        // 五行统计条形图
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("五行统计")
            val qty = f?.wuXingQty ?: c.wuXing.toMap()
            val order = listOf("木", "火", "土", "金", "水")
            val shishenOf = mapOf("木" to "财星", "火" to "官星", "土" to "印星", "金" to "比劫", "水" to "食伤")
            val barColor = mapOf("木" to Color(0xFF4CAF50), "火" to Color(0xFFE53935), "土" to Color(0xFFC8883C),
                "金" to Color(0xFFD9A441), "水" to Color(0xFF3D7FC1))
            val maxQty = (order.maxOfOrNull { qty[it] ?: 0 } ?: 1).coerceAtLeast(1)
            for (wx in order) {
                val cnt = qty[wx] ?: 0
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(wx, color = Color(0xFF333333), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFECF0F3))) {
                        Box(Modifier.fillMaxWidth(cnt / maxQty.toFloat()).height(9.dp)
                            .clip(RoundedCornerShape(5.dp)).background(barColor[wx] ?: Color.Gray))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("$cnt 个${shishenOf[wx] ?: ""}", color = Color(0xFF666666), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 袁天罡称骨算命
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("袁天罡称骨算命")
            BzInfo("骨重", c.chenggu.weight)
            Text(c.chenggu.verse, color = Color(0xFF333333), fontSize = 13.sp, lineHeight = 21.sp,
                modifier = Modifier.padding(vertical = 6.dp))
            if (f != null && f.chengguDetail.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                // 解析文本含 <br/> 分段标记，逐段渲染
                f.chengguDetail.split("<br/>", "<br>", "\n").filter { it.isNotBlank() }.forEach { seg ->
                    val isHead = seg in setOf("古文记载", "综合解析", "综合详解", "详解")
                    if (isHead) {
                        Spacer(Modifier.height(8.dp))
                        BzSection(seg)
                    } else {
                        Text(seg.trim(), color = Color(0xFF444444), fontSize = 12.5.sp, lineHeight = 20.sp,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("* 以上内容来自互联网，仅供参考，不等于真实评测。", color = Color(0xFF999999), fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

/* ============================ 命盘页（图2） ============================ */
@Composable
private fun BzMingpanPage(c: BaziChart.Chart, f: BaziFull.Data?) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // 顶部信息
        Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, BZ_RED, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) { Text(c.shengXiao, fontSize = 32.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    BzInfo("农历", (f?.nongliFull ?: c.lunar) + "  (" + (if (c.gender == "男") "乾造" else "坤造") + ")")
                    BzInfo("北京时间", c.solar)
                    BzInfo("真太阳时", c.solar)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 四柱排盘表格
        Column(Modifier.fillMaxWidth().background(Color.White).padding(8.dp)) {
            Row(Modifier.fillMaxWidth().background(Color(0xFFF7F7F7)).padding(vertical = 6.dp)) {
                Text("四柱", color = Color(0xFFFFFFFF), fontSize = 12.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
                for (h in listOf("年柱", "月柱", "日柱", "时柱")) Text(h, color = Color(0xFF888888), fontSize = 13.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            // 十神
            BzTableRow("十神", c.pillars.map { it.shiShen }) { Color(0xFF222222) to 13.sp }
            // 天干（五行着色 + 大字）
            BzTableRow("天干", c.pillars.map { it.gan }) { g -> (WX_COLOR_DETAIL[GAN_WX_OF(g)] ?: BZ_RED) to 24.sp }
            // 地支（五行着色 + 大字）
            BzTableRow("地支", c.pillars.map { it.zhi }) { z -> (WX_COLOR_DETAIL[JishiyuBase.ZHI_WUXING[z] ?: ""] ?: BZ_RED) to 24.sp }
            // 藏干+十神（合并，每格分行）
            Row(Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).padding(vertical = 6.dp)) {
                Text("藏干", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
                c.pillars.forEach { p ->
                    val gs = p.cangGan.map { it.toString() }; val ss = p.cangShen.split("·")
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        for (i in gs.indices) Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(gs[i], color = WX_COLOR_LIGHT[GAN_WX_OF(gs[i])] ?: Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(ss.getOrNull(i) ?: "", color = Color(0xFF333333), fontSize = 12.sp)
                        }
                    }
                }
            }
            BzTableRow("地势", c.pillars.map { it.deShi }) { Color(0xFF222222) to 12.sp }
            BzTableRow("自坐", c.pillars.map { it.ziZuo }) { Color(0xFF222222) to 12.sp }
            BzTableRow("空亡", c.pillars.map { it.kongWang }) { Color(0xFF222222) to 12.sp }
            BzTableRow("纳音", c.pillars.map { it.naYin }) { Color(0xFF1565C0) to 12.sp }
            // 神煞（每行多个，竖排）
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("神煞", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
                c.pillars.forEach { p ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        p.shenSha.split("、", "·").filter { it.isNotBlank() && it != "无" }.forEach {
                            Text(it, color = Color(0xFF3F51B5), fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 五行旺衰（旺相休囚死）
        if (f != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                val labels = listOf("旺", "相", "休", "囚", "死")
                val colors = listOf(Color(0xFF2E7D32), Color(0xFF388E3C), Color(0xFFF9A825), Color(0xFFEF6C00), Color(0xFFC62828))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))) {
                    f.wangShuai.forEachIndexed { i, wx ->
                        Box(Modifier.weight(1f).background(colors[i]).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(wx + labels[i], color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                BzLiuYi("天干留意", f.liuYi.tiangan)
                Spacer(Modifier.height(6.dp))
                BzLiuYi("地支留意", f.liuYi.dizhi)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("* 提示：点击十神、神煞、纳音显示详情。", color = Color(0xFF999999), fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BzLiuYi(title: String, items: List<String>) {
    BzSection(title)
    if (items.isEmpty()) {
        Text("无", color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (s in items) {
            Text(s, color = BZ_RED, fontSize = 11.5.sp,
                modifier = Modifier.border(1.dp, BZ_LINE, RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF6F4), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

/* ============================ 详盘页（图3） ============================ */
private val WX_COLOR_DETAIL = mapOf(
    "木" to Color(0xFF00897B), "火" to Color(0xFFD32F2F), "土" to Color(0xFFAD6A00),
    "金" to Color(0xFFD98C00), "水" to Color(0xFF1565C0)
)
private val WX_COLOR_LIGHT = mapOf(
    "木" to Color(0xFF2E7D32), "火" to Color(0xFFC62828), "土" to Color(0xFFB8860B),
    "金" to Color(0xFFB8860B), "水" to Color(0xFF1565C0)
)

@Composable
private fun BzDetailPage(c: BaziChart.Chart, f: BaziFull.Data?, xp: BaziXiangpan.Data?) {
    var taimingOn by remember { mutableStateOf(false) }   // 胎命身：开=左列换胎元/命宫/身宫
    var chuangongOn by remember { mutableStateOf(false) } // 串宫：开=显示干支串宫两行
    var taisuiType by remember { mutableIntStateOf(1) }   // 起太岁：1流年 2大运 3日干
    // 顶部"流年/大运"列随下方大运选区联动（默认第一步大运 + 其首流年）
    var topDy by remember { mutableIntStateOf(0) }
    var topLn by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // 顶部信息卡：生肖 + 农历 + 北京时间/真太阳时 + 胎命身/串宫开关
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, BZ_RED, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Text(c.shengXiao, fontSize = 32.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                BzInfo("农历", (f?.nongliFull ?: c.lunar) + "  " + (if (c.gender == "男") "(乾造)" else "(坤造)"))
                BzInfo("北京时间", c.solar)
                BzInfo("真太阳时", c.solar)
            }
            Column(horizontalAlignment = Alignment.End) {
                BzToggle("胎命身", taimingOn, onToggle = { taimingOn = it })
                Spacer(Modifier.height(6.dp))
                BzToggle("串宫", chuangongOn, onToggle = { chuangongOn = it })
            }
        }
        Spacer(Modifier.height(6.dp))

        // 8 列对照表（流日/流月/流年/小运 ‖ 年月日时），横向滚动
        if (xp != null) {
            val dayGan = c.pillars[2].gan
            val isMan = c.gender == "男"
            val niannayin = c.pillars[0].naYin
            val bazi = c.pillars.flatMap { listOf(it.gan, it.zhi) }
            // 胎命身三柱（真值：庚寅/庚寅/壬辰）
            val sg = remember(c) { BaziChuangong.sanGong(c.yearGan, c.pillars[1].gan, c.pillars[1].zhi, c.pillars[3].zhi) }
            val tyCol = remember(sg) { BaziXiangpan.colOf(sg.taiyuan, dayGan, bazi, isMan, 7, niannayin) }
            val mgCol = remember(sg) { BaziXiangpan.colOf(sg.minggong, dayGan, bazi, isMan, 6, niannayin) }
            val shCol = remember(sg) { BaziXiangpan.colOf(sg.shengong, dayGan, bazi, isMan, 5, niannayin) }

            // 串宫：太岁支 + 每列干支的十二神
            val taisuiZhi = BaziChuangong.taisuiZhi(taisuiType, xp.liunian.zhi, xp.xiaoyun.zhi, dayGan)
            fun gods(gz: String) = BaziChuangong.gods12(gz, taisuiZhi)
            // 顶部表：原版紧凑 7 列「日期 | 流年 | 大运 || 年柱 月柱 日柱 时柱」
            // 流年/大运列随下方大运选区联动；胎命身开时左两列换为胎元/命宫/身宫
            val dyList = f?.dayun ?: emptyList()
            val safeDy = topDy.coerceIn(0, maxOf(0, dyList.size - 1))
            val selDayun = dyList.getOrNull(safeDy)
            val lnList = selDayun?.liunian ?: emptyList()
            val safeLn = topLn.coerceIn(0, maxOf(0, lnList.size - 1))
            val selLiunian = lnList.getOrNull(safeLn)
            val liunianCol = selLiunian?.let { BaziXiangpan.colOf(it.ganzhi, dayGan, bazi, isMan, 6, niannayin) }
            val dayunCol = selDayun?.let { BaziXiangpan.colOf(it.ganzhi, dayGan, bazi, isMan, 1, niannayin) }
            // 起太岁方式：点击"串宫"行标签切换（流年→大运→日干）
            Column(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    // 行标签列（固定）
                    Column(Modifier.width(34.dp)) {
                        Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                            Text("日期", color = Color(0xFF999999), fontSize = 10.sp)
                        }
                        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) { Text("十神", color = Color(0xFF999999), fontSize = 10.sp) }
                        if (chuangongOn) Box(Modifier.fillMaxWidth().height(22.dp).clickable { taisuiType = taisuiType % 3 + 1 },
                            contentAlignment = Alignment.Center) { Text("串宫", color = Color(0xFFC62828), fontSize = 9.sp) }
                        Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) { Text("天干", color = Color(0xFF999999), fontSize = 10.sp) }
                        Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) { Text("地支", color = Color(0xFF999999), fontSize = 10.sp) }
                        if (chuangongOn) Box(Modifier.fillMaxWidth().height(22.dp).clickable { taisuiType = taisuiType % 3 + 1 },
                            contentAlignment = Alignment.Center) { Text("串宫", color = Color(0xFFC62828), fontSize = 9.sp) }
                        listOf("藏干","地势","自坐","空亡","纳音").forEach { lbl ->
                            Box(Modifier.fillMaxWidth().height(if (lbl=="藏干") 84.dp else 34.dp),
                                contentAlignment = Alignment.Center) { Text(lbl, color = Color(0xFF999999), fontSize = 10.sp) }
                        }
                    }
                    // 胎命身开=胎元/命宫/身宫；关=流年/大运（与原版一致）
                    if (taimingOn) {
                        XpColCard(tyCol, "胎元", sg.taiyuan, showGods = chuangongOn,
                            ganGod = gods(sg.taiyuan).gan, zhiGod = gods(sg.taiyuan).zhi)
                        XpColCard(mgCol, "命宫", sg.minggong, showGods = chuangongOn,
                            ganGod = gods(sg.minggong).gan, zhiGod = gods(sg.minggong).zhi)
                        XpColCard(shCol, "身宫", sg.shengong, showGods = chuangongOn,
                            ganGod = gods(sg.shengong).gan, zhiGod = gods(sg.shengong).zhi)
                    } else {
                        if (liunianCol != null) XpColCard(liunianCol, "流年", "${selLiunian!!.year}", showGods = chuangongOn,
                            ganGod = gods(liunianCol.ganzhi).gan, zhiGod = gods(liunianCol.ganzhi).zhi)
                        if (dayunCol != null) XpColCard(dayunCol, "大运", "${selDayun!!.startYear}", divider = true, showGods = chuangongOn,
                            ganGod = gods(dayunCol.ganzhi).gan, zhiGod = gods(dayunCol.ganzhi).zhi)
                    }
                    // 年月日时四柱
                    XpColCard(xp.year, "年柱", c.lunar.replace("日",""), showGods = chuangongOn,
                        ganGod = gods(xp.year.ganzhi).gan, zhiGod = gods(xp.year.ganzhi).zhi,
                        hiGan = chuangongOn && taisuiType==3)
                    XpColCard(xp.month, "月柱", "十月", showGods = chuangongOn,
                        ganGod = gods(xp.month.ganzhi).gan, zhiGod = gods(xp.month.ganzhi).zhi)
                    XpColCard(xp.day, "日柱", "十四", showGods = chuangongOn,
                        ganGod = gods(xp.day.ganzhi).gan, zhiGod = gods(xp.day.ganzhi).zhi,
                        hiGan = chuangongOn && taisuiType==3)
                    XpColCard(xp.hour, "时柱", "辰时", showGods = chuangongOn,
                        ganGod = gods(xp.hour.ganzhi).gan, zhiGod = gods(xp.hour.ganzhi).zhi)
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // 起运/交运信息
        if (f != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("起运：出生${f.qiYun.startYear}年${f.qiYun.startMonth}月${f.qiYun.startDay}天${f.qiYun.startHour}时后起运",
                        color = Color(0xFF444444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("${f.dayun.firstOrNull()?.startAge ?: 1}虚岁", color = Color(0xFF888888), fontSize = 11.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("交运：逢${f.qiYun.jiaoyunGan1}、${f.qiYun.jiaoyunGan2}年 ${f.qiYun.jieName}后${f.qiYun.days}天交大运",
                        color = Color(0xFF444444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("司令：${xp?.siLing ?: ""}", color = Color(0xFF888888), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            // 逐级下钻大运网格：点大运→流年，点流年→流月，点流月→流日（同步顶部流年/大运列）
            BzDayunDrill(c = c, f = f, onSelect = { dy, ln -> topDy = dy; topLn = ln })
            Spacer(Modifier.height(6.dp))
        }

        // 五行旺衰彩条
        if (f != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp)) {
                val labels = listOf("旺", "相", "休", "囚", "死")
                val colors = listOf(Color(0xFF1976D2), Color(0xFF00897B), Color(0xFFF9A825), Color(0xFF8D4E00), Color(0xFFE53935))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp))) {
                    f.wangShuai.forEachIndexed { i, wx ->
                        Box(Modifier.weight(1f).background(colors[i]).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(wx + labels[i], color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // 天干留意 + 地支留意（详盘：含流日流月流年小运）
        val ly = xp?.liuYi ?: f?.liuYi
        if (ly != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                BzLiuYi("天干留意", ly.tiangan)
                Spacer(Modifier.height(8.dp))
                BzLiuYi("地支留意", ly.dizhi)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("* 提示：点击十神、神煞、纳音显示详情。", color = Color(0xFF8A96B5), fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

/** 详盘 8 列表格中的一列：表头(公历/农历) + 9 行属性 */
@Composable
private fun XpColCard(
    col: BaziXiangpan.Col, head1: String, head2: String, divider: Boolean = false,
    showGods: Boolean = false, ganGod: String = "", zhiGod: String = "",
    hiGan: Boolean = false
) {
    Column(
        Modifier.width(52.dp)
            .then(if (divider) Modifier.border(1.dp, Color(0xFFE0E0E0)) else Modifier)
            .padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 表头
        Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
            Text(head1, color = Color(0xFF777777), fontSize = 11.sp)
        }
        // 十神
        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) { Text(col.ganShen, color = Color(0xFF222222), fontSize = 12.sp) }
        // 天干串宫行（十神下方）：红字白底
        if (showGods) Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            Text(ganGod, color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        // 天干
        Box(Modifier.fillMaxWidth().height(42.dp).background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
            Text(col.gan, color = WX_COLOR_DETAIL[col.ganWx] ?: BZ_RED, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
        // 地支
        Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
            Text(col.zhi, color = WX_COLOR_DETAIL[col.zhiWx] ?: BZ_RED, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
        // 地支串宫行（地支下方）：红字白底
        if (showGods) Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            Text(zhiGod, color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        // 藏干+十神
        Column(Modifier.fillMaxWidth().height(84.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val gs = col.cangGan.map { it.toString() }
            val ss = col.cangShen.split("·")
            for (i in gs.indices) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(gs.getOrNull(i) ?: "", color = WX_COLOR_LIGHT[GAN_WX_OF(gs.getOrNull(i) ?: "")] ?: Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(ss.getOrNull(i) ?: "", color = Color(0xFF333333), fontSize = 11.sp)
                }
            }
        }
        // 地势/自坐/空亡
        for (v in listOf(col.deShi, col.ziZuo, col.kongWang)) {
            Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) { Text(v, color = Color(0xFF222222), fontSize = 11.sp) }
        }
        // 纳音
        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
            Text(col.naYin, color = Color(0xFF1565C0), fontSize = 11.sp)
        }
    }
}

private fun GAN_WX_OF(gan: String) = when (gan) {
    "甲","乙" -> "木"; "丙","丁" -> "火"; "戊","己" -> "土"; "庚","辛" -> "金"; "壬","癸" -> "水"; else -> ""
}

/** 命盘页四柱表的一行：行标签 + 4 列（颜色/字号由回调决定） */
@Composable
private fun BzTableRow(label: String, values: List<String>, style: (String) -> Pair<Color, androidx.compose.ui.unit.TextUnit>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
        values.forEach { v ->
            val st = style(v)
            Text(v, color = st.first, fontSize = st.second, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
}

/**
 * 逐级下钻大运网格（对齐 ji.js.cn 详盘）：
 * 大运行(10步) → 点某步显示其 10 流年 → 点某流年显示其 12 流月 → 点某流月显示其流日。
 * 未选中任何下层时，该层隐藏；选中后在其下方展开，保持"点哪级出哪级"。
 */
@Composable
private fun BzDayunDrill(c: BaziChart.Chart, f: BaziFull.Data, onSelect: (Int, Int) -> Unit = { _, _ -> }) {
    val dayGan = c.pillars[2].gan
    var expanded by remember { mutableStateOf(false) }   // 大运开关：false=逐级下钻，true=树状全展开(原版图1)
    var selDy by remember { mutableIntStateOf(-1) }      // 选中的大运索引；-2=小运；-1 未选
    var selLn by remember { mutableIntStateOf(-1) }      // 选中的流年索引
    var selYue by remember { mutableIntStateOf(-1) }     // 选中的流月索引
    var selRi by remember { mutableIntStateOf(-1) }      // 选中的流日索引

    // 小运阶段(1-5岁)流年，点击小运时展开
    val xiaoyunLn = remember(f) { BaziCascade.xiaoyunLiunian(f.qiYun, f.dayun, dayGan) }
    // 当前选中的流年列表（大运 or 小运）
    val curLiunian: List<BaziFull.LiuNian> = when {
        selDy == -2 -> xiaoyunLn
        selDy in f.dayun.indices -> f.dayun[selDy].liunian
        else -> emptyList()
    }
    val doToggle: (Boolean) -> Unit = { v ->
        expanded = v
        selDy = -1; selLn = -1; selYue = -1; selRi = -1
    }

    Column(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 8.dp)) {
        if (expanded) {
            // ===== 专业细盘（原版）：大运/始于/流年/止于/流月 树状全展开 =====
            BzTreeAll(f = f, xiaoyunLn = xiaoyunLn, dayGan = dayGan, onToggle = doToggle)
        } else {
            // ===== 逐级下钻模式（原版图2）：点哪级出哪级 =====
            DrillRow(label = "大运", switch = { BzMiniSwitch(checked = false, onToggle = doToggle) }) {
                DrillHeadCell(head1 = "${f.qiYun.startYear}", head2 = "1-5岁", gan = "小", zhi = "运",
                    selected = selDy == -2,
                    onClick = { selDy = -2; selLn = -1; selYue = -1; selRi = -1 })
                f.dayun.forEachIndexed { i, dy ->
                    DrillHeadCell(head1 = "${dy.startYear}", head2 = "${dy.startAge}岁",
                        gan = dy.gan, zhi = dy.zhi, ganSs = dy.ganSs, zhiSs = dy.zhiSs,
                        selected = selDy == i,
                        onClick = { selDy = i; selLn = 0; selYue = -1; selRi = -1; onSelect(i, 0) })
                }
            }
            // 流年
            if (curLiunian.isNotEmpty()) {
                DrillRow(label = "流年") {
                    curLiunian.forEachIndexed { j, ln ->
                        DrillSubCell(head = "${ln.year}", gan = ln.gan, zhi = ln.zhi,
                            ganWx = ln.ganWx, zhiWx = ln.zhiWx, ganSs = ln.ganSs, zhiSs = ln.zhiSs,
                        selected = selLn == j,
                        onClick = { selLn = j; selYue = -1; selRi = -1; onSelect(selDy, j) })
                    }
                }
            }
            // 流月
            if (selLn in curLiunian.indices) {
                val ln = curLiunian[selLn]
                val yueList = remember(selDy, selLn) { BaziCascade.liuYue(ln, dayGan) }
                DrillRow(label = "流月") {
                    yueList.forEachIndexed { k, yue ->
                        DrillSubCell(head = yue.head, sub = yue.sub, gan = yue.gan, zhi = yue.zhi,
                            ganWx = yue.ganWx, zhiWx = yue.zhiWx, ganSs = yue.ganSs, zhiSs = yue.zhiSs,
                            selected = selYue == k,
                            onClick = { selYue = k; selRi = -1 })
                    }
                }
            }
            // 流日
            if (selLn in curLiunian.indices && selYue in 0..11) {
                val ln = curLiunian[selLn]
                val riList = remember(selDy, selLn, selYue) { BaziCascade.liuRi(ln, selYue, dayGan) }
                DrillRow(label = "流日") {
                    riList.forEachIndexed { d, ri ->
                        DrillSubCell(head = ri.head, sub = ri.sub, gan = ri.gan, zhi = ri.zhi,
                            ganWx = ri.ganWx, zhiWx = ri.zhiWx, ganSs = ri.ganSs, zhiSs = ri.zhiSs,
                            selected = selRi == d,
                            onClick = { selRi = d })
                    }
                }
            }
        }
    }
}

/** 专业细盘一列：大运头 + 始于 + 流年区 + 止于 */
private data class ProCol(
    val head1: String, val head2: String,
    val gan: String, val zhi: String, val ganSs: String, val zhiSs: String,
    val isSmall: Boolean, val liunian: List<BaziFull.LiuNian>,
    val cs: String, val endYear: Int
)

private const val PRO_HEAD_H = 82   // 大运头高度
private const val PRO_BEGIN_H = 40  // 始于行高度
private const val PRO_FLOW_H = 336  // 流年区高度(12格*28)
private const val PRO_END_H = 44    // 止于行高度

/** 专业细盘（原版 lab.yxbug 专业细盘）：大运/始于/流年/止于/流月 树状全展开 */
@Composable
private fun BzTreeAll(f: BaziFull.Data, xiaoyunLn: List<BaziFull.LiuNian>, dayGan: String, onToggle: (Boolean) -> Unit) {
    val cols = remember(f, xiaoyunLn, dayGan) {
        val list = mutableListOf<ProCol>()
        if (xiaoyunLn.isNotEmpty()) {
            val flow = xiaoyunLn.toMutableList()
            f.dayun.firstOrNull()?.liunian?.firstOrNull()?.let { flow.add(it) } // 交界年(起运年)
            list.add(ProCol(
                "${xiaoyunLn[0].year}", "1-5岁", "小", "运", "", "", true, flow,
                JishiyuBase.queryShengwang(dayGan, xiaoyunLn[0].zhi),
                xiaoyunLn[0].year + f.qiYun.startYear
            ))
        }
        f.dayun.forEachIndexed { i, dy ->
            val flow = dy.liunian.toMutableList()
            f.dayun.getOrNull(i + 1)?.liunian?.firstOrNull()?.let { flow.add(it) } // 交界年
            list.add(ProCol(
                "${dy.startYear}", "${dy.startAge}岁", dy.gan, dy.zhi, dy.ganSs, dy.zhiSs, false, flow,
                JishiyuBase.queryShengwang(dayGan, dy.liunian[0].zhi),
                dy.startYear + 10
            ))
        }
        list
    }
    var sel by remember { mutableIntStateOf(0) }
    val selLn = cols.getOrNull(sel)?.liunian?.firstOrNull()
    val yue = remember(selLn, dayGan) { if (selLn != null) BaziCascade.liuYue(selLn, dayGan) else emptyList() }

    Column(Modifier.fillMaxWidth().background(Color.White)) {
        // ===== 大运/始于/流年/止于 主表 =====
        Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFECECEC))) {
            // 左侧固定标签列
            Column(Modifier.width(34.dp).fillMaxHeight().background(Color(0xFFF6F3EC)),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.height(PRO_HEAD_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("大", color = Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("运", color = Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    BzMiniSwitch(checked = true, onToggle = onToggle)
                }
                Box(Modifier.height(PRO_BEGIN_H.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("始于", color = Color(0xFF666666), fontSize = 11.sp)
                }
                Box(Modifier.height(PRO_FLOW_H.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("流年", color = Color(0xFF666666), fontSize = 11.sp)
                }
                Box(Modifier.height(PRO_END_H.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("止于", color = Color(0xFF666666), fontSize = 11.sp)
                }
            }
            // 右侧横滚列
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                cols.forEachIndexed { idx, col ->
                    ProTreeColumn(col = col, selected = idx == sel, onClick = { sel = idx })
                }
            }
        }
        // ===== 流月行：其他月份 + 立冬/大雪/小寒 =====
        Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFECECEC))) {
            Box(Modifier.width(34.dp).height(98.dp).background(Color(0xFFF6F3EC)), contentAlignment = Alignment.Center) {
                Text("流月", color = Color(0xFF666666), fontSize = 11.sp)
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(50.dp).height(98.dp).background(Color(0xFFF5F5F5))
                    .border(0.5.dp, Color(0xFFE8E8E8)), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    listOf("其","他","月","份").forEach { Text(it, color = Color(0xFF888888), fontSize = 12.sp) }
                    Text("?", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                }
                for (mi in 9..11) {
                    val m = yue.getOrNull(mi)
                    if (m != null) ProYueCell(m = m, dayGan = dayGan)
                }
            }
        }
    }
}

/** 专业细盘的一列 */
@Composable
private fun ProTreeColumn(col: ProCol, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(52.dp).clickable(onClick = onClick)
        .background(if (selected) Color(0xFFFDECEA) else Color.White)
        .border(0.5.dp, Color(0xFFECECEC)), horizontalAlignment = Alignment.CenterHorizontally) {
        // 大运头
        Column(Modifier.height(PRO_HEAD_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(col.head1, color = Color(0xFF888888), fontSize = 10.sp, lineHeight = 12.sp)
            Text(col.head2, color = Color(0xFF888888), fontSize = 10.sp, lineHeight = 12.sp)
            if (col.isSmall) {
                Text("小", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("运", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(col.gan, color = WX_COLOR_DETAIL[GAN_WX_OF(col.gan)] ?: BZ_RED, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(col.ganSs, color = Color(0xFFC62828), fontSize = 9.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(col.zhi, color = WX_COLOR_DETAIL[JishiyuBase.ZHI_WUXING[col.zhi] ?: ""] ?: BZ_RED, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(col.zhiSs, color = Color(0xFFC62828), fontSize = 9.sp)
                }
            }
            Text(col.cs, color = Color(0xFF999999), fontSize = 10.sp)
        }
        // 始于（年份 + 首年流年干支）
        val begin = col.liunian.firstOrNull()
        Column(Modifier.height(PRO_BEGIN_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(if (begin != null) "${begin.year}" else "", color = Color(0xFF888888), fontSize = 10.sp)
            if (begin != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(begin.gan, color = WX_COLOR_DETAIL[begin.ganWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(begin.zhi, color = WX_COLOR_DETAIL[begin.zhiWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }
        // 流年区（竖排干支，带五行色）
        Column(Modifier.height(PRO_FLOW_H.dp).fillMaxWidth()) {
            col.liunian.forEach { ln ->
                Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(ln.gan, color = WX_COLOR_DETAIL[ln.ganWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(ln.zhi, color = WX_COLOR_DETAIL[ln.zhiWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }
        // 止于（年份 + 长生）
        Column(Modifier.height(PRO_END_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("${col.endYear}", color = Color(0xFF888888), fontSize = 10.sp)
            Text(col.cs, color = Color(0xFF666666), fontSize = 12.sp)
        }
    }
}

/** 流月格：节气名 + 月/日 + 干支带十神 + 长生 */
@Composable
private fun ProYueCell(m: BaziCascade.Cell, dayGan: String) {
    val cs = JishiyuBase.queryShengwang(dayGan, m.zhi)
    Column(Modifier.width(56.dp).height(98.dp).background(Color(0xFFF6F3EC))
        .border(0.5.dp, Color(0xFFE8E8E8)), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(m.head, color = Color(0xFF444444), fontSize = 12.sp)
        Text(m.sub, color = Color(0xFF888888), fontSize = 10.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.gan, color = WX_COLOR_DETAIL[m.ganWx] ?: BZ_RED, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(m.ganSs, color = Color(0xFFC62828), fontSize = 9.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.zhi, color = WX_COLOR_DETAIL[m.zhiWx] ?: BZ_RED, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(m.zhiSs, color = Color(0xFFC62828), fontSize = 9.sp)
        }
        if (cs.isNotEmpty()) Text(cs, color = Color(0xFF888888), fontSize = 10.sp)
    }
}

/** 红色小开关（仿原版大运行开关） */
@Composable
private fun BzMiniSwitch(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier.size(width = 24.dp, height = 13.dp).clip(RoundedCornerShape(7.dp))
            .background(if (checked) BZ_RED else Color(0xFFC9C9C9))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 1.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(Color.White))
    }
}

/** 下钻表格的一行：左侧竖排行标签(可带开关) + 横向滚动的格子 */
@Composable
private fun DrillRow(label: String, switch: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFECECEC))) {
        Column(Modifier.width(30.dp).background(Color(0xFFF7F4EC)).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (switch != null) Arrangement.Center else Arrangement.Center) {
            Text(label, color = Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            if (switch != null) { Spacer(Modifier.height(4.dp)); switch() }
        }
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) { content() }
    }
}

/** 大运行格子：顶部两年份信息 + 干支带十神（选中红框高亮） */
@Composable
private fun DrillHeadCell(
    head1: String, head2: String, gan: String, zhi: String,
    ganSs: String = "", zhiSs: String = "",
    selected: Boolean, onClick: () -> Unit
) {
    Column(
        Modifier.width(46.dp).clickable(onClick = onClick)
            .background(if (selected) Color(0xFFFDECEA) else Color.White)
            .then(if (selected) Modifier.border(1.5.dp, BZ_RED) else Modifier.border(0.5.dp, Color(0xFFEEEEEE)))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(head1, color = Color(0xFF888888), fontSize = 9.sp, lineHeight = 11.sp)
        Text(head2, color = Color(0xFF888888), fontSize = 9.sp, lineHeight = 11.sp)
        Spacer(Modifier.height(2.dp))
        if (zhi == "运" && gan == "小") {
            Text("小", color = Color(0xFF666666), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("运", color = Color(0xFF666666), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(gan, color = WX_COLOR_DETAIL[GAN_WX_OF(gan)] ?: Color(0xFF222222), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(ganSs, color = Color(0xFFC62828), fontSize = 8.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(zhi, color = WX_COLOR_DETAIL[JishiyuBase.ZHI_WUXING[zhi] ?: ""] ?: Color(0xFF222222), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(zhiSs, color = Color(0xFFC62828), fontSize = 8.sp)
            }
        }
    }
}

/** 流年/流月/流日格子：表头(head/sub) + 干支带十神 */
@Composable
private fun DrillSubCell(
    head: String, sub: String = "", gan: String, zhi: String,
    ganWx: String, zhiWx: String, ganSs: String, zhiSs: String,
    selected: Boolean, onClick: () -> Unit
) {
    Column(
        Modifier.width(46.dp).clickable(onClick = onClick)
            .background(if (selected) Color(0xFFFDECEA) else Color.White)
            .then(if (selected) Modifier.border(1.5.dp, BZ_RED) else Modifier.border(0.5.dp, Color(0xFFEEEEEE)))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(head, color = Color(0xFF777777), fontSize = 10.sp, lineHeight = 12.sp)
        if (sub.isNotEmpty()) Text(sub, color = Color(0xFFAAAAAA), fontSize = 9.sp, lineHeight = 11.sp)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(gan, color = WX_COLOR_DETAIL[ganWx] ?: Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(ganSs, color = Color(0xFFC62828), fontSize = 8.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(zhi, color = WX_COLOR_DETAIL[zhiWx] ?: Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(zhiSs, color = Color(0xFFC62828), fontSize = 8.sp)
        }
    }
}

/** 主页右上角的小开关（视觉，仿原版 胎命身/串宫） */
@Composable
private fun BzToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onToggle(!checked) }) {
        Text(label, color = BZ_RED, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Box(Modifier.size(width = 34.dp, height = 18.dp).clip(RoundedCornerShape(9.dp))
            .background(if (checked) BZ_RED else Color(0xFFCCCCCC)), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(50)).background(Color.White)
                .padding(start = if (checked) 18.dp else 2.dp))
        }
    }
}

@Composable private fun BzNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(BZ_CARD).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("‹ 返回", color = BZ_GOLD, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f)); Spacer(Modifier.width(52.dp))
    }
}

@Composable private fun BzField(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label：", color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(88.dp))
        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(BZ_CARD)
            .border(1.dp, BZ_LINE, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) { content() }
    }
}

@Composable private fun BzRadioLine(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label：", color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(88.dp))
        options.forEachIndexed { idx, name ->
            Row(Modifier.clickable { onSelect(idx) }.padding(end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(17.dp).clip(RoundedCornerShape(50))
                    .background(if (selected == idx) BZ_RED else Color.Transparent)
                    .then(if (selected == idx) Modifier else Modifier.border(1.dp, BZ_LINE, RoundedCornerShape(50))),
                    contentAlignment = Alignment.Center) {
                    if (selected == idx) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
                }
                Spacer(Modifier.width(5.dp))
                Text(name, color = if (selected == idx) BZ_GOLD else AppColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable private fun BzInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(70.dp))
        Text(value, color = Color(0xFF222222), fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable private fun BzSection(title: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(15.dp).background(BZ_RED))
        Spacer(Modifier.width(8.dp))
        Text(title, color = BZ_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun baziFaceText(c: BaziChart.Chart, q: String, createdAt: String) = buildString {
    appendLine("八 字 排 盘"); appendLine("命主：$q"); appendLine("时间：$createdAt")
    appendLine("四柱：${c.baziText}"); appendLine("农历：${c.lunar} · ${c.shengXiao}${c.gender}")
    appendLine("十神：${c.pillars.joinToString(" ") { it.shiShen }}")
    appendLine("纳音：${c.pillars.joinToString(" ") { it.naYin }}")
    appendLine("胎元${c.taiYuan} 命宫${c.mingGong} 命卦${c.mingGua}")
    appendLine("旺衰：${c.analysis.wangshuai} 用神：${c.analysis.yongshen.joinToString("、")}")
    appendLine("骨重：${c.chenggu.weight}")
}.trim()

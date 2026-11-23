package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.DaLiuRen
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 大六壬专用起卦页：仿原版 view_da6ren。
 * 起课时间 / 出生年份(带干支) / 性别 / 占事时辰 / 换将方式 / 贵神类型 / 贵神顺逆。
 * 详情页仿原版十二地支九宫盘 + 四课 + 三传。
 */
@Composable
fun LiuRenCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var birthYear by remember { mutableStateOf(LocalDate.now().year - 30) }
    var isMan by remember { mutableStateOf(true) }
    var zhanTime by remember { mutableStateOf("") }
    var yueJiangMethod by remember { mutableStateOf(1) }
    var guiMethod by remember { mutableStateOf(1) }
    var guiSunni by remember { mutableStateOf(1) }
    var question by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showYear by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<DaLiuRen.Result?>(null) }
    var castDt by remember { mutableStateOf(LocalDateTime.now()) }

    if (result == null) {
        JiDialogShell(title = "大 六 壬", onBack = onBack, onCast = {
            try {
                val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
                castDt = dt
                result = DaLiuRen.paipan(dt, isMan, birthYear, yueJiangMethod, guiMethod, guiSunni, zhanTime)
                try {
                    val id = db.insert(dt.format(dtFmt), "大六壬 · $question", "daliuren", "大六壬",
                        lrFaceText(result!!, question), "六壬课")
                    Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { }
            } catch (e: Exception) {
                Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }) {
            JiRow("起课时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(12.dp))
            JiRow("出生年份：") {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(JiForm.INPUT_BG)
                    .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp)).clickable { showYear = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp)) { Text("$birthYear 年 (${LRYearGanzhi(birthYear)}) ▾", color = JiForm.TEXT, fontSize = 13.sp) }
                Spacer(Modifier.width(10.dp))
                JiSeg(listOf("男", "女"), if (isMan) 0 else 1) { isMan = it == 0 }
            }
            Spacer(Modifier.height(14.dp))
            JiChipLine("占事时辰：", listOf("当前","子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"),
                if (zhanTime.isEmpty()) 0 else "子丑寅卯辰巳午未申酉戌亥".indexOf(zhanTime) + 1) { idx ->
                zhanTime = if (idx == 0) "" else "子丑寅卯辰巳午未申酉戌亥"[idx - 1].toString()
            }
            Spacer(Modifier.height(12.dp))
            JiChipLine("换将方式：", listOf("节气", "年月日时取余"), yueJiangMethod - 1) { yueJiangMethod = it + 1 }
            Spacer(Modifier.height(10.dp))
            JiChipLine("贵神类型：", listOf("卯酉区分", "白昼", "夜晚"), guiMethod - 1) { guiMethod = it + 1 }
            Spacer(Modifier.height(10.dp))
            JiChipLine("贵神顺逆：", listOf("自动", "男顺女逆"), guiSunni - 1) { guiSunni = it + 1 }
            Spacer(Modifier.height(12.dp))
            JiRow("事项：") { JiInput(question, { question = it }, "输入预测事项（选填）", Modifier.fillMaxWidth()) }
        }
        if (showDate) JiDateTimeDialog(
            initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
            onDismiss = { showDate = false },
            onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false }
        )
        if (showYear) BoardYearDialog(birthYear, { showYear = false }) { birthYear = it; showYear = false }
    } else {
        LiuRenDetailScreen(result!!, castDt, question, birthYear, isMan, yueJiangMethod, guiMethod, guiSunni, zhanTime, db, ctx, onBack) { result = it }
    }
}

@Composable
private fun LrInfoRow(label: String, value: String, head: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF555555), fontSize = 12.sp,
            modifier = Modifier.width(66.dp).padding(vertical = 6.dp))
        Text(value, color = if (head) BoardTheme.RED else QD_TEXT, fontSize = 13.sp,
            fontWeight = if (head) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f).padding(vertical = 6.dp))
    }
}

@Composable
internal fun LRRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(78.dp))
        content()
    }
}

@Composable
internal fun LRSeg(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row {
        options.forEachIndexed { i, s ->
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (i == selected) BoardTheme.RED else BoardTheme.CARD)
                .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(s, color = Color.White, fontSize = 13.sp)
            }
            if (i < options.size - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun LRChipRow(label: String, items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { i, s ->
                Box(Modifier.clip(RoundedCornerShape(8.dp)).weight(1f)
                    .background(if (i == selected) Color(0x2AD9A441) else BoardTheme.CARD)
                    .then(if (i == selected) Modifier.border(1.dp, BoardTheme.GOLD, RoundedCornerShape(8.dp)) else Modifier)
                    .clickable { onSelect(i) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(s, color = if (i == selected) BoardTheme.GOLD else AppColors.Paper, fontSize = 12.sp,
                        maxLines = 1)
                }
            }
        }
    }
}

private fun LRYearGanzhi(year: Int): String {
    val g = "甲乙丙丁戊己庚辛壬癸"
    val z = "子丑寅卯辰巳午未申酉戌亥"
    val i = Math.floorMod(year - 4, 60)
    return "" + g[i % 10] + z[i % 12]
}

@Composable
private fun BoardYearDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var y by remember { mutableStateOf(initial) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, BoardTheme.LINE, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择出生年份", color = BoardTheme.GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            BoardStepper("年份", y, 1930, LocalDate.now().year) { y = it }
            Text("${LRYearGanzhi(y)} 年", color = AppColors.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.CARD)
                    .clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.RED)
                    .clickable { onConfirm(y) }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==================== 详情页：仿原版十二地支盘 + 四课三传 ====================

@Composable
private fun LiuRenDetailScreen(
    r: DaLiuRen.Result, dt: LocalDateTime, question: String,
    birthYear: Int, isMan: Boolean, yueJiangMethod: Int, guiMethod: Int, guiSunni: Int, zhanTime: String,
    db: FortuneDb, ctx: android.content.Context, onBack: () -> Unit,
    onResult: (DaLiuRen.Result) -> Unit
) {
    fun repaipan(nd: LocalDateTime) = DaLiuRen.paipan(nd, isMan, birthYear, yueJiangMethod, guiMethod, guiSunni, zhanTime)
    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
            .padding(bottom = 40.dp)) {
            Spacer(Modifier.height(24.dp))
            BoardNavBar("大 六 壬 · 课", onBack)
            Spacer(Modifier.height(10.dp))

            // 信息表（仿原版白底表格：事项/时间/起课时辰/节气/四柱/月将性别年命行年空亡）
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White)) {
                LrInfoRow("事项", if (question.isNotBlank()) question else "输入预测事项", head = true)
                LrInfoRow("时间", r.date)
                LrInfoRow("起课时辰", r.zhanbuTime + "时占")
                LrInfoRow("节气", r.jieqi)
                // 四柱表头
                Row(Modifier.fillMaxWidth()) {
                    Text("", color = Color(0xFF4A6FA5), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(66.dp).padding(vertical = 6.dp))
                    listOf("年柱","月柱","日柱","时柱").forEach {
                        Text(it, color = Color(0xFF4A6FA5), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
                    }
                }
                // 四柱值（竖排两字，日时红色，仿原版）
                Row(Modifier.fillMaxWidth()) {
                    Text("四柱", color = Color(0xFF555555), fontSize = 12.sp,
                        modifier = Modifier.width(66.dp).padding(vertical = 6.dp))
                    r.siZhu.forEachIndexed { i, gz ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val c = if (i >= 2) BoardTheme.RED else QD_TEXT
                            Text(gz[0].toString(), color = c, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(gz[1].toString(), color = c, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        }
                    }
                }
                // 月将/性别/年命/行年/空亡 表头
                Row(Modifier.fillMaxWidth()) {
                    listOf("月将","性别","年命","行年","空亡").forEach {
                        Text(it, color = Color(0xFF4A6FA5), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
                    }
                }
                // 对应值
                Row(Modifier.fillMaxWidth()) {
                    listOf(r.yueJiang, if (r.isMan) "男" else "女", r.yearGanzhi, r.xingYear, r.kongwang).forEach {
                        Text(it, color = QD_TEXT, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 十二地支天地盘（环形九宫）
            BoardSectionTitle("天 地 盘")
            LiuRenPlate(r)
            Spacer(Modifier.height(12.dp))

            // 四课三传
            BoardSectionTitle("四 课 三 传")
            LiuRenKeChuan(r)
            Spacer(Modifier.height(12.dp))

            // 翻课
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.clip(RoundedCornerShape(10.dp)).weight(1f).background(BoardTheme.CARD)
                    .border(1.dp, BoardTheme.LINE, RoundedCornerShape(10.dp))
                    .clickable { onResult(repaipan(dt.minusHours(2))) }
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("← 上一时辰", color = BoardTheme.GOLD, fontSize = 13.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(10.dp)).weight(1f).background(BoardTheme.CARD)
                    .border(1.dp, BoardTheme.LINE, RoundedCornerShape(10.dp))
                    .clickable { onResult(repaipan(dt.plusHours(2))) }
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("下一时辰 →", color = BoardTheme.GOLD, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            com.fortune.stall.ui.components.ResultActionBar(faceText = lrFaceText(r, question), dark = true)
        }
    }
}

/** 仿原版天地盘："回"字形三层同心方框 + 中心留白。外圈天干、中圈天将、内圈天盘地支。
 *  数据映射已按原版逐格校验：地盘支顺时针自左上起 = 巳午未申/酉戌/亥子丑寅/卯辰。
 *  每层同一方位共用同一个地盘支 key，取 r.tianGan / r.guiShen / r.tianPan。 */
@Composable
private fun LiuRenPlate(r: DaLiuRen.Result) {
    // 12 方位（顺时针，自顶行左起）：顶 巳午未申 / 右 酉戌 / 底 亥子丑寅(右→左) / 左 卯辰(下→上)
    // 归一化到 [0,1] 的四边形网格坐标（4×4 顶点环，中心 2×2 留白）
    // 每个方位按其在环上的位置给出 (x,y)
    // 顶/底行的字收缩在中间（x 0.26~0.74），左右列留在两侧（x 0.08/0.92），
    // 这样左右列视觉上只有 2 个字，不会与顶底行的字连成一竖列（贴合原版）。
    val coords = listOf(
        0.26f to 0.08f, 0.42f to 0.08f, 0.58f to 0.08f, 0.74f to 0.08f,   // 顶4
        0.92f to 0.36f, 0.92f to 0.64f,                                     // 右2
        0.74f to 0.92f, 0.58f to 0.92f, 0.42f to 0.92f, 0.26f to 0.92f,   // 底4(右→左)
        0.08f to 0.64f, 0.08f to 0.36f                                      // 左2(下→上)
    )
    val keys = listOf("巳","午","未","申","酉","戌","亥","子","丑","寅","卯","辰")
    val ganVals  = keys.map { r.tianGan[it] ?: "" }
    val shenVals = keys.map { r.guiShen[it] ?: "" }
    val zhiVals  = keys.map { r.tianPan[it] ?: "" }
    // 日干（四柱日柱天干），原版把日干标红
    val dayGan = r.siZhu.getOrNull(2)?.firstOrNull()?.toString() ?: ""

    Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp)
        .clip(RoundedCornerShape(8.dp)).border(1.dp, BoardTheme.LINE, RoundedCornerShape(8.dp))
        .background(Color(0xFFEDEDED)).padding(8.dp), contentAlignment = Alignment.Center) {
        LiuRenConcentricPlate(coords, ganVals, shenVals, zhiVals, dayGan)
    }
}

/** 仿原版天地盘：用背景色块分层（浅灰外圈 → 中灰 → 浅灰 → 白色中心），无边框线。
 *  外圈天干、中圈天将、内圈天盘地支；贵人与日干标红。 */
@Composable
private fun LiuRenConcentricPlate(
    coords: List<Pair<Float, Float>>,
    gan: List<String>, shen: List<String>, zhi: List<String>,
    dayGan: String
) {
    val mColor = Color(0xFF1A1A1A)
    val emptyColor = Color(0xFFBDBDBD)
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val side = maxWidth
        data class Ring(val pad: Float, val vals: List<String>, val isGan: Boolean, val isShen: Boolean)
        val rings = listOf(
            Ring(0.00f, gan, true, false),      // 外：天干
            Ring(0.14f, shen, false, true),     // 中：天将
            Ring(0.27f, zhi, false, false)      // 内：天盘地支
        )
        // 背景色块（从外到内依次叠加）：浅灰 → 中灰 → 浅灰白 → 白
        val layers = listOf(
            0.00f to Color(0xFFEDEDED),
            0.16f to Color(0xFFD8D8D8),
            0.30f to Color(0xFFF2F2F2),
            0.39f to Color(0xFFFFFFFF)
        )
        layers.forEach { (p, c) ->
            Box(Modifier.size(side * (1 - 2 * p)).offset(x = side * p, y = side * p).background(c))
        }
        // 文字：把 0..1 的网格坐标映射到每层色块
        rings.forEach { ring ->
            val inset = side * ring.pad
            val span = side * (1 - 2 * ring.pad)
            ring.vals.forEachIndexed { i, v ->
                if (v.isBlank()) return@forEachIndexed
                val (gx, gy) = coords[i]
                val x = inset + span * gx
                val y = inset + span * gy
                val color = when {
                    ring.isGan && v == dayGan -> BoardTheme.RED   // 日干标红
                    ring.isShen && v == "贵" -> BoardTheme.RED    // 贵人标红
                    ring.isGan && v == "○" -> emptyColor
                    else -> mColor
                }
                Text(v, color = color, fontSize = (side.value * 0.056f).sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = x - 13.dp, y = y - 13.dp).width(26.dp),
                    textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

private fun lrFaceText(r: DaLiuRen.Result, q: String): String = buildString {
    append("大六壬排盘\n")
    if (q.isNotBlank()) append("事项：$q\n")
    append("${r.date} · ${r.zhanbuTime}时占\n")
    append("四柱：${r.siZhu.joinToString(" ")}\n")
    append("月将：${r.yueJiang} 空亡：${r.kongwang}\n")
    append("年命：${r.yearGanzhi} ${if (r.isMan) "男" else "女"}\n")
    append("四课：${r.fourKe.joinToString(" / ") { it.joinToString("") }}\n")
    append("三传：${r.threeChuan.joinToString(" / ") { it.joinToString("") }}\n")
}

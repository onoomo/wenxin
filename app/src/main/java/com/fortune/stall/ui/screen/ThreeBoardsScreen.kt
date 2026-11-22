package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.DaLiuRen
import com.fortune.stall.engine.jishiyu.Qimen
import com.fortune.stall.engine.jishiyu.Taiyi
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 奇门穿壬 / 太乙三式 专用起卦页。
 * 奇门穿壬 = 奇门九宫 + 大六壬（贵神/月将/年煞/命宫）同盘；
 * 太乙三式 = 上述二式 + 太乙积数/主客算（三盘合一）。
 */
@Composable
fun CompositeCastScreen(db: FortuneDb, kind: String, onBack: () -> Unit) {
    val threeBoard = kind == "taiyi3shi"
    val title = if (threeBoard) "太 乙 三 式" else "奇 门 穿 壬"
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var yearMingText by remember { mutableStateOf(LocalDateTime.now().year.toString()) }
    var isMan by remember { mutableStateOf(true) }
    var guiMethod by remember { mutableStateOf(1) }
    var yongGan by remember { mutableStateOf("甲") }
    var yongZhi by remember { mutableStateOf("子") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showYear by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var prepared by remember { mutableStateOf(false) }

    val GAN = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val ZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

    if (prepared) {
        val dt = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() }
        CompositeDetailScreen(db, threeBoard, dt, isMan, (yearMingText.toIntOrNull() ?: dt.year),
            guiMethod, yongGan + yongZhi, question, onBack)
        return
    }

    JiDialogShell(title = title, onBack = onBack, onCast = { prepared = true }) {
        JiRow("起课时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(12.dp))
        JiRow("出生年份：") {
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(JiForm.INPUT_BG)
                .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp)).clickable { showYear = true }
                .padding(horizontal = 12.dp, vertical = 9.dp)) { Text("$yearMingText ▾", color = JiForm.TEXT, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(12.dp))
        JiRow("性别：") { JiSeg(listOf("男", "女"), if (isMan) 0 else 1) { isMan = it == 0 } }
        Spacer(Modifier.height(14.dp))
        JiChipLine("用神天干：", GAN.toList(), GAN.indexOf(yongGan).coerceAtLeast(0)) { yongGan = GAN[it] }
        Spacer(Modifier.height(10.dp))
        JiChipLine("用神地支：", ZHI.toList(), ZHI.indexOf(yongZhi).coerceAtLeast(0)) { yongZhi = ZHI[it] }
        Spacer(Modifier.height(10.dp))
        JiChipLine("贵神类型：", listOf("卯酉区分", "白昼", "夜晚"), guiMethod - 1) { guiMethod = it + 1 }
    }
    if (showDate) BoardDateDialog(initial = try { LocalDate.parse(dtText.substring(0, 10)) } catch (e: Exception) { LocalDate.now() },
        onDismiss = { showDate = false },
        onConfirm = { d -> val t = dtText.substring(11).ifEmpty { "00:00" }; dtText = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"; showDate = false; showTime = true })
    if (showTime) BoardTimeDialog(initial = try { LocalTime.parse(dtText.substring(11)) } catch (e: Exception) { LocalTime.now() },
        onDismiss = { showTime = false },
        onConfirm = { t -> dtText = "${dtText.substring(0, 10)} ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"; showTime = false })
    if (showYear) BoardIntDialog("出生年份", (yearMingText.toIntOrNull() ?: 1990), 1900, 2199, { showYear = false }) { yearMingText = it.toString(); showYear = false }
}

@Composable
private fun SegBtn(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(10.dp))
        .background(if (active) BoardTheme.RED else BoardTheme.CARD)
        .clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (active) Color.White else AppColors.Muted, fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ChipRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    var row1 = items.take(6); var row2 = items.drop(6)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row1.forEach { Chip(it, it == selected, Modifier.weight(1f), onSelect) }
        }
        if (row2.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row2.forEach { Chip(it, it == selected, Modifier.weight(1f), onSelect) }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, modifier: Modifier, onClick: (String) -> Unit) {
    Box(modifier.clip(RoundedCornerShape(8.dp))
        .background(if (active) BoardTheme.GOLD else BoardTheme.CARD)
        .clickable { onClick(label) }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (active) Color(0xFF1E1B14) else Color.White,
            fontSize = 14.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Serif)
    }
}

@Composable
private fun BoardPickRow(label: String, value: String, onDate: () -> Unit, onTime: () -> Unit) {
    Text(label, color = AppColors.Muted, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(BoardTheme.CARD)
            .clickable(onClick = onDate).padding(16.dp), contentAlignment = Alignment.Center) {
            Text(value.split("  ")[0], color = Color.White, fontSize = 15.sp)
        }
        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(BoardTheme.CARD)
            .clickable(onClick = onTime).padding(16.dp), contentAlignment = Alignment.Center) {
            Text(value.split("  ").getOrNull(1) ?: "", color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
internal fun BoardIntDialog(title: String, initial: Int, min: Int, max: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, BoardTheme.LINE, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = BoardTheme.GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            BoardStepper("数值", v, min, max) { v = it }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.CARD)
                    .clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.RED)
                    .clickable { onConfirm(v) }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun CompositeDetailScreen(
    db: FortuneDb, threeBoard: Boolean, dt: LocalDateTime, isMan: Boolean, yearMing: Int,
    guiMethod: Int, yongShen: String, question: String, onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val q = remember(dt) { Qimen.paipan(dt, false) }
    val lr = remember(dt) { DaLiuRen.paipan(dt, isMan, yearMing, 1, guiMethod, 1, "", yongShen) }
    val ty = remember(dt) { if (threeBoard) Taiyi.paipan(dt, isMan, 0, yongShen) else null }

    LaunchedEffect(Unit) {
        val method = if (threeBoard) "taiyi3shi" else "chuanren"
        val name = if (threeBoard) "太乙三式" else "奇门穿壬"
        val face = "${q.yangDun}${"遁"}${q.jushu}局 · 用神$yongShen"
        db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), question, method, name, face, q.jieqi)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(QD_BG)) {
        BoardNavBar(if (threeBoard) "太乙三式" else "奇门穿壬", onBack)

        // 信息表（紧凑）
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 3.dp)) {
            QdRow("事项", question.ifEmpty { "—" })
            QdRow("时间", q.date)
            QdRow("节气", q.jieqi)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("阴阳", color = AppColors.Muted, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                Text("${if (q.yangDun) "阳遁" else "阴遁"}${q.jushu}局", color = BoardTheme.RED,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (h in listOf("值符", "值使", "旬首", "马星", "空亡")) {
                    Text(h, color = AppColors.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                for (v in listOf(q.zhifu, q.zhishi, q.xunhead, q.maxing, q.kongwang)) {
                    Text(v, color = BoardTheme.MING, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (h in listOf("月将", "年命", "用神", "贵人", "性别")) {
                    Text(h, color = AppColors.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                val guiren = lr.guiShen.values.firstOrNull { it.contains("贵") } ?: lr.yueJiang
                for (v in listOf(lr.yueJiang, lr.yearGanzhi, yongShen, guiren, if (isMan) "男" else "女")) {
                    Text(v, color = BoardTheme.GUI, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("四柱", color = AppColors.Muted, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                for (gz in q.siZhu) Text(gz, color = QD_TEXT, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))

        if (threeBoard && ty != null) {
            // ===== 太乙积数·三式之最（仿原版：米黄横条标题 + 深红数值 + 详细列表）=====
            // 米黄横条标题：左侧橙色竖条 + 琥珀色加粗文字
            Row(Modifier.fillMaxWidth().background(Color(0xFFF6EEDC))
                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(15.dp).background(Color(0xFFE08A34)))
                Spacer(Modifier.width(8.dp))
                Text("太乙积数 · 三式之最", color = Color(0xFFC0812A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp)) {
                // 5列汇总表头
                Row(Modifier.fillMaxWidth()) {
                    for (h in listOf("积数", "主算", "客算", "天马", "太乙宫位")) {
                        Text(h, color = Color(0xFF8A8A8A), fontSize = 12.sp,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                // 5列数值：深红加粗
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    for (v in listOf(ty.jishu.toString(), ty.zhusuan.toString(), ty.keshuan.toString(), ty.tianma, "${ty.tygongwei}${ty.guan}")) {
                        Text(v, color = Color(0xFF9E1A1A), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(3.dp))
                // 详细列表：灰蓝标签 + 深色值
                for ((k, v) in ty.lines) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(k, color = Color(0xFF4A5A8A), fontSize = 13.sp, modifier = Modifier.width(44.dp))
                        Text(v, color = QD_TEXT, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // 九宫盘：太乙三式用完整回字形四层盘；奇门穿壬用奇门内+六壬外围
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (threeBoard && ty != null) TaiyiFullBoard(q, lr, ty) else Q6Board(q, lr, showMa = true)
        }
        Spacer(Modifier.height(10.dp))

        // 大地盘 4 列小表（仿原版）
        val earth = listOf(
            listOf("亥", "卯", "酉"), listOf("午", "戌", "寅"),
            listOf("丑", "巳", "申"), listOf("庚", "子", "辰")
        )
        Column(Modifier.align(Alignment.CenterHorizontally)
            .border(1.dp, Color(0xFFAAAAAA), RoundedCornerShape(2.dp)).padding(6.dp)) {
            for (row in earth) {
                Row {
                    for (cell in row) Text(cell, color = Color(0xFF444444), fontSize = 14.sp,
                        fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                        modifier = Modifier.width(34.dp).padding(vertical = 2.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Text("奇门符号颜色：入墓、击刑、门迫、刑+墓",
            color = Color(0xFF666666), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("六壬符号颜色：贵神、年煞、命宫、建星",
            color = Color(0xFF666666), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (threeBoard) {
            Text("太乙地(人)盘：", color = Color(0xFF666666), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("太乙天(神)盘：", color = Color(0xFF666666), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CompositeGrid(q: Qimen.Result, lr: DaLiuRen.Result) {
    val visual = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))
    val ring = mapOf(
        "巽" to "辰", "离" to "午", "坤" to "申", "震" to "卯", "兑" to "酉",
        "艮" to "寅", "坎" to "子", "乾" to "戌"
    )
    val cellH = 72.dp
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF888888), RoundedCornerShape(6.dp))
        .background(Color.White)) {
        visual.forEachIndexed { ri, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { ci, g ->
                    val cell = q.pan[g] ?: Qimen.Cell()
                    val z = ring[g]
                    Column(Modifier.weight(1f).height(cellH).padding(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        // 外围六壬信息（贵神/月将/天干）
                        Text(z ?: "", color = AppColors.Muted, fontSize = 10.sp)
                        if (z != null) {
                            Text(listOfNotNull(lr.tianGan[z], lr.guiShen[z], lr.tianPan[z]).joinToString(" "),
                                color = AppColors.Muted, fontSize = 9.sp, maxLines = 1)
                            Text(listOfNotNull(lr.nianSha[z], lr.gong12[z]).joinToString(" "),
                                fontSize = 9.sp, maxLines = 1)
                        }
                        Spacer(Modifier.height(2.dp))
                        // 奇门内圈
                        if (g == "中") {
                            Text("中", color = Color(0xFFAAAAAA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(cell.bashen, color = QD_TEXT, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(cell.tianpan + " " + shortStar(cell.jiuxing), color = BoardTheme.YEAR, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(cell.dipan + " " + shortMen(cell.men), color = BoardTheme.MONTH, fontSize = 12.sp)
                        }
                    }
                    if (ci < 2) Box(Modifier.width(1.dp).height(cellH).background(Color(0xFFCCCCCC)))
                }
            }
            if (ri < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCCCCCC)))
        }
    }
}

private fun shortStar(s: String) = s  // 九星简称（蓬/芮/冲…）本身即原版显示
private fun shortMen(s: String) = s  // 八门简称（休/生/伤…）本身即原版显示

@Composable
internal fun LiuRenKeChuan(lr: DaLiuRen.Result) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        .clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp)) {
        Text("四课", color = BoardTheme.GOLD, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        for (ke in lr.fourKe) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (c in ke) Text(c, color = QD_TEXT, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("三传", color = BoardTheme.GOLD, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        for (ch in lr.threeChuan) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (c in ch) Text(c, color = BoardTheme.NIAN, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
    }
}

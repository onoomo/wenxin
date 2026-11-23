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
        val face = threeBoardFaceText(threeBoard, q, lr, ty, yongShen, question)
        db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), question, method, name, face, q.jieqi)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(QD_BG)) {
        BoardNavBar(if (threeBoard) "太乙三式" else "奇门穿壬", onBack)

        // ===== 信息区：仿原版（灰蓝标签 + 黑值 + 竖排四柱）=====
        val infoLabel = Color(0xFF4A5A8A)
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 4.dp)) {
            QdInfo("事项", question.ifEmpty { "输入预测事项" }, infoLabel, question.isEmpty())
            QdInfo("时间", q.date, infoLabel)
            // 节气行：右侧带 阴遁/阳遁局
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("节气", color = infoLabel, fontSize = 15.sp, modifier = Modifier.width(52.dp))
                Text(q.jieqi, color = QD_TEXT, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("${if (q.yangDun) "阳遁" else "阴遁"}${q.jushu}局", color = QD_TEXT, fontSize = 15.sp)
            }
            // 值符组
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                for (h in listOf("值符", "值使", "旬首", "马星", "空亡")) {
                    Text(h, color = infoLabel, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                for (v in listOf(q.zhifu, q.zhishi, q.xunhead, q.maxing, q.kongwang)) {
                    Text(v, color = QD_TEXT, fontSize = 15.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            // 月将组
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                for (h in listOf("月将", "年命", "用神", "贵人", "性别")) {
                    Text(h, color = infoLabel, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                val guiren = lr.guiShen.values.firstOrNull { it.contains("贵") } ?: lr.yueJiang
                for (v in listOf(lr.yueJiang, lr.yearGanzhi, yongShen, guiren, if (isMan) "男" else "女")) {
                    Text(v, color = QD_TEXT, fontSize = 15.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            // 四柱：表头 + 竖排大字（每柱天干/地支各占一行）
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Spacer(Modifier.width(52.dp))
                for (h in listOf("年柱", "月柱", "日柱", "时柱")) {
                    Text(h, color = infoLabel, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                Text("四柱", color = infoLabel, fontSize = 15.sp, modifier = Modifier.width(52.dp))
                for (gz in q.siZhu) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(gz.getOrNull(0)?.toString() ?: "", color = QD_TEXT, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Text(gz.getOrNull(1)?.toString() ?: "", color = QD_TEXT, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
        // 太乙三式：积数/主算数/客算数/天马/太乙宫位 5列（原版灰底蓝头 + 黑值）
        if (threeBoard && ty != null) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().background(Color(0xFFE9E9E9)).padding(vertical = 7.dp)) {
                    for (h in listOf("积数", "主算数", "客算数", "天马", "太乙宫位")) {
                        Text(h, color = Color(0xFF2E4487), fontSize = 13.sp,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                Row(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 7.dp)) {
                    for (v in listOf(ty.jishu.toString(), ty.zhusuan.toString(), ty.keshuan.toString(), ty.tianma, "${ty.tygongwei}${ty.guan}")) {
                        Text(v, color = Color(0xFF222222), fontSize = 14.sp,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // 九宫盘：太乙三式用完整回字形四层盘；奇门穿壬用奇门内+六壬外围
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (threeBoard && ty != null) TaiyiFullBoard(q, lr, ty) else Q6Board(q, lr, showMa = true)
        }
        Spacer(Modifier.height(10.dp))

        // ===== 底部两个小表：大六壬四课 + 三传（仿原版 view_qimen_3shi.html）=====
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            // 左表：四课（4列3行；天将 / 上神(粗) / 下神(粗)，列倒序 4→1）
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(Modifier.border(1.dp, Color(0xFFAAAAAA), RoundedCornerShape(2.dp)).padding(6.dp)) {
                    for (r in 0..2) {
                        Row {
                            for (k in 3 downTo 0) {
                                val cell = lr.fourKe.getOrNull(k)?.getOrNull(2 - r) ?: ""
                                Text(cell, color = Color(0xFF222222), fontSize = 15.sp,
                                    fontWeight = if (r == 0) FontWeight.Normal else FontWeight.Bold,
                                    fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                                    modifier = Modifier.width(30.dp).padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            // 右表：三传（3行4列；六亲 / 天干 / 地支(粗) / 天将(灰)）
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(Modifier.border(1.dp, Color(0xFFAAAAAA), RoundedCornerShape(2.dp)).padding(6.dp)) {
                    for (ch in lr.threeChuan.take(3)) {
                        Row {
                            Text(ch.getOrNull(3) ?: "", color = Color(0xFF222222), fontSize = 15.sp,
                                fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                                modifier = Modifier.width(30.dp).padding(vertical = 2.dp))
                            Text(ch.getOrNull(1) ?: "", color = Color(0xFF222222), fontSize = 15.sp,
                                fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                                modifier = Modifier.width(30.dp).padding(vertical = 2.dp))
                            Text(ch.getOrNull(0) ?: "", color = Color(0xFF222222), fontSize = 15.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                                modifier = Modifier.width(30.dp).padding(vertical = 2.dp))
                            Text(ch.getOrNull(2) ?: "", color = Color(0xFF888888), fontSize = 15.sp,
                                fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                                modifier = Modifier.width(30.dp).padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ===== 图例：逐字着色 + 地盘/天盘色块（仿原版 CSS）=====
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Text("奇门符号颜色：", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("入墓", color = Color(0xFFD0AF0F), fontSize = 13.sp)
                    Text("、", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("击刑", color = Color(0xFFBA55D3), fontSize = 13.sp)
                    Text("、", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("门迫", color = Color(0xFFFF0000), fontSize = 13.sp)
                    Text("、", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("刑+墓", color = Color(0xFF1E90FF), fontSize = 13.sp)
                }
                Row {
                    Text("六壬符号颜色：", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("贵神", color = Color(0xFFFF7700), fontSize = 13.sp)
                    Text("、", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("年煞", color = Color(0xFF801300), fontSize = 13.sp)
                    Text("、", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("命宫", color = Color(0xFF0000FF), fontSize = 13.sp)
                    Text("、", color = Color(0xFF555555), fontSize = 13.sp)
                    Text("建星", color = Color(0xFF008000), fontSize = 13.sp)
                }
                if (threeBoard) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("太乙地(人)盘：", color = Color(0xFF555555), fontSize = 13.sp)
                        Box(Modifier.width(25.dp).height(15.dp).background(Color(0xFFE3E3E3))
                            .border(1.dp, Color(0xFFAAAAAA), RoundedCornerShape(3.dp)))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("太乙天(神)盘：", color = Color(0xFF555555), fontSize = 13.sp)
                        Box(Modifier.width(25.dp).height(15.dp).background(Color(0xFFF7EECD))
                            .border(1.dp, Color(0xFF888888), RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(
            faceText = threeBoardFaceText(threeBoard, q, lr, ty, yongShen, question)
        )
    }
}

/** 完整三式盘面文本：供存档/卦账复制/问AI 统一使用 */
private fun threeBoardFaceText(
    threeBoard: Boolean, q: com.fortune.stall.engine.jishiyu.Qimen.Result,
    lr: com.fortune.stall.engine.jishiyu.DaLiuRen.Result,
    ty: com.fortune.stall.engine.jishiyu.Taiyi.Result?,
    yongShen: String, question: String
) = buildString {
    val title = if (threeBoard) "太乙三式" else "奇门穿壬"
    appendLine("【$title · 排盘】")
    if (question.isNotBlank()) appendLine("事项：$question")
    appendLine("时间：${q.date}　节气：${q.jieqi}")
    appendLine("四柱：${q.siZhu.joinToString(" ")}")
    appendLine("${if (q.yangDun) "阳遁" else "阴遁"}${q.jushu}局 · 用神$yongShen")
    appendLine("值符${q.zhifu}(${q.zhifuGong}) 值使${q.zhishi}(${q.zhishiGong}) 旬首${q.xunhead} 空亡${q.kongwang} 马星${q.maxing}")
    appendLine("月将${lr.yueJiang} 占时${lr.zhanbuTime} 年命${lr.yearGanzhi} 行年${lr.xingYear}")
    appendLine()
    appendLine("── 奇门九宫 ──")
    for (g in com.fortune.stall.engine.jishiyu.QimenData._8GUA_S) {
        val c = q.pan[g] ?: continue
        appendLine("$g：天盘${c.tianpan} 九星${c.jiuxing} 八门${c.men} 八神${c.bashen} 地盘${c.dipan} 隐干${c.yingan}")
    }
    appendLine()
    appendLine("── 大六壬四课三传 ──")
    lr.fourKe.forEachIndexed { i, k -> appendLine("第${i + 1}课：${k.joinToString(" ")}") }
    appendLine("三传：${lr.threeChuan.map { it.joinToString(" ") }.joinToString(" → ")}")
    if (ty != null) {
        appendLine()
        appendLine("── 太乙 ──")
        appendLine("积数${ty.jishu} 主算${ty.zhusuan} 客算${ty.keshuan} 天马${ty.tianma} 太乙宫位${ty.tygongwei}${ty.guan}")
        ty.lines.forEach { (k, v) -> appendLine("$k：$v") }
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

/** 仿原版信息行：灰蓝标签 + 值（占位值用灰色） */
@Composable
private fun QdInfo(label: String, value: String, labelColor: Color, placeholder: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = labelColor, fontSize = 15.sp, modifier = Modifier.width(52.dp))
        Text(value, color = if (placeholder) Color(0xFF999999) else QD_TEXT, fontSize = 15.sp)
    }
}

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

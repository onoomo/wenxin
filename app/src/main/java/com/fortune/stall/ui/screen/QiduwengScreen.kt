package com.fortune.stall.ui.screen

import android.widget.Toast
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
import com.fortune.stall.engine.jishiyu.Qiduweng
import com.fortune.stall.engine.jishiyu.QiduwengData
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 戚都翁专用起卦页：日期时间 → 体用五星盘 + 占断口诀 */
@Composable
fun QiduwengCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var showDate by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Qiduweng.Result?>(null) }
    var question by remember { mutableStateOf("") }

    if (result != null) {
        QiduwengDetailScreen(result!!, question, onBack)
        return
    }

    JiDialogShell(title = "戚 都 翁 排 盘", onBack = onBack, onCast = {
        try {
            val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
            val rr = Qiduweng.paipan(dt)
            val id = db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                question, "qiduweng", "戚都翁", qdFaceText(rr, question), rr.ju)
            Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
            result = rr
        } catch (e: Exception) {
            Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }) {
        JiRow("起课时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(12.dp))
        JiRow("预测事项：") { JiInput(question, { question = it.take(30) }, "输入预测事项（选填）", Modifier.fillMaxWidth()) }
    }
    if (showDate) JiDateTimeDialog(
        initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
        onDismiss = { showDate = false },
        onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false }
    )
}

@Composable
internal fun BoardNavBar(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(BoardTheme.RED).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(26.dp))
        }
    }
}

@Composable
private fun QiduwengDetailScreen(r: Qiduweng.Result, question: String, onBack: () -> Unit) {
    var info by remember { mutableStateOf<String?>(null) }
    var caseIdx by remember { mutableStateOf(0) }
    val caseKeys = QiduwengData.CASES.keys.toList()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F5F5))) {
        BoardNavBar("戚都翁", onBack)

        // 信息表：事项/起局/日期/节气 —— 灰蓝标签 + 内容
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            QdInfoLine("事项", question.ifEmpty { "输入预测事项" }, valueGray = question.isEmpty())
            QdInfoLine("起局", r.ju)
            QdInfoLine("日期", r.date)
            QdInfoLine("节气", r.jieqi)
            // 四柱表头 + 竖排干支
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Spacer(Modifier.width(52.dp))
                for (h in listOf("年柱", "月柱", "日柱", "时柱"))
                    Text(h, color = QD_LABEL, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("四柱", color = QD_LABEL, fontSize = 15.sp, modifier = Modifier.width(52.dp))
                for (gz in r.siZhu) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(gz.getOrNull(0)?.toString() ?: "", color = qdGanColor(gz.getOrNull(0)?.toString() ?: ""),
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Text(gz.getOrNull(1)?.toString() ?: "", color = qdGanColor(gz.getOrNull(1)?.toString() ?: ""),
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Spacer(Modifier.width(52.dp))
                for (k in r.xunKong) Text(k, color = QD_TEXT, fontSize = 15.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(14.dp))

        // 体用大卡（居中，白底圆角阴影）
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(16.dp)).background(Color.White)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 用：小字别名 + 大字五行 + 状态
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("用", color = Color(0xFF888888), fontSize = 15.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
                Spacer(Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(r.hourStarAlias, color = qdWxColor(r.yongXing), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(r.yongXing, color = qdWxColor(r.yongXing), fontSize = 44.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif)
                }
                Spacer(Modifier.width(6.dp))
                Text(r.yongState, color = Color(0xFF999999), fontSize = 15.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Start)
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(1.dp).background(Color(0xFFEEEEEE)))
            Spacer(Modifier.height(14.dp))
            // 体：小字体 + 大字地支 + 状态
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("体", color = Color(0xFF888888), fontSize = 15.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
                Spacer(Modifier.width(6.dp))
                Text(r.tiZhi, color = qdWxColor(r.tiWx), fontSize = 44.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif)
                Spacer(Modifier.width(6.dp))
                Text(r.tiState, color = Color(0xFF999999), fontSize = 15.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Start)
            }
            Spacer(Modifier.height(18.dp))
            Text("月令${r.yueLingZhi}${r.yueLingWx}月　${r.relation}", color = Color(0xFF999999), fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))

        // 占断口诀卡
        QdCaseCard(caseKeys, caseIdx, { caseIdx = it })
        Spacer(Modifier.height(16.dp))
        com.fortune.stall.ui.components.ResultActionBar(faceText = qdFaceText(r, question))
        Spacer(Modifier.height(24.dp))
    }

    info?.let { Dialog(onDismissRequest = { info = null }) {
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, BoardTheme.LINE, RoundedCornerShape(14.dp)).padding(18.dp)
            .verticalScroll(rememberScrollState())) {
            Text("五行类象", color = BoardTheme.GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(it, color = AppColors.Paper, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(20.dp))
                .background(BoardTheme.RED).clickable { info = null }.padding(horizontal = 28.dp, vertical = 9.dp)) {
                Text("知道了", color = Color.White, fontSize = 14.sp)
            }
        }
    } }
}

/** 完整戚都翁盘面文本：供存档/卦账复制/问AI 统一使用 */
private fun qdFaceText(r: Qiduweng.Result, question: String) = buildString {
    appendLine("【戚都翁 · 小六壬排盘】")
    if (question.isNotBlank()) appendLine("事项：$question")
    appendLine("起局：${r.ju}")
    appendLine("日期：${r.date}　节气：${r.jieqi}")
    appendLine("四柱：${r.siZhu.joinToString(" ")}　旬空：${r.xunKong.joinToString(" ")}")
    appendLine("农历：${if (r.isLeap) "闰" else ""}${r.lunarMonth}月${r.lunarDay}日")
    appendLine()
    appendLine("月令：${r.yueLingZhi}(${r.yueLingWx})　时辰：${r.hourZhi}(${r.hourStar}/${r.hourStarAlias})")
    appendLine("体：${r.tiZhi}(${r.tiWx}) ${r.tiState}　用：${r.yongXing} ${r.yongState}")
    appendLine("体用关系：${r.relation} —— ${r.relationDesc}")
    appendLine("结论：${r.summary}")
    appendLine()
    appendLine("── 地盘十二宫星曜 ──")
    r.plate.forEach { p ->
        val marks = buildString {
            if (p.isMonth) append("月"); if (p.isDay) append("日")
            if (p.isHour) append("时"); if (p.isTi) append("体")
        }
        appendLine("${p.zhi}(${p.wuxing}) ${p.starName}${if (p.starAlias.isNotBlank() && p.starAlias != p.starName) "/${p.starAlias}" else ""}${if (marks.isNotBlank()) " [$marks]" else ""}")
    }
    if (r.process.isNotEmpty()) {
        appendLine()
        appendLine("── 起卦步骤 ──")
        r.process.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
    }
}

private fun buildWuxingInfo(wx: String): String = buildString {
    QiduwengData.WUXING_INFO.forEach { (sec, m) ->
        append(sec).append("：").append(m[wx] ?: "—").append("\n\n")
    }
}.trim()

@Composable
internal fun QdRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = AppColors.Muted, fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Text(value, color = QD_TEXT, fontSize = 13.sp)
    }
}

/** 原版信息行：左侧灰蓝标签，右侧内容 */
@Composable
private fun QdInfoLine(label: String, value: String, valueGray: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 14.dp)) {
        Text(label, color = QD_LABEL, fontSize = 15.sp, modifier = Modifier.width(52.dp))
        Text(value, color = if (valueGray) Color(0xFF999999) else QD_TEXT, fontSize = 15.sp)
    }
}

private val QD_LABEL = Color(0xFF4A5A8A)   // 灰蓝标签色
private fun qdWxColor(wx: String) = when (wx) {
    "金" -> Color(0xFFFFA500); "水" -> Color(0xFF0074E4); "木" -> Color(0xFF00A879)
    "火" -> Color(0xFFED4D49); "土" -> Color(0xFFA64B00); else -> QD_TEXT
}
/** 四柱天干地支按五行着色：甲乙木 丙丁火 戊己土 庚辛金 壬癸水；地支按主气五行 */
private fun qdGanColor(g: String) = when (g) {
    "甲", "乙", "寅", "卯" -> Color(0xFF00A879)
    "丙", "丁", "巳", "午" -> Color(0xFFED4D49)
    "戊", "己", "辰", "戌", "丑", "未" -> Color(0xFFA64B00)
    "庚", "辛", "申", "酉" -> Color(0xFFFFA500)
    "壬", "癸", "亥", "子" -> Color(0xFF0074E4)
    else -> QD_TEXT
}

@Composable
private fun QdRoleCard(modifier: Modifier, role: String, name: String, sub: String, state: String, color: Color, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(BoardTheme.CARD)
        .clickable(onClick = onClick).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(role, color = AppColors.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(name, color = color, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = AppColors.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(color).padding(horizontal = 12.dp, vertical = 3.dp)) {
            Text(state, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QdStarPlate(r: Qiduweng.Result) {
    // 按建寅顺序排成 4 行 3 列（寅卯辰/巳午未/申酉戌/亥子丑）
    val order = Qiduweng.DIZHI_ORDER
    val chunks = order.toList().chunked(3)
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        .clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFBBBBBB), RoundedCornerShape(8.dp))
        .background(Color.White)) {
        chunks.forEachIndexed { ri, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { ci, z ->
                    val item = r.plate.find { it.zhi == z }
                    val marks = buildString {
                        if (item?.isMonth == true) append("月")
                        if (item?.isDay == true) append("日")
                        if (item?.isHour == true) append("时")
                    }
                    Column(Modifier.weight(1f).height(60.dp).padding(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text(z, color = AppColors.Muted, fontSize = 10.sp)
                        Text(item?.starName ?: "", color = wxColor(item?.star ?: ""),
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(marks, color = BoardTheme.RED, fontSize = 10.sp)
                    }
                    if (ci < 2) Box(Modifier.width(1.dp).height(60.dp).background(Color(0xFFDDDDDD)))
                }
            }
            if (ri < chunks.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD)))
        }
    }
}

private fun wxColor(wx: String) = when (wx) {
    "木" -> BoardTheme.YEAR; "火" -> BoardTheme.MONTH; "金" -> BoardTheme.DAY
    "水" -> BoardTheme.MING; else -> Color(0xFF7A6A3A)
}

@Composable
private fun QdCaseCard(keys: List<String>, idx: Int, onIdx: (Int) -> Unit) {
    val key = keys.getOrNull(idx) ?: return
    val lines = QiduwengData.CASES[key] ?: emptyList()
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(14.dp)).background(Color.White)
        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(14.dp))
        .padding(horizontal = 12.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(50))
                .clickable { onIdx((idx - 1 + keys.size) % keys.size) }, contentAlignment = Alignment.Center) {
                Text("‹", color = QD_RED, fontSize = 20.sp)
            }
            Text(key, color = QD_RED, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(50))
                .clickable { onIdx((idx + 1) % keys.size) }, contentAlignment = Alignment.Center) {
                Text("›", color = QD_RED, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        for (line in lines) Text(line, color = Color(0xFF666666), fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("${idx + 1} / ${keys.size}", color = Color(0xFFBBBBBB), fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

private val QD_RED = Color(0xFFB0392E)

internal val QD_BG = Color(0xFFF5F1E8)
internal val QD_TEXT = Color(0xFF2A2620)

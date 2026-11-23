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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.MeiHua
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 梅花易数专用起卦页：仿原版 view_meihuayishu。
 * 三种起卦方式：时间起卦 / 数字起卦(+动爻加时辰) / 手动起卦(上下卦+动爻)。
 * 详情页：本卦/互卦/变卦/错卦/综卦六爻图 + 体用五行 + 动爻。
 */
@Composable
fun MeiHuaCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var method by remember { mutableStateOf(0) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var digits by remember { mutableStateOf("") }
    var addHour by remember { mutableStateOf(false) }
    var upperGua by remember { mutableStateOf(0) }
    var lowerGua by remember { mutableStateOf(7) }
    var deltaYao by remember { mutableStateOf(1) }
    var question by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<MeiHua.GuaData?>(null) }
    var castDesc by remember { mutableStateOf("") }
    var castDt by remember { mutableStateOf(LocalDateTime.now()) }

    if (result == null) {
        JiDialogShell(title = "梅 花 易 数", onBack = onBack, onCast = {
            val dt = LocalDateTime.of(date, time)
            val g = when (method) {
                0 -> MeiHua.byTime(dt)
                1 -> {
                    if (digits.isBlank()) { Toast.makeText(ctx, "请输入数字", Toast.LENGTH_SHORT).show(); return@JiDialogShell }
                    MeiHua.byNumber(digits, addHour, dt)
                }
                else -> MeiHua.compose(MeiHua.GUA8[upperGua], MeiHua.GUA8[lowerGua], deltaYao)
            }
            castDesc = when (method) {
                0 -> "时间起卦 · ${dt.format(dtFmt)}"
                1 -> "数字起卦 · $digits${if (addHour) "（动爻加时辰）" else ""}"
                else -> "手动起卦 · ${MeiHua.GUA8[upperGua]}上${MeiHua.GUA8[lowerGua]}下 · ${listOf("初","二","三","四","五","上")[deltaYao - 1]}爻动"
            }
            result = g
            castDt = dt
            try {
                val id = db.insert(dt.format(dtFmt), "梅花易数 · $question", "meihua", "梅花易数",
                    mhFaceText(g, question, castDesc), "梅花卦")
                Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { }
        }) {
            JiChipLine("起卦方式：", listOf("时间起卦", "数字起卦", "手动起卦"), method) { method = it }
            Spacer(Modifier.height(14.dp))
            when (method) {
                0 -> {
                    JiRow("当前时间：") {
                        JiDateInput("${date.toString()}  ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}", { showDate = true }, Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("起卦方法(月日数为农历)：上卦=(年支数+月数+日数)÷8余数；下卦=(年支数+月数+日数+时支数)÷8余数；动爻=÷6余数。",
                        color = JiForm.SUB, fontSize = 11.sp, lineHeight = 16.sp)
                }
                1 -> {
                    JiRow("输入数字：") { JiInput(digits, { digits = it.filter { c -> c.isDigit() }.take(10) }, "输入 1~10 位数字", Modifier.fillMaxWidth(), mono = true) }
                    Spacer(Modifier.height(10.dp))
                    JiRadioLine("动爻加时辰", if (addHour) 0 else 1, listOf("是", "否")) { addHour = it == 0 }
                }
                else -> {
                    JiChipLine("上卦：", MeiHua.GUA8.toList(), upperGua) { upperGua = it }
                    Spacer(Modifier.height(10.dp))
                    JiChipLine("下卦：", MeiHua.GUA8.toList(), lowerGua) { lowerGua = it }
                    Spacer(Modifier.height(10.dp))
                    JiChipLine("动爻：", listOf("初","二","三","四","五","上"), deltaYao - 1) { deltaYao = it + 1 }
                }
            }
            Spacer(Modifier.height(12.dp))
            JiRow("事项：") { JiInput(question, { question = it }, "输入预测事项（选填）", Modifier.fillMaxWidth()) }
        }
        if (showDate) JiDateTimeDialog(
            initial = LocalDateTime.of(date, time),
            onDismiss = { showDate = false },
            onConfirm = { dt -> date = dt.toLocalDate(); time = dt.toLocalTime().withSecond(0).withNano(0); showDate = false }
        )
    } else {
        MeiHuaDetailScreen(result!!, question, castDesc, castDt, onBack)
    }
}

@Composable
private fun MHPick(label: String, items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEachIndexed { i, s ->
                Box(Modifier.clip(RoundedCornerShape(8.dp)).weight(1f)
                    .background(if (i == selected) Color(0x2AD9A441) else BoardTheme.CARD)
                    .then(if (i == selected) Modifier.border(1.dp, BoardTheme.GOLD, RoundedCornerShape(8.dp)) else Modifier)
                    .clickable { onSelect(i) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(s, color = if (i == selected) BoardTheme.GOLD else AppColors.Paper, fontSize = 15.sp)
                }
            }
        }
    }
}

// ==================== 详情页：六爻图 + 体用 + 点击卦身看解读 ====================

@Composable
private fun MeiHuaDetailScreen(g: MeiHua.GuaData, question: String, desc: String, dt: java.time.LocalDateTime, onBack: () -> Unit) {
    val ctx = LocalContext.current
    // 选中查看的卦（默认本卦）
    var selKey by remember { mutableIntStateOf(0) } // 0本 1互 2变 3错 4综
    val guas = listOf(
        Triple("【本卦】", g.base, g.deltaYao),
        Triple("【互卦】", g.hu, 0),
        Triple("【变卦】", g.bian, 0),
        Triple("【错卦】", g.cuo, 0),
        Triple("【综卦】", g.zong, 0)
    )
    val selBits = guas[selKey].second
    // 从 assets 读取对应卦的解读文本（按 6 位卦码命名）
    val detailText = remember(selBits) {
        try {
            ctx.assets.open("gua64/$selBits.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "（暂无此卦解读文本）"
        }
    }
    // 农历、四柱、节气
    val lun = remember(dt) { com.fortune.stall.util.LunarCalendar.fromDateTime(dt) }
    val dateText = remember(dt, lun) {
        "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时${dt.minute}分（${lun.month}月${lun.day}日 ${lun.hourZhi}时）"
    }
    val jieqiText = remember(dt) {
        com.fortune.stall.util.SolarTerm.rangeAt(dt)?.let { "${it.from.display} ~ ${it.to.display}" } ?: ""
    }
    val methodName = desc.split(" · ").firstOrNull() ?: desc

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        // 红底标题栏（仿原版：← 标题 + 右图标）
        Box(Modifier.fillMaxWidth().background(BoardTheme.RED).padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("梅花易数", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("▣", color = Color.White, fontSize = 16.sp)
                    Text("☰", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // 白底信息区（紧凑）
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 3.dp)) {
            MhInfoLine("事项", if (question.isNotBlank()) question else "输入预测事项")
            MhInfoLine("时间", dateText)
            MhInfoLine("起卦方式", methodName)
            MhInfoLine("节气", jieqiText)
            // 四柱横排
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("四柱", color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(48.dp))
                for (p in listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)) {
                    val gan = p.getOrNull(0)?.toString() ?: ""
                    val zhi = p.getOrNull(1)?.toString() ?: ""
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(gan, color = Color(0xFF222222), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Text(zhi, color = Color(0xFF222222), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // 五卦一行并排（可点击切换解读）
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            guas.forEachIndexed { i, t ->
                MHGuaCard(t.first, t.second, MeiHua.guaName(t.second), t.third,
                    Modifier.weight(1f), selKey == i) { selKey = i }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text("↑ 点击卦身查看解释", color = Color(0xFF999999), fontSize = 11.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))

        // 卦解读文本（点击卦身切换）
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp)) {
            Text("【${MeiHua.guaName(selBits)}】", color = BoardTheme.RED, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(detailText, color = QD_TEXT, fontSize = 12.5.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(6.dp))

        // 体用信息
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp)) {
            Text("体 用 五 行", color = BoardTheme.RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MhInfoLine("本卦", "${MeiHua.guaName(g.base)}（${g.upper}上${g.lower}下）")
            MhInfoLine("动爻", "第 ${g.deltaYao} 爻（${listOf("初","二","三","四","五","上")[g.deltaYao - 1]}爻）动")
            MhInfoLine("体卦", "${g.tiGua} · ${g.tiWx}")
            MhInfoLine("用卦", "${g.yongGua} · ${g.yongWx}")
            MhInfoLine("体用", g.relation)
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(faceText = mhFaceText(g, question, desc))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable private fun MhInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(64.dp))
        Text(value, color = QD_TEXT, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

/** 八卦五行 → 爻色（仿原版：金橙褐/木绿/水青/火红/土褐） */
private fun mhWxColor(gua: String) = when (gua) {
    "乾", "兑" -> Color(0xFFC8862A)   // 金-橙褐
    "震", "巽" -> Color(0xFF3E9B4F)   // 木-绿
    "坎" -> Color(0xFF2E9AA8)         // 水-青
    "离" -> Color(0xFFC0392B)         // 火-红
    "艮", "坤" -> Color(0xFF9A6B3F)   // 土-褐
    else -> QD_TEXT
}

/** bits 前 3 位=上卦、后 3 位=下卦，查 GUA8 索引得八卦名 */
private fun mhYaoColors(bits: String): List<Color> {
    val g8 = MeiHua.GUA8
    val upper = mhYaoColorsTri(bits, 0, g8)
    val lower = mhYaoColorsTri(bits, 3, g8)
    return upper + lower
}

private fun mhYaoColorsTri(bits: String, start: Int, g8: Array<String>): List<Color> {
    val tri = bits.substring(start, minOf(start + 3, bits.length))
    if (tri.length != 3) return List(maxOf(0, 3 - start)) { QD_TEXT }
    val idx = (tri[0] - '0') * 4 + (tri[1] - '0') * 2 + (tri[2] - '0')
    val c = mhWxColor(g8.getOrNull(idx) ?: "")
    return List(3) { c }
}

/** 六爻卦象卡：bits 为 6 位（上卦在前），从左到右为上爻→初爻 */
@Composable
private fun MHCardYao(bits: String, deltaYao: Int) {
    val colors = mhYaoColors(bits)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 0 until minOf(6, bits.length)) {
            val isYang = bits[i] == '1'
            val isMoving = deltaYao > 0 && (6 - i) == deltaYao
            val lineColor = if (isMoving) BoardTheme.RED else colors.getOrNull(i) ?: QD_TEXT
            if (isYang) {
                Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(2.dp)).background(lineColor))
            } else {
                Row(Modifier.width(40.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(2.dp)).background(lineColor))
                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(2.dp)).background(lineColor))
                }
            }
        }
    }
}

@Composable
private fun MHGuaCard(title: String, bits: String, name: String, deltaYao: Int, modifier: Modifier,
                      selected: Boolean = false, onClick: () -> Unit = {}) {
    Column(modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFFFFF3E0) else Color(0xFFF7F5EF))
        .then(if (selected) Modifier.border(2.dp, BoardTheme.RED, RoundedCornerShape(8.dp)) else Modifier.border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp)))
        .clickable { onClick() }.padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = if (selected) BoardTheme.RED else Color(0xFF555555), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("用", color = Color(0xFF888888), fontSize = 8.sp)
        MHCardYao(bits, deltaYao)
        Text("体", color = Color(0xFF888888), fontSize = 8.sp)
        Spacer(Modifier.height(3.dp))
        Text(name, color = QD_TEXT, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

private fun mhFaceText(g: MeiHua.GuaData, q: String, desc: String): String = buildString {
    append("梅花易数排盘\n")
    if (q.isNotBlank()) append("事项：$q\n")
    append("$desc\n")
    append("本卦：${MeiHua.guaName(g.base)}  互卦：${MeiHua.guaName(g.hu)}  变卦：${MeiHua.guaName(g.bian)}\n")
    append("错卦：${MeiHua.guaName(g.cuo)}  综卦：${MeiHua.guaName(g.zong)}\n")
    append("动爻：第 ${g.deltaYao} 爻\n")
    append("体：${g.tiGua}(${g.tiWx})  用：${g.yongGua}(${g.yongWx})  ${g.relation}\n")
}

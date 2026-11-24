package com.fortune.stall.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.DaoJiaLiuRen
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.AiBridge
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DJ_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val DJ_BG = Color(0xFFF5F1E8)
private val DJ_CARD = Color(0xFFFBF8F1)
private val DJ_LINE = Color(0x33B89B6A)
private val DJ_GOLD = Color(0xFFA8843E)
private val DJ_RED = Color(0xFFB23A2E)

private val DJ_WX_COLOR = mapOf(
    "木" to Color(0xFF3E7A4E), "火" to Color(0xFFC0392B), "土" to Color(0xFF9A6F2E),
    "金" to Color(0xFF7A7A80), "水" to Color(0xFF2E5C8A)
)

/** 道家小六壬 · 六宫排盘页 */
@Composable
fun DaoJiaLiuRenScreen(db: FortuneDb, onBack: () -> Unit) {
    val context = LocalContext.current
    var method by remember { mutableStateOf(0) } // 0=时间 1=数字 2=随机
    var dt by remember { mutableStateOf(LocalDateTime.now()) }
    var numberText by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<DaoJiaLiuRen.Chart?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var expandedPalace by remember { mutableStateOf<Int?>(null) }

    fun doCast() {
        val now = if (method == 2) LocalDateTime.now() else dt
        result = DaoJiaLiuRen.cast(now)
    }

    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(25.dp))
            // 顶栏
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFC0532B), Color(0xFFD4703E))))
                    .clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("道 家 小 六 壬", color = AppColors.Paper, fontSize = 19.sp,
                        fontFamily = FontFamily.Serif, letterSpacing = 2.sp)
                    Text("大安起月 · 月上起日 · 日上起时", color = AppColors.Muted, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
            Spacer(Modifier.height(18.dp))

            if (result == null) {
                // ===== 起卦表单 =====
                DJCard {
                    Text("预测事项", color = DJ_GOLD, fontSize = 13.sp, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = question, onValueChange = { question = it.take(120) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入你所问之事…", color = Color(0xFFAAA39A), fontSize = 13.sp) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFFFFF), unfocusedContainerColor = Color(0xFFFFFFFF),
                            focusedTextColor = Color(0xFF3A3226), unfocusedTextColor = Color(0xFF3A3226),
                            cursorColor = DJ_GOLD, focusedBorderColor = DJ_GOLD, unfocusedBorderColor = DJ_LINE
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )
                }
                Spacer(Modifier.height(12.dp))

                DJCard {
                    Text("起卦方式", color = DJ_GOLD, fontSize = 13.sp, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("时间起卦", "数字起卦", "随机起卦").forEachIndexed { i, label ->
                            val sel = method == i
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                .background(if (sel) DJ_GOLD else Color(0x12000000))
                                .clickable { method = i }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center) {
                                Text(label, color = if (sel) Color.White else Color(0xFF6B6258), fontSize = 11.5.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    when (method) {
                        0 -> {
                            Text("公历时间", color = DJ_GOLD, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dt.format(DJ_DT_FMT), color = Color(0xFF3A3226), fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { showDate = true })
                                Spacer(Modifier.width(8.dp))
                                Text("↻", color = DJ_RED, fontSize = 18.sp,
                                    modifier = Modifier.clickable { dt = LocalDateTime.now() })
                                Spacer(Modifier.width(8.dp))
                                Text("选时", color = DJ_GOLD, fontSize = 11.sp,
                                    modifier = Modifier.clickable { showTime = true })
                            }
                            Spacer(Modifier.height(12.dp))
                            val preview = DaoJiaLiuRen.cast(dt)
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0E9DA)).padding(12.dp)) {
                                Column {
                                    Text("时间推算", color = DJ_GOLD, fontSize = 11.sp,
                                        fontFamily = FontFamily.Serif)
                                    Spacer(Modifier.height(6.dp))
                                    Text("农历 ${preview.lunarDisplay} ${preview.shichenDisplay}",
                                        color = Color(0xFF4A4236), fontSize = 13.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("干支 ${preview.ganzhiDisplay}",
                                        color = Color(0xFF4A4236), fontSize = 12.sp)
                                }
                            }
                        }
                        1 -> {
                            Text("输入数字（1-999）", color = DJ_GOLD, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = numberText,
                                onValueChange = { if (it.all { c -> c.isDigit() }) numberText = it.take(3) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("如 168", color = Color(0xFFAAA39A)) },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFFFFFFF), unfocusedContainerColor = Color(0xFFFFFFFF),
                                    focusedTextColor = Color(0xFF3A3226), unfocusedTextColor = Color(0xFF3A3226),
                                    cursorColor = DJ_GOLD, focusedBorderColor = DJ_GOLD, unfocusedBorderColor = DJ_LINE
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp)
                            )
                        }
                        else -> Text("点击排盘将随机起一宫", color = Color(0xFF6B6258), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFC0532B), Color(0xFFD4703E))))
                    .clickable { doCast() }, contentAlignment = Alignment.Center) {
                    Text("起 卦 排 盘", color = Color.White, fontSize = 17.sp,
                        fontFamily = FontFamily.Serif, letterSpacing = 4.sp)
                }
            } else {
                // ===== 结果页 =====
                DJResult(result!!, question, expandedPalace) { expandedPalace = if (expandedPalace == it) null else it }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DJResultButton("重新起卦", false, Modifier.weight(1f)) { result = null }
                    DJResultButton("复制排盘", false, Modifier.weight(1f)) {
                        val text = DaoJiaLiuRen.faceText(result!!)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("道家小六壬排盘", text))
                        Toast.makeText(context, "排盘已复制", Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DJResultButton("存入卦账", false, Modifier.weight(1f)) {
                        val createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        val title = question.ifBlank { "道家小六壬" }
                        val face = DaoJiaLiuRen.faceText(result!!)
                        db.insert(createdAt, title, "道家小六壬", "六宫排盘", face,
                            "${result!!.resultName} · ${result!!.resultSummary}")
                        Toast.makeText(context, "已存入卦账", Toast.LENGTH_SHORT).show()
                    }
                    DJResultButton("问 AI", true, Modifier.weight(1f)) {
                        val prompt = "请用道家小六壬解读这一卦：\n${DaoJiaLiuRen.faceText(result!!)}" +
                            if (question.isBlank()) "" else "\n\n我所问之事：$question"
                        AiBridge.askAi?.invoke(prompt)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDate) DJDateDialog(dt.toLocalDate(), { showDate = false }) { d ->
        dt = d.atTime(dt.toLocalTime()); showDate = false
    }
    if (showTime) DJTimeDialog(dt.toLocalTime(), { showTime = false }) { t ->
        dt = dt.toLocalDate().atTime(t); showTime = false
    }
}

@Composable
private fun DJCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(DJ_CARD).border(1.dp, DJ_LINE, RoundedCornerShape(14.dp))
        .padding(16.dp)) { content() }
}

@Composable
private fun DJResult(
    c: DaoJiaLiuRen.Chart, question: String, expanded: Int?, onExpand: (Int) -> Unit
) {
    // 时宫结果大卡
    val resultWx = c.palaces.first { it.isResult }.element
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF2E2A24), Color(0xFF464033))))
        .border(1.dp, Color(0x66D9A441), RoundedCornerShape(16.dp))
        .padding(20.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("时 宫 结 果", color = Color(0xD9D9A441), fontSize = 11.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(8.dp))
            Text(c.resultName, color = Color(0xFFF0D89A), fontSize = 38.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
            Spacer(Modifier.height(8.dp))
            Text(c.resultSummary, color = Color(0xCCF4F0E6), fontSize = 12.5.sp, textAlign = TextAlign.Center)
        }
    }
    Spacer(Modifier.height(12.dp))

    // 落宫路径
    DJCard {
        Text("落 宫 顺 序", color = DJ_GOLD, fontSize = 12.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            DJPathStep("月", DaoJiaLiuRen.palaceName(c.monthPalace))
            Text("→", color = DJ_GOLD, fontSize = 18.sp)
            DJPathStep("日", DaoJiaLiuRen.palaceName(c.dayPalace))
            Text("→", color = DJ_GOLD, fontSize = 18.sp)
            DJPathStep("时", DaoJiaLiuRen.palaceName(c.hourPalace), highlight = true)
        }
    }
    Spacer(Modifier.height(12.dp))

    // 六宫网格（九宫式：中列空，按 GRID_ORDER）
    DJCard {
        Text("六 宫 盘 面 · 点宫看断语", color = DJ_GOLD, fontSize = 12.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(12.dp))
        DJGrid(c) { onExpand(it) }
    }
    Spacer(Modifier.height(12.dp))

    // 历法信息
    DJCard {
        DJInfoRow("阳历", c.solarDisplay)
        Spacer(Modifier.height(6.dp))
        DJInfoRow("农历", c.lunarDisplay + " " + c.shichenDisplay)
        Spacer(Modifier.height(6.dp))
        DJInfoRow("干支", c.ganzhiDisplay)
        Spacer(Modifier.height(6.dp))
        DJInfoRow("旬空", c.kongWang)
    }

    // 展开的宫位断语
    if (expanded != null) {
        Spacer(Modifier.height(12.dp))
        DJPalaceDetail(expanded) { onExpand(expanded) }
    }
}

@Composable
private fun DJPathStep(label: String, name: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
            .background(if (highlight) Brush.linearGradient(listOf(Color(0xFFC0532B), Color(0xFFD4703E))) else Brush.linearGradient(listOf(Color(0xFFEDE4D2), Color(0xFFE3D6BC))))
            .border(1.dp, if (highlight) Color(0xFFD9A441) else DJ_LINE, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Text(name, color = if (highlight) Color.White else Color(0xFF5A4A32),
                fontSize = 15.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        Text("$label 宫", color = if (highlight) DJ_RED else Color(0xFF8A7A62), fontSize = 10.5.sp)
    }
}

@Composable
private fun DJGrid(c: DaoJiaLiuRen.Chart, onClick: (Int) -> Unit) {
    // 原版为两行六宫：NO.2 NO.3 NO.4 / NO.1 NO.6 NO.5
    val order = DaoJiaLiuRen.GRID_ORDER
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until 3) {
                    val id = order[row * 3 + col]
                    val p = c.palaces.first { it.id == id }
                    val tint = DJ_WX_COLOR[p.element] ?: DJ_GOLD
                    Box(Modifier.weight(1f).height(104.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (p.isResult) Color(0xFFFFF3E0) else Color(0xFFF4EEE1))
                        .border(1.dp, if (p.isResult) DJ_RED else DJ_LINE, RoundedCornerShape(10.dp))
                        .clickable { onClick(id) }
                        .padding(horizontal = 7.dp, vertical = 8.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(p.code, color = Color(0xFF9A8E7A), fontSize = 8.sp,
                                    modifier = Modifier.weight(1f))
                                if (p.tags.isNotEmpty()) {
                                    Text(p.tags.joinToString(""), color = DJ_RED,
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(p.name, color = tint, fontSize = 17.sp,
                                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                            Text("${p.heavenlyStem}${p.earthlyBranch}(${p.element})",
                                color = Color(0xFF6B5F4E), fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${p.fiveStar} ${p.sixGod}", color = Color(0xFF8A7A62), fontSize = 8.5.sp)
                            Text(p.sixKin, color = Color(0xFF8A7A62), fontSize = 8.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DJPalaceDetail(id: Int, onClose: () -> Unit) {
    val k = DaoJiaLiuRen.KNOWLEDGE[id] ?: return
    val p = DaoJiaLiuRen.PALACES.first { it.id == id }
    DJCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${p.name} · ${p.element}", color = DJ_GOLD, fontSize = 15.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("收起", color = DJ_RED, fontSize = 11.sp, modifier = Modifier.clickable { onClose() })
        }
        Spacer(Modifier.height(10.dp))
        Text(k.song, color = Color(0xFF6A4E2A), fontSize = 12.5.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(10.dp))
        DJDetailLine("总论", k.general)
        DJDetailLine("感情", k.love)
        DJDetailLine("事业", k.career)
        DJDetailLine("财运", k.wealth)
        DJDetailLine("健康", k.health)
        DJDetailLine("应期", k.timing)
        DJDetailLine("方位", k.direction)
    }
}

@Composable
private fun DJDetailLine(label: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label：", color = DJ_GOLD, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text, color = Color(0xFF5A5044), fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DJInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = DJ_GOLD, fontSize = 11.5.sp, modifier = Modifier.width(44.dp))
        Text(value, color = Color(0xFF4A4236), fontSize = 11.5.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DJResultButton(text: String, gold: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(10.dp))
        .background(if (gold) Brush.horizontalGradient(listOf(Color(0xFFC0532B), Color(0xFFD4703E))) else Brush.horizontalGradient(listOf(Color(0xFF6A5C46), Color(0xFF85755A))))
        .clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun DJDateDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }
    val months = listOf("正月","二月","三月","四月","五月","六月","七月","八月","九月","十月","冬月","腊月")
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DJ_CARD).padding(20.dp)) {
            Text("选择日期", color = DJ_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            DJNumStepper("年", year, 1900, 2100) { year = it }
            Spacer(Modifier.height(8.dp))
            DJNumStepper("月", month, 1, 12) { month = it }
            Spacer(Modifier.height(8.dp))
            DJNumStepper("日", day, 1, 31) { day = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DJResultButton("取消", false, Modifier.weight(1f)) { onDismiss() }
                DJResultButton("确定", true, Modifier.weight(1f)) {
                    val d = runCatching { LocalDate.of(year, month, day) }.getOrDefault(initial)
                    onConfirm(d)
                }
            }
        }
    }
}

@Composable
private fun DJTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DJ_CARD).padding(20.dp)) {
            Text("选择时辰", color = DJ_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            DJNumStepper("时", hour, 0, 23) { hour = it }
            Spacer(Modifier.height(8.dp))
            DJNumStepper("分", minute, 0, 59) { minute = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DJResultButton("取消", false, Modifier.weight(1f)) { onDismiss() }
                DJResultButton("确定", true, Modifier.weight(1f)) { onConfirm(LocalTime.of(hour, minute)) }
            }
        }
    }
}

@Composable
private fun DJNumStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DJ_GOLD, fontSize = 12.sp, modifier = Modifier.width(34.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x12000000))
            .clickable { if (value > min) onChange(value - 1) }, contentAlignment = Alignment.Center) {
            Text("−", color = DJ_GOLD, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(value.toString(), color = Color(0xFF4A4236), fontSize = 16.sp,
            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x12000000))
            .clickable { if (value < max) onChange(value + 1) }, contentAlignment = Alignment.Center) {
            Text("+", color = DJ_GOLD, fontSize = 18.sp)
        }
    }
}

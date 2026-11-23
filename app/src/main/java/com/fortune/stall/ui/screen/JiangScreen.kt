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
import com.fortune.stall.engine.FortuneEngine
import com.fortune.stall.engine.JiangExplain
import com.fortune.stall.engine.JiangXiaoLiuRen
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val CATEGORIES = listOf("吉凶", "事业", "感情", "财运", "官司", "健康", "出行", "其他")

/** 江氏小六壬 · 六宫排盘页 */
@Composable
fun JiangScreen(db: FortuneDb) {
    val context = LocalContext.current
    var method by remember { mutableStateOf(JiangXiaoLiuRen.Method.RISHI) }
    var liuShenMode by remember { mutableStateOf(JiangXiaoLiuRen.LiuShenMode.HUO) }
    var category by remember { mutableStateOf("吉凶") }
    var dateText by remember { mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var timeText by remember { mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var numberText by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var chart by remember { mutableStateOf<JiangXiaoLiuRen.Chart?>(null) }
    var faceText by remember { mutableStateOf("") }
    var explainCell by remember { mutableStateOf<JiangXiaoLiuRen.PalaceCell?>(null) }
    var castDt by remember { mutableStateOf(LocalDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 当前选择对应的完整时刻（日期+时间）
    fun currentDt(): LocalDateTime = try {
        LocalDateTime.parse("$dateText $timeText", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (e: Exception) { LocalDateTime.now() }

    val previewLunar = remember(dateText, timeText) { LunarCalendar.fromDateTime(currentDt()) }

    fun reset() {
        chart = null; faceText = ""; question = ""; numberText = ""
    }

    fun doCast() {
        val dt = currentDt()
        val num = numberText.toIntOrNull()?.takeIf { method == JiangXiaoLiuRen.Method.SHUZI && it >= 1 }
        val q = question.trim()
        val qi = SolarTerm.rangeAt(dt)
        val qiText = if (qi == null) "—" else "${qi.from.display} ~ ${qi.to.display}"
        val (c, face) = JiangXiaoLiuRen.cast(method, dt, num, q, qiText, liuShenMode)
        castDt = dt
        // 落账（卦面即印章）
        val now = LocalDateTime.now()
        val id = db.insert(
            now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            if (q.isBlank()) category else "$category · $q",
            "jiangxiaoliuren", method.name.lowercase(),
            "落印中…", FortuneEngine.sealLine(now)
        )
        db.updateFace(id, face)
        chart = c; faceText = face
    }

    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题与时间条仅在起卦前显示；排盘后只留宫格，对齐原版
            if (chart == null) {
                Spacer(Modifier.height(25.dp))
                Text("江 氏 小 六 壬", color = AppColors.Paper, fontSize = 26.sp,
                    fontFamily = FontFamily.Serif, letterSpacing = 3.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))

                // —— 起卦表单：字段按起课方式分开，对齐 APK 各起课页 ——
                JFieldLabel("排盘类型")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (sm in JiangXiaoLiuRen.LiuShenMode.values()) {
                        val on = liuShenMode == sm
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                            .background(if (on) Color(0x1AD9A441) else Color(0x10FFFFFF))
                            .then(if (on) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(999.dp)) else Modifier)
                            .clickable { liuShenMode = sm; reset() }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center) {
                            Text(sm.label, color = if (on) AppColors.Lantern else AppColors.Paper, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                JFieldLabel("起课方式")
                val methods = JiangXiaoLiuRen.Method.values().toList()
                for (row in listOf(methods.take(2), methods.drop(2))) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in row) {
                            val on = method == m
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                                .background(if (on) Color(0x1AD9A441) else Color(0x10FFFFFF))
                                .then(if (on) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(999.dp)) else Modifier)
                                .clickable { method = m; reset() }.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center) {
                                Text(m.label, color = if (on) AppColors.Lantern else AppColors.Paper, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(6.dp))

                // 数字起课：数字输入行
                if (method == JiangXiaoLiuRen.Method.SHUZI) {
                    JFieldLabel("数字")
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0x0FFFFFFF)).border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)) {
                        if (numberText.isEmpty()) Text("点击输入数字", color = AppColors.Dim, fontSize = 13.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = numberText,
                            onValueChange = { numberText = it.filter { ch -> ch.isDigit() }.take(3) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = AppColors.Paper),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 日时起课：日期选择行
                if (method == JiangXiaoLiuRen.Method.RISHI) {
                    JFieldLabel("日期")
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0x0FFFFFFF)).border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Text(dateText, color = AppColors.Paper, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 时刻/日时/数字三种方式都要选时间；随机起课不需要
                if (method != JiangXiaoLiuRen.Method.SUJI) {
                    JFieldLabel("时间")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(Color(0x0FFFFFFF)).border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp))
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text(timeText, color = AppColors.Paper, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x1AD9A441))
                            .clickable {
                                val now = LocalDateTime.now()
                                dateText = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"))
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                        ) { Text("获取当前时间", color = AppColors.Lantern, fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(6.dp))
                    // 农历预览：时刻起课显示「X时X刻」，其余显示「农历X月X X时」
                    val lunarPreview = when (method) {
                        JiangXiaoLiuRen.Method.SHIKE ->
                            "${previewLunar.hourZhi}时${JiangXiaoLiuRen.keZhi(currentDt().hour, currentDt().minute)}刻"
                        else -> "农历${previewLunar.month}月${previewLunar.day}日 ${previewLunar.hourZhi}时"
                    }
                    Text(lunarPreview, color = AppColors.Lantern, fontSize = 11.5.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                }

                JFieldLabel("事由")
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x0FFFFFFF))
                    .border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    androidx.compose.material3.TextField(
                        value = question, onValueChange = { question = it.take(120) },
                        placeholder = { Text("请输入起课事由（非必填）", color = AppColors.Dim, fontSize = 13.sp) },
                        minLines = 2, maxLines = 4,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                            cursorColor = AppColors.Lantern, focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(18.dp))
                JPrimeButton("确 定") { doCast() }
                Spacer(Modifier.height(40.dp))
            } else {
                // —— 排盘结果 ——
                Spacer(Modifier.height(14.dp))
                val c = chart!!
                val qiRange = SolarTerm.rangeAt(castDt)
                JInfoTable(c, question.trim(), qiRange)
                Spacer(Modifier.height(12.dp))
                JJiangGrid(c) { explainCell = it }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    JResultButton("复制卦面 · 拿去给 AI 断", gold = true, Modifier.weight(1.2f)) {
                        copyText(context, faceText)
                        Toast.makeText(context, "卦面已复制——拿去给你的 AI 断吧", Toast.LENGTH_SHORT).show()
                    }
                    JResultButton("再问一事", gold = false, Modifier.weight(0.8f)) { reset() }
                }
                Spacer(Modifier.height(10.dp))
                JResultButton("✦ 问 AI 解读卦面", gold = false, Modifier.fillMaxWidth()) {
                    val cb = com.fortune.stall.util.AiBridge.askAi
                    if (cb != null) cb(faceText) else Toast.makeText(context, "AI 通道未就绪", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(40.dp))
            }
        }
        explainCell?.let { cell ->
            JExplainDialog(cell = cell, onDismiss = { explainCell = null })
        }
        if (showDatePicker) {
            JDateOnlyDialog(
                initial = try {
                    LocalDate.parse(dateText.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                } catch (e: Exception) { LocalDate.now() },
                onDismiss = { showDatePicker = false },
                onConfirm = { d ->
                    dateText = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    showDatePicker = false
                }
            )
        }
        if (showTimePicker) {
            JTimeDialog(
                initial = try {
                    LocalTime.parse(timeText.trim(), DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) { LocalTime.now() },
                onDismiss = { showTimePicker = false },
                onConfirm = { t ->
                    timeText = t.format(DateTimeFormatter.ofPattern("HH:mm"))
                    showTimePicker = false
                }
            )
        }
    }
}

@Composable
private fun JInfoStrip(lun: LunarCalendar.LunarDate) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0x141E1C1A), Color(0x14161412))))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${lun.yearGanZhi}年 ${lun.monthGanZhi}月 ${lun.dayGanZhi}日 ${lun.hourGanZhi}时",
            color = AppColors.Paper, fontSize = 15.sp, fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center)
        Text("农历${lun.month}月${lun.day}日 · ${lun.hourZhi}时", color = AppColors.Lantern,
            fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// jishiyu 吉时雨 · 宫格视觉规范（与小六壬三宫完全一致，纯 Compose 还原，非 HTML）
private val J_THEME = Color(0xFFD12824)          // --theme-color
private val J_GRID_LINE = Color(0xFF000000)      // 黑框
private val J_GONG_BG = Color(0xFFFFFFFF)        // 白底
private val J_MARK_GRAY = Color(0xFF999999)      // 打标灰
private val J_INK = Color(0xFF222222)            // 六神/六亲黑字
// 五行色（jishiyu --jin/--shui/--mu/--huo/--tu）
private val J_WX = mapOf(
    "金" to Color(0xFFFFA500), "水" to Color(0xFF0074E4), "木" to Color(0xFF00A879),
    "火" to Color(0xFFED4D49), "土" to Color(0xFFA64B00)
)
private fun jwx(w: String) = J_WX[w] ?: Color(0xFF4B3521)
/** 九星 → 五行（天空无五行，返回空） */
private fun jXingWx(xing: String) = when (xing) {
    "木星" -> "木"; "火星" -> "火"; "土星" -> "土"; "金星" -> "金"; "水星" -> "水"; else -> ""
}

/** jishiyu 风格信息表：事项 / 起局 / 日期 / 节气 / 四柱 / 空亡 */
@Composable
private fun JInfoTable(c: JiangXiaoLiuRen.Chart, question: String, qi: SolarTerm.TermRange?) {
    val labelColor = Color(0xFF3F5375)   // 蓝灰表头
    val valueColor = Color(0xFF222222)
    val qiColor = Color(0xFF2E5A88)
    val rowAlt = Color(0xFFF7F8FA)
    val lineColor = Color(0xFFE2E5EA)
    val qText = if (question.isBlank()) "输入预测事项" else question
    val qiText = if (qi == null) "—" else "${qi.from.display} ~ ${qi.to.display}"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
        .background(Color.White).border(1.dp, lineColor, RoundedCornerShape(6.dp))) {
        // 通用信息行：标签(左) + 值(跨列)
        JInfoRow("事项", qText, labelColor, if (question.isBlank()) Color(0xFF9AA0A8) else valueColor, rowAlt, lineColor)
        // 起课信息按 APK formattedCourseInfo：每种方式各自的「时间/日期」整行显示
        JInfoRow("起课", c.courseInfoText, labelColor, valueColor, Color.White, lineColor, 12.sp)
        JInfoRow("节气", qiText, labelColor, qiColor, Color.White, lineColor, 11.sp)
        // 四柱：表头行 + 干支行 + 空亡行（5 列：标签 + 年月日时）
        val colLabels = listOf("年柱", "月柱", "日柱", "时柱")
        val gz = listOf(c.yearGz, c.monthGz, c.dayGz, c.hourGz)
        val kong = listOf(c.yearKong, c.monthKong, c.dayKong, c.hourKong)
        JInfoGridRow("四柱", colLabels, labelColor, Color(0xFFEEF1F5), valueColor, lineColor, 12.5.sp, false)
        JInfoGridRow("", gz, labelColor, Color.White, Color(0xFF3F5375), lineColor, 20.sp, true)
        JInfoGridRow("空亡", kong, labelColor, rowAlt, valueColor, lineColor, 14.sp, false)
    }
}

/** 单行：左标签 + 右值 */
@Composable
private fun JInfoRow(
    label: String, value: String, labelColor: Color, valueColor: Color,
    bg: Color, lineColor: Color, valueSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(label, color = labelColor, fontSize = 13.sp, modifier = Modifier.width(52.dp))
        Text(value, color = valueColor, fontSize = valueSize, modifier = Modifier.weight(1f))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(lineColor))
}

/** 网格行：左标签 + 4 列 */
@Composable
private fun JInfoGridRow(
    label: String, values: List<String>, labelColor: Color, bg: Color,
    valueColor: Color, lineColor: Color, valueSize: androidx.compose.ui.unit.TextUnit,
    bold: Boolean
) {
    Row(Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = labelColor, fontSize = 13.sp, modifier = Modifier.width(52.dp))
        values.forEach { v ->
            Text(v, color = valueColor, fontSize = valueSize,
                fontFamily = FontFamily.Serif,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(lineColor))
}

/**
 * 江氏六宫格：3列×2行，白底细黑线。
 * 每宫 2列×4行：六神|九星 · 地支(五行)|六亲 · 宫名标签|日时身打标 · 底部五行色带。
 */
@Composable
private fun JJiangGrid(c: JiangXiaoLiuRen.Chart, onCell: (JiangXiaoLiuRen.PalaceCell) -> Unit) {
    val order = JiangXiaoLiuRen.DISPLAY_ORDER
    Column(Modifier.fillMaxWidth().background(J_GONG_BG).border(1.dp, J_GRID_LINE)) {
        for (r in 0..1) {
            if (r > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(J_GRID_LINE))
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..2) {
                    if (col > 0) Box(Modifier.width(1.dp).height(148.dp).background(J_GRID_LINE))
                    JPalaceCard(c.cells[order[r * 3 + col]], Modifier.weight(1f), onCell)
                }
            }
        }
    }
}

@Composable
private fun JPalaceCard(
    cell: JiangXiaoLiuRen.PalaceCell,
    modifier: Modifier,
    onCell: (JiangXiaoLiuRen.PalaceCell) -> Unit
) {
    val xingWx = jXingWx(cell.wuXing)
    val xingColor = if (xingWx.isEmpty()) J_MARK_GRAY else jwx(xingWx)
    val zhiWx = LunarCalendar.ZHI_WUXING[cell.zhi] ?: "木"
    // 宫格底部标记按 APK getRiShiText 随起课方式变化（时刻=时/刻、日时=日/时、数字/随机=时+数字）
    val mark = cell.riShiText
    Column(modifier.height(148.dp).background(J_GONG_BG).clickable { onCell(cell) }) {
        // 行1：九星（左，五行色） / 六神（右，黑粗）—— 按用户要求互换
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp, vertical = 4.dp)) {
            Text(cell.wuXing, color = xingColor, fontSize = 13.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(cell.liuShen, color = J_INK, fontSize = 13.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        // 行2：地支（左，五行色，按 APK 不显示金木水火土） / 六亲（右）—— 身宫显示红「身」+ 主事六亲
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(cell.zhi, color = jwx(zhiWx), fontSize = 14.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                if (cell.isBody) {
                    Text("身", color = Color(0xFFC0392B), fontSize = 12.sp,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 3.dp))
                    if (cell.zhushiRen.isNotEmpty())
                        Text(cell.zhushiRen, color = J_INK, fontSize = 13.sp, fontFamily = FontFamily.Serif)
                } else {
                    Text(cell.liuQin, color = J_INK, fontSize = 13.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
        // 行3：日时身打标（左，灰） / 宫名（右，黑色粗体）—— 按 APK 界面宫名在右
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(mark, color = Color(0xFFC0392B), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(cell.palaceName, color = J_INK, fontSize = 15.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        // 行4：底部五行色带（宫五行）
        Box(Modifier.fillMaxWidth().height(10.dp).background(jwx(cell.palaceWuxing)))
    }
}

/** 日期选择弹窗（年月日），供日时起课用 */
@Composable
private fun JDateOnlyDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }
    fun daysInMonth(): Int = java.time.YearMonth.of(year, month).lengthOfMonth()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text("选择日期", color = AppColors.Lantern, fontSize = 17.sp,
                fontFamily = FontFamily.Serif, letterSpacing = 2.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JNumberStepper("年", year, 1900, 2100, Modifier.weight(1.3f)) { year = it }
                JNumberStepper("月", month, 1, 12, Modifier.weight(0.9f)) {
                    month = it
                    val max = daysInMonth()
                    if (day > max) day = max
                }
                JNumberStepper("日", day, 1, daysInMonth(), Modifier.weight(0.9f)) { day = it }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x12FFFFFF))
                    .clickable { onDismiss() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x2AD9A441))
                    .border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp))
                    .clickable { onConfirm(LocalDate.of(year, month, day)) }
                    .padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("确 定", color = AppColors.Lantern, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

/** 时间选择弹窗（十二时辰 + 十二刻），供时刻/日时/数字起课用 */
@Composable
private fun JTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var zhiIndex by remember { mutableStateOf(LunarCalendar.DIZHI.indexOf(LunarCalendar.hourToZhi(initial.hour)).let { if (it < 0) 0 else it }) }
    var keIndex by remember { mutableStateOf((initial.minute / 10) % 12) }
    fun zhiHour(idx: Int): Int = (idx * 2 + 23) % 24
    fun hourOfKe(): Int = (zhiHour(zhiIndex) + (keIndex * 10) / 60) % 24
    fun minuteOfKe(): Int = (keIndex * 10) % 60
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text("选择时间", color = AppColors.Lantern, fontSize = 17.sp,
                fontFamily = FontFamily.Serif, letterSpacing = 2.sp)
            Spacer(Modifier.height(14.dp))
            Text("选择时辰", color = AppColors.Muted, fontSize = 12.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            val shichen = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
            Column(Modifier.fillMaxWidth()) {
                for (r in 0..2) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0..3) {
                            val idx = r * 4 + c
                            val on = idx == zhiIndex
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                .background(if (on) Color(0x2AD9A441) else Color(0x10FFFFFF))
                                .then(if (on) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(9.dp)) else Modifier)
                                .clickable { zhiIndex = idx }.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center) {
                                Text(shichen[idx], color = if (on) AppColors.Lantern else AppColors.Paper,
                                    fontSize = 14.sp, fontFamily = FontFamily.Serif)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("选择刻（每刻 10 分钟）", color = AppColors.Muted, fontSize = 12.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Column(Modifier.fillMaxWidth()) {
                for (r in 0..1) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0..5) {
                            val idx = r * 6 + c
                            val on = idx == keIndex
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                .background(if (on) Color(0x2AD9A441) else Color(0x10FFFFFF))
                                .then(if (on) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(9.dp)) else Modifier)
                                .clickable { keIndex = idx }.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center) {
                                Text(LunarCalendar.DIZHI[idx], color = if (on) AppColors.Lantern else AppColors.Paper,
                                    fontSize = 13.sp, fontFamily = FontFamily.Serif)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("${LunarCalendar.DIZHI[zhiIndex]}时${LunarCalendar.DIZHI[keIndex]}刻 · ${String.format("%02d:%02d", hourOfKe(), minuteOfKe())}",
                color = AppColors.Muted, fontSize = 11.5.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x12FFFFFF))
                    .clickable { onDismiss() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x2AD9A441))
                    .border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp))
                    .clickable { onConfirm(LocalTime.of(hourOfKe(), minuteOfKe())) }
                    .padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("确 定", color = AppColors.Lantern, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

/** 数字增减器（标签 + ◀ 数值 ▶） */
@Composable
private fun JNumberStepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x0FFFFFFF))
        .border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AppColors.Muted, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text("◀", color = if (value > min) AppColors.Lantern else AppColors.Dim, fontSize = 13.sp,
                modifier = Modifier.clickable(enabled = value > min) { onChange(value - 1) }.padding(horizontal = 8.dp, vertical = 4.dp))
            Text(value.toString(), color = AppColors.Paper, fontSize = 18.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 2.dp))
            Text("▶", color = if (value < max) AppColors.Lantern else AppColors.Dim, fontSize = 13.sp,
                modifier = Modifier.clickable(enabled = value < max) { onChange(value + 1) }.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun JExplainDialog(cell: JiangXiaoLiuRen.PalaceCell, onDismiss: () -> Unit) {
    // 按 APK 宫位弹窗逻辑：口诀+六宫类象 → 六神 → 六亲 → 五星
    val terms = JiangExplain.termsFor(cell.palaceName, cell.liuShen, cell.liuQin, cell.wuXing)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text("${cell.palaceName} · 宫位详解", color = AppColors.Lantern, fontSize = 17.sp,
                fontFamily = FontFamily.Serif, letterSpacing = 2.sp)
            Text("${cell.palaceName}宫 · ${cell.wuXing} · ${cell.liuShen} · ${cell.zhi}(${cell.liuQin})",
                color = AppColors.Muted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                terms.forEach { term -> JExplainTermBlock(term) }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0x1AD9A441))
                .clickable(onClick = onDismiss).padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) { Text("知道了", color = AppColors.Lantern, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun JExplainTermBlock(term: JiangExplain.Term) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0FFFFFFF))
            .border(1.dp, AppColors.Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x1AD9A441))
                .padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(term.category, color = AppColors.Lantern, fontSize = 10.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(term.name, color = AppColors.Paper, fontSize = 15.sp, fontFamily = FontFamily.Serif)
        }
        Spacer(Modifier.height(6.dp))
        Text(term.desc, color = AppColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        if (term.fields.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            term.fields.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(k, color = AppColors.Lantern, fontSize = 11.5.sp,
                        modifier = Modifier.width(58.dp))
                    Text(v, color = AppColors.Paper, fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun JFieldLabel(text: String) {
    Text(text, color = AppColors.Muted, fontSize = 12.sp, letterSpacing = 2.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp))
}

@Composable
private fun JSegmented(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, label ->
            val on = i == selected
            Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                .background(if (on) Color(0x1AD9A441) else Color(0x10FFFFFF))
                .then(if (on) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(999.dp)) else Modifier)
                .clickable { onSelect(i) }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center) {
                Text(label, color = if (on) AppColors.Lantern else AppColors.Paper, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun JChipRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    val on = label == selected
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                        .background(if (on) Color(0x1AD9A441) else Color(0x10FFFFFF))
                        .clickable { onSelect(label) }.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, color = if (on) AppColors.Lantern else AppColors.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun JPrimeButton(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFFC0532B), Color(0xFFD4703E))))
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 17.sp, fontFamily = FontFamily.Serif, letterSpacing = 4.sp)
    }
}

@Composable
private fun JResultButton(text: String, gold: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(10.dp))
        .background(if (gold) Color(0x1AD9A441) else Color(0x10FFFFFF))
        .clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (gold) AppColors.Lantern else AppColors.Paper, fontSize = 11.5.sp, textAlign = TextAlign.Center)
    }
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("卦面", text))
}

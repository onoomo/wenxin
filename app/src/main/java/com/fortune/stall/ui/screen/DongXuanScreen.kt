package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.DongXuan
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.util.AiBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DX_GOLD = Color(0xFFD9A441)
private val DX_RED = Color(0xFFB0392E)
private val DX_INK = Color(0xFF222222)
private val DX_LINE = Color(0xFFCFCAC0)

/** 五行 → 原版配色（木绿 火红 土黄 金秋 水蓝） */
private fun dxWxColor(wx: String): Color = when (wx) {
    "木" -> Color(0xFF3F8A34)
    "火" -> Color(0xFFC0392B)
    "土" -> Color(0xFFB07D1E)
    "金" -> Color(0xFFB8912A)
    "水" -> Color(0xFF2E5C9E)
    else -> Color(0xFF8A8276)
}
/** 旺相休囚死配色（旺红 相黄 休囚死蓝） */
private fun dxWangColor(w: String): Color = when (w) {
    "旺" -> Color(0xFFC0392B)
    "相" -> Color(0xFFB07D1E)
    "休", "囚", "死" -> Color(0xFF2E5C9E)
    else -> Color(0xFF8A8276)
}
/** 小六壬六神配色（对齐原版：大安/小吉青绿 速喜红 赤口灰 空亡棕 留连黑） */
private fun dxShenColor(s: String): Color = when (s) {
    "大安", "小吉" -> Color(0xFF2E8B57)
    "速喜" -> Color(0xFFC0392B)
    "赤口" -> Color(0xFF9A9288)
    "空亡" -> Color(0xFF8A5A2B)
    else -> Color(0xFF222222)
}
/** 地支 → 二十八宿简称（对齐原版图2：丑室 卯房 巳鬼 未翼 酉昴 亥斗） */
private val DX_ZHI_XIU = mapOf(
    "子" to "虚", "丑" to "室", "寅" to "元", "卯" to "房",
    "辰" to "尾", "巳" to "鬼", "午" to "星", "未" to "翼",
    "申" to "奎", "酉" to "昴", "戌" to "崩", "亥" to "斗")
/** 宫地支 → 右上角天干小字（对齐原版图2，竖排两字：丑辛癸 卯癸乙 巳乙 未乙丁 酉丁己 亥己辛） */
private val DX_ZHI_CANG = mapOf(
    "子" to "癸", "丑" to "辛\n癸", "寅" to "甲", "卯" to "癸\n乙",
    "辰" to "戊", "巳" to "乙", "午" to "丁", "未" to "乙\n丁",
    "申" to "庚", "酉" to "丁\n己", "戌" to "戊", "亥" to "己\n辛")
/** 底部六兽小字（按 PALACES 顺序，对齐图2：大安青龙 留连朱雀 速喜勾陈 赤口腾蛇 小吉白虎 空亡玄武） */
private val DX_BEASTS = arrayOf("蟠泥 青龙", "安巢 朱雀", "禄印 勾陈", "入林 腾蛇", "临门 白虎", "伏藏 玄武")

/** 东玄 · 三数六宫排盘（起卦页 + 排盘页） */
@Composable
fun DongXuanScreen(db: FortuneDb, onBack: () -> Unit) {
    var chart by remember { mutableStateOf<DongXuan.Chart?>(null) }
    if (chart == null) {
        DongXuanForm(onBack = onBack) { ns, dt ->
            try {
                val c = DongXuan.chart(ns, dt)
                chart = c
                val now = LocalDateTime.now()
                val id = db.insert(
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "东玄 · ${c.numText}", "dongxuan", "三数起局",
                    "起局中…", c.faceText
                )
                db.updateFace(id, c.faceText)
            } catch (e: Exception) {
                // 起局失败不落账
            }
        }
    } else {
        DongXuanResult(chart!!, onBack = { chart = null })
    }
}

/* ============================ 起卦页（图1） ============================ */
@Composable
private fun DongXuanForm(onBack: () -> Unit, onCast: (List<Int>, LocalDateTime) -> Unit) {
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    var question by remember { mutableStateOf("") }
    var numText by remember { mutableStateOf("") }          // 单个输入框：三个数连写，如 458 / 4 5 8 / 4,5,8
    val numFocus = remember { FocusRequester() }            // 点击整框聚焦，修复输入法不弹出
    var selDt by remember { mutableStateOf(LocalDateTime.now()) }
    var showDt by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val solar = selDt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    val lunarNow = com.fortune.stall.util.LunarCalendar.fromDateTime(selDt)

    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).imePadding().padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            // 顶栏
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(72.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2330)).clickable { onBack() }
                    .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text("〈 返回", color = Color(0xFFD9D0BE), fontSize = 13.sp)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("东 玄 排 盘", color = Color(0xFFF0ECE0), fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                }
                Spacer(Modifier.width(72.dp))
            }
            Spacer(Modifier.height(18.dp))

            // 预测事项
            DXCard {
                DXCardTitle("预 测 事 项")
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0AFFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)) {
                    if (question.isEmpty()) Text("输入你所问之事…", color = Color(0xFF9A8E7A), fontSize = 13.sp)
                    BasicTextField(
                        value = question, onValueChange = { question = it.take(60) },
                        singleLine = false, maxLines = 3,
                        textStyle = TextStyle(color = Color(0xFFF0E8D8), fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 起卦方式 + 数字 + 时间
            DXCard {
                DXCardTitle("起 卦 方 式")
                Spacer(Modifier.height(12.dp))
                // 数字起卦（选中态），不可切换
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(DX_RED).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("数字起卦", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("公历时间", color = DX_GOLD, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("点击编辑", color = Color(0xFF9A8E7A), fontSize = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0AFFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(10.dp))
                    .clickable { showDt = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(solar, color = Color(0xFFF0E8D8), fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Text("✎", color = DX_GOLD, fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))

                // 单个数字输入框：三个数连写，如 458 / 4 5 8 / 4,5,8
                Text("报数（1-999，个数不限）", color = DX_GOLD, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0AFFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(10.dp))
                    .clickable {
                        numFocus.requestFocus()
                        scope.launch { delay(150); scrollState.animateScrollTo(scrollState.maxValue) }
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp)) {
                    if (numText.isEmpty()) Text("输入报数，如 11 或 4 5 8", color = Color(0xFF9A8E7A), fontSize = 15.sp)
                    BasicTextField(
                        value = numText,
                        onValueChange = { raw -> numText = raw.filter { it.isDigit() || it == ' ' || it == ',' }.take(12) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        textStyle = TextStyle(color = Color(0xFFF0E8D8), fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().focusRequester(numFocus)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("报数 1-999，个数不限；单个如 11，多个用空格 / 逗号分隔", color = Color(0xFF9A8E7A), fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0x14FFFFFF)).padding(12.dp)) {
                    Column {
                        Text("时间推算", color = DX_GOLD, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("农历 ${lunarNow.month}月${lunarNow.day}日 ${lunarNow.hourZhi}时",
                            color = Color(0xFFD9D0BE), fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("干支 ${lunarNow.yearGanZhi}年 ${lunarNow.monthGanZhi}月 ${lunarNow.dayGanZhi}日 ${lunarNow.hourGanZhi}时",
                            color = Color(0xFFD9D0BE), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            // 起卦排盘按钮
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFC1542E), Color(0xFFD2693E))))
                .clickable {
                    focusManager.clearFocus()
                    val nums = parseNumbers(numText)
                    if (nums == null) {
                        Toast.makeText(ctx, "请输入 1-999 的报数", Toast.LENGTH_SHORT).show()
                    } else {
                        onCast(nums, selDt)
                    }
                }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("起 卦 排 盘", color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            }
            Spacer(Modifier.height(30.dp))
        }
        if (showDt) {
            DXDateTimeDialog(
                initial = selDt,
                onDismiss = { showDt = false },
                onConfirm = { dt -> selDt = dt; showDt = false }
            )
        }
    }
}

/* ============================ 起局时间快捷编辑弹窗 ============================ */
@Composable
private fun DXDateTimeDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    var y by remember { mutableStateOf(initial.year) }
    var mo by remember { mutableStateOf(initial.monthValue) }
    var d by remember { mutableStateOf(initial.dayOfMonth) }
    var h by remember { mutableStateOf(initial.hour) }
    var mi by remember { mutableStateOf(initial.minute) }
    val now = LocalDateTime.now()
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF241E16))
            .border(1.dp, Color(0x52D9A441), RoundedCornerShape(16.dp))
            .padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("选择起局时间", color = DX_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(4.dp))
            Text("点击数字直接输入，或用 − / + 微调", color = Color(0xFF9A8E7A), fontSize = 10.sp)
            Spacer(Modifier.height(12.dp))

            // 快捷项
            Text("快捷", color = Color(0xFF9A8E7A), fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DXQuickChip("现在") { y = now.year; mo = now.monthValue; d = now.dayOfMonth; h = now.hour; mi = now.minute }
                DXQuickChip("今天") { h = now.hour; mi = now.minute }
                DXQuickChip("去年") { y = now.year - 1 }
                DXQuickChip("整点") { mi = 0 }
                listOf("1980", "1990", "2000", "2010", "2020").forEach { yr ->
                    DXQuickChip(yr) { y = yr.toInt() }
                }
            }
            Spacer(Modifier.height(8.dp))
            // 十二时辰快捷（子23 丑1 … 亥21）
            val hourMap = listOf("子" to 23, "丑" to 1, "寅" to 3, "卯" to 5, "辰" to 7, "巳" to 9,
                "午" to 11, "未" to 13, "申" to 15, "酉" to 17, "戌" to 19, "亥" to 21)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                hourMap.forEach { (zhi, hh) -> DXQuickChip("${zhi}时") { h = hh; mi = 0 } }
            }
            Spacer(Modifier.height(14.dp))

            DXNumStepper("年", y, 1900, 2100) { y = it }
            DXNumStepper("月", mo, 1, 12) { mo = it }
            DXNumStepper("日", d, 1, 31) { d = it }
            DXNumStepper("时", h, 0, 23) { h = it }
            DXNumStepper("分", mi, 0, 59) { mi = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x14FFFFFF))
                    .clickable(onClick = onDismiss).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = Color(0xFFB8AE9A), fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(DX_RED)
                    .clickable {
                        val dim = java.time.YearMonth.of(y, mo).lengthOfMonth()
                        onConfirm(LocalDateTime.of(y, mo, kotlin.math.min(d, dim), h, mi))
                    }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DXQuickChip(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0x14FFFFFF))
        .border(1.dp, Color(0x52D9A441), RoundedCornerShape(14.dp))
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = Color(0xFFD9D0BE), fontSize = 11.sp)
    }
}

/** 深色单行步进器：− [数字] +，点击中间数字可直接键盘输入。 */
@Composable
private fun DXNumStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var buf by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DX_GOLD, fontSize = 13.sp, modifier = Modifier.width(34.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF))
            .clickable { onChange((value - 1).coerceAtLeast(min)) }, contentAlignment = Alignment.Center) {
            Text("−", color = DX_GOLD, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x52D9A441), RoundedCornerShape(8.dp))
            .clickable { editing = true; buf = value.toString() }
            .padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            if (editing) {
                BasicTextField(
                    value = buf,
                    onValueChange = { raw ->
                        buf = raw.filter { it.isDigit() }.take(4)
                        val p = buf.toIntOrNull()
                        if (p != null && p in min..max) onChange(p)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color(0xFFF0E8D8), fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().focusRequester(fr)
                )
                LaunchedEffect(Unit) {
                    try { fr.requestFocus() } catch (_: Exception) {}
                }
            } else {
                Text(value.toString(), color = Color(0xFFF0E8D8), fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF))
            .clickable { onChange((value + 1).coerceAtMost(max)) }, contentAlignment = Alignment.Center) {
            Text("＋", color = DX_GOLD, fontSize = 18.sp)
        }
    }
}

/** 解析报数（个数不限，每个 1-999）：无分隔符时整体作为一个数（如「11」「458」）；有空格/逗号按其分隔为多个数。非法返回 null */
private fun parseNumbers(raw: String): List<Int>? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    val nums = if (t.contains(' ') || t.contains(',')) {
        t.split(' ', ',').filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: return null }
    } else {
        listOf(t.toIntOrNull() ?: return null)
    }
    if (nums.isEmpty() || nums.any { it !in 1..999 }) return null
    return nums
}

@Composable
private fun DXNumberBox(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9A8E7A), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color(0x0AFFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(10.dp))
            .padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            if (value.isEmpty()) Text("-", color = Color(0xFF6B6258), fontSize = 18.sp)
            BasicTextField(
                value = value,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }
                    val v = digits.toIntOrNull()
                    onChange(if (v != null && v in 1..10) v.toString() else "")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = Color(0xFFF0E8D8), fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DXCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Color(0x14FFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(16.dp))
        .padding(16.dp)) { content() }
}

@Composable
private fun DXCardTitle(text: String) {
    Text(text, color = DX_GOLD, fontSize = 15.sp, fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
}

/* ============================ 排盘页（图2） ============================ */
@Composable
private fun DongXuanResult(c: DongXuan.Chart, onBack: () -> Unit) {
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color(0xFFF4F2ED))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(16.dp))
            // 顶栏
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(72.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEDE8DC)).clickable { onBack() }
                    .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text("〈", color = Color(0xFF6B6258), fontSize = 18.sp)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("东 玄 排 盘", color = Color(0xFF3A2E12), fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
                Spacer(Modifier.width(72.dp))
            }
            Spacer(Modifier.height(14.dp))

            // 报数标题：◈ numText · 时支
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("◈", color = Color(0xFF3A2E12), fontSize = 30.sp)
                Spacer(Modifier.width(12.dp))
                Text("${c.numText} · ${c.timeZhi}", color = Color(0xFF222222), fontSize = 28.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))

            // 四值 / 旬空 信息条
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFECEAE3)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("四值", color = Color(0xFF2E4A7A), fontSize = 15.sp,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(52.dp))
                    Box(Modifier.width(1.dp).height(18.dp).background(Color(0xFF2E4A7A)))
                    Spacer(Modifier.width(10.dp))
                    Text(c.ganZhi, color = Color(0xFF19356B), fontSize = 16.sp,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("旬空", color = Color(0xFF2E4A7A), fontSize = 15.sp,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(52.dp))
                    Box(Modifier.width(1.dp).height(18.dp).background(Color(0xFF2E4A7A)))
                    Spacer(Modifier.width(10.dp))
                    Text("${c.yearKong} · ${c.monthKong} · ${c.dayKong} · ${c.hourKong}",
                        color = Color(0xFF19356B), fontSize = 15.sp, fontFamily = FontFamily.Serif)
                }
            }
            Spacer(Modifier.height(12.dp))

            // 六宫格
            DXGrid(c)
            Spacer(Modifier.height(16.dp))

            // 底部按钮
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE4DCCB)).clickable {
                        val cb = AiBridge.askAi
                        if (cb != null) cb(c.faceText) else Toast.makeText(ctx, "AI 通道未就绪", Toast.LENGTH_SHORT).show()
                    }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("问 AI 解读", color = Color(0xFF6B4E1E), fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE4DCCB)).clickable {
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("东玄卦面", c.faceText))
                        Toast.makeText(ctx, "卦面已复制", Toast.LENGTH_SHORT).show()
                    }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("复制卦面", color = Color(0xFF6B4E1E), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 六宫格：3列×2行，每宫多行密集信息，对齐参考图2 */
@Composable
private fun DXGrid(c: DongXuan.Chart) {
    val order = DongXuan.DISPLAY_ORDER
    Column(Modifier.fillMaxWidth().background(Color.White).border(1.dp, DX_LINE)) {
        for (r in 0..1) {
            if (r > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(DX_LINE))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                for (col in 0..2) {
                    if (col > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(DX_LINE))
                    DXCellCard(c.cells[order[r * 3 + col]], Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DXCellCard(cell: DongXuan.Cell, modifier: Modifier) {
    // 五行判定：取九星/纳音末字
    val starWx = cell.star.lastOrNull()?.toString() ?: ""
    val nayinWx = cell.nayin.lastOrNull()?.toString() ?: ""
    // 底部六兽：直接用引擎按阳盘/阴盘算好的完整串（别称 兽名），不再用写死的阴盘表
    val beast = cell.liuShen
    Column(modifier.heightIn(min = 186.dp).background(Color.White).padding(horizontal = 4.dp, vertical = 3.dp)) {
        // ① 顶行：八神（黑）+ 别称（灰）｜九星（五行色）
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(cell.god, color = DX_INK, fontSize = 10.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(2.dp))
            Text(cell.godAlias, color = Color(0xFF9A9288), fontSize = 8.sp,
                modifier = Modifier.weight(1f), maxLines = 1)
            Text(cell.star, color = dxWxColor(starWx), fontSize = 11.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold)
        }
        // ② 纳音（居中，五行色）+ 右上角干（竖排两字，完整显示）
        Box(Modifier.fillMaxWidth().heightIn(min = 40.dp)) {
            Text(cell.nayin, color = dxWxColor(nayinWx), fontSize = 11.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center), maxLines = 1)
            Text(DX_ZHI_CANG[cell.zhi] ?: "", color = DX_INK, fontSize = 11.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, lineHeight = 13.sp,
                modifier = Modifier.align(Alignment.TopEnd))
        }
        // ③ 旺衰（状态色，单独一行）
        Text(cell.wangshuai, color = dxWangColor(cell.wangshuai), fontSize = 10.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        // ④ 地支大字 + 宿字 + 对/身红标 ｜ 六亲
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(cell.zhi, color = DX_INK, fontSize = 24.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(1.dp))
            Text(DX_ZHI_XIU[cell.zhi] ?: "", color = Color(0xFF7E766C), fontSize = 9.sp,
                fontFamily = FontFamily.Serif)
            Spacer(Modifier.width(2.dp))
            if (cell.isDay) Text("对", color = Color(0xFFC0392B), fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            if (cell.isBody) Text("身", color = Color(0xFFC0392B), fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(cell.liuqin, color = DX_INK, fontSize = 15.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, maxLines = 1)
        }
        // ⑤ 长生（灰）
        Text(cell.changsheng, color = Color(0xFF9A9288), fontSize = 9.sp, fontFamily = FontFamily.Serif)
        // ⑥ 卦象（居中）
        Text(cell.gua, color = Color(0xFF4A4238), fontSize = 10.sp, fontFamily = FontFamily.Serif,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        // ⑦ 底行三列：左(地分小字在上/主事大字在下) ｜ 中(宫名彩色大字) ｜ 右(别称小字在上/兽名大字在下)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            // 左：地分（小，上）+ 主事（大，下）—— 移到宫名左边
            Column(Modifier.weight(1f)) {
                Text(cell.gan, color = Color(0xFF6B6258), fontSize = 9.sp, fontFamily = FontFamily.Serif, maxLines = 1)
                Text(cell.zhushi, color = Color(0xFF6B6258), fontSize = 13.sp,
                    fontFamily = FontFamily.Serif, maxLines = 1)
            }
            // 中：宫名彩色大字，居中
            Box(Modifier.weight(1.2f), contentAlignment = Alignment.BottomCenter) {
                Text(cell.palace, color = dxShenColor(cell.palace), fontSize = 17.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            }
            // 右：六兽别称（小，上）+ 兽名（大，下）—— 别称在兽名上方
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                val parts = beast.split(" ")
                Text(parts.getOrElse(0) { "" }, color = Color(0xFF9A9288), fontSize = 9.sp,
                    fontFamily = FontFamily.Serif, maxLines = 1)
                Text(parts.getOrElse(1) { "" }, color = Color(0xFF6B6258), fontSize = 13.sp,
                    fontFamily = FontFamily.Serif, maxLines = 1)
            }
        }
    }
}

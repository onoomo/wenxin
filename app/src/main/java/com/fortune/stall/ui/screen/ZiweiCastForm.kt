package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.fortune.stall.util.BirthPlaces
import com.fortune.stall.util.DateInputAlgo

/**
 * 紫微专用起卦页（1:1 对齐参考图1）：
 * 姓名 / 性别 / 日期输入(公历·农历·四柱) / 年·月·日 / 使用真太阳时
 * —— 勾选后才显示「精准时间(小时1-24/分钟快捷点击选择)」「出生地」；无流派行。
 * 四种日期口径统一由 DateInputAlgo 解析成公历时刻。
 */
@Composable
fun ZiweiCastForm(onBack: () -> Unit, onCast: (JiMingpanFormResult) -> Unit) {
    val ctx = LocalContext.current
    val C = ZwFormTheme
    val now = remember { LocalDate.now() }

    var name by remember { mutableStateOf("") }
    var isMan by remember { mutableStateOf(true) }
    var calendar by remember { mutableStateOf(0) } // 0 公历 / 1 农历 / 2 四柱
    var yearText by remember { mutableStateOf(now.year.toString()) }
    var monthText by remember { mutableStateOf(now.monthValue.toString()) }
    var dayText by remember { mutableStateOf(now.dayOfMonth.toString()) }
    var hourText by remember { mutableStateOf(LocalDateTime.now().hour.toString()) } // 精准小时（1-24，24 视为 0 点/晚子）
    var minuteText by remember { mutableStateOf(LocalDateTime.now().minute.toString()) } // 精准分钟（0-59）
    var showHour by remember { mutableStateOf(false) }   // 小时快捷选择弹窗
    var showMinute by remember { mutableStateOf(false) } // 分钟快捷选择弹窗
    // 四柱模式四个干支框
    var gzY by remember { mutableStateOf("") }
    var gzM by remember { mutableStateOf("") }
    var gzD by remember { mutableStateOf("") }
    var gzH by remember { mutableStateOf("") }
    var trueSolar by remember { mutableStateOf(false) } // 默认不勾选：不显示精准时间/出生地
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var showProv by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }
    var showDist by remember { mutableStateOf(false) }

    val cityList = BirthPlaces.PROVINCES.firstOrNull { it.name == province }?.cities ?: emptyList()
    val districtList = cityList.firstOrNull { it.name == city }?.districts ?: emptyList()
    val autoLon = if (province.isNotBlank() && city.isNotBlank())
        BirthPlaces.lonOf(province, city, district) else null
    val placeLabel = listOf(province, city, district).filter { it.isNotBlank() }.joinToString("·")

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Transparent).clickable { onBack() })
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(C.BG)
                .border(1.dp, C.LINE, RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                // 标题栏
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("紫 微 斗 数", color = C.GOLD, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("（文墨天机）", color = C.SUB, fontSize = 11.sp)
                    }
                    Box(Modifier.size(30.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                        Text("×", color = C.SUB, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))

                // 姓名（图1：仅输入框，无"福主"字、无不存开关）
                ZwFieldLabel("姓名")
                FormInput(value = name, onValue = { name = it }, placeholder = "请输入姓名")

                Spacer(Modifier.height(14.dp))
                // 性别
                ZwFieldLabel("性别")
                JiSegThemed(listOf("男", "女"), if (isMan) 0 else 1,
                    onSelect = { isMan = it == 0 }, accent = C.RED,
                    unselectedBg = C.PANEL, border = C.LINE, textColor = C.TEXT)

                Spacer(Modifier.height(14.dp))
                // 日期输入（公历 / 农历 / 四柱）
                ZwFieldLabel("日期输入")
                JiSegThemed(listOf("公历", "农历", "四柱"), calendar,
                    onSelect = { calendar = it }, accent = C.RED,
                    unselectedBg = C.PANEL, border = C.LINE, textColor = C.TEXT)

                Spacer(Modifier.height(12.dp))
                if (calendar < 2) {
                    // 公历 / 农历：年 / 月 / 日 三框 + 精准时间
                    Row(Modifier.fillMaxWidth()) {
                        FormInput(yearText, { yearText = it }, "2000", "年", Modifier.weight(1.2f), KeyboardType.Number)
                        Spacer(Modifier.width(8.dp))
                        FormInput(monthText, { monthText = it }, "1-12", "月", Modifier.weight(1f), KeyboardType.Number)
                        Spacer(Modifier.width(8.dp))
                        FormInput(dayText, { dayText = it }, "1-31", "日", Modifier.weight(1f), KeyboardType.Number)
                    }
                } else {
                    // 四柱：年柱 / 月柱 / 日柱 / 时柱
                    Row(Modifier.fillMaxWidth()) {
                        FormInput(gzY, { gzY = it }, "年柱", "年柱", Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        FormInput(gzM, { gzM = it }, "月柱", "月柱", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        FormInput(gzD, { gzD = it }, "日柱", "日柱", Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        FormInput(gzH, { gzH = it }, "时柱", "时柱", Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(12.dp))
                // 使用真太阳时（勾选后，精准时间和出生地才显示）
                Row(Modifier.fillMaxWidth().clickable { trueSolar = !trueSolar },
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (trueSolar) C.RED else Color.Transparent)
                        .border(1.dp, if (trueSolar) C.RED else C.LINE, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center) {
                        if (trueSolar) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(7.dp))
                    Text("使用真太阳时（需精准时分和出生地）", color = C.TEXT, fontSize = 13.sp)
                }

                // 勾选真太阳时后：精准时间(小时 1-24 + 分钟) + 出生地 才显示
                if (trueSolar && calendar < 2) {
                    Spacer(Modifier.height(12.dp))
                    ZwFieldLabel("精准时间（24 小时制，点击选择）")
                    Row(Modifier.fillMaxWidth()) {
                        // 小时（1-24）：点击弹出快捷选择
                        Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp))
                            .background(C.PANEL).border(1.dp, C.LINE, RoundedCornerShape(6.dp))
                            .clickable { showHour = true }
                            .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${hourText.trim().ifBlank { "0" }} 时", color = C.TEXT, fontSize = 13.sp,
                                modifier = Modifier.weight(1f))
                            Text("▾", color = C.GOLD, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        // 分钟（0-59）：点击弹出快捷选择
                        Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp))
                            .background(C.PANEL).border(1.dp, C.LINE, RoundedCornerShape(6.dp))
                            .clickable { showMinute = true }
                            .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${minuteText.trim().ifBlank { "0" }} 分", color = C.TEXT, fontSize = 13.sp,
                                modifier = Modifier.weight(1f))
                            Text("▾", color = C.GOLD, fontSize = 12.sp)
                        }
                    }
                }

                if (trueSolar) {
                    Spacer(Modifier.height(12.dp))
                    // 出生地
                    ZwFieldLabel("出生地")
                    Row(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp))
                        .background(C.PANEL).border(1.dp, C.LINE, RoundedCornerShape(6.dp))
                        .clickable { showProv = true }
                        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (placeLabel.isBlank()) "请选择出生地" else placeLabel,
                            color = if (placeLabel.isBlank()) C.SUB else C.TEXT, fontSize = 13.sp,
                            modifier = Modifier.weight(1f))
                        Text("选择", color = C.GOLD, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(18.dp))
                // 排盘按钮
                Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(24.dp))
                    .background(C.RED).clickable {
                        // 勾选真太阳时必须先选出生地：无经度则无法校正，拦截不排盘
                        if (trueSolar && autoLon == null) {
                            Toast.makeText(ctx, "请选择出生地后再排盘", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        // 精准小时(1-24) + 分钟：勾选真太阳时用所填时分，否则用当前时刻
                        // 24 点按 0 点（次日）处理，与 24 小时制/晚子时一致
                        val hhRaw = hourText.trim().toIntOrNull() ?: LocalDateTime.now().hour
                        val hh = (hhRaw.coerceIn(1, 24) % 24)
                        val mm = minuteText.trim().toIntOrNull()?.coerceIn(0, 59) ?: 0
                        val parsed = when (calendar) {
                            0 -> DateInputAlgo.parseSolar(
                                yearText.trim().toIntOrNull() ?: now.year,
                                monthText.trim().toIntOrNull() ?: 1,
                                dayText.trim().toIntOrNull() ?: 1, hh, mm)
                            1 -> DateInputAlgo.parseLunar(
                                yearText.trim().toIntOrNull() ?: now.year,
                                monthText.trim().toIntOrNull() ?: 1,
                                dayText.trim().toIntOrNull() ?: 1, hh, mm)
                            else -> DateInputAlgo.parseFourPillars(
                                gzY.trim(), gzM.trim(), gzD.trim(), gzH.trim())
                        }
                        if (parsed.error != null || parsed.dateTime == null) {
                            Toast.makeText(ctx, parsed.error ?: "日期无效", Toast.LENGTH_SHORT).show()
                        } else {
                            onCast(JiMingpanFormResult(
                                name = name, isMan = isMan, calendar = calendar,
                                dateTime = parsed.dateTime, earlyLateZi = false,
                                trueSolar = trueSolar, summerTime = false, noSave = false,
                                longitude = if (trueSolar) autoLon else null))
                        }
                    }, contentAlignment = Alignment.Center) {
                    Text("排 盘", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // 小时 1-24 快捷选择（24 表示次日 0 点/晚子）
        if (showHour) JiOptionDialog("选择小时（24 小时制）", (1..24).map { "$it" }, true,
            { showHour = false }) { sel -> hourText = sel; showHour = false }
        // 分钟 0-59 快捷选择
        if (showMinute) JiOptionDialog("选择分钟", (0..59).map { "$it" }, true,
            { showMinute = false }) { sel -> minuteText = sel; showMinute = false }

        if (showProv) JiOptionDialog("选择省份", BirthPlaces.PROVINCES.map { it.name }, true,
            { showProv = false }) { p -> province = p; city = ""; district = ""; showProv = false; showCity = true }
        if (showCity) JiOptionDialog("选择城市", cityList.map { it.name }, true,
            { showCity = false }) { c ->
            city = c; district = ""
            val hasDist = cityList.firstOrNull { it.name == c }?.districts?.isNotEmpty() == true
            showCity = false
            if (hasDist) showDist = true
        }
        if (showDist) JiOptionDialog("选择区县", districtList.map { it.name }, true,
            { showDist = false }) { g -> district = g; showDist = false }
    }
}

@Composable
private fun ZwFieldLabel(text: String) {
    Text(text, color = ZwFormTheme.SUB, fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun FormInput(
    value: String, onValue: (String) -> Unit, placeholder: String,
    label: String? = null, modifier: Modifier = Modifier, keyboard: KeyboardType = KeyboardType.Text
) {
    Box(modifier.height(40.dp).clip(RoundedCornerShape(6.dp))
        .background(ZwFormTheme.PANEL).border(1.dp, ZwFormTheme.LINE, RoundedCornerShape(6.dp))
        .padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
        BasicTextField(value = value, onValueChange = onValue, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            textStyle = TextStyle(fontSize = 13.sp, color = ZwFormTheme.TEXT),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = ZwFormTheme.SUB, fontSize = 12.sp)
                else inner()
            })
        if (label != null && value.isNotEmpty()) {
            Text(label, color = ZwFormTheme.SUB, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fortune.stall.ui.components.Starfield
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 原版弹窗表单配色（ji.js.cn） */
internal object JiForm {
    val RED = Color(0xFFC0392B)
    val RED_DARK = Color(0xFFA93226)
    val WHITE = Color(0xFFFFFFFF)
    val TEXT = Color(0xFF333333)
    val SUB = Color(0xFF888888)
    val LINE = Color(0xFFE6E6E6)
    val INPUT_BG = Color(0xFFFAFAFA)
    val SELECT_BG = Color(0xFFF5F5F5)
}

/** 命盘类原版起卦表单结果 */
data class JiMingpanFormResult(
    val name: String,
    val isMan: Boolean,
    val calendar: Int,          // 0 公历 / 1 农历 / 2 四柱
    val dateTime: LocalDateTime,
    val earlyLateZi: Boolean,   // 早晚子时：是=true
    val trueSolar: Boolean,     // 真太阳时：是=true
    val summerTime: Boolean,    // 夏令时：开=true
    val noSave: Boolean,         // 不存开关：开=true 表示不存档
    val longitude: Double? = null // 出生地经度（真太阳时校正用，东经为正）
)

/**
 * 原版 ji.js.cn 命盘类起卦表单（八字/紫微/命理奇门/阴盘奇门共用）。
 * 白底弹窗：福主姓名+不存、男女、公历/农历/四柱、日期、早晚子时、真太阳时+夏令时、红色排盘按钮。
 */
@Composable
fun JiMingpanCastForm(
    title: String,
    onBack: () -> Unit,
    showLongitude: Boolean = false,
    dark: Boolean = false,
    onCast: (JiMingpanFormResult) -> Unit
) {
    // 紫微深金主题色板；其它模块沿用原版白底 JiForm
    val C = if (dark) ZwFormTheme else null
    val cText = C?.TEXT ?: JiForm.TEXT
    val cSub = C?.SUB ?: JiForm.SUB
    val cLine = C?.LINE ?: JiForm.LINE
    val cInputBg = C?.PANEL ?: JiForm.INPUT_BG
    val cSelectBg = C?.PANEL ?: JiForm.SELECT_BG
    val cRed = C?.RED ?: JiForm.RED
    val cBg = C?.BG ?: JiForm.WHITE
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var name by remember { mutableStateOf("") }
    var noSave by remember { mutableStateOf(false) }
    var isMan by remember { mutableStateOf(true) }
    var calendar by remember { mutableStateOf(0) }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var earlyLateZi by remember { mutableStateOf(false) }
    var trueSolar by remember { mutableStateOf(false) }
    var summerTime by remember { mutableStateOf(true) }
    var longitudeText by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showProv by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }
    val cityList = com.fortune.stall.util.BirthPlaces.PROVINCES.firstOrNull { it.name == province }?.cities ?: emptyList()
    // 选中城市后自动带出经度；手填经度仍允许覆盖
    val autoLon = if (province.isNotBlank() && city.isNotBlank())
        com.fortune.stall.util.BirthPlaces.lonOf(province, city) else null
    val lon: Double? = longitudeText.trim().toDoubleOrNull() ?: autoLon
    val lonOffsetMin = lon?.let { ((it - 120.0) * 4).let { v -> if (v >= 0) "+${v.toInt()}分" else "${v.toInt()}分" } } ?: ""
    Box(Modifier.fillMaxSize()) {
        // 弹窗上方黑色区域改为透明背景，点击即退出
        Box(Modifier.fillMaxSize().background(Color.Transparent).clickable { onBack() })

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(cBg)
                .border(1.dp, if (dark) cLine else Color.Transparent, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                // 标题栏
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = if (dark) cRed else cText, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.size(30.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                        Text("×", color = cSub, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 福主姓名 + 不存开关
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("福主姓名", color = cText, fontSize = 14.sp, modifier = Modifier.width(72.dp))
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(6.dp))
                        .background(cInputBg).border(1.dp, cLine, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(value = name, onValueChange = { name = it }, singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = cText),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner -> if (name.isEmpty()) Text("如需保存，请输入姓名", color = cSub, fontSize = 12.sp) else inner() })
                    }
                    Spacer(Modifier.width(10.dp))
                    JiSwitch(checked = noSave, onChecked = { noSave = it }, label = "不存", accent = cRed, trackOff = cLine, labelColor = cSub)
                }
                Spacer(Modifier.height(14.dp))

                // 男/女 + 公历/农历/四柱
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(72.dp))
                    JiSegThemed(listOf("男", "女"), if (isMan) 0 else 1, onSelect = { isMan = it == 0 },
                        accent = cRed, unselectedBg = cSelectBg, border = cLine, textColor = cText)
                    Spacer(Modifier.weight(1f))
                    JiSegThemed(listOf("公历", "农历", "四柱"), calendar, onSelect = { calendar = it },
                        accent = cRed, unselectedBg = cSelectBg, border = cLine, textColor = cText)
                }
                Spacer(Modifier.height(14.dp))

                // 日期
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("日期", color = cText, fontSize = 14.sp, modifier = Modifier.width(72.dp))
                    Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp))
                        .background(cInputBg).border(1.dp, cRed, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(dtText, color = cText, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f).clickable { showDate = true })
                        Text("▼", color = cSub, fontSize = 12.sp, modifier = Modifier.clickable { showDate = true })
                    }
                }
                Spacer(Modifier.height(14.dp))

                // 早晚子时
                JiRadioLineThemed(label = "早晚子时", selected = if (earlyLateZi) 1 else 0, options = listOf("是", "否"),
                    onSelect = { earlyLateZi = it == 0 }, accent = cRed, textColor = cText)
                Spacer(Modifier.height(12.dp))

                // 真太阳时 + 夏令时
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("真太阳时", color = cText, fontSize = 14.sp, modifier = Modifier.width(72.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf("是", "否").forEachIndexed { i, t ->
                            val sel = (i == 0) == trueSolar
                            Row(Modifier.clickable { trueSolar = i == 0 }.padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                JiRadioDot(sel, accent = cRed, border = cLine)
                                Spacer(Modifier.width(5.dp))
                                Text(t, color = if (sel) cRed else cText, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    JiSwitch(checked = summerTime, onChecked = { summerTime = it }, label = "夏令时", accent = cRed, trackOff = cLine, labelColor = cSub)
                }
                Spacer(Modifier.height(if (showLongitude) 10.dp else 18.dp))

                // 出生地（真太阳时校正）—— 仅紫微等需要的模块显示
                if (showLongitude) {
                    PlacePicker(
                        province = province, city = city,
                        provinceLabel = "省份", cityLabel = "城市",
                        cText = cText, cSub = cSub, cLine = cLine, cInputBg = cInputBg, cRed = cRed,
                        onProvinceClick = { showProv = true },
                        onCityClick = { if (province.isNotBlank()) showCity = true }
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("经度", color = cText, fontSize = 13.sp, modifier = Modifier.width(72.dp))
                        Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(6.dp))
                            .background(cInputBg).border(1.dp, cLine, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                            BasicTextField(value = longitudeText, onValueChange = { longitudeText = it }, singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp, color = cText),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (longitudeText.isEmpty() && autoLon == null)
                                        Text("选城市自动带出，也可手填（如 116.4）", color = cSub, fontSize = 10.sp)
                                    else if (longitudeText.isEmpty() && autoLon != null)
                                        Text("${autoLon}°E  $lonOffsetMin  $province·$city（可手填覆盖）", color = cText, fontSize = 11.sp)
                                    else inner()
                                })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 排盘按钮
                Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(24.dp))
                    .background(cRed)
                    .clickable {
                        val dt = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() }
                        onCast(JiMingpanFormResult(name, isMan, calendar, dt, earlyLateZi, trueSolar, summerTime, noSave, lon))
                    }, contentAlignment = Alignment.Center) {
                    Text("排 盘", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (dark) {
            if (showDate) ZwDateTimeDialog(
                initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
                onDismiss = { showDate = false },
                onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false }
            )
        } else {
            if (showDate) BoardDateDialog(
                initial = try { LocalDate.parse(dtText.substring(0, 10)) } catch (e: Exception) { LocalDate.now() },
                onDismiss = { showDate = false },
                onConfirm = { d -> val t = dtText.substring(11).ifEmpty { "00:00" }
                    dtText = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"; showDate = false })
        }
        // 省份选择
        if (showProv) JiOptionDialog(
            title = "选择省份", options = com.fortune.stall.util.BirthPlaces.PROVINCES.map { it.name },
            dark = dark, onDismiss = { showProv = false }, onSelect = { p ->
                province = p; city = ""; longitudeText = ""; showProv = false; showCity = true
            })
        // 城市选择
        if (showCity) JiOptionDialog(
            title = "选择城市", options = cityList.map { it.name },
            dark = dark, onDismiss = { showCity = false }, onSelect = { c ->
                city = c; longitudeText = ""; showCity = false
            })
    }
}

/** 省份/城市两栏选择入口 */
@Composable
private fun PlacePicker(
    province: String, city: String,
    provinceLabel: String, cityLabel: String,
    cText: Color, cSub: Color, cLine: Color, cInputBg: Color, cRed: Color,
    onProvinceClick: () -> Unit, onCityClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("出生地", color = cText, fontSize = 14.sp, modifier = Modifier.width(72.dp))
        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(6.dp))
            .background(cInputBg).border(1.dp, cLine, RoundedCornerShape(6.dp))
            .clickable(onClick = onProvinceClick)
            .padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text(if (province.isBlank()) provinceLabel else province,
                color = if (province.isBlank()) cSub else cText, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(6.dp))
            .background(cInputBg).border(1.dp, cLine, RoundedCornerShape(6.dp))
            .clickable(onClick = onCityClick)
            .padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text(if (city.isBlank()) cityLabel else city,
                color = if (city.isBlank()) cSub else cText, fontSize = 12.sp)
        }
    }
}

/** 通用单列选项弹窗（省市选择复用） */
@Composable
private fun JiOptionDialog(
    title: String, options: List<String>, dark: Boolean,
    onDismiss: () -> Unit, onSelect: (String) -> Unit
) {
    val bg = if (dark) ZwFormTheme.BG else JiForm.WHITE
    val textC = if (dark) ZwFormTheme.TEXT else JiForm.TEXT
    val goldC = if (dark) ZwFormTheme.GOLD else JiForm.RED
    val lineC = if (dark) ZwFormTheme.LINE else JiForm.LINE
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(bg)
            .border(1.dp, lineC, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Text(title, color = goldC, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(options) { opt ->
                    Text(opt, color = textC, fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(opt) }.padding(12.dp, 9.dp))
                }
            }
        }
    }
}


@Composable
internal fun JiSwitch(
    checked: Boolean, onChecked: (Boolean) -> Unit, label: String,
    accent: Color = JiForm.RED, trackOff: Color = JiForm.LINE, labelColor: Color = JiForm.SUB
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(38.dp).height(22.dp).clip(RoundedCornerShape(11.dp))
            .background(if (checked) accent else trackOff)
            .clickable { onChecked(!checked) }.padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(50)).background(Color.White))
        }
        Spacer(Modifier.width(5.dp))
        Text(label, color = labelColor, fontSize = 12.sp)
    }
}

@Composable
internal fun JiSeg(options: List<String>, selected: Int, onSelect: (Int) -> Unit) =
    JiSegThemed(options, selected, onSelect, JiForm.RED, JiForm.SELECT_BG, JiForm.LINE, JiForm.TEXT)

@Composable
internal fun JiSegThemed(
    options: List<String>, selected: Int, onSelect: (Int) -> Unit,
    accent: Color, unselectedBg: Color, border: Color, textColor: Color
) {
    Row(Modifier.clip(RoundedCornerShape(16.dp)).background(unselectedBg).border(1.dp, border, RoundedCornerShape(16.dp))) {
        options.forEachIndexed { i, t ->
            val sel = i == selected
            Box(Modifier.clip(RoundedCornerShape(16.dp))
                .background(if (sel) accent else Color.Transparent)
                .clickable { onSelect(i) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(t, color = if (sel) Color.White else textColor, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun JiRadioLine(
    label: String, selected: Int, options: List<String>, onSelect: (Int) -> Unit
) = JiRadioLineThemed(label, selected, options, onSelect, JiForm.RED, JiForm.TEXT)

@Composable
internal fun JiRadioLineThemed(
    label: String, selected: Int, options: List<String>, onSelect: (Int) -> Unit,
    accent: Color, textColor: Color
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = textColor, fontSize = 14.sp, modifier = Modifier.width(72.dp))
        options.forEachIndexed { i, t ->
            val sel = i == selected
            Row(Modifier.clickable { onSelect(i) }.padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                JiRadioDot(sel, accent = accent, border = JiForm.LINE)
                Spacer(Modifier.width(5.dp))
                Text(t, color = if (sel) accent else textColor, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun JiRadioDot(selected: Boolean, accent: Color = JiForm.RED, border: Color = JiForm.LINE) {
    Box(Modifier.size(16.dp).clip(RoundedCornerShape(50))
        .border(1.dp, if (selected) accent else border, RoundedCornerShape(50))
        .padding(3.dp), contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(accent))
    }
}

/** 白底弹窗骨架（供非命盘类模块复用）：红色顶栏标题 + 白底内容 + 红色按钮 */
@Composable
fun JiDialogShell(
    title: String,
    onBack: () -> Unit,
    onCast: () -> Unit,
    buttonText: String = "排 盘",
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // 弹窗上方黑色区域改为透明背景，点击即退出
        Box(Modifier.fillMaxSize().background(Color.Transparent).clickable { onBack() })
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(JiForm.WHITE)
            .padding(horizontal = 18.dp, vertical = 14.dp)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = JiForm.TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.size(30.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                        Text("×", color = JiForm.SUB, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                content()
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(24.dp))
                    .background(JiForm.RED).clickable(onClick = onCast), contentAlignment = Alignment.Center) {
                    Text(buttonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ============ 白底表单通用辅助组件（供各术数模块复用） ============

@Composable
internal fun JiRow(label: String, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = JiForm.TEXT, fontSize = 13.sp, modifier = Modifier.width(76.dp))
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
internal fun JiInput(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false
) {
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(JiForm.INPUT_BG)
        .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp))
        .padding(horizontal = 10.dp, vertical = 9.dp), contentAlignment = Alignment.CenterStart) {
        BasicTextField(value = value, onValueChange = onValue, singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = JiForm.TEXT,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = JiForm.SUB, fontSize = 12.sp) else inner() })
    }
}

@Composable
internal fun JiDateInput(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(6.dp)).background(JiForm.INPUT_BG)
        .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp))
        .padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = JiForm.TEXT, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f).clickable(onClick = onClick))
        Text("▼", color = JiForm.SUB, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
internal fun JiChipLine(label: String, items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = JiForm.TEXT, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { i, t ->
                val sel = i == selected
                Box(Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (sel) JiForm.RED else JiForm.SELECT_BG)
                    .border(1.dp, if (sel) JiForm.RED else JiForm.LINE, RoundedCornerShape(6.dp))
                    .clickable { onSelect(i) }.padding(horizontal = 11.dp, vertical = 7.dp)) {
                    Text(t, color = if (sel) Color.White else JiForm.TEXT, fontSize = 12.sp)
                }
            }
        }
    }
}

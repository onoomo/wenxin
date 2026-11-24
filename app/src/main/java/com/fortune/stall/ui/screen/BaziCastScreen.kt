package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.BaziCascade
import com.fortune.stall.engine.jishiyu.BaziChuangong
import com.fortune.stall.engine.jishiyu.BaziChart
import com.fortune.stall.engine.jishiyu.BaziFull
import com.fortune.stall.engine.jishiyu.BaziXiangpan
import com.fortune.stall.engine.jishiyu.JishiyuBase
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.BirthPlaces
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val BZ_RED = Color(0xFFB0392E)
private val BZ_GOLD = Color(0xFFD9A441)
private val BZ_LINE = Color(0x52D9A441)
private val BZ_CARD = Color(0x14FFFFFF)

private val BZ_GAN = "甲乙丙丁戊己庚辛壬癸"
private val BZ_ZHI = "子丑寅卯辰巳午未申酉戌亥"
private val BZ_ZHI_WX = mapOf("子" to "水", "丑" to "土", "寅" to "木", "卯" to "木", "辰" to "土", "巳" to "火",
    "午" to "火", "未" to "土", "申" to "金", "酉" to "金", "戌" to "土", "亥" to "水")
// 神煞简称 → 全称（基本信息页神煞区）
private val BZ_SHEN_FULL = mapOf(
    "天乙" to "天乙贵人", "太极" to "太极贵人", "文昌" to "文昌贵人", "福星" to "福星贵人",
    "天德" to "天德贵人", "月德" to "月德贵人", "天德合" to "天德合", "月德合" to "月德合",
    "国印" to "国印贵人", "学堂" to "学堂", "词馆" to "词馆", "驿马" to "驿马", "华盖" to "华盖",
    "将星" to "将星", "禄神" to "禄神", "羊刃" to "羊刃", "劫煞" to "劫煞", "亡神" to "亡神",
    "桃花" to "桃花", "红鸾" to "红鸾", "天喜" to "天喜", "孤辰" to "孤辰", "寡宿" to "寡宿",
    "空亡" to "空亡", "魁罡" to "魁罡", "金舆" to "金舆", "天医" to "天医", "天赦" to "天赦")
// 十二长生状态解读（基本信息页日支长生）
private val BZ_CHANGSHENG_DESC = mapOf(
    "长生" to "万物始生，生机勃发", "沐浴" to "沐浴更衣，初显锋芒", "冠带" to "冠带加身，渐入佳境",
    "临官" to "临官任职，精力充沛", "帝旺" to "帝旺至极，气势最盛", "衰" to "由盛转衰，渐趋平和",
    "病" to "如病需养，宜守不宜进", "死" to "万物寂静，蓄势待发", "墓" to "入墓收藏，厚积薄发",
    "绝" to "绝处逢生，否极泰来", "胎" to "胎息孕育，新的开始", "养" to "休养生息，涵养待时")

/**
 * 八字排盘专用起卦页（图1风格）：姓名 / 公历农历 / 出生日期时间 / 时区校正 / 出生地区 / 性别 / 同事。
 * 排盘后进入完整命盘详情页并自动存档。
 */
@Composable
fun BaziCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var chart by remember { mutableStateOf<BaziChart.Chart?>(null) }
    var fullData by remember { mutableStateOf<BaziFull.Data?>(null) }
    var xpData by remember { mutableStateOf<BaziXiangpan.Data?>(null) }
    if (chart == null) {
        BaziPaipanForm(onBack = onBack) { dt, isMan, name, question, noSave ->
            try {
                val c = BaziChart.build(dt, isMan)
                val fd = BaziFull.build(dt, isMan, c)
                fullData = fd
                xpData = BaziXiangpan.build(dt, isMan, c, fd)
                chart = c
                val q = name.ifBlank { "${dt.year}年出生 · 八字排盘" }
                val createdAt = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                if (!noSave) {
                    val id = db.insert(createdAt, q, "bazi", if (isMan) "男命" else "女命", "排盘中…", c.baziText)
                    db.updateFace(id, baziFaceText(c, q, createdAt))
                    Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    chart?.let { c -> BaziDetailScreen(chart = c, full = fullData, xp = xpData, onBack = { chart = null }) }
}

/* ============================ 图1风格：八字排盘起卦表单 ============================ */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BaziPaipanForm(
    onBack: () -> Unit,
    onCast: (dt: LocalDateTime, isMan: Boolean, name: String, question: String, noSave: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isLunar by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hour by remember { mutableStateOf(20) }
    var minute by remember { mutableStateOf(43) }
    var isMan by remember { mutableStateOf(true) }
    var question by remember { mutableStateOf("") }
    var solarTrue by remember { mutableStateOf(false) }   // 真太阳时校正
    var earlyZi by remember { mutableStateOf(false) }      // 早晚子时
    var summerTime by remember { mutableStateOf(false) }   // 夏令时
    var provinceIdx by remember { mutableIntStateOf(0) }
    var cityIdx by remember { mutableIntStateOf(0) }
    var districtIdx by remember { mutableIntStateOf(0) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showArea by remember { mutableStateOf(false) }
    var showReverse by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var noSave by remember { mutableStateOf(false) }

    val provinces = BirthPlaces.PROVINCES
    val province = provinces.getOrNull(provinceIdx)
    val city = province?.cities?.getOrNull(cityIdx)
    val district = city?.districts?.getOrNull(districtIdx)
    val areaName = when {
        district != null -> "${province!!.name}${city!!.name}${district.name}"
        city != null -> "${province!!.name}${city.name}"
        province != null -> province.name
        else -> ""
    }
    val lon = district?.lon ?: city?.lon ?: province?.lon ?: 116.4053

    fun correctedDt(): LocalDateTime {
        var dt = date.atTime(hour, minute)
        if (summerTime) dt = dt.minusHours(1)          // 夏令时拨回一小时
        if (solarTrue) {                                // 真太阳时：经度每差1度=4分钟
            val offsetMin = ((lon - 120.0) * 4.0).toLong()
            dt = dt.plusMinutes(offsetMin)
        }
        return dt
    }

    Box(Modifier.fillMaxSize()) {
        com.fortune.stall.ui.components.Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            BzNavBar("八 字 排 盘", onBack)
            Spacer(Modifier.height(18.dp))

            // 基本信息
            BzFormCard {
                BzFormTitle("基 本 信 息")
                Spacer(Modifier.height(12.dp))
                BzInput(name, "姓名（选填）") { name = it.take(20) }
            }
            Spacer(Modifier.height(12.dp))

            // 出生时间
            BzFormCard {
                BzFormTitle("出 生 时 间")
                Spacer(Modifier.height(12.dp))
                // 公历/农历切换
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("公历", "农历").forEachIndexed { i, label ->
                        val sel = (i == 0) == !isLunar
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (sel) BZ_RED else Color(0xFFF0EBE0))
                            .border(1.dp, if (sel) BZ_RED else Color(0x33B89B6A), RoundedCornerShape(10.dp))
                            .clickable { isLunar = (i == 1) }.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center) {
                            Text(label, color = if (sel) Color.White else Color(0xFF6B6258), fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                BzFormField("出生日期", if (isLunar) "${date.year}年${date.monthValue}月${date.dayOfMonth}日（农历）" else "${date.year}年${date.monthValue}月${date.dayOfMonth}日") { showDate = true }
                Spacer(Modifier.height(10.dp))
                BzFormField("出生时间", String.format("%02d时%02d分", hour, minute)) { showTime = true }

                Spacer(Modifier.height(14.dp))
                Text("时区校正", color = BZ_GOLD, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("真太阳时", solarTrue) { solarTrue = !solarTrue },
                        Triple("早晚子时", earlyZi) { earlyZi = !earlyZi },
                        Triple("夏令时", summerTime) { summerTime = !summerTime }
                    ).forEach { (label, on, toggle) ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                            .background(if (on) Color(0x33B0392E) else Color(0xFFF0EBE0))
                            .border(1.dp, if (on) BZ_RED else Color(0x33B89B6A), RoundedCornerShape(9.dp))
                            .clickable { toggle() }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center) {
                            Text(label, color = if (on) BZ_RED else Color(0xFF6B6258), fontSize = 11.5.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("真太阳时按出生地经度校正（东经120°为准，每度4分钟）", color = Color(0xFF9A8E7A), fontSize = 10.sp)

                Spacer(Modifier.height(12.dp))
                BzFormField("出生地区", areaName.ifBlank { "请选择出生地区" }) { showArea = true }

                Spacer(Modifier.height(14.dp))
                Text("性别", color = BZ_GOLD, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("男", "女").forEachIndexed { i, label ->
                        val sel = isMan == (i == 0)
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Color(0xFFD9B25A) else Color(0xFFF0EBE0))
                            .border(1.dp, if (sel) Color(0xFFB8912F) else Color(0x33B89B6A), RoundedCornerShape(10.dp))
                            .clickable { isMan = (i == 0) }.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center) {
                            Text(if (i == 0) "♂ 男" else "♀ 女", color = if (sel) Color(0xFF3A2E12) else Color(0xFF6B6258), fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                BzInput(question, "同事（选填）") { question = it.take(40) }
            }
            Spacer(Modifier.height(16.dp))

            // 开始排盘
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFC9A24A), Color(0xFFE0BE6B))))
                .clickable { onCast(correctedDt(), isMan, name, question, noSave) },
                contentAlignment = Alignment.Center) {
                Text("开 始 排 盘", color = Color(0xFF3A2E12), fontSize = 17.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(12.dp))
            // 四柱反查
            Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0x14FFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(12.dp))
                .clickable { showReverse = true }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("四柱反查", color = BZ_GOLD, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            // 八字排盘规则
            BzFormCard {
                Row(Modifier.fillMaxWidth().clickable { showRules = !showRules }, verticalAlignment = Alignment.CenterVertically) {
                    BzFormTitle("八 字 排 盘 规 则")
                    Spacer(Modifier.weight(1f))
                    Text(if (showRules) "收起" else "展开", color = BZ_GOLD, fontSize = 12.sp)
                }
                if (showRules) {
                    Spacer(Modifier.height(10.dp))
                    val rules = listOf(
                        "八字以出生年月日时排定四柱干支，每柱天干一地支。",
                        "年柱以立春为界，月柱以节气为界，日柱以子时为界。",
                        "时柱根据出生时辰确定，每两小时为一个时辰。",
                        "日主为日柱天干，是八字分析的核心。",
                        "旺衰以月令为主，兼看其他干支对日主之生克。",
                        "喜用神为命局所需之五行，忌神为命局所忌之五行。",
                        "大运由月柱顺逆推排，每运管十年。"
                    )
                    rules.forEach {
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("• ", color = BZ_GOLD, fontSize = 12.sp)
                            Text(it, color = Color(0xFFD9D0BE), fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showDate) BzDatePick(date, { showDate = false }) { date = it; showDate = false }
    if (showTime) BzTimePick(hour, minute, { showTime = false }) { h, m -> hour = h; minute = m; showTime = false }
    if (showArea) BzAreaPick(provinceIdx, cityIdx, districtIdx,
        { p, c, d -> provinceIdx = p; cityIdx = c; districtIdx = d }, { showArea = false })
    if (showReverse) BzReverseDialog { showReverse = false }
}

@Composable
private fun BzFormCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Color(0x14FFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(16.dp))
        .padding(16.dp)) { content() }
}

@Composable
private fun BzFormTitle(text: String) {
    Text(text, color = BZ_GOLD, fontSize = 13.sp, fontFamily = FontFamily.Serif, letterSpacing = 2.sp)
}

@Composable
private fun BzInput(value: String, placeholder: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color(0xFF9A8E7A), fontSize = 13.sp) },
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x0AFFFFFF), unfocusedContainerColor = Color(0x0AFFFFFF),
            focusedTextColor = Color(0xFFF0E8D8), unfocusedTextColor = Color(0xFFF0E8D8),
            cursorColor = BZ_GOLD, focusedBorderColor = BZ_GOLD, unfocusedBorderColor = Color(0x52D9A441)
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
    )
}

@Composable
private fun BzFormField(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFD9D0BE), fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
            .background(Color(0x0AFFFFFF)).border(1.dp, Color(0x52D9A441), RoundedCornerShape(10.dp))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(value, color = Color(0xFFF0E8D8), fontSize = 13.sp)
        }
    }
}

@Composable
private fun BzDatePick(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var y by remember { mutableIntStateOf(initial.year) }
    var m by remember { mutableIntStateOf(initial.monthValue) }
    var d by remember { mutableIntStateOf(initial.dayOfMonth) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF241E16)).padding(20.dp)) {
            Text("选择出生日期", color = BZ_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            BzStepper("年", y, 1900, 2100) { y = it }
            Spacer(Modifier.height(8.dp))
            BzStepper("月", m, 1, 12) { m = it }
            Spacer(Modifier.height(8.dp))
            BzStepper("日", d, 1, 31) { d = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BzDlgBtn("取消", false, Modifier.weight(1f), onDismiss)
                BzDlgBtn("确定", true, Modifier.weight(1f)) {
                    onConfirm(runCatching { LocalDate.of(y, m, d) }.getOrDefault(initial))
                }
            }
        }
    }
}

@Composable
private fun BzTimePick(initialH: Int, initialM: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var h by remember { mutableIntStateOf(initialH) }
    var m by remember { mutableIntStateOf(initialM) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF241E16)).padding(20.dp)) {
            Text("选择出生时辰", color = BZ_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            BzStepper("时", h, 0, 23) { h = it }
            Spacer(Modifier.height(8.dp))
            BzStepper("分", m, 0, 59) { m = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BzDlgBtn("取消", false, Modifier.weight(1f), onDismiss)
                BzDlgBtn("确定", true, Modifier.weight(1f)) { onConfirm(h, m) }
            }
        }
    }
}

@Composable
private fun BzStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = BZ_GOLD, fontSize = 13.sp, modifier = Modifier.width(34.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF))
            .clickable { if (value < max) onChange(value + 1) }, contentAlignment = Alignment.Center) {
            Text("+", color = BZ_GOLD, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(value.toString(), color = Color(0xFFF0E8D8), fontSize = 16.sp,
            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF))
            .clickable { if (value > min) onChange(value - 1) }, contentAlignment = Alignment.Center) {
            Text("−", color = BZ_GOLD, fontSize = 18.sp)
        }
    }
}

@Composable
private fun BzDlgBtn(text: String, gold: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(9.dp))
        .background(Brush.horizontalGradient(
            if (gold) listOf(Color(0xFFC9A24A), Color(0xFFE0BE6B))
            else listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF))))
        .clickable { onClick() }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (gold) Color(0xFF3A2E12) else Color(0xFFD9D0BE), fontSize = 13.sp)
    }
}

@Composable
private fun BzAreaPick(
    pIdx: Int, cIdx: Int, dIdx: Int,
    onSelect: (Int, Int, Int) -> Unit, onDismiss: () -> Unit
) {
    var p by remember { mutableIntStateOf(pIdx) }
    var c by remember { mutableIntStateOf(cIdx) }
    var d by remember { mutableIntStateOf(dIdx) }
    val provinces = BirthPlaces.PROVINCES
    val cities = provinces.getOrNull(p)?.cities ?: emptyList()
    val districts = cities.getOrNull(c)?.districts ?: emptyList()
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF241E16)).padding(18.dp)) {
            Text("选择出生地区", color = BZ_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(12.dp))
            Text("省 / 直辖市", color = Color(0xFFD9D0BE), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().height(38.dp)) {
                items(provinces.size) { i ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 2.dp)
                        .background(if (p == i) BZ_RED else Color(0x14FFFFFF)).clickable { p = i; c = 0; d = 0 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(provinces[i].name, color = Color(0xFFF0E8D8), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("城市 / 区县", color = Color(0xFFD9D0BE), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().height(38.dp)) {
                items(cities.size) { i ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 2.dp)
                        .background(if (c == i) BZ_RED else Color(0x14FFFFFF)).clickable { c = i; d = 0 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(cities[i].name, color = Color(0xFFF0E8D8), fontSize = 12.sp)
                    }
                }
            }
            if (districts.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("区县", color = Color(0xFFD9D0BE), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().height(38.dp)) {
                    items(districts.size) { i ->
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 2.dp)
                            .background(if (d == i) BZ_RED else Color(0x14FFFFFF)).clickable { d = i }
                            .padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(districts[i].name, color = Color(0xFFF0E8D8), fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BzDlgBtn("取消", false, Modifier.weight(1f), onDismiss)
                BzDlgBtn("确定", true, Modifier.weight(1f)) { onSelect(p, c, d); onDismiss() }
            }
        }
    }
}

/** 内置分析弹窗：基于 BaziAnalyzer 的格局/旺衰/用神/性格/行为/风险/大运，仿原版自带分析。 */
@Composable
private fun BzBuiltInDialog(c: BaziChart.Chart, onDismiss: () -> Unit) {
    val a = c.analysis
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF241E16))
            .padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("✦ 内置命理分析", color = BZ_GOLD, fontSize = 17.sp, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("✕", color = Color(0xFF9A8E7A), fontSize = 16.sp, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(Modifier.height(4.dp))
            Text("${c.lunar}  ${if (c.gender == "男") "乾造" else "坤造"}  ·  ${c.baziText}",
                color = Color(0xFF9A8E7A), fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                @Composable
                fun sec(title: String, body: String) {
                    if (body.isBlank()) return
                    Spacer(Modifier.height(10.dp))
                    Text(title, color = BZ_GOLD, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(body, color = Color(0xFFD9D0BE), fontSize = 12.5.sp, lineHeight = 20.sp)
                }
                sec("【总论】", buildString {
                    append("日主${c.pillars[2].gan}，${a.wangshuai}。")
                    if (!a.geju.isNullOrBlank()) append("格局：${a.geju}。")
                    if (!a.congge.isNullOrBlank()) append("从格：${a.congge}。")
                    append("综合评分 ${a.score} 分。")
                })
                sec("【五行喜忌】", "旺衰：${a.wangshuai}；用神：${a.yongshen.joinToString("、")}；" +
                    "调候：${a.tiaohou.joinToString("、")}；主导五行：${a.dominantWuxing}。")
                sec("【性格心性】", a.personality)
                sec("【主导十神】", a.dominantShishen)
                sec("【行为作风】", a.behavior)
                sec("【行事风格】", a.style)
                if (a.risks.isNotEmpty()) sec("【风险提示】", a.risks.joinToString("；"))
                if (a.detail.isNotEmpty()) sec("【详细论断】", a.detail.joinToString("\n"))
            }
            Spacer(Modifier.height(14.dp))
            BzDlgBtn("关闭", true, Modifier.fillMaxWidth(), onDismiss)
        }
    }
}

/** 十神/神煞/纳音 知识详情弹窗。 */
@Composable
private fun BzKnowledgeDialog(target: BzDetailTarget, onDismiss: () -> Unit) {
    val (title, raw) = bzKnowledge(target.type, target.name)
    val paras = remember(target) {
        raw.split("<br/>", "<br>", "\n").map { it.trim() }.filter { it.isNotBlank() }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF241E16))
            .padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = BZ_GOLD, fontSize = 17.sp, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("✕", color = Color(0xFF9A8E7A), fontSize = 16.sp, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                paras.forEach { p ->
                    val isHead = p.length <= 8 && (p.startsWith("优点") || p.startsWith("缺点") ||
                        p.startsWith("代表") || p.startsWith("命中") || p in setOf("身强时", "身弱时", "格局", "断句"))
                    if (isHead) {
                        Spacer(Modifier.height(8.dp))
                        Text(p, color = BZ_GOLD, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(p, color = Color(0xFFD9D0BE), fontSize = 12.5.sp, lineHeight = 20.sp,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            BzDlgBtn("关闭", true, Modifier.fillMaxWidth(), onDismiss)
        }
    }
}

@Composable
private fun BzReverseDialog(onDismiss: () -> Unit) {
    var gz by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF241E16)).padding(20.dp)) {
            Text("四柱反查", color = BZ_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(6.dp))
            Text("输入四柱干支（如：甲子 乙丑 丙寅 丁卯），查询对应公历年份参考。", color = Color(0xFFB8AC98), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            BzInput(gz, "例如：丙午 己亥 壬寅 庚戌") { gz = it }
            Spacer(Modifier.height(12.dp))
            val parts = gz.trim().split(Regex("\\s+")).filter { it.length == 2 }
            if (parts.isNotEmpty()) {
                val years = parts.mapIndexed { i, p -> "${listOf("年","月","日","时").getOrElse(i){""}}柱 $p" }
                Text(years.joinToString("  "), color = Color(0xFFD9D0BE), fontSize = 12.sp)
                val yi = com.fortune.stall.util.LunarCalendar.TIANGAN.indexOf(parts[0].first().toString())
                if (yi >= 0) Text("年干“${parts[0].first()}”对应公历年份尾数为 ${(yi - 3).let { ((it % 10) + 10) % 10 }}", color = BZ_GOLD, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            BzDlgBtn("关闭", true, Modifier.fillMaxWidth(), onDismiss)
        }
    }
}

@Composable
private fun BaziDetailScreen(chart: BaziChart.Chart, full: BaziFull.Data?, xp: BaziXiangpan.Data?, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var detailTarget by remember { mutableStateOf<BzDetailTarget?>(null) }
    var showBuiltIn by remember { mutableStateOf(false) }
    val tabs = listOf("基本", "命盘", "详盘")
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F1E8))) {
        // 顶栏（仿原版吉时雨八字 红色标题栏）
        Box(Modifier.fillMaxWidth().background(BZ_RED).padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text("八 字 排 盘", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(28.dp))
            }
        }
        // Tab 行（浅灰底，选中项红字红下划线）
        Row(Modifier.fillMaxWidth().background(Color(0xFFEFEDE6))) {
            tabs.forEachIndexed { i, t ->
                val sel = tab == i
                Column(
                    Modifier.weight(1f).clickable { tab = i }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t, color = if (sel) BZ_RED else Color(0xFF666666),
                        fontSize = 16.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    if (sel) Box(Modifier.width(28.dp).height(2.dp).background(BZ_RED))
                }
            }
        }
        // 页面内容
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth()) {
                when (tab) {
                    0 -> BzBasicPage(chart, full)
                    1 -> BzMingpanPage(chart, full, onDetail = { detailTarget = it })
                    2 -> BzDetailPage(chart, full, xp, onDetail = { detailTarget = it })
                }
                Spacer(Modifier.height(12.dp))
                // 内置分析（原版自带） + 复制排盘 + 问AI
                val faceText = when (tab) {
                    0 -> baziBasicText(chart, full)
                    1 -> baziMingpanText(chart, full)
                    else -> baziDetailText(chart, full, xp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF8E44AD), Color(0xFF6A3093))))
                        .clickable { showBuiltIn = true }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text("✦ 内置分析", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.weight(1.4f)) {
                        com.fortune.stall.ui.components.ResultActionBar(faceText = faceText)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    detailTarget?.let { BzKnowledgeDialog(it) { detailTarget = null } }
    if (showBuiltIn) BzBuiltInDialog(chart) { showBuiltIn = false }
}

/* ============================ 基本页（图1） ============================ */
@Composable
private fun BzBasicPage(c: BaziChart.Chart, f: BaziFull.Data?) {
    val dayGan = c.pillars[2].gan
    val dayWx = GAN_WX_OF(dayGan)
    // 五虎遁：由年干 + 宫支补出命宫/身宫的完整干支（年干甲己丙寅起、乙庚戊寅、丙辛庚寅、丁壬壬寅、戊癸甲寅）
    fun palaceGz(gong: String): String {
        val zhi = gong.replace("宫", "").ifBlank { "寅" }
        val zi = BZ_ZHI.indexOf(zhi).coerceAtLeast(0)
        val start = when (c.yearGan) { "甲", "己" -> 2; "乙", "庚" -> 4; "丙", "辛" -> 6; "丁", "壬" -> 8; "戊", "癸" -> 0; else -> 2 }
        val off = ((zi - 2) + 12) % 12
        return "" + BZ_GAN[(start + off) % 10] + zhi
    }
    val mingGz = palaceGz(c.mingGong)
    val shenGz = palaceGz(c.shenGong)
    // 忌神：克日主之五行（官杀），如癸水日主→土
    val jishen = WX_KE.entries.firstOrNull { it.value == dayWx }?.key ?: ""
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // 顶部身份卡（仿原版：头像 + 农历时辰 + 乾造 + 公历）
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, BZ_RED, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) { Text(c.shengXiao, fontSize = 30.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("命主八字", color = Color(0xFF222222), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text((f?.nongliFull ?: c.lunar) + "  " + (if (c.gender == "男") "乾造" else "坤造"),
                        color = Color(0xFF444444), fontSize = 12.5.sp)
                    Text("公历 " + c.solar, color = Color(0xFF888888), fontSize = 11.5.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 时间信息
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("时间信息")
            BzInfo("阳历", "公历" + c.solar)
            BzInfo("农历", "农历" + (f?.nongliFull ?: c.lunar))
            BzInfo("性别", c.gender)
            BzInfo("属相", c.shengXiao)
        }
        Spacer(Modifier.height(6.dp))
        // 命宫 · 胎元 · 身宫
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("命宫·胎元·身宫")
            Row(Modifier.fillMaxWidth()) {
                BzPalaceCell(Modifier.weight(1f), mingGz, "命宫", "精神")
                BzPalaceCell(Modifier.weight(1f), c.taiYuan, "胎元", "先天")
                BzPalaceCell(Modifier.weight(1f), shenGz, "身宫", "后天")
            }
        }
        Spacer(Modifier.height(6.dp))
        // 纳音 · 空亡
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("纳音·空亡")
            val pn = listOf("年柱", "月柱", "日柱", "时柱")
            c.pillars.forEachIndexed { i, p ->
                BzInfo(pn[i], p.ganZhi + "  " + p.naYin)
            }
            BzInfo("空亡", c.pillars[2].kongWang)
        }
        Spacer(Modifier.height(6.dp))
        // 喜用忌神
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("喜用忌神")
            BzTagRow("喜用神", c.analysis.yongshen.filter { it.isNotBlank() }, Color(0xFF2E7D5B))
            BzTagRow("忌神", if (jishen.isNotBlank()) listOf(jishen) else emptyList(), Color(0xFFB0392E))
        }
        Spacer(Modifier.height(6.dp))
        // 神煞（按名称汇总，标注来源地支）
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("神煞")
            val grouped = linkedMapOf<String, MutableList<String>>()
            c.pillars.forEach { p ->
                p.shenSha.split("、", "·").filter { it.isNotBlank() && it != "无" }.forEach { raw ->
                    val full = BZ_SHEN_FULL[raw] ?: raw
                    grouped.getOrPut(full) { mutableListOf() }.apply { if (p.zhi !in this) add(p.zhi) }
                }
            }
            if (grouped.isEmpty()) Text("无", color = Color(0xFF999999), fontSize = 12.sp)
            else grouped.forEach { (name, zhis) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Text(name, color = Color(0xFF2E7D5B), fontSize = 12.sp, modifier = Modifier.width(84.dp))
                    WrapTags(zhis.map { "($it)" }, Color(0xFF2E7D5B))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 刑冲合害
        if (f != null && (f.liuYi.tiangan.isNotEmpty() || f.liuYi.dizhi.isNotEmpty())) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
                BzSection("刑冲合害")
                val all = f.liuYi.tiangan + f.liuYi.dizhi
                all.forEach { Text("• $it", color = Color(0xFF555555), fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)) }
            }
            Spacer(Modifier.height(6.dp))
        }
        // 十二长生
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("十二长生")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BZ_ZHI.forEach { ch ->
                    val z = ch.toString()
                    val st = com.fortune.stall.engine.jishiyu.JishiyuBase.queryShengwang(dayGan, z)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(z, color = Color(0xFF888888), fontSize = 11.sp)
                        Text(st.firstOrNull()?.toString() ?: "", color = BZ_RED, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            BzInfo("日支长生", c.pillars[2].deShi + " · " + (BZ_CHANGSHENG_DESC[c.pillars[2].deShi] ?: ""))
        }
        Spacer(Modifier.height(6.dp))
        // 时间/日主/性格 已在上方，补日主分析与性格
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("日主分析")
            BzInfo("日主", dayGan + " " + dayWx)
            val wsDesc = when (c.analysis.wangshuai) {
                "身强" -> "日主五行偏旺，气势充盈"
                "身弱" -> "日主五行偏弱，宜生扶"
                else -> "日主五行平衡，不偏不倚"
            }
            BzInfo("旺衰", c.analysis.wangshuai + " · " + wsDesc)
        }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("性格")
            val personality = listOf(c.analysis.personality, c.analysis.behavior, c.analysis.style)
                .filter { it.isNotBlank() }.distinct().joinToString("，")
            Text(personality.ifBlank { "—" }, color = Color(0xFF444444), fontSize = 12.5.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(6.dp))
        // 五行统计条形图（原版口径：4天干+4地支本气+全部藏干）
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("五行统计")
            val qty = f?.wuXingQty ?: c.wuXing.toMap()
            val order = listOf("木", "火", "土", "金", "水")
            val barColor = mapOf("木" to Color(0xFF4CAF50), "火" to Color(0xFFE53935), "土" to Color(0xFFC8883C),
                "金" to Color(0xFFD9A441), "水" to Color(0xFF3D7FC1))
            val maxQty = (order.maxOfOrNull { qty[it] ?: 0 } ?: 1).coerceAtLeast(1)
            for (wx in order) {
                val cnt = qty[wx] ?: 0
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(wx, color = barColor[wx] ?: Color(0xFF333333), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp))
                    Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFECF0F3))) {
                        Box(Modifier.fillMaxWidth(cnt / maxQty.toFloat()).height(9.dp)
                            .clip(RoundedCornerShape(5.dp)).background(barColor[wx] ?: Color.Gray))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("$cnt", color = barColor[wx] ?: Color(0xFF666666), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 五行旺衰（原版底部彩条：木相 火囚 土死 金休 水旺）
        if (f != null && f.wangShuai.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
                BzSection("五行旺衰")
                val labels = listOf("旺", "相", "休", "囚", "死")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    f.wangShuai.forEachIndexed { i, wx ->
                        Text(wx + labels.getOrNull(i), color = BZ_RED, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        // 袁天罡称骨算命
        Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
            BzSection("袁天罡称骨算命")
            BzInfo("骨重", c.chenggu.weight)
            Text(c.chenggu.verse, color = Color(0xFF333333), fontSize = 13.sp, lineHeight = 21.sp,
                modifier = Modifier.padding(vertical = 6.dp))
            if (f != null && f.chengguDetail.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                f.chengguDetail.split("<br/>", "<br>", "\n").filter { it.isNotBlank() }.forEach { seg ->
                    val isHead = seg in setOf("古文记载", "综合解析", "综合详解", "详解")
                    if (isHead) { Spacer(Modifier.height(8.dp)); BzSection(seg) }
                    else Text(seg.trim(), color = Color(0xFF444444), fontSize = 12.5.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("* 以上内容来自互联网，仅供参考，不等于真实评测。", color = Color(0xFF999999), fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

/** 命宫/胎元/身宫 单元：干支 + 五行小字 + 标签。 */
@Composable
private fun BzPalaceCell(mod: Modifier, gz: String, title: String, tag: String) {
    Column(mod, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color(0xFF888888), fontSize = 11.sp)
        Text(gz, color = BZ_RED, fontSize = 19.sp,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        val wx = if (gz.length >= 1) (GAN_WX_OF(gz[0].toString()) + (BZ_ZHI_WX[gz.getOrNull(1)?.toString()] ?: "")) else ""
        Text(wx, color = Color(0xFF999999), fontSize = 10.sp)
        Text(tag, color = Color(0xFFAAAAAA), fontSize = 10.sp)
    }
}

/** 喜用神/忌神 标签行。 */
@Composable
private fun BzTagRow(label: String, items: List<String>, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(64.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { BzChip(it, color) }
        }
    }
}

@Composable
private fun WrapTags(items: List<String>, color: Color) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { BzChip(it, color) }
    }
}

@Composable
private fun BzChip(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.14f))
        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, color = color, fontSize = 11.sp)
    }
}

/* ============================ 命盘页（图2） ============================ */
@Composable
private fun BzMingpanPage(c: BaziChart.Chart, f: BaziFull.Data?, onDetail: (BzDetailTarget) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // 顶部信息
        Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, BZ_RED, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) { Text(c.shengXiao, fontSize = 32.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    BzInfo("农历", (f?.nongliFull ?: c.lunar) + "  (" + (if (c.gender == "男") "乾造" else "坤造") + ")")
                    BzInfo("北京时间", c.solar)
                    BzInfo("真太阳时", c.solar)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 四柱排盘表格
        Column(Modifier.fillMaxWidth().background(Color.White).padding(8.dp)) {
            Row(Modifier.fillMaxWidth().background(Color(0xFFF7F7F7)).padding(vertical = 6.dp)) {
                Text("四柱", color = Color(0xFFFFFFFF), fontSize = 12.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
                for (h in listOf("年柱", "月柱", "日柱", "时柱")) Text(h, color = Color(0xFF888888), fontSize = 13.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            // 十神（可点击查看详情）
            BzTableRow("十神", c.pillars.map { it.shiShen }, detailType = "十神", onDetail = onDetail) { Color(0xFF222222) to 13.sp }
            // 天干（五行着色 + 大字）
            BzTableRow("天干", c.pillars.map { it.gan }) { g -> (WX_COLOR_DETAIL[GAN_WX_OF(g)] ?: BZ_RED) to 24.sp }
            // 地支（五行着色 + 大字）
            BzTableRow("地支", c.pillars.map { it.zhi }) { z -> (WX_COLOR_DETAIL[JishiyuBase.ZHI_WUXING[z] ?: ""] ?: BZ_RED) to 24.sp }
            // 藏干+十神（合并，每格分行）
            Row(Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).padding(vertical = 6.dp)) {
                Text("藏干", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
                c.pillars.forEach { p ->
                    val gs = p.cangGan.map { it.toString() }; val ss = p.cangShen.split("·")
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        for (i in gs.indices) Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(gs[i], color = WX_COLOR_LIGHT[GAN_WX_OF(gs[i])] ?: Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onDetail(BzDetailTarget("十神", com.fortune.stall.engine.jishiyu.Shishen.query(gs[i], c.pillars[2].gan))) })
                            Text(ss.getOrNull(i) ?: "", color = Color(0xFF333333), fontSize = 12.sp,
                                modifier = Modifier.clickable { val sv = ss.getOrNull(i) ?: ""; if (sv.isNotBlank()) onDetail(BzDetailTarget("藏干十神", sv)) })
                        }
                    }
                }
            }
            BzTableRow("地势", c.pillars.map { it.deShi }) { Color(0xFF222222) to 12.sp }
            BzTableRow("自坐", c.pillars.map { it.ziZuo }) { Color(0xFF222222) to 12.sp }
            BzTableRow("空亡", c.pillars.map { it.kongWang }) { Color(0xFF222222) to 12.sp }
            BzTableRow("纳音", c.pillars.map { it.naYin }, detailType = "纳音", onDetail = onDetail) { Color(0xFF1565C0) to 12.sp }
            // 神煞（每行多个，竖排，可点击查看详情）
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("神煞", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
                c.pillars.forEach { p ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        p.shenSha.split("、", "·").filter { it.isNotBlank() && it != "无" }.forEach {
                            Text(it, color = Color(0xFF3F51B5), fontSize = 10.sp, lineHeight = 15.sp,
                                modifier = Modifier.clickable { onDetail(BzDetailTarget("神煞", it)) })
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 五行旺衰（旺相休囚死）
        if (f != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                val labels = listOf("旺", "相", "休", "囚", "死")
                val colors = listOf(Color(0xFF2E7D32), Color(0xFF388E3C), Color(0xFFF9A825), Color(0xFFEF6C00), Color(0xFFC62828))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))) {
                    f.wangShuai.forEachIndexed { i, wx ->
                        Box(Modifier.weight(1f).background(colors[i]).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(wx + labels[i], color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                BzLiuYi("天干留意", f.liuYi.tiangan)
                Spacer(Modifier.height(6.dp))
                BzLiuYi("地支留意", f.liuYi.dizhi)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("* 提示：点击十神、神煞、纳音显示详情。", color = Color(0xFF999999), fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BzLiuYi(title: String, items: List<String>) {
    BzSection(title)
    if (items.isEmpty()) {
        Text("无", color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (s in items) {
            Text(s, color = BZ_RED, fontSize = 11.5.sp,
                modifier = Modifier.border(1.dp, BZ_LINE, RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF6F4), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

/* ============================ 详盘页（图3） ============================ */
private val WX_COLOR_DETAIL = mapOf(
    "木" to Color(0xFF00897B), "火" to Color(0xFFD32F2F), "土" to Color(0xFFAD6A00),
    "金" to Color(0xFFD98C00), "水" to Color(0xFF1565C0)
)
private val WX_COLOR_LIGHT = mapOf(
    "木" to Color(0xFF2E7D32), "火" to Color(0xFFC62828), "土" to Color(0xFFB8860B),
    "金" to Color(0xFFB8860B), "水" to Color(0xFF1565C0)
)

@Composable
private fun BzDetailPage(c: BaziChart.Chart, f: BaziFull.Data?, xp: BaziXiangpan.Data?, onDetail: (BzDetailTarget) -> Unit) {
    var taimingOn by remember { mutableStateOf(false) }   // 胎命身：开=左列换胎元/命宫/身宫
    var chuangongOn by remember { mutableStateOf(false) } // 串宫：开=显示干支串宫两行
    var taisuiType by remember { mutableIntStateOf(1) }   // 起太岁：1流年 2大运 3日干
    // 顶部"流年/大运"列随下方大运选区联动（默认第一步大运 + 其首流年）
    var topDy by remember { mutableIntStateOf(0) }
    var topLn by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // 顶部信息卡：生肖 + 农历 + 北京时间/真太阳时 + 胎命身/串宫开关
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, BZ_RED, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Text(c.shengXiao, fontSize = 32.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                BzInfo("农历", (f?.nongliFull ?: c.lunar) + "  " + (if (c.gender == "男") "(乾造)" else "(坤造)"))
                BzInfo("北京时间", c.solar)
                BzInfo("真太阳时", c.solar)
            }
            Column(horizontalAlignment = Alignment.End) {
                BzToggle("胎命身", taimingOn, onToggle = { taimingOn = it })
                Spacer(Modifier.height(6.dp))
                BzToggle("串宫", chuangongOn, onToggle = { chuangongOn = it })
            }
        }
        Spacer(Modifier.height(6.dp))

        // 8 列对照表（流日/流月/流年/小运 ‖ 年月日时），横向滚动
        if (xp != null) {
            val dayGan = c.pillars[2].gan
            val isMan = c.gender == "男"
            val niannayin = c.pillars[0].naYin
            val bazi = c.pillars.flatMap { listOf(it.gan, it.zhi) }
            // 胎命身三柱（真值：庚寅/庚寅/壬辰）
            val sg = remember(c) { BaziChuangong.sanGong(c.yearGan, c.pillars[1].gan, c.pillars[1].zhi, c.pillars[3].zhi) }
            val tyCol = remember(sg) { BaziXiangpan.colOf(sg.taiyuan, dayGan, bazi, isMan, 7, niannayin) }
            val mgCol = remember(sg) { BaziXiangpan.colOf(sg.minggong, dayGan, bazi, isMan, 6, niannayin) }
            val shCol = remember(sg) { BaziXiangpan.colOf(sg.shengong, dayGan, bazi, isMan, 5, niannayin) }

            // 串宫：太岁支 + 每列干支的十二神
            val taisuiZhi = BaziChuangong.taisuiZhi(taisuiType, xp.liunian.zhi, xp.xiaoyun.zhi, dayGan)
            fun gods(gz: String) = BaziChuangong.gods12(gz, taisuiZhi)
            // 顶部表：原版紧凑 7 列「日期 | 流年 | 大运 || 年柱 月柱 日柱 时柱」
            // 流年/大运列随下方大运选区联动；胎命身开时左两列换为胎元/命宫/身宫
            val dyList = f?.dayun ?: emptyList()
            val safeDy = topDy.coerceIn(0, maxOf(0, dyList.size - 1))
            val selDayun = dyList.getOrNull(safeDy)
            val lnList = selDayun?.liunian ?: emptyList()
            val safeLn = topLn.coerceIn(0, maxOf(0, lnList.size - 1))
            val selLiunian = lnList.getOrNull(safeLn)
            val liunianCol = selLiunian?.let { BaziXiangpan.colOf(it.ganzhi, dayGan, bazi, isMan, 6, niannayin) }
            val dayunCol = selDayun?.let { BaziXiangpan.colOf(it.ganzhi, dayGan, bazi, isMan, 1, niannayin) }
            // 起太岁方式：点击"串宫"行标签切换（流年→大运→日干）
            Column(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    // 行标签列（固定）
                    Column(Modifier.width(34.dp)) {
                        Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                            Text("日期", color = Color(0xFF999999), fontSize = 10.sp)
                        }
                        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) { Text("十神", color = Color(0xFF999999), fontSize = 10.sp) }
                        if (chuangongOn) Box(Modifier.fillMaxWidth().height(22.dp).clickable { taisuiType = taisuiType % 3 + 1 },
                            contentAlignment = Alignment.Center) { Text("串宫", color = Color(0xFFC62828), fontSize = 9.sp) }
                        Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) { Text("天干", color = Color(0xFF999999), fontSize = 10.sp) }
                        Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) { Text("地支", color = Color(0xFF999999), fontSize = 10.sp) }
                        if (chuangongOn) Box(Modifier.fillMaxWidth().height(22.dp).clickable { taisuiType = taisuiType % 3 + 1 },
                            contentAlignment = Alignment.Center) { Text("串宫", color = Color(0xFFC62828), fontSize = 9.sp) }
                        listOf("藏干","地势","自坐","空亡","纳音","神煞").forEach { lbl ->
                            Box(Modifier.fillMaxWidth().height(if (lbl=="藏干") 84.dp else if (lbl=="神煞") 96.dp else 34.dp),
                                contentAlignment = Alignment.Center) { Text(lbl, color = Color(0xFF999999), fontSize = 10.sp) }
                        }
                    }
                    // 胎命身开=胎元/命宫/身宫；关=流年/大运（与原版一致）
                    if (taimingOn) {
                        XpColCard(tyCol, "胎元", sg.taiyuan, showGods = chuangongOn,
                            ganGod = gods(sg.taiyuan).gan, zhiGod = gods(sg.taiyuan).zhi)
                        XpColCard(mgCol, "命宫", sg.minggong, showGods = chuangongOn,
                            ganGod = gods(sg.minggong).gan, zhiGod = gods(sg.minggong).zhi)
                        XpColCard(shCol, "身宫", sg.shengong, showGods = chuangongOn,
                            ganGod = gods(sg.shengong).gan, zhiGod = gods(sg.shengong).zhi)
                    } else {
                        if (liunianCol != null) XpColCard(liunianCol, "流年", "${selLiunian!!.year}", showGods = chuangongOn,
                            ganGod = gods(liunianCol.ganzhi).gan, zhiGod = gods(liunianCol.ganzhi).zhi)
                        if (dayunCol != null) XpColCard(dayunCol, "大运", "${selDayun!!.startYear}", divider = true, showGods = chuangongOn,
                            ganGod = gods(dayunCol.ganzhi).gan, zhiGod = gods(dayunCol.ganzhi).zhi)
                    }
                    // 年月日时四柱
                    XpColCard(xp.year, "年柱", c.lunar.replace("日",""), showGods = chuangongOn,
                        ganGod = gods(xp.year.ganzhi).gan, zhiGod = gods(xp.year.ganzhi).zhi,
                        hiGan = chuangongOn && taisuiType==3, onDetail = onDetail)
                    XpColCard(xp.month, "月柱", "十月", showGods = chuangongOn,
                        ganGod = gods(xp.month.ganzhi).gan, zhiGod = gods(xp.month.ganzhi).zhi, onDetail = onDetail)
                    XpColCard(xp.day, "日柱", "十四", showGods = chuangongOn,
                        ganGod = gods(xp.day.ganzhi).gan, zhiGod = gods(xp.day.ganzhi).zhi,
                        hiGan = chuangongOn && taisuiType==3, onDetail = onDetail)
                    XpColCard(xp.hour, "时柱", "辰时", showGods = chuangongOn,
                        ganGod = gods(xp.hour.ganzhi).gan, zhiGod = gods(xp.hour.ganzhi).zhi, onDetail = onDetail)
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // 起运/交运信息
        if (f != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("起运：出生${f.qiYun.startYear}年${f.qiYun.startMonth}月${f.qiYun.startDay}天${f.qiYun.startHour}时后起运",
                        color = Color(0xFF444444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("${f.dayun.firstOrNull()?.startAge ?: 1}虚岁", color = Color(0xFF888888), fontSize = 11.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("交运：逢${f.qiYun.jiaoyunGan1}、${f.qiYun.jiaoyunGan2}年 ${f.qiYun.jieName}后${f.qiYun.days}天交大运",
                        color = Color(0xFF444444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("司令：${xp?.siLing ?: ""}", color = Color(0xFF888888), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            // 逐级下钻大运网格：点大运→流年，点流年→流月，点流月→流日（同步顶部流年/大运列）
            BzDayunDrill(c = c, f = f, onSelect = { dy, ln -> topDy = dy; topLn = ln })
            Spacer(Modifier.height(6.dp))
        }

        // 五行旺衰彩条
        if (f != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp)) {
                val labels = listOf("旺", "相", "休", "囚", "死")
                val colors = listOf(Color(0xFF1976D2), Color(0xFF00897B), Color(0xFFF9A825), Color(0xFF8D4E00), Color(0xFFE53935))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp))) {
                    f.wangShuai.forEachIndexed { i, wx ->
                        Box(Modifier.weight(1f).background(colors[i]).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(wx + labels[i], color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // 天干留意 + 地支留意（详盘：含流日流月流年小运）
        val ly = xp?.liuYi ?: f?.liuYi
        if (ly != null) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                BzLiuYi("天干留意", ly.tiangan)
                Spacer(Modifier.height(8.dp))
                BzLiuYi("地支留意", ly.dizhi)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("* 提示：点击十神、神煞、纳音显示详情。", color = Color(0xFF8A96B5), fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

/** 详盘 8 列表格中的一列：表头(公历/农历) + 9 行属性 */
@Composable
private fun XpColCard(
    col: BaziXiangpan.Col, head1: String, head2: String, divider: Boolean = false,
    showGods: Boolean = false, ganGod: String = "", zhiGod: String = "",
    hiGan: Boolean = false, onDetail: (BzDetailTarget) -> Unit = {}
) {
    Column(
        Modifier.width(52.dp)
            .then(if (divider) Modifier.border(1.dp, Color(0xFFE0E0E0)) else Modifier)
            .padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 表头
        Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
            Text(head1, color = Color(0xFF777777), fontSize = 11.sp)
        }
        // 十神（可点击查看详情）
        Box(Modifier.fillMaxWidth().height(36.dp).clickable {
            if (col.ganShen.isNotBlank()) onDetail(BzDetailTarget("十神", col.ganShen))
        }, contentAlignment = Alignment.Center) { Text(col.ganShen, color = Color(0xFF222222), fontSize = 12.sp) }
        // 天干串宫行（十神下方）：红字白底
        if (showGods) Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            Text(ganGod, color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        // 天干
        Box(Modifier.fillMaxWidth().height(42.dp).background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
            Text(col.gan, color = WX_COLOR_DETAIL[col.ganWx] ?: BZ_RED, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
        // 地支
        Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
            Text(col.zhi, color = WX_COLOR_DETAIL[col.zhiWx] ?: BZ_RED, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
        // 地支串宫行（地支下方）：红字白底
        if (showGods) Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            Text(zhiGod, color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        // 藏干+十神
        Column(Modifier.fillMaxWidth().height(84.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val gs = col.cangGan.map { it.toString() }
            val ss = col.cangShen.split("·")
            for (i in gs.indices) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(gs.getOrNull(i) ?: "", color = WX_COLOR_LIGHT[GAN_WX_OF(gs.getOrNull(i) ?: "")] ?: Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(ss.getOrNull(i) ?: "", color = Color(0xFF333333), fontSize = 11.sp)
                }
            }
        }
        // 地势/自坐/空亡
        for (v in listOf(col.deShi, col.ziZuo, col.kongWang)) {
            Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) { Text(v, color = Color(0xFF222222), fontSize = 11.sp) }
        }
        // 纳音（可点击查看详情）
        Box(Modifier.fillMaxWidth().height(34.dp).clickable {
            if (col.naYin.isNotBlank()) onDetail(BzDetailTarget("纳音", col.naYin))
        }, contentAlignment = Alignment.Center) {
            Text(col.naYin, color = Color(0xFF1565C0), fontSize = 11.sp)
        }
        // 神煞（竖排多个，与表头 96.dp 对齐）
        Column(Modifier.fillMaxWidth().height(96.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            val ss = col.shenSha.split("、", "·").filter { it.isNotBlank() && it != "无" }
            if (ss.isEmpty()) Text("无", color = Color(0xFFAAAAAA), fontSize = 9.sp)
            else ss.forEach { Text(it, color = Color(0xFF3F51B5), fontSize = 9.sp, lineHeight = 13.sp,
                modifier = Modifier.clickable { onDetail(BzDetailTarget("神煞", it)) }) }
        }
    }
}

private fun GAN_WX_OF(gan: String) = when (gan) {
    "甲","乙" -> "木"; "丙","丁" -> "火"; "戊","己" -> "土"; "庚","辛" -> "金"; "壬","癸" -> "水"; else -> ""
}

// 五行相生（我生者=食伤，生我者=印）与相克（我克者=财，克我者=官）
private val WX_SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
private val WX_KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")

/** 某五行相对日主的十神类别（财/官/印/比劫/食伤）。 */
private fun shishenCategory(wx: String, dayWx: String) = when {
    wx == dayWx -> "比劫"
    WX_SHENG[dayWx] == wx -> "食伤"
    WX_SHENG[wx] == dayWx -> "印星"
    WX_KE[dayWx] == wx -> "财星"
    WX_KE[wx] == dayWx -> "官星"
    else -> ""
}

/** 点击查询项：十神 / 神煞 / 纳音 / 藏干十神。 */
private data class BzDetailTarget(val type: String, val name: String)

/** 取某项的知识解释（正文，可能含 <br/> 分段）。 */
private fun bzKnowledge(type: String, name: String): Pair<String, String> {
    return when (type) {
        "十神", "藏干十神" -> {
            val key = when (name) {
                "比" -> "比肩"; "劫" -> "劫财"; "食" -> "食神"; "伤" -> "伤官"
                "才" -> "偏财"; "财" -> "正财"; "杀" -> "七杀"; "官" -> "正官"
                "枭" -> "偏印"; "印" -> "正印"; "日元" -> "日主"; else -> name
            }
            val body = com.fortune.stall.engine.jishiyu.Shishen.HELP[key]
            "十神 · $key" to (body ?: "$key：日主与对应天干的生克关系，代表命局中该类人事物与性格倾向。")
        }
        "神煞" -> "神煞 · $name" to (SHENSHA_KNOWLEDGE[name] ?:
            "$name：八字神煞之一，由特定干支组合推演而来，吉者助运、凶者示警，需结合全局喜忌参看，不可单凭一星定论。")
        "纳音" -> "纳音 · $name" to (NAYIN_KNOWLEDGE[name] ?:
            "$name：六十甲子纳音之一，以五行之象喻人物品性，古称\"纳音配命\"，作为格局与性情参看的辅助。")
        else -> name to "暂无详解。"
    }
}

/** 神煞简明知识（常见 40+ 神煞，取核心断语）。 */
private val SHENSHA_KNOWLEDGE = mapOf(
    "天乙贵人" to "命中最吉之神，主逢凶化吉、易得贵人扶助，人缘佳、遇难呈祥。",
    "太极贵人" to "主聪明好学、喜神秘事物，有钻研精神，宜学术、玄学、研究之道。",
    "文昌贵人" to "主聪明好学、才华文采，利读书、考试、著述，文书学业有成。",
    "天厨贵人" to "主衣食丰足、口福好，一生不缺饮食，宜饮食、烹饪相关。",
    "福星贵人" to "主一生福禄相伴、少灾少难，性情温和，家宅安宁。",
    "国印贵人" to "主诚实可靠、有权威，宜公职、管理，掌权印之象。",
    "学堂" to "主聪明、学业有成，利于读书求知，文科才华突出。",
    "词馆" to "主文采出众、口才佳，宜写作、教育、演艺等文字口舌之业。",
    "驿马" to "主走动、迁移、奔波，宜外出发展、交通运输、外向型职业。",
    "桃花" to "又称咸池，主异性缘、魅力、情感丰富，也主才艺与人缘。",
    "红鸾" to "主婚姻喜事、姻缘，逢流年引动多有婚恋之喜。",
    "天喜" to "主喜庆、婚姻、添丁之喜，一生喜事较多。",
    "将星" to "主领导力、权威，能统御众人，宜担当管理者、团队核心。",
    "华盖" to "主聪明孤高、喜哲学玄学艺术，聪明好学但易孤独，宜宗教、研究、艺术。",
    "劫煞" to "主耗损、被夺、意外破财，凡事宜守，防小人夺财。",
    "亡神" to "主心机、失落、易失势，细胞内耗，宜谨慎行事。",
    "灾煞" to "主灾病、意外、血光，流年逢之宜注意健康与安全。",
    "勾绞" to "主牵连、纠纷、官非口舌，宜远离是非、慎重签约。",
    "元辰" to "主坎坷、感情波折，做事多阻，宜稳中求进。",
    "大耗" to "主破财、耗损，大半因意外或人情花费，宜量入为出。",
    "小耗" to "主小的财物损耗、破小财，无大碍。",
    "岁破" to "主与太岁相冲，流年逢之诸事不顺、宜守不宜进。",
    "羊刃" to "主刚烈、暴戾、血光，身强者尤忌，宜收敛脾气、注意刀兵之灾。",
    "飞刃" to "主血光、争斗、意外伤灾，宜谨慎、避免冲突。",
    "血刃" to "主血光之灾、开刀手术、意外创伤，流年逢之注意健康。",
    "禄神" to "主俸禄、财禄、衣食，天干之禄，主稳定收入与生活保障。",
    "天德贵人" to "至善之神，主心地仁善、逢凶化吉、消灾解厄，一生多贵人。",
    "月德贵人" to "主仁慈、人缘好、逢凶化吉，能化解灾厄。",
    "天德合" to "主吉上加吉、贵人相助，化解凶煞之力更强。",
    "月德合" to "主和顺、人缘佳、遇难呈祥。",
    "天赦日" to "主消灾解厄、诸事顺遂，百无禁忌。",
    "天医" to "主医药、健康、医缘，宜从医、养生，疾病易愈。",
    "孤辰" to "主孤独、六亲缘薄，性情独立，宜修身养性。",
    "寡宿" to "主孤寂、独居，女命尤显，人际较淡。",
    "空亡" to "主虚耗、落空、事难成，吉神落空则其力减，凶煞落空则其害轻。",
    "十灵日" to "主聪慧灵秀、直觉强，悟性高。",
    "魁罡" to "主刚毅果断、聪明有威，性情刚烈，女命婚姻宜慎。",
    "金舆" to "主富贵、车马之荣，得配偶或长辈之助。",
    "红艳" to "主风情、异性缘旺、魅力强，也主才艺。",
    "流霞" to "女命主产厄、血光，孕期宜慎。",
    "日贵" to "主品性端庄、有贵人助，一生较为顺遂。",
    "德秀贵人" to "主聪明秀气、温文有礼，才华与德行兼备。"
)

/** 六十甲子纳音简明知识（按纳音名）。 */
private val NAYIN_KNOWLEDGE = mapOf(
    "海中金" to "金藏于水，主内敛深沉、藏而不露，需时机淬炼方显其贵。",
    "炉中火" to "火居炉中，主热情内蓄、可塑性高，得木助则旺。",
    "大林木" to "木之繁茂，主生机旺盛、胸怀宽广，宜成林立业。",
    "路旁土" to "土在路边，主宽厚包容、任劳任怨，需草木养护。",
    "剑锋金" to "金之锋锐，主刚毅果决、锋芒毕露，宜裁决、武职。",
    "山头火" to "火照山头，主光明磊落、志向高远，气势宏大。",
    "涧下水" to "水出山涧，主清澈灵动、性情柔和，绵延不绝。",
    "城头土" to "土筑城垣，主坚实可靠、有防御力，宜守护之职。",
    "白蜡金" to "金之温润，主外柔内刚、体态清秀，需火炼成器。",
    "杨柳木" to "木之柔韧，主适应力强、性情温和，随风而立。",
    "泉中水" to "水涌清泉，主聪慧明澈、源源不断，润物无声。",
    "屋上土" to "土覆屋宇，主庇护他人、稳重实在，宜安居乐业。",
    "霹雳火" to "火之猛烈，主刚烈果断、声威显赫，行动力强。",
    "松柏木" to "木之坚挺，主坚贞不屈、傲骨铮铮，耐寒不凋。",
    "长流水" to "水之绵长，主聪慧灵活、意志连绵，善积小流。",
    "平地木" to "木生平地，主平顺扎实、生生不息，宜稳步发展。",
    "壁上土" to "土附墙壁，主依附性强、注重外表，需依附根基。",
    "金箔金" to "金之轻薄，主外表华美、内质轻柔，需衬托方显。",
    "佛灯火" to "火如灯烛，主温和有礼、照亮他人，默默奉献。",
    "天河水" to "水在高天，主清高远大、胸怀广阔，超凡脱俗。",
    "大驿土" to "土通大道，主通达四方、包容万物，宜交通贸易。",
    "钗钏金" to "金饰之美，主精致华贵、性情温柔，宜装饰珠宝。",
    "桑柘木" to "木之柔韧，主蚕桑之利、温厚务实，宜纺织养生。",
    "大溪水" to "水汇成溪，主奔流向前、思路开阔，气势渐盛。",
    "沙中土" to "土杂沙砾，主散布四方、根基较浅，需水木养护。",
    "天上火" to "火悬天上，主光明普照、气势恢宏，恩泽广泛。",
    "石榴木" to "木结石榴，主多子多福、热情如火，鲜艳夺目。",
    "大海水" to "水之浩渺，主胸怀宽广、气度不凡，包容万象。"
)

/** 命盘页四柱表的一行：行标签 + 4 列（颜色/字号由回调决定） */
@Composable
private fun BzTableRow(label: String, values: List<String>,
                       detailType: String? = null, onDetail: ((BzDetailTarget) -> Unit)? = null,
                       style: (String) -> Pair<Color, androidx.compose.ui.unit.TextUnit>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(0.72f), textAlign = TextAlign.Center)
        values.forEach { v ->
            val st = style(v)
            val clickable = if (detailType != null && onDetail != null && v.isNotBlank() && v != "无")
                Modifier.clickable { onDetail(BzDetailTarget(detailType, v)) } else Modifier
            Text(v, color = st.first, fontSize = st.second, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f).then(clickable), textAlign = TextAlign.Center)
        }
    }
}

/**
 * 逐级下钻大运网格（对齐 ji.js.cn 详盘）：
 * 大运行(10步) → 点某步显示其 10 流年 → 点某流年显示其 12 流月 → 点某流月显示其流日。
 * 未选中任何下层时，该层隐藏；选中后在其下方展开，保持"点哪级出哪级"。
 */
@Composable
private fun BzDayunDrill(c: BaziChart.Chart, f: BaziFull.Data, onSelect: (Int, Int) -> Unit = { _, _ -> }) {
    val dayGan = c.pillars[2].gan
    var expanded by remember { mutableStateOf(false) }   // 大运开关：false=逐级下钻，true=树状全展开(原版图1)
    var selDy by remember { mutableIntStateOf(-1) }      // 选中的大运索引；-2=小运；-1 未选
    var selLn by remember { mutableIntStateOf(-1) }      // 选中的流年索引
    var selYue by remember { mutableIntStateOf(-1) }     // 选中的流月索引
    var selRi by remember { mutableIntStateOf(-1) }      // 选中的流日索引

    // 小运阶段(1-5岁)流年，点击小运时展开
    val xiaoyunLn = remember(f) { BaziCascade.xiaoyunLiunian(f.qiYun, f.dayun, dayGan) }
    // 当前选中的流年列表（大运 or 小运）
    val curLiunian: List<BaziFull.LiuNian> = when {
        selDy == -2 -> xiaoyunLn
        selDy in f.dayun.indices -> f.dayun[selDy].liunian
        else -> emptyList()
    }
    val doToggle: (Boolean) -> Unit = { v ->
        expanded = v
        selDy = -1; selLn = -1; selYue = -1; selRi = -1
    }

    Column(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 8.dp)) {
        if (expanded) {
            // ===== 专业细盘（原版）：大运/始于/流年/止于/流月 树状全展开 =====
            BzTreeAll(f = f, xiaoyunLn = xiaoyunLn, dayGan = dayGan, onToggle = doToggle)
        } else {
            // ===== 逐级下钻模式（原版图2）：点哪级出哪级 =====
            DrillRow(label = "大运", switch = { BzMiniSwitch(checked = false, onToggle = doToggle) }) {
                DrillHeadCell(head1 = "${f.qiYun.startYear}", head2 = "1-5岁", gan = "小", zhi = "运",
                    selected = selDy == -2,
                    onClick = { selDy = -2; selLn = -1; selYue = -1; selRi = -1 })
                f.dayun.forEachIndexed { i, dy ->
                    DrillHeadCell(head1 = "${dy.startYear}", head2 = "${dy.startAge}岁",
                        gan = dy.gan, zhi = dy.zhi, ganSs = dy.ganSs, zhiSs = dy.zhiSs,
                        selected = selDy == i,
                        onClick = { selDy = i; selLn = 0; selYue = -1; selRi = -1; onSelect(i, 0) })
                }
            }
            // 流年
            if (curLiunian.isNotEmpty()) {
                DrillRow(label = "流年") {
                    curLiunian.forEachIndexed { j, ln ->
                        DrillSubCell(head = "${ln.year}", gan = ln.gan, zhi = ln.zhi,
                            ganWx = ln.ganWx, zhiWx = ln.zhiWx, ganSs = ln.ganSs, zhiSs = ln.zhiSs,
                        selected = selLn == j,
                        onClick = { selLn = j; selYue = -1; selRi = -1; onSelect(selDy, j) })
                    }
                }
            }
            // 流月
            if (selLn in curLiunian.indices) {
                val ln = curLiunian[selLn]
                val yueList = remember(selDy, selLn) { BaziCascade.liuYue(ln, dayGan) }
                DrillRow(label = "流月") {
                    yueList.forEachIndexed { k, yue ->
                        DrillSubCell(head = yue.head, sub = yue.sub, gan = yue.gan, zhi = yue.zhi,
                            ganWx = yue.ganWx, zhiWx = yue.zhiWx, ganSs = yue.ganSs, zhiSs = yue.zhiSs,
                            selected = selYue == k,
                            onClick = { selYue = k; selRi = -1 })
                    }
                }
            }
            // 流日
            if (selLn in curLiunian.indices && selYue in 0..11) {
                val ln = curLiunian[selLn]
                val riList = remember(selDy, selLn, selYue) { BaziCascade.liuRi(ln, selYue, dayGan) }
                DrillRow(label = "流日") {
                    riList.forEachIndexed { d, ri ->
                        DrillSubCell(head = ri.head, sub = ri.sub, gan = ri.gan, zhi = ri.zhi,
                            ganWx = ri.ganWx, zhiWx = ri.zhiWx, ganSs = ri.ganSs, zhiSs = ri.zhiSs,
                            selected = selRi == d,
                            onClick = { selRi = d })
                    }
                }
            }
        }
    }
}

/** 专业细盘一列：大运头 + 始于 + 流年区 + 止于 */
private data class ProCol(
    val head1: String, val head2: String,
    val gan: String, val zhi: String, val ganSs: String, val zhiSs: String,
    val isSmall: Boolean, val liunian: List<BaziFull.LiuNian>,
    val cs: String, val endYear: Int
)

private const val PRO_HEAD_H = 82   // 大运头高度
private const val PRO_BEGIN_H = 40  // 始于行高度
private const val PRO_FLOW_H = 336  // 流年区高度(12格*28)
private const val PRO_END_H = 44    // 止于行高度

/** 专业细盘（原版 lab.yxbug 专业细盘）：大运/始于/流年/止于/流月 树状全展开 */
@Composable
private fun BzTreeAll(f: BaziFull.Data, xiaoyunLn: List<BaziFull.LiuNian>, dayGan: String, onToggle: (Boolean) -> Unit) {
    val cols = remember(f, xiaoyunLn, dayGan) {
        val list = mutableListOf<ProCol>()
        if (xiaoyunLn.isNotEmpty()) {
            val flow = xiaoyunLn.toMutableList()
            f.dayun.firstOrNull()?.liunian?.firstOrNull()?.let { flow.add(it) } // 交界年(起运年)
            list.add(ProCol(
                "${xiaoyunLn[0].year}", "1-5岁", "小", "运", "", "", true, flow,
                JishiyuBase.queryShengwang(dayGan, xiaoyunLn[0].zhi),
                xiaoyunLn[0].year + f.qiYun.startYear
            ))
        }
        f.dayun.forEachIndexed { i, dy ->
            val flow = dy.liunian.toMutableList()
            f.dayun.getOrNull(i + 1)?.liunian?.firstOrNull()?.let { flow.add(it) } // 交界年
            list.add(ProCol(
                "${dy.startYear}", "${dy.startAge}岁", dy.gan, dy.zhi, dy.ganSs, dy.zhiSs, false, flow,
                JishiyuBase.queryShengwang(dayGan, dy.liunian[0].zhi),
                dy.startYear + 10
            ))
        }
        list
    }
    var sel by remember { mutableIntStateOf(0) }
    val selLn = cols.getOrNull(sel)?.liunian?.firstOrNull()
    val yue = remember(selLn, dayGan) { if (selLn != null) BaziCascade.liuYue(selLn, dayGan) else emptyList() }

    Column(Modifier.fillMaxWidth().background(Color.White)) {
        // ===== 大运/始于/流年/止于 主表 =====
        Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFECECEC))) {
            // 左侧固定标签列
            Column(Modifier.width(34.dp).fillMaxHeight().background(Color(0xFFF6F3EC)),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.height(PRO_HEAD_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("大", color = Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("运", color = Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    BzMiniSwitch(checked = true, onToggle = onToggle)
                }
                Box(Modifier.height(PRO_BEGIN_H.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("始于", color = Color(0xFF666666), fontSize = 11.sp)
                }
                Box(Modifier.height(PRO_FLOW_H.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("流年", color = Color(0xFF666666), fontSize = 11.sp)
                }
                Box(Modifier.height(PRO_END_H.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("止于", color = Color(0xFF666666), fontSize = 11.sp)
                }
            }
            // 右侧横滚列
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                cols.forEachIndexed { idx, col ->
                    ProTreeColumn(col = col, selected = idx == sel, onClick = { sel = idx })
                }
            }
        }
        // ===== 流月行：其他月份 + 立冬/大雪/小寒 =====
        Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFECECEC))) {
            Box(Modifier.width(34.dp).height(98.dp).background(Color(0xFFF6F3EC)), contentAlignment = Alignment.Center) {
                Text("流月", color = Color(0xFF666666), fontSize = 11.sp)
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(50.dp).height(98.dp).background(Color(0xFFF5F5F5))
                    .border(0.5.dp, Color(0xFFE8E8E8)), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    listOf("其","他","月","份").forEach { Text(it, color = Color(0xFF888888), fontSize = 12.sp) }
                    Text("?", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                }
                for (mi in 9..11) {
                    val m = yue.getOrNull(mi)
                    if (m != null) ProYueCell(m = m, dayGan = dayGan)
                }
            }
        }
    }
}

/** 专业细盘的一列 */
@Composable
private fun ProTreeColumn(col: ProCol, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(52.dp).clickable(onClick = onClick)
        .background(if (selected) Color(0xFFFDECEA) else Color.White)
        .border(0.5.dp, Color(0xFFECECEC)), horizontalAlignment = Alignment.CenterHorizontally) {
        // 大运头
        Column(Modifier.height(PRO_HEAD_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(col.head1, color = Color(0xFF888888), fontSize = 10.sp, lineHeight = 12.sp)
            Text(col.head2, color = Color(0xFF888888), fontSize = 10.sp, lineHeight = 12.sp)
            if (col.isSmall) {
                Text("小", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("运", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(col.gan, color = WX_COLOR_DETAIL[GAN_WX_OF(col.gan)] ?: BZ_RED, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(col.ganSs, color = Color(0xFFC62828), fontSize = 9.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(col.zhi, color = WX_COLOR_DETAIL[JishiyuBase.ZHI_WUXING[col.zhi] ?: ""] ?: BZ_RED, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(col.zhiSs, color = Color(0xFFC62828), fontSize = 9.sp)
                }
            }
            Text(col.cs, color = Color(0xFF999999), fontSize = 10.sp)
        }
        // 始于（年份 + 首年流年干支）
        val begin = col.liunian.firstOrNull()
        Column(Modifier.height(PRO_BEGIN_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(if (begin != null) "${begin.year}" else "", color = Color(0xFF888888), fontSize = 10.sp)
            if (begin != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(begin.gan, color = WX_COLOR_DETAIL[begin.ganWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(begin.zhi, color = WX_COLOR_DETAIL[begin.zhiWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }
        // 流年区（竖排干支，带五行色）
        Column(Modifier.height(PRO_FLOW_H.dp).fillMaxWidth()) {
            col.liunian.forEach { ln ->
                Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(ln.gan, color = WX_COLOR_DETAIL[ln.ganWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Text(ln.zhi, color = WX_COLOR_DETAIL[ln.zhiWx] ?: BZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }
        // 止于（年份 + 长生）
        Column(Modifier.height(PRO_END_H.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("${col.endYear}", color = Color(0xFF888888), fontSize = 10.sp)
            Text(col.cs, color = Color(0xFF666666), fontSize = 12.sp)
        }
    }
}

/** 流月格：节气名 + 月/日 + 干支带十神 + 长生 */
@Composable
private fun ProYueCell(m: BaziCascade.Cell, dayGan: String) {
    val cs = JishiyuBase.queryShengwang(dayGan, m.zhi)
    Column(Modifier.width(56.dp).height(98.dp).background(Color(0xFFF6F3EC))
        .border(0.5.dp, Color(0xFFE8E8E8)), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(m.head, color = Color(0xFF444444), fontSize = 12.sp)
        Text(m.sub, color = Color(0xFF888888), fontSize = 10.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.gan, color = WX_COLOR_DETAIL[m.ganWx] ?: BZ_RED, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(m.ganSs, color = Color(0xFFC62828), fontSize = 9.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.zhi, color = WX_COLOR_DETAIL[m.zhiWx] ?: BZ_RED, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(m.zhiSs, color = Color(0xFFC62828), fontSize = 9.sp)
        }
        if (cs.isNotEmpty()) Text(cs, color = Color(0xFF888888), fontSize = 10.sp)
    }
}

/** 红色小开关（仿原版大运行开关） */
@Composable
private fun BzMiniSwitch(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier.size(width = 24.dp, height = 13.dp).clip(RoundedCornerShape(7.dp))
            .background(if (checked) BZ_RED else Color(0xFFC9C9C9))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 1.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(Color.White))
    }
}

/** 下钻表格的一行：左侧竖排行标签(可带开关) + 横向滚动的格子 */
@Composable
private fun DrillRow(label: String, switch: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFECECEC))) {
        Column(Modifier.width(30.dp).background(Color(0xFFF7F4EC)).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (switch != null) Arrangement.Center else Arrangement.Center) {
            Text(label, color = Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            if (switch != null) { Spacer(Modifier.height(4.dp)); switch() }
        }
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) { content() }
    }
}

/** 大运行格子：顶部两年份信息 + 干支带十神（选中红框高亮） */
@Composable
private fun DrillHeadCell(
    head1: String, head2: String, gan: String, zhi: String,
    ganSs: String = "", zhiSs: String = "",
    selected: Boolean, onClick: () -> Unit
) {
    Column(
        Modifier.width(46.dp).clickable(onClick = onClick)
            .background(if (selected) Color(0xFFFDECEA) else Color.White)
            .then(if (selected) Modifier.border(1.5.dp, BZ_RED) else Modifier.border(0.5.dp, Color(0xFFEEEEEE)))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(head1, color = Color(0xFF888888), fontSize = 9.sp, lineHeight = 11.sp)
        Text(head2, color = Color(0xFF888888), fontSize = 9.sp, lineHeight = 11.sp)
        Spacer(Modifier.height(2.dp))
        if (zhi == "运" && gan == "小") {
            Text("小", color = Color(0xFF666666), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("运", color = Color(0xFF666666), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(gan, color = WX_COLOR_DETAIL[GAN_WX_OF(gan)] ?: Color(0xFF222222), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(ganSs, color = Color(0xFFC62828), fontSize = 8.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(zhi, color = WX_COLOR_DETAIL[JishiyuBase.ZHI_WUXING[zhi] ?: ""] ?: Color(0xFF222222), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(zhiSs, color = Color(0xFFC62828), fontSize = 8.sp)
            }
        }
    }
}

/** 流年/流月/流日格子：表头(head/sub) + 干支带十神 */
@Composable
private fun DrillSubCell(
    head: String, sub: String = "", gan: String, zhi: String,
    ganWx: String, zhiWx: String, ganSs: String, zhiSs: String,
    selected: Boolean, onClick: () -> Unit
) {
    Column(
        Modifier.width(46.dp).clickable(onClick = onClick)
            .background(if (selected) Color(0xFFFDECEA) else Color.White)
            .then(if (selected) Modifier.border(1.5.dp, BZ_RED) else Modifier.border(0.5.dp, Color(0xFFEEEEEE)))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(head, color = Color(0xFF777777), fontSize = 10.sp, lineHeight = 12.sp)
        if (sub.isNotEmpty()) Text(sub, color = Color(0xFFAAAAAA), fontSize = 9.sp, lineHeight = 11.sp)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(gan, color = WX_COLOR_DETAIL[ganWx] ?: Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(ganSs, color = Color(0xFFC62828), fontSize = 8.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(zhi, color = WX_COLOR_DETAIL[zhiWx] ?: Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(zhiSs, color = Color(0xFFC62828), fontSize = 8.sp)
        }
    }
}

/** 主页右上角的小开关（视觉，仿原版 胎命身/串宫） */
@Composable
private fun BzToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onToggle(!checked) }) {
        Text(label, color = BZ_RED, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Box(Modifier.size(width = 34.dp, height = 18.dp).clip(RoundedCornerShape(9.dp))
            .background(if (checked) BZ_RED else Color(0xFFCCCCCC)), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(50)).background(Color.White)
                .padding(start = if (checked) 18.dp else 2.dp))
        }
    }
}

@Composable private fun BzNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(BZ_CARD).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("‹ 返回", color = BZ_GOLD, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f)); Spacer(Modifier.width(52.dp))
    }
}

@Composable private fun BzField(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label：", color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(88.dp))
        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(BZ_CARD)
            .border(1.dp, BZ_LINE, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) { content() }
    }
}

@Composable private fun BzRadioLine(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label：", color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(88.dp))
        options.forEachIndexed { idx, name ->
            Row(Modifier.clickable { onSelect(idx) }.padding(end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(17.dp).clip(RoundedCornerShape(50))
                    .background(if (selected == idx) BZ_RED else Color.Transparent)
                    .then(if (selected == idx) Modifier else Modifier.border(1.dp, BZ_LINE, RoundedCornerShape(50))),
                    contentAlignment = Alignment.Center) {
                    if (selected == idx) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
                }
                Spacer(Modifier.width(5.dp))
                Text(name, color = if (selected == idx) BZ_GOLD else AppColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable private fun BzInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(70.dp))
        Text(value, color = Color(0xFF222222), fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable private fun BzSection(title: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(15.dp).background(BZ_RED))
        Spacer(Modifier.width(8.dp))
        Text(title, color = BZ_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun baziFaceText(c: BaziChart.Chart, q: String, createdAt: String) = buildString {
    appendLine("八 字 排 盘"); appendLine("命主：$q"); appendLine("时间：$createdAt")
    appendLine("四柱：${c.baziText}"); appendLine("农历：${c.lunar} · ${c.shengXiao}${c.gender}")
    appendLine("十神：${c.pillars.joinToString(" ") { it.shiShen }}")
    appendLine("纳音：${c.pillars.joinToString(" ") { it.naYin }}")
    appendLine("胎元${c.taiYuan} 命宫${c.mingGong} 命卦${c.mingGua}")
    appendLine("旺衰：${c.analysis.wangshuai} 用神：${c.analysis.yongshen.joinToString("、")}")
    appendLine("骨重：${c.chenggu.weight}")
}.trim()

/** 基本页完整复制文本。 */
private fun baziBasicText(c: BaziChart.Chart, f: BaziFull.Data?) = buildString {
    appendLine("【八字排盘 · 基本信息】")
    appendLine("农历：${f?.nongliFull ?: c.lunar}  ${if (c.gender == "男") "乾造" else "坤造"}")
    appendLine("北京时间：${c.solar}    真太阳时：${c.solar}")
    appendLine("出生地区：${f?.birthArea ?: ""}")
    appendLine("出生节气：${f?.jieqiFull ?: c.jieQi}")
    appendLine("命卦：${(f?.mingGuaDir ?: "")} ${c.mingGua}")
    appendLine()
    appendLine("四柱：${c.baziText}")
    appendLine("十神：${c.pillars.joinToString("  ") { it.shiShen }}")
    appendLine("纳音：${c.pillars.joinToString("  ") { it.naYin }}")
    appendLine("胎元 ${c.taiYuan} · 胎息 ${c.taiXi} · 命宫 ${c.mingGong} · 身宫 ${c.shenGong}")
    appendLine()
    appendLine("【五行统计（含藏干）】")
    val qty = f?.wuXingQty ?: c.wuXing.toMap()
    val dayWx = GAN_WX_OF(c.pillars[2].gan)
    for (wx in listOf("木", "火", "土", "金", "水")) {
        appendLine("${wx}：${qty[wx] ?: 0} 个（${shishenCategory(wx, dayWx)}）")
    }
    appendLine()
    appendLine("【旺衰用神】${c.analysis.wangshuai}  用神：${c.analysis.yongshen.joinToString("、")}")
    appendLine("【袁天罡称骨】${c.chenggu.weight}  ${c.chenggu.verse}")
    if (f != null && f.wangShuai.isNotEmpty()) {
        val labels = listOf("旺", "相", "休", "囚", "死")
        appendLine("【五行旺衰】${f.wangShuai.mapIndexed { i, wx -> wx + labels[i] }.joinToString("  ")}")
    }
}.trim()

/** 命盘页完整复制文本（四柱表全部行 + 旺衰 + 留意）。 */
private fun baziMingpanText(c: BaziChart.Chart, f: BaziFull.Data?) = buildString {
    appendLine("【八字排盘 · 命盘】")
    appendLine("${c.lunar}  ${if (c.gender == "男") "乾造" else "坤造"}  北京时间：${c.solar}")
    val names = listOf("年柱", "月柱", "日柱", "时柱")
    fun row(label: String, values: List<String>) = appendLine("$label：${values.mapIndexed { i, v -> "${names[i]} $v" }.joinToString("  ")}")
    row("十神", c.pillars.map { it.shiShen })
    row("天干", c.pillars.map { it.gan })
    row("地支", c.pillars.map { it.zhi })
    row("藏干", c.pillars.map { it.cangGan })
    row("藏干十神", c.pillars.map { it.cangShen })
    row("地势", c.pillars.map { it.deShi })
    row("自坐", c.pillars.map { it.ziZuo })
    row("空亡", c.pillars.map { it.kongWang })
    row("纳音", c.pillars.map { it.naYin })
    row("神煞", c.pillars.map { it.shenSha })
    if (f != null) {
        appendLine()
        val labels = listOf("旺", "相", "休", "囚", "死")
        appendLine("【五行旺衰】${f.wangShuai.mapIndexed { i, wx -> wx + labels[i] }.joinToString("  ")}")
        if (f.liuYi.tiangan.isNotEmpty()) appendLine("【天干留意】${f.liuYi.tiangan.joinToString("、")}")
        if (f.liuYi.dizhi.isNotEmpty()) appendLine("【地支留意】${f.liuYi.dizhi.joinToString("、")}")
    }
}.trim()

/** 详盘页完整复制文本（原局列 + 大运/流年/流月/流日/小运八列盘 + 起运 + 逐级大运 + 旺衰留意）。 */
private fun baziDetailText(c: BaziChart.Chart, f: BaziFull.Data?, xp: BaziXiangpan.Data?) = buildString {
    appendLine("【八字排盘 · 详盘】")
    appendLine("${c.lunar}  ${if (c.gender == "男") "乾造" else "坤造"}  北京时间：${c.solar}")
    if (xp != null) {
        appendLine()
        fun colBlock(title: String, col: BaziXiangpan.Col) {
            appendLine("[$title] ${col.ganzhi}  十神:${col.ganShen}  藏干:${col.cangGan}(${col.cangShen})")
            appendLine("  地势:${col.deShi}  自坐:${col.ziZuo}  空亡:${col.kongWang}  纳音:${col.naYin}")
            val ss = col.shenSha.split("、", "·").filter { it.isNotBlank() && it != "无" }
            if (ss.isNotEmpty()) appendLine("  神煞:${ss.joinToString("、")}")
        }
        colBlock("年柱", xp.year); colBlock("月柱", xp.month)
        colBlock("日柱", xp.day); colBlock("时柱", xp.hour)
        appendLine()
        colBlock("流年${xp.liunianYear}", xp.liunian)
        appendLine("司令：${xp.siLing}")
        if (xp.liuYi.tiangan.isNotEmpty()) appendLine("【天干留意】${xp.liuYi.tiangan.joinToString("、")}")
        if (xp.liuYi.dizhi.isNotEmpty()) appendLine("【地支留意】${xp.liuYi.dizhi.joinToString("、")}")
    }
    if (f != null) {
        appendLine()
        appendLine("起运：出生${f.qiYun.startYear}年${f.qiYun.startMonth}月${f.qiYun.startDay}天${f.qiYun.startHour}时后起运")
        appendLine("交运：逢${f.qiYun.jiaoyunGan1}、${f.qiYun.jiaoyunGan2}年 ${f.qiYun.jieName}后${f.qiYun.days}天交大运")
        if (f.dayun.isNotEmpty()) {
            appendLine("【大运流年】")
            for (dy in f.dayun) {
                appendLine("  ${dy.ganzhi} ${dy.startYear}年起(${dy.startAge}虚岁)  ${dy.ganSs}${dy.zhiSs}")
                for (ln in dy.liunian) appendLine("    ${ln.year} ${ln.ganzhi} ${ln.age}岁")
            }
        }
        val labels = listOf("旺", "相", "休", "囚", "死")
        if (f.wangShuai.isNotEmpty()) appendLine("【五行旺衰】${f.wangShuai.mapIndexed { i, wx -> wx + labels[i] }.joinToString("  ")}")
    }
}.trim()

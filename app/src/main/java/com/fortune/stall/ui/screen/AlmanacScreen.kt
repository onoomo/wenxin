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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.jishiyu.LunarAlmanac
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val AL_RED = Color(0xFFD0342C)       // 顶栏中国红
private val AL_RED_DARK = Color(0xFFB82822)
private val AL_TEXT = Color(0xFF222222)
private val AL_SUB = Color(0xFF6B6B6B)
private val AL_LINE = Color(0xFFEDEDED)
private val AL_LUCKY = Color(0xFF1E9E4A)     // 宜 绿
private val AL_UNLUCKY = Color(0xFFC0392B)   // 忌 红
private val AL_GOLD = Color(0xFFB8862B)

/**
 * 老黄历专用日历页（仿原版图1）：红顶日期左右切换 + 当日黄历详情 + 十二时辰宜忌。
 * 不走排盘流程，直接进入即可浏览、翻日。
 */
@Composable
fun LaoHuangLiScreen(onBack: () -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var selectedHour by remember { mutableStateOf((java.time.LocalDateTime.now().hour + 1) / 2 % 12) }

    val day = remember(date) { LunarAlmanac.paipan(date.atTime(12, 0)) }
    val hourCells = remember(day.dayGz) {
        LunarAlmanac.hourCells(day.dayGz[0].toString(), day.dayGz[1].toString())
    }
    val hourDetail = remember(day.dayGz, selectedHour) { LunarAlmanac.hourDetail(day.dayGz, selectedHour) }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // ===== 红色顶栏 =====
            Column(Modifier.fillMaxWidth().background(AL_RED)) {
                Spacer(Modifier.statusBarsPadding())
                // 第一行：图标 + 标题 + 今
                Row(
                    Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("‹", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable(onClick = onBack))
                    Spacer(Modifier.width(14.dp))
                    Text("分享", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { })
                    Spacer(Modifier.weight(1f))
                    Text("老黄历", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(0.8f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("今", color = Color.White, fontSize = 13.sp, modifier = Modifier.clickable {
                            date = LocalDate.now()
                            selectedHour = (java.time.LocalDateTime.now().hour + 1) / 2 % 12
                        })
                    }
                }
                // 第二行：日期左右切换
                Row(
                    Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("‹", color = Color.White, fontSize = 30.sp, modifier = Modifier
                        .width(40.dp).clickable { date = date.minusDays(1) }, textAlign = TextAlign.Start)
                    Text("${date.year}年${date.monthValue}月${date.dayOfMonth}日 星期${day.weekCn}",
                        color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("›", color = Color.White, fontSize = 30.sp, modifier = Modifier
                        .width(40.dp).clickable { date = date.plusDays(1) }, textAlign = TextAlign.End)
                }
            }

            // ===== 农历大字 + 四柱 =====
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val lunarBig = (if (day.isLeap) "闰" else "") + monthCn(day.lunarMonth) + "月" + dayCn(day.lunarDay)
                Text(lunarBig, color = AL_RED, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                Text(day.baziText, color = AL_TEXT, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            }

            // ===== 宜 / 忌 =====
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                YiJiRow(true, day.yi)
                Spacer(Modifier.height(8.dp))
                YiJiRow(false, day.ji)
            }
            Spacer(Modifier.height(14.dp))

            // ===== 信息宫格 =====
            Column(Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                // 4列：廿八星宿/纳音五行/黄道/十二神
                GridRow4(
                    listOf(
                        "廿八星宿" to (day.xiu + day.animal),
                        "纳音五行" to day.naYin,
                        day.zhiShenType to day.zhiShen,
                        "十二神" to (day.zhiXing + "日")
                    )
                )
                // 4列：财神/喜神/福神/阳贵
                GridRow4(
                    listOf(
                        "财神" to day.caiShen,
                        "喜神" to day.xiShen,
                        "福神" to day.fuShen,
                        "阳贵" to day.yangGui
                    )
                )
                Spacer(Modifier.height(1.dp).fillMaxWidth().background(AL_LINE))
                // 三格：今日胎神 / 彭祖百忌 / 冲煞
                Row(Modifier.fillMaxWidth()) {
                    InfoCell("今日胎神", day.taiShenText, Modifier.weight(1f))
                    InfoCell("彭祖百忌", day.pengZu.replace("\n", " "), Modifier.weight(1.4f))
                    InfoCell("冲煞", day.chongSha, Modifier.weight(1f))
                }
                Spacer(Modifier.height(1.dp).fillMaxWidth().background(AL_LINE))
                // 三格：吉神宜趋 / 凶神宜忌
                Row(Modifier.fillMaxWidth()) {
                    InfoCell("吉神宜趋", day.jiShen.take(6).joinToString(" "), Modifier.weight(1f))
                    InfoCell("凶神宜忌", day.xiongSha.take(6).joinToString(" "), Modifier.weight(1f))
                }
            }

            // ===== 十二时辰宜忌表 =====
            Spacer(Modifier.height(6.dp))
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                // 表头行：时辰 / 天干
                Row(Modifier.fillMaxWidth().background(Color(0xFFFBF3F3)).height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("时辰", color = AL_UNLUCKY, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f)) {
                        hourCells.forEach { c ->
                            Text(c.gan, color = AL_TEXT, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }
                // 地支行
                Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("宜忌", color = AL_UNLUCKY, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f)) {
                        hourCells.forEachIndexed { i, c ->
                            val sel = i == selectedHour
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(4.dp))
                                .background(if (sel) AL_RED else Color.Transparent)
                                .clickable { selectedHour = i }, contentAlignment = Alignment.Center) {
                                Text(c.zhi, color = if (sel) Color.White else AL_TEXT, fontSize = 13.sp)
                            }
                        }
                    }
                }
                // 吉凶行
                Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(44.dp))
                    Row(Modifier.weight(1f)) {
                        hourCells.forEachIndexed { i, c ->
                            val sel = i == selectedHour
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(4.dp))
                                .background(if (sel) AL_RED else Color.Transparent)
                                .clickable { selectedHour = i }, contentAlignment = Alignment.Center) {
                                Text(c.luck, color = if (c.luck == "吉") AL_LUCKY else AL_UNLUCKY, fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
                // 选中时辰指针
                Row(Modifier.fillMaxWidth().height(20.dp)) {
                    Spacer(Modifier.width(44.dp))
                    Row(Modifier.weight(1f)) {
                        hourCells.forEachIndexed { i, _ ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (i == selectedHour) Text("▲", color = AL_RED, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ===== 选中时辰详情 =====
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.dp, bottom = 24.dp)) {
                Text("${hourDetail.ganZhi}时 ${hourDetail.range} ${hourDetail.luck}",
                    color = AL_TEXT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("${hourDetail.chongSha}    财神${hourDetail.caiShen} 喜神${hourDetail.xiShen} 福神${hourDetail.fuShen} 阳贵${hourDetail.yangGui}",
                    color = AL_SUB, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                YiJiRow(true, hourDetail.yi, compact = true)
                Spacer(Modifier.height(6.dp))
                YiJiRow(false, hourDetail.ji, compact = true)
            }
        }
    }
}

/** 宜/忌 一行（绿色/红色圆形图标 + 文本） */
@Composable
private fun YiJiRow(isYi: Boolean, items: List<String>, compact: Boolean = false) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(if (compact) 20.dp else 24.dp).clip(RoundedCornerShape(50))
                .background(if (isYi) AL_LUCKY else AL_UNLUCKY),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isYi) "宜" else "忌", color = Color.White, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        val txt = if (items.isEmpty()) "无" else items.take(12).joinToString(" · ")
        Text(txt, color = AL_TEXT, fontSize = if (compact) 12.sp else 13.5.sp,
            lineHeight = if (compact) 18.sp else 22.sp, modifier = Modifier.weight(1f))
    }
}

/** 4列均分信息格（标题红/值黑） */
@Composable
private fun GridRow4(items: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth().height(66.dp)) {
        items.forEach { (label, value) ->
            Column(Modifier.weight(1f).fillMaxSize().border(0.5.dp, AL_LINE)
                .padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(label, color = AL_RED, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(value, color = AL_TEXT, fontSize = 12.5.sp, textAlign = TextAlign.Center,
                    maxLines = 2, lineHeight = 15.sp)
            }
        }
    }
}

/** 信息格（标题红/值黑，可跨列） */
@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.height(76.dp).border(0.5.dp, AL_LINE).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(label, color = AL_RED, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, color = AL_TEXT, fontSize = 12.sp, textAlign = TextAlign.Center,
            maxLines = 3, lineHeight = 15.sp)
    }
}

private fun monthCn(m: Int): String = arrayOf("","正","二","三","四","五","六","七","八","九","十","十一","十二").getOrElse(m) { "$m" }
private fun dayCn(d: Int): String {
    if (d == 10) return "初十"
    if (d == 20) return "二十"
    if (d == 30) return "三十"
    val head = arrayOf("","初","十","廿","卅")
    val digits = arrayOf("","一","二","三","四","五","六","七","八","九")
    return when {
        d < 10 -> "初" + digits[d]
        d < 20 -> "十" + digits[d - 10]
        d < 30 -> "廿" + digits[d - 20]
        else -> "卅" + digits[d - 30]
    }
}

/**
 * 万年历专用日历页（仿原版图2）：红顶月份左右切换 + 整月日历 + 选中日四柱宜忌详情。
 */
@Composable
fun WanNianLiScreen(onBack: () -> Unit) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }

    // 选中日始终落在当前显示月份：翻月时同步到该月1号（若跨月）
    fun shiftMonth(delta: Int) {
        val next = ym.plusMonths(delta.toLong())
        ym = next
        if (selected.monthValue != next.monthValue || selected.year != next.year) {
            selected = if (delta > 0) next.atDay(1) else next.atEndOfMonth()
        }
    }

    val day = remember(selected) { LunarAlmanac.paipan(selected.atTime(12, 0)) }
    val hourCells = remember(day.dayGz) {
        LunarAlmanac.hourCells(day.dayGz[0].toString(), day.dayGz[1].toString())
    }
    var selectedHour by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // ===== 红色顶栏 =====
            Column(Modifier.fillMaxWidth().background(AL_RED)) {
                Spacer(Modifier.statusBarsPadding())
                Row(
                    Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("‹", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable(onClick = onBack))
                    Spacer(Modifier.width(14.dp))
                    Text("分享", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { })
                    Spacer(Modifier.weight(1f))
                    Text("万年历", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(0.8f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("今", color = Color.White, fontSize = 13.sp, modifier = Modifier.clickable {
                            ym = YearMonth.now(); selected = LocalDate.now()
                        })
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("‹", color = Color.White, fontSize = 30.sp, modifier = Modifier
                        .width(40.dp).clickable { shiftMonth(-1) }, textAlign = TextAlign.Start)
                    Text("${ym.year}年${ym.monthValue}月", color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("›", color = Color.White, fontSize = 30.sp, modifier = Modifier
                        .width(40.dp).clickable { shiftMonth(1) }, textAlign = TextAlign.End)
                }
            }

            // ===== 星期表头 =====
            Row(Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("日","一","二","三","四","五","六").forEach {
                    Text(it, color = AL_SUB, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }

            // ===== 月历网格 =====
            val firstDow = ym.atDay(1).dayOfWeek.value % 7 // 周日=0
            val daysInMonth = ym.lengthOfMonth()
            val cells = remember(ym) {
                val list = mutableListOf<LocalDate?>()
                repeat(firstDow) { list.add(null) }
                for (d in 1..daysInMonth) list.add(ym.atDay(d))
                while (list.size % 7 != 0) list.add(null)
                list
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().height(60.dp)) {
                        week.forEach { dt ->
                            Box(Modifier.weight(1f).fillMaxSize().padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (dt == selected) AL_RED else Color.Transparent)
                                .clickable { if (dt != null) selected = dt },
                                contentAlignment = Alignment.Center) {
                                if (dt != null) {
                                    val isSel = dt == selected
                                    val weekend = dt.dayOfWeek.value >= 6
                                    val fest = LunarAlmanac.festivalShort(dt)
                                    val lunarDt = remember(dt) { java.time.temporal.ChronoUnit.DAYS.between(
                                        LocalDate.of(2000, 1, 7), dt).toInt() }
                                    val gz = com.fortune.stall.util.LunarCalendar.ganZhi(lunarDt)
                                    val lunar = remember(dt) { com.fortune.stall.util.LunarCalendar.fromDateTime(dt.atTime(12,0)) }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${dt.dayOfMonth}", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else if (fest.isNotEmpty() || weekend) AL_RED else AL_TEXT)
                                        val subLabel = fest.ifEmpty {
                                            if (lunar.day == 1) monthCn(lunar.month) + "月"
                                            else dayCn(lunar.day)
                                        }
                                        Text(subLabel, fontSize = 9.5.sp, color = if (isSel) Color.White else if (fest.isNotEmpty()) AL_RED else AL_SUB,
                                            maxLines = 1, lineHeight = 11.sp)
                                        Text(gz, fontSize = 9.sp, color = if (isSel) Color.White.copy(0.9f) else AL_GOLD, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== 地支选择圆点 =====
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                com.fortune.stall.util.LunarCalendar.DIZHI.forEachIndexed { i, z ->
                    val sel = i == selectedHour
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(50))
                        .background(if (sel) AL_RED else Color.Transparent)
                        .border(1.dp, if (sel) AL_RED else AL_RED.copy(0.5f), RoundedCornerShape(50))
                        .clickable { selectedHour = i }, contentAlignment = Alignment.Center) {
                        Text(z, color = if (sel) Color.White else AL_RED, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(1.dp).fillMaxWidth().background(AL_LINE))

            // ===== 选中日详情 =====
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${day.solarYear}年${day.solarMonth}月${day.solarDay}日 星期${day.weekCn}",
                        color = AL_SUB, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Row {
                        listOf("年","月","日","时").forEach {
                            Text(it, color = AL_RED, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text((if (day.isLeap) "闰" else "") + monthCn(day.lunarMonth) + "月" + dayCn(day.lunarDay),
                            color = AL_TEXT, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Spacer(Modifier.height(4.dp))
                        Text(day.festivals.firstOrNull() ?: "-", color = AL_SUB, fontSize = 13.sp)
                    }
                    // 四柱 + 十神简称
                    val riGan = day.dayGz[0].toString()
                    val pillars = listOf(day.yearGz, day.monthGz, day.dayGz, day.timeGz)
                    Row {
                        pillars.forEachIndexed { idx, gz ->
                            val gan = gz[0].toString(); val zhi = gz[1].toString()
                            val ganSh = if (idx == 2) "元" else com.fortune.stall.engine.jishiyu.Shishen.jc(
                                com.fortune.stall.engine.jishiyu.Shishen.query(gan, riGan))
                            val zhiSh = com.fortune.stall.engine.jishiyu.Shishen.jc(
                                com.fortune.stall.engine.jishiyu.Shishen.query(cangGan(zhi), riGan))
                            Column(Modifier.width(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$gan$ganSh", color = AL_TEXT, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                                Text("$zhi$zhiSh", color = AL_TEXT, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                YiJiRow(true, day.yi, compact = true)
                Spacer(Modifier.height(6.dp))
                YiJiRow(false, day.ji, compact = true)
                // 道教节日
                val tao = day.festivals.filter { it.contains("戊") || it in listOf("三会日","三元日","五腊日","天赦日","八节","八会") }
                if (tao.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(AL_LUCKY))
                        Spacer(Modifier.width(6.dp))
                        Text("(道)${tao.joinToString(" ")}", color = AL_SUB, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/** 地支藏干（本气），用于四柱地支十神 */
private fun cangGan(zhi: String): String = when (zhi) {
    "子" -> "癸"; "丑" -> "己"; "寅" -> "甲"; "卯" -> "乙"; "辰" -> "戊"; "巳" -> "丙"
    "午" -> "丁"; "未" -> "己"; "申" -> "庚"; "酉" -> "辛"; "戌" -> "戊"; "亥" -> "壬"
    else -> "甲"
}

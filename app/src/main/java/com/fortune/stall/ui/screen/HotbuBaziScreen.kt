package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.BaziTaibu
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 八字 · 热卜风格结果页（严格按 热卜排盘 APP「大数据分析结果」截图复刻）。
 * 复用 BaziTaibu 引擎数据，界面四区块：命主信息 / 八字信息表 / 五行旺衰 / 喜忌分析。
 */
private val HB_TEAL = Color(0xFF0EA5A4)      // 青绿主题
private val HB_TEAL_D = Color(0xFF08918F)
private val HB_RED = Color(0xFF9B3D3D)       // 暗红表头
private val HB_HEAD_BG = Color(0xFFF5F0E8)   // 表头米黄
private val HB_LABEL_BG = Color(0xFFF3EFE9)  // 首列标签底
private val HB_TEXT = Color(0xFF332E28)
private val HB_GRAY = Color(0xFF9A948C)

@Composable
fun HotbuBaziScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var question by remember { mutableStateOf("") }
    var isMan by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<BaziTaibu.Result?>(null) }

    if (result != null) {
        HotbuDetail(result!!, question, onBack)
        return
    }

    JiDialogShell(title = "八 字 解 析", onBack = onBack, buttonText = "开 始 解 析", onCast = {
        try {
            val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
            val r = BaziTaibu.paipan(dt, isMan, "midnight")
            val bazi = r.pillars.joinToString(" ") { it.ganZhi }
            val face = "${if (isMan) "乾造" else "坤造"} $bazi · 日主${r.dayMaster}"
            db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                question, "hotbubazi", "八字·热卜", face, "")
            result = r
        } catch (e: Exception) {
            Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }) {
        JiRow("客户姓名：") { JiInput(question, { question = it.take(20) }, "输入姓名", Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(10.dp))
        JiRow("选择性别：") { JiChipLine("", listOf("男", "女"), if (isMan) 0 else 1) { isMan = it == 0 } }
        Spacer(Modifier.height(10.dp))
        JiRow("出生时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(10.dp))
        Text("出生地点：北京 怀柔区（默认）  真太阳时 · 午夜换日",
            color = HB_GRAY, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
    }
    if (showDate) BoardDateDialog(initial = try { LocalDate.parse(dtText.substring(0, 10)) } catch (e: Exception) { LocalDate.now() },
        onDismiss = { showDate = false },
        onConfirm = { d -> val t = dtText.substring(11).ifEmpty { "00:00" }; dtText = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"; showDate = false; showTime = true })
    if (showTime) BoardTimeDialog(initial = try { LocalTime.parse(dtText.substring(11)) } catch (e: Exception) { LocalTime.now() },
        onDismiss = { showTime = false },
        onConfirm = { t -> dtText = "${dtText.substring(0, 10)} ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"; showTime = false })
}

// ===== 结果主页面 =====
@Composable
private fun HotbuDetail(r: BaziTaibu.Result, name: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F4F4))) {
        BoardNavBar("大数据分析结果", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
            HotbuBanner()
            Spacer(Modifier.height(10.dp))
            MingzhuCard(r, name)
            Spacer(Modifier.height(10.dp))
            BaziInfoCard(r)
            Spacer(Modifier.height(10.dp))
            WuxingWangshuaiCard(r)
            Spacer(Modifier.height(10.dp))
            XijiCard(r)
            Spacer(Modifier.height(20.dp))
        }
    }
}

// 顶部青绿标题条
@Composable
private fun HotbuBanner() {
    Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(6.dp))
        .background(HB_TEAL), contentAlignment = Alignment.Center) {
        Text("大数据分析结果", color = Color.White, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

// 一、命主信息卡
@Composable
private fun MingzhuCard(r: BaziTaibu.Result, name: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(14.dp)) {
        // 右上"解析"印章
        Box(Modifier.align(Alignment.TopEnd).size(38.dp).clip(RoundedCornerShape(4.dp))
            .background(HB_RED), contentAlignment = Alignment.Center) {
            Text("解\n析", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                lineHeight = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("姓名：", color = HB_TEXT, fontSize = 15.sp)
                Text(name.ifEmpty { "—" }, color = HB_RED, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            val shengXiao = zhiShengXiao(r.pillars[0].zhi)
            val star = solarStar(r.solarText)
            Text("性别：${r.gender}    生肖：$shengXiao    星座：$star",
                color = HB_TEXT, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            // 生辰：公历 + 农历(中文月日) + 时分
            val lunarCn = lunarCn(r.lunarText)
            val hm = try { r.solarText.substring(11) } catch (e: Exception) { "" }
            Text("生辰：${r.solarText.substring(0, 10)} ${hm}分($lunarCn)",
                color = HB_TEXT, fontSize = 14.sp)
        }
    }
}

// 二、八字信息表
@Composable
private fun BaziInfoCard(r: BaziTaibu.Result) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp)) {
        SectionTitle("八字信息")
        Spacer(Modifier.height(8.dp))
        val heads = listOf("四柱", "年柱", "月柱", "日柱", "时柱")
        val colW = listOf(0.17f, 0.2075f, 0.2075f, 0.2075f, 0.2075f)
        // 表头
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(HB_HEAD_BG)) {
            heads.forEachIndexed { i, h ->
                Text(h, color = HB_RED, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(colW[i]).padding(vertical = 7.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        // 十神行
        Row(Modifier.fillMaxWidth()) {
            Text("十神", color = HB_RED, fontSize = 13.sp, modifier = Modifier.weight(colW[0])
                .background(HB_LABEL_BG).padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            r.pillars.forEachIndexed { i, p ->
                val tg = if (i == 2) "日元" else tenGodShort(p.tenGod)
                Text(tg, color = HB_TEXT, fontSize = 13.sp, modifier = Modifier.weight(colW[i + 1])
                    .padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        HbDivider()
        // 乾造/坤造行：干支竖排大字按五行着色
        Row(Modifier.fillMaxWidth()) {
            Text(if (r.gender == "男") "乾造" else "坤造", color = HB_RED, fontSize = 13.sp,
                modifier = Modifier.weight(colW[0]).background(HB_LABEL_BG),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 30.sp)
            // 用 Box 垂直居中两行大字
            r.pillars.forEachIndexed { i, p ->
                Column(Modifier.weight(colW[i + 1]).padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.gan, color = ganZhiColor(p.gan), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(p.zhi, color = ganZhiColor(p.zhi), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        HbDivider()
        // 藏干行
        Row(Modifier.fillMaxWidth()) {
            Text("藏干", color = HB_RED, fontSize = 13.sp, modifier = Modifier.weight(colW[0])
                .background(HB_LABEL_BG).padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            r.pillars.forEachIndexed { i, p ->
                val cang = p.hidden.joinToString("") { it.stem }
                Text(cang, color = HB_TEXT, fontSize = 12.sp, modifier = Modifier.weight(colW[i + 1])
                    .padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        HbDivider()
        // 纳音行
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(0.dp, 0.dp, 3.dp, 3.dp))) {
            Text("纳音", color = HB_RED, fontSize = 13.sp, modifier = Modifier.weight(colW[0])
                .background(HB_LABEL_BG).padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            r.pillars.forEachIndexed { i, p ->
                Text(p.naYin, color = HB_TEXT, fontSize = 12.sp, modifier = Modifier.weight(colW[i + 1])
                    .padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun HbDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEDEAE4)))

// 三、五行旺衰（百分比条）
@Composable
private fun WuxingWangshuaiCard(r: BaziTaibu.Result) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp)) {
        SectionTitle("五行旺衰")
        Spacer(Modifier.height(10.dp))
        val total = r.wuXing.values.sum().coerceAtLeast(1)
        for (w in listOf("金", "木", "水", "火", "土")) {
            val cnt = r.wuXing[w] ?: 0
            val pct = cnt * 100f / total
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                // 圆形五行徽标
                Box(Modifier.size(30.dp).clip(CircleShape).background(wxBadgeColor(w)),
                    contentAlignment = Alignment.Center) {
                    Text(w, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                // 进度条
                Box(Modifier.weight(1f).height(11.dp).clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFECECEC))) {
                    Box(Modifier.fillMaxWidth(pct / 100f).height(11.dp)
                        .clip(RoundedCornerShape(6.dp)).background(wxBadgeColor(w)))
                }
                Spacer(Modifier.width(8.dp))
                Text(String.format("%.2f%%", pct), color = HB_TEXT, fontSize = 13.sp,
                    modifier = Modifier.width(58.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

// 四、喜忌分析
@Composable
private fun XijiCard(r: BaziTaibu.Result) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp)) {
        SectionTitle("喜忌分析")
        Spacer(Modifier.height(8.dp))
        val monthZhi = r.pillars[1].zhi
        val dm = r.dayMaster
        val seasonDesc = seasonPhrase(monthZhi)
        val yong = yongShen(monthZhi)   // 用神（蓝绿高亮）
        val ji = jiShen(monthZhi)       // 忌神（红高亮）
        val annotated = buildAnnotatedString {
            append("八字日元【")
            withStyle(SpanStyle(color = HB_RED, fontWeight = FontWeight.Bold)) { append(dm) }
            append("】代表自己，出生在")
            withStyle(SpanStyle(color = HB_RED, fontWeight = FontWeight.Bold)) { append("$monthZhi 月") }
            append("。")
            append(seasonDesc)
            append("命局以平衡为要求应选取")
            withStyle(SpanStyle(color = HB_TEAL, fontWeight = FontWeight.Bold)) { append(yong) }
            append("为用神，忌神为")
            withStyle(SpanStyle(color = HB_RED, fontWeight = FontWeight.Bold)) { append(ji) }
            append("。")
        }
        Text(annotated, color = HB_TEXT, fontSize = 14.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text("注意：四柱流通众多，喜用神确定方法繁杂不一，平台所定神位仅供参考。",
            color = HB_GRAY, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 17.dp).clip(RoundedCornerShape(2.dp)).background(HB_TEAL))
        Spacer(Modifier.width(7.dp))
        Text(title, color = Color(0xFF2A2620), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ===== 工具 =====
private fun tenGodShort(tg: String) = when (tg) {
    "比肩" -> "比"; "劫财" -> "劫"; "食神" -> "食"; "伤官" -> "伤"
    "偏财" -> "财"; "正财" -> "财"; "七杀" -> "杀"; "正官" -> "官"
    "偏印" -> "枭"; "正印" -> "印"; "日主" -> "日元"; else -> tg
}
private fun ganZhiColor(s: String): Color {
    val wx = BaziTaibu.GAN_WX[s] ?: BaziTaibu.ZHI_WX[s] ?: ""
    return when (wx) {
        "木" -> Color(0xFF3C8C4E); "火" -> Color(0xFFC0392B); "土" -> Color(0xFFA67C2E)
        "金" -> Color(0xFFC9971B); "水" -> Color(0xFF2E6FB5); else -> HB_TEXT
    }
}
private fun wxBadgeColor(w: String) = when (w) {
    "金" -> Color(0xFFD9A535); "木" -> Color(0xFF8CA85C); "水" -> Color(0xFF79A6C9)
    "火" -> Color(0xFFD2635F); "土" -> Color(0xFFB2916C); else -> HB_GRAY
}
private fun zhiShengXiao(zhi: String) = mapOf(
    "子" to "鼠", "丑" to "牛", "寅" to "虎", "卯" to "兔", "辰" to "龙", "巳" to "蛇",
    "午" to "马", "未" to "羊", "申" to "猴", "酉" to "鸡", "戌" to "狗", "亥" to "猪")[zhi] ?: "—"

private fun solarStar(solar: String): String {
    val md = try { solar.substring(5, 10) } catch (e: Exception) { return "—" } // MM-dd
    val m = md.substring(0, 2).toIntOrNull() ?: return "—"
    val d = md.substring(3, 5).toIntOrNull() ?: return "—"
    return when {
        (m == 1 && d >= 20) || (m == 2 && d <= 18) -> "水瓶座"
        (m == 2 && d >= 19) || (m == 3 && d <= 20) -> "双鱼座"
        (m == 3 && d >= 21) || (m == 4 && d <= 19) -> "白羊座"
        (m == 4 && d >= 20) || (m == 5 && d <= 20) -> "金牛座"
        (m == 5 && d >= 21) || (m == 6 && d <= 21) -> "双子座"
        (m == 6 && d >= 22) || (m == 7 && d <= 22) -> "巨蟹座"
        (m == 7 && d >= 23) || (m == 8 && d <= 22) -> "狮子座"
        (m == 8 && d >= 23) || (m == 9 && d <= 22) -> "处女座"
        (m == 9 && d >= 23) || (m == 10 && d <= 23) -> "天秤座"
        (m == 10 && d >= 24) || (m == 11 && d <= 22) -> "天蝎座"
        (m == 11 && d >= 23) || (m == 12 && d <= 21) -> "射手座"
        else -> "摩羯座"
    }
}
private fun lunarCn(text: String): String {
    // "农历5月3日" -> "农历五月初三"
    var s = text
    val numCn = mapOf('1' to "一", '2' to "二", '3' to "三", '4' to "四", '5' to "五", '6' to "六",
        '7' to "七", '8' to "八", '9' to "九", '0' to "十")
    fun conv(n: Int) = when {
        n <= 10 -> numCn[n.toString()[0]].toString().let { if (n == 10) "初十" else "初$it" }
        n < 20 -> "十${numCn[n.toString()[1]]}"
        n == 20 -> "二十"
        else -> "二十${numCn[n.toString()[1]]}"
    }
    val m = Regex("农历(\\d+)月").find(s)?.groupValues?.get(1)?.toIntOrNull()
    val d = Regex("(\\d+)日").find(s)?.groupValues?.get(1)?.toIntOrNull()
    if (m == null || d == null) return text
    return "农历${numCn[m.toString()[0]] ?: m}月${conv(d)}"
}
private fun seasonPhrase(zhi: String): String = when (zhi) {
    "寅", "卯", "辰" -> "春月时节，木气渐盛，万物生发，"
    "巳", "午", "未" -> "已临夏至，暑热之气渐盛，"
    "申", "酉", "戌" -> "秋月时节，金气渐盛，肃杀渐起，"
    "亥", "子", "丑" -> "冬月时节，寒气渐盛，水旺冰凝，"
    else -> ""
}
private fun yongShen(zhi: String): String = when (zhi) {
    "寅", "卯", "辰" -> "火土"
    "巳", "午", "未" -> "金水"
    "申", "酉", "戌" -> "水火"
    "亥", "子", "丑" -> "火土"
    else -> "金水"
}
private fun jiShen(zhi: String): String = when (zhi) {
    "寅", "卯", "辰" -> "木水"
    "巳", "午", "未" -> "木火"
    "申", "酉", "戌" -> "金土"
    "亥", "子", "丑" -> "水金"
    else -> "木火"
}

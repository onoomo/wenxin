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
import com.fortune.stall.engine.jishiyu.YiZhangJing
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val YZ_RED = Color(0xFFB0392E)
private val YZ_GOLD = Color(0xFFD9A441)
private val YZ_LINE = Color(0x52D9A441)
private val YZ_CARD = Color(0x14FFFFFF)
private val YZ_SS = Color(0xFFC0392B) // 神煞红
private val YZ_LD = Color(0xFF16A085) // 六道青

/** 达摩一掌经专用起卦页 */
@Composable
fun YizhangCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var isMan by remember { mutableStateOf(true) }
    var sunni by remember { mutableStateOf(1) }
    var showDate by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Triple<LocalDateTime, Boolean, Int>?>(null) }

    if (detail == null) {
        JiDialogShell(title = "达 摩 一 掌 经", onBack = onBack, onCast = {
            try {
                val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
                detail = Triple(dt, isMan, sunni)
                val id = db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "达摩一掌经 · ${if (isMan) "男" else "女"}", "yizhangjing",
                    if (isMan) "男命" else "女命", yzFaceText(dt, isMan, sunni), "一掌经")
                Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }) {
            JiRow("出生时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(12.dp))
            JiRow("性别：") { JiSeg(listOf("男", "女"), if (isMan) 0 else 1) { isMan = it == 0 } }
            Spacer(Modifier.height(14.dp))
            JiChipLine("顺逆方式：", listOf("男顺女逆", "阳男阴女顺，阴男阳女逆"), sunni - 1) { sunni = it + 1 }
        }
        if (showDate) JiDateTimeDialog(
            initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
            onDismiss = { showDate = false },
            onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false }
        )
    } else {
        val (dt, man, sn) = detail!!
        YizhangDetailScreen(dt = dt, isMan = man, sunni = sn, onBack = { detail = null })
    }
}

private fun yzFaceText(dt: LocalDateTime, isMan: Boolean, sunni: Int): String {
    val r = YiZhangJing.cast(dt, isMan, sunni)
    return buildString {
        append("时间：${r.dateText}\n节气：${r.jieqi}\n")
        append("四柱：${r.siZhu.joinToString(" ")}\n")
        append("年宫 ${r.nian.zhi}·${r.nian.shensha}  月宫 ${r.yue.zhi}·${r.yue.shensha}\n")
        append("日宫 ${r.ri.zhi}·${r.ri.shensha}  时宫 ${r.shi.zhi}·${r.shi.shensha}\n")
    }
}

@Composable
private fun YzNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = YZ_GOLD, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 18.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(30.dp))
    }
}

@Composable
private fun YzChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(10.dp))
        .background(if (selected) Color(0x2AD9A441) else YZ_CARD)
        .then(if (selected) Modifier.border(1.dp, YZ_LINE, RoundedCornerShape(10.dp)) else Modifier)
        .clickable(onClick = onClick).padding(horizontal = 26.dp, vertical = 12.dp)) {
        Text(label, color = if (selected) YZ_GOLD else AppColors.Paper, fontSize = 14.sp)
    }
}

@Composable
private fun YzChoiceWide(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(if (selected) Color(0x2AD9A441) else YZ_CARD)
        .then(if (selected) Modifier.border(1.dp, YZ_LINE, RoundedCornerShape(10.dp)) else Modifier)
        .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, color = if (selected) YZ_GOLD else AppColors.Paper, fontSize = 13.sp)
    }
}

/** 仿原版达摩一掌经详情页 */
@Composable
private fun YizhangDetailScreen(dt: LocalDateTime, isMan: Boolean, sunni: Int, onBack: () -> Unit) {
    var offset by remember(dt) { mutableStateOf(0) }
    var help by remember { mutableStateOf<Pair<String, String>?>(null) } // title, content
    val curDt = remember(offset) { dt.plusHours((offset * 2).toLong()) }
    val r = remember(curDt) { YiZhangJing.cast(curDt, isMan, sunni) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        Box(Modifier.fillMaxWidth().background(YZ_RED).padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text("达摩一掌经", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(28.dp))
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp)) {
            YzInfoRow("时间", r.dateText)
            YzInfoRow("节气", r.jieqi)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (gz in r.siZhu) Text(gz, color = Color(0xFF222222), fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))

        // 十二地支宫盘（仿原版 4x4 环绕 + 中心）
        YzGrid(r, onHelp = { t, c -> help = t to c })
        Spacer(Modifier.height(12.dp))

        // 翻时辰
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YzPill("上一时辰", Modifier.weight(1f)) { offset -= 1 }
            YzPill("下一时辰", Modifier.weight(1f)) { offset += 1 }
        }
        Spacer(Modifier.height(12.dp))

        // 四宫详解卡片
        YzCard("年宫", r.nian, "年") { help = "神煞 · ${r.nian.shensha}" to (YiZhangJing.SHENSHA_INFO[r.nian.shensha] ?: "") }
        YzCard("月宫", r.yue, "月") { help = "神煞 · ${r.yue.shensha}" to (YiZhangJing.SHENSHA_INFO[r.yue.shensha] ?: "") }
        YzCard("日宫", r.ri, "日") { help = "神煞 · ${r.ri.shensha}" to (YiZhangJing.SHENSHA_INFO[r.ri.shensha] ?: "") }
        YzCard("时宫", r.shi, "时") { help = "神煞 · ${r.shi.shensha}" to (YiZhangJing.SHENSHA_INFO[r.shi.shensha] ?: "") }

        // 看法总纲
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(8.dp))
            .background(Color.White).padding(14.dp)) {
            Text("看法总纲", color = YZ_RED, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("年为前四世，月为前三世，日为前二世，时为前一世。\n" +
                "年为根基，论祖业之兴废。月为提纲，断兄弟之有无。\n" +
                "日论夫妻，主中年之造化。时为子息，定晚年之荣枯。\n" +
                "四柱看命，以时为主，逢吉则吉，逢凶则凶。",
                color = Color(0xFF444444), fontSize = 13.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(faceText = yzFaceText(dt, isMan, sunni))
        Spacer(Modifier.height(24.dp))
    }
    help?.let { (t, c) -> YzHelpDialog(t, c) { help = null } }
}

@Composable
private fun YzInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Text(value, color = Color(0xFF222222), fontSize = 12.5.sp)
    }
}

/** 十二地支环绕宫盘 */
@Composable
private fun YzGrid(r: YiZhangJing.Result, onHelp: (String, String) -> Unit) {
    // 原版布局：上排 巳午未申 / 中排 辰 [中心] 酉 / 下排 卯戌 / 底排 寅丑子亥
    val top = arrayOf("巳", "午", "未", "申")
    val mid = arrayOf("辰", "酉")
    val low = arrayOf("卯", "戌")
    val bot = arrayOf("寅", "丑", "子", "亥")
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(8.dp))
        .background(Color.White).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        YzGridRow(top, r, onHelp)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.weight(1f)) { YzCell(mid[0], r, onHelp) }
            YzCenter(r, onHelp)
            Box(Modifier.weight(1f)) { YzCell(mid[1], r, onHelp) }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            Box(Modifier.width(70.dp)) { YzCell(low[0], r, onHelp) }
            Spacer(Modifier.width(150.dp))
            Box(Modifier.width(70.dp)) { YzCell(low[1], r, onHelp) }
        }
        Spacer(Modifier.height(4.dp))
        YzGridRow(bot, r, onHelp)
    }
}

@Composable
private fun YzGridRow(zhi: Array<String>, r: YiZhangJing.Result, onHelp: (String, String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        for (z in zhi) Box(Modifier.weight(1f)) { YzCell(z, r, onHelp) }
    }
}

@Composable
private fun YzCell(zhi: String, r: YiZhangJing.Result, onHelp: (String, String) -> Unit) {
    val ss = YiZhangJing.SHEN_SHA[zhi] ?: ""
    val ld = YiZhangJing.LIUDAO[zhi] ?: ""
    Column(Modifier.padding(4.dp).clickable { onHelp("$zhi · $ss", YiZhangJing.SHENSHA_INFO[ss] ?: "") },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(zhi, color = Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(ss, color = YZ_SS, fontSize = 11.sp)
        Text(ld, color = YZ_LD, fontSize = 10.sp)
    }
}

@Composable
private fun YzCenter(r: YiZhangJing.Result, onHelp: (String, String) -> Unit) {
    val list = listOf(
        "年宫" to r.nian, "月宫" to r.yue, "日宫" to r.ri, "时宫" to r.shi
    )
    Column(Modifier.width(150.dp).border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp)).padding(6.dp)) {
        for ((name, g) in list) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color(0xFF999999), fontSize = 10.sp, modifier = Modifier.width(32.dp))
                Text(g.zhi, color = Color(0xFF222222), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(g.shensha, color = YZ_SS, fontSize = 11.sp, modifier = Modifier.clickable {
                    onHelp("神煞 · ${g.shensha}", YiZhangJing.SHENSHA_INFO[g.shensha] ?: "")
                })
                Text(g.liudao.take(2), color = YZ_LD, fontSize = 10.sp, modifier = Modifier.clickable {
                    onHelp("六道 · ${g.liudao}", YiZhangJing.LIUDAO_INFO[g.liudao] ?: "")
                })
            }
        }
    }
}

@Composable
private fun YzPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFF8A2A22), YZ_RED)))
        .clickable(onClick = onClick).padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun YzCard(title: String, g: YiZhangJing.Gong, field: String, onShensha: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(8.dp))
        .background(Color.White).padding(12.dp)) {
        Text(title, color = YZ_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${g.zhi} · ", color = Color(0xFF222222), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(g.shensha, color = YZ_SS, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onShensha))
            Spacer(Modifier.width(10.dp))
            Text(g.liudao, color = YZ_LD, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(g.gongInfo, color = Color(0xFF444444), fontSize = 12.5.sp, lineHeight = 19.sp)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun YzHelpDialog(title: String, content: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F1E8)).padding(20.dp)
            .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = YZ_RED, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(content, color = Color(0xFF333333), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(YZ_RED)
                .clickable(onClick = onDismiss).padding(horizontal = 28.dp, vertical = 10.dp)) {
                Text("知道了", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun YzDateDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var y by remember { mutableStateOf(initial.year) }; var m by remember { mutableStateOf(initial.monthValue) }; var d by remember { mutableStateOf(initial.dayOfMonth) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, YZ_LINE, RoundedCornerShape(16.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择日期", color = YZ_GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            YzStepper("年", y, 1800, 2199) { y = it }; YzStepper("月", m, 1, 12) { m = it }; YzStepper("日", d, 1, 31) { d = it }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(YZ_CARD).clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) { Text("取消", color = AppColors.Muted, fontSize = 14.sp) }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(YZ_RED).clickable {
                    val dim = java.time.YearMonth.of(y, m).lengthOfMonth(); onConfirm(LocalDate.of(y, m, kotlin.math.min(d, dim)))
                }.padding(horizontal = 24.dp, vertical = 10.dp)) { Text("确定", color = Color.White, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun YzTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var h by remember { mutableStateOf(initial.hour) }; var mi by remember { mutableStateOf(initial.minute) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, YZ_LINE, RoundedCornerShape(16.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择时间", color = YZ_GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            YzStepper("时", h, 0, 23) { h = it }; YzStepper("分", mi, 0, 59) { mi = it }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(YZ_CARD).clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) { Text("取消", color = AppColors.Muted, fontSize = 14.sp) }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(YZ_RED).clickable { onConfirm(LocalTime.of(h, mi)) }.padding(horizontal = 24.dp, vertical = 10.dp)) { Text("确定", color = Color.White, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun YzStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(36.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(YZ_CARD).clickable { onChange(kotlin.math.max(min, value - 1)) }.padding(12.dp)) { Text("−", color = YZ_GOLD, fontSize = 16.sp) }
        Spacer(Modifier.width(16.dp))
        Text("$value", color = Color.White, fontSize = 18.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(YZ_CARD).clickable { onChange(kotlin.math.min(max, value + 1)) }.padding(12.dp)) { Text("+", color = YZ_GOLD, fontSize = 16.sp) }
    }
}

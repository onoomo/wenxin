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
import com.fortune.stall.engine.jishiyu.NineStar
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val FX_RED = Color(0xFFB0392E)
private val FX_GOLD = Color(0xFFD9A441)
private val FX_LINE = Color(0x52D9A441)
private val FX_CARD = Color(0x14FFFFFF)
// 飞星颜色（仿原版 ji.js.cn）：年绿 月蓝 日黄 时紫
private val FX_YEAR = Color(0xFF1B9A5A)
private val FX_MONTH = Color(0xFF2E6FD0)
private val FX_DAY = Color(0xFFE08A1E)
private val FX_HOUR = Color(0xFF9B3FC0)
// 原版文字色：标题蓝灰、标签灰蓝
private val FX_TITLE_TEXT = Color(0xFF41566E)
private val FX_LABEL = Color(0xFF64748B)

/** 玄空飞星专用起卦页：日期时间 → 仿原版九宫飞星盘（合成 + 年月日时四个单盘） */
@Composable
fun FeixingCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var showDate by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<LocalDateTime?>(null) }

    if (detail == null) {
        JiDialogShell(title = "玄 空 飞 星", onBack = onBack, onCast = {
            try {
                val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
                detail = dt
                val id = db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "玄空飞星 · 排盘", "feixing", "玄空飞星", fxFaceText(dt), "飞星盘")
                Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }) {
            JiRow("选择时间：") {
                JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth())
            }
        }
        if (showDate) JiDateTimeDialog(
            initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
            onDismiss = { showDate = false },
            onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false }
        )
    } else {
        FeixingDetailScreen(dt = detail!!, onBack = { detail = null })
    }
}

private fun fxFaceText(dt: LocalDateTime): String {
    val p = NineStar.panData(dt)
    return buildString {
        append("日期：${p.dateText}\n节气：${p.jieqi}\n${p.yearYun}\n")
        append("四柱：${p.siZhu.joinToString(" ")}\n")
    }
}

@Composable
private fun FxNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = FX_GOLD, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 18.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(30.dp))
    }
}

/** 仿原版飞星详情页 */
@Composable
private fun FeixingDetailScreen(dt: LocalDateTime, onBack: () -> Unit) {
    var help by remember { mutableStateOf<String?>(null) }
    val p = remember(dt) { NineStar.panData(dt) }
    val visual = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        Box(Modifier.fillMaxWidth().background(FX_RED).padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text("玄空飞星", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(28.dp))
            }
        }

        // 日期 / 节气 / 四柱（仿原版紧凑排布）
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp)) {
            // 日期：公历 + 农历括号
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("日期", color = FX_LABEL, fontSize = 13.sp, modifier = Modifier.width(46.dp))
                Text(p.dateText + "(${p.lunarText})", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            // 节气：节气区间 + 右侧三元九运
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("节气", color = FX_LABEL, fontSize = 13.sp, modifier = Modifier.width(46.dp))
                    Text(p.jieqi, color = Color(0xFF1A1A1A), fontSize = 14.sp)
                }
                Text(p.yearYun, color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            // 四柱：竖排（每字一行），大字
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("四柱", color = FX_LABEL, fontSize = 13.sp, modifier = Modifier.width(46.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (gz in p.siZhu) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            for (ch in gz) Text(ch.toString(), color = Color(0xFF1A1A1A), fontSize = 22.sp,
                                fontFamily = FontFamily.Serif, lineHeight = 26.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // 飞星颜色纯文字图例：飞星颜色：年、月、日、时
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.Center) {
            Text("飞星颜色：", color = Color(0xFF333333), fontSize = 14.sp)
            Text("年", color = FX_YEAR, fontSize = 14.sp)
            Text("、", color = Color(0xFF333333), fontSize = 14.sp)
            Text("月", color = FX_MONTH, fontSize = 14.sp)
            Text("、", color = Color(0xFF333333), fontSize = 14.sp)
            Text("日", color = FX_DAY, fontSize = 14.sp)
            Text("、", color = Color(0xFF333333), fontSize = 14.sp)
            Text("时", color = FX_HOUR, fontSize = 14.sp)
        }
        Spacer(Modifier.height(4.dp))

        FxSectionTitle("飞星排盘")
        FxGrid(visual, p, mode = 0, onHelp = { help = it })
        Spacer(Modifier.height(10.dp))

        for (m in 1..4) {
            FxSectionTitle(when (m) { 1 -> "年飞星"; 2 -> "月飞星"; 3 -> "日飞星"; else -> "时飞星" })
            FxGrid(visual, p, mode = m, onHelp = { help = it })
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(faceText = fxFaceText(dt))
        Spacer(Modifier.height(16.dp))
    }
    help?.let { FxHelpDialog(it) { help = null } }
}

@Composable
private fun FxInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Text(value, color = Color(0xFF222222), fontSize = 13.sp)
    }
}

@Composable
private fun FxLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun FxSectionTitle(title: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
        .background(Color(0xFFF2F2F4)).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(title, color = FX_TITLE_TEXT, fontSize = 15.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun FxGrid(visual: Array<Array<String>>, p: NineStar.PanData, mode: Int, onHelp: (String) -> Unit) {
    val cellH = if (mode == 0) 108.dp else 92.dp
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(3.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(3.dp))
        .background(Color.White)) {
        visual.forEachIndexed { ri, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { ci, g ->
                    val stars = p.gongStars[g] ?: intArrayOf(0, 0, 0, 0)
                    FxCell(g, stars, mode, cellH, Modifier.weight(1f), onHelp)
                    if (ci < 2) Box(Modifier.width(1.dp).height(cellH).background(Color(0xFF333333)))
                }
            }
            if (ri < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF333333)))
        }
    }
}

@Composable
private fun FxCell(g: String, stars: IntArray, mode: Int, cellH: androidx.compose.ui.unit.Dp,
                   modifier: Modifier, onHelp: (String) -> Unit) {
    Column(modifier.height(cellH).clickable {
        val s = if (mode == 0) stars else intArrayOf(stars[mode - 1])
        onHelp("${g}宫：" + s.joinToString(" ") { NineStar.START_NAMES[it - 1] })
    }, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (mode == 0) {
            // 合成盘：规整 2×2 —— 左上绿(年) / 右上蓝(月) / 左下黄(日) / 右下紫(时)
            Column(Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(NineStar.START_CODES[stars[0] - 1], color = FX_YEAR,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(NineStar.START_CODES[stars[1] - 1], color = FX_MONTH,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(NineStar.START_CODES[stars[2] - 1], color = FX_DAY,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(NineStar.START_CODES[stars[3] - 1], color = FX_HOUR,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // 单盘：黑色星名文字（如「九紫」），仿原版
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val s = stars[mode - 1]
                Text(NineStar.START_NAMES[s - 1], color = Color(0xFF1A1A1A), fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun FxHelpDialog(text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val parts = text.split("：", limit = 2)
        val names = parts.getOrNull(1)?.split(" ") ?: emptyList()
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F1E8)).padding(20.dp)
            .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("飞星象意", color = FX_RED, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(parts.getOrNull(0) ?: "", color = Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            for (n in names) {
                val key = NineStar.START_NAMES.firstOrNull { n.contains(it) || it.contains(n) }
                if (key != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("【$key】${NineStar.HELPER[key] ?: ""}", color = Color(0xFF333333), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(FX_RED)
                .clickable(onClick = onDismiss).padding(horizontal = 28.dp, vertical = 10.dp)) {
                Text("知道了", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FxDateDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var y by remember { mutableStateOf(initial.year) }
    var m by remember { mutableStateOf(initial.monthValue) }
    var d by remember { mutableStateOf(initial.dayOfMonth) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, FX_LINE, RoundedCornerShape(16.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择日期", color = FX_GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            FxStepper("年", y, 1800, 2199) { y = it }
            FxStepper("月", m, 1, 12) { m = it }
            FxStepper("日", d, 1, 31) { d = it }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(FX_CARD).clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(FX_RED).clickable {
                    val dim = java.time.YearMonth.of(y, m).lengthOfMonth()
                    onConfirm(LocalDate.of(y, m, kotlin.math.min(d, dim)))
                }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun FxTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var h by remember { mutableStateOf(initial.hour) }
    var mi by remember { mutableStateOf(initial.minute) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, FX_LINE, RoundedCornerShape(16.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择时间", color = FX_GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            FxStepper("时", h, 0, 23) { h = it }
            FxStepper("分", mi, 0, 59) { mi = it }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(FX_CARD).clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(FX_RED).clickable { onConfirm(LocalTime.of(h, mi)) }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun FxStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(36.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(FX_CARD).clickable { onChange(kotlin.math.max(min, value - 1)) }.padding(12.dp)) {
            Text("−", color = FX_GOLD, fontSize = 16.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text("$value", color = Color.White, fontSize = 18.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(FX_CARD).clickable { onChange(kotlin.math.min(max, value + 1)) }.padding(12.dp)) {
            Text("+", color = FX_GOLD, fontSize = 16.sp)
        }
    }
}

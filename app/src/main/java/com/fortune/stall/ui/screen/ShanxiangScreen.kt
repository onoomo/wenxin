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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.Qimen
import com.fortune.stall.engine.jishiyu.ShanxiangQimen
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val SX_RED = Color(0xFFB0392E)
private val SX_GOLD = Color(0xFFD9A441)
private val SX_LINE = Color(0x52D9A441)
private val SX_CARD = Color(0x14FFFFFF)

/**
 * 山向奇门专用起卦页：选年份 + 输入度数(0-360) → 度数查 72 局 → 仿原版九宫盘详情页。
 */
@Composable
fun ShanxiangCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var year by remember { mutableStateOf(LocalDateTime.now().year) }
    var degreeText by remember { mutableStateOf("180") }
    var question by remember { mutableStateOf("") }
    var showYear by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    if (detail == null) {
        JiDialogShell(title = "山 向 奇 门", onBack = onBack, onCast = {
            val d = degreeText.toIntOrNull()
            if (d == null || d < 0 || d > 360) {
                Toast.makeText(ctx, "必须输入 0-360 之间的度数", Toast.LENGTH_SHORT).show()
            } else {
                detail = year to d
                val def = ShanxiangQimen.findByDegree(d)
                val q = question.ifBlank { "山向奇门 · ${def?.shan}山${def?.xiang}向" }
                val createdAt = "$year-01-01 00:00:00"
                val id = db.insert(createdAt, q, "shanxiangqimen", "${def?.shan}山${def?.xiang}向",
                    sxFaceText(year, d, q), "${year}年${d}度")
                Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
            }
        }) {
            JiRow("选择年份：") {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(JiForm.INPUT_BG)
                    .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp))
                    .clickable { showYear = true }.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Text("$year 年 ▾", color = JiForm.TEXT, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            JiRow("输入度数：") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(JiForm.SELECT_BG)
                        .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp)).clickable {
                            val v = degreeText.toIntOrNull() ?: 180; degreeText = kotlin.math.max(0, v - 1).toString()
                        }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text("−", color = JiForm.TEXT, fontSize = 16.sp) }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(70.dp).clip(RoundedCornerShape(6.dp)).background(JiForm.INPUT_BG)
                        .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp)).padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        BasicTextField(value = degreeText, onValueChange = { str -> degreeText = str.filter { c -> c.isDigit() }.take(3) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = JiForm.TEXT, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(JiForm.SELECT_BG)
                        .border(1.dp, JiForm.LINE, RoundedCornerShape(6.dp)).clickable {
                            val v = degreeText.toIntOrNull() ?: 180; degreeText = kotlin.math.min(360, v + 1).toString()
                        }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text("+", color = JiForm.TEXT, fontSize = 16.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            JiRow("预测事项：") { JiInput(question, { question = it }, "输入预测事项（选填）", Modifier.fillMaxWidth()) }
        }
        if (showYear) SxYearDialog(initial = year, onDismiss = { showYear = false }, onConfirm = { year = it; showYear = false })
    } else {
        val (y, d) = detail!!
        ShanxiangDetailScreen(year = y, degree = d, question = question.ifBlank { "输入预测事项" }, onBack = { detail = null })
    }
}

private fun sxFaceText(year: Int, degree: Int, q: String): String = buildString {
    val def = ShanxiangQimen.findByDegree(degree)
    append("事项：$q\n")
    append("山向：${def?.shan}山${def?.xiang}向（${degree}～${degree + 4}度）\n")
    append("年份：$year 年\n")
    append("局：${if (def != null && def.ju > 0) "阳遁" else "阴遁"}${def?.let { kotlin.math.abs(it.ju) }}局\n")
}

@Composable
private fun SxNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = SX_GOLD, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 18.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(30.dp))
    }
}

/** 山向奇门仿原版详情页：白底九宫盘 + 翻局 */
@Composable
private fun ShanxiangDetailScreen(year: Int, degree: Int, question: String, onBack: () -> Unit) {
    var deg by remember(degree) { mutableStateOf(degree) }
    var showHelp by remember { mutableStateOf<String?>(null) }
    val def = remember(deg) { ShanxiangQimen.findByDegree(deg) }
    // 山向奇门以当年立春午时起局（取当年公历 6 月 1 日 12:00 作稳定基准，节气月支由引擎处理）
    val dt = remember(year) { LocalDateTime.of(year, 6, 1, 12, 0) }
    val result = remember(deg, year) {
        try { Qimen.paipan(dt, true, shanDegree = deg) } catch (e: Exception) { null }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        Box(Modifier.fillMaxWidth().background(SX_RED).padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text("山向奇门", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(28.dp))
            }
        }

        if (result == null || def == null) {
            Text("排盘数据异常", color = Color(0xFF333333), fontSize = 14.sp, modifier = Modifier.padding(20.dp))
            return@Column
        }
        val r = result!!

        // 信息表
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp)) {
            SxInfoRow("事项", question)
            SxInfoRow("山向", "${def.shan}山${def.xiang}向  度数(${def.degree}～${def.degree + 4})")
            SxInfoRow("年份", "$year 年")
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("干支", color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(40.dp))
                Text(r.siZhu[2] + r.siZhu[3], color = Color(0xFF222222), fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Spacer(Modifier.width(10.dp))
                Text("黄泉", color = Color(0xFF888888), fontSize = 12.sp)
                Text(def.huangquan, color = Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${if (r.yangDun) "阳遁" else "阴遁"}${r.jushu}局", color = SX_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                for (h in listOf("值符", "值使", "旬首", "马星", "空亡")) {
                    Text(h, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                for (v in listOf(r.zhifu, r.zhishi, r.xunhead, r.maxing, r.kongwang)) {
                    Text(v, color = Color(0xFF2A6DB0), fontSize = 12.5.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // 九宫盘（含外围隐干一圈与四角红「马」）
        SxPanWithSurround(r) { showHelp = it }
        Spacer(Modifier.height(10.dp))

        Text("符号颜色：入墓(灰)  击刑(红)  门迫(黄)  刑+墓(蓝)", color = Color(0xFF888888),
            fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SxPill("上一局(-5)", Modifier.weight(1f)) { deg = kotlin.math.max(0, deg - 5) }
            SxPill("当前盘", Modifier.weight(1f)) { deg = degree }
            SxPill("下一局(+5)", Modifier.weight(1f)) { deg = kotlin.math.min(360, deg + 5) }
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(faceText = sxFaceText(year, deg, question))
        Spacer(Modifier.height(24.dp))
    }

    showHelp?.let { SxHelpDialog(it) { showHelp = null } }
}

@Composable
private fun SxInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Text(value, color = Color(0xFF222222), fontSize = 13.sp)
    }
}

@Composable
private fun SxPanWithSurround(r: Qimen.Result, onHelp: (String) -> Unit) {
    val edge = 24.dp // 外围标注列宽
    val cellH = 140.dp
    val lineC = Color(0xFFBBBBBB)
    val yinganC = Color(0xFF222222)
    val visual = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))
    // 顶部马（巽 左上 / 坤 右上）与顶部隐干（离）
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(Modifier.width(edge), contentAlignment = Alignment.BottomCenter) {
            if (r.pan["巽"]?.ma == true) Text("马", color = SX_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            val c = r.pan["离"]
            Text((c?.yingan ?: "") + (c?.yinganJi ?: ""), color = yinganC,
                fontSize = 16.sp, fontFamily = FontFamily.Serif)
        }
        Box(Modifier.width(edge), contentAlignment = Alignment.BottomCenter) {
            if (r.pan["坤"]?.ma == true) Text("马", color = SX_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
    // 中部：左隐干列 + 九宫格 + 右隐干列
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        // 左列（巽/震/艮）
        Column(Modifier.width(edge).height(424.dp)) {
            for (g in listOf("巽", "震", "艮")) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val c = r.pan[g]
                    Text((c?.yingan ?: "") + (c?.yinganJi ?: ""), color = yinganC,
                        fontSize = 16.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
        // 九宫格
        Column(Modifier.weight(1f)
            .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF666666), RoundedCornerShape(6.dp))
            .background(Color.White)) {
            visual.forEachIndexed { ri, row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEachIndexed { ci, g ->
                        val cell = r.pan[g] ?: Qimen.Cell()
                        SxCellView(g, cell, Modifier.weight(1f)) { onHelp(it) }
                        if (ci < 2) Box(Modifier.width(1.dp).height(cellH).background(lineC))
                    }
                }
                if (ri < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(lineC))
            }
        }
        // 右列（坤/兑/乾）
        Column(Modifier.width(edge).height(424.dp)) {
            for (g in listOf("坤", "兑", "乾")) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val c = r.pan[g]
                    Text((c?.yingan ?: "") + (c?.yinganJi ?: ""), color = yinganC,
                        fontSize = 16.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
    // 底部马（艮 左下 / 乾 右下）与底部隐干（坎）
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(Modifier.width(edge), contentAlignment = Alignment.TopCenter) {
            if (r.pan["艮"]?.ma == true) Text("马", color = SX_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            val c = r.pan["坎"]
            Text((c?.yingan ?: "") + (c?.yinganJi ?: ""), color = yinganC,
                fontSize = 16.sp, fontFamily = FontFamily.Serif)
        }
        Box(Modifier.width(edge), contentAlignment = Alignment.TopCenter) {
            if (r.pan["乾"]?.ma == true) Text("马", color = SX_RED, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SxCellView(gong: String, c: Qimen.Cell, modifier: Modifier, onHelp: (String) -> Unit) {
    Column(modifier.height(140.dp).padding(horizontal = 3.dp, vertical = 5.dp)
        .clickable { onHelp("${gong}宫：天盘 ${c.tianpan} ${c.jiuxing}，地盘 ${c.dipan} ${c.men}，八神 ${c.bashen}") },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text(c.bashen.ifEmpty { " " }, color = Color(0xFF333333), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (gong == "中") {
            Text(c.dipan, color = Color(0xFFAAAAAA), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("${c.yingan}${sxFullStar(c.jiuxing)}  ${c.tianpan}",
                color = Color(0xFF222222), fontSize = 12.sp)
            Text("${sxFullMen(c.men)}  ${c.dipan}", color = Color(0xFF222222), fontSize = 12.sp)
        }
    }
}

private fun sxFullStar(s: String) = when (s) {
    "蓬" -> "天蓬"; "芮" -> "天芮"; "冲" -> "天冲"; "辅" -> "天辅"; "禽" -> "天禽"
    "心" -> "天心"; "柱" -> "天柱"; "任" -> "天任"; "英" -> "天英"; else -> s
}
private fun sxFullMen(s: String) = when (s) {
    "休" -> "休门"; "生" -> "生门"; "伤" -> "伤门"; "杜" -> "杜门"; "景" -> "景门"
    "死" -> "死门"; "惊" -> "惊门"; "开" -> "开门"; else -> s
}

@Composable
private fun SxPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(
        Brush.horizontalGradient(listOf(Color(0xFF8A2A22), SX_RED)))
        .clickable(onClick = onClick).padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SxYearDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var y by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, SX_LINE, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择年份", color = SX_GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(SX_CARD).clickable { y = kotlin.math.max(1800, y - 1) }.padding(14.dp)) {
                    Text("−", color = SX_GOLD, fontSize = 18.sp)
                }
                Spacer(Modifier.width(24.dp))
                Text("$y", color = Color.White, fontSize = 26.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(24.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(SX_CARD).clickable { y = kotlin.math.min(2199, y + 1) }.padding(14.dp)) {
                    Text("+", color = SX_GOLD, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(SX_CARD).clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(SX_RED).clickable { onConfirm(y) }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SxHelpDialog(text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F1E8)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("宫位信息", color = SX_RED, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(text, color = Color(0xFF333333), fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(SX_RED).clickable(onClick = onDismiss).padding(horizontal = 28.dp, vertical = 10.dp)) {
                Text("知道了", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

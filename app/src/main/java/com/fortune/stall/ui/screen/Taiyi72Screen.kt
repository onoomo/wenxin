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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.Taiyi72
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 太乙神数 · 传统七十二局 独立模块。
 * 起卦弹窗（时间/盘级/积数流派/手工积数）→ 结果页（信息表 + 积数卡 + 主客算 + 十六方位盘 + 星将落点）。
 */
@Composable
fun Taiyi72CastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var question by remember { mutableStateOf("") }
    var modeIdx by remember { mutableStateOf(3) } // 默认时盘
    var traditionIdx by remember { mutableStateOf(0) } // 默认金镜
    var manualAcc by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Taiyi72.Result?>(null) }

    val modes = listOf(Taiyi72.Mode.YEAR, Taiyi72.Mode.MONTH, Taiyi72.Mode.DAY, Taiyi72.Mode.HOUR)
    val traditions = listOf(Taiyi72.Tradition.JINJING, Taiyi72.Tradition.TONGZONG)

    if (result != null) {
        Taiyi72DetailScreen(result!!, question, onBack)
        return
    }

    JiDialogShell(title = "太 乙 神 数", onBack = onBack, buttonText = "排 盘", onCast = {
        try {
            val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
            val acc = manualAcc.trim().toLongOrNull()
            val r = Taiyi72.paipan(dt, modes[modeIdx], traditions[traditionIdx], acc, null)
            val face = ty72FaceText(r, question)
            db.insert(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                question, "taiyi72", "太乙神数", face, r.jieqi)
            result = r
        } catch (e: Exception) {
            Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }) {
        JiRow("起局时间：") { JiDateInput(dtText, { showDate = true }, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(12.dp))
        JiRow("预测事项：") { JiInput(question, { question = it.take(30) }, "输入预测事项（选填）", Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(12.dp))
        JiChipLine("起局尺度：", modes.map { it.label }, modeIdx) { modeIdx = it }
        Spacer(Modifier.height(10.dp))
        JiChipLine("积数纪元：", traditions.map { it.label }, traditionIdx) { traditionIdx = it }
        Spacer(Modifier.height(10.dp))
        JiRow("手工积数：") { JiInput(manualAcc, { manualAcc = it.filter { c -> c.isDigit() }.take(12) }, "留空=自动（可填复核）", Modifier.fillMaxWidth()) }
    }
    if (showDate) BoardDateDialog(initial = try { LocalDate.parse(dtText.substring(0, 10)) } catch (e: Exception) { LocalDate.now() },
        onDismiss = { showDate = false },
        onConfirm = { d -> val t = dtText.substring(11).ifEmpty { "00:00" }; dtText = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"; showDate = false; showTime = true })
    if (showTime) BoardTimeDialog(initial = try { LocalTime.parse(dtText.substring(11)) } catch (e: Exception) { LocalTime.now() },
        onDismiss = { showTime = false },
        onConfirm = { t -> dtText = "${dtText.substring(0, 10)} ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"; showTime = false })
}

private val TY72_LABEL = Color(0xFF4A5A8A)
private val TY72_TEXT = Color(0xFF2A2620)

@Composable
private fun Ty72Info(label: String, value: String, placeholder: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = TY72_LABEL, fontSize = 15.sp, modifier = Modifier.width(52.dp))
        Text(value, color = if (placeholder) Color(0xFF999999) else TY72_TEXT, fontSize = 15.sp)
    }
}

@Composable
private fun Taiyi72DetailScreen(r: Taiyi72.Result, question: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        BoardNavBar("太乙神数", onBack)

        // ===== 信息表 =====
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 4.dp)) {
            Ty72Info("事项", question.ifEmpty { "输入预测事项" }, question.isEmpty())
            Ty72Info("时间", r.solarText)
            Ty72Info("节气", r.jieqi)
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("农历", color = TY72_LABEL, fontSize = 15.sp, modifier = Modifier.width(52.dp))
                Text(r.lunarText + "   ${r.modeLabel}", color = TY72_TEXT, fontSize = 15.sp)
            }
            // 四柱
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Spacer(Modifier.width(52.dp))
                for (h in listOf("年柱", "月柱", "日柱", "时柱"))
                    Text(h, color = TY72_LABEL, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("四柱", color = TY72_LABEL, fontSize = 15.sp, modifier = Modifier.width(52.dp))
                for (gz in r.siZhu) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(gz.getOrNull(0)?.toString() ?: "", color = TY72_TEXT, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Text(gz.getOrNull(1)?.toString() ?: "", color = TY72_TEXT, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // ===== 积数/局数 5 列（灰底蓝头 + 黑值，仿太乙三式原版）=====
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().background(Color(0xFFE9E9E9)).padding(vertical = 7.dp)) {
                for (h in listOf("积数", "纪元", "局数", "阴阳遁", "太乙宫位"))
                    Text(h, color = Color(0xFF2E4487), fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 7.dp)) {
                val vals = listOf(r.accumulatedNumber.toString(), r.traditionLabel, "${r.bureau}局", r.dunLabel, "${r.taiyiPosition}(${r.taiyiPalace})")
                for (v in vals)
                    Text(v, color = Color(0xFF222222), fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(10.dp))

        // ===== 主客算 + 大小将 双卡 =====
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ty72CountCard("主算", r.home, Modifier.weight(1f))
            Ty72CountCard("客算", r.away, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        // ===== 十六方位盘（含中宫），4×4 网格 =====
        Text("十六方位 · 太乙神盘", color = TY72_LABEL, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFAAAAAA), RoundedCornerShape(6.dp))
                .background(Color.White)
        ) {
            for (row in 0..3) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..3) {
                        val p = r.palaces[row * 4 + col]
                        val isStar = p.stars.isNotEmpty()
                        Box(
                            Modifier.weight(1f).height(72.dp)
                                .background(if (isStar) Color(0xFFF7EECD) else Color(0xFFF2F2F2))
                                .border(0.5.dp, Color(0xFFCCCCCC))
                                .padding(2.dp)
                        ) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(p.position, color = TY72_TEXT, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif)
                                Text(p.deity, color = Color(0xFF777777), fontSize = 9.sp, maxLines = 1)
                                Text(p.stars.joinToString(" "), color = Color(0xFFC0392B), fontSize = 9.sp,
                                    textAlign = TextAlign.Center, lineHeight = 11.sp, maxLines = 3)
                            }
                        }
                    }
                }
                if (row < 3) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFBBBBBB)))
            }
        }
        Spacer(Modifier.height(10.dp))

        // ===== 星将落点详表 =====
        Text("星将落宫", color = TY72_LABEL, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(6.dp))
            .background(Color.White).padding(8.dp)) {
            // 主客大小将方位由 palaces 中的落点反查
            fun posOf(name: String) = r.palaces.firstOrNull { name in it.stars }?.position ?: "—"
            val all = listOf(
                "太乙" to r.taiyiPosition, "文昌" to r.wenChang, "始击" to r.shiJi,
                "计神" to r.jiShen, "合神" to r.heShen, "太岁" to r.taiSui,
                "主大将" to posOf("主大将"), "主参将" to posOf("主参将"),
                "客大将" to posOf("客大将"), "客参将" to posOf("客参将")
            )
            for (pair in all) {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(pair.first, color = TY72_LABEL, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(pair.second, color = TY72_TEXT, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        com.fortune.stall.ui.components.ResultActionBar(faceText = ty72FaceText(r, question))
        Spacer(Modifier.height(10.dp))
        Text("规则：太乙金镜式经·七十二局立成（taibu-core MIT）", color = Color(0xFF999999), fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Ty72CountCard(title: String, g: Taiyi72.General, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(8.dp)).background(Color.White)
            .border(1.dp, Color(0xFFE0D8C0), RoundedCornerShape(8.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = TY72_LABEL, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("${g.count}", color = Color(0xFFC0392B), fontSize = 30.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("大将", color = Color(0xFF888888), fontSize = 11.sp)
                Text("${g.general}", color = TY72_TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("参将", color = Color(0xFF888888), fontSize = 11.sp)
                Text("${g.assistant}", color = TY72_TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 完整太乙盘面文本：供存档/卦账复制/问AI 统一使用 */
private fun ty72FaceText(r: Taiyi72.Result, question: String) = buildString {
    appendLine("【太乙神数 · 七十二局】")
    if (question.isNotBlank()) appendLine("事项：$question")
    appendLine("公历：${r.solarText}")
    appendLine("农历：${r.lunarText}　节气：${r.jieqi}")
    appendLine("四柱：${r.siZhu.joinToString(" ")}")
    appendLine()
    appendLine("${r.modeLabel} · ${r.traditionLabel} · 积数${r.accumulatedNumber}（${r.accumulationSource}）")
    appendLine("${r.dunLabel}${r.bureau}局 · 太乙宫位${r.taiyiPalace}(${r.taiyiPosition}) · 规则${r.ruleVersion}")
    appendLine("主算${r.home.count}（大将${r.home.general} 参将${r.home.assistant}）　客算${r.away.count}（大将${r.away.general} 参将${r.away.assistant}）")
    appendLine()
    appendLine("── 十六方位神盘 ──")
    r.palaces.forEach { p ->
        appendLine("${p.position}[${p.deity}]：${p.stars.joinToString(" ").ifEmpty { "空" }}")
    }
    appendLine()
    appendLine("── 星将落宫 ──")
    appendLine("太乙${r.taiyiPosition} 文昌${r.wenChang} 始击${r.shiJi} 计神${r.jiShen} 合神${r.heShen} 太岁${r.taiSui}")
    fun posOf(name: String) = r.palaces.firstOrNull { name in it.stars }?.position ?: "—"
    appendLine("主大将${posOf("主大将")} 主参将${posOf("主参将")} 客大将${posOf("客大将")} 客参将${posOf("客参将")}")
}

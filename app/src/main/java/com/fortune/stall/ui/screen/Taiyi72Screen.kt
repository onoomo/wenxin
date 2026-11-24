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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 太乙神数 · 传统七十二局 独立模块。
 * 起卦弹窗（时间/盘级/积数流派/手工积数）→ 结果页（信息表 + 传统圆形盘）。
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
    if (showDate) JiDateTimeDialog(
        initial = try { LocalDateTime.parse(dtText.trim(), dtFmt) } catch (e: Exception) { LocalDateTime.now() },
        onDismiss = { showDate = false },
        onConfirm = { dt -> dtText = dt.format(dtFmt); showDate = false }
    )
}

private val TY72_LABEL = Color(0xFFB8862B)   // 信息表标签金棕
private val TY72_TEXT = Color(0xFF2A2620)    // 正文深墨
private val TY72_GRID = Color(0xFFD8D3C8)    // 表格线
private val TY72_GAN = listOf(
    Color(0xFFC0392B), Color(0xFF8E6E2E), Color(0xFF2E6DB4), Color(0xFFB4442E) // 年月日时天干配色（红/棕/蓝/棕红）
)
private val TY72_WX = mapOf(
    '甲' to Color(0xFF2E7D32), '乙' to Color(0xFF2E7D32),
    '丙' to Color(0xFFC0392B), '丁' to Color(0xFFC0392B),
    '戊' to Color(0xFFB8862B), '己' to Color(0xFFB8862B),
    '庚' to Color(0xFF6B6B6B), '辛' to Color(0xFF6B6B6B),
    '壬' to Color(0xFF2E6DB4), '癸' to Color(0xFF2E6DB4),
    '子' to Color(0xFF2E6DB4), '丑' to Color(0xFFB8862B),
    '寅' to Color(0xFF2E7D32), '卯' to Color(0xFF2E7D32),
    '辰' to Color(0xFFB8862B), '巳' to Color(0xFFC0392B),
    '午' to Color(0xFFC0392B), '未' to Color(0xFFB8862B),
    '申' to Color(0xFF6B6B6B), '酉' to Color(0xFF6B6B6B),
    '戌' to Color(0xFFB8862B), '亥' to Color(0xFF2E6DB4)
)

/** 信息表整行：标签(窄) + 内容 */
@Composable
private fun Ty72Info(label: String, value: String, valueColor: Color = TY72_TEXT, placeholder: Boolean = false) {
    Row(Modifier.fillMaxWidth().border(0.5.dp, TY72_GRID)) {
        Box(Modifier.width(56.dp).background(Color(0xFFFBF7EE)).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(label, color = TY72_LABEL, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.weight(1f).padding(vertical = 9.dp, horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(value, color = if (placeholder) Color(0xFFAAAAAA) else valueColor,
                fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Taiyi72DetailScreen(r: Taiyi72.Result, question: String, onBack: () -> Unit) {
    var curQuestion by remember { mutableStateOf(question) }
    var editQuestion by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(question) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        BoardNavBar("太乙神数", onBack)

        // ===== 信息表（表格式：事项/盘式/日期/节气 + 四柱区）=====
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp)).border(1.dp, TY72_GRID).background(Color.White)) {
            // 事项（可点编辑）
            Row(Modifier.fillMaxWidth().clickable { draft = curQuestion; editQuestion = true }.border(0.5.dp, TY72_GRID)) {
                Box(Modifier.width(56.dp).background(Color(0xFFFBF7EE)).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text("事项", color = TY72_LABEL, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Row(Modifier.weight(1f).padding(vertical = 9.dp, horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (curQuestion.isBlank()) "（点击填写事项）" else curQuestion,
                        color = if (curQuestion.isBlank()) Color(0xFFAAAAAA) else TY72_TEXT,
                        fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("✎", color = TY72_LABEL, fontSize = 15.sp)
                }
            }
            // 盘式
            Ty72Info("盘式", "【时太乙】 【指津】")
            // 日期
            Ty72Info("日期", r.dateCn.ifEmpty { r.solarText })
            // 节气
            Ty72Info("节气", r.jieqiDetail.ifEmpty { r.jieqi })

            // 四柱表：表头 + 四柱 + 空亡 + 值使 + 开门（列对应 年/月/日/时）
            val heads = listOf("年柱", "月柱", "日柱", "时柱")
            Row(Modifier.fillMaxWidth().border(0.5.dp, TY72_GRID)) {
                Box(Modifier.width(56.dp).background(Color(0xFFFBF7EE)).border(0.5.dp, TY72_GRID).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text("四柱", color = TY72_LABEL, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                heads.forEach { h ->
                    Box(Modifier.weight(1f).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text(h, color = TY72_LABEL, fontSize = 14.sp)
                    }
                }
            }
            // 四柱干支（横排天干在上、地支在下，按五行配色）
            Row(Modifier.fillMaxWidth().border(0.5.dp, TY72_GRID)) {
                Spacer(Modifier.width(56.dp).background(Color(0xFFFBF7EE)).border(0.5.dp, TY72_GRID))
                r.siZhu.forEach { gz ->
                    Column(Modifier.weight(1f).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val gan = gz.getOrNull(0)
                        val zhi = gz.getOrNull(1)
                        if (gan != null) Text(gan.toString(), color = TY72_WX[gan] ?: TY72_TEXT, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        if (zhi != null) Text(zhi.toString(), color = TY72_WX[zhi] ?: TY72_TEXT, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    }
                }
            }
            // 空亡
            Row(Modifier.fillMaxWidth().border(0.5.dp, TY72_GRID)) {
                Box(Modifier.width(56.dp).background(Color(0xFFFBF7EE)).border(0.5.dp, TY72_GRID).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("空亡", color = TY72_LABEL, fontSize = 14.sp)
                }
                val kw = r.kongWang.ifEmpty { listOf("", "", "", "") }
                kw.forEach { v ->
                    Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(v, color = Color(0xFF9A9A9A), fontSize = 13.sp)
                    }
                }
            }
            // 值使（列头）
            Row(Modifier.fillMaxWidth().border(0.5.dp, TY72_GRID)) {
                Box(Modifier.width(56.dp).background(Color(0xFFFBF7EE)).border(0.5.dp, TY72_GRID).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("值使", color = TY72_LABEL, fontSize = 14.sp)
                }
                listOf("局数", "主算", "客算", "定算").forEach { h ->
                    Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(h, color = TY72_LABEL, fontSize = 14.sp)
                    }
                }
            }
            // 开门（值）
            Row(Modifier.fillMaxWidth().border(0.5.dp, TY72_GRID)) {
                Box(Modifier.width(56.dp).background(Color(0xFFFBF7EE)).border(0.5.dp, TY72_GRID).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text("开门", color = TY72_TEXT, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                val vals = listOf("${r.dunLabel}${r.bureau}局", "${r.home.count}", "${r.away.count}", "${(r.home.count + r.away.count) / 2}")
                vals.forEach { v ->
                    Box(Modifier.weight(1f).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                        Text(v, color = TY72_TEXT, fontSize = 14.sp)
                    }
                }
            }
        }

        // ===== 传统圆形盘 =====
        TaiyiRoundBoard(r, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))

        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(
            faceText = ty72FaceText(r, curQuestion),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(20.dp))
    }

    if (editQuestion) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { editQuestion = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(20.dp)) {
                Text("填写事项", color = TY72_TEXT, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = draft, onValueChange = { draft = it.take(30) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("输入预测事项") }
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("取消", color = Color(0xFF888888), fontSize = 14.sp,
                        modifier = Modifier.clickable { editQuestion = false }.padding(10.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("确定", color = TY72_LABEL, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { curQuestion = draft.trim(); editQuestion = false }.padding(10.dp))
                }
            }
        }
    }
}

/** 完整太乙盘面文本：供存档/卦账复制/问AI 统一使用 */
private fun ty72FaceText(r: Taiyi72.Result, question: String) = buildString {
    appendLine("【太乙神数 · 七十二局】")
    if (question.isNotBlank()) appendLine("事项：$question")
    appendLine("盘式：【时太乙】【指津】")
    appendLine("日期：${r.dateCn.ifEmpty { r.solarText }}")
    appendLine("节气：${r.jieqiDetail.ifEmpty { r.jieqi }}")
    appendLine("四柱：${r.siZhu.joinToString(" ")}")
    appendLine("空亡：${r.kongWang.joinToString(" ")}")
    appendLine("开门：${r.dunLabel}${r.bureau}局　主算${r.home.count} 客算${r.away.count} 定算${(r.home.count + r.away.count) / 2}")
    appendLine()
    appendLine("${r.modeLabel} · ${r.traditionLabel} · 积数${r.accumulatedNumber}（${r.accumulationSource}）")
    appendLine("太乙宫位${r.taiyiPalace}(${r.taiyiPosition}) · 规则${r.ruleVersion}")
    appendLine()
    appendLine("── 星将落宫 ──")
    r.starLandings.forEach { appendLine("${it.name} → ${it.position}") }
    appendLine()
    appendLine("── 十六方位神盘 ──")
    r.palaces.forEach { p ->
        appendLine("${p.position}[${p.deity}]：${p.stars.joinToString(" ").ifEmpty { "空" }}")
    }
}

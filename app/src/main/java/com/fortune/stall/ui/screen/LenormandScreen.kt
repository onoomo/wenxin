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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.Lenormand
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.AiBridge
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 雷诺曼占卜 —— 独立页面（不并入塔罗）。
 * 移植自 Brhiza/mingyu 的雷诺曼牌库/牌阵/组合算法；页面自带起卦表单 + 结果牌面。
 * 牌面按牌阵布局：单牌/三牌/五牌/关系/决策/元素用横排，九宫 3×3，大桌 9×4。
 */
@Composable
fun LenormandScreen(db: com.fortune.stall.data.FortuneDb, onBack: () -> Unit) {
    val context = LocalContext.current
    var spreadKey by remember { mutableStateOf("three") }
    var question by remember { mutableStateOf("") }
    var manualMode by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") } // 手录牌号：空格/逗号分隔 1-36
    var result by remember { mutableStateOf<Lenormand.Result?>(null) }
    var faceText by remember { mutableStateOf("") }
    var casting by remember { mutableStateOf(false) }

    val spread = Lenormand.spreadOf(spreadKey)

    fun doDraw() {
        when {
            question.isBlank() -> {
                Toast.makeText(context, "无事不占——先把事说清", Toast.LENGTH_SHORT).show(); return
            }
            manualMode -> {
                val ids = manualText.split(Regex("[\\s,，]+")).filter { it.isNotBlank() }
                    .mapNotNull { it.toIntOrNull() }
                if (ids.size != spread.positions.size || ids.any { it !in 1..36 } || ids.distinct().size != ids.size) {
                    Toast.makeText(context, "请录入 ${spread.positions.size} 个不重复的牌号（1-36）", Toast.LENGTH_SHORT).show(); return
                }
                result = Lenormand.draw(spreadKey, Random, ids)
            }
            else -> result = Lenormand.draw(spreadKey, Random)
        }
        result?.let { faceText = Lenormand.render(it, question) }
        casting = false
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E0D0B), Color(0xFF16120C))))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(44.dp))
            // 顶部返回
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0x0AFFFFFF)).clickable { onBack() },
                    contentAlignment = Alignment.Center) {
                    Text("‹", color = AppColors.Paper, fontSize = 24.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text("雷诺曼 · Lenormand", color = AppColors.Lantern, fontSize = 15.sp,
                    fontFamily = FontFamily.Serif, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text("雷 诺 曼", color = AppColors.Paper, fontSize = 26.sp, fontFamily = FontFamily.Serif,
                letterSpacing = 4.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("36 张牌 · 以牌位与相邻合读照见事体", color = AppColors.Muted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))

            // 牌阵选择
            LmFieldLabel("选择牌阵（${spread.positions.size} 张）")
            val cols = remember { mutableListOf<List<Lenormand.Spread>>() }
            cols.clear(); cols.addAll(Lenormand.SPREADS.chunked(2))
            cols.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { sp ->
                        val sel = sp.key == spreadKey
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Color(0x17D9A441) else Color(0x0AFFFFFF))
                            .then(if (sel) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp)) else Modifier)
                            .clickable { spreadKey = sp.key; result = null; manualText = "" }
                            .padding(horizontal = 6.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sp.name, color = if (sel) AppColors.Paper else AppColors.Muted,
                                    fontSize = 12.5.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text("${sp.positions.size} 张", color = AppColors.Dim, fontSize = 9.5.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 所问何事
            LmFieldLabel("所问何事")
            OutlinedTextField(
                value = question, onValueChange = { question = it },
                placeholder = { Text("无事不占——先把事说清", color = AppColors.Dim) },
                minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                    focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                    cursorColor = AppColors.Lantern, focusedContainerColor = Color(0x08FFFFFF),
                    unfocusedContainerColor = Color(0x08FFFFFF)
                )
            )
            Spacer(Modifier.height(10.dp))

            // 抽牌方式
            LmFieldLabel("抽牌方式")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "随机抽牌", true to "手动录牌").forEach { (m, label) ->
                    val sel = m == manualMode
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .background(if (sel) Color(0x17D9A441) else Color(0x0AFFFFFF))
                        .then(if (sel) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(9.dp)) else Modifier)
                        .clickable { manualMode = m; result = null }
                        .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (sel) AppColors.Paper else AppColors.Muted, fontSize = 12.5.sp)
                    }
                }
            }
            if (manualMode) {
                Spacer(Modifier.height(8.dp))
                Text("按牌位顺序录入牌号（1-36，英文/中文逗号或空格分隔）", color = AppColors.Dim, fontSize = 10.5.sp)
                OutlinedTextField(
                    value = manualText, onValueChange = { manualText = it },
                    placeholder = { Text("如: 24 31 9", color = AppColors.Dim) },
                    minLines = 1, maxLines = 2, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                        focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                        cursorColor = AppColors.Lantern, focusedContainerColor = Color(0x08FFFFFF),
                        unfocusedContainerColor = Color(0x08FFFFFF)
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("牌位：${spread.positions.joinToString(" · ")}", color = AppColors.Dim, fontSize = 10.5.sp,
                lineHeight = 16.sp, modifier = Modifier.fillMaxWidth())

            // 抽牌按钮
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { casting = true; doDraw() }, enabled = !casting,
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC96442), contentColor = AppColors.Paper
                )
            ) { Text(if (manualMode) "呈 牌 解 读" else "抽 牌", letterSpacing = 4.sp, fontSize = 15.sp) }

            // 结果牌面
            result?.let { res ->
                Spacer(Modifier.height(18.dp))
                LenormandBoard(res)
                if (res.combinations.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    LmFieldLabel("两牌组合（${res.combinations.size}）")
                    res.combinations.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color(0x0AFFFFFF)).padding(8.dp)) {
                            Text("${c.card1}+${c.card2}", color = AppColors.Lantern, fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${c.position1} × ${c.position2}｜${c.relation}｜${c.source}",
                                    color = AppColors.Dim, fontSize = 9.5.sp)
                                Text(c.meaning, color = AppColors.Paper, fontSize = 11.5.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
                if (res.spreadKey == "nine") {
                    Lenormand.analyzeNineGrid(res.cards.map { it.card })?.let { cross ->
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0AFFFFFF)).padding(10.dp)) {
                            Text(cross.summary, color = AppColors.Paper, fontSize = 11.5.sp, lineHeight = 18.sp)
                        }
                    }
                }
                if (res.layoutEvidence.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LmFieldLabel("布局证据")
                    res.layoutEvidence.forEach {
                        Text("· $it", color = AppColors.Muted, fontSize = 11.sp, lineHeight = 17.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .background(Color(0x1AD9A441)).clickable {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("雷诺曼牌面", faceText))
                            Toast.makeText(context, "牌面已复制", Toast.LENGTH_SHORT).show()
                        }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("复制牌面", color = AppColors.Lantern, fontSize = 11.5.sp)
                    }
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .background(Brush.verticalGradient(listOf(Color(0x66D9A441), Color(0x44C96442))))
                        .clickable {
                            val cb = AiBridge.askAi
                            if (cb != null) cb(faceText)
                            else Toast.makeText(context, "AI 通道未就绪", Toast.LENGTH_SHORT).show()
                        }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("✦ 问 AI 解读", color = AppColors.Paper, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // 落账
                androidx.compose.runtime.LaunchedEffect(res) {
                    val now = LocalDateTime.now()
                    val seal = com.fortune.stall.engine.FortuneEngine.sealLine(now)
                    val id = db.insert(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        question, "lenormand", spreadKey, "落印中…", seal)
                    db.updateFace(id, faceText)
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun LmFieldLabel(text: String) {
    Text(text, color = AppColors.Muted, fontSize = 11.5.sp, letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp))
}

/** 牌面网格：按牌阵选择布局 */
@Composable
private fun LenormandBoard(res: Lenormand.Result) {
    when {
        res.spreadKey == "grandTableau" -> {
            val rows = res.cards.chunked(9)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { Box(Modifier.weight(1f)) { LmCard(it, compact = true) } }
                    repeat(9 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        res.spreadKey == "nine" -> {
            res.cards.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { Box(Modifier.weight(1f)) { LmCard(it, compact = false) } }
                }
            }
        }
        else -> {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                res.cards.forEach { LmCard(it, compact = false) }
            }
        }
    }
}

@Composable
private fun LmCard(d: Lenormand.Drawn, compact: Boolean) {
    val gradColors = listOf(Color(0xFF1C1A16), Color(0xFF14120E))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(if (compact) 5.dp else 8.dp))
        .background(Brush.verticalGradient(gradColors))
        .border(1.dp, AppColors.GoldLine, RoundedCornerShape(if (compact) 5.dp else 8.dp))
        .padding(if (compact) 4.dp else 10.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${d.card.id}", color = AppColors.Lantern,
                    fontSize = if (compact) 8.sp else 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(if (compact) 3.dp else 7.dp))
                Text(d.position, color = AppColors.Dim, fontSize = if (compact) 7.5.sp else 10.sp, maxLines = 1)
            }
            Text(d.card.name, color = AppColors.Paper,
                fontSize = if (compact) 11.sp else 17.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp))
            if (!compact) {
                Text(d.card.keywords.joinToString(" · "), color = AppColors.Lantern,
                    fontSize = 10.5.sp, modifier = Modifier.padding(top = 2.dp))
                Text(d.card.meaning, color = AppColors.Muted, fontSize = 11.sp, lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 3.dp))
            }
            if (d.house != null) {
                Text("落 ${d.house} 宫", color = AppColors.Ember,
                    fontSize = if (compact) 7.5.sp else 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (d.row != null && d.column != null && !compact) {
                Text("第${d.row}排 第${d.column}列", color = AppColors.Dim, fontSize = 9.5.sp,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

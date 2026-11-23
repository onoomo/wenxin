package com.fortune.stall.ui.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.CastResult
import com.fortune.stall.engine.jishiyu.CastSection
import com.fortune.stall.engine.jishiyu.FieldType
import com.fortune.stall.engine.jishiyu.InputField
import com.fortune.stall.engine.jishiyu.JishiyuRegistry
import com.fortune.stall.engine.jishiyu.LiuyaoDetail
import com.fortune.stall.engine.jishiyu.ModuleDef
import com.fortune.stall.ui.components.Coin
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val JI_GOLD = Color(0xFFD9A441)
private val JI_CARD = Color(0x14FFFFFF)
private val JI_LINE = Color(0x52D9A441)
private val JI_RED = Color(0xFFC0392B)

/** 术数中心：列出全部模块，点进各自的排盘页 */
@Composable
fun JishiyuHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            JNavBar("术 数 排 盘", onBack)
            Spacer(Modifier.height(8.dp))
            Text("吉时雨算法 · 十八般术数", color = AppColors.Muted, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(20.dp))

            // 六爻排盘已移到「六爻」传承页，术数中心不再重复列出
            val mods = JishiyuRegistry.modules.filter { it.key != "liuyao" }
            var i = 0
            while (i < mods.size) {
                val row = mutableListOf<ModuleDef>()
                repeat(2) { if (i < mods.size) { row.add(mods[i]); i++ } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (m in row) Box(Modifier.weight(1f)) { JModuleCard(m) { onOpen(m.key) } }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun JModuleCard(m: ModuleDef, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xB8232330), Color(0xB81C1C22))))
            .border(1.dp, JI_LINE, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(14.dp)
    ) {
        Text(m.eyebrow, color = JI_GOLD, fontSize = 9.5.sp, letterSpacing = 3.sp)
        Text(m.name, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 5.dp))
        Text(m.desc, color = AppColors.Muted, fontSize = 10.5.sp, lineHeight = 17.sp)
    }
}

/** 术数排盘分发：六爻用专用起卦页，其余模块用通用专用起卦页 + 独立详情页 */
@Composable
fun JishiyuCastScreen(moduleKey: String, db: FortuneDb, onBack: () -> Unit) {
    val module = JishiyuRegistry.modules.firstOrNull { it.key == moduleKey }
    if (module == null) { JNavBar("未找到模块", onBack); return }

    // 六爻使用专用起卦页（手动指定/自动摇卦/手动摇卦）
    if (moduleKey == "liuyao") { LiuyaoCastScreen(db = db, onBack = onBack); return }

    // 奇门遁甲使用专用起卦页 + 仿原版九宫格详情
    if (moduleKey == "qimen") { QimenCastScreen(db = db, onBack = onBack); return }

    // 山向奇门 / 玄空飞星 / 达摩一掌经：专用起卦页 + 仿原版详情
    when (moduleKey) {
        "shanxiangqimen" -> { ShanxiangCastScreen(db = db, onBack = onBack); return }
        "ninestar" -> { FeixingCastScreen(db = db, onBack = onBack); return }
        "yizhangjing" -> { YizhangCastScreen(db = db, onBack = onBack); return }
        "daliuren" -> { LiuRenCastScreen(db = db, onBack = onBack); return }
        "meihua", "meihua-time", "meihua-num" -> { MeiHuaCastScreen(db = db, onBack = onBack); return }
    }

    // 其余模块：独立专用起卦页 → 仿原版详细结果页 → 自动存档
    JishiyuProScreen(module = module, db = db, onBack = onBack)
}

// ==================== 通用术数：独立起卦页 + 仿原版详情页 + 存档 ====================

/** 通用详情页入参 */
data class JishiyuDetailInput(
    val moduleName: String,
    val eyebrow: String,
    val question: String,
    val timeText: String,
    val result: CastResult
)

@Composable
private fun JishiyuProScreen(module: ModuleDef, db: FortuneDb, onBack: () -> Unit) {
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var question by remember(module.key) { mutableStateOf("") }
    val values = remember(module.key) {
        mutableStateOf(module.fields.associate { it.key to it.default }.toMutableMap())
    }
    var pickingDateFor by remember { mutableStateOf<String?>(null) }
    var pickingTimeFor by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<JishiyuDetailInput?>(null) }

    LaunchedEffect(toast) { if (toast != null) { delay(1800); toast = null } }

    fun doCast() {
        val r = module.cast(values.value)
        val timeKey = module.fields.firstOrNull {
            it.type == FieldType.DATETIME || it.type == FieldType.DATE || it.type == FieldType.TIME
        }?.key
        val timeText = timeKey?.let { values.value[it] }?.takeIf { it.isNotBlank() }
            ?: LocalDateTime.now().format(dtFmt)
        val createdAt = try {
            LocalDateTime.parse(timeText, dtFmt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) { timeText }
        val q = question.ifBlank { module.name + " · 排盘" }
        val modeLabel = module.eyebrow.replace(" ", "")
        val id = db.insert(createdAt, q, module.key, modeLabel, "落印中…", r.subtitle)
        db.updateFace(id, castResultToText(r, q, createdAt, modeLabel))
        toast = "已存入存档 · 卦账 #$id"
        detail = JishiyuDetailInput(module.name, modeLabel, q, timeText, r)
    }

    if (detail == null) {
        JiDialogShell(title = module.name, onBack = onBack, onCast = { doCast() }) {
            JiRow("事项：") { JiInput(question, { question = it }, "输入预测事项（选填）", Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(14.dp))
            for (f in module.fields) {
                Text(f.label, color = JiForm.TEXT, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                JProField(
                    f = f,
                    value = values.value[f.key] ?: "",
                    onChange = { values.value[f.key] = it },
                    onPickDate = { pickingDateFor = f.key },
                    onPickTime = { pickingTimeFor = f.key }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }

        // 日期/时间选择
        pickingDateFor?.let { key ->
            val cur = values.value[key] ?: ""
            LyDateDialog(
                initial = try { LocalDate.parse(cur.substring(0, 10)) } catch (e: Exception) { LocalDate.now() },
                onDismiss = { pickingDateFor = null },
                onConfirm = { d ->
                    val t = if (cur.length >= 16) cur.substring(11) else "00:00"
                    values.value[key] = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"
                    pickingDateFor = null
                }
            )
        }
        pickingTimeFor?.let { key ->
            val cur = values.value[key] ?: ""
            LyTimeDialog(
                initial = try { LocalTime.parse(cur.substring(11)) } catch (e: Exception) { LocalTime.now() },
                onDismiss = { pickingTimeFor = null },
                onConfirm = { t ->
                    val day = if (cur.length >= 10) cur.substring(0, 10) else LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    values.value[key] = "$day ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    pickingTimeFor = null
                }
            )
        }

    Box(Modifier.fillMaxSize()) {
        toast?.let { msg ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
                .clip(RoundedCornerShape(20.dp)).background(Color(0xE6322620))
                .border(1.dp, JI_LINE, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text(msg, color = JI_GOLD, fontSize = 13.sp) }
        }
        // 仿原版详情结果页：覆盖整个起卦页
        detail?.let { input ->
            Box(Modifier.fillMaxSize().background(Color(0xFFF5F1E8))) {
                JishiyuDetailScreen(input = input, onBack = { detail = null })
            }
        }
    }
}

/** 通用字段输入：性别切换 / 日期时间可点选 / 文本数字框 */
@Composable
private fun JProField(
    f: InputField,
    value: String,
    onChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    when (f.type) {
        FieldType.GENDER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (g in listOf("男", "女")) {
                val on = value == g
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (on) JiForm.RED else JiForm.SELECT_BG)
                    .border(1.dp, if (on) JiForm.RED else JiForm.LINE, RoundedCornerShape(8.dp))
                    .clickable { onChange(g) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(g, color = if (on) Color.White else JiForm.TEXT, fontSize = 13.sp)
                }
            }
        }
        FieldType.DATETIME, FieldType.DATE, FieldType.TIME -> {
            JiDateInput(value.ifBlank { f.hint.ifEmpty { "请选择" } }, {
                if (f.type == FieldType.TIME) onPickTime() else onPickDate()
            }, Modifier.fillMaxWidth())
        }
        else -> JiInput(value, onChange, f.hint, Modifier.fillMaxWidth())
    }
}

/** 仿原版通用详情结果页（浅色纸面 + 红色标题栏） */
@Composable
private fun JishiyuDetailScreen(input: JishiyuDetailInput, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        // 顶部标题栏
        Box(Modifier.fillMaxWidth().background(Color(0xFFB0392E)).padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text(input.moduleName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(28.dp))
            }
        }

        // 信息表
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp)) {
            InfoRow("事项", input.question)
            InfoRow("时间", input.timeText)
            InfoRow("排盘", input.eyebrow)
            if (input.result.subtitle.isNotBlank()) InfoRow("提要", input.result.subtitle)
        }
        Spacer(Modifier.height(8.dp))

        // 各分节
        for (s in input.result.sections) {
            ProDetailSection(s)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(
            faceText = castResultToText(input.result, input.question, input.timeText, input.eyebrow)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProDetailSection(s: CastSection) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (s.title.isNotBlank()) {
            Text(s.title, color = Color(0xFFB0392E), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF6E6D2)).padding(horizontal = 8.dp, vertical = 6.dp))
            Spacer(Modifier.height(8.dp))
        }
        for ((k, v) in s.rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(k, color = Color(0xFF888888), fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                Text(v, color = Color(0xFF222222), fontSize = 12.5.sp, modifier = Modifier.weight(2f),
                    fontFamily = FontFamily.Monospace)
            }
        }
        s.table?.let { tb ->
            for (row in tb) {
                Row(Modifier.fillMaxWidth()) {
                    for (cell in row) Text(cell, color = Color(0xFF222222), fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
        for (para in s.paragraphs) {
            Text(para, color = Color(0xFF444444), fontSize = 13.sp, lineHeight = 21.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
        }
    }
}

@Composable
private fun JNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(JI_CARD)
            .clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("‹ 返回", color = JI_GOLD, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(52.dp))
    }
}

@Composable
private fun JField(label: String) {
    Text(label, color = JI_GOLD, fontSize = 12.sp, letterSpacing = 3.sp,
        modifier = Modifier.fillMaxWidth())
}

@Composable
private fun JInputBox(f: com.fortune.stall.engine.jishiyu.InputField, value: String, onChange: (String) -> Unit) {
    when (f.type) {
        FieldType.GENDER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (g in listOf("男", "女")) {
                val on = value == g
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (on) Color(0x2AD9A441) else JI_CARD)
                    .then(if (on) Modifier.border(1.dp, JI_LINE, RoundedCornerShape(10.dp)) else Modifier)
                    .clickable { onChange(g) }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(g, color = if (on) JI_GOLD else AppColors.Paper, fontSize = 14.sp)
                }
            }
        }
        else -> Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(JI_CARD)
                .border(1.dp, JI_LINE, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (value.isEmpty()) Text(f.hint, color = AppColors.Dim, fontSize = 13.sp)
            androidx.compose.foundation.text.BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = AppColors.Paper, fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun JResultView(r: CastResult, faceText: String? = null) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
            .border(1.dp, JI_LINE, RoundedCornerShape(14.dp)).padding(16.dp)
    ) {
        Text(r.title, color = JI_GOLD, fontSize = 20.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 5.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (r.subtitle.isNotEmpty()) {
            Text(r.subtitle, color = AppColors.Muted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        for (s in r.sections) JSection(s)
        if (faceText != null) {
            Spacer(Modifier.height(12.dp))
            com.fortune.stall.ui.components.ResultActionBar(faceText = faceText, dark = true)
        }
    }
}

@Composable
private fun JSection(s: CastSection) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        if (s.title.isNotEmpty()) {
            Text(s.title, color = if (s.highlighted) JI_GOLD else AppColors.Paper,
                fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0x12D9A441)).padding(horizontal = 8.dp, vertical = 6.dp))
            Spacer(Modifier.height(8.dp))
        }
        if (s.rows.isNotEmpty()) {
            for ((k, v) in s.rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(k, color = AppColors.Muted, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                    Text(v, color = AppColors.Paper, fontSize = 12.5.sp, modifier = Modifier.weight(2f),
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
        s.table?.let { tb ->
            for (row in tb) {
                Row(Modifier.fillMaxWidth()) {
                    for (cell in row) Text(cell, color = AppColors.Paper, fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
        for (para in s.paragraphs) {
            Text(para, color = AppColors.Paper, fontSize = 12.5.sp, lineHeight = 21.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
        }
    }
}

// ==================== 六爻专用起卦页 ====================
private val YAO_OPTIONS = listOf(
    "1" to "少阳 ███", "0" to "少阴 █　█", "1o" to "老阳 ███o", "0x" to "老阴 █　█x"
)
@Composable
fun LiuyaoCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var method by remember { mutableStateOf(0) }          // 0 手动指定 / 1 自动摇卦 / 2 手动摇卦
    // 内部顺序: index 0=初爻 ... 5=上爻（与引擎 yaoList 一致）
    var yaoValues by remember { mutableStateOf(List(6) { "1" }) }
    var pickIndex by remember { mutableStateOf(-1) }      // 正在选择的下标(内部)
    var coins by remember { mutableStateOf(listOf(true, true, true)) } // true=字面
    var shaking by remember { mutableStateOf(false) }
    var shakeCount by remember { mutableStateOf(0) }      // 手动摇卦已落定爻数
    var manualPending by remember { mutableStateOf(false) } // 手动: 已点一次, 等待落定
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CastResult?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var mingText by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf<LiuyaoDetailInput?>(null) }
    val scope = rememberCoroutineScope()

    // 铜钱翻动：由协程直接驱动角度状态，每帧重组，确保 release 版也可见
    val isShaking = shaking || manualPending
    var flipAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            while (true) {
                flipAngle = (flipAngle + 30f) % 360f
                delay(20L)
            }
        } else {
            flipAngle = 0f
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) { delay(1800); toast = null }
    }

    fun shakeOnce(): Pair<String, List<Boolean>> {
        val faces = List(3) { Random.nextBoolean() }
        val total = faces.count { it }                    // 字面(正)个数
        val v = when (total) { 3 -> "0x"; 2 -> "0"; 1 -> "1"; else -> "1o" }
        return v to faces
    }
    fun setYao(i: Int, v: String) {
        val t = yaoValues.toMutableList(); t[i] = v; yaoValues = t; result = null
    }
    fun setMethod(m: Int) {
        method = m; result = null
        if (m == 0) yaoValues = List(6) { "1" }           // 手动指定默认全少阳
        if (m == 2) { shakeCount = 0; manualPending = false }
    }
    fun doCast() {
        if (yaoValues.any { it.isEmpty() }) { toast = "请先摇完所有卦爻！"; return }
        val module = JishiyuRegistry.modules.first { it.key == "liuyao" }
        val r = module.cast(mapOf("yao" to yaoValues.joinToString(""), "dt" to dtText.trim()))
        result = r
        val mName = listOf("手动指定", "自动摇卦", "手动摇卦")[method]
        // 落印存档：排盘记录统一收进「存档 · 卦账」
        val createdAt = try {
            LocalDateTime.parse(dtText.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) { dtText.trim() }
        val question = mingText.ifBlank { "六爻排盘" }
        val id = db.insert(
            createdAt = createdAt,
            question = question,
            method = "liuyao",
            mode = mName,
            face = "落印中…",
            seal = "六爻 · ${r.subtitle}"
        )
        db.updateFace(id, castResultToText(r, question, createdAt, mName))
        toast = "已存入存档 · 卦账 #$id"
        detailInput = LiuyaoDetailInput(
            dt = dtText.trim(),
            yao = yaoValues.toList(),
            method = mName,
            ming = mingText.ifEmpty { "输入预测事项" }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            JNavBar("六爻排盘", onBack)
            Spacer(Modifier.height(18.dp))

            // 预测事项
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("事项：", color = AppColors.Paper, fontSize = 14.sp)
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(JI_CARD)
                        .border(1.dp, JI_LINE, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (mingText.isEmpty()) Text("输入预测事项", color = AppColors.Dim, fontSize = 13.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = mingText, onValueChange = { mingText = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = AppColors.Paper),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 起卦时间 + 刷新按钮
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("起卦时间：", color = AppColors.Paper, fontSize = 14.sp)
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(JI_CARD)
                        .border(1.dp, JI_LINE, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (dtText.length >= 10) dtText.substring(0, 10) else dtText,
                        color = JI_GOLD, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { showDate = true })
                    if (dtText.length >= 16) {
                        Spacer(Modifier.width(8.dp))
                        Text(dtText.substring(11), color = JI_GOLD, fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.clickable { showTime = true })
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(JI_CARD).border(1.dp, JI_LINE, RoundedCornerShape(50))
                        .clickable { dtText = LocalDateTime.now().format(dtFmt); result = null }
                        .padding(10.dp)
                ) { Text("↻", color = JI_RED, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(16.dp))

            // 起卦方式
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("起卦方式：", color = AppColors.Paper, fontSize = 14.sp)
                val methods = listOf("手动指定", "自动摇卦", "手动摇卦")
                methods.forEachIndexed { idx, name ->
                    Row(
                        Modifier.clickable { setMethod(idx) }.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(16.dp).clip(RoundedCornerShape(50))
                                .background(if (method == idx) JI_GOLD else Color.Transparent)
                                .then(if (method == idx) Modifier else Modifier.border(1.dp, JI_LINE, RoundedCornerShape(50))),
                            contentAlignment = Alignment.Center
                        ) { if (method == idx) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1A150C))) }
                        Spacer(Modifier.width(4.dp))
                        Text(name, color = if (method == idx) JI_GOLD else AppColors.Muted, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // 六爻下拉框: 显示上→初, 内部索引 5→0
            val labels = listOf("上爻","五爻","四爻","三爻","二爻","初爻")
            for (ui in 0..5) {
                val idx = 5 - ui
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(labels[ui] + "：", color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(52.dp))
                    val shown = YAO_OPTIONS.firstOrNull { it.first == yaoValues[idx] }?.second
                        ?: if (yaoValues[idx].isEmpty()) "请选择" else yaoValues[idx]
                    val isPicking = (method == 0 && pickIndex == idx)
                    val active = (method == 2 && shakeCount == idx + 1) ||
                                 (method == 2 && manualPending && shakeCount == idx)
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(JI_CARD)
                            .border(1.dp, if (active) JI_RED else JI_LINE, RoundedCornerShape(10.dp))
                            .clickable { if (method == 0) pickIndex = idx }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(shown, color = if (shown == "请选择") AppColors.Dim else AppColors.Paper,
                                fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("▼", color = JI_GOLD, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // 摇卦区（自动/手动）
            if (method == 1 || method == 2) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0x18C0392B), Color(0x0AC0392B))))
                        .border(1.dp, Color(0x40C0392B), RoundedCornerShape(14.dp))
                        .clickable {
                            if (method == 1) {
                                if (shaking) return@clickable
                                shaking = true; result = null
                                yaoValues = List(6) { "" }
                                scope.launch {
                                    for (i in 0..5) {
                                        delay(700)
                                        val (v, f) = shakeOnce(); coins = f
                                        val t = yaoValues.toMutableList(); t[i] = v; yaoValues = t
                                    }
                                    shaking = false
                                }
                            } else {
                                if (manualPending) {
                                    val (v, f) = shakeOnce(); coins = f
                                    val i = shakeCount
                                    if (i in 0..5) setYao(i, v)
                                    shakeCount += 1; manualPending = false
                                } else {
                                    if (shakeCount >= 6) shakeCount = 0
                                    if (shakeCount == 0) { yaoValues = List(6) { "" }; result = null }
                                    manualPending = true
                                    coins = listOf(Random.nextBoolean(), Random.nextBoolean(), Random.nextBoolean())
                                }
                            }
                        }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            val coinDensity = LocalDensity.current.density
                            // 三枚同步绕垂直轴翻转 + 上下跳动；非 lambda 版 graphicsLayer 保证逐帧重组生效
                            val jump = if (isShaking) kotlin.math.sin(Math.toRadians(flipAngle.toDouble())).toFloat() * 7f else 0f
                            val showBack = isShaking && flipAngle > 90f && flipAngle < 270f
                            coins.forEach { zi ->
                                Coin(
                                    zi = if (showBack) !zi else zi,
                                    modifier = Modifier.graphicsLayer(
                                        rotationY = if (isShaking) flipAngle else 0f,
                                        translationY = jump,
                                        cameraDistance = 14f * coinDensity
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(if (method == 1) "开始自动摇卦" else "手动摇卦",
                            color = Color(0xFF2A1A12), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    if (method == 1)
                        "起卦前须静心敛神，心中默念所占之事，点击上面按钮开始自动摇卦。"
                    else
                        "起卦前须静心敛神，心中默念所占之事，点击上面按钮开始摇卦，再点一次可得一爻，反复6次。" +
                        (if (shakeCount > 0) "　已摇${shakeCount}爻" else ""),
                    color = AppColors.Dim, fontSize = 11.sp, lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8A6A2F), Color(0xFFD9A441))))
                    .clickable { doCast() }.padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("开 始 排 盘", color = Color(0xFF1A150C), fontSize = 16.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            }
            Spacer(Modifier.height(16.dp))

            result?.let { r ->
                JResultView(r, castResultToText(
                    r, mingText.ifBlank { "六爻排盘" }, dtText.trim(),
                    listOf("手动指定", "自动摇卦", "手动摇卦")[method]
                ))
            }
        }

        // 选择爻
        if (pickIndex >= 0) {
            val pi = pickIndex
            Dialog(onDismissRequest = { pickIndex = -1 }) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                        .border(1.dp, JI_LINE, RoundedCornerShape(14.dp)).padding(16.dp)
                ) {
                    Text("选择爻象", color = JI_GOLD, fontSize = 16.sp, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.height(10.dp))
                    for ((v, label) in YAO_OPTIONS) {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                                .background(if (yaoValues[pi] == v) Color(0x2AD9A441) else Color(0x10FFFFFF))
                                .clickable { setYao(pi, v); pickIndex = -1 }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) { Text(label, color = if (yaoValues[pi] == v) JI_GOLD else AppColors.Paper, fontSize = 14.sp) }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        // 日期/时间选择
        if (showDate) {
            LyDateDialog(
                initial = try { LocalDate.parse(dtText.substring(0, 10)) } catch (e: Exception) { LocalDate.now() },
                onDismiss = { showDate = false },
                onConfirm = { d ->
                    val t = dtText.substring(11).ifEmpty { "00:00" }
                    dtText = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"; showDate = false; result = null
                }
            )
        }
        if (showTime) {
            LyTimeDialog(
                initial = try { LocalTime.parse(dtText.substring(11)) } catch (e: Exception) { LocalTime.now() },
                onDismiss = { showTime = false },
                onConfirm = { t ->
                    dtText = "${dtText.substring(0, 10)} ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    showTime = false; result = null
                }
            )
        }
        toast?.let { msg ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
                .clip(RoundedCornerShape(20.dp)).background(Color(0xE6322620))
                .border(1.dp, JI_LINE, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text(msg, color = JI_GOLD, fontSize = 13.sp) }
        }

        // 排盘详细页：覆盖整个起卦页
        detailInput?.let { input ->
            Box(Modifier.fillMaxSize().background(Color(0xFF0E0D0A))) {
                LiuyaoDetailScreen(input = input, onBack = { detailInput = null })
            }
        }
    }
}

/** 进入详细结果页所需的起卦参数 */
data class LiuyaoDetailInput(val dt: String, val yao: List<String>, val method: String, val ming: String)

/** 把六爻排盘结果序列化为可存档、可复制的纯文本卦面 */
private fun castResultToText(r: CastResult, question: String, createdAt: String, method: String): String {
    val sb = StringBuilder()
    sb.appendLine(r.title)
    if (r.subtitle.isNotBlank()) sb.appendLine(r.subtitle)
    sb.appendLine("事项：$question")
    sb.appendLine("时间：$createdAt")
    sb.appendLine("方式：$method")
    sb.appendLine()
    for (s in r.sections) {
        if (s.title.isNotBlank()) { sb.appendLine("【${s.title}】") }
        for ((k, v) in s.rows) sb.appendLine("$k：$v")
        s.table?.let { tb -> for (row in tb) sb.appendLine(row.joinToString("  ")) }
        for (p in s.paragraphs) sb.appendLine(p)
        if (s.title.isNotBlank()) sb.appendLine()
    }
    return sb.toString().trim()
}

@Composable
private fun LyDateDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }
    fun dim(): Int = YearMonth.of(year, month).lengthOfMonth()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, JI_LINE, RoundedCornerShape(14.dp)).padding(16.dp)
        ) {
            Text("选择日期", color = JI_GOLD, fontSize = 17.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LyStepper("年", year, 1900, 2100, Modifier.weight(1.3f)) { year = it }
                LyStepper("月", month, 1, 12, Modifier.weight(0.9f)) { month = it; if (day > dim()) day = dim() }
                LyStepper("日", day, 1, dim(), Modifier.weight(0.9f)) { day = it }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x12FFFFFF))
                    .clickable { onDismiss() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x2AD9A441))
                    .border(1.dp, JI_LINE, RoundedCornerShape(10.dp))
                    .clickable { onConfirm(LocalDate.of(year, month, day)) }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center) {
                    Text("确 定", color = JI_GOLD, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
private fun LyTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, JI_LINE, RoundedCornerShape(14.dp)).padding(16.dp)
        ) {
            Text("选择时间", color = JI_GOLD, fontSize = 17.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LyStepper("时", hour, 0, 23, Modifier.weight(1f)) { hour = it }
                LyStepper("分", minute, 0, 59, Modifier.weight(1f)) { minute = it }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x12FFFFFF))
                    .clickable { onDismiss() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x2AD9A441))
                    .border(1.dp, JI_LINE, RoundedCornerShape(10.dp))
                    .clickable { onConfirm(LocalTime.of(hour, minute)) }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center) {
                    Text("确 定", color = JI_GOLD, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
private fun LyStepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x10FFFFFF)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AppColors.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text("＋", color = JI_GOLD, fontSize = 18.sp, modifier = Modifier.clickable { onChange((value + 1).coerceAtMost(max)) })
        Text("$value", color = AppColors.Paper, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        Text("－", color = JI_GOLD, fontSize = 18.sp, modifier = Modifier.clickable { onChange((value - 1).coerceAtLeast(min)) })
    }
}

// ==================== 六爻详细结果页 ====================
@Composable
fun LiuyaoDetailScreen(input: LiuyaoDetailInput, onBack: () -> Unit) {
    val data = remember(input) {
        try {
            val dt = LocalDateTime.parse(input.dt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            LiuyaoDetail.build(dt, input.yao, input.ming, input.method)
        } catch (e: Exception) {
            null
        }
    }
    var shenshaExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        // 顶部标题栏（深色，仿原版）
        Box(
            Modifier.fillMaxWidth().background(Color(0xFFB0392E)).padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text("六爻", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(28.dp))
            }
        }

        if (data == null) {
            Text("排盘数据解析失败", color = Color(0xFF333333), fontSize = 14.sp,
                modifier = Modifier.padding(20.dp))
            return@Column
        }

        // ===== 信息表 =====
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp)) {
            InfoRow("事项", data.ming)
            InfoRow("时间", data.timeText)
            InfoRow("起卦方式", data.method)
            if (data.jieQi.isNotEmpty()) InfoRow("节气", data.jieQi)
            InfoRow("干支", data.ganZhi)
            InfoRow("空亡", data.kongWang)
            // 神煞（可展开）
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                Text("神煞", color = Color(0xFF888888), fontSize = 13.sp, modifier = Modifier.width(52.dp))
                Column(Modifier.weight(1f)) {
                    val shown = if (shenshaExpanded || data.shenSha.length <= 26) data.shenSha
                                else data.shenSha.take(26) + " ..."
                    Text(shown, color = Color(0xFF222222), fontSize = 13.sp)
                }
                if (data.shenSha.length > 26) {
                    Text(if (shenshaExpanded) "收起" else "展开", color = Color(0xFFB0392E), fontSize = 13.sp,
                        modifier = Modifier.clickable { shenshaExpanded = !shenshaExpanded })
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ===== 六神 + 本卦表格 =====
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("六神　　　　本卦", color = Color(0xFF222222), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(data.guaLine, color = Color(0xFF777777), fontSize = 12.5.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            for (row in data.rows) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.liuShen, color = Color(0xFF666666), fontSize = 13.sp, modifier = Modifier.width(46.dp))
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.liuQin, color = Color(0xFF222222), fontSize = 13.5.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(row.gan + row.zhi, color = Color(0xFF222222), fontSize = 13.5.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(row.wuXing, color = Color(0xFF888888), fontSize = 12.5.sp)
                        if (row.fushen.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Text("伏:${row.fushen}", color = Color(0xFF999999), fontSize = 10.sp)
                        }
                    }
                    // 世应标记
                    Text(
                        when { row.shi -> "世"; row.ying -> "应"; else -> "" },
                        color = Color(0xFFB0392E), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp), textAlign = TextAlign.Center
                    )
                    // 爻象
                    YaoGlyphView(row.yao, row.moving)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ===== 世爻解读 =====
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 12.dp)) {
            for (line in data.explainLines) {
                val isHead = line == data.shiLiuQin + "持世" || line.startsWith("解说")
                Text(
                    line,
                    color = if (isHead) Color(0xFF222222) else Color(0xFF444444),
                    fontSize = 13.5.sp,
                    fontWeight = if (isHead) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 13.sp, modifier = Modifier.width(52.dp))
        Text(value, color = Color(0xFF222222), fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

/** 爻象：阳爻整条实线，阴爻断开；动爻用红色并加 o/x 标记 */
@Composable
private fun YaoGlyphView(yao: Int, moving: Boolean) {
    val color = if (moving) Color(0xFFB0392E) else Color(0xFF222222)
    Canvas(modifier = Modifier.size(width = 62.dp, height = 18.dp)) {
        val w = size.width; val h = size.height
        val y = h * 0.42f; val th = h * 0.16f
        if (yao == 1) {
            drawRect(color, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(w, th))
        } else {
            val gap = w * 0.12f
            drawRect(color, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(w / 2 - gap, th))
            drawRect(color, androidx.compose.ui.geometry.Offset(w / 2 + gap, y), androidx.compose.ui.geometry.Size(w / 2 - gap, th))
        }
    }
}

package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.fortune.stall.engine.jishiyu.Qianxu
import com.fortune.stall.engine.jishiyu.QianxuData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ============ 原版纸色主题（还原 yu2102897706.github.io/qianxu CSS）============
private object QX {
    val PAPER = Color(0xFFEFE5CF)
    val PAPER2 = Color(0xFFE3D5B8)
    val PAPER3 = Color(0xFFF6EEDC)
    val INK = Color(0xFF2B251B)
    val INK2 = Color(0xFF6B6150)
    val INK3 = Color(0xFF98907E)
    val LINE = Color(0xFFC9BB9D)
    val CINNABAR = Color(0xFFA83A2A)
    val WATER = Color(0xFF2F4F6B)
    val WOOD = Color(0xFF4A6B3A)
    val METAL = Color(0xFF7C7667)
    val EARTH = Color(0xFF8F6E33)
    val XIONG = Color(0xFF241F18)
    fun wx(c: String) = when (c) { "水" -> WATER; "火" -> CINNABAR; "木" -> WOOD; "金" -> METAL; else -> EARTH }
    fun ji(c: String) = when (c) { "吉" -> CINNABAR; "臧" -> WOOD; "平" -> Color(0xFF7A7163); "否" -> Color(0xFF3E4A5C); "凶" -> XIONG; else -> INK3 }
}

/**
 * 潜虚筮法 —— 从 APK 潜虚_1 反编译还原的完整页面。
 * 三 Tab：潜虚（演卦）/ 命图 / 记录；含 70 策蓍草分步动画、定名、求变、结果页。
 */
@Composable
fun QianxuScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) } // 0 演卦(手动) 1 自动演示
    var state by remember { mutableStateOf<Qianxu.State?>(null) }
    var showMingStage by remember { mutableStateOf(false) }
    var showBianStage by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var showMingCard by remember { mutableStateOf(false) }
    var showBianCard by remember { mutableStateOf(false) }
    var showStop by remember { mutableStateOf(false) }
    var sheetMing by remember { mutableStateOf<QianxuData.Ming?>(null) }
    val mingLog = remember { mutableStateListOf<Triple<Int, String, String>>() }
    val bianLog = remember { mutableStateListOf<Triple<Int, String, String>>() }
    val scope = rememberCoroutineScope()

    // 演卦阶段状态
    var phase by remember { mutableStateOf("idle") } // split1/hang1/yidie1/combine/split2/hang2/yidie2 ; bian: split/hang/yidie
    var l1 by remember { mutableIntStateOf(0) }; var r1 by remember { mutableIntStateOf(0) }
    var l2 by remember { mutableIntStateOf(0) }; var r2 by remember { mutableIntStateOf(0) }
    var counted by remember { mutableIntStateOf(0) }
    var resA by remember { mutableIntStateOf(0) }; var resB by remember { mutableIntStateOf(0) }
    var resPos by remember { mutableIntStateOf(0) }
    var isBian by remember { mutableStateOf(false) }

    fun addLog(list: androidx.compose.runtime.snapshots.SnapshotStateList<Triple<Int, String, String>>, no: Int, t: String, v: String) {
        val idx = list.indexOfFirst { it.first == no }
        if (idx >= 0) list[idx] = Triple(no, t, v) else list.add(Triple(no, t, v))
    }

    // ---- 定名：手动演卦阶段推进 ----
    fun mingSplit(pick: Int) {
        if (pick < 2 || pick > 68) {
            Toast.makeText(ctx, "点第 2 到 68 根之间的一根", Toast.LENGTH_SHORT).show(); return
        }
        if (phase == "split1") { l1 = pick; r1 = 70 - pick; phase = "hang1"
            addLog(mingLog, 1, "取七十策，分而为二", "$l1 / $r1"); addLog(mingLog, 2, "取左之一挂于右", "") }
        else if (phase == "split2") { l2 = pick; r2 = 70 - pick; phase = "hang2"
            addLog(mingLog, 4, "复合为一，再分", "$l2 / $r2"); addLog(mingLog, 5, "取左之一挂于右，揲右", "") }
    }
    fun mingHang() {
        if (phase == "hang1") { phase = "yidie1"; counted = 0; addLog(mingLog, 3, "揲左，以十为一组", "") }
        else if (phase == "hang2") { phase = "yidie2"; counted = 0; addLog(mingLog, 6, "揲右，以十为一组", "") }
    }
    fun mingCountOne() {
        val group = 10
        if (phase == "yidie1") {
            val base = l1 - 1; val remain = base - counted
            if (remain > group) { counted += group; return }
            resA = remain; addLog(mingLog, 3, "揲左，以十为一组", "左余 $remain")
            phase = "combine"; counted = 0
        } else if (phase == "yidie2") {
            val base = r2 + 1; val remain = base - counted
            if (remain > group) { counted += group; return }
            resB = remain; addLog(mingLog, 6, "揲右，以十为一组", "右余 $remain")
            // 完成定名
            val st = Qianxu.castMingManual(resA, resB, l1, l2)
            state = st.copy(question = question)
            addLog(mingLog, 7, "观其所合，以名命之", "${st.ming.name} 名")
            phase = "done"
            if (st.noVar) { showStop = true } else { showMingCard = true }
        }
    }
    fun mingAutoCount() { repeat(30) { mingCountOne() } }

    // ---- 求变：手动演卦阶段推进 ----
    fun bianSplit(pick: Int) {
        if (pick < 2 || pick > 68) { Toast.makeText(ctx, "点第 2 到 68 根之间的一根", Toast.LENGTH_SHORT).show(); return }
        l1 = pick; r1 = 70 - pick; phase = "hang"
        addLog(bianLog, 1, "复取七十策，分而为二", "$l1 / $r1")
        addLog(bianLog, 2, if (state?.mode?.yang == true) "取左之一挂于右，揲左" else "取右之一挂于左，揲右", "")
    }
    fun bianHang() { phase = "yidie"; counted = 0; addLog(bianLog, 3, "揲之以七，所余为变", "") }
    fun bianCountOne() {
        val yang = state?.mode?.yang == true
        val base = if (yang) l1 - 1 else r1 - 1
        val remain = base - counted
        if (remain > 7) { counted += 7; return }
        resPos = remain
        val pn = Qianxu.POS_NAME[resPos] ?: "初"
        addLog(bianLog, 3, "揲之以七，所余为变", "余 $resPos → $pn")
        state?.let { state = Qianxu.resolveBian(it, resPos) }
        phase = "done"; showBianCard = true
    }
    fun bianAutoCount() { repeat(30) { bianCountOne() } }

    // ---- 自动演示（定时动画）----
    fun autoCastMing() {
        mingLog.clear(); showMingStage = true; showMingCard = false; showStop = false
        val st = Qianxu.castMing().copy(question = question)
        state = st
        val steps = listOf(
            "取七十策，分而为二" to "${st.l1} / ${st.r1}",
            "取左之一挂于右" to "",
            "揲左，以十为一组" to "左余 ${st.a}",
            "复合为一，再分" to "${st.l2} / ${st.r2}",
            "取左之一挂于右，揲右，以十为一组" to "右余 ${st.b}",
            "观其所合，以名命之" to "${st.ming.name} 名"
        )
        val stageLeft = listOf(70, st.l1, st.l1 - 1, st.l1 - 1, 70, st.l2, st.l2 - 1, st.l2 - 1)
        val stageRight = listOf(0, st.r1, st.r1 + 1, st.r1 + 1, 0, st.r2, st.r2 + 1, st.r2 + 1)
        val remainTag = listOf(0, 0, 0, st.a, 0, 0, 0, st.b)
        val mapIdx = listOf(0, 1, 2, 3, 4, 5, 6, 7)
        scope.launch {
            steps.forEachIndexed { i, s ->
                addLog(mingLog, i + 1, s.first, s.second)
                val k = mapIdx.getOrElse(i) { 7 }
                l1 = stageLeft[k]; r1 = stageRight[k]
                if (remainTag[k] > 0) resA = remainTag[k]
                delay(360)
            }
            resA = st.a; resB = st.b
            showMingStage = false
            if (st.noVar) showStop = true else showMingCard = true
        }
    }
    fun autoCastBian() {
        val st = state ?: return
        if (st.noVar) return
        bianLog.clear(); showBianStage = true; showBianCard = false
        val b = Qianxu.castBian(st)
        state = b
        val yang = st.mode.yang
        scope.launch {
            addLog(bianLog, 1, "复取七十策，分而为二", "${b.l1} / ${b.r1}")
            l1 = b.l1; r1 = b.r1; delay(360)
            addLog(bianLog, 2, if (yang) "取左之一挂于右 · 置右揲左" else "取右之一挂于左 · 置左揲右", "")
            delay(360)
            addLog(bianLog, 3, "揲之以七，所余为变", "余 ${b.pos} → ${Qianxu.POS_NAME[b.pos]}")
            resPos = b.pos; delay(360)
            showBianStage = false; showBianCard = true
        }
    }

    fun doCast() {
        if (mode == 1) autoCastMing()
        else {
            mingLog.clear(); showMingStage = true; showMingCard = false; showStop = false
            phase = "split1"; counted = 0
            addLog(mingLog, 1, "取七十策，分而为二", "")
        }
    }
    fun doBian() {
        val st = state ?: return
        if (st.noVar) return
        if (mode == 1) autoCastBian()
        else {
            bianLog.clear(); showBianStage = true; showBianCard = false; isBian = true
            phase = "split"; counted = 0
            addLog(bianLog, 1, "复取七十策，分而为二", "")
        }
    }
    fun resetAll() {
        state = null; showMingStage = false; showBianStage = false; showResult = false
        showMingCard = false; showBianCard = false; showStop = false
        phase = "idle"; mingLog.clear(); bianLog.clear()
        tab = 0
    }
    fun openResult() {
        state?.let {
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            db.insert(time, it.question.ifBlank { "潜虚筮法" }, "qianxu", it.ming.key,
                Qianxu.faceText(it, time), it.ming.name)
            showResult = true
        }
    }

    if (showResult && state != null) {
        QianxuResultPage(state!!, onBack = { showResult = false }, onAgain = { resetAll() }, onCopy = {
            val t = Qianxu.faceText(state!!, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            android.content.ClipboardManager::class.java
            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("潜虚筮法", t))
            Toast.makeText(ctx, "结果已复制", Toast.LENGTH_SHORT).show()
        })
        return
    }

    Box(Modifier.fillMaxSize().background(QX.PAPER)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            // 顶栏
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = QX.CINNABAR, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { if (state == null) onBack() else resetAll() })
                    Spacer(Modifier.width(8.dp))
                    Text("潜虚", color = QX.INK, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(QX.CINNABAR),
                    contentAlignment = Alignment.Center) {
                    Text("潜虚", color = QX.PAPER3, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.5.dp).background(QX.INK))
            // Tabs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("潜虚", "命图", "记录").forEachIndexed { i, t ->
                    val on = tab == i
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { tab = i }) {
                        Text(t, color = if (on) QX.INK else QX.INK2, fontSize = 14.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.width(28.dp).height(if (on) 2.dp else 0.dp).background(QX.CINNABAR))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            when (tab) {
                0 -> QianxuDivineTab(
                    question, { question = it }, mode, { mode = it },
                    state, showMingStage, showBianStage, showMingCard, showBianCard, showStop,
                    phase, l1, r1, l2, r2, counted, resA, resB, resPos, isBian,
                    mingLog, bianLog,
                    onCast = { doCast() }, onBian = { doBian() },
                    onMingSplit = { mingSplit(it) }, onMingHang = { mingHang() },
                    onMingCount = { mingCountOne() }, onMingAutoCount = { mingAutoCount() },
                    onBianSplit = { bianSplit(it) }, onBianHang = { bianHang() },
                    onBianCount = { bianCountOne() }, onBianAutoCount = { bianAutoCount() },
                    onCombine = { phase = "split2"; counted = 0; addLog(mingLog, 4, "复合为一，再分", "") },
                    onResult = { openResult() }, onAgain = { resetAll() },
                    onSheet = { sheetMing = it }
                )
                1 -> QianxuMapTab { sheetMing = it }
                2 -> QianxuHistTab(db)
            }
            Spacer(Modifier.height(30.dp))
        }

        // 详情浮层
        sheetMing?.let { QxSheet(it) { sheetMing = null } }
    }
}

// ============ 演卦 Tab ============
@Composable
private fun QianxuDivineTab(
    question: String, onQuestion: (String) -> Unit, mode: Int, onMode: (Int) -> Unit,
    state: Qianxu.State?, showMingStage: Boolean, showBianStage: Boolean,
    showMingCard: Boolean, showBianCard: Boolean, showStop: Boolean,
    phase: String, l1: Int, r1: Int, l2: Int, r2: Int, counted: Int,
    resA: Int, resB: Int, resPos: Int, isBian: Boolean,
    mingLog: List<Triple<Int, String, String>>, bianLog: List<Triple<Int, String, String>>,
    onCast: () -> Unit, onBian: () -> Unit,
    onMingSplit: (Int) -> Unit, onMingHang: () -> Unit, onMingCount: () -> Unit, onMingAutoCount: () -> Unit,
    onBianSplit: (Int) -> Unit, onBianHang: () -> Unit, onBianCount: () -> Unit, onBianAutoCount: () -> Unit,
    onCombine: () -> Unit, onResult: () -> Unit, onAgain: () -> Unit, onSheet: (QianxuData.Ming) -> Unit
) {
    // 未起卦：起卦表单
    if (state == null && !showMingStage) {
        Text("所问何事（可以不填）", color = QX.INK2, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(QX.PAPER3)
            .border(1.dp, QX.LINE, RoundedCornerShape(4.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value = question, onValueChange = onQuestion, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = QX.INK),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> if (question.isEmpty()) Text("如：此行可否？此事如何？", color = QX.INK3, fontSize = 14.sp) else inner() }
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            QxPill("演 卦", mode == 0) { onMode(0) }
            Spacer(Modifier.width(10.dp))
            QxPill("自动演示", mode == 1) { onMode(1) }
        }
        Spacer(Modifier.height(18.dp))
        QxBigButton("起 卦", onCast)
        Spacer(Modifier.height(24.dp))
        RulesBox()
        return
    }

    // 演卦舞台（定名 / 求变）
    if (showMingStage || showBianStage) {
        val bian = showBianStage
        val yang = state?.mode?.yang == true
        Stage(
            title = if (bian) "蓍 草 求 变" else "蓍 草 演 之",
            left = l1, right = r1, group = if (bian) 7 else 10,
            counted = counted, remain = when {
                bian -> resPos
                phase == "yidie1" -> resA
                phase == "yidie2" -> resB
                else -> 0
            },
            hungRight = if (bian) !yang else (phase == "hang1" || phase == "hang2" || phase == "yidie1" || phase == "yidie2"),
            info = stageInfo(phase, bian, l1, r1, resA, resB, resPos, counted),
            depleteLeft = if (bian) yang else phase == "yidie1",
            depleteRight = if (bian) !yang else phase == "yidie2",
            onLeft = when (phase) {
                "hang1", "hang2" -> onMingHang          // 定名挂一：点左堆
                "yidie1" -> onMingCount                  // 揲左：点左堆数一组
                "yidie"  -> if (yang) onBianCount else null // 求变阳：点左堆数一组
                "hang" -> if (yang) onBianHang else null  // 求变挂一：阳点左堆
                else -> null
            },
            onRight = when (phase) {
                "yidie2" -> onMingCount                  // 揲右：点右堆数一组
                "yidie"  -> if (!yang) onBianCount else null // 求变阴：点右堆数一组
                "hang" -> if (!yang) onBianHang else null // 求变挂一：阴点右堆
                else -> null
            }
        )
        Spacer(Modifier.height(10.dp))
        CastLog(if (bian) "潜虚 · 求变" else "潜虚 · 定名", if (bian) bianLog else mingLog)
        Spacer(Modifier.height(10.dp))
        // 交互提示：每个阶段都明确告诉用户点哪里
        val hint = when (phase) {
            "split1", "split2", "split" -> "点下方蓍草：这根及左边归左堆，其余归右堆"
            "hang1", "hang2" -> "点左堆任意一根，挂于右堆（或按『自动挂一』）"
            "hang" -> if (yang) "点左堆任意一根，挂于右堆（或按『自动挂一』）"
                      else "点右堆任意一根，挂于左堆（或按『自动挂一』）"
            "yidie1" -> "点左堆蓍草数一组（每点一次数一组），或按『数一组』"
            "yidie2" -> "点右堆蓍草数一组（每点一次数一组），或按『数一组』"
            "yidie"  -> if (yang) "点左堆蓍草数一组（每点一次数一组），或按『数一组』"
                        else "点右堆蓍草数一组（每点一次数一组），或按『数一组』"
            else -> ""
        }
        if (hint.isNotBlank()) {
            Text(hint, color = QX.CINNABAR, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
        }
        // 分二/挂一阶段：显示可逐根点选的蓍草条
        val showPicker = phase == "split1" || phase == "split2" || phase == "split" ||
            phase == "hang1" || phase == "hang2" || phase == "hang"
        if (showPicker) {
            StalkPicker(onPick = {
                when (phase) {
                    "split1", "split2" -> onMingSplit(it)
                    "split" -> onBianSplit(it)
                    "hang1", "hang2" -> onMingHang()   // 定名挂一：点任意根即挂
                    "hang" -> onBianHang()             // 求变挂一：点任意根即挂
                }
            })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            when (phase) {
                "hang1", "hang2" -> QxMiniBtn("自动挂一") { onMingHang() }
                "hang" -> QxMiniBtn("自动挂一") { onBianHang() }
                "yidie1", "yidie2" -> { QxMiniBtn("数一组") { onMingCount() }; Spacer(Modifier.width(8.dp)); QxMiniBtn("自动数完", ghost = true) { onMingAutoCount() } }
                "yidie" -> { QxMiniBtn("数一组") { onBianCount() }; Spacer(Modifier.width(8.dp)); QxMiniBtn("自动数完", ghost = true) { onBianAutoCount() } }
                "combine" -> QxMiniBtn("复合为一") { onCombine() }
            }
        }
        return
    }

    // 不占卡（元/餘/齊）
    if (showStop && state != null) {
        QxStopCard(state!!, onAgain)
        return
    }

    // 定名卡
    if (showMingCard && state != null) {
        MingCard(state!!, onBian, onSheet)
    }
    // 求变卡
    if (showBianCard && state != null) {
        Spacer(Modifier.height(16.dp))
        BianCard(state!!, onResult, onAgain)
    }
}

@Composable
private fun stageInfo(phase: String, bian: Boolean, l1: Int, r1: Int, a: Int, b: Int, pos: Int, counted: Int): String {
    val cnt = if (counted > 0) " · 已数 $counted 根" else ""
    return when (phase) {
        "split1" -> "取七十策 · 分而为二（第一次）"
        "split2" -> "复合为一 · 再分而为二（第二次）"
        "hang1", "hang2" -> "取左之一，挂于右"
        "yidie1" -> "揲左以十为一组$cnt"
        "yidie2" -> "揲右以十为一组$cnt"
        "combine" -> "揲左得余 $a · 复合为一"
        "split" -> "取七十策 · 分而为二"
        "hang" -> if (l1 > 0) "取左之一挂于右 · 置右揲左" else "取右之一挂于左 · 置左揲右"
        "yidie" -> "揲之以七，所余为变$cnt"
        else -> ""
    }
}

// ============ 蓍草舞台 ============
@Composable
private fun Stage(title: String, left: Int, right: Int, group: Int, counted: Int,
                  remain: Int, hungRight: Boolean, info: String,
                  depleteLeft: Boolean = false, depleteRight: Boolean = false,
                  onLeft: (() -> Unit)? = null, onRight: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        .background(QX.PAPER3).border(1.dp, QX.LINE, RoundedCornerShape(4.dp))
        .padding(14.dp)) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = QX.INK2, fontSize = 12.sp, letterSpacing = 2.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { Box(Modifier.size(width = 3.dp, height = 12.dp).border(1.dp, QX.INK3)) }
                    Spacer(Modifier.width(4.dp))
                    Text("虚其五", color = QX.INK3, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 90.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pile("左", left, group, if (depleteLeft) counted else 0, remain, showHung = !hungRight, Modifier.weight(1f), onLeft)
                Pile("右", right, group, if (depleteRight) counted else 0, remain, showHung = hungRight, Modifier.weight(1f), onRight)
            }
            Spacer(Modifier.height(10.dp))
            Text(info, color = QX.INK, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Pile(name: String, count: Int, group: Int, counted: Int, remain: Int,
                 showHung: Boolean, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Column(modifier.then(clickable).clip(RoundedCornerShape(4.dp)).border(1.dp, QX.LINE, RoundedCornerShape(4.dp))
        .padding(8.dp).heightIn(min = 90.dp)) {
        Text(name, color = QX.INK3, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        val display = (count - counted).coerceAtLeast(0)
        if (display == 0) {
            Text("—", color = QX.INK3, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        } else {
            val groups = (display + group - 1) / group
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                repeat(groups.coerceAtMost(12)) { gi ->
                    val n = kotlin.math.min(group, display - gi * group)
                    Row {
                        repeat(n) { si ->
                            val hung = showHung && gi == 0 && si == 0
                            Box(Modifier.size(width = 3.dp, height = 18.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (hung) QX.CINNABAR else QX.METAL))
                            Spacer(Modifier.width(1.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
        if (remain > 0) {
            Spacer(Modifier.height(4.dp))
            Text("余 $remain", color = QX.CINNABAR, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StalkPicker(onPick: (Int) -> Unit) {
    val scroll = rememberScrollState()
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
        for (i in 2..68) {
            // 每根热区 12dp 宽（便于手指点中），内部保留 4dp 细蓍草视觉
            Box(Modifier.size(width = 12.dp, height = 40.dp).clickable { onPick(i) },
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 4.dp, height = 22.dp).clip(RoundedCornerShape(1.dp))
                    .background(QX.METAL))
            }
        }
    }
}

@Composable
private fun CastLog(title: String, logs: List<Triple<Int, String, String>>) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        .background(QX.PAPER2.copy(alpha = 0.45f)).padding(12.dp)) {
        Column {
            Text(title, color = QX.CINNABAR, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            logs.sortedBy { it.first }.forEach { (no, t, v) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("$no.", color = QX.CINNABAR, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(t, color = QX.INK2, fontSize = 13.sp)
                        if (v.isNotBlank()) Text(v, color = QX.INK, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ============ 定名卡 ============
@Composable
private fun MingCard(s: Qianxu.State, onBian: () -> Unit, onSheet: (QianxuData.Ming) -> Unit) {
    val m = s.ming
    val (x, y) = Qianxu.pairOf(m.key)
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        .background(QX.PAPER3).border(1.dp, QX.LINE, RoundedCornerShape(4.dp))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (s.question.isNotBlank()) "所问：${s.question}" else "潜虚之名", color = QX.INK2, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(QX.wx(m.wx)))
                    Spacer(Modifier.width(4.dp))
                    Text(m.wx, color = QX.INK, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(m.name, color = QX.INK, fontSize = 72.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif)
            Text("左${x}右${y}", color = QX.INK2, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("左余 $x · 右余 $y · 数合 ${m.key}", color = QX.INK2, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            // 取用 badge
            val badge = when {
                m.key in Qianxu.NO_VAR -> "xy-pure" to "${m.name} 无变 · 不可占"
                m.key in listOf("11", "22", "33", "44", "55") -> "生数纯 · 从阳取用（显）" to ""
                m.key in listOf("66", "77", "88", "99", "1010") -> "成数纯 · 从阴取用（幽）" to ""
                s.mode.yang -> "阳名 · 先主后客（显）" to ""
                else -> "阴名 · 先客后主（幽）" to ""
            }
            val bcolor = if (m.key in Qianxu.NO_VAR) QX.INK3 else if (s.mode.yang) QX.CINNABAR else QX.WATER
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).border(1.dp, bcolor, RoundedCornerShape(3.dp))
                .padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(if (m.key in Qianxu.NO_VAR) badge.second else badge.first, color = bcolor, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(QX.PAPER2.copy(alpha = 0.4f)).padding(10.dp)) {
                Column {
                    Text("『${m.name}』${m.def}", color = QX.INK2, fontSize = 13.sp, lineHeight = 22.sp)
                    if (m.yi.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("白话：${m.yi}", color = QX.INK, fontSize = 13.sp, lineHeight = 22.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(if (Qianxu.hasSevenPos(m)) "此名七位齐备，变在初二三四五六上之间，初、上为事之始终，不占。点击查看全貌。"
                 else "元、餘、齊三者无变，皆不占。", color = QX.INK3, fontSize = 12.sp,
                modifier = Modifier.clickable { onSheet(m) })
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).background(QX.CINNABAR)
                    .clickable { onBian() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("求 变", color = QX.PAPER3, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).border(1.dp, QX.INK, RoundedCornerShape(3.dp))
                    .clickable { onSheet(m) }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("看全貌", color = QX.INK, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============ 求变卡 ============
@Composable
private fun BianCard(s: Qianxu.State, onResult: () -> Unit, onAgain: () -> Unit) {
    val m = s.ming
    val posName = Qianxu.POS_NAME[s.pos] ?: "初"
    val info = m.pos[posName]
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        .background(QX.PAPER3).border(1.5.dp, QX.INK, RoundedCornerShape(4.dp))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${m.name} · ${posName}位", color = QX.INK2, fontSize = 12.sp)
                Box(Modifier.clip(RoundedCornerShape(3.dp)).background(QX.ji(s.ji)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(s.ji, color = QX.PAPER3, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (info != null) {
                Text(info.ci, color = QX.INK, fontSize = 16.sp, lineHeight = 28.sp)
                Spacer(Modifier.height(6.dp))
                Text("解：${info.xiang}", color = QX.INK2, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("大意：${info.dayi}", color = QX.INK2, fontSize = 13.sp, lineHeight = 21.sp)
            }
            Spacer(Modifier.height(10.dp))
            if (s.mode.yin && s.jiRaw.isNotBlank()) {
                Text("取幽：此名属阴，吉凶臧否与显相反。${s.jiRaw} → ${s.ji}", color = QX.WATER, fontSize = 12.sp)
            } else {
                Text("取显：此名属阳，吉凶直读 ${s.jiRaw}", color = QX.CINNABAR, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).border(1.dp, QX.INK, RoundedCornerShape(3.dp))
                    .clickable { onAgain() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("重新起卦", color = QX.INK, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).background(QX.CINNABAR)
                    .clickable { onResult() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("查看结果", color = QX.PAPER3, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QxStopCard(s: Qianxu.State, onAgain: () -> Unit) {
    Spacer(Modifier.height(30.dp))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(QX.PAPER3)
        .border(1.5.dp, QX.INK, RoundedCornerShape(4.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("不 占", color = QX.INK, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("${s.ming.name} · ${s.ming.key}", color = QX.INK2, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("元、餘、齊三者无变，皆不占。欲判后事吉凶，须再筮。", color = QX.INK2, fontSize = 13.sp,
                textAlign = TextAlign.Center, lineHeight = 22.sp)
            Spacer(Modifier.height(20.dp))
            QxBigButton("再 筮", onAgain)
        }
    }
}

// ============ 命图 Tab ============
@Composable
private fun QianxuMapTab(onOpen: (QianxuData.Ming) -> Unit) {
    var query by remember { mutableStateOf("") }
    Text("潜 虚 命 图", color = QX.INK, fontSize = 22.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
    Text("五十五名 · 五行生成之数 · 点击查看名辞", color = QX.INK2, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(QX.PAPER3)
        .border(1.dp, QX.LINE, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        androidx.compose.foundation.text.BasicTextField(value = query, onValueChange = { query = it }, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = QX.INK),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner -> if (query.isEmpty()) Text("搜索：卦名 / 数合 / 五行", color = QX.INK3, fontSize = 13.sp) else inner() })
    }
    Spacer(Modifier.height(14.dp))
    val filtered = remember(query) {
        if (query.isBlank()) QianxuData.DATA
        else QianxuData.DATA.filter { m ->
            m.name.contains(query) || m.key.contains(query) || m.wx.contains(query) || m.def.contains(query) || m.yi.contains(query)
        }
    }
    if (filtered.isEmpty()) {
        Text("未找到含「$query」的语句", color = QX.INK3, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    } else {
        val cols = 6
        val rows = (filtered.size + cols - 1) / cols
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until cols) {
                    val idx = r * cols + c
                    Box(Modifier.weight(1f)) {
                        if (idx < filtered.size) {
                            val m = filtered[idx]
                            val noVar = m.key in Qianxu.NO_VAR
                            val (x, y) = Qianxu.pairOf(m.key)
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                                .background(QX.PAPER3).border(1.dp, if (noVar) QX.LINE else QX.INK2, RoundedCornerShape(4.dp))
                                .clickable { onOpen(m) }.padding(vertical = 10.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(m.name, color = if (noVar) QX.INK3 else QX.INK, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1)
                                Text("$x·$y", color = QX.INK3, fontSize = 10.sp)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(QX.wx(m.wx)))
                                    Spacer(Modifier.width(2.dp))
                                    Text(m.wx, color = QX.INK2, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ============ 记录 Tab ============
@Composable
private fun QianxuHistTab(db: FortuneDb) {
    val records = remember { db.recent(100).filter { it.method == "qianxu" } }
    Text("潜 虚 记 录", color = QX.INK, fontSize = 22.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
    Text("起卦自动留存 · 历史最多保留 100 条", color = QX.INK2, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    if (records.isEmpty()) {
        Text("暂无记录", color = QX.INK3, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    } else {
        records.take(100).forEach { rec ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(QX.PAPER3)
                .border(1.dp, QX.LINE, RoundedCornerShape(4.dp)).padding(12.dp)) {
                Column {
                    Text("${rec.createdAt} · ${rec.question}", color = QX.INK3, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(rec.face, color = QX.INK, fontSize = 12.sp, maxLines = 3, lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ============ 结果页 ============
@Composable
private fun QianxuResultPage(s: Qianxu.State, onBack: () -> Unit, onAgain: () -> Unit, onCopy: () -> Unit) {
    val m = s.ming
    val (x, y) = Qianxu.pairOf(m.key)
    val posName = Qianxu.POS_NAME[s.pos] ?: ""
    val info = if (posName.isNotBlank()) m.pos[posName] else null
    val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    Box(Modifier.fillMaxSize().background(QX.PAPER)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("‹ 返回", color = QX.CINNABAR, fontSize = 15.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.height(16.dp))
            Text("潜 虚", color = QX.CINNABAR, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, letterSpacing = 6.sp)
            Spacer(Modifier.height(6.dp))
            Text("起卦时间：$time${if (s.question.isNotBlank()) " · 所问：${s.question}" else ""}",
                color = QX.INK3, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            // 定名卡
            QxResultCard("定 名") {
                Text(m.name, color = QX.INK, fontSize = 56.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("左余 $x · 右余 $y · 数合 ${m.key} · 五行属${m.wx}",
                    color = QX.INK2, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                val modeText = when {
                    m.key in Qianxu.NO_VAR -> "${m.name}·无变·不占"
                    m.key in listOf("11", "22", "33", "44", "55") -> "生数纯 · 从阳取显"
                    m.key in listOf("66", "77", "88", "99", "1010") -> "成数纯 · 从阴取幽"
                    s.mode.yang -> "阳名 · 先主后客 · 用显"
                    else -> "阴名 · 先客后主 · 用幽"
                }
                Spacer(Modifier.height(10.dp))
                Text(modeText, color = if (s.mode.yang) QX.CINNABAR else QX.WATER, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                        .border(1.dp, if (s.mode.yang) QX.CINNABAR else QX.WATER, RoundedCornerShape(3.dp)).padding(vertical = 5.dp),
                    textAlign = TextAlign.Center)
                if (m.total.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(m.total.replace("||", " · "), color = QX.INK, fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, lineHeight = 24.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("『${m.name}』${m.def}", color = QX.INK2, fontSize = 13.sp, lineHeight = 22.sp)
                if (m.yi.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("白话：${m.yi}", color = QX.INK, fontSize = 13.sp, lineHeight = 22.sp)
                }
            }
            // 求变卡
            Spacer(Modifier.height(16.dp))
            QxResultCard(if (s.pos in 1..7) "求 变" else "此名无变") {
                if (s.pos in 1..7 && s.pos !in listOf(1, 7)) {
                    Text(s.ji, color = QX.ji(s.ji), fontSize = 48.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Text("变在${posName}位", color = QX.INK2, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (s.ji.isNotBlank() && s.ji != "吉凶不现" && s.pos in 2..6) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("显读 ${s.jiRaw}", color = if (s.mode.yang) QX.CINNABAR else QX.INK3,
                            fontSize = 13.sp, fontWeight = if (s.mode.yang) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.width(16.dp))
                        Text("幽读 ${Qianxu.revJi(s.jiRaw)}", color = if (s.mode.yin) QX.WATER else QX.INK3,
                            fontSize = 13.sp, fontWeight = if (s.mode.yin) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                if (info != null && s.pos in 1..7) {
                    Spacer(Modifier.height(10.dp))
                    Text("『${info.ci}』", color = QX.INK, fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, lineHeight = 26.sp)
                    if (info.dayi.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("大意：${info.dayi}", color = QX.INK2, fontSize = 13.sp, lineHeight = 22.sp)
                    }
                    Text("解曰：${info.xiang}", color = QX.INK2, fontSize = 12.sp, lineHeight = 21.sp)
                }
                val note = when {
                    m.key in Qianxu.NO_VAR -> "元、餘、齊三者无变，皆不占。欲判后事吉凶，须再筮。"
                    s.pos == 1 -> "变在初位，初为事之始，不占。欲知后事吉凶，请再筮。"
                    s.pos == 7 -> "变在上位，上为事之终，不占。欲知后事吉凶，请再筮。"
                    s.pos == 0 -> "仅定名，尚未求变。"
                    s.ji == "吉凶不现" -> "本位移吉凶不现，直读爻辞。"
                    s.mode.yin -> "取幽：此名属阴，吉凶臧否与显相反。"
                    else -> "取显：此名属阳，吉凶直读。"
                }
                Spacer(Modifier.height(12.dp))
                Text(note, color = QX.INK3, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, lineHeight = 21.sp)
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).border(1.dp, QX.INK, RoundedCornerShape(3.dp))
                    .clickable { onCopy() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("复制结果", color = QX.INK, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).background(QX.CINNABAR)
                    .clickable { onAgain() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("重新起卦", color = QX.PAPER3, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(QX.CINNABAR)
                .clickable {
                    val cb = com.fortune.stall.util.AiBridge.askAi
                    if (cb != null) cb(Qianxu.faceText(s, time))
                }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Text("✦ 问 AI 解读", color = QX.PAPER3, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun QxResultCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(QX.PAPER2.copy(alpha = 0.35f))
        .border(1.dp, QX.LINE, RoundedCornerShape(6.dp)).padding(20.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = QX.INK3, fontSize = 12.sp, letterSpacing = 5.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ============ 名辞详情浮层 ============
@Composable
private fun QxSheet(m: QianxuData.Ming, onClose: () -> Unit) {
    val (x, y) = Qianxu.pairOf(m.key)
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onClose() }) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 580.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(QX.PAPER3)
            .clickable(enabled = false) {}) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(m.name, color = QX.INK, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Text("${m.key} · 左${x}右${y}", color = QX.INK2, fontSize = 13.sp)
                    }
                    Text("×", color = QX.INK3, fontSize = 24.sp, modifier = Modifier.clickable { onClose() })
                }
                Spacer(Modifier.height(8.dp))
                Text("五行属${m.wx}${when { m.key in listOf("11", "22", "33", "44", "55") -> " · 生数纯（从阳取显）"; m.key in listOf("66", "77", "88", "99", "1010") -> " · 成数纯（从阴取幽）"; else -> "" }}",
                    color = QX.INK2, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(QX.PAPER2.copy(alpha = 0.4f)).padding(12.dp)) {
                    Column {
                        Text(m.def, color = QX.INK2, fontSize = 14.sp, lineHeight = 24.sp)
                        if (m.yi.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("白话：${m.yi}", color = QX.INK, fontSize = 13.sp, lineHeight = 22.sp)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (m.pos.isNotEmpty()) {
                    listOf("初", "二", "三", "四", "五", "六", "上").forEach { pk ->
                        m.pos[pk]?.let { p ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(pk, color = QX.CINNABAR, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                                Column(Modifier.weight(1f)) {
                                    Text("${p.ji}  ${p.ci}", color = QX.INK, fontSize = 13.sp)
                                    Text("大意：${p.dayi}", color = QX.INK2, fontSize = 12.sp)
                                    Text(p.xiang, color = QX.INK3, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                } else if (m.total.isNotBlank()) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                        .background(QX.CINNABAR.copy(alpha = 0.06f)).border(1.dp, QX.CINNABAR.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                        .padding(12.dp)) {
                        Text(m.total.replace("||", " · "), color = QX.INK, fontSize = 14.sp, lineHeight = 24.sp)
                    }
                } else {
                    Text("此名无变，不占。", color = QX.INK3, fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ============ 通用小组件 ============
@Composable
private fun QxPill(text: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(3.dp)).background(if (on) QX.INK else QX.PAPER3)
        .border(1.dp, QX.INK, RoundedCornerShape(3.dp)).clickable { onClick() }
        .padding(horizontal = 22.dp, vertical = 8.dp)) {
        Text(text, color = if (on) QX.PAPER3 else QX.INK, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QxBigButton(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(QX.INK)
        .clickable { onClick() }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
        Text(text, color = QX.PAPER3, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
    }
}

@Composable
private fun QxMiniBtn(text: String, ghost: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(3.dp))
        .background(if (ghost) Color.Transparent else QX.PAPER3)
        .border(1.dp, QX.INK2, RoundedCornerShape(3.dp)).clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text, color = QX.INK, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RulesBox() {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).border(1.dp, QX.LINE, RoundedCornerShape(4.dp))
        .background(QX.PAPER2.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp)) {
            Text("演卦之法  ＋", color = QX.INK2, fontSize = 13.sp, modifier = Modifier.clickable { open = !open })
            if (open) {
                Spacer(Modifier.height(8.dp))
                Text("取七十策，虚其五不用。分而为二，取一挂一；以十为一组揲之，归其余，得左余数；复合为一再分，同法得右余数。左右余数合成一名（五十五名之一）。\n求变则复取七十策，以七为一组揲之，所余即变位（初一至六上）。阳名（生数纯）从右揲左取显，阴名（成数纯）从左揲右取幽；吉凶臧否，阴者与显相反。初、上为始终，不占。",
                    color = QX.INK2, fontSize = 12.sp, lineHeight = 21.sp)
            }
        }
    }
}

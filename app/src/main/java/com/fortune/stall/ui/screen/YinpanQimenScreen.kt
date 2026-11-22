package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.jishiyu.Qimen
import com.fortune.stall.engine.jishiyu.QimenJu
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val YP_RED = Color(0xFFB0392E)
private val YP_GOLD = Color(0xFFD9A441)
private val YP_LINE = Color(0x52D9A441)
private val YP_CARD = Color(0x14FFFFFF)

/**
 * 阴盘奇门专用起卦页 —— 依据 ji.js.cn 阴盘奇门。
 * 事项 + 起局时间 + 起局方式 → 阴盘九宫盘详情页（翻局/长生/地盘八神），自动存档。
 */
@Composable
fun YinpanQimenCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var detail by remember { mutableStateOf<Triple<LocalDateTime, Int, String>?>(null) }
    if (detail == null) {
        JiMingpanCastForm(title = "阴 盘 奇 门", onBack = onBack) { f ->
            try {
                val method = 1
                val ju = QimenJu.ju(f.dateTime, method)
                val r = Qimen.paipan(f.dateTime, true, ju.cusJushu)
                val q = f.name.ifBlank { "阴盘奇门 · 起局" }
                detail = Triple(f.dateTime, method, q)
                if (!f.noSave) {
                    val createdAt = f.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    val id = db.insert(createdAt, q, "yinpanqimen", ju.methodName, "排盘中…", ju.name)
                    db.updateFace(id, yinpanFaceText(r, q, createdAt, ju.methodName, ju.name))
                    Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    detail?.let { (dt, method, q) -> YinpanDetailScreen(dt = dt, method = method, question = q, onBack = { detail = null }) }
}

@Composable
private fun YinpanDetailScreen(dt: LocalDateTime, method: Int, question: String, onBack: () -> Unit) {
    var juOffset by remember { mutableStateOf(0) }
    var showZS by remember { mutableStateOf(true) }
    var showDiShen by remember { mutableStateOf(true) }
    var customJu by remember { mutableStateOf<Int?>(null) }
    var showJuPicker by remember { mutableStateOf(false) }
    val curDt = remember(juOffset) { dt.plusHours((juOffset * 2).toLong()) }
    val ju = remember(juOffset) { QimenJu.ju(curDt, method) }
    val curJuNum = customJu ?: ju.num            // 当前局数（正 1..9，用于显示）
    val cusJu = if (ju.yangDun) curJuNum else -curJuNum  // 引擎需带符号：正=阳遁 / 负=阴遁
    val r = remember(juOffset, customJu) { try { Qimen.paipan(curDt, true, cusJu) } catch (e: Exception) { null } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        // 标题栏（紧凑）
        Box(Modifier.fillMaxWidth().background(YP_RED).padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("阴盘奇门", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(26.dp))
            }
        }
        if (r == null) { Text("排盘数据异常", color = Color(0xFF333333), fontSize = 14.sp, modifier = Modifier.padding(20.dp)); return@Column }
        val rr = r!!

        // ===== 紧凑信息区 =====
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 2.dp)) {
            YpInfoRow("事项", question, Color(0xFF777777))
            YpInfoRow("日期", rr.date, Color(0xFF222222))
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("节气", color = Color(0xFF2E6FA8), fontSize = 12.sp, modifier = Modifier.width(44.dp))
                Text(rr.jieqi, color = Color(0xFF222222), fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("月将${rr.yueJiang}", color = Color(0xFF222222), fontSize = 12.sp,
                    modifier = Modifier.padding(end = 10.dp))
                Text("${if (ju.yangDun) "阳遁" else "阴遁"}${curJuNum}局", color = Color(0xFF222222), fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                for (h in listOf("值符", "值使", "旬首", "马星", "空亡")) Text(h, color = Color(0xFF2E6FA8),
                    fontSize = 11.5.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 3.dp)) {
                for (v in listOf(rr.zhifu, rr.zhishi, rr.xunhead, rr.maxing, rr.kongwang)) Text(v,
                    color = Color(0xFF222222), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        // ===== 四柱（竖排，省高度）=====
        Column(Modifier.fillMaxWidth().background(Color(0xFFEFEDE6)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("四柱", color = QmBoardTheme.GRAY, fontSize = 11.sp, modifier = Modifier.width(44.dp))
                for (h in listOf("年柱", "月柱", "日柱", "时柱")) Text(h, color = Color(0xFF2E6FA8), fontSize = 11.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(44.dp))
                for (p in rr.siZhu) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        for (ch in p) Text(ch.toString(), color = Color(0xFF222222), fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }

        // ===== 九宫盘 + 外围月将建除圈 =====
        YpBoardWithRing(rr, ju.yangDun, curJuNum, showZS, showDiShen)
        Spacer(Modifier.height(3.dp))

        // ===== 符号颜色说明 =====
        YpLegend()
        Spacer(Modifier.height(4.dp))

        // ===== 按钮组 =====
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QmRedButton("上一局", Modifier.weight(1f)) { juOffset -= 1; customJu = null }
            QmRedButton("当前时盘", Modifier.weight(1f)) { juOffset = 0; customJu = null; showJuPicker = false }
            QmRedButton("下一局", Modifier.weight(1f)) { juOffset += 1; customJu = null }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            QmRedButton(if (showJuPicker) "收起选局" else "自选局", Modifier.width(120.dp)) { showJuPicker = !showJuPicker }
        }
        if (showJuPicker) {
            YpJuPicker(ju.yangDun, curJuNum, customJu) { picked -> customJu = picked; showJuPicker = false }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YpOutlineButton("长生状态", showZS, Modifier.weight(1f)) { showZS = !showZS }
            YpOutlineButton("天门地户", showDiShen, Modifier.weight(1f)) { showDiShen = !showDiShen }
            YpOutlineButton("九宫全景", false, Modifier.weight(1f)) { showZS = true; showDiShen = true }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable private fun YpInfoRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF2E6FA8), fontSize = 12.sp, modifier = Modifier.width(44.dp))
        Text(value, color = valueColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable private fun YpOutlineButton(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
        .border(1.dp, YP_RED, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = YP_RED, fontSize = 12.5.sp)
    }
}

/** 月将名（十二地支 → 月将），用于外围建除圈 */
private val YP_YUEJIANG_NAME = mapOf(
    "亥" to "登明", "子" to "神后", "丑" to "大吉", "寅" to "功曹", "卯" to "太冲", "辰" to "天罡",
    "巳" to "太乙", "午" to "胜光", "未" to "小吉", "申" to "传送", "酉" to "从魁", "戌" to "河魁"
)
/** 建除十二神顺序（月将地支起"建"顺行） */
private val YP_JIANCHU = listOf("建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭")
private val YP_DIZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
/** 十二地支在九宫盘外围的位置（顺时针自顶行左起：顶/右/底/左） */
private val YP_RING_BRANCHES = listOf("亥", "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌")

private fun ypJianchuFor(branch: String, yueJiang: String): String {
    val yjIdx = YP_DIZHI.indexOf(yueJiang).coerceAtLeast(0)
    val bIdx = YP_DIZHI.indexOf(branch).coerceAtLeast(0)
    return YP_JIANCHU[(bIdx - yjIdx + 12) % 12]
}

/** 九宫盘 + 外围一圈"月将名+地支+建除"，仿原版紧凑布局 */
@Composable
internal fun YpBoardWithRing(r: Qimen.Result, yangDun: Boolean, juShu: Int,
                             showZS: Boolean, showDiShen: Boolean,
                             highlightGong: String? = null) {
    val cellH = 74.dp
    val ringLabelW = 46.dp
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 顶行三支（亥 子 丑）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in 0..2) YpRingLabel(YP_RING_BRANCHES[i], r.yueJiang)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 左列三支（申 酉 戌）
            Column(Modifier.width(ringLabelW), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly) {
                for (i in 9..11) YpRingBranch(YP_RING_BRANCHES[i], r.yueJiang)
            }
            // 九宫格
            Column(Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF555555), RoundedCornerShape(4.dp)).background(Color.White)) {
                QM_VISUAL.forEachIndexed { ri, row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEachIndexed { ci, g ->
                            YpCell(g, r.pan[g] ?: Qimen.Cell(), Modifier.weight(1f), cellH, showZS, showDiShen,
                                highlight = (g == highlightGong))
                            if (ci < 2) Box(Modifier.width(1.dp).height(cellH).background(Color(0xFFBBBBBB)))
                        }
                    }
                    if (ri < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFBBBBBB)))
                }
            }
            // 右列三支（寅 卯 辰）
            Column(Modifier.width(ringLabelW), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly) {
                for (i in 3..5) YpRingBranch(YP_RING_BRANCHES[i], r.yueJiang)
            }
        }
        // 底行三支（未 午 巳）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in 8 downTo 6) YpRingLabel(YP_RING_BRANCHES[i], r.yueJiang)
        }
    }
}

@Composable private fun YpRingLabel(branch: String, yueJiang: String) {
    val jc = ypJianchuFor(branch, yueJiang)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(YP_YUEJIANG_NAME[branch] ?: branch, color = Color(0xFF222222), fontSize = 12.sp)
        Text(branch, color = Color(0xFF222222), fontSize = 12.sp)
        Text(jc, color = ypJcColor(jc), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun YpRingBranch(branch: String, yueJiang: String) {
    val jc = ypJianchuFor(branch, yueJiang)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(YP_YUEJIANG_NAME[branch] ?: branch, color = Color(0xFF222222), fontSize = 12.sp)
        Text(branch, color = Color(0xFF222222), fontSize = 12.sp)
        Text(jc, color = ypJcColor(jc), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun ypJcColor(jc: String) = when (jc) {
    "建", "满", "成" -> Color(0xFFC8842A)      // 橙
    "除", "定", "开" -> Color(0xFF3E9B4F)      // 绿
    "危", "收", "执" -> Color(0xFF1F9AA8)      // 青
    "破" -> Color(0xFFC0392B)                    // 红
    else -> Color(0xFF222222)
}

/** 紧凑单元格（仿原版：八神/九星+天盘/八门+地盘 + 隐干 + 长生小字） */
@Composable
private fun YpCell(gong: String, c: Qimen.Cell, modifier: Modifier, h: androidx.compose.ui.unit.Dp,
                   showZS: Boolean, showDiShen: Boolean, highlight: Boolean = false) {
    Column(modifier.height(h).padding(horizontal = 3.dp, vertical = 2.dp)
        .then(if (highlight) Modifier.background(Color(0xFFFFF0A8)).border(2.dp, Color(0xFFD32F2F)) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        if (gong == "中") {
            Text(c.dipan, color = Color(0xFFAAAAAA), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
            // 第一行：八神 + 九星(左) / 天盘干+长生(右)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(qmFullBaShen(c.bashen), color = Color(0xFF222222), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (c.ma) Text(" 马", color = Color(0xFFC0392B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    if (c.yingan.isNotEmpty()) Text(c.yingan, color = Color(0xFF999999), fontSize = 9.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.tianpan12.isNotEmpty())
                        Text(c.tianpan12 + if (c.tianpanJi12.isNotEmpty()) c.tianpanJi12 else "", color = QmBoardTheme.BLUE, fontSize = 8.sp)
                    Text(c.tianpan, color = qmGanColor(c.tianpan, c.tianpan_jixing, c.tianpan_rumu),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            // 第二行：九星·门(左) / 地盘干+长生(右)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(qmFullJiuXing(c.jiuxing), color = Color(0xFF222222), fontSize = 11.sp)
                    Text(qmFullMen(c.men), color = if (c.men_po) Color(0xFFC9A227) else Color(0xFF222222), fontSize = 11.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.dipan12.isNotEmpty())
                        Text(c.dipan12 + if (c.dipanJi12.isNotEmpty()) c.dipanJi12 else "", color = QmBoardTheme.BLUE, fontSize = 8.sp)
                    Text(c.dipan, color = qmGanColor(c.dipan, c.dipan_jixing, c.dipan_rumu),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** 符号颜色说明行 */
@Composable internal fun YpLegend() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("符号颜色：", color = Color(0xFF222222), fontSize = 11.sp)
            Text("入墓", color = Color(0xFF9A9A9A), fontSize = 11.sp)
            Text("、", color = Color(0xFF888888), fontSize = 11.sp)
            Text("击刑", color = Color(0xFF8E44AD), fontSize = 11.sp)
            Text("、", color = Color(0xFF888888), fontSize = 11.sp)
            Text("门迫", color = Color(0xFFC0392B), fontSize = 11.sp)
            Text("、", color = Color(0xFF888888), fontSize = 11.sp)
            Text("刑+墓", color = Color(0xFF2E6FA8), fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("　　　　　", color = Color.Transparent, fontSize = 11.sp)
            Text("天三门", color = Color(0xFF1F9AA8), fontSize = 11.sp)
            Text("、", color = Color(0xFF888888), fontSize = 11.sp)
            Text("地四户", color = Color(0xFF3E9B4F), fontSize = 11.sp)
        }
    }
}

/** 自选局选择器（阴/阳遁 1~9 局） */
@Composable private fun YpJuPicker(yangDun: Boolean, curJu: Int, selected: Int?, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(8.dp)).background(Color.White)
        .border(1.dp, YP_LINE, RoundedCornerShape(8.dp)).padding(10.dp)) {
        Text("自选局 · ${if (yangDun) "阳遁" else "阴遁"}（当前第 $curJu 局）", color = QmBoardTheme.GRAY, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        for (row in 0..2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 1..3) {
                    val n = row * 3 + col
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(if (selected == n) YP_RED else Color(0xFFF0EBE0))
                        .clickable { onPick(n) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text("${n}局", color = if (selected == n) Color.White else Color(0xFF444444), fontSize = 13.sp)
                    }
                }
            }
            if (row < 2) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable private fun YpToggle(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(if (active) YP_RED else Color(0x18FFFFFF))
        .border(1.dp, YP_LINE, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (active) Color.White else QmBoardTheme.GRAY, fontSize = 13.sp)
    }
}

@Composable private fun YpNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(YP_CARD).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("‹ 返回", color = YP_GOLD, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f)); Spacer(Modifier.width(52.dp))
    }
}

@Composable private fun YpRadioLine(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(88.dp))
        options.forEachIndexed { idx, name ->
            Row(Modifier.clickable { onSelect(idx) }.padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(17.dp).clip(RoundedCornerShape(50))
                    .background(if (selected == idx) YP_RED else Color.Transparent)
                    .then(if (selected == idx) Modifier else Modifier.border(1.dp, YP_LINE, RoundedCornerShape(50))),
                    contentAlignment = Alignment.Center) {
                    if (selected == idx) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
                }
                Spacer(Modifier.width(5.dp))
                Text(name, color = if (selected == idx) YP_GOLD else AppColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

private fun yinpanFaceText(r: Qimen.Result, q: String, createdAt: String, jufa: String, juName: String) = buildString {
    appendLine("阴 盘 奇 门"); appendLine("事项：$q"); appendLine("时间：$createdAt")
    appendLine("起局：$jufa · $juName"); appendLine("四柱：${r.siZhu.joinToString(" ")}")
    appendLine("节气：${r.jieqi}")
    appendLine("旬首：${r.xunhead} 空亡：${r.kongwang} 值符：${r.zhifu} 值使：${r.zhishi} 马星：${r.maxing}")
    for (row in QM_VISUAL) appendLine(row.joinToString("  ") { g ->
        val c = r.pan[g]!!; "[$g ${qmFullBaShen(c.bashen)}·${qmFullJiuXing(c.jiuxing)}${c.tianpan}${qmFullMen(c.men)}·${c.dipan}]"
    })
}.trim()

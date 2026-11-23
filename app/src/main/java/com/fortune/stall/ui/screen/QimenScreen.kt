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
import androidx.compose.ui.window.Dialog
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
import com.fortune.stall.engine.jishiyu.QimenData
import com.fortune.stall.engine.jishiyu.QimenJu
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val QM_RED = Color(0xFFB0392E)
private val QM_GOLD = Color(0xFFD9A441)
private val QM_LINE = Color(0x52D9A441)
private val QM_CARD = Color(0x14FFFFFF)

/**
 * 奇门遁甲专用起卦页：排盘时间、起局方式(拆补/置闰/茅山)、暗干起法(值使门起/门地盘起)。
 * 点击开始排盘 → 仿原版九宫格详情页 → 自动存档。
 */
@Composable
fun QimenCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var dtText by remember { mutableStateOf(LocalDateTime.now().format(dtFmt)) }
    var question by remember { mutableStateOf("") }
    var juFa by remember { mutableStateOf(0) }       // 0 拆补 / 1 置闰 / 2 茅山
    var anGan by remember { mutableStateOf(0) }      // 0 值使门起 / 1 门地盘起
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var detailDt by remember { mutableStateOf<LocalDateTime?>(null) } // 进入详情页的起局时间
    var detailMethod by remember { mutableStateOf(1) }               // 进入详情页的起局方式(1拆补/2置闰/3茅山)

    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            QmNavBar("奇 门 遁 甲", onBack)
            Spacer(Modifier.height(18.dp))

            // 事项
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("事项：", color = AppColors.Paper, fontSize = 14.sp)
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(QM_CARD)
                        .border(1.dp, QM_LINE, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (question.isEmpty()) Text("输入预测事项（选填）", color = AppColors.Dim, fontSize = 13.sp)
                    BasicTextField(
                        value = question, onValueChange = { question = it }, singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = AppColors.Paper),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // 排盘时间
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("排盘时间：", color = AppColors.Paper, fontSize = 14.sp)
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(QM_CARD)
                        .border(1.dp, QM_LINE, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (dtText.length >= 10) dtText.substring(0, 10) else dtText,
                        color = QM_GOLD, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { showDate = true })
                    Spacer(Modifier.width(10.dp))
                    if (dtText.length >= 16) Text(dtText.substring(11), color = QM_GOLD, fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.clickable { showTime = true })
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(QM_CARD).border(1.dp, QM_LINE, RoundedCornerShape(50))
                    .clickable { dtText = LocalDateTime.now().format(dtFmt) }.padding(10.dp)) {
                    Text("↻", color = Color(0xFFC0392B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))

            // 起局方式
            RadioLine(label = "起局方式：", options = listOf("拆补", "置闰", "茅山"), selected = juFa) { juFa = it }
            Spacer(Modifier.height(12.dp))
            // 暗干起法
            RadioLine(label = "暗干起法：", options = listOf("值使门起", "门地盘起"), selected = anGan) { anGan = it }
            Spacer(Modifier.height(24.dp))

            // 开始排盘
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8A2A22), QM_RED)))
                    .clickable {
                        try {
                            val dt = LocalDateTime.parse(dtText.trim(), dtFmt)
                            val method = juFa + 1   // 0/1/2 → 1拆补/2置闰/3茅山
                            val ju = QimenJu.ju(dt, method)
                            val r = Qimen.paipan(dt, true, ju.cusJushu)
                            detailDt = dt
                            detailMethod = method
                            // 存档
                            val q = question.ifBlank { "奇门遁甲 · 排盘" }
                            val jufaName = ju.methodName
                            val createdAt = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            val id = db.insert(createdAt, q, "qimen", jufaName, "落印中…", ju.name)
                            db.updateFace(id, qimenFaceText(r, q, createdAt, jufaName, ju.name))
                            Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }.padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("开 始 排 盘", color = Color.White, fontSize = 16.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showDate) {
            QmDateDialog(
                initial = try { java.time.LocalDate.parse(dtText.substring(0, 10)) } catch (e: Exception) { java.time.LocalDate.now() },
                onDismiss = { showDate = false },
                onConfirm = { d ->
                    val t = dtText.substring(11).ifEmpty { "00:00" }
                    dtText = "${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $t"; showDate = false
                }
            )
        }
        if (showTime) {
            QmTimeDialog(
                initial = try { java.time.LocalTime.parse(dtText.substring(11)) } catch (e: Exception) { java.time.LocalTime.now() },
                onDismiss = { showTime = false },
                onConfirm = { t ->
                    dtText = "${dtText.substring(0, 10)} ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"; showTime = false
                }
            )
        }

        // 仿原版详情页覆盖整个起卦页
        detailDt?.let { dt ->
            QimenDetailScreen(
                dt = dt,
                method = detailMethod,
                question = question.ifBlank { "输入预测事项" },
                onBack = { detailDt = null }
            )
        }
    }
}

@Composable
private fun RadioLine(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(88.dp))
        options.forEachIndexed { idx, name ->
            Row(Modifier.clickable { onSelect(idx) }.padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(17.dp).clip(RoundedCornerShape(50))
                        .background(if (selected == idx) QM_RED else Color.Transparent)
                        .then(if (selected == idx) Modifier else Modifier.border(1.dp, QM_LINE, RoundedCornerShape(50))),
                    contentAlignment = Alignment.Center
                ) { if (selected == idx) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White)) }
                Spacer(Modifier.width(5.dp))
                Text(name, color = if (selected == idx) QM_GOLD else AppColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

/** 仿原版奇门详情页：信息表 + 四柱 + 3x3 九宫格 + 符号颜色 + 翻局按钮 */
@Composable
private fun QimenDetailScreen(
    dt: java.time.LocalDateTime,
    method: Int,
    question: String,
    onBack: () -> Unit
) {
    // 翻局：原版上一局/下一局为 ±1 时辰(2小时)，到相邻时辰后按起局方式重新算局
    var juOffset by remember { mutableStateOf(0) }
    var showZoom by remember { mutableStateOf(false) }   // 点击放大九宫格浮层
    var showZhangsheng by remember { mutableStateOf(true) }  // 显示十二长生状态
    var showDiShen by remember { mutableStateOf(true) }      // 显示地盘八神
    val curDt = remember(juOffset) { dt.plusHours((juOffset * 2).toLong()) }
    val ju = remember(juOffset) { QimenJu.ju(curDt, method) }
    val result = remember(juOffset) {
        try { Qimen.paipan(curDt, true, ju.cusJushu) } catch (e: Exception) { null }
    }
    fun flipJu(delta: Int) { juOffset += delta }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        // 顶部红色标题栏（紧凑）
        Box(Modifier.fillMaxWidth().background(QM_RED).padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("奇门遁甲", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(26.dp))
            }
        }

        if (result == null) {
            Text("排盘数据异常", color = Color(0xFF333333), fontSize = 14.sp, modifier = Modifier.padding(20.dp))
            return@Column
        }
        val r = result!!

        // 信息表（紧凑）
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 2.dp)) {
            QmInfoRow("事项", question)
            QmInfoRow("局数", "${ju.methodName} · ${ju.name}")
            QmInfoRow("日期", r.date)
            QmInfoRow("节气", r.jieqi)
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                for (h in listOf("值符", "值使", "旬首", "马星", "空亡")) {
                    Text(h, color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 3.dp)) {
                for (v in listOf(r.zhifu, r.zhishi, r.xunhead, r.maxing, r.kongwang)) {
                    Text(v, color = Color(0xFF2A6DB0), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // 四柱（点击可放大九宫格）
        Column(Modifier.fillMaxWidth().background(Color(0xFFEFEDE6)).padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable { showZoom = true }) {
            Row(Modifier.fillMaxWidth()) {
                Text("四柱", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(0.5f))
                for (h in listOf("年柱", "月柱", "日柱", "时柱")) {
                    Text(h, color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
                Text("⤢", color = Color(0xFFB0392E), fontSize = 14.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(0.5f))
                for (p in r.siZhu) {
                    Text(p, color = Color(0xFF222222), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.weight(0.5f))
            }
        }
        Spacer(Modifier.height(6.dp))

        // 3x3 九宫格（洛书布局）——点击放大
        val visual = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))
        val cellH = 76.dp
        // 原版遁甲盘灰底：阳遁 4巽/3震/8艮/1坎，阴遁 9离/2坤/7兑/6乾
        val grayGongs = if (r.yangDun) setOf("巽", "震", "艮", "坎") else setOf("离", "坤", "兑", "乾")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF666666), RoundedCornerShape(6.dp))
                .background(Color.White).clickable { showZoom = true }
        ) {
            visual.forEachIndexed { rIdx, row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEachIndexed { cIdx, g ->
                        val cell = r.pan[g] ?: Qimen.Cell()
                        QimenCellView(g, cell, Modifier.weight(1f), showZhangsheng, showDiShen,
                            gray = g in grayGongs)
                        if (cIdx < 2) Box(Modifier.width(1.dp).height(cellH).background(Color(0xFFBBBBBB)))
                    }
                }
                if (rIdx < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFBBBBBB)))
            }
        }
        Spacer(Modifier.height(6.dp))

        // 符号颜色说明
        Text("符号颜色：", color = Color(0xFF888888), fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)) {
            Text("入墓", color = Color(0xFF999999), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("击刑", color = Color(0xFFB0392E), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("门迫", color = Color(0xFFC9A227), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("刑+墓", color = Color(0xFF2A6DB0), fontSize = 11.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        // 翻局按钮
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RedPill("上一局", Modifier.weight(1f)) { flipJu(-1) }
            RedPill("当前盘", Modifier.weight(1f)) { juOffset = 0 }
            RedPill("下一局", Modifier.weight(1f)) { flipJu(1) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TogglePill("长生状态", showZhangsheng, Modifier.weight(1f)) { showZhangsheng = !showZhangsheng }
            TogglePill("地盘八神", showDiShen, Modifier.weight(1f)) { showDiShen = !showDiShen }
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(
            faceText = qimenFaceText(r, question, curDt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), ju.methodName, ju.name)
        )
        Spacer(Modifier.height(6.dp))

        // 点击四柱/九宫格弹出的放大浮层（仿原版）
        if (showZoom) {
            QimenZoomDialog(r = r, siZhu = r.siZhu, showZhangsheng = showZhangsheng,
                showDiShen = showDiShen, onDismiss = { showZoom = false })
        }
    }
}

@Composable
private fun RedPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(QM_RED).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 12.5.sp)
    }
}

/** 开关按钮：开启=金色高亮，关闭=灰底 */
@Composable
private fun TogglePill(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (active) Brush.horizontalGradient(listOf(Color(0xFF8A6A2F), Color(0xFFD9A441))) else null
    Box(modifier.clip(RoundedCornerShape(20.dp))
        .then(if (bg != null) Modifier.background(bg) else Modifier.background(Color(0xFFE0DDD5)))
        .border(1.dp, if (active) Color(0xFFB89142) else Color(0xFFBBBBBB), RoundedCornerShape(20.dp))
        .clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(text + if (active) " ✓" else "", color = if (active) Color(0xFF1A150C) else Color(0xFF666666),
            fontSize = 12.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

/** 点击盘面弹出的放大九宫格浮层：顶部四柱标题栏 + 放大的 3x3 详细盘 + 关闭按钮 */
@Composable
private fun QimenZoomDialog(
    r: Qimen.Result, siZhu: List<String>,
    showZhangsheng: Boolean = true, showDiShen: Boolean = true,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFBF8F0))
                .border(1.dp, Color(0xFFBBBBBB), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            // 顶部四柱标题栏：放大图标 + 四柱 + 关闭
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⤢", color = Color(0xFF888888), fontSize = 18.sp, modifier = Modifier.weight(0.5f))
                Row(Modifier.weight(4f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (p in siZhu) {
                        Text(p, color = Color(0xFF1A1A1A), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif, textAlign = TextAlign.Center)
                    }
                }
                Box(Modifier.weight(0.5f).size(26.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE5E5E5))
                    .clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Text("✕", color = Color(0xFF777777), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))

            // 放大的 3x3 九宫格
            val visual = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF888888), RoundedCornerShape(6.dp)).background(Color.White)
            ) {
                visual.forEachIndexed { rIdx, row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEachIndexed { cIdx, g ->
                            val cell = r.pan[g] ?: Qimen.Cell()
                            QimenZoomCellView(g, cell, Modifier.weight(1f), showZhangsheng, showDiShen)
                            if (cIdx < 2) Box(Modifier.width(1.dp).height(150.dp).background(Color(0xFFCCCCCC)))
                        }
                    }
                    if (rIdx < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCCCCCC)))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("点空白处关闭", color = Color(0xFFAAAAAA), fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

/** 放大格子（仿图1详盘，字号更大）：八神 / [隐干·九星 | 长生·天盘干] / [地盘八神·门 | 长生·地盘干] */
@Composable
private fun QimenZoomCellView(
    gong: String, c: Qimen.Cell, modifier: Modifier,
    showZS: Boolean = true, showDiShen: Boolean = true
) {
    Column(
        modifier.height(150.dp).padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // 八神全名 + 马星
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val bs = fullBaShen(c.bashen)
            if (bs.isNotEmpty()) Text(bs, color = Color(0xFF333333), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (c.ma) Text(if (bs.isNotEmpty()) " 马" else "马", color = Color(0xFFC0392B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        if (gong == "中") {
            Text(c.dipan, color = Color(0xFFAAAAAA), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        } else {
            // 天盘行
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (c.yingan.isNotEmpty()) Text(c.yingan, color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(fullJiuXing(c.jiuxing), color = Color(0xFF222222), fontSize = 14.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.tianpan12.isNotEmpty())
                        Text(c.tianpan12 + if (c.tianpanJi12.isNotEmpty()) " " + c.tianpanJi12 else "",
                            color = Color(0xFF2E6FA8), fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (c.tianpan.isNotEmpty())
                            Text(c.tianpan, color = ganColor(c.tianpan, c.tianpan_jixing, c.tianpan_rumu),
                                fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (c.tianpanJi.isNotEmpty()) Text(c.tianpanJi, color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    }
                }
            }
            // 地盘行
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (showDiShen && c.diShen.isNotEmpty()) Text(c.diShen + " ", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    val mn = fullMen(c.men)
                    if (mn.isNotEmpty())
                        Text(mn, color = if (c.men_po) Color(0xFFC9A227) else Color(0xFF222222), fontSize = 14.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.dipan12.isNotEmpty())
                        Text(c.dipan12 + if (c.dipanJi12.isNotEmpty()) " " + c.dipanJi12 else "",
                            color = Color(0xFF2E6FA8), fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (c.dipan.isNotEmpty())
                            Text(c.dipan, color = ganColor(c.dipan, c.dipan_jixing, c.dipan_rumu),
                                fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (c.dipanJi.isNotEmpty()) Text(c.dipanJi, color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/** 单个九宫格（仿图1详盘）：八神 / [隐干·九星 | 长生·天盘干] / [地盘八神·门 | 长生·地盘干] */
@Composable
private fun QimenCellView(
    gong: String, c: Qimen.Cell, modifier: Modifier,
    showZS: Boolean = true, showDiShen: Boolean = true,
    gray: Boolean = false
) {
    Column(
        modifier.height(76.dp).padding(horizontal = 3.dp, vertical = 2.dp)
            .then(if (gray) Modifier.background(Color(0x40B6B6B6)) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // 八神全名 + 马星（顶部居中）
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(fullBaShen(c.bashen), color = Color(0xFF333333), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (c.ma) Text(" 马", color = Color(0xFFC0392B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        if (gong == "中") {
            // 中宫：只显示地盘干（寄宫）
            Text(c.dipan, color = Color(0xFFAAAAAA), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
            // 天盘行：左=隐干·九星，右=长生(蓝小字)·天盘干
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (c.yingan.isNotEmpty()) Text(c.yingan, color = Color(0xFFAAAAAA), fontSize = 10.sp)
                    Spacer(Modifier.width(2.dp))
                    Text(fullJiuXing(c.jiuxing), color = Color(0xFF222222), fontSize = 11.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.tianpan12.isNotEmpty())
                        Text(c.tianpan12 + if (c.tianpanJi12.isNotEmpty()) " " + c.tianpanJi12 else "",
                            color = Color(0xFF2E6FA8), fontSize = 8.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.tianpan, color = ganColor(c.tianpan, c.tianpan_jixing, c.tianpan_rumu),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (c.tianpanJi.isNotEmpty()) Text(c.tianpanJi, color = Color(0xFF999999), fontSize = 9.sp)
                    }
                }
            }
            // 地盘行：左=地盘八神(灰)·门，右=长生(蓝小字)·地盘干
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (showDiShen && c.diShen.isNotEmpty()) Text(c.diShen + " ", color = Color(0xFFAAAAAA), fontSize = 10.sp)
                    Text(fullMen(c.men), color = if (c.men_po) Color(0xFFC9A227) else Color(0xFF222222), fontSize = 11.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.dipan12.isNotEmpty())
                        Text(c.dipan12 + if (c.dipanJi12.isNotEmpty()) " " + c.dipanJi12 else "",
                            color = Color(0xFF2E6FA8), fontSize = 8.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.dipan, color = ganColor(c.dipan, c.dipan_jixing, c.dipan_rumu),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (c.dipanJi.isNotEmpty()) Text(c.dipanJi, color = Color(0xFF999999), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

/** 简称→全名：八神（引擎存 符/蛇/阴/六/白/玄/地/天） */
private fun fullBaShen(s: String): String = when (s) {
    "符" -> "值符"; "蛇" -> "螣蛇"; "阴" -> "太阴"; "六" -> "六合"
    "白" -> "白虎"; "玄" -> "玄武"; "地" -> "九地"; "天" -> "九天"
    else -> s
}
/** 简称→全名：九星（引擎存 蓬/芮/冲/辅/禽/心/柱/任/英） */
private fun fullJiuXing(s: String): String = when (s) {
    "蓬" -> "天蓬"; "芮" -> "天芮"; "冲" -> "天冲"; "辅" -> "天辅"; "禽" -> "天禽"
    "心" -> "天心"; "柱" -> "天柱"; "任" -> "天任"; "英" -> "天英"
    else -> s
}
/** 简称→全名：八门（引擎存 休/生/伤/杜/景/死/惊/开） */
private fun fullMen(s: String): String = when (s) {
    "休" -> "休门"; "生" -> "生门"; "伤" -> "伤门"; "杜" -> "杜门"
    "景" -> "景门"; "死" -> "死门"; "惊" -> "惊门"; "开" -> "开门"
    else -> s
}

private fun ganColor(gan: String, jixing: Boolean, rumu: Boolean): Color = when {
    jixing && rumu -> Color(0xFF2A6DB0)
    jixing -> Color(0xFFB0392E)
    rumu -> Color(0xFF999999)
    else -> Color(0xFF222222)
}

private fun qimenFaceText(r: Qimen.Result, q: String, createdAt: String, jufa: String, juName: String): String = buildString {
    appendLine("奇 门 遁 甲")
    appendLine("事项：$q")
    appendLine("时间：$createdAt")
    appendLine("起局：$jufa · $juName")
    appendLine("四柱：${r.siZhu.joinToString(" ")}")
    appendLine("节气：${r.jieqi}")
    appendLine("旬首：${r.xunhead}  空亡：${r.kongwang}  值符：${r.zhifu}  值使：${r.zhishi}  马星：${r.maxing}")
    appendLine()
    val visual = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))
    for (row in visual) {
        appendLine(row.joinToString("  ") { g ->
            val c = r.pan[g]!!
            "[$g ${fullBaShen(c.bashen)}·${fullJiuXing(c.jiuxing)}${c.tianpan}${fullMen(c.men)}·${c.dipan}]"
        })
    }
}.trim()

// ==================== 本文件私有辅助组件（避免与 JishiyuScreen 的 private 函数跨文件冲突） ====================

@Composable
private fun QmNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(QM_CARD)
            .clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("‹ 返回", color = QM_GOLD, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(52.dp))
    }
}

@Composable
private fun QmInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.width(48.dp))
        Text(value, color = Color(0xFF222222), fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QmDateDialog(initial: java.time.LocalDate, onDismiss: () -> Unit, onConfirm: (java.time.LocalDate) -> Unit) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }
    fun dim(): Int = java.time.YearMonth.of(year, month).lengthOfMonth()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, QM_LINE, RoundedCornerShape(14.dp)).padding(16.dp)
        ) {
            Text("选择日期", color = QM_GOLD, fontSize = 17.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QmStepper("年", year, 1900, 2100, Modifier.weight(1.3f)) { year = it }
                QmStepper("月", month, 1, 12, Modifier.weight(0.9f)) { month = it; if (day > dim()) day = dim() }
                QmStepper("日", day, 1, dim(), Modifier.weight(0.9f)) { day = it }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x12FFFFFF))
                    .clickable { onDismiss() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x2AD9A441))
                    .border(1.dp, QM_LINE, RoundedCornerShape(10.dp))
                    .clickable { onConfirm(java.time.LocalDate.of(year, month, day)) }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center) {
                    Text("确 定", color = QM_GOLD, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
private fun QmTimeDialog(initial: java.time.LocalTime, onDismiss: () -> Unit, onConfirm: (java.time.LocalTime) -> Unit) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF242019), Color(0xFF16150F))))
                .border(1.dp, QM_LINE, RoundedCornerShape(14.dp)).padding(16.dp)
        ) {
            Text("选择时间", color = QM_GOLD, fontSize = 17.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QmStepper("时", hour, 0, 23, Modifier.weight(1f)) { hour = it }
                QmStepper("分", minute, 0, 59, Modifier.weight(1f)) { minute = it }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x12FFFFFF))
                    .clickable { onDismiss() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x2AD9A441))
                    .border(1.dp, QM_LINE, RoundedCornerShape(10.dp))
                    .clickable { onConfirm(java.time.LocalTime.of(hour, minute)) }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center) {
                    Text("确 定", color = QM_GOLD, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
private fun QmStepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x10FFFFFF)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AppColors.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text("＋", color = QM_GOLD, fontSize = 18.sp, modifier = Modifier.clickable { onChange((value + 1).coerceAtMost(max)) })
        Text("$value", color = AppColors.Paper, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        Text("－", color = QM_GOLD, fontSize = 18.sp, modifier = Modifier.clickable { onChange((value - 1).coerceAtLeast(min)) })
    }
}

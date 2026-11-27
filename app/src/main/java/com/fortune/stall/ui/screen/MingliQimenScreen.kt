package com.fortune.stall.ui.screen

import android.widget.Toast
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
import com.fortune.stall.engine.jishiyu.BaziChart
import com.fortune.stall.engine.jishiyu.MingliQimen
import com.fortune.stall.engine.jishiyu.Qimen
import com.fortune.stall.ui.theme.AppColors
import java.time.format.DateTimeFormatter

private val ML_RED = Color(0xFFB0392E)
private val ML_GOLD = Color(0xFFD9A441)
private val ML_LINE = Color(0x52D9A441)
private val ML_CARD = Color(0x14FFFFFF)

/**
 * 命理奇门专用起卦页 —— 依据 ji.js.cn 命理奇门(终身盘)。
 * 姓名 / 性别 / 出生时间 → 终身阴盘九宫 + 大运(10步) + 流年(10年) 详情页，自动存档。
 */
@Composable
fun MingliQimenCastScreen(db: FortuneDb, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<MingliQimen.Result?>(null) }
    if (result == null) {
        JiMingpanCastForm(title = "命 理 奇 门", onBack = onBack) { f ->
            try {
                val r = MingliQimen.paipan(f.name, f.isMan, f.dateTime)
                result = r
                if (!f.noSave) {
                    val q = "${f.name.ifBlank { "命主" }} · 命理奇门"
                    val createdAt = f.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    val id = db.insert(createdAt, q, "mingliqimen", if (f.isMan) "男命顺逆" else "女命顺逆",
                        "排盘中…", r.siZhu.joinToString(" "))
                    db.updateFace(id, mingliFaceText(r, q, createdAt))
                    Toast.makeText(ctx, "已存入存档 · 卦账 #$id", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "排盘失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    result?.let { MingliDetailScreen(result = it, onBack = { result = null }) }
}

/** 地支 → 九宫宫位（仿原版 GONG_12ZHI） */
private val ML_GONG_12ZHI = mapOf(
    "亥" to "乾", "子" to "坎", "丑" to "艮", "寅" to "艮", "卯" to "震",
    "辰" to "巽", "巳" to "巽", "午" to "离", "未" to "坤", "申" to "坤",
    "酉" to "兑", "戌" to "乾"
)

/** 按天干找天盘干所在的宫（仿原版 mlGongTips）；甲找值符宫 */
private fun mlGongByGan(pan: Map<String, Qimen.Cell>, gan: String): String? {
    for ((gong, c) in pan) {
        if (c.tianpan == gan || c.tianpanJi == gan) return gong
        if (gan == "甲" && c.bashen == "符") return gong
    }
    return null
}

/** 十神全称 → 简写（比/劫/食/伤/才/财/杀/官/枭/印） */
private fun mlShenShort(full: String) = when (full) {
    "比肩" -> "比"; "劫财" -> "劫"; "食神" -> "食"; "伤官" -> "伤"; "偏财" -> "才";
    "正财" -> "财"; "七杀" -> "杀"; "正官" -> "官"; "偏印" -> "枭"; "正印" -> "印"; else -> full
}

/** 十神简写配色（仿原版） */
private fun mlShenColor(s: String) = when (s) {
    "比", "劫" -> Color(0xFF2E6FA8)     // 蓝
    "食", "伤" -> Color(0xFFC08A2E)     // 黄棕
    "才", "财" -> Color(0xFF3E8E4F)     // 绿
    "杀", "官" -> Color(0xFFC0392B)     // 红
    "枭", "印" -> Color(0xFF2E6FA8)     // 蓝
    else -> Color(0xFF222222)
}

@Composable
private fun MingliDetailScreen(result: MingliQimen.Result, onBack: () -> Unit) {
    var showZS by remember { mutableStateOf(true) }
    var showDiShen by remember { mutableStateOf(true) }
    var selDaYun by remember { mutableStateOf(0) }   // 选中的大运索引
    var selLiuNian by remember { mutableStateOf(-1) } // 选中的流年索引(-1无)
    var hlGong by remember { mutableStateOf<String?>(null) } // 九宫盘高亮宫位
    val r = result.qimen
    val dayGan = result.siZhu.getOrNull(2)?.firstOrNull()?.toString() ?: ""

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF5F1E8))) {
        // 标题栏（紧凑）
        Box(Modifier.fillMaxWidth().background(ML_RED).padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("命理奇门", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(26.dp))
            }
        }

        // ===== 四柱（原版：顶部表头 + 每柱干+十神 / 支+十神，左"乾造/坤造"）=====
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(if (result.gender == "男") "乾造" else "坤造", color = Color(0xFF2E6FA8),
                    fontSize = 13.sp, modifier = Modifier.width(44.dp))
                for (h in listOf("年柱", "月柱", "日柱", "时柱")) Text(h, color = Color(0xFF2E6FA8),
                    fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${result.name}\n属${result.shengXiao}", color = QmBoardTheme.GRAY, fontSize = 10.sp,
                    modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                for (p in result.siZhu) {
                    val gan = p.getOrNull(0)?.toString() ?: ""
                    val zhi = p.getOrNull(1)?.toString() ?: ""
                    val ganShen = mlShenShort(BaziChart.shiShen(dayGan, gan))
                    val zhiShen = mlShenShort(BaziChart.shiShen(dayGan, zhi))
                    Column(Modifier.weight(1f).clickable { hlGong = mlGongByGan(r.pan, gan) },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(gan, color = Color(0xFF222222), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(ganShen, color = mlShenColor(ganShen), fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(zhi, color = Color(0xFF222222), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(zhiShen, color = mlShenColor(zhiShen), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ===== 九宫盘 + 外围月将建除圈（复用阴盘组件）=====
        YpBoardWithRing(r, r.yangDun, r.jushu, showZS, showDiShen, highlightGong = hlGong)
        Spacer(Modifier.height(2.dp))

        // ===== 符号颜色说明 =====
        YpLegend()
        Spacer(Modifier.height(3.dp))

        // ===== 起运行 =====
        Text("出生${result.startAge}年${if (result.shunXing) "顺行" else "逆行"}起运",
            color = Color(0xFF2E6FA8), fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(3.dp))

        // ===== 大运表（可点击）=====
        mlYunTable("大运", result.daYun, result.startYear, result.shunXing, dayGan, selDaYun, true) { i ->
            selDaYun = i
            selLiuNian = -1
            // 仿原版：点大运按天干找天盘宫高亮
            hlGong = mlGongByGan(r.pan, result.daYun[i].ganZhi.getOrNull(0)?.toString() ?: "")
        }
        Spacer(Modifier.height(3.dp))

        // ===== 流年表（可点击）=====
        mlLiuNianTable(result.liuNian, dayGan, selDaYun, selLiuNian) { i ->
            selLiuNian = i
            // 仿原版：点流年按地支查 GONG_12ZHI 找地盘宫高亮
            hlGong = ML_GONG_12ZHI[result.liuNian[i].ganZhi.getOrNull(1)?.toString()]
        }
        Spacer(Modifier.height(8.dp))

        // ===== 底部按钮 =====
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MlOutlineBtn("长生状态", showZS, Modifier.weight(1f)) { showZS = !showZS }
            MlOutlineBtn("天门地户", showDiShen, Modifier.weight(1f)) { showDiShen = !showDiShen }
            MlOutlineBtn("九宫全景", false, Modifier.weight(1f)) { showZS = true; showDiShen = true }
        }
        Spacer(Modifier.height(12.dp))
        com.fortune.stall.ui.components.ResultActionBar(
            faceText = mingliFaceText(result, "${result.name} · 命理奇门", "")
        )
    }
}

/** 大运表：标签列 + 10 个可点击大运格（年份/岁数/干+十神/支+十神） */
@Composable
private fun mlYunTable(label: String, steps: List<MingliQimen.YunStep>, startYear: Int, shunXing: Boolean,
                       dayGan: String, selected: Int, isDaYun: Boolean, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(4.dp)).border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
        .background(Color.White)) {
        Row(Modifier.fillMaxWidth()) {
            // 标签列
            Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                Text(label, color = Color(0xFF2E6FA8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                steps.forEachIndexed { i, s ->
                    val gz = s.ganZhi
                    val gan = gz.getOrNull(0)?.toString() ?: ""
                    val zhi = gz.getOrNull(1)?.toString() ?: ""
                    val ganShen = mlShenShort(BaziChart.shiShen(dayGan, gan))
                    val zhiShen = mlShenShort(BaziChart.shiShen(dayGan, zhi))
                    val year = startYear + i * 10
                    val age = s.startAge + i * 10
                    Column(Modifier.width(56.dp).clickable { onSelect(i) }
                        .background(if (selected == i) Color(0xFFE4E4E4) else Color.White)
                        .border(1.dp, if (selected == i) ML_RED else Color(0xFFEEEEEE))
                        .padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$year", color = Color(0xFF666666), fontSize = 11.sp)
                        Text("${age}岁", color = Color(0xFF888888), fontSize = 10.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(gan, color = Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(ganShen, color = mlShenColor(ganShen), fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(zhi, color = Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(zhiShen, color = mlShenColor(zhiShen), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/** 流年表：标签列 + 10 个可点击流年格（年份/干+十神/支+十神），随选中大运高亮该运起始段 */
@Composable
private fun mlLiuNianTable(steps: List<MingliQimen.YunStep>, dayGan: String, selDaYun: Int,
                           selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(4.dp)).border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
        .background(Color.White)) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                Text("流年", color = Color(0xFF2E6FA8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                steps.forEachIndexed { i, s ->
                    val gz = s.ganZhi
                    val gan = gz.getOrNull(0)?.toString() ?: ""
                    val zhi = gz.getOrNull(1)?.toString() ?: ""
                    val ganShen = mlShenShort(BaziChart.shiShen(dayGan, gan))
                    val zhiShen = mlShenShort(BaziChart.shiShen(dayGan, zhi))
                    val yearMatch = s.years.trimEnd('年').toIntOrNull()
                    val inYun = yearMatch != null && ((yearMatch - (s.startAge)) / 10) == selDaYun
                    Column(Modifier.width(52.dp).clickable { onSelect(i) }
                        .background(if (selected == i) Color(0xFFE4E4E4) else if (inYun) Color(0xFFF7F1E4) else Color.White)
                        .border(1.dp, if (selected == i) ML_RED else Color(0xFFEEEEEE))
                        .padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.years, color = Color(0xFF666666), fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(gan, color = Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(ganShen, color = mlShenColor(ganShen), fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(zhi, color = Color(0xFF222222), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(zhiShen, color = mlShenColor(zhiShen), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun MlOutlineBtn(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
        .border(1.dp, ML_RED, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = ML_RED, fontSize = 12.5.sp)
    }
}

@Composable private fun MlToggle(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(if (active) ML_RED else Color(0x18FFFFFF))
        .border(1.dp, ML_LINE, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (active) Color.White else QmBoardTheme.GRAY, fontSize = 13.sp)
    }
}

@Composable private fun MlTab(text: String, idx: Int, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(if (selected == idx) ML_RED else Color(0x18FFFFFF))
        .border(1.dp, ML_LINE, RoundedCornerShape(18.dp)).clickable { onSelect(idx) }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (selected == idx) Color.White else QmBoardTheme.GRAY, fontSize = 13.sp)
    }
}

@Composable private fun YunGrid(steps: List<MingliQimen.YunStep>, selected: Int, onSelect: (Int) -> Unit) {
    val hs = rememberScrollState()
    Row(Modifier.fillMaxWidth().horizontalScroll(hs).padding(horizontal = 12.dp)) {
        steps.forEachIndexed { i, s ->
            Column(Modifier.width(88.dp).padding(end = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected == i) Color(0x30B0392E) else Color.White)
                .border(1.dp, if (selected == i) ML_RED else Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                .clickable { onSelect(i) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.label, color = QmBoardTheme.GRAY, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(s.ganZhi, color = ML_RED, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(4.dp))
                Text("${s.startAge}岁", color = Color(0xFF333333), fontSize = 11.sp)
                Text(s.years, color = QmBoardTheme.GRAY, fontSize = 10.sp)
            }
        }
    }
}

@Composable private fun MlNavBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(ML_CARD).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("‹ 返回", color = ML_GOLD, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f)); Spacer(Modifier.width(52.dp))
    }
}

@Composable private fun MlRadioLine(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label：", color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(84.dp))
        options.forEachIndexed { idx, name ->
            Row(Modifier.clickable { onSelect(idx) }.padding(end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(17.dp).clip(RoundedCornerShape(50))
                    .background(if (selected == idx) ML_RED else Color.Transparent)
                    .then(if (selected == idx) Modifier else Modifier.border(1.dp, ML_LINE, RoundedCornerShape(50))),
                    contentAlignment = Alignment.Center) {
                    if (selected == idx) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
                }
                Spacer(Modifier.width(5.dp))
                Text(name, color = if (selected == idx) ML_GOLD else AppColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

private fun mingliFaceText(r: MingliQimen.Result, q: String, createdAt: String) = buildString {
    appendLine("命 理 奇 门（终身盘）"); appendLine("命主：$q"); appendLine("时间：$createdAt")
    appendLine("四柱：${r.siZhu.joinToString(" ")}")
    appendLine("局式：${if (r.qimen.yangDun) "阳遁" else "阴遁"}${r.qimen.jushu}局 · ${r.startAge}岁起运 · ${if (r.shunXing) "顺行" else "逆行"}")
    appendLine("大运：${r.daYun.joinToString(" ") { it.ganZhi }}")
    appendLine("流年：${r.liuNian.joinToString(" ") { it.ganZhi }}")
}.trim()

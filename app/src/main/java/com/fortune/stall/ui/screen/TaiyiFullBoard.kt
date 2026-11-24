package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.jishiyu.DaLiuRen
import com.fortune.stall.engine.jishiyu.Qimen
import com.fortune.stall.engine.jishiyu.Taiyi

/**
 * 原版「太乙三式」完整排盘（1:1 仿 qimen3shi_tpl.html + qimen.css）。
 *
 * 整体为 540×540 的"回"字形四层结构（zoom 0.62 适配手机）：
 *   ① 最外圈：米黄底「活神十神标签」（太乙/主大/文昌… + 比劫枭印…）
 *   ② 次外圈：灰底「太乙十六神」（大灵九巽、大神九巳…）固定名，边角错位排列
 *   ③ 内圈：大六壬标注（天干/贵神(橙)/月将 + 年煞(褐)/命宫(蓝)/建除(绿)）
 *   ④ 中心：奇门九宫格
 */
internal object TyTheme {
    val BG = Color(0xFFEFEFEF)
    val BORDER = Color(0xFFD3D3D3)
    val GOD_BG = Color(0xFFE3E3E3)
    val GOD_BORDER = Color(0xFFAAAAAA)
    val ACTIVE_BG = Color(0xFFF7EECD)
    val TAIYI_BG = Color(0xFFDC143C)
    val GUI = Color(0xFFFF7700)
    val NIAN = Color(0xFF801300)
    val GONG = Color(0xFF0000FF)
    val JC = Color(0xFF008000)
    val DARK = Color(0xFF222222)
    val GRAY = Color(0xFF888888)
    val RED = Color(0xFFC0392B)
}

private val TY_DZ = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
private val TY_JC = listOf("建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭")
private fun tyJianchu(branch: String, yueJiang: String): String {
    val yj = TY_DZ.indexOf(yueJiang).coerceAtLeast(0)
    val b = TY_DZ.indexOf(branch).coerceAtLeast(0)
    return TY_JC[(b - yj + 12) % 12]
}

/** 米黄底活神标签：解析 "太乙比1主参财1" 为若干 4 字令牌，太乙两字红底 */
@Composable
private fun TyActiveLabel(text: String, modifier: Modifier) {
    val tokens = text.chunked(4).filter { it.isNotBlank() }
    Box(
        modifier.clip(RoundedCornerShape(3.dp)).background(TyTheme.ACTIVE_BG)
            .border(1.dp, Color.Gray, RoundedCornerShape(3.dp))
            .padding(horizontal = 2.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (tok in tokens) {
                Row {
                    if (tok.startsWith("太乙")) {
                        Text("太乙", color = Color(0xFFFFF8DC), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif, modifier = Modifier.background(TyTheme.TAIYI_BG))
                        Text(tok.drop(2), color = TyTheme.DARK, fontSize = 9.sp, fontFamily = FontFamily.Serif)
                    } else {
                        Text(tok, color = TyTheme.DARK, fontSize = 9.sp, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
    }
}

/** 灰底太乙十六神格（固定名），窄边竖排或横排 */
@Composable
private fun TyGodCell(name: String, modifier: Modifier, vertical: Boolean = false) {
    Box(
        modifier.background(TyTheme.GOD_BG).border(1.dp, TyTheme.GOD_BORDER),
        contentAlignment = Alignment.Center
    ) {
        if (vertical) {
            // 每两字一行竖排：太阳/四辰
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name.take(2), color = TyTheme.DARK, fontSize = 9.sp, fontFamily = FontFamily.Serif, lineHeight = 11.sp)
                Text(name.drop(2), color = TyTheme.DARK, fontSize = 9.sp, fontFamily = FontFamily.Serif, lineHeight = 11.sp)
            }
        } else {
            Text(name, color = TyTheme.DARK, fontSize = 9.sp, fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center, lineHeight = 11.sp)
        }
    }
}

/** 大六壬标注：天干/贵神/月将 + 年煞/命宫/建除（上下两行或左右两行） */
@Composable
private fun TyRenMark(lr: DaLiuRen.Result, branch: String, yueJiang: String, horizontalFirst: Boolean) {
    val gan = lr.tianGan[branch] ?: ""
    val gui = lr.guiShen[branch] ?: ""
    val yj = lr.tianPan[branch] ?: lr.yueJiang
    val ns = lr.nianSha[branch] ?: ""
    val gong = lr.gong12[branch] ?: ""
    val jc = tyJianchu(branch, yueJiang)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(gan, color = TyTheme.GRAY, fontSize = 8.sp, fontFamily = FontFamily.Serif)
            Text(gui, color = TyTheme.GUI, fontSize = 8.sp, fontFamily = FontFamily.Serif)
            Text(yj, color = TyTheme.DARK, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
        Row {
            Text(ns, color = TyTheme.NIAN, fontSize = 8.sp, fontFamily = FontFamily.Serif)
            Text(gong, color = TyTheme.GONG, fontSize = 8.sp, fontFamily = FontFamily.Serif)
            Text(jc, color = TyTheme.JC, fontSize = 8.sp, fontFamily = FontFamily.Serif)
        }
    }
}

/** 九宫单格（2列×3行，仿 Q6Cell） */
@Composable
private fun TyNineCell(c: Qimen.Cell, modifier: Modifier) {
    val bs = c.bashen + if (c.kongwang) "〇" else ""
    Column(modifier.padding(horizontal = 2.dp, vertical = 1.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(bs, color = TyTheme.DARK, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(c.yingan + c.yinganJi, color = TyTheme.GRAY, fontSize = 7.sp, maxLines = 1,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Row(Modifier.fillMaxWidth()) {
            val tp = if (c.tianpan_jixing || c.tianpan_rumu) TyTheme.RED else TyTheme.DARK
            Text(c.tianpan + c.tianpanJi, color = tp, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, maxLines = 1)
            Text(c.jiuxing, color = TyTheme.DARK, fontSize = 8.sp, maxLines = 1,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Row(Modifier.fillMaxWidth()) {
            val dp = if (c.dipan_jixing || c.dipan_rumu) TyTheme.RED else TyTheme.DARK
            Text(c.dipan + c.dipanJi, color = dp, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, maxLines = 1)
            Text(c.men, color = if (c.men_po) TyTheme.RED else TyTheme.DARK, fontSize = 8.sp, maxLines = 1,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

/**
 * 太乙三式完整盘。gods/active 各 16 项，顺时针自左上角起：
 * 0巽 1巳 2午 3未 4坤 5申 6酉 7戌 8乾 9亥 10子 11丑 12艮 13寅 14卯 15辰
 */
@Composable
internal fun TaiyiFullBoard(q: Qimen.Result, lr: DaLiuRen.Result, ty: Taiyi.Result) {
    val gods = ty.shen16.gods
    val act = ty.shen16.active
    val yj = lr.yueJiang
    // 原版 540×540 的 0.65 倍 ≈ 352dp，适配手机宽度
    val activeW = 46.dp  // 活神标签带宽(原70)
    val godW = 26.dp     // 十六神带宽(原40)
    val renW = 26.dp     // 六壬标注带宽(原20+20)
    val corner = activeW + godW + renW  // 角区=98dp(原150)
    val edge = 156.dp    // 中间九宫边长(原240)

    Box(Modifier.wrapContentSize().clip(RoundedCornerShape(6.dp)).background(TyTheme.BG)
        .border(1.dp, TyTheme.BORDER, RoundedCornerShape(6.dp)).padding(2.dp)) {
        Column {
            // ===== 上边：左角 + 上沿 + 右角 =====
            Row {
                // 左上角：大灵九巽 + 活神
                Box(Modifier.size(corner)) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(activeW), Alignment.Center) { TyActiveLabel(act[0], Modifier.fillMaxWidth(0.95f)) }
                        TyGodCell(gods[0], Modifier.fillMaxWidth().height(godW))
                    }
                }
                // 上沿：巳午未（活神 / 十六神 / 六壬）
                Column(Modifier.width(edge)) {
                    Row(Modifier.fillMaxWidth().height(activeW)) {
                        for (i in 1..3) Box(Modifier.weight(1f)) { TyActiveLabel(act[i], Modifier.fillMaxWidth(0.96f)) }
                    }
                    Row(Modifier.fillMaxWidth().height(godW)) {
                        for (i in 1..3) TyGodCell(gods[i], Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().height(renW)) {
                        for (b in listOf("巳", "午", "未")) Box(Modifier.weight(1f)) { TyRenMark(lr, b, yj, true) }
                    }
                }
                // 右上角：大武七坤 + 活神
                Box(Modifier.size(corner)) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(activeW), Alignment.Center) { TyActiveLabel(act[4], Modifier.fillMaxWidth(0.95f)) }
                        TyGodCell(gods[4], Modifier.fillMaxWidth().height(godW))
                    }
                }
            }
            // ===== 中间：左边 + 九宫 + 右边 =====
            Row {
                // 左边：辰卯寅（活神|十六神|六壬）
                Column(Modifier.width(corner)) {
                    val idx = listOf(15, 14, 13); val br = listOf("辰", "卯", "寅")
                    for (k in 0..2) Row(Modifier.fillMaxWidth().weight(1f)) {
                        Box(Modifier.width(activeW)) { TyActiveLabel(act[idx[k]], Modifier.fillMaxWidth(0.92f)) }
                        Box(Modifier.width(godW)) { TyGodCell(gods[idx[k]], Modifier.fillMaxWidth(), vertical = true) }
                        Box(Modifier.weight(1f)) { TyRenMark(lr, br[k], yj, false) }
                    }
                }
                // 中心九宫
                Column(Modifier.size(edge).background(Color.White).border(1.dp, Color(0xFF888888))) {
                    for (ri in 0..2) {
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            for (ci in 0..2) {
                                val g = QM_VISUAL[ri][ci]
                                Box(Modifier.weight(1f)) {
                                    if (g == "中") Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text("中", color = Color(0xFFBBBBBB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else TyNineCell(q.pan[g] ?: Qimen.Cell(), Modifier.fillMaxWidth())
                                    if (ci < 2) Box(Modifier.width(1.dp).fillMaxHeight()
                                        .background(Color(0xFFAAAAAA)).align(Alignment.CenterEnd))
                                }
                            }
                        }
                        if (ri < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFAAAAAA)))
                    }
                }
                // 右边：申酉戌（六壬|十六神|活神）
                Column(Modifier.width(corner)) {
                    val idx = listOf(5, 6, 7); val br = listOf("申", "酉", "戌")
                    for (k in 0..2) Row(Modifier.fillMaxWidth().weight(1f)) {
                        Box(Modifier.weight(1f)) { TyRenMark(lr, br[k], yj, false) }
                        Box(Modifier.width(godW)) { TyGodCell(gods[idx[k]], Modifier.fillMaxWidth(), vertical = true) }
                        Box(Modifier.width(activeW)) { TyActiveLabel(act[idx[k]], Modifier.fillMaxWidth(0.92f)) }
                    }
                }
            }
            // ===== 下边：左角 + 下沿 + 右角 =====
            Row {
                Box(Modifier.size(corner)) {
                    Column(Modifier.fillMaxSize()) {
                        TyGodCell(gods[12], Modifier.fillMaxWidth().height(godW))
                        Box(Modifier.fillMaxWidth().height(activeW), Alignment.Center) { TyActiveLabel(act[12], Modifier.fillMaxWidth(0.95f)) }
                    }
                }
                Column(Modifier.width(edge)) {
                    Row(Modifier.fillMaxWidth().height(renW)) {
                        for (b in listOf("丑", "子", "亥")) Box(Modifier.weight(1f)) { TyRenMark(lr, b, yj, true) }
                    }
                    Row(Modifier.fillMaxWidth().height(godW)) {
                        for (i in listOf(11, 10, 9)) TyGodCell(gods[i], Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().height(activeW)) {
                        for (i in listOf(11, 10, 9)) Box(Modifier.weight(1f)) { TyActiveLabel(act[i], Modifier.fillMaxWidth(0.96f)) }
                    }
                }
                Box(Modifier.size(corner)) {
                    Column(Modifier.fillMaxSize()) {
                        TyGodCell(gods[8], Modifier.fillMaxWidth().height(godW))
                        Box(Modifier.fillMaxWidth().height(activeW), Alignment.Center) { TyActiveLabel(act[8], Modifier.fillMaxWidth(0.95f)) }
                    }
                }
            }
        }
    }
}

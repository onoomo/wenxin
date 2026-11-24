package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

/**
 * 奇门穿壬 九宫盘（仿原版 qimen6ren_tpl.html）。
 * 九宫每格 2 列 x 3 行：
 *   行1 左=八神+空亡〇  右=隐干(灰)
 *   行2 左=天盘干+寄宫  右=九星
 *   行3 左=地盘干+寄宫  右=八门
 * 外围一圈为六壬标注：每方位含「天干/贵神(橙)/月将」+「年煞(褐)/宫(蓝)/建除(绿)」。
 */

// 地支 → 天干/贵神/月将/年煞/宫/建除 由 DaLiuRen.Result 的 Map 提供
private val Q6_DIZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
private val Q6_JIANCHU = listOf("建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭")
private val Q6_YUEJIANG_NAME = mapOf(
    "亥" to "登明", "子" to "神后", "丑" to "大吉", "寅" to "功曹", "卯" to "太冲", "辰" to "天罡",
    "巳" to "太乙", "午" to "胜光", "未" to "小吉", "申" to "传送", "酉" to "从魁", "戌" to "河魁"
)

private fun q6Jianchu(branch: String, yueJiang: String): String {
    val yj = Q6_DIZHI.indexOf(yueJiang).coerceAtLeast(0)
    val b = Q6_DIZHI.indexOf(branch).coerceAtLeast(0)
    return Q6_JIANCHU[(b - yj + 12) % 12]
}

private val C_GUI = Color(0xFFFF7700)      // 贵神 橙
private val C_NIANSHA = Color(0xFF801300)  // 年煞 褐红
private val C_GONG = Color(0xFF0000FF)     // 宫 蓝
private val C_JC = Color(0xFF008000)       // 建除 绿
private val C_GRAY = Color(0xFF999999)
private val C_RED = Color(0xFFC0392B)
private val C_BLACK = Color(0xFF222222)

/** 外围一个方位标注：上排 天干·贵神·月将 ，下排 年煞·宫·建除 */
@Composable
private fun Q6RingCell(lr: DaLiuRen.Result, branch: String, yueJiang: String, topFirst: Boolean,
                       align: Alignment.Horizontal = Alignment.CenterHorizontally, w: androidx.compose.ui.unit.Dp = 78.dp) {
    val gan = lr.tianGan[branch] ?: ""
    val gui = lr.guiShen[branch] ?: ""
    val yj = lr.tianPan[branch] ?: lr.yueJiang
    val ns = lr.nianSha[branch] ?: ""
    val gong = lr.gong12[branch] ?: ""
    val jc = q6Jianchu(branch, yueJiang)
    Column(Modifier.width(w), horizontalAlignment = align) {
        if (topFirst) {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text(gan, color = C_BLACK, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                Text(gui, color = C_GUI, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                Text(yj, color = C_BLACK, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            }
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text(ns, color = C_NIANSHA, fontSize = 10.sp, fontFamily = FontFamily.Serif)
                Text(gong, color = C_GONG, fontSize = 10.sp, fontFamily = FontFamily.Serif)
                Text(jc, color = C_JC, fontSize = 10.sp, fontFamily = FontFamily.Serif)
            }
        } else {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text(ns, color = C_NIANSHA, fontSize = 10.sp, fontFamily = FontFamily.Serif)
                Text(gong, color = C_GONG, fontSize = 10.sp, fontFamily = FontFamily.Serif)
                Text(jc, color = C_JC, fontSize = 10.sp, fontFamily = FontFamily.Serif)
            }
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text(yj, color = C_BLACK, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(gui, color = C_GUI, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                Text(gan, color = C_BLACK, fontSize = 11.sp, fontFamily = FontFamily.Serif)
            }
        }
    }
}

/** 单个九宫格（2列 x 3行） */
@Composable
private fun Q6Cell(gong: String, c: Qimen.Cell, modifier: Modifier, h: androidx.compose.ui.unit.Dp) {
    val bs = c.bashen + if (c.kongwang) "〇" else ""
    Column(modifier.height(h).padding(horizontal = 4.dp, vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(bs, color = C_BLACK, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(c.yingan + c.yinganJi, color = C_GRAY, fontSize = 11.sp, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            val tpColor = when {
                c.tianpan_jixing || c.tianpan_rumu -> C_RED else -> C_BLACK
            }
            Text(c.tianpan + c.tianpanJi, color = tpColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, maxLines = 1)
            Text(c.jiuxing, color = C_BLACK, fontSize = 12.sp, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            val dpColor = when {
                c.dipan_jixing || c.dipan_rumu -> C_RED else -> C_BLACK
            }
            Text(c.dipan + c.dipanJi, color = dpColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, maxLines = 1)
            Text(c.men, color = if (c.men_po) C_RED else C_BLACK, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/**
 * 穿壬/太乙 九宫盘 + 外围六壬标注。
 * @param extraRing 太乙时在九宫外围再显示16神（本函数用占位，太乙另做）
 */
@Composable
internal fun Q6Board(q: Qimen.Result, lr: DaLiuRen.Result, showMa: Boolean = true) {
    val yj = lr.yueJiang
    val cellH = 82.dp
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 顶行：巳 午 未（上三行 + 下年煞宫建除）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
            for (b in listOf("巳", "午", "未")) Q6RingCell(lr, b, yj, topFirst = true)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 左列：辰 卯 寅
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
                for (b in listOf("辰", "卯", "寅")) Q6RingCell(lr, b, yj, topFirst = true, w = 76.dp)
            }
            // 九宫格
            Column(Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).border(1.dp, C_BLACK, RoundedCornerShape(3.dp)).background(Color.White)) {
                QM_VISUAL.forEachIndexed { ri, row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEachIndexed { ci, g ->
                            Q6Cell(g, q.pan[g] ?: Qimen.Cell(), Modifier.weight(1f), cellH)
                            if (ci < 2) Box(Modifier.width(1.dp).height(cellH).background(Color(0xFF888888)))
                        }
                    }
                    if (ri < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF888888)))
                }
            }
            // 右列：申 酉 戌
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
                for (b in listOf("申", "酉", "戌")) Q6RingCell(lr, b, yj, topFirst = true, w = 76.dp)
            }
        }
        // 底行：丑 子 亥（上年煞宫建除 + 下三行）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
            for (b in listOf("丑", "子", "亥")) Q6RingCell(lr, b, yj, topFirst = false)
        }
    }
}

package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.jishiyu.Qimen

/** 阴盘奇门/命理奇门共用的九宫格渲染（浅底仿原版） */
internal object QmBoardTheme {
    val RED = Color(0xFFB0392E)
    val GOLD = Color(0xFFD9A441)
    val LINE = Color(0x52D9A441)
    val TEXT = Color(0xFF222222)
    val GRAY = Color(0xFF888888)
    val BLUE = Color(0xFF2E6FA8)
}

internal val QM_VISUAL = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))

@Composable
internal fun QmBoardCell(gong: String, c: Qimen.Cell, modifier: Modifier, showZS: Boolean = true, showDiShen: Boolean = true) {
    Column(modifier.height(150.dp).padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(qmFullBaShen(c.bashen), color = QmBoardTheme.TEXT, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (c.ma) Text(" 马", color = Color(0xFFC0392B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (gong == "中") {
            Text(c.dipan, color = Color(0xFFAAAAAA), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (c.yingan.isNotEmpty()) Text(c.yingan, color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    Spacer(Modifier.width(2.dp))
                    Text(qmFullJiuXing(c.jiuxing), color = QmBoardTheme.TEXT, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.tianpan12.isNotEmpty())
                        Text(c.tianpan12 + if (c.tianpanJi12.isNotEmpty()) " " + c.tianpanJi12 else "",
                            color = QmBoardTheme.BLUE, fontSize = 9.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.tianpan, color = qmGanColor(c.tianpan, c.tianpan_jixing, c.tianpan_rumu),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (c.tianpanJi.isNotEmpty()) Text(c.tianpanJi, color = Color(0xFF999999), fontSize = 10.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (showDiShen && c.diShen.isNotEmpty()) Text(c.diShen + " ", color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    Text(qmFullMen(c.men), color = if (c.men_po) Color(0xFFC9A227) else QmBoardTheme.TEXT, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (showZS && c.dipan12.isNotEmpty())
                        Text(c.dipan12 + if (c.dipanJi12.isNotEmpty()) " " + c.dipanJi12 else "",
                            color = QmBoardTheme.BLUE, fontSize = 9.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.dipan, color = qmGanColor(c.dipan, c.dipan_jixing, c.dipan_rumu),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (c.dipanJi.isNotEmpty()) Text(c.dipanJi, color = Color(0xFF999999), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun QmBoardGrid(r: Qimen.Result, showZS: Boolean = true, showDiShen: Boolean = true) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF666666), RoundedCornerShape(6.dp))
        .background(Color.White)) {
        QM_VISUAL.forEachIndexed { rIdx, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { cIdx, g ->
                    QmBoardCell(g, r.pan[g] ?: Qimen.Cell(), Modifier.weight(1f), showZS, showDiShen)
                    if (cIdx < 2) Box(Modifier.width(1.dp).height(150.dp).background(Color(0xFFBBBBBB)))
                }
            }
            if (rIdx < 2) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFBBBBBB)))
        }
    }
}

@Composable
internal fun QmInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = QmBoardTheme.GRAY, fontSize = 13.sp, modifier = Modifier.width(50.dp))
        Text(value, color = QmBoardTheme.TEXT, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun QmRedButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(QmBoardTheme.RED)
        .clickable(onClick = onClick).padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

internal fun qmFullBaShen(s: String) = when (s) {
    "符" -> "值符"; "蛇" -> "螣蛇"; "阴" -> "太阴"; "六" -> "六合"
    "白" -> "白虎"; "玄" -> "玄武"; "地" -> "九地"; "天" -> "九天"; else -> s
}
internal fun qmFullJiuXing(s: String) = when (s) {
    "蓬" -> "天蓬"; "芮" -> "天芮"; "冲" -> "天冲"; "辅" -> "天辅"; "禽" -> "天禽"
    "心" -> "天心"; "柱" -> "天柱"; "任" -> "天任"; "英" -> "天英"; else -> s
}
internal fun qmFullMen(s: String) = when (s) {
    "休" -> "休门"; "生" -> "生门"; "伤" -> "伤门"; "杜" -> "杜门"
    "景" -> "景门"; "死" -> "死门"; "惊" -> "惊门"; "开" -> "开门"; else -> s
}
internal fun qmGanColor(gan: String, jixing: Boolean, rumu: Boolean) = when {
    jixing && rumu -> Color(0xFF2A6DB0)
    jixing -> Color(0xFFB0392E)
    rumu -> Color(0xFF999999)
    else -> Color(0xFF222222)
}

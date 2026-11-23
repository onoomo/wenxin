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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fortune.stall.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 三式模块共享的深色主题色 */
internal object BoardTheme {
    val RED = Color(0xFFB0392E)
    val GOLD = Color(0xFFD9A441)
    val LINE = Color(0x52D9A441)
    val CARD = Color(0x14FFFFFF)
    val YEAR = Color(0xFF27843B)
    val MONTH = Color(0xFFC0392B)
    val DAY = Color(0xFFC98A12)
    val HOUR = Color(0xFF7D3C98)
    val GUI = Color(0xFFFF7700)   // 贵神
    val NIAN = Color(0xFF801300)  // 年煞
    val MING = Color(0xFF2A56D0)  // 命宫
    val JIAN = Color(0xFF27843B)  // 建星
}

@Composable
internal fun BoardStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.Paper, fontSize = 14.sp, modifier = Modifier.width(44.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(BoardTheme.CARD)
            .clickable { onChange(kotlin.math.max(min, value - 1)) }.padding(12.dp)) {
            Text("−", color = BoardTheme.GOLD, fontSize = 16.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text("$value", color = Color.White, fontSize = 18.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(BoardTheme.CARD)
            .clickable { onChange(kotlin.math.min(max, value + 1)) }.padding(12.dp)) {
            Text("＋", color = BoardTheme.GOLD, fontSize = 16.sp)
        }
    }
}

@Composable
internal fun BoardDateDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var y by remember { mutableStateOf(initial.year) }
    var m by remember { mutableStateOf(initial.monthValue) }
    var d by remember { mutableStateOf(initial.dayOfMonth) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, BoardTheme.LINE, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择日期", color = BoardTheme.GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            BoardStepper("年", y, 1800, 2199) { y = it }
            BoardStepper("月", m, 1, 12) { m = it }
            BoardStepper("日", d, 1, 31) { d = it }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.CARD)
                    .clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.RED).clickable {
                    val dim = java.time.YearMonth.of(y, m).lengthOfMonth()
                    onConfirm(LocalDate.of(y, m, kotlin.math.min(d, dim)))
                }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * 一次选完「年/月/日/时/分」的合并弹窗，样式与 BoardDateDialog 一致。
 * 八字排盘需要精确到出生「几时几分」，避免先选日期再选时间两步。
 */
@Composable
internal fun BoardDateTimeDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    var y by remember { mutableStateOf(initial.year) }
    var m by remember { mutableStateOf(initial.monthValue) }
    var d by remember { mutableStateOf(initial.dayOfMonth) }
    var h by remember { mutableStateOf(initial.hour) }
    var mi by remember { mutableStateOf(initial.minute) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, BoardTheme.LINE, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择出生时间", color = BoardTheme.GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            BoardStepper("年", y, 1800, 2199) { y = it }
            BoardStepper("月", m, 1, 12) { m = it }
            BoardStepper("日", d, 1, 31) { d = it }
            BoardStepper("时", h, 0, 23) { h = it }
            BoardStepper("分", mi, 0, 59) { mi = it }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.CARD)
                    .clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.RED).clickable {
                    val dim = java.time.YearMonth.of(y, m).lengthOfMonth()
                    onConfirm(LocalDateTime.of(y, m, kotlin.math.min(d, dim), h, mi))
                }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
internal fun BoardTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var h by remember { mutableStateOf(initial.hour) }
    var mi by remember { mutableStateOf(initial.minute) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B14))
            .border(1.dp, BoardTheme.LINE, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择时间", color = BoardTheme.GOLD, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            BoardStepper("时", h, 0, 23) { h = it }
            BoardStepper("分", mi, 0, 59) { mi = it }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.CARD)
                    .clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BoardTheme.RED)
                    .clickable { onConfirm(LocalTime.of(h, mi)) }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
internal fun BoardSectionTitle(title: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(15.dp).background(BoardTheme.GOLD))
        Spacer(Modifier.width(7.dp))
        Text(title, color = BoardTheme.GOLD, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

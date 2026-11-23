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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDateTime

/** 紫微起卦弹窗的深金国风配色 */
internal object ZwFormTheme {
    val BG = Color(0xFF0E121C)          // 深蓝黑底
    val PANEL = Color(0xFF171C2A)       // 面板略亮
    val GOLD = Color(0xFFD4A843)        // 墨金
    val TEXT = Color(0xFFE8E4DA)        // 主文字
    val SUB = Color(0xFF9A9BA6)         // 次文字
    val RED = Color(0xFFB23A2E)         // 朱红按钮
    val BTN = Color(0x18D4A843)         // +/- 圆形按钮底
    val LINE = Color(0x40D4A843)
}

/**
 * 深金国风「选择出生时间」弹窗：年/月/日/时/分 五行。
 * 每行两侧 -/+ 步进；点击中间数字可弹出键盘直接输入，输入即夹紧写回，点“确定”取最终值。
 */
@Composable
internal fun ZwDateTimeDialog(
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
        Column(
            Modifier.clip(RoundedCornerShape(18.dp))
                .background(ZwFormTheme.BG)
                .border(1.dp, ZwFormTheme.LINE, RoundedCornerShape(18.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("选择出生时间", color = ZwFormTheme.GOLD, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("点击数字可键盘输入，或用 − / + 调整", color = ZwFormTheme.SUB, fontSize = 10.sp)
            Spacer(Modifier.height(10.dp))
            ZwStepper("年", y, 1900, 2100) { y = it }
            ZwStepper("月", m, 1, 12) { m = it }
            ZwStepper("日", d, 1, 31) { d = it }
            ZwStepper("时", h, 0, 23) { h = it }
            ZwStepper("分", mi, 0, 59) { mi = it }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZwFormTheme.BTN)
                    .clickable(onClick = onDismiss).padding(horizontal = 26.dp, vertical = 10.dp)) {
                    Text("取消", color = ZwFormTheme.SUB, fontSize = 14.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZwFormTheme.RED).clickable {
                    val dim = java.time.YearMonth.of(y, m).lengthOfMonth()
                    onConfirm(LocalDateTime.of(y, m, kotlin.math.min(d, dim), h, mi))
                }.padding(horizontal = 26.dp, vertical = 10.dp)) {
                    Text("确定", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * 单行步进器：− [数字/输入框] +。
 * value 始终反映最新值；点击中间数字进入输入模式，逐字把合法数字夹紧后写回 value。
 */
@Composable
private fun ZwStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var buf by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZwFormTheme.TEXT, fontSize = 15.sp, modifier = Modifier.width(34.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(ZwFormTheme.BTN)
            .clickable {
                editing = false
                onChange(maxOf(min, value - 1))
            }.padding(12.dp)) {
            Text("−", color = ZwFormTheme.GOLD, fontSize = 18.sp)
        }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(72.dp).height(38.dp).clip(RoundedCornerShape(8.dp))
            .background(ZwFormTheme.PANEL).border(1.dp, ZwFormTheme.LINE, RoundedCornerShape(8.dp))
            .clickable {
                if (!editing) { buf = ""; editing = true }
            }, contentAlignment = Alignment.Center) {
            if (editing) {
                BasicTextField(
                    value = buf,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        buf = digits
                        digits.toIntOrNull()?.let { onChange(it.coerceIn(min, max)) }
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                )
            } else {
                Text("$value", color = Color.White, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.clip(RoundedCornerShape(50)).background(ZwFormTheme.BTN)
            .clickable {
                editing = false
                onChange(minOf(max, value + 1))
            }.padding(12.dp)) {
            Text("+", color = ZwFormTheme.GOLD, fontSize = 18.sp)
        }
    }
}

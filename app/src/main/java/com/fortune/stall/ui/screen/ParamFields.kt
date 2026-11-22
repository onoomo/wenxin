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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.ui.theme.AppColors

private val ZHI = "子丑寅卯辰巳午未申酉戌亥".toCharArray().map { it.toString() }

/** 参数输入区（按模式动态显示） */
@Composable
fun ParamFields(
    def: MethodDef,
    mode: Mode,
    pA: String, onA: (String) -> Unit,
    pB: String, onB: (String) -> Unit,
    pC: String, onC: (String) -> Unit,
    hourZhi: String, onHour: (String) -> Unit,
    gender: String, onGender: (String) -> Unit,
    methodKey: String,
    cardNames: String, onCardNames: (String) -> Unit,
    enabled: Boolean
) {
    val id = mode.id

    when {
        id == "interpret" -> {
            FieldLabel("牌名（空格分隔，逆位写 reversed:牌名）")
            NumField(value = cardNames, onValue = onCardNames, placeholder = "如: 愚者 reversed:高塔 圣杯十", keyboard = KeyboardType.Text, enabled = enabled)
            Spacer(Modifier.height(10.dp))
        }
        methodKey == "xiaoliuren" && id == "lunar" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { FieldLabel("农历月"); NumField(pA, onA, "1-12", enabled = enabled) }
                Box(Modifier.weight(1f)) { FieldLabel("农历日"); NumField(pB, onB, "1-30", enabled = enabled) }
            }
            Spacer(Modifier.height(10.dp))
            ZhiDropdown(hourZhi, onHour, enabled)
            Spacer(Modifier.height(10.dp))
        }
        methodKey == "xiaoliuren" && id == "numbers" -> {
            FieldLabel("随口报三个数")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { NumField(pA, onA, "第一数", enabled = enabled) }
                Box(Modifier.weight(1f)) { NumField(pB, onB, "第二数", enabled = enabled) }
                Box(Modifier.weight(1f)) { NumField(pC, onC, "第三数", enabled = enabled) }
            }
            Spacer(Modifier.height(10.dp))
        }
        methodKey == "liuyao" && id == "numbers" -> {
            FieldLabel("报三个数：下卦(1-8)、上卦(1-8)、动爻(1-6)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { NumField(pA, onA, "下卦", enabled = enabled) }
                Box(Modifier.weight(1f)) { NumField(pB, onB, "上卦", enabled = enabled) }
                Box(Modifier.weight(1f)) { NumField(pC, onC, "动爻", enabled = enabled) }
            }
            Spacer(Modifier.height(10.dp))
        }
        methodKey == "tarot" && id == "custom" -> {
            FieldLabel("张数（1-12）")
            NumField(pA, onA, "如 5", enabled = enabled)
            Spacer(Modifier.height(10.dp))
        }
    }

    // 小六壬性别（可不注）
    if (def.gender && id != "interpret") {
        Spacer(Modifier.height(4.dp))
        FieldLabel("问者（贵人分男女，可不注）")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("", "男", "女").forEach { g ->
                val sel = g == gender
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (sel) Color(0x17D9A441) else Color(0x08FFFFFF))
                        .then(if (sel) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(999.dp)) else Modifier)
                        .clickable(enabled = enabled) { onGender(g) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) { Text(if (g.isEmpty()) "不注" else g, color = if (sel) AppColors.Paper else AppColors.Muted, fontSize = 13.sp) }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun NumField(value: String, onValue: (String) -> Unit, placeholder: String, keyboard: KeyboardType = KeyboardType.Number, enabled: Boolean = true) {
    OutlinedTextField(
        value = value, onValueChange = onValue, enabled = enabled,
        placeholder = { Text(placeholder, color = AppColors.Dim) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
            focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
            cursorColor = AppColors.Lantern, focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF)
        )
    )
}

@Composable
private fun ZhiDropdown(value: String, onValue: (String) -> Unit, enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    FieldLabel("时辰")
    Box {
        OutlinedTextField(
            value = "${value}时", onValueChange = {},
            readOnly = true, enabled = enabled,
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = true },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF),
                disabledTextColor = AppColors.Muted, disabledBorderColor = AppColors.Line,
                disabledContainerColor = Color(0x08FFFFFF)
            ),
            trailingIcon = {
                Text("▾", color = AppColors.Muted)
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1C1B22))
        ) {
            ZHI.forEach { z ->
                DropdownMenuItem(
                    text = { Text("${z}时", color = AppColors.Paper, fontSize = 14.sp) },
                    onClick = { onValue(z); expanded = false }
                )
            }
        }
    }
}

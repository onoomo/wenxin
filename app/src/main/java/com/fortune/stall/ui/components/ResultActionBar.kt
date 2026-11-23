package com.fortune.stall.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.AiBridge

/**
 * 通用结果操作条：复制排盘（给 AI / 分享） + 问 AI 解读。
 * 所有术法结果页在滚动区底部统一调用，参数只有完整盘面文本 faceText。
 * @param dark true=深金国风底（紫微等深色页），false=纸面/通用页
 */
@Composable
fun ResultActionBar(faceText: String, modifier: Modifier = Modifier, dark: Boolean = false) {
    val context = LocalContext.current
    val bg = if (dark) Color(0x1AD9A441) else Color(0xFF2A2620)
    val border = if (dark) AppColors.GoldLine else Color(0x66D9A441)
    val fg = if (dark) AppColors.Lantern else Color(0xFFE6C178)
    val askGradient = Brush.verticalGradient(
        if (dark) listOf(Color(0x99D9A441), Color(0x66C96442))
        else listOf(Color(0xFFC0392B), Color(0xFF96281B))
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(bg)
                .border(1.dp, border, RoundedCornerShape(10.dp))
                .clickable {
                    copyFaceText(context, faceText)
                    Toast.makeText(context, "排盘已复制——拿去给你的 AI 断吧", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("⧉ 复制排盘", color = fg, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(askGradient)
                .border(1.dp, border, RoundedCornerShape(10.dp))
                .clickable {
                    val cb = AiBridge.askAi
                    if (cb != null) cb(faceText)
                    else Toast.makeText(context, "AI 通道未就绪", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("✦ 问 AI 解读", color = if (dark) AppColors.Ink else Color.White,
                fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun copyFaceText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("排盘", text))
}

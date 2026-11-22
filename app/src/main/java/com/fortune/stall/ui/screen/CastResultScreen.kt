package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.FortuneEngine
import com.fortune.stall.ui.components.ResultCard
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors

/** 起卦/抽牌结果页：点击抽牌后跳转到此独立页面展示卦面。 */
@Composable
fun CastResultScreen(
    output: FortuneEngine.CastOutput,
    onBack: () -> Unit,
    onAgain: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 顶部返回栏
            Row(
                Modifier.fillMaxWidth().background(Color(0x0AFFFFFF)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(30.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = AppColors.Paper, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "卦 成", color = AppColors.Paper, fontSize = 18.sp,
                    fontFamily = FontFamily.Serif, letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(30.dp))
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ResultCard(output, onReset = onAgain)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

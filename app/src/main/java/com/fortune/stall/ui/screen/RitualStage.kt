package com.fortune.stall.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.ui.components.Coin
import com.fortune.stall.ui.components.Die
import com.fortune.stall.ui.components.YaoGlyph
import com.fortune.stall.ui.theme.AppColors

private val YAO_NAME = arrayOf("初", "二", "三", "四", "五", "上")

/** 仪式动画台：骰子 / 铜钱逐爻显现 / 落印锁标记 */
@Composable
fun RitualStage(
    dicePips: List<Int>?,
    coinTosses: List<Int>?,
    coinYaos: List<Int>?,
    showCoinRows: Int,
    casting: Boolean,
    locked: Boolean
) {
    when {
        dicePips != null -> {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                    dicePips.forEach { Die(pips = it, rolling = false) }
                }
                if (locked) LockTag("落 地 即 锁 · 一 事 一 卦")
            }
        }
        coinTosses != null && coinYaos != null -> {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // 从上爻到初爻显示（初爻在下）
                for (i in (showCoinRows - 1) downTo 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Text("${YAO_NAME[i]}爻", color = AppColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                            repeat(3) { idx ->
                                val isZi = idx >= (3 - coinTosses[i])
                                Coin(zi = isZi)
                            }
                        }
                        YaoGlyph(coinYaos[i])
                    }
                }
                if (locked) LockTag("六 爻 已 成 · 落 地 即 锁")
            }
        }
        casting -> {
            Text("仪式进行中…", color = AppColors.Dim, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LockTag(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text("─ ─ ─  $text  ─ ─ ─", color = AppColors.Lantern, fontSize = 11.5.sp, letterSpacing = 2.sp)
    }
}

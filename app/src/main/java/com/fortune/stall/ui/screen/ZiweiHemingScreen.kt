package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.engine.jishiyu.Ziwei
import com.fortune.stall.engine.jishiyu.ZiweiHeming
import com.fortune.stall.ui.theme.AppColors

/**
 * 紫微合盘页（借鉴 ziwei-ds /heming）：依次排甲方、乙方两张盘，
 * 展示双方命宫/夫妻宫/福德宫摘要，再把两盘简报 + 内置合盘方法论注入 AI 做 6 维解读。
 */
@Composable
fun ZiweiHemingScreen(
    onBack: () -> Unit,
    onAskAi: (String) -> Unit
) {
    var a by remember { mutableStateOf<Ziwei.Result?>(null) }
    var b by remember { mutableStateOf<Ziwei.Result?>(null) }
    var lonA by remember { mutableStateOf<Double?>(null) }
    var lonB by remember { mutableStateOf<Double?>(null) }

    Box(Modifier.fillMaxSize().background(AppColors.Ink)) {
        when {
            a == null -> JiMingpanCastForm(title = "合 盘 · 甲 方", onBack = onBack, showLongitude = true, dark = true) { f ->
                val lon = if (f.trueSolar) f.longitude else null
                a = Ziwei.paipan(f.dateTime, f.isMan, -1, lon, false)
                lonA = lon
            }
            b == null -> JiMingpanCastForm(title = "合 盘 · 乙 方", onBack = { a = null }, showLongitude = true, dark = true) { f ->
                val lon = if (f.trueSolar) f.longitude else null
                b = Ziwei.paipan(f.dateTime, f.isMan, -1, lon, false)
                lonB = lon
            }
            else -> HemingResult(a!!, b!!, onBack = { b = null }, onReA = { a = null }, onAskAi = onAskAi)
        }
    }
}

@Composable
private fun HemingResult(
    a: Ziwei.Result, b: Ziwei.Result,
    onBack: () -> Unit, onReA: () -> Unit, onAskAi: (String) -> Unit
) {
    val gold = AppColors.GoldLine
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = AppColors.Lantern, fontSize = 28.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(10.dp))
            Text("紫微合盘", color = AppColors.Paper, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("倪海厦体系 · 双宫联参（夫妻宫 + 福德宫）", color = AppColors.Dim, fontSize = 10.5.sp)
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            HemingCard("甲方", a, gold, Modifier.weight(1f))
            HemingCard("乙方", b, gold, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(AppColors.Lantern).clickable {
                val ctx = buildString {
                    append(ZiweiHeming.METHODOLOGY)
                    append("\n\n").append(ZiweiHeming.OUTPUT_FORMAT)
                    append("\n\n=== 甲方命盘 ===\n").append(hemingFace(a, "甲方"))
                    append("\n\n=== 乙方命盘 ===\n").append(hemingFace(b, "乙方"))
                }
                onAskAi(ctx)
            }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text("✦ 开始合盘解读（6 维 · AI）", color = AppColors.Ink, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color(0x12FFFFFF)).border(1.dp, gold.copy(0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onReA).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text("重新起甲方盘", color = AppColors.Lantern, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HemingCard(title: String, r: Ziwei.Result, gold: Color, modifier: Modifier) {
    val ming = keyPalace(r, "命宫")
    val fuqi = keyPalace(r, "夫妻宫")
    val fude = keyPalace(r, "福德宫")
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x10FFFFFF))
        .border(1.dp, gold.copy(0.5f), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(title, color = gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("命宫 ${r.ebSoul}", color = AppColors.Paper, fontSize = 11.sp)
        Text("夫妻 ${pStars(fuqi)}", color = AppColors.Paper, fontSize = 10.5.sp)
        Text("福德 ${pStars(fude)}", color = AppColors.Paper, fontSize = 10.5.sp)
        Spacer(Modifier.height(4.dp))
        Text("${r.solarDate.substringBefore('-')} · ${r.juName}", color = AppColors.Dim, fontSize = 9.sp)
        if (ming != null) Text("(${r.chineseDate.split(' ').getOrNull(0) ?: ""})", color = AppColors.Dim, fontSize = 9.sp)
    }
}

private fun keyPalace(r: Ziwei.Result, gong: String): Ziwei.Palace? =
    r.palaces.firstOrNull { Ziwei.gongNameAt(r, hemingZhiIdx(it.zhi)) == gong }

private fun pStars(p: Ziwei.Palace?): String =
    if (p == null) "—" else p.major.joinToString("、") { it.name }.ifBlank { "空宫" }

/** 合盘用的单盘简报：聚焦命宫/夫妻宫/福德宫 + 四化 + 大限 */
private fun hemingFace(r: Ziwei.Result, label: String) = buildString {
    appendLine("$label：${r.solarDate} ${r.chineseDate} 命宫${r.ebSoul} 身宫${r.ebBody} ${r.juName} 命主${r.soul}")
    listOf("命宫", "夫妻宫", "福德宫").forEach { g ->
        val p = keyPalace(r, g)
        if (p != null) {
            val stars = (p.major + p.minor).joinToString(" ") { s ->
                s.name + (if (s.brightness.isNotBlank()) "(${s.brightness})" else "") +
                    (if (s.mutagen.isNotBlank()) "[${s.mutagen}]" else "")
            }
            appendLine("$g[${p.gan}${p.zhi}]：$stars")
        }
    }
    appendLine("生年四化：${r.yearGan} 年；当前大限：${Ziwei.currentDaXian(r, java.time.Year.now().value).first}")
}

private fun hemingZhiIdx(zhi: String) =
    listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥").indexOf(zhi).coerceAtLeast(0)

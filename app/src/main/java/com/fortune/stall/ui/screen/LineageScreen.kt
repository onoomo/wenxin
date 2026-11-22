package com.fortune.stall.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors

/** 一门传承：key 用于标识，glyph/eyebrow/title/desc 用于展示 */
data class Lineage(
    val key: String,
    val glyph: String,
    val eyebrow: String,
    val title: String,
    val desc: String
)

// 技能包资料里只有两套算法，各对应一门真实传承：
// 小六壬 —— 小六壬三宫断卦（君谦一脉理法 + 取象大全 + 道家小六壬 汇编而成）
// 六爻   —— 传统纳甲筮法（京房纳甲，取通行传统规则整理）
// 不虚构额外门派；每门只有资料里真实存在的这一项。
private val XIAO_LINEAGES = listOf(
    Lineage("sangong", "壬", "三 宫 断 卦", "小六壬三宫断卦",
        "掐指三宫，定位为先、象要活断。资料汇君谦一脉理法、取象大全、道家小六壬。"),
    Lineage("jiang", "江", "六 宫 排 盘", "江氏小六壬",
        "六宫排盘，九星、六神、六亲、地支并用，日时身宫同参。移植自江氏排盘器。")
)

private val LIUYAO_LINEAGES = listOf(
    Lineage("najia", "爻", "纳 甲 筮 法", "传统纳甲筮法",
        "京房纳甲装卦，用神为纲、月日为权。取通行传统规则整理。"),
    Lineage("liuyupaipan", "盘", "吉 时 雨 排 盘", "六爻排盘",
        "铜钱摇卦纳甲装卦：六亲、六神、世应、动变、伏神、神煞，一卦排到底。")
)

fun lineagesFor(methodKey: String): List<Lineage> = when (methodKey) {
    "xiaoliuren" -> XIAO_LINEAGES
    "liuyao" -> LIUYAO_LINEAGES
    else -> emptyList()
}

/** 传承选择页：择一脉而入，再进起卦 */
@Composable
fun LineageScreen(
    methodKey: String,
    methodTitle: String,
    onBack: () -> Unit,
    onPick: (Lineage) -> Unit
) {
    val lineages = lineagesFor(methodKey)
    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(25.dp))
            Text("择 传 承", color = AppColors.Paper, fontSize = 26.sp, fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))

            lineages.forEach { lin -> LineageRow(lin = lin, onClick = { onPick(lin) }) }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LineageRow(lin: Lineage, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xB8232330), Color(0xB81C1C22))))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(lin.glyph, color = AppColors.Lantern, fontSize = 20.sp, fontFamily = FontFamily.Serif)
        }
        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
            Text(lin.title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif)
            Text(lin.desc, color = AppColors.Muted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text("›", color = AppColors.Dim, fontSize = 20.sp)
    }
    Spacer(Modifier.height(12.dp))
}

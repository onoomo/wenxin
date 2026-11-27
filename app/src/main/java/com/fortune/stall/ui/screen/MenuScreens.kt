package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.DisplaySettings
import com.fortune.stall.util.TarotImageLoader
import com.fortune.stall.util.TarotSettings
import kotlinx.coroutines.launch

/** 容器页通用骨架：星幕背景 + 顶栏返回 + 标题 + 一列可点击条 */
@Composable
private fun MenuShell(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    topGap: Dp = 28.dp,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(topGap))
            Text(title, color = AppColors.Paper, fontSize = 26.sp, fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(subtitle, color = AppColors.Muted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            content()
            Spacer(Modifier.height(60.dp))
        }
        // 右上角快捷按钮区：必须放在 Column 之后(Box 中后绘制在上层)，
        // 否则 fillMaxSize+verticalScroll 的 Column 会拦截点击导致按钮点不到。
        Box(Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = topGap - 12.dp)) {
            actions()
        }
    }
}

/** 可点击条：胶囊外框 + 左侧图标字 + 标题/说明 + 右侧箭头 */
@Composable
private fun MenuRow(
    glyph: String,
    eyebrow: String,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(14.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(androidx.compose.ui.graphics.Color(0xB8232330), androidx.compose.ui.graphics.Color(0xB81C1C22))
            ))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.padding(end = 13.dp).clip(RoundedCornerShape(9.dp))
                .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, color = AppColors.Lantern, fontSize = 16.sp, fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = AppColors.Lantern, fontSize = 10.sp, letterSpacing = 3.sp)
            Text(title, color = AppColors.Paper, fontSize = 17.sp, fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 4.dp))
            Text(desc, color = AppColors.Muted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text("›", color = AppColors.Dim, fontSize = 20.sp)
    }
    Spacer(Modifier.height(12.dp))
}

/** 存档页 —— 里面放卦账与收藏 */
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpenLedger: () -> Unit,
    onOpenFavorites: () -> Unit,
    favoriteCount: Int = 0
) {
    MenuShell(title = "存 档", subtitle = "落过的印，都收在这里", onBack = onBack) {
        MenuRow(
            glyph = "账", eyebrow = "卦 账", title = "卦账",
            desc = "每一卦的落印、卦面与断语，翻开看看。", onClick = onOpenLedger
        )
        MenuRow(
            glyph = "★", eyebrow = "收 藏", title = "收藏",
            desc = if (favoriteCount > 0) "已星标 $favoriteCount 卦，翻开细看。" else "左滑卦账加星标，收藏都在这里。",
            onClick = onOpenFavorites
        )
    }
}

/** 设置页 —— 显示牌面开关 + 下载牌图 + 技能包（可点击条） */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSkills: () -> Unit,
    onManageTiles: () -> Unit,
    onOpenAi: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 78) }
    var cachedCount by remember { mutableStateOf(0) }
    // 扫目录移到 IO 线程，避免进入设置页时主线程阻塞
    LaunchedEffect(Unit) { cachedCount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { TarotImageLoader.downloadedCount(context) } }

    MenuShell(
        title = "设 置", subtitle = "摊面上用具与心法", onBack = onBack, topGap = 28.dp,
        actions = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingQuickChip(
                    glyph = "✦", label = "动效", active = DisplaySettings.introAnim.value
                ) {
                    val next = !DisplaySettings.introAnim.value
                    DisplaySettings.setIntroAnim(next)
                    Toast.makeText(context, if (next) "已开启开场动画" else "已隐藏开场动画", Toast.LENGTH_SHORT).show()
                }
                SettingQuickChip(
                    glyph = "▦", label = "图标", active = DisplaySettings.iconMode.value
                ) {
                    val next = !DisplaySettings.iconMode.value
                    DisplaySettings.setIconMode(next)
                    Toast.makeText(context, if (next) "已切换：图标网格" else "已切换：详细摊面", Toast.LENGTH_SHORT).show()
                }
            }
        }
    ) {
        // 固定两列：① 下载塔罗牌 ② 显示塔罗牌面 ③ 技能包 ④ AI设置 ⑤ 术法
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingCard(
                eyebrow = "牌 图",
                title = when {
                    downloading -> "下载中 ${progress.first}/${progress.second}"
                    cachedCount >= 78 -> "牌图已就绪 ($cachedCount/78)"
                    cachedCount > 0 -> "继续下载 ($cachedCount/78)"
                    else -> "下载塔罗牌 (78 张)"
                },
                desc = when {
                    downloading -> "正在从 Gitee 拉取牌面，完成后离线可用…"
                    cachedCount >= 78 -> "78 张牌面已存本机，抽牌零流量。点击可重新下载。"
                    else -> "点击下载 78 张牌面到本机（约 2.4MB），之后离线可用。"
                },
                modifier = Modifier.weight(1f),
                onClick = {
                    if (downloading) return@SettingCard
                    if (cachedCount >= 78) {
                        TarotImageLoader.clearAll(context)
                        cachedCount = 0
                        Toast.makeText(context, "已删除牌图，APK 保持精简", Toast.LENGTH_SHORT).show()
                    } else {
                        downloading = true
                        scope.launch {
                            val n = TarotImageLoader.downloadAll(context) { done, total -> progress = done to total }
                            cachedCount = n
                            downloading = false
                            Toast.makeText(context, "牌图下载完成 $n/78，可离线看牌", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) { CardArrow() }
            SettingCard(
                eyebrow = "牌 面", title = "显示塔罗牌面",
                desc = "关闭后牌面只显示文字；开启需先下载牌图。",
                modifier = Modifier.weight(1f),
                onClick = { TarotSettings.setShowCardImage(!TarotSettings.showCardImage.value) }
            ) { SmallSwitch(TarotSettings.showCardImage.value) { TarotSettings.setShowCardImage(it) } }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingCard(
                eyebrow = "心 法", title = "技能包",
                desc = "小六壬 · 六爻 · 塔罗 的断卦工作流与分册资料。",
                modifier = Modifier.weight(1f), onClick = onOpenSkills
            ) { CardArrow() }
            SettingCard(
                eyebrow = "智 能", title = "AI 设置",
                desc = "配置 OpenAI 兼容接口的密钥与提示词，卦账里「问 AI」即可解读排盘。",
                modifier = Modifier.weight(1f), onClick = onOpenAi
            ) { CardArrow() }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingCard(
                eyebrow = "术 法", title = "隐藏 / 排序术法",
                desc = "进入首页编辑态：拖动调整术法顺序，点 × 隐藏不用的术法。",
                modifier = Modifier.weight(1f), onClick = onManageTiles
            ) { CardArrow() }
            Spacer(Modifier.weight(1f))
        }
    }
}


/** 两列紧凑设置卡片：无左侧图标，眉标在上、标题、说明，尾部放开关或箭头 */
@Composable
private fun SettingCard(
    eyebrow: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(14.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(androidx.compose.ui.graphics.Color(0xB8232330), androidx.compose.ui.graphics.Color(0xB81C1C22))
            ))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Text(eyebrow, color = AppColors.Lantern, fontSize = 10.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = AppColors.Paper, fontSize = 16.sp, fontFamily = FontFamily.Serif,
                modifier = Modifier.weight(1f), maxLines = 1)
            trailing()
        }
        Spacer(Modifier.height(6.dp))
        Text(desc, color = AppColors.Muted, fontSize = 11.sp, minLines = 2, maxLines = 2)
    }
}

/** 设置页右上角快捷胶囊：金色图标 + 文字，active 高亮描边 */
@Composable
private fun SettingQuickChip(
    glyph: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, if (active) AppColors.GoldLine else AppColors.Line), RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(androidx.compose.ui.graphics.Color(0xE8262632), androidx.compose.ui.graphics.Color(0xF016161C))
            ))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(glyph, color = AppColors.Lantern, fontSize = 15.sp)
        Text(label, color = if (active) AppColors.Paper else AppColors.Muted,
            fontSize = 12.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Serif)
    }
}

/** 卡片尾部右箭头 */
@Composable
private fun CardArrow() {
    Text("›", color = AppColors.Dim, fontSize = 19.sp)
}

/** 卡片尾部小开关 */
@Composable
private fun SmallSwitch(checked: Boolean, onChecked: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChecked,
        modifier = Modifier.scale(0.82f),
        colors = SwitchDefaults.colors(
            checkedThumbColor = AppColors.Ink,
            checkedTrackColor = AppColors.Lantern,
            uncheckedThumbColor = AppColors.Muted,
            uncheckedTrackColor = AppColors.Shop2
        )
    )
}

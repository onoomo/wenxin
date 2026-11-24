package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import com.fortune.stall.R
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.engine.jishiyu.JishiyuRegistry
import com.fortune.stall.util.DisplaySettings
import com.fortune.stall.util.HomeTilesSettings

/** 开场动画进程级标记：App 存活期间只播一次，页面切换/点图标返回不再重播 */
private object HomeIntroOnce {
    var shown = false
}

/** 问心处 · 摊面首页 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenCast: (String) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenJishiyu: () -> Unit,
    onOpenJishiyuModule: (String) -> Unit
) {
    // 顶部安全留白：读取系统真实边衬区（状态栏高度 + 挖孔安全区，取较大值）。
    // 使用 IgnoringVisibility 变体，即使沉浸模式下状态栏被隐藏也能拿到其高度。
    // 不叠加任何额外呼吸空间，留白完全等于设备边衬区高度。
    val density = LocalDensity.current
    val topInset = with(density) {
        maxOf(
            WindowInsets.statusBarsIgnoringVisibility.getTop(this),
            WindowInsets.displayCutout.getTop(this)
        ).toDp()
    }
    // 底部悬浮栏安全区：导航栏高度 + 挖孔底部（沉浸隐藏也能取到）
    val bottomInset = with(density) {
        maxOf(
            WindowInsets.navigationBarsIgnoringVisibility.getBottom(this),
            WindowInsets.displayCutout.getBottom(this)
        ).toDp()
    }
    val context = LocalContext.current

    // ===== 开场动画：问心处 → 心形 → 消失（快速流畅，约 0.85s，仅首次打开播放一次）=====
    // phase 0 文字停留 → 1 文字淡出+心形淡入 → 2 心形放大 → 3 完成(标题区收起)
    // 已播放过则直接进入完成态（无标题、无动画），避免点图标/切页返回时反复重播
    var animPhase by remember { mutableStateOf(if (HomeIntroOnce.shown) 3 else 0) }
    var introDone by remember { mutableStateOf(HomeIntroOnce.shown) }
    LaunchedEffect(Unit) {
        if (HomeIntroOnce.shown) return@LaunchedEffect
        delay(240);  animPhase = 1   // 文字短暂停留后开始变身
        delay(180);  animPhase = 2   // 心形放大
        delay(170);  animPhase = 3   // 心形消散
        delay(190);  introDone = true // 标题区彻底收起，露出图1布局
        HomeIntroOnce.shown = true
    }
    // 统一用 FastOutSlowIn 缓动：起步快、收尾柔，整体更顺
    val textAlpha by animateFloatAsState(
        targetValue = if (animPhase == 0) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing), label = "textAlpha")
    val textScale by animateFloatAsState(
        targetValue = if (animPhase == 0) 1f else 1.12f,
        animationSpec = tween(260, easing = FastOutSlowInEasing), label = "textScale")
    val heartAlpha by animateFloatAsState(
        targetValue = when (animPhase) { 1 -> 1f; 2 -> 1f; else -> 0f },
        animationSpec = tween(240, easing = FastOutSlowInEasing), label = "heartAlpha")
    val heartScale by animateFloatAsState(
        targetValue = when (animPhase) { 1 -> 0.6f; 2 -> 1.1f; else -> 1.28f },
        animationSpec = tween(300, easing = FastOutSlowInEasing), label = "heartScale")
    // 标题占位高度：动画完成后收为 0，让副标题上移（图1效果）
    // 用 Float 动画再乘 density 转 Dp，避免依赖完整 animation 包
    val heroHeightFrac by animateFloatAsState(
        targetValue = if (introDone) 0f else 1f,
        animationSpec = tween(240, easing = FastOutSlowInEasing), label = "heroHeight")
    val heroHeight = with(density) { (58f * heroHeightFrac).dp }

    Box(modifier = Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp + bottomInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topInset))
            // Hero：问心处 → 心形 → 消失
            Box(
                modifier = Modifier.fillMaxWidth().height(heroHeight),
                contentAlignment = Alignment.Center
            ) {
                // 「问心处」文字：phase0 显示，之后淡出并轻微放大
                Text(
                    "问心处",
                    color = AppColors.Paper,
                    fontSize = 36.sp, letterSpacing = 7.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(textAlpha).scale(textScale)
                )
                // 心形图案：phase1/2 淡入、放大，phase3 淡出消失
                if (!introDone) {
                    Canvas(
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(heartAlpha)
                            .scale(heartScale)
                    ) {
                        drawHeart(AppColors.Paper)
                    }
                }
            }
            Text(
                "自家夜市的算卦小摊 · 卦面即印章",
                color = AppColors.Muted, fontSize = 12.5.sp, letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(28.dp))

            // 编辑态提示条：拖动排序、点角标隐藏、完成、恢复默认
            if (HomeTilesSettings.editMode.value) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xE8262632), Color(0xF016161C))))
                        .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("编 辑 术 法", color = AppColors.Lantern, fontSize = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, letterSpacing = 2.sp)
                    Text("长按拖动可排序 · 点右上角 × 隐藏", color = AppColors.Muted, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(AppColors.Ember).clickable { HomeTilesSettings.reset() }
                            .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("恢复默认", color = Color.White, fontSize = 12.sp)
                        }
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(AppColors.Lantern).clickable { HomeTilesSettings.editMode.value = false }
                            .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("完 成", color = AppColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // 统一点击分流：小六壬/六爻/塔罗走传承页，其余走术数模块入口
            fun openTile(key: String, label: String) {
                when {
                    key.isEmpty() -> Toast.makeText(
                        context, "「$label」排盘功能移植中，敬请期待", Toast.LENGTH_SHORT
                    ).show()
                    key in listOf("xiaoliuren", "liuyao", "tarot") -> onOpenCast(key)
                    else -> onOpenJishiyuModule(key)
                }
            }

            // 编辑态：由长按磁贴进入，或设置页「隐藏/排序术法」入口进入
            val editing by HomeTilesSettings.editMode
            // 按自定义顺序 + 隐藏设置生成当前要显示的清单（两者模式共用）
            val allByKey = remember { HOME_TILES.associateBy { it.key } }
            val displayedTiles = remember(HomeTilesSettings.revision.value) {
                HomeTilesSettings.visibleKeys().mapNotNull { allByKey[it] }
            }

            if (DisplaySettings.iconMode.value) {
                IconGridEditable(
                    tiles = displayedTiles,
                    editing = editing,
                    openTile = { key, label -> openTile(key, label) }
                )
            } else {
                // 详细模式：按自定义顺序与隐藏设置列出，每项带眉标与详细说明
                for (t in displayedTiles) {
                    StallCard(
                        eyebrow = t.eyebrow, title = t.label, desc = t.desc,
                        onClick = { openTile(t.key, t.label) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // 底部悬浮胶囊栏：存档 / 设置 合并为一体，固定在屏幕底部
        BottomCapsuleBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp + bottomInset),
            onOpenArchive = onOpenArchive,
            onOpenSettings = onOpenSettings
        )
    }
}

/** 在 DrawScope 中画一颗实心心形（用于开场「问心处 → 心 → 消失」动画）。 */
private fun DrawScope.drawHeart(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.92f)                 // 心尖
        cubicTo(w * 1.12f, h * 0.52f, w * 0.98f, h * 0.02f, w * 0.5f, h * 0.28f) // 右半
        cubicTo(w * 0.02f, h * 0.02f, w * -0.12f, h * 0.52f, w * 0.5f, h * 0.92f) // 左半
        close()
    }
    drawPath(path = path, color = color)
}

/** 底部悬浮胶囊栏：两个入口合并在一块玻璃质感胶囊里，中间竖线分隔 */
@Composable
private fun BottomCapsuleBar(
    modifier: Modifier = Modifier,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xE8262632), Color(0xF016161C))))
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenArchive).padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▤", color = AppColors.Lantern, fontSize = 17.sp)
                Text("存 档", color = AppColors.Paper, fontSize = 13.sp, letterSpacing = 3.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 3.dp))
            }
        }
        // 分隔竖线
        Box(
            Modifier.height(28.dp).width(1.dp).background(AppColors.GoldLine.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenSettings).padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚙", color = AppColors.Lantern, fontSize = 17.sp)
                Text("设 置", color = AppColors.Paper, fontSize = 13.sp, letterSpacing = 3.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun StallCard(eyebrow: String, title: String, desc: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(
                Color(0xB8232330), Color(0xB81C1C22)
            )))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(eyebrow, color = AppColors.Lantern, fontSize = 10.5.sp, letterSpacing = 4.sp)
                Text(title, color = AppColors.Paper, fontSize = 21.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 7.dp, bottom = 6.dp))
                Text(desc, color = AppColors.Muted, fontSize = 12.5.sp, lineHeight = 22.sp)
            }
            Text("›", color = AppColors.Dim, fontSize = 22.sp)
        }
    }
}

/** 图标模式条目：名称 + 目标 key（空=暂无模块）+ 图标类型 */
private data class IconTile1(val label: String, val key: String, val glyph: Glyph)

/** 单个图标磁贴：100% 透明卡片 + 余烬红圆角方块 + 白色线性图标 + 浅色名称（融入深空背景） */
@Composable
private fun IconTileNew(item: IconTile1, onClick: () -> Unit) {
    // 名称单行显示：按字数自适应字号，避免窄格把名字折成两行
    val labelSize = when {
        item.label.length >= 5 -> 10.sp
        item.label.length == 4 -> 11.sp
        else -> 12.sp
    }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(15.dp))
                .background(AppColors.Ember),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = glyphRes(item.glyph)),
                contentDescription = item.label,
                modifier = Modifier.fillMaxSize(0.66f),
                contentScale = ContentScale.Fit
            )
        }
        Text(item.label, color = AppColors.Paper, fontSize = labelSize,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            maxLines = 1, softWrap = false,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

/**
 * 4 列可编辑术法网格。
 * 常态：点击打开术法；长按进入编辑态。
 * 编辑态：拖动磁贴到目标位置松手即重排；点右上角 × 隐藏该术法。
 */
@Composable
private fun IconGridEditable(
    tiles: List<HomeTile>,
    editing: Boolean,
    openTile: (String, String) -> Unit
) {
    // 每个格子在父容器内的边界（按当前显示顺序索引 -> Rect），用于拖拽落点命中
    val rects = remember { mutableMapOf<Int, Rect>() }
    // 当前正在拖动的显示索引与位移
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // 落点高亮
    var hoverIndex by remember { mutableStateOf<Int?>(null) }

    // 编辑态开关变化时清理拖拽态
    LaunchedEffect(editing) {
        if (!editing) { draggingIndex = null; dragOffset = Offset.Zero; hoverIndex = null }
    }

    var idx = 0
    while (idx < tiles.size) {
        val row = mutableListOf<Pair<Int, HomeTile>>()
        repeat(4) { if (idx < tiles.size) { row.add(idx to tiles[idx]); idx++ } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { slot ->
                Box(Modifier.weight(1f)) {
                    if (slot < row.size) {
                        val (displayIndex, item) = row[slot]
                        val isDragging = draggingIndex == displayIndex
                        val isHover = hoverIndex == displayIndex && !isDragging
                        val hidden = HomeTilesSettings.isHidden(item.key)
                        val tileModifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { c ->
                                val p = c.positionInRoot()
                                rects[displayIndex] = Rect(
                                    p.x, p.y,
                                    p.x + c.size.width, p.y + c.size.height
                                )
                            }
                            .graphicsLayer {
                                if (isDragging) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    scaleX = 1.06f
                                    scaleY = 1.06f
                                    shadowElevation = 12f
                                }
                            }
                        Box(
                            modifier = tileModifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = if (isDragging || isHover) 2.dp else 0.dp,
                                    color = if (isDragging || isHover) AppColors.Lantern else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .graphicsLayer { alpha = if (hidden && editing) 0.45f else 1f }
                                .combinedClickable(
                                    onClick = { if (!editing) openTile(item.key, item.label) },
                                    onLongClick = {
                                        if (!editing) HomeTilesSettings.editMode.value = true
                                    }
                                )
                                .then(
                                    if (editing) Modifier.pointerInput(displayIndex) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingIndex = displayIndex
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                                // 命中检测：拖动方块中心落在哪个显示索引
                                                val center = Offset(
                                                    rects[displayIndex]?.center?.x?.plus(dragOffset.x) ?: 0f,
                                                    rects[displayIndex]?.center?.y?.plus(dragOffset.y) ?: 0f
                                                )
                                                hoverIndex = rects.entries.firstOrNull { (i, r) ->
                                                    i != displayIndex && r.contains(center)
                                                }?.key
                                            },
                                            onDragEnd = {
                                                val target = hoverIndex
                                                if (target != null && draggingIndex != null) {
                                                    val fromKey = tiles[draggingIndex!!].key
                                                    val toKey = tiles[target].key
                                                    val from = HomeTilesSettings.order.value.indexOf(fromKey)
                                                    val to = HomeTilesSettings.order.value.indexOf(toKey)
                                                    HomeTilesSettings.move(from, to)
                                                }
                                                draggingIndex = null
                                                dragOffset = Offset.Zero
                                                hoverIndex = null
                                            },
                                            onDragCancel = {
                                                draggingIndex = null
                                                dragOffset = Offset.Zero
                                                hoverIndex = null
                                            }
                                        )
                                    } else Modifier
                                )
                        ) {
                            IconTileNew(IconTile1(item.label, item.key, item.glyph)) {
                                if (!editing) openTile(item.key, item.label)
                            }
                            if (editing) {
                                // 隐藏角标：点一下隐藏/恢复
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 6.dp, end = 6.dp)
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (hidden) AppColors.Lantern else AppColors.Ember)
                                        .clickable { HomeTilesSettings.toggleHidden(item.key) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (hidden) "✓" else "×",
                                        color = if (hidden) AppColors.Ink else Color.White,
                                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** 用 Canvas 画出图1风格的白色线性图标 */
private fun DrawScope.drawGlyph(glyph: Glyph) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.075f)
    val white = Color.White
    when (glyph) {
        Glyph.BAZI -> { // 八字排盘：栅格窗
            val r = w * 0.62f
            val l = (w - r) / 2
            drawRoundRect(white, topLeft = Offset(l, h * 0.18f), size = Size(r, h * 0.64f),
                cornerRadius = CornerRadius(w * 0.06f), style = stroke)
            drawLine(white, Offset(l + r * 0.33f, h * 0.18f), Offset(l + r * 0.33f, h * 0.82f), stroke.width)
            drawLine(white, Offset(l + r * 0.67f, h * 0.18f), Offset(l + r * 0.67f, h * 0.82f), stroke.width)
            drawLine(white, Offset(l, h * 0.5f), Offset(l + r, h * 0.5f), stroke.width)
        }
        Glyph.MINGLI -> { // 命理奇门：终身盘网格带点
            val r = w * 0.6f; val l = (w - r) / 2; val t = h * 0.2f; val b = h * 0.8f
            drawRoundRect(white, topLeft = Offset(l, t), size = Size(r, b - t),
                cornerRadius = CornerRadius(w * 0.05f), style = stroke)
            drawLine(white, Offset(l + r / 2, t), Offset(l + r / 2, b), stroke.width)
            drawLine(white, Offset(l, t + (b - t) / 2), Offset(l + r, t + (b - t) / 2), stroke.width)
            drawCircle(white, radius = w * 0.045f, center = Offset(l + r / 2, t + (b - t) / 2))
        }
        Glyph.YINPAN -> { // 阴盘奇门：月牙
            val cx = w / 2; val cy = h / 2; val r = w * 0.32f
            drawCircle(white, radius = r, center = Offset(cx, cy), style = stroke)
            drawCircle(white, radius = r * 0.62f, center = Offset(cx + r * 0.42f, cy - r * 0.16f))
        }
        Glyph.QIMEN -> { // 九宫格
            val r = w * 0.66f; val l = (w - r) / 2; val t = h * 0.17f; val b = h * 0.83f
            drawRect(white, topLeft = Offset(l, t), size = Size(r, b - t), style = stroke)
            drawLine(white, Offset(l + r / 3, t), Offset(l + r / 3, b), stroke.width)
            drawLine(white, Offset(l + 2 * r / 3, t), Offset(l + 2 * r / 3, b), stroke.width)
            drawLine(white, Offset(l, t + (b - t) / 3), Offset(l + r, t + (b - t) / 3), stroke.width)
            drawLine(white, Offset(l, t + 2 * (b - t) / 3), Offset(l + r, t + 2 * (b - t) / 3), stroke.width)
        }
        Glyph.ZIWEI -> { // 紫微斗数：星点连线
            val pts = listOf(Offset(w * 0.24f, h * 0.3f), Offset(w * 0.5f, h * 0.2f),
                Offset(w * 0.76f, h * 0.3f), Offset(w * 0.68f, h * 0.62f), Offset(w * 0.32f, h * 0.62f))
            for (i in pts.indices) drawLine(white, pts[i], pts[(i + 1) % pts.size], stroke.width)
            pts.forEach { drawCircle(white, radius = w * 0.06f, center = it) }
        }
        Glyph.LIUYAO -> { // 六爻：六条横纹
            for (i in 0 until 6) {
                val y = h * (0.22f + i * 0.112f)
                drawLine(white, Offset(w * 0.24f, y), Offset(w * 0.76f, y), stroke.width)
            }
        }
        Glyph.MEIHUA -> { // 梅花：五瓣花
            val cx = w / 2; val cy = h / 2
            for (i in 0 until 5) {
                val a = (Math.PI * 2 * i / 5).toFloat()
                drawCircle(white, radius = w * 0.17f,
                    center = Offset(cx + kotlin.math.cos(a) * w * 0.2f, cy + kotlin.math.sin(a) * h * 0.2f),
                    style = stroke)
            }
        }
        Glyph.DALIUREN -> { // 大六壬：同心圆带心
            val cx = w / 2; val cy = h / 2
            drawCircle(white, radius = w * 0.32f, center = Offset(cx, cy), style = stroke)
            drawCircle(white, radius = w * 0.2f, center = Offset(cx, cy), style = stroke)
            drawCircle(white, radius = w * 0.06f, center = Offset(cx, cy))
        }
        Glyph.QIDU -> { // 戚都翁：方块叠印
            drawRoundRect(white, topLeft = Offset(w * 0.26f, h * 0.24f), size = Size(w * 0.42f, h * 0.34f),
                cornerRadius = CornerRadius(w * 0.06f), style = stroke)
            drawRoundRect(white, topLeft = Offset(w * 0.32f, h * 0.44f), size = Size(w * 0.42f, h * 0.34f),
                cornerRadius = CornerRadius(w * 0.06f), style = stroke)
        }
        Glyph.CHUANREN -> { // 奇门穿壬：圆内九宫
            val cx = w / 2; val cy = h / 2; val r = w * 0.34f
            drawCircle(white, radius = r, center = Offset(cx, cy), style = stroke)
            val g = r * 0.55f
            drawLine(white, Offset(cx - g, cy - g), Offset(cx + g, cy - g), stroke.width)
            drawLine(white, Offset(cx - g, cy + g), Offset(cx + g, cy + g), stroke.width)
            drawLine(white, Offset(cx - g, cy - g), Offset(cx - g, cy + g), stroke.width)
            drawLine(white, Offset(cx + g, cy - g), Offset(cx + g, cy + g), stroke.width)
        }
        Glyph.XIAOLIUREN -> { // 小六壬：六边形
            val cx = w / 2; val cy = h / 2; val r = w * 0.34f
            val pts = (0 until 6).map {
                val a = (Math.PI / 3 * it).toFloat()
                Offset(cx + kotlin.math.cos(a) * r, cy + kotlin.math.sin(a) * r)
            }
            for (i in pts.indices) drawLine(white, pts[i], pts[(i + 1) % 6], stroke.width)
            drawLine(white, pts[0], pts[3], stroke.width)
            drawLine(white, pts[1], pts[4], stroke.width)
            drawLine(white, pts[2], pts[5], stroke.width)
        }
        Glyph.TAROT -> { // 塔罗：卡牌方框
            val r = w * 0.5f; val l = (w - r) / 2
            drawRoundRect(white, topLeft = Offset(l, h * 0.16f), size = Size(r, h * 0.68f),
                cornerRadius = CornerRadius(w * 0.06f), style = stroke)
            drawLine(white, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.72f), stroke.width)
            drawLine(white, Offset(l + r * 0.25f, h * 0.4f), Offset(l + r * 0.75f, h * 0.4f), stroke.width)
        }
        Glyph.SHANXIANG -> { // 山向奇门：罗盘指针
            val cx = w / 2; val cy = h / 2; val r = w * 0.34f
            drawCircle(white, radius = r, center = Offset(cx, cy), style = stroke)
            val path = Path().apply {
                moveTo(cx, cy - r * 0.72f)
                lineTo(cx - r * 0.28f, cy + r * 0.4f)
                lineTo(cx, cy + r * 0.18f)
                lineTo(cx + r * 0.28f, cy + r * 0.4f)
                close()
            }
            drawPath(path, white, style = stroke)
        }
        Glyph.FEIXING -> { // 玄空飞星：三叶旋风
            val cx = w / 2; val cy = h / 2
            for (i in 0 until 3) {
                val a = (Math.PI * 2 * i / 3).toFloat()
                val path = Path().apply {
                    moveTo(cx, cy)
                    quadraticTo(cx + kotlin.math.cos(a) * w * 0.3f, cy + kotlin.math.sin(a) * h * 0.3f - h * 0.14f,
                        cx + kotlin.math.cos(a) * w * 0.38f, cy + kotlin.math.sin(a) * h * 0.38f)
                }
                drawPath(path, white, style = stroke)
            }
            drawCircle(white, radius = w * 0.07f, center = Offset(cx, cy))
        }
        Glyph.YIZHANG -> { // 达摩一掌经：手掌
            drawRoundRect(white, topLeft = Offset(w * 0.34f, h * 0.34f), size = Size(w * 0.32f, h * 0.46f),
                cornerRadius = CornerRadius(w * 0.08f), style = stroke)
            for (i in 0 until 4) {
                val x = w * (0.38f + i * 0.08f)
                drawLine(white, Offset(x, h * 0.34f), Offset(x, h * 0.18f), stroke.width)
            }
        }
        Glyph.WANNIANLI -> { // 万年历：日历
            val r = w * 0.6f; val l = (w - r) / 2; val t = h * 0.26f; val b = h * 0.82f
            drawRoundRect(white, topLeft = Offset(l, t), size = Size(r, b - t),
                cornerRadius = CornerRadius(w * 0.05f), style = stroke)
            drawLine(white, Offset(l, t + (b - t) * 0.28f), Offset(l + r, t + (b - t) * 0.28f), stroke.width)
            drawLine(white, Offset(l + r * 0.32f, t - h * 0.08f), Offset(l + r * 0.32f, t + h * 0.06f), stroke.width)
            drawLine(white, Offset(l + r * 0.68f, t - h * 0.08f), Offset(l + r * 0.68f, t + h * 0.06f), stroke.width)
            for (i in 0..1) for (j in 0..2) {
                drawCircle(white, radius = w * 0.025f,
                    center = Offset(l + r * (0.22f + j * 0.28f), t + (b - t) * (0.48f + i * 0.26f)))
            }
        }
        Glyph.LAOHUANGLI -> { // 老黄历：日历带阴阳
            val r = w * 0.6f; val l = (w - r) / 2; val t = h * 0.26f; val b = h * 0.82f
            drawRoundRect(white, topLeft = Offset(l, t), size = Size(r, b - t),
                cornerRadius = CornerRadius(w * 0.05f), style = stroke)
            drawLine(white, Offset(l + r * 0.32f, t - h * 0.08f), Offset(l + r * 0.32f, t + h * 0.06f), stroke.width)
            drawLine(white, Offset(l + r * 0.68f, t - h * 0.08f), Offset(l + r * 0.68f, t + h * 0.06f), stroke.width)
            val cx = w / 2; val cy = (t + b) / 2 + h * 0.06f; val rr = w * 0.15f
            drawCircle(white, radius = rr, center = Offset(cx, cy), style = stroke)
            drawLine(white, Offset(cx, cy - rr), Offset(cx, cy + rr), stroke.width)
            drawCircle(white, radius = rr * 0.22f, center = Offset(cx, cy - rr * 0.5f))
        }
        Glyph.HEHUN -> { // 八字合婚：双环相扣
            drawCircle(white, radius = w * 0.24f, center = Offset(w * 0.38f, h * 0.5f), style = stroke)
            drawCircle(white, radius = w * 0.24f, center = Offset(w * 0.62f, h * 0.5f), style = stroke)
        }
        Glyph.PHONE -> { // 手机号吉凶：手机
            val r = w * 0.44f; val l = (w - r) / 2
            drawRoundRect(white, topLeft = Offset(l, h * 0.14f), size = Size(r, h * 0.72f),
                cornerRadius = CornerRadius(w * 0.08f), style = stroke)
            drawLine(white, Offset(w * 0.44f, h * 0.2f), Offset(w * 0.56f, h * 0.2f), stroke.width)
            drawCircle(white, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.78f))
        }
        Glyph.QIANXU -> { // 潜虚筮法：一束蓍草
            for (i in 0..4) {
                val x = w * (0.26f + i * 0.12f)
                drawLine(white, Offset(x, h * 0.82f), Offset(w * 0.5f + (i - 2) * w * 0.045f, h * 0.18f), stroke.width)
            }
            drawLine(white, Offset(w * 0.28f, h * 0.58f), Offset(w * 0.72f, h * 0.58f), stroke.width)
        }
    }
}

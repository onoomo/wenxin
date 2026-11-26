package com.fortune.stall.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.data.Session
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 卦象分类：名称 + 命中的 method 集合（"全部"用 null 表示） */
private data class LedgerCategory(val name: String, val methods: Set<String>?)

/** 卦账顶部分类条（第一行固定展示，点击展开全部） */
private val LEDGER_CATEGORIES = listOf(
    LedgerCategory("全部", null),
    LedgerCategory("奇门", setOf("qimen", "mingliqimen", "yinpanqimen", "shanxiangqimen", "chuanren")),
    LedgerCategory("紫微", setOf("ziwei")),
    LedgerCategory("六爻", setOf("liuyao")),
    LedgerCategory("梅花", setOf("meihua")),
    LedgerCategory("六壬", setOf("daliuren", "xiaoliuren")),
    LedgerCategory("飞星", setOf("ninestar")),
    LedgerCategory("一掌经", setOf("yizhangjing")),
    LedgerCategory("戚都翁", setOf("qiduweng")),
    LedgerCategory("潜虚", setOf("qianxu")),
    LedgerCategory("塔罗", setOf("tarot")),
    LedgerCategory("雷诺曼", setOf("lenormand")),
    LedgerCategory("其它", setOf("shuzi", "wannianli", "laohuangli", "chenggu", "custom"))
)

/** 卦账页：列表（左滑收藏/复制/删除）+ 详情弹层 + 断语存档 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerScreen(
    db: FortuneDb,
    onBack: () -> Unit,
    onlyFavorites: Boolean = false,
    onAskAi: (Session) -> Unit = {}
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(if (onlyFavorites) db.favorites() else db.recent()) }
    var selected by remember { mutableStateOf<Session?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf("全部") }
    var expanded by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var multiSelect by remember { mutableStateOf(false) }
    var checkedIds by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(refreshKey) {
        sessions = if (onlyFavorites) db.favorites() else db.recent()
    }

    fun reload() { refreshKey++ }

    // 按当前分类筛选
    val visible = remember(sessions, category) {
        val cat = LEDGER_CATEGORIES.firstOrNull { it.name == category }
        if (cat?.methods == null) sessions else sessions.filter { it.method in cat.methods }
    }

    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(52.dp))
            Spacer(Modifier.height(14.dp))
            // 标题行：居中标题 + 右上角「多选 / 退出多选」
            Box(Modifier.fillMaxWidth()) {
                Text(if (onlyFavorites) "收 藏" else "卦 账", color = AppColors.Paper, fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 3.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(if (multiSelect) "退出多选" else "多选", color = AppColors.Lantern, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)
                        .clickable {
                            multiSelect = !multiSelect
                            checkedIds = emptySet()
                        })
            }
            Text(
                if (multiSelect) "点选要删除的卦 · 可多选"
                else if (onlyFavorites) "左滑取消收藏 · 星标都在这里"
                else "左滑一卦：收藏 · 复制排盘 · 删除",
                color = AppColors.Muted, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))

            // 卦象分类选择条：默认一行 5 个，点击可展开全部
            val shownCats = if (expanded) LEDGER_CATEGORIES else LEDGER_CATEGORIES.take(5)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                shownCats.forEach { cat ->
                    val sel = cat.name == category
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (sel) AppColors.Ember else Color(0x1AF4F3EF))
                            .border(1.dp, if (sel) AppColors.Ember else AppColors.GoldLine.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable { category = cat.name }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(cat.name, color = if (sel) AppColors.Paper else AppColors.Muted,
                            fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                // 展开/收起
                Box(
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1AF4F3EF))
                        .border(1.dp, AppColors.GoldLine.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (expanded) "收起▴" else "展开▾", color = AppColors.Lantern, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (visible.isEmpty()) {
                Text(if (category != "全部") "「$category」类暂无记录" else if (onlyFavorites) "还没有收藏的卦。去卦账左滑星标收藏。" else "账上还没落过印。去起第一卦。",
                    color = AppColors.Dim, fontSize = 12.5.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(visible, key = { it.id }) { s ->
                        if (multiSelect) {
                            val checked = s.id in checkedIds
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(if (checked) Color(0x22326B5A) else Color(0xFF1C1C22))
                                    .border(1.dp, if (checked) AppColors.Jade else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable {
                                        checkedIds = if (checked) checkedIds - s.id else checkedIds + s.id
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                                            .background(if (checked) AppColors.Jade else Color(0x1AF4F3EF))
                                            .border(1.dp, if (checked) AppColors.Jade else AppColors.GoldLine.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (checked) Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(Modifier.weight(1f)) { LedgerRowContent(s) }
                                }
                            }
                        } else {
                            SwipeLedgerRow(
                                s = s,
                                onClick = { selected = db.get(s.id) },
                                onFavorite = { db.setFavorite(s.id, !s.favorite); reload() },
                                onCopy = {
                                    copyText(context, s.face)
                                    Toast.makeText(context, "排盘已复制——拿去给你的 AI 断吧", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { db.delete(s.id); reload() }
                            )
                        }
                    }
                }
            }

            // 多选操作栏：全选/取消 + 已选数 + 删除
            if (multiSelect) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14F4F3EF)).border(1.dp, AppColors.GoldLine.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (checkedIds.size == visible.size && visible.isNotEmpty()) "取消全选" else "全选",
                        color = AppColors.Lantern, fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            checkedIds = if (checkedIds.size == visible.size) emptySet() else visible.map { it.id }.toSet()
                        }
                    )
                    Text("已选 ${checkedIds.size} 卦", color = AppColors.Muted, fontSize = 12.sp,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (checkedIds.isEmpty()) Color(0x33FFFFFF) else AppColors.Ember)
                            .clickable(enabled = checkedIds.isNotEmpty()) { showDeleteSelectedConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text("删除", color = if (checkedIds.isEmpty()) AppColors.Dim else Color.White,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        selected?.let { session ->
            SessionSheet(session = session, onClose = { selected = null },
                onAskAi = { onAskAi(session) },
                onSave = { v ->
                    db.saveVerdict(session.id, v)
                    selected = db.get(session.id)
                    reload()
                })
        }

        if (showClearConfirm) {
            Box(
                Modifier.fillMaxSize().background(Color(0x99000000)).clickable { showClearConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth(0.78f).clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1B22)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("清空卦象", color = AppColors.Paper, fontSize = 18.sp,
                        fontFamily = FontFamily.Serif, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (onlyFavorites) "确定清空全部收藏吗？此操作不可恢复。" else "确定清空全部卦象记录吗？此操作不可恢复。",
                        color = AppColors.Muted, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp))
                            .border(1.dp, AppColors.GoldLine.copy(alpha = 0.4f), RoundedCornerShape(21.dp))
                            .clickable { showClearConfirm = false }, contentAlignment = Alignment.Center) {
                            Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                        }
                        Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp))
                            .background(AppColors.Ember).clickable {
                                db.clearAll(onlyFavorites)
                                showClearConfirm = false
                                reload()
                                Toast.makeText(context, if (onlyFavorites) "收藏已清空" else "卦象已清空", Toast.LENGTH_SHORT).show()
                            }, contentAlignment = Alignment.Center) {
                            Text("清空", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showDeleteSelectedConfirm) {
            Box(
                Modifier.fillMaxSize().background(Color(0x99000000)).clickable { showDeleteSelectedConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth(0.78f).clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1B22)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("删除选中卦象", color = AppColors.Paper, fontSize = 18.sp,
                        fontFamily = FontFamily.Serif, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("确定删除选中的 ${checkedIds.size} 条记录吗？此操作不可恢复。",
                        color = AppColors.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp))
                            .border(1.dp, AppColors.GoldLine.copy(alpha = 0.4f), RoundedCornerShape(21.dp))
                            .clickable { showDeleteSelectedConfirm = false }, contentAlignment = Alignment.Center) {
                            Text("取消", color = AppColors.Muted, fontSize = 14.sp)
                        }
                        Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp))
                            .background(AppColors.Ember).clickable {
                                val n = checkedIds.size
                                db.deleteIds(checkedIds.toList())
                                checkedIds = emptySet()
                                multiSelect = false
                                showDeleteSelectedConfirm = false
                                reload()
                                Toast.makeText(context, "已删除 $n 条记录", Toast.LENGTH_SHORT).show()
                            }, contentAlignment = Alignment.Center) {
                            Text("删除", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** 左滑可操作的一行：底层三个操作按钮 + 上层内容（水平拖动露出） */
@Composable
private fun SwipeLedgerRow(
    s: Session,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val actionWidth = 76.dp
    val actions = listOf(
        SwipeAction("收藏", if (s.favorite) "★" else "☆", AppColors.Lantern, onFavorite),
        SwipeAction("复制", "⧉", AppColors.Jade, onCopy),
        SwipeAction("删除", "✕", AppColors.Ember, onDelete)
    )
    val totalPx = with(androidx.compose.ui.platform.LocalDensity.current) { (actionWidth * 3).toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF16161B))) {
        // 底层：操作按钮（右对齐三格）
        Row(Modifier.align(Alignment.CenterEnd)) {
            actions.forEach { act ->
                Column(
                    Modifier.width(actionWidth).height(76.dp).background(act.color.copy(alpha = 0.22f))
                        .clickable {
                            act.onClick()
                            scope.launch { offsetX.animateTo(0f, tween(200)) }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(act.glyph, color = act.color, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(act.label, color = act.color, fontSize = 11.sp)
                }
            }
        }
        // 上层：内容（可左滑）
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(Color(0xFF1C1C22))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                offsetX.animateTo(
                                    if (offsetX.value < -totalPx / 2) -totalPx else 0f,
                                    tween(200)
                                )
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.animateTo((offsetX.value + dragAmount).coerceIn(-totalPx, 0f))
                        }
                    }
                }
                .clickable {
                    if (offsetX.value < -1f) {
                        scope.launch { offsetX.animateTo(0f, tween(200)) }
                    } else onClick()
                }
        ) {
            LedgerRowContent(s)
        }
    }
}

private data class SwipeAction(val label: String, val glyph: String, val color: Color, val onClick: () -> Unit)

@Composable
private fun LedgerRowContent(s: Session) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, AppColors.GoldLine), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(s.badge, color = AppColors.Lantern, fontSize = 14.sp, fontFamily = FontFamily.Serif)
        }
        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.favorite) Text("★ ", color = AppColors.Lantern, fontSize = 12.sp)
                Text(s.question.ifBlank { "（无题）" }, color = AppColors.Paper, fontSize = 13.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("#${s.id} · ${s.createdAt} · ${s.mode}", color = AppColors.Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
        if (s.verdict.isNotBlank()) Text("已断", color = AppColors.Jade, fontSize = 11.sp)
        Text("›", color = AppColors.Dim, fontSize = 16.sp)
    }
}

@Composable
private fun SessionSheet(
    session: Session,
    onClose: () -> Unit,
    onAskAi: () -> Unit,
    onSave: (String) -> Unit
) {
    var verdict by remember(session.id) { mutableStateOf(session.verdict) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x88000000)).clickable(onClick = onClose))
        Column(
            Modifier.fillMaxWidth().height(560.dp).align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF191816))
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("卦局 #${session.id}", color = AppColors.Paper, fontSize = 16.sp, fontFamily = FontFamily.Serif)
                Spacer(Modifier.weight(1f))
                Text("×", color = AppColors.Muted, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onClose))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(session.face, color = AppColors.Paper, fontSize = 12.5.sp, lineHeight = 21.sp, fontFamily = FontFamily.Monospace)
                if (session.seal.isNotBlank()) {
                    Text("落款 · ${session.seal}", color = AppColors.Lantern, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 14.dp))
                }
                Spacer(Modifier.height(14.dp))
                // 问 AI 按钮：带当前卦面进入 AI 解卦对话
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color(0x66D9A441), Color(0x44C96442))))
                        .border(1.dp, AppColors.Lantern.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onAskAi).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✦", color = AppColors.Paper, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("问 AI 解此卦", color = AppColors.Paper, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("断 语 存 档", color = AppColors.Muted, fontSize = 10.5.sp, letterSpacing = 4.sp)
                OutlinedTextField(
                    value = verdict, onValueChange = { verdict = it },
                    placeholder = { Text("把 AI 或你自己的断语记在这里…", color = AppColors.Dim) },
                    minLines = 4, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                        focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                        cursorColor = AppColors.Lantern, focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x1AD9A441))
                    .clickable { onSave(verdict) }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("存 断 语", color = AppColors.Lantern, fontSize = 14.sp, letterSpacing = 3.sp)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("排盘", text))
}

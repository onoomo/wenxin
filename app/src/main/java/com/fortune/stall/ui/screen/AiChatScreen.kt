package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.data.Session
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.AiClient
import com.fortune.stall.util.AiPreset
import com.fortune.stall.util.ModelsResult
import com.fortune.stall.util.PromptPreset
import com.fortune.stall.util.AiResult
import com.fortune.stall.util.AiSettings
import com.fortune.stall.util.ChatMessage
import kotlinx.coroutines.launch

/**
 * AI 解卦对话页：当前排盘下拉 + 对话解读 / API 配置 双 Tab。
 * @param initialSession 从卦账详情「问 AI」带过来的那一卦；为空则默认最近一卦。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    db: FortuneDb,
    initialSession: Session? = null,
    directContext: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val topInset = with(density) {
        maxOf(
            WindowInsets.statusBarsIgnoringVisibility.getTop(this),
            WindowInsets.displayCutout.getTop(this)
        ).toDp()
    }
    val bottomInset = with(density) {
        maxOf(
            WindowInsets.navigationBarsIgnoringVisibility.getBottom(this),
            WindowInsets.displayCutout.getBottom(this)
        ).toDp()
    }

    var tab by remember { mutableStateOf(0) } // 0=对话解读 1=API配置
    var selected by remember { mutableStateOf(initialSession) }
    var showPicker by remember { mutableStateOf(false) }
    val sessions = remember { db.recent() }
    // 对话状态提升到 Tab 之外：切到 API 配置再切回不丢，只有退出本界面（组合销毁）才清空
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val inputState = remember { mutableStateOf("") }
    val busyState = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(topInset))
            // 顶栏：返回 + 标题
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", color = AppColors.Lantern, fontSize = 30.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.width(10.dp))
                Text("问 AI", color = AppColors.Paper, fontSize = 20.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.weight(1f))
                Text("AI 解读仅供参考", color = AppColors.Dim, fontSize = 10.5.sp)
            }

            // 当前排盘下拉
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, AppColors.GoldLine.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { showPicker = !showPicker }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("当前排盘", color = AppColors.Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                        Text(
                            selected?.let { "${it.question.ifBlank { "卦局 #${it.id}" }} · ${it.createdAt}" }
                                ?: "未选择排盘（将按通用提示词解读）",
                            color = if (selected == null) AppColors.Dim else AppColors.Paper,
                            fontSize = 12.5.sp, maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(if (showPicker) "▴" else "▾", color = AppColors.Lantern, fontSize = 14.sp)
                }
                if (showPicker) {
                    LazyColumn(
                        Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1B22))
                            .border(1.dp, AppColors.GoldLine.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        item {
                            Text("不指定排盘", color = AppColors.Muted, fontSize = 12.5.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { selected = null; showPicker = false }
                                    .padding(12.dp))
                        }
                        items(sessions) { s ->
                            Text("${s.badge}  ${s.question.ifBlank { "卦局 #${s.id}" }} · ${s.createdAt}",
                                color = if (s.id == selected?.id) AppColors.Lantern else AppColors.Paper,
                                fontSize = 12.5.sp, maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { selected = s; showPicker = false }
                                    .padding(12.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            // 双 Tab
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabChip("对话解读", tab == 0, Modifier.weight(1f)) { tab = 0 }
                TabChip("API 配置", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
            Spacer(Modifier.height(10.dp))

            if (tab == 0) {
                ChatPanel(
                    session = selected, directContext = directContext, bottomInset = bottomInset,
                    messages = messages, inputState = inputState, busyState = busyState,
                    listState = listState
                )
            } else {
                ApiConfigPanel()
            }
        }
    }
}

@Composable
private fun TabChip(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(20.dp))
            .background(if (active) Color(0x55D9A441) else Color(0x12FFFFFF))
            .border(1.dp, if (active) AppColors.Lantern else AppColors.GoldLine.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) AppColors.Paper else AppColors.Muted,
            fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, letterSpacing = 2.sp)
    }
}

/** 对话解读面板 */
@Composable
private fun ChatPanel(
    session: Session?,
    directContext: String?,
    bottomInset: androidx.compose.ui.unit.Dp,
    messages: SnapshotStateList<ChatMessage>,
    inputState: MutableState<String>,
    busyState: MutableState<Boolean>,
    listState: LazyListState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by inputState
    var busy by busyState

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        if (!AiSettings.hasKey()) {
            Toast.makeText(context, "请先在「API 配置」里填写 API Key", Toast.LENGTH_SHORT).show()
            return
        }
        input = ""
        messages.add(ChatMessage("user", text))
        busy = true
        scope.launch {
          try {
            // 组装 system 指令 + 当前排盘上下文 + 历史对话
            val sys = buildString {
                append(AiSettings.systemPrompt.value)
                if (directContext != null) {
                    if (directContext.contains("合盘分析核心框架")) {
                        // 合盘上下文已自带方法论与输出结构，不再追加 13 维解读框架
                        append("\n\n你是紫微斗数合盘专家，请严格依据下方【合盘方法论】与【输出结构要求】解读两张命盘。")
                    } else {
                        // 紫微命盘直接注入：13 维解读框架（对齐参考项目 ziwei-ds 的 13 维选项卡）
                        append("\n\n你是紫微斗数解读专家。下面是一张紫微命盘，请按 13 个维度逐一解读，每维用小标题分段：")
                        append("1.命宫（性格与格局）2.兄弟 3.夫妻 4.子女 5.财帛 6.疾厄 7.迁移 8.人际（仆役）")
                        append("9.官禄 10.田宅 11.福德 12.父母 13.综合（大限流年走势）。")
                        append("结合主星亮度、四化（禄权科忌）、自化与来因、长生十二神、三方四正与飞星，给出具体而有条理的推断。")
                    }
                    append("\n\n【解读材料】\n")
                    append(directContext.take(AiSettings.contextChars.value * 3))
                } else if (session != null) {
                    append("\n\n【当前排盘卦象】\n")
                    val face = session.face.take(AiSettings.contextChars.value)
                    append(face)
                    append("\n【落款】").append(session.seal)
                }
                append("\n\n请用简体中文作答，解读仅供参考。")
            }
            val payload = mutableListOf<ChatMessage>()
            payload.add(ChatMessage("system", sys))
            payload.addAll(messages.toList())
            val result = AiClient.chat(
                AiSettings.effectiveEndpoint(),
                AiSettings.apiKey.value,
                AiSettings.model.value,
                payload
            )
            when (result) {
                is AiResult.Ok -> messages.add(ChatMessage("assistant", result.text))
                is AiResult.Err -> messages.add(ChatMessage("assistant", "⚠ ${result.message}"))
            }
          } catch (e: Exception) {
            messages.add(ChatMessage("assistant", "⚠ 发送失败：${e.message ?: e.javaClass.simpleName}"))
          } finally {
            busy = false
          }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).imePadding()) {
        // 会话工具行：对话仅在退出本界面后清空，当前配置名 · 模型名（由 API 配置提供，切换即时更新）
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("对话保留至退出", color = AppColors.Dim, fontSize = 10.5.sp)
            Spacer(Modifier.weight(1f))
            // 读 revision 让配置切换/重命名后即时重组，模型名直接取当前激活配置
            AiSettings.revision.value
            val curCfg = AiSettings.current()
            val shownModel = curCfg?.model?.ifBlank { AiSettings.model.value } ?: AiSettings.model.value
            Text(
                curCfg?.name?.let { "$it · $shownModel" } ?: shownModel,
                color = AppColors.Lantern, fontSize = 12.sp,
                fontWeight = FontWeight.Medium, maxLines = 1
            )
        }
        Spacer(Modifier.height(8.dp))

        // 消息区
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("开始对话", color = AppColors.Paper, fontSize = 18.sp,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("输入问题，AI 将为你解读排盘", color = AppColors.Dim, fontSize = 12.5.sp)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp)
                ) {
                    items(messages) { m -> ChatBubble(m) }
                    if (busy) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                                    color = AppColors.Lantern
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("AI 正在推演…（最长约 90 秒）", color = AppColors.Muted, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        // 输入行
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp + bottomInset),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("输入问题…", color = AppColors.Dim) },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).imeFix(),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                    focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                    cursorColor = AppColors.Lantern,
                    focusedContainerColor = Color(0x0AFFFFFF), unfocusedContainerColor = Color(0x0AFFFFFF)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(50))
                    .background(if (busy) AppColors.Shop2 else AppColors.Lantern)
                    .clickable { if (!busy) send() },
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = if (busy) AppColors.Muted else AppColors.Ink,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatBubble(m: ChatMessage) {
    val isUser = m.role == "user"
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(14.dp))
                .background(
                    if (isUser) Color(0x4AC96442) else Color(0x14FFFFFF)
                )
                .border(1.dp, AppColors.GoldLine.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(horizontal = 13.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isUser) "我" else "AI", color = AppColors.Lantern, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f))
                Text("复制", color = AppColors.Lantern, fontSize = 10.sp,
                    modifier = Modifier.clickable {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("对话内容", m.content))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    })
            }
            Spacer(Modifier.height(4.dp))
            // SelectionContainer：长按可选中并复制整段解读文本
            SelectionContainer {
                Text(m.content, color = AppColors.Paper, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
    }
}

/** API 配置面板：多套配置切换 + 端点 / 密钥 / 模型 / 提示词 / 上下文长度 + 预设 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApiConfigPanel() {
    val context = LocalContext.current
    var name by remember { mutableStateOf(AiSettings.current()?.name ?: "") }
    var endpoint by remember { mutableStateOf(AiSettings.endpoint.value) }
    var apiPathField by remember { mutableStateOf(AiSettings.apiPath.value) }
    var apiKey by remember { mutableStateOf(AiSettings.apiKey.value) }
    var model by remember { mutableStateOf(AiSettings.model.value) }
    var prompt by remember { mutableStateOf(AiSettings.systemPrompt.value) }
    var ctx by remember { mutableStateOf(AiSettings.contextChars.value.toString()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPromptPicker by remember { mutableStateOf(false) }
    var showSavePromptDialog by remember { mutableStateOf(false) }
    var fetchedModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var showModelPicker by remember { mutableStateOf(false) }
    var loadingModels by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 切换配置时，把编辑区重置为该配置的值
    LaunchedEffect(AiSettings.activeId.value) {
        val c = AiSettings.current()
        name = c?.name ?: ""
        endpoint = AiSettings.endpoint.value
        apiPathField = AiSettings.apiPath.value
        apiKey = AiSettings.apiKey.value
        model = AiSettings.model.value
        prompt = AiSettings.systemPrompt.value
        ctx = AiSettings.contextChars.value.toString()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp).imePadding()) {
        Text("接入 OpenAI 兼容的对话接口，密钥只保存在本机", color = AppColors.Muted, fontSize = 11.5.sp)
        Spacer(Modifier.height(12.dp))

        // ===== 多套配置：横向切换 + 新增 + 删除 =====
        Text("配置方案（可保存多套，点击切换）", color = AppColors.Dim, fontSize = 10.5.sp)
        Spacer(Modifier.height(8.dp))
        // 读 revision 触发配置变更后的重组
        AiSettings.revision.value
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiSettings.configs.forEach { c ->
                ConfigChip(
                    label = c.name.ifBlank { "未命名" },
                    active = c.id == AiSettings.activeId.value,
                    onClick = { AiSettings.switchTo(c.id) }
                )
            }
            ConfigChip(label = "＋ 新增", active = false, highlight = true) { showAddDialog = true }
        }
        if (AiSettings.configs.size > 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                "删除当前配置「${AiSettings.current()?.name ?: ""}」",
                color = Color(0xFFE08A7A), fontSize = 11.sp,
                modifier = Modifier.clickable {
                    AiSettings.deleteConfig(AiSettings.activeId.value)
                    Toast.makeText(context, "已删除配置", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Spacer(Modifier.height(14.dp))

        AiField("配置名称", name, { name = it }, singleLine = true)
        Spacer(Modifier.height(10.dp))

        // 各大 AI 预设（接口与模型名按 2026 最新文档），点击一键填入
        Text("选择服务商一键填入接口与模型名", color = AppColors.Dim, fontSize = 10.5.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiSettings.PRESETS.forEach { p ->
                PresetChip(p.name, active = endpoint == p.endpoint) {
                    endpoint = p.endpoint
                    model = p.model
                    apiPathField = p.apiPath
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        AiField("API 地址", endpoint, { endpoint = it }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        AiField("接口路径（如 /chat/completions，留空则用上方完整地址）", apiPathField, { apiPathField = it }, singleLine = true)
        // 当前服务商的接口协议标识（如 OpenAI 兼容 = openai-completions）
        val curProtocol = AiSettings.PRESETS.firstOrNull { it.endpoint == endpoint }?.protocol.orEmpty()
        if (curProtocol.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("接口协议：$curProtocol", color = AppColors.Lantern, fontSize = 10.5.sp)
        }
        Spacer(Modifier.height(10.dp))
        AiField("API Key", apiKey, { apiKey = it }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        AiField("模型名（可手填，或读取接口列表选择）", model, { model = it }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        // 读取模型列表：需当前 API 地址 + Key 组合有效
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (loadingModels) AppColors.Shop2 else Color(0x12FFFFFF))
                    .border(1.dp, AppColors.GoldLine.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable {
                        if (loadingModels) return@clickable
                        if (endpoint.isBlank() || apiKey.isBlank()) {
                            Toast.makeText(context, "请先填写 API 地址与 Key", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        loadingModels = true
                        scope.launch {
                            val finalEp = AiSettings.buildEndpoint(endpoint, apiPathField)
                            when (val r = AiClient.fetchModels(finalEp, apiKey.trim())) {
                                is ModelsResult.Ok -> {
                                    fetchedModels = r.models
                                    showModelPicker = true
                                }
                                is ModelsResult.Err ->
                                    Toast.makeText(context, r.message, Toast.LENGTH_LONG).show()
                            }
                            loadingModels = false
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    if (loadingModels) "读取中…" else "⇩ 读取模型列表",
                    color = if (loadingModels) AppColors.Muted else AppColors.Lantern,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("需 API 地址 + Key 有效", color = AppColors.Dim, fontSize = 10.5.sp)
        }
        Spacer(Modifier.height(10.dp))
        AiField("上下文字数上限", ctx, { ctx = it }, singleLine = true, numeric = true)
        Spacer(Modifier.height(12.dp))

        // 自定义提示词：标签行右侧带「切换人设」+「确认保存」按钮
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("自定义提示词（System Prompt）", color = AppColors.Muted,
                fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AD9A441))
                    .border(1.dp, AppColors.GoldLine.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { showPromptPicker = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("⇄ 切换人设", color = AppColors.Lantern, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Lantern)
                    .clickable { showSavePromptDialog = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("✓ 确认保存", color = AppColors.Ink, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
        AiSettings.revision.value // 自定义人设增删后重组
        val currentPresetName = AiSettings.allPrompts().firstOrNull { it.text == prompt }?.name
        if (currentPresetName != null) {
            Text("当前人设：$currentPresetName", color = AppColors.Dim, fontSize = 10.5.sp)
            Spacer(Modifier.height(5.dp))
        }
        OutlinedTextField(
            value = prompt, onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp).imeFix(),
            shape = RoundedCornerShape(10.dp),
            // 输入法修复：多行文本允许正常换行（ImeAction.Default + 不强制单行），回车可换行
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                cursorColor = AppColors.Lantern,
                focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF)
            )
        )
        Spacer(Modifier.height(4.dp))
        Text("改好提示词后点「确认保存」，起个名字存入人设列表", color = AppColors.Dim, fontSize = 10.sp)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(AppColors.Lantern)
                .clickable {
                    val n = ctx.toIntOrNull()?.coerceIn(500, 20000) ?: 6000
                    AiSettings.saveActive(
                        name.trim().ifBlank { "未命名配置" },
                        endpoint.trim(), apiKey.trim(), model.trim(), prompt, n, apiPathField.trim()
                    )
                    Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 13.dp), contentAlignment = Alignment.Center
        ) {
            Text("保 存 配 置", color = AppColors.Ink, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddDialog) {
        AddConfigDialog(
            onDismiss = { showAddDialog = false },
            onAddByName = { n ->
                AiSettings.addConfig(n)
                showAddDialog = false
                Toast.makeText(context, "已新增配置「$n」", Toast.LENGTH_SHORT).show()
            },
            onAddByPreset = { p ->
                AiSettings.addConfig(p.name, p)
                showAddDialog = false
                Toast.makeText(context, "已新增配置「${p.name}」", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showModelPicker) {
        ModelPickerDialog(
            models = fetchedModels,
            current = model,
            onDismiss = { showModelPicker = false },
            onSelect = { picked ->
                model = picked
                showModelPicker = false
                Toast.makeText(context, "已选择模型：$picked（记得点保存配置）", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPromptPicker) {
        PromptPresetDialog(
            currentText = prompt,
            onDismiss = { showPromptPicker = false },
            onSelect = { picked ->
                prompt = picked.text
                showPromptPicker = false
                Toast.makeText(context, "已切换人设：${picked.name}（记得点保存配置）", Toast.LENGTH_SHORT).show()
            },
            onDeleteCustom = { nm ->
                AiSettings.deleteCustomPrompt(nm)
                Toast.makeText(context, "已删除人设：$nm", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSavePromptDialog) {
        SavePromptDialog(
            currentText = prompt,
            onDismiss = { showSavePromptDialog = false },
            onSave = { nm ->
                AiSettings.saveCustomPrompt(nm, prompt)
                showSavePromptDialog = false
                Toast.makeText(context, "人设「$nm」已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/** 确认保存人设弹窗：给当前提示词起名字，存入人设列表 */
@Composable
private fun SavePromptDialog(
    currentText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1B22))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(16.dp))
                .padding(18.dp).imePadding().verticalScroll(rememberScrollState())
        ) {
            Text("保存为人设", color = AppColors.Paper, fontSize = 16.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("给这段提示词起个名字，之后可在「切换人设」里选用",
                color = AppColors.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Text("人设名称", color = AppColors.Dim, fontSize = 10.5.sp)
            Spacer(Modifier.height(5.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("例如：我的断卦风格", color = AppColors.Dim) },
                modifier = Modifier.fillMaxWidth().imeFix(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                    focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                    cursorColor = AppColors.Lantern,
                    focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF)
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "提示词预览：${currentText.take(60).replace("\n", " ")}${if (currentText.length > 60) "…" else ""}",
                color = AppColors.Dim, fontSize = 10.5.sp, maxLines = 2
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("取消", color = AppColors.Muted, fontSize = 13.sp,
                    modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 12.dp, vertical = 6.dp))
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (name.isNotBlank()) AppColors.Lantern else Color(0x33FFFFFF))
                        .clickable { if (name.isNotBlank()) onSave(name.trim()) }
                        .padding(horizontal = 18.dp, vertical = 7.dp)
                ) {
                    Text("确认保存", color = if (name.isNotBlank()) AppColors.Ink else AppColors.Dim,
                        fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** 人设切换弹窗：列出内置 + 自定义人设，点选即填入；自定义可删除 */
@Composable
private fun PromptPresetDialog(
    currentText: String,
    onDismiss: () -> Unit,
    onSelect: (PromptPreset) -> Unit,
    onDeleteCustom: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1B22))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text("切换断卦人设", color = AppColors.Paper, fontSize = 16.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("点选后填入提示词框，保存配置即生效", color = AppColors.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiSettings.revision.value // 自定义人设增删后重组
                items(AiSettings.allPrompts()) { p ->
                    val active = p.text == currentText
                    val isCustom = AiSettings.customPrompts.any { it.name == p.name && it.text == p.text }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (active) Color(0x33D9A441) else Color(0x10FFFFFF))
                            .border(
                                1.dp,
                                if (active) AppColors.Lantern else AppColors.GoldLine.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(p) }
                            .padding(horizontal = 13.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, color = if (active) AppColors.Paper else AppColors.Lantern,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            if (active) Text("当前 ✓", color = AppColors.Lantern, fontSize = 10.5.sp)
                            if (isCustom) {
                                Spacer(Modifier.width(8.dp))
                                Text("删除", color = Color(0xFFE08A7A), fontSize = 10.5.sp,
                                    modifier = Modifier.clickable { onDeleteCustom(p.name) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            p.text,
                            color = AppColors.Muted, fontSize = 10.5.sp, lineHeight = 16.sp,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0x12FFFFFF)).clickable { onDismiss() }
                .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("关闭", color = AppColors.Muted, fontSize = 12.5.sp)
            }
        }
    }
}

/** 模型列表选择弹窗：从接口读取到的可用模型，点选后填入模型名 */
@Composable
private fun ModelPickerDialog(
    models: List<String>,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1B22))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text("可用模型（${models.size}）", color = AppColors.Paper, fontSize = 16.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("由当前 API 地址 + Key 读取，点选填入", color = AppColors.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(models) { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (m == current) Color(0x33D9A441) else Color(0x10FFFFFF))
                            .clickable { onSelect(m) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(m, color = if (m == current) AppColors.Paper else AppColors.Lantern,
                            fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                        if (m == current) Text("✓", color = AppColors.Lantern, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0x12FFFFFF)).clickable { onDismiss() }
                .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("关闭", color = AppColors.Muted, fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun ConfigChip(
    label: String,
    active: Boolean,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        active -> Color(0x55D9A441)
        highlight -> Color(0x1AD9A441)
        else -> Color(0x12FFFFFF)
    }
    Box(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                1.dp,
                if (active || highlight) AppColors.Lantern else AppColors.GoldLine.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (active || highlight) AppColors.Paper else AppColors.Lantern,
            fontSize = 11.5.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** 新增配置弹窗：可自定义名称，或从预设一键建立 */
@Composable
private fun AddConfigDialog(
    onDismiss: () -> Unit,
    onAddByName: (String) -> Unit,
    onAddByPreset: (AiPreset) -> Unit
) {
    var customName by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1B22))
                .border(1.dp, AppColors.GoldLine, RoundedCornerShape(16.dp))
                .padding(18.dp).imePadding().verticalScroll(rememberScrollState())
        ) {
            Text("新增配置", color = AppColors.Paper, fontSize = 16.sp,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("自定义名称，或从服务商预设一键建立", color = AppColors.Muted, fontSize = 11.5.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = customName, onValueChange = { customName = it },
                singleLine = true,
                placeholder = { Text("例如：工作号 DeepSeek", color = AppColors.Dim) },
                modifier = Modifier.fillMaxWidth().imeFix(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                    focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                    cursorColor = AppColors.Lantern,
                    focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF)
                )
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("取消", color = AppColors.Muted, fontSize = 13.sp,
                    modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 12.dp, vertical = 6.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "创建", color = if (customName.isBlank()) AppColors.Dim else AppColors.Lantern,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        if (customName.isNotBlank()) onAddByName(customName.trim())
                    }.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("或选择预设：", color = AppColors.Dim, fontSize = 10.5.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiSettings.PRESETS.forEach { p ->
                    PresetChip(p.name, active = false) { onAddByPreset(p) }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0x55D9A441) else Color(0x12FFFFFF))
            .border(1.dp, if (active) AppColors.Lantern else AppColors.GoldLine.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (active) AppColors.Paper else AppColors.Lantern,
            fontSize = 11.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

/**
 * 任意输入框通用输入法修复：获得焦点时自动把自身滚动到可见区域，
 * 软键盘弹出后输入框不会被遮挡。所有文本输入统一走这里。
 */
@Composable
private fun Modifier.imeFix(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(requester)
        .onFocusEvent { state ->
            if (state.isFocused) {
                scope.launch {
                    kotlinx.coroutines.delay(150)
                    requester.bringIntoView()
                }
            }
        }
}

@Composable
private fun AiField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    singleLine: Boolean,
    numeric: Boolean = false
) {
    Text(label, color = AppColors.Muted, fontSize = 11.sp, letterSpacing = 1.sp)
    Spacer(Modifier.height(5.dp))
    OutlinedTextField(
        value = value, onValueChange = onChange,
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
            .then(if (singleLine) Modifier else Modifier.heightIn(min = 130.dp))
            .imeFix(),
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
            focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
            cursorColor = AppColors.Lantern,
            focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF)
        )
    )
}

package com.fortune.stall.ui.screen

import android.content.Intent
import android.net.Uri as AndroidUri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.engine.FortuneEngine
import com.fortune.stall.engine.LiuYao
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.util.LunarCalendar
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class Mode(val id: String, val label: String, val rec: Boolean = false, val tip: String = "", val ritual: String? = null, val btn: String? = null)
data class MethodDef(val key: String, val title: String, val note: String, val modes: List<Mode>, val gender: Boolean = false)

private val METHODS = listOf(
    MethodDef("xiaoliuren", "小六壬 · 快卦", "三宫掐指，急事小事立断", listOf(
        Mode("time", "按时起卦", true, "以此刻时辰掐算——问事当下，卦在当下。"),
        Mode("lunar", "农历起卦", false, "按农历月日加时辰掐宫。"),
        Mode("numbers", "报数起卦", false, "你随口报三个数。数由你出，卦由摊上落印。"),
        Mode("dice", "掷骰起卦", false, "三枚骰子在摊上摇——落地即锁，一事一卦。", ritual = "dice", btn = "掷 骰 子")
    ), gender = true),
    MethodDef("liuyao", "六爻 · 深卦", "纳甲装卦，世应动变慢慢看", listOf(
        Mode("time", "按时起卦", true, "以此刻年月日时起卦装卦。"),
        Mode("numbers", "报数起卦", false, "报三个数：下卦、上卦、动爻。"),
        Mode("coin", "摇铜钱", false, "三枚铜钱摇六次，一爻一爻自下而上。", ritual = "coin", btn = "摇 铜 钱")
    )),
    MethodDef("tarot", "塔罗 · 心镜", "牌背是同一片星空", listOf(
        Mode("one", "单牌", false, "一张牌，照当下最要紧的那件事。"),
        Mode("three", "三牌阵", true, "过去 · 现在 · 未来，一条时间线。"),
        Mode("celtic", "凯尔特十字", false, "十张牌的大阵，按位次逐张翻。"),
        Mode("custom", "自定张数", false, "自己定抽几张。"),
        Mode("interpret", "自报牌名", false, "手上抽好的实体牌，报名字来解。逆位写 reversed:牌名。", btn = "呈 牌 解 读")
    ))
)

/** 起卦页 */
@Composable
fun CastScreen(methodKey: String, lineageName: String? = null, db: FortuneDb, onBack: () -> Unit,
               onResult: (FortuneEngine.CastOutput) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val def = METHODS.first { it.key == methodKey }
    var curMode by remember { mutableStateOf(def.modes.first { it.rec }.id) }
    var question by remember { mutableStateOf("") }

    // 参数输入
    var pA by remember { mutableStateOf("") }
    var pB by remember { mutableStateOf("") }
    var pC by remember { mutableStateOf("") }
    var hourZhi by remember { mutableStateOf(LunarCalendar.hourToZhi(LocalDateTime.now().hour)) }
    var gender by remember { mutableStateOf("") }
    var cardNames by remember { mutableStateOf("") }

    // 起卦时间（年月日时分）—— 时间/农历起卦可精确指定，时辰从中派生
    var castDateTime by remember { mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0)) }
    var showCastTime by remember { mutableStateOf(false) }
    val dtFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    // 仅"按时起卦"这类完全依赖公历时刻的模式才显示年月日时分选择器；
    // 农历起卦走"农历月日+时辰下拉"，本身已含时辰，保持原交互。
    val needsCastTime = methodKey != "tarot" && curMode == "time"

    var casting by remember { mutableStateOf(false) }
    var lockedRoll by remember { mutableStateOf<Pair<String, String>?>(null) } // kind -> question
    var output by remember { mutableStateOf<FortuneEngine.CastOutput?>(null) }

    // 仪式动画状态
    var dicePips by remember { mutableStateOf<List<Int>?>(null) }
    var coinTosses by remember { mutableStateOf<List<Int>?>(null) }
    var coinYaos by remember { mutableStateOf<List<Int>?>(null) }
    var showCoinRows by remember { mutableStateOf(0) }

    val mode = def.modes.first { it.id == curMode }

    fun resetCast() {
        output = null; dicePips = null; coinTosses = null; coinYaos = null
        showCoinRows = 0; lockedRoll = null; question = ""; pA = ""; pB = ""; pC = ""; cardNames = ""
    }

    suspend fun doCast() {
        // 时间/农历起卦用用户所选时刻（含时分）；其它模式用当前时刻
        val now = if (needsCastTime) castDateTime else LocalDateTime.now()
        if (needsCastTime) hourZhi = LunarCalendar.hourToZhi(now.hour)
        val a = pA.toIntOrNull(); val b = pB.toIntOrNull(); val c = pC.toIntOrNull()
        // 先落账拿到卦局编号（印章），再生成卦面
        val id = db.insert(
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            question = question, method = methodKey, mode = curMode,
            face = "落印中…", seal = FortuneEngine.sealLine(now)
        )
        val result = FortuneEngine.cast(
            sessionId = id, method = FortuneEngine.Method.valueOf(methodKey.uppercase()),
            mode = curMode, question = question, now = now,
            a = a, b = b, c = c, hourZhi = hourZhi, gender = gender.ifBlank { null },
            cardNames = if (curMode == "interpret") cardNames.split(Regex("\\s+")).filter { it.isNotBlank() } else null
        )
        // 回填完整卦面到卦账（卦面即印章）
        db.updateFace(id, result.face)
        output = result
        onResult(result)
    }

    suspend fun rollDice() {
        casting = true; dicePips = null; output = null
        val now = LocalDateTime.now()
        val pips = listOf(Random.nextInt(1, 7), Random.nextInt(1, 7), Random.nextInt(1, 7))
        delay(900)
        dicePips = pips
        val id = db.insert(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), question,
            methodKey, curMode, "落印中…", FortuneEngine.sealLine(now))
        val result = FortuneEngine.cast(id, FortuneEngine.Method.XIAOLIUREN, "numbers", question, now,
            a = pips[0], b = pips[1], c = pips[2], hourZhi = hourZhi, gender = gender.ifBlank { null })
        db.updateFace(id, result.face)
        lockedRoll = "dice" to question
        output = result; casting = false
        onResult(result)
    }

    suspend fun rollCoin() {
        casting = true; coinTosses = null; coinYaos = null; showCoinRows = 0; output = null
        val now = LocalDateTime.now()
        val tosses = mutableListOf<Int>(); val yaos = mutableListOf<Int>()
        repeat(6) {
            val backs = (0..2).count { Random.nextBoolean() }
            tosses.add(backs)
            yaos.add(when (backs) { 3 -> 9; 2 -> 8; 1 -> 7; else -> 6 })
        }
        coinTosses = tosses; coinYaos = yaos
        for (i in 1..6) { delay(430); showCoinRows = i }
        val id = db.insert(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), question,
            methodKey, curMode, "落印中…", FortuneEngine.sealLine(now))
        val (xia, shang, dong) = LiuYao.yaoToGua(yaos)
        val faceText = LiuYao.zhuangGua(xia, shang, dong, now)
        val seal = FortuneEngine.sealLine(now)
        val wrapped = "🎐 卦局 #$id 已落印 · $seal\n问: $question\n" + "─".repeat(24) + "\n" + faceText + "\n" + "─".repeat(24) + "\n卦面为服务端印章, 只断不改。"
        db.updateFace(id, wrapped)
        val result = FortuneEngine.CastOutput(wrapped, seal, FortuneEngine.Method.LIUYAO, "coin")
        output = result
        lockedRoll = "coin" to question
        casting = false
        onResult(result)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(52.dp))
            Text(def.title, color = AppColors.Paper, fontSize = 26.sp, fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(def.note, color = AppColors.Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))

            // 模式 chips
            FlowRow(horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                def.modes.forEach { m ->
                    val sel = m.id == curMode
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (sel) Color(0x17D9A441) else Color(0x0AFFFFFF))
                            .then(if (sel) Modifier.border(1.dp, AppColors.GoldLine, RoundedCornerShape(999.dp)) else Modifier)
                            .clickable(enabled = !casting) {
                                curMode = m.id; output = null; dicePips = null; coinTosses = null; showCoinRows = 0; lockedRoll = null
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(m.label + if (m.rec) " 荐" else "", color = if (sel) AppColors.Paper else AppColors.Muted, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // 所问何事
            FieldLabel("所问何事")
            OutlinedTextField(
                value = question, onValueChange = { question = it },
                placeholder = { Text("无事不占——先把事说清", color = AppColors.Dim) },
                minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.Paper, unfocusedTextColor = AppColors.Paper,
                    focusedBorderColor = AppColors.GoldLine, unfocusedBorderColor = AppColors.Line,
                    cursorColor = AppColors.Lantern, focusedContainerColor = Color(0x08FFFFFF), unfocusedContainerColor = Color(0x08FFFFFF)
                )
            )
            Spacer(Modifier.height(8.dp))

            // 起卦时间（年月日时分）—— 按时/农历起卦可精确指定
            if (needsCastTime) {
                FieldLabel("起卦时间（年 / 月 / 日 / 时 / 分）")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(Color(0x0AFFFFFF)).border(1.dp, AppColors.GoldLine, RoundedCornerShape(10.dp))
                            .clickable(enabled = !casting) { showCastTime = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text(castDateTime.format(dtFmt), color = AppColors.Paper,
                            fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x17D9A441))
                            .clickable(enabled = !casting) {
                                val n = LocalDateTime.now().withSecond(0).withNano(0)
                                castDateTime = n; hourZhi = LunarCalendar.hourToZhi(n.hour)
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) { Text("此刻", color = AppColors.Lantern, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(10.dp))
            }

            // 参数区
            ParamFields(def, mode, pA, { pA = it }, pB, { pB = it }, pC, { pC = it },
                hourZhi, { hourZhi = it }, gender, { gender = it },
                methodKey, cardNames, { cardNames = it }, !casting)

            Text(mode.tip, color = AppColors.Dim, fontSize = 11.sp, lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center)

            // 起卦按钮
            val btnLocked = lockedRoll != null
            Button(
                onClick = {
                    if (question.isBlank()) { Toast.makeText(context, "无事不占——先把事说清", Toast.LENGTH_SHORT).show(); return@Button }
                    if (btnLocked) { Toast.makeText(context, "骰钱落地即成印，同一事先写缘由再重摇", Toast.LENGTH_SHORT).show(); return@Button }
                    if (curMode == "dice") { scope.launch { rollDice() } }
                    else if (curMode == "coin") { scope.launch { rollCoin() } }
                    else {
                        // 参数校验
                        if (curMode == "lunar" && (pA.isBlank() || pB.isBlank())) { Toast.makeText(context, "农历月日要填全", Toast.LENGTH_SHORT).show(); return@Button }
                        if (curMode == "numbers" && (pA.isBlank() || pB.isBlank() || pC.isBlank())) { Toast.makeText(context, "三个数报齐", Toast.LENGTH_SHORT).show(); return@Button }
                        if (curMode == "interpret" && cardNames.isBlank()) { Toast.makeText(context, "先把牌名报上", Toast.LENGTH_SHORT).show(); return@Button }
                        casting = true
                        scope.launch { delay(500); doCast(); casting = false }
                    }
                },
                enabled = !casting,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC96442), disabledContainerColor = Color(0x55C96442),
                    contentColor = AppColors.Paper
                )
            ) {
                Text(
                    when {
                        casting -> "落 印 中 …"
                        btnLocked -> "已 落 印 · 点此依仪重摇"
                        mode.btn != null -> mode.btn
                        methodKey == "tarot" -> "抽 牌"
                        else -> "起 卦"
                    }, letterSpacing = 4.sp, fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            // 塔罗资料框（Gitee 玄学资料库，点击用浏览器打开 PDF）
            if (methodKey == "tarot") TarotResourceBox()

            // 仪式动画区
            RitualStage(dicePips, coinTosses, coinYaos, showCoinRows, casting, lockedRoll != null)
            Spacer(Modifier.height(8.dp))

            Spacer(Modifier.height(80.dp))
        }

        // 起卦时间选择弹窗（年月日时分，可键盘输入 + 步进）
        if (showCastTime) {
            BoardDateTimeDialog(
                initial = castDateTime,
                onDismiss = { showCastTime = false },
                onConfirm = { dt ->
                    castDateTime = dt.withSecond(0).withNano(0)
                    hourZhi = LunarCalendar.hourToZhi(dt.hour)
                    showCastTime = false
                }
            )
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, color = AppColors.Muted, fontSize = 11.5.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
}

/** 塔罗资料框：Gitee 玄学资料库中的塔罗 PDF，点击用浏览器打开。
 *  来源仓库：https://gitee.com/leopku/xuanxue */
private data class TarotDoc(val name: String, val url: String)

private val TAROT_DOCS = listOf(
    TarotDoc("78度的智慧（英豪译）", "https://gitee.com/leopku/xuanxue/raw/master/78%E5%BA%A6%E7%9A%84%E6%99%BA%E6%85%A7%EF%BC%88%E8%8B%B1%E8%B1%AA%E8%AF%91%EF%BC%89.pdf"),
    TarotDoc("其实你已经很塔罗了（图文版）", "https://gitee.com/leopku/xuanxue/raw/master/%E5%85%B6%E5%AE%9E%E4%BD%A0%E5%B7%B2%E7%BB%8F%E5%BE%88%E5%A1%94%E7%BD%97%E4%BA%86%EF%BC%88%E5%9B%BE%E6%96%87%E7%89%88%EF%BC%89.pdf"),
    TarotDoc("塔罗入门十九堂课", "https://gitee.com/leopku/xuanxue/raw/master/%E5%A1%94%E7%BD%97%E5%85%A5%E9%97%A8%E5%8D%81%E4%B9%9D%E5%A0%82%E8%AF%BE.pdf"),
    TarotDoc("塔罗全书", "https://gitee.com/leopku/xuanxue/raw/master/%E5%A1%94%E7%BD%97%E5%85%A8%E4%B9%A6.pdf"),
    TarotDoc("塔罗图钥", "https://gitee.com/leopku/xuanxue/raw/master/%E5%A1%94%E7%BD%97%E5%9B%BE%E9%92%A5.pdf"),
    TarotDoc("塔罗白皮书 - 星宿", "https://gitee.com/leopku/xuanxue/raw/master/%E5%A1%94%E7%BD%97%E7%99%BD%E7%9A%AE%E4%B9%A6%20-%20%E6%98%9F%E5%AE%BF.pdf"),
    TarotDoc("塔罗逆位精解", "https://gitee.com/leopku/xuanxue/raw/master/%E5%A1%94%E7%BD%97%E9%80%86%E4%BD%8D%E7%B2%BE%E8%A7%A3.pdf"),
    TarotDoc("直觉式塔罗", "https://gitee.com/leopku/xuanxue/raw/master/%E7%9B%B4%E8%A7%89%E5%BC%8F%E5%A1%94%E7%BD%97.pdf")
)

@Composable
private fun TarotResourceBox() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, AppColors.GoldLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text("塔 罗 资 料 库", color = AppColors.Lantern, fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("Gitee 玄学资料库 · 点击书名在浏览器打开", color = AppColors.Dim, fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp), textAlign = TextAlign.Center)
        for (doc in TAROT_DOCS) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, AndroidUri.parse(doc.url)))
                        } catch (e: Exception) {
                            Toast.makeText(context, "没有可用的浏览器", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▸", color = AppColors.Lantern, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text(doc.name, color = AppColors.Paper, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                Text("PDF", color = AppColors.Muted, fontSize = 9.sp)
            }
        }
        Text("资料来源：gitee.com/leopku/xuanxue", color = AppColors.Dim, fontSize = 9.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
    }
}

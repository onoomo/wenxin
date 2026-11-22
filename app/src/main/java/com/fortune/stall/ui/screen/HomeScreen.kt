package com.fortune.stall.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import com.fortune.stall.R
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors
import com.fortune.stall.engine.jishiyu.JishiyuRegistry
import com.fortune.stall.util.DisplaySettings

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
            // Hero
            Text(
                "问心处",
                color = AppColors.Paper,
                fontSize = 46.sp, letterSpacing = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
            )
            Text(
                "自家夜市的算卦小摊 · 卦面即印章",
                color = AppColors.Muted, fontSize = 12.5.sp, letterSpacing = 1.sp
            )
            Spacer(Modifier.height(28.dp))

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

            if (DisplaySettings.iconMode.value) {
                // 图标模式：每行固定 4 个，末行不足用空位补齐，保持每个磁贴尺寸一致
                val icons = HOME_TILES.map { IconTile1(it.label, it.key, it.glyph) }
                var idx = 0
                while (idx < icons.size) {
                    val row = mutableListOf<IconTile1>()
                    repeat(4) { if (idx < icons.size) { row.add(icons[idx]); idx++ } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 始终占满 4 个等宽槽位，最后一个不会被撑成整行大卡
                        repeat(4) { slot ->
                            Box(Modifier.weight(1f)) {
                                if (slot < row.size) {
                                    val item = row[slot]
                                    IconTileNew(item) { openTile(item.key, item.label) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                // 详细模式：20 项全部列出，每项带眉标与详细说明
                for (t in HOME_TILES) {
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
                    modifier = Modifier.padding(top = 7.dp, bottom = 6.dp))
                Text(desc, color = AppColors.Muted, fontSize = 12.5.sp, lineHeight = 22.sp)
            }
            Text("›", color = AppColors.Dim, fontSize = 22.sp)
        }
    }
}

/** 首页 20 项模块清单：名称 + 目标 key + 图标 + 眉标 + 详细说明（图标/详细两种模式共用） */
private data class HomeTile(
    val label: String, val key: String, val glyph: Glyph,
    val eyebrow: String, val desc: String
)

private val HOME_TILES = listOf(
    HomeTile("八字排盘", "bazi-paipan", Glyph.BAZI, "四 柱 命 理",
        "按出生年月日时排定四柱干支，标注十神、藏干、纳音与神煞，起大运流年，断一生格局与起伏。"),
    HomeTile("命理奇门", "mingliqimen", Glyph.MINGLI, "终 身 奇 门",
        "以生辰起终身奇门盘，配大运流年逐限推移，看一生富贵吉凶与关键时刻的天地人三才格局。"),
    HomeTile("阴盘奇门", "yinpanqimen", Glyph.YINPAN, "阴 盘 起 局",
        "阴盘奇门时盘起局，布三奇六仪、八神九星八门，隐干、马星、空亡一并排出，用于占断当下事体。"),
    HomeTile("奇门遁甲", "qimen", Glyph.QIMEN, "九 宫 时 盘",
        "按时辰起局，拆补、置闰、茅山定阴阳遁局数，值符值使、天盘地盘九宫齐备，问事择方的经典术数。"),
    HomeTile("紫微斗数", "ziwei", Glyph.ZIWEI, "十 八 飞 星",
        "以生辰安命身宫，布十四主星、辅弼魁钺与四化，十二宫逐格推演，细看性情、事业、姻缘与财运。"),
    HomeTile("六爻", "liuyao", Glyph.LIUYAO, "纳 甲 装 卦",
        "铜钱摇卦、纳甲装卦，六亲六神、世应动变、伏神神煞俱全，大事慢问，一爻一爻看事体成败。"),
    HomeTile("梅花易数", "meihua-time", Glyph.MEIHUA, "观 象 起 卦",
        "时间、报数、手动三种起卦，取上下卦与动爻，推本卦、互卦、变卦、错综卦，以体用生克断吉凶。"),
    HomeTile("大六壬", "daliuren", Glyph.DALIUREN, "三 式 之 首",
        "四课三传、天盘月将、贵人六亲与十二宫行年，占事时辰可灵活设定，断事精细，被誉为三式之首。"),
    HomeTile("潜虚筮法", "qianxu", Glyph.QIANXU, "七 十 演 策",
        "取七十策虚其五，分二挂一揲十定名，得五十五名之一；再揲以求变位，按阴阳取显幽，断吉凶臧否。"),
    HomeTile("塔罗牌占卜", "tarot", Glyph.TAROT, "心 象 之 镜",
        "七十八张牌照心象，单张、三牌或凯尔特十字多种牌阵，随问随翻，照见当下心境与走向。"),
    HomeTile("小六壬", "xiaoliuren", Glyph.XIAOLIUREN, "掐 指 快 卦",
        "掐指三宫，问急事小事。按时起课或摇骰报数，一掐即得答案，是随身携带的快捷占法。"),
    HomeTile("戚都翁", "qiduweng", Glyph.QIDU, "未 卜 先 知",
        "以时间起课，定体用星、月令旺衰与五行类象，断来人办事的成败迟速，属古法未卜先知。"),
    HomeTile("奇门穿壬", "chuanren", Glyph.CHUANREN, "奇 壬 合 参",
        "奇门九宫与大六壬贵神、月将、年煞、命宫同盘合参，二式互证，占断更为周全。"),
    HomeTile("太乙三式", "taiyi3shi", Glyph.TAIYI, "三 式 合 一",
        "太乙积数定局，主客算与十六神齐备，合奇门、六壬三盘为一，推演天时国运与大事格局。"),
    HomeTile("山向奇门", "shanxiangqimen", Glyph.SHANXIANG, "二 十 四 山",
        "按罗盘度数查七十二局，以年柱代日、五鼠遁定时，布阳阴遁九宫盘，专用于立向择方。"),
    HomeTile("玄空飞星", "ninestar", Glyph.FEIXING, "洛 书 飞 泊",
        "年、月、日、时四颗飞星入九宫，飞泊洛书配九星吉凶，可结合经度校正真太阳时，看方位旺衰。"),
    HomeTile("达摩一掌经", "yizhangjing", Glyph.YIZHANG, "六 道 轮 回",
        "以生辰年月日时地支落宫，查六道与神煞，断一生因果、性情与早年晚景，一掌之中见轮回。"),
    HomeTile("万年历", "wannianli", Glyph.WANNIANLI, "公 农 对 照",
        "整月日历逐日列出农历、干支、节气与生肖，点选任一天即可查当日老黄历，日期查询的入口。"),
    HomeTile("老黄历", "laohuangli", Glyph.LAOHUANGLI, "择 日 经 典",
        "宜忌、冲煞、值神建星、二十八宿、纳音、吉神凶煞、彭祖百忌与财喜福神方位，择日办事的参照。"),
    HomeTile("八字合婚", "hehun", Glyph.HEHUN, "天 作 之 合",
        "按双方八字比对生肖、纳音、婚宫、用神、十神、桃花等十二项，逐条给出结论与合婚等级。"),
    HomeTile("手机号预测", "shuzi", Glyph.PHONE, "磁 场 数 理",
        "手机号八星磁场分析，延年、天医、生气为吉，六煞、五鬼、祸害、绝命为凶，逐位解读尾数与象义。")
)

/** 图标模式条目：名称 + 目标 key（空=暂无模块）+ 图标类型 */
private data class IconTile1(val label: String, val key: String, val glyph: Glyph)

/** 图1的 20 类线性图标 */
private enum class Glyph {
    BAZI, MINGLI, YINPAN, QIMEN, ZIWEI, LIUYAO, MEIHUA, DALIUREN, XIAOLIUREN,
    QIDU, CHUANREN, TAIYI, SHANXIANG, FEIXING, YIZHANG, WANNIANLI, LAOHUANGLI, HEHUN, PHONE, TAROT, QIANXU
}

/** 原版 ji.js.cn 的 SVG 图标资源（1:1 移植） */
private fun glyphRes(g: Glyph): Int = when (g) {
    Glyph.BAZI -> R.drawable.ic_g_bazi
    Glyph.MINGLI -> R.drawable.ic_g_mingli
    Glyph.YINPAN -> R.drawable.ic_g_yinpan
    Glyph.QIMEN -> R.drawable.ic_g_qimen
    Glyph.ZIWEI -> R.drawable.ic_g_ziwei
    Glyph.LIUYAO -> R.drawable.ic_g_liuyao
    Glyph.MEIHUA -> R.drawable.ic_g_meihua
    Glyph.DALIUREN -> R.drawable.ic_g_daliuren
    Glyph.XIAOLIUREN -> R.drawable.ic_g_xiaoliuren
    Glyph.QIDU -> R.drawable.ic_g_qidu
    Glyph.CHUANREN -> R.drawable.ic_g_chuanren
    Glyph.TAIYI -> R.drawable.ic_g_taiyi
    Glyph.SHANXIANG -> R.drawable.ic_g_shanxiang
    Glyph.FEIXING -> R.drawable.ic_g_feixing
    Glyph.YIZHANG -> R.drawable.ic_g_yizhang
    Glyph.WANNIANLI -> R.drawable.ic_g_wannianli
    Glyph.LAOHUANGLI -> R.drawable.ic_g_laohuangli
    Glyph.HEHUN -> R.drawable.ic_g_hehun
    Glyph.PHONE -> R.drawable.ic_g_phone
    Glyph.TAROT -> R.drawable.ic_g_tarot
    Glyph.QIANXU -> R.drawable.ic_g_qianxu
}

/** 单个图标磁贴：100% 透明卡片 + 余烬红圆角方块 + 白色线性图标 + 浅色名称（融入深空背景） */
@Composable
private fun IconTileNew(item: IconTile1, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
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
        Text(item.label, color = AppColors.Paper, fontSize = 12.sp,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 9.dp))
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
        Glyph.TAIYI -> { // 太乙三式：三圆相叠
            drawCircle(white, radius = w * 0.19f, center = Offset(w * 0.38f, h * 0.4f), style = stroke)
            drawCircle(white, radius = w * 0.19f, center = Offset(w * 0.62f, h * 0.4f), style = stroke)
            drawCircle(white, radius = w * 0.19f, center = Offset(w * 0.5f, h * 0.64f), style = stroke)
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

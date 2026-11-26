package com.fortune.stall

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fortune.stall.data.FortuneDb
import com.fortune.stall.data.Session
import com.fortune.stall.data.SkillRepository
import com.fortune.stall.engine.FortuneEngine
import com.fortune.stall.util.AiBridge
import com.fortune.stall.ui.screen.LaoHuangLiScreen
import com.fortune.stall.ui.screen.WanNianLiScreen
import com.fortune.stall.ui.screen.ArchiveScreen
import com.fortune.stall.ui.screen.CastScreen
import com.fortune.stall.ui.screen.CastResultScreen
import com.fortune.stall.ui.screen.HomeScreen
import com.fortune.stall.ui.screen.AiChatScreen
import com.fortune.stall.ui.screen.DaoJiaLiuRenScreen
import com.fortune.stall.ui.screen.BaziCastScreen
import com.fortune.stall.ui.screen.JiangScreen
import com.fortune.stall.ui.screen.JishiyuCastScreen
import com.fortune.stall.ui.screen.JishiyuHubScreen
import com.fortune.stall.ui.screen.LedgerScreen
import com.fortune.stall.ui.screen.LineageScreen
import com.fortune.stall.ui.screen.Lineage
import com.fortune.stall.ui.screen.LiuyaoCastScreen
import com.fortune.stall.ui.screen.ShanxiangCastScreen
import com.fortune.stall.ui.screen.FeixingCastScreen
import com.fortune.stall.ui.screen.YizhangCastScreen
import com.fortune.stall.ui.screen.QiduwengCastScreen
import com.fortune.stall.ui.screen.CompositeCastScreen
import com.fortune.stall.ui.screen.LiuRenCastScreen
import com.fortune.stall.ui.screen.MeiHuaCastScreen
import com.fortune.stall.ui.screen.ZiweiCastScreen
import com.fortune.stall.ui.screen.ZiweiHemingScreen
import com.fortune.stall.ui.screen.LenormandScreen
import com.fortune.stall.ui.screen.YinpanQimenCastScreen
import com.fortune.stall.ui.screen.MingliQimenCastScreen
import com.fortune.stall.ui.screen.QianxuScreen
import com.fortune.stall.ui.screen.SettingsScreen
import com.fortune.stall.ui.screen.SkillsScreen
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.FortuneTheme
import com.fortune.stall.util.AiSettings
import com.fortune.stall.util.DisplaySettings
import com.fortune.stall.util.HomeTilesSettings
import com.fortune.stall.util.Immersive
import com.fortune.stall.util.TarotSettings
import com.fortune.stall.util.ZiweiSettings

class MainActivity : ComponentActivity() {
    // 当前页面是否需要显示系统导航栏（仅首页全屏隐藏，其余页面都显示）
    private var showNavBar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全屏沉浸：隐藏状态栏/导航栏 + 挖孔屏适配（API 24-36）
        Immersive.enter(this)

        val db = FortuneDb(this)
        val skillRepo = SkillRepository(this)
        TarotSettings.init(this)
        DisplaySettings.init(this)
        HomeTilesSettings.init(this)
        AiSettings.init(this)
        ZiweiSettings.init(this)

        setContent {
            FortuneTheme {
                AppNavigation(db = db, skillRepo = skillRepo, onNavBarMode = { showNavBar = it })
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 部分机型从后台回来会重置系统栏：按当前页面需要恢复
        if (hasFocus) {
            if (showNavBar) Immersive.showNavigationBar(this) else Immersive.enter(this)
        }
    }
}

private enum class Route { HOME, LINEAGE, CAST, CAST_RESULT, JIANG, DAOJIA_LIUREN, BAZI, JISHUYU_HUB, JISHUYU_CAST, LIUYAO_PAI, WANNIANLI, LAOHUANGLI, SHANXIANG, FEIXING, YIZHANG, QIDUWENG, CHUANREN, DALIUREN, MEIHUA, YINPAN, MINGLI, ZIWEI, HEMING, QIANXU, LENORMAND, ARCHIVE, LEDGER, FAVORITES, SETTINGS, SKILLS, AI_CHAT }

@Composable
private fun AppNavigation(
    db: FortuneDb,
    skillRepo: SkillRepository,
    onNavBarMode: (Boolean) -> Unit
) {
    // 用导航栈管理层级：主页 -> 存档 -> 卦账、主页 -> 设置 -> 技能包
    var stack by remember { mutableStateOf(listOf(Route.HOME)) }
    val route = stack.last()
    var castMethod by remember { mutableStateOf("xiaoliuren") }
    var lineageName by remember { mutableStateOf<String?>(null) }
    var jishiyuKey by remember { mutableStateOf("") }
    var castResult by remember { mutableStateOf<FortuneEngine.CastOutput?>(null) }
    var aiSession by remember { mutableStateOf<Session?>(null) }
    var aiDirectContext by remember { mutableStateOf<String?>(null) }

    // 仅首页（图1宫格页）保持全屏沉浸隐藏系统导航栏，其余所有页面都显示底部导航条。
    // 进入非首页 show，回到首页恢复隐藏；并同步给 Activity 供切后台回来时恢复。
    val context = LocalContext.current
    LaunchedEffect(route) {
        val needNav = route != Route.HOME
        onNavBarMode(needNav)
        val activity = context as? Activity
        if (activity != null) {
            if (needNav) Immersive.showNavigationBar(activity) else Immersive.enter(activity)
        }
    }

    fun go(r: Route) { stack = stack + r }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }

    // 所有术法结果页通过 AiBridge 携带完整盘面进入 AI 解读（无需逐页传参）
    AiBridge.askAi = { ctx -> aiDirectContext = ctx; aiSession = null; go(Route.AI_CHAT) }
    // 术数模块统一入口：万年历/老黄历走专用日历页，其余走通用排盘页
    fun openJishiyuModule(key: String) {
        jishiyuKey = key
        when (key) {
            "wannianli" -> go(Route.WANNIANLI)
            "laohuangli" -> go(Route.LAOHUANGLI)
            "shanxiangqimen" -> go(Route.SHANXIANG)
            "ninestar" -> go(Route.FEIXING)
            "yizhangjing" -> go(Route.YIZHANG)
            "qiduweng" -> go(Route.QIDUWENG)
            "chuanren" -> go(Route.CHUANREN)
            "taiyi3shi", "taiyi72", "taiyishenshu" -> go(Route.JISHUYU_HUB)
            "bazi-paipan", "bazi-analysis", "bazi" -> go(Route.BAZI)
            "daliuren" -> go(Route.DALIUREN)
            "meihua", "meihua-time", "meihua-num" -> go(Route.MEIHUA)
            "yinpanqimen" -> go(Route.YINPAN)
            "mingliqimen" -> go(Route.MINGLI)
            "ziwei" -> go(Route.ZIWEI)
            "qianxu" -> go(Route.QIANXU)
            "lenormand" -> go(Route.LENORMAND)
            else -> go(Route.JISHUYU_CAST)
        }
    }
    // 用 r 替换当前栈顶（不新增层级）：传承页选完传承后进起卦页，
    // 栈从 [HOME, LINEAGE] → [HOME, CAST]，这样从起卦页返回一次就直接回主页。
    fun replaceTop(r: Route) { stack = stack.dropLast(1) + r }

    BackHandler(enabled = stack.size > 1) { back() }

    // 底部弹窗形态的起卦页：底层铺星空（与首页一致），让透明区透出背景而非黑底，
    // 视觉上成为浮在星空上的弹窗。结果/命盘页本身是不透明全屏，会自然盖住这层星空。
    val sheetRoutes = setOf(
        Route.SHANXIANG, Route.FEIXING, Route.YIZHANG, Route.QIDUWENG,
        Route.CHUANREN, Route.DALIUREN, Route.MEIHUA,
        Route.YINPAN, Route.MINGLI, Route.ZIWEI, Route.HEMING,
        Route.LENORMAND, Route.JISHUYU_CAST
    )
    Box(Modifier.fillMaxSize()) {
    if (route in sheetRoutes) {
        Starfield(Modifier.fillMaxSize())
    }
    when (route) {
        Route.HOME -> HomeScreen(
            onOpenCast = { key ->
                castMethod = key
                if (key == "xiaoliuren" || key == "liuyao") {
                    // 小六壬 / 六爻：先选传承
                    lineageName = null
                    go(Route.LINEAGE)
                } else {
                    // 塔罗：直接进起卦页
                    lineageName = null
                    go(Route.CAST)
                }
            },
            onOpenArchive = { go(Route.ARCHIVE) },
            onOpenSettings = { go(Route.SETTINGS) },
            onOpenJishiyu = { go(Route.JISHUYU_HUB) },
            onOpenJishiyuModule = { key -> openJishiyuModule(key) }
        )
        Route.LINEAGE -> LineageScreen(
            methodKey = castMethod,
            methodTitle = if (castMethod == "xiaoliuren") "小六壬 · 快卦" else "六爻 · 深卦",
            onBack = { back() },
            onPick = { lin ->
                lineageName = lin.title
                when (lin.key) {
                    "daojia" -> replaceTop(Route.DAOJIA_LIUREN)
                    "jiang" -> replaceTop(Route.JIANG)
                    "liuyupaipan" -> replaceTop(Route.LIUYAO_PAI)
                    else -> replaceTop(Route.CAST)
                }
            }
        )
        Route.JIANG -> JiangScreen(db = db)
        Route.DAOJIA_LIUREN -> DaoJiaLiuRenScreen(db = db, onBack = { back() })
        Route.BAZI -> BaziCastScreen(db = db, onBack = { back() })
        Route.LIUYAO_PAI -> LiuyaoCastScreen(db = db, onBack = { back() })
        Route.JISHUYU_HUB -> JishiyuHubScreen(
            onOpen = { key -> openJishiyuModule(key) },
            onBack = { back() }
        )
        Route.JISHUYU_CAST -> JishiyuCastScreen(moduleKey = jishiyuKey, db = db, onBack = { back() })
        Route.WANNIANLI -> WanNianLiScreen(onBack = { back() })
        Route.LAOHUANGLI -> LaoHuangLiScreen(onBack = { back() })
        Route.SHANXIANG -> ShanxiangCastScreen(db = db, onBack = { back() })
        Route.FEIXING -> FeixingCastScreen(db = db, onBack = { back() })
        Route.YIZHANG -> YizhangCastScreen(db = db, onBack = { back() })
        Route.QIDUWENG -> QiduwengCastScreen(db = db, onBack = { back() })
        Route.CHUANREN -> CompositeCastScreen(db = db, kind = "chuanren", onBack = { back() })
        Route.DALIUREN -> LiuRenCastScreen(db = db, onBack = { back() })
        Route.MEIHUA -> MeiHuaCastScreen(db = db, onBack = { back() })
        Route.YINPAN -> YinpanQimenCastScreen(db = db, onBack = { back() })
        Route.MINGLI -> MingliQimenCastScreen(db = db, onBack = { back() })
        Route.ZIWEI -> ZiweiCastScreen(db = db, onBack = { back() },
            onAskAi = { ctx -> aiDirectContext = ctx; aiSession = null; go(Route.AI_CHAT) },
            onHeming = { go(Route.HEMING) })
        Route.HEMING -> ZiweiHemingScreen(
            onBack = { back() },
            onAskAi = { ctx -> aiDirectContext = ctx; aiSession = null; go(Route.AI_CHAT) })
        Route.QIANXU -> QianxuScreen(db = db, onBack = { back() })
        Route.LENORMAND -> LenormandScreen(db = db, onBack = { back() })
        Route.CAST -> CastScreen(
            methodKey = castMethod, lineageName = lineageName, db = db, onBack = { back() },
            onResult = { out -> castResult = out; go(Route.CAST_RESULT) }
        )
        Route.CAST_RESULT -> castResult?.let { out ->
            CastResultScreen(
                output = out,
                onBack = { back() },
                onAgain = { castResult = null; back() }
            )
        } ?: androidx.compose.material3.Text("无结果", color = androidx.compose.ui.graphics.Color.White)
        Route.ARCHIVE -> ArchiveScreen(
            onBack = { back() }, onOpenLedger = { go(Route.LEDGER) },
            onOpenFavorites = { go(Route.FAVORITES) }, favoriteCount = db.favoriteCount()
        )
        Route.LEDGER -> LedgerScreen(db = db, onBack = { back() },
            onAskAi = { s -> aiSession = s; aiDirectContext = null; go(Route.AI_CHAT) })
        Route.FAVORITES -> LedgerScreen(db = db, onBack = { back() }, onlyFavorites = true,
            onAskAi = { s -> aiSession = s; aiDirectContext = null; go(Route.AI_CHAT) })
        Route.SETTINGS -> SettingsScreen(
            onBack = { back() }, onOpenSkills = { go(Route.SKILLS) },
            onOpenAi = { aiSession = null; aiDirectContext = null; go(Route.AI_CHAT) },
            onManageTiles = {
                HomeTilesSettings.editMode.value = true
                back()
            }
        )
        Route.SKILLS -> SkillsScreen(repo = skillRepo, onBack = { back() })
        Route.AI_CHAT -> AiChatScreen(db = db, initialSession = aiSession,
            directContext = aiDirectContext, onBack = { back() })
    }
    }
}

package com.fortune.stall.engine

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * 道家小六壬 · 六宫排盘引擎
 *
 * 移植自「读爻观吉 1.8.0」（com.drone.nofeerty）的 XiaoLiuRenEngine。
 * 道家一脉起法：「大安起月，月上起日，日上起时」——
 * 从大安(宫1)顺数农历月得月宫，再从月宫顺数农历日得日宫，再从日宫顺数时辰序得时宫。
 * 六宫各配地支、天干、五星、六神、六亲；时宫为「自身」，余宫按地支五行生克定六亲。
 */
object DaoJiaLiuRen {

    /** 六宫基础定义：id / 宫名 / code / 断语摘要 / 五行 */
    data class PalaceSeed(val id: Int, val code: String, val name: String, val summary: String, val element: String)

    val PALACES = listOf(
        PalaceSeed(1, "NO.1", "大安", "事宜守成，先稳后进。", "木"),
        PalaceSeed(2, "NO.2", "留连", "进展偏慢，易有牵绊。", "土"),
        PalaceSeed(3, "NO.3", "速喜", "有喜讯，事多速应。", "火"),
        PalaceSeed(4, "NO.4", "赤口", "主口舌是非，宜慎言。", "金"),
        PalaceSeed(5, "NO.5", "小吉", "缓中见利，可得小成。", "水"),
        PalaceSeed(6, "NO.6", "空亡", "事象未实，宜静观。", "土")
    )

    /** 九宫式网格显示顺序（原版 FixedGridDisplayOrder）*/
    val GRID_ORDER = listOf(2, 3, 4, 1, 6, 5)

    /** 每宫的判词知识：歌诀 + 总论 + 感情/事业/财运/健康 + 应期 + 方位 */
    data class Knowledge(
        val song: String, val general: String, val love: String, val career: String,
        val wealth: String, val health: String, val timing: String, val direction: String
    )

    val KNOWLEDGE = mapOf(
        1 to Knowledge(
            "大安事事昌，求谋在东方，失物去不远，宅舍保安康。",
            "大安为六壬之首，属木，主生发、安定。此宫代表事情平稳发展，适合守成而非冒进。求财可得，但不宜急躁。出行平安，诉讼和解。",
            "感情稳定，单身者有望遇到踏实可靠的对象。已有伴侣者关系和谐，适合谈婚论嫁。宜循序渐进。",
            "工作稳定，适合维持现状。不宜跳槽或做重大变动。领导赏识，同事和睦。创业者宜稳固根基。",
            "财运平稳，正财可期。不宜投机，宜稳健理财。东方求财有利，春季财运最佳。",
            "身体状况良好，注意肝胆保养。宜多运动，保持心情舒畅。忌熬夜伤肝。",
            "应在卯时（5-7点）或寅时（3-5点）。应在春季，三日内应验。",
            "吉利方位为东方。出行宜往东方，求财亦然。"),
        2 to Knowledge(
            "留连事难成，求谋日未明，官事只宜缓，去者未回程。",
            "留连属土，主迟滞、拖延。此宫代表事情进展缓慢，容易反复。不宜急进，宜等待时机。失物难寻，出行延误。",
            "感情上有拖延之象，单身者桃花运弱，需耐心等待。恋爱中的人可能面临冷战或沟通不畅。",
            "工作进展缓慢，项目容易延期。不宜做重大决策，宜等待更好的时机。",
            "财运不佳，投资容易被套。催债困难，宜节流，减少不必要开支。",
            "注意脾胃消化系统问题。容易感到疲倦乏力。宜规律饮食，少食生冷。",
            "应在辰时（7-9点）或丑时（1-3点）。应在月末或季末，拖延后方应。",
            "中央方位为宜。不宜远行，宜静守。"),
        3 to Knowledge(
            "速喜喜来临，求财向南行，失物申未午，逢人路上寻。",
            "速喜属火，主快速、喜庆。此宫代表好消息即将到来，事情发展迅速。求财有利，出行顺利，诉讼胜诉。",
            "桃花运旺，单身者很快会遇到心仪对象。恋爱中的人感情升温，适合表白求婚。",
            "工作顺利，有升职加薪机会。项目进展迅速，得到领导认可。创业者生意兴隆。",
            "财运极佳，正财偏财皆旺。南方求财最利，夏季财运巅峰。投资可获丰厚回报。",
            "精力充沛，状态良好。注意心脏和血液循环。忌暴饮暴食，忌大喜大悲。",
            "应在午时（11-13点）或巳时（9-11点）。应在三日内，速速应验。",
            "吉利方位为南方。出行宜往南方，面试谈判亦然。"),
        4 to Knowledge(
            "赤口主口舌，官非切要防，失物速速找，行人有惊慌。",
            "赤口属金，主口舌是非、争执。此宫代表容易与人发生争执，需谨言慎行。出行有阻碍，诉讼不利。",
            "感情上容易发生争吵，单身者桃花带刺。恋爱中的人要注意沟通方式，避免言语伤害。",
            "工作中容易与同事或客户发生矛盾。不宜谈判签约，容易产生纠纷。注意合同条款。",
            "因口舌破财，容易因争执损失金钱。投资需谨慎，防止被忽悠。西方求财不利。",
            "注意呼吸系统和口腔问题。容易上火、喉咙痛。宜清淡饮食，多喝水。",
            "应在酉时（17-19点）或申时（15-17点）。应在秋季或金旺之日。",
            "西方不利，宜避开。出行注意安全，防止意外。"),
        5 to Knowledge(
            "小吉最吉昌，路上好商量，阴人来报喜，失物在坤方。",
            "小吉属水，主吉利、顺利。此宫代表事情最终会有一个好的结果，但需要耐心。贵人相助，逢凶化吉。",
            "感情运势良好，单身者通过朋友介绍有望脱单。恋爱中的人感情稳定，适合见家长。",
            "工作上有贵人相助，遇到困难会有人帮忙。适合合作项目，团队协作顺利。",
            "财运不错，有意外之财。北方求财有利，冬季财运最佳。适合长线投资。",
            "身体状况良好，注意肾脏和泌尿系统。宜多喝水，保持充足睡眠。",
            "应在亥时（21-23点）或子时（23-1点）。应在冬季或水旺之日。",
            "吉利方位为北方。出行宜往北方，求合作亦然。"),
        6 to Knowledge(
            "空亡事不祥，阴人多乖张，求财无利益，行人有灾殃。",
            "空亡属土，主空虚、落空。此宫代表事情容易落空，不宜做重大决定。失物难寻，出行不利。但逢空则静，适合修身养性。",
            "感情上容易落空，单身者桃花运弱，遇到的人可能不靠谱。已婚者注意第三者介入。",
            "工作上容易徒劳无功，付出多回报少。不宜跳槽或创业，容易失败。适合学习充电。",
            "财运低迷，投资容易亏损。不宜借贷，容易有去无回。宜守不宜攻，减少开支。",
            "身体容易出问题，注意脾胃和免疫系统。精神状态不佳，宜静养。",
            "应在戌时（19-21点）或未时（13-15点）。应在土旺之日或月末。",
            "中央方位不利。不宜远行，宜在家静修。")
    )

    /** 一宫的排盘结果 */
    data class PalaceInfo(
        val id: Int, val code: String, val name: String, val summary: String,
        val earthlyBranch: String, val heavenlyStem: String, val element: String,
        val fiveStar: String, val sixGod: String, val sixKin: String,
        val tags: List<String>, val isResult: Boolean
    )

    /** 完整盘面 */
    data class Chart(
        val monthPalace: Int, val dayPalace: Int, val hourPalace: Int,
        val resultName: String, val resultSummary: String,
        val palaces: List<PalaceInfo>,
        val solarDisplay: String, val lunarDisplay: String, val ganzhiDisplay: String,
        val shichenDisplay: String, val siZhuDisplay: String, val kongWang: String,
        val yearGanZhi: String, val monthGanZhi: String, val dayGanZhi: String, val hourGanZhi: String,
        val kongWangFour: List<String>
    )

    private val DIZHI = LunarCalendar.DIZHI
    private val GANZHI60: List<String> = run {
        val gan = LunarCalendar.TIANGAN; val zhi = LunarCalendar.DIZHI
        (0 until 60).map { gan[it % 10].toString() + zhi[it % 12] }
    }

    private fun advance(start: Int, steps: Int): Int = ((start - 1 + steps - 1) % 6 + 6) % 6 + 1

    private val WUXING_OF_BRANCH = mapOf(
        "子" to "水", "丑" to "土", "寅" to "木", "卯" to "木", "辰" to "土", "巳" to "火",
        "午" to "火", "未" to "土", "申" to "金", "酉" to "金", "戌" to "土", "亥" to "水"
    )
    private val WUXING_OF_STEM = mapOf(
        "甲" to "木", "乙" to "木", "丙" to "火", "丁" to "火", "戊" to "土",
        "己" to "土", "庚" to "金", "辛" to "金", "壬" to "水", "癸" to "水"
    )
    private val SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")

    /** 六神（按地支的日六神法）*/
    private fun sixGodByBranch(branch: String): String = when (branch) {
        "寅", "卯" -> "青龙"
        "巳", "午" -> "朱雀"
        "丑", "辰" -> "勾陈"
        "未", "戌" -> "螣蛇"
        "申", "酉" -> "白虎"
        "子", "亥" -> "玄武"
        else -> ""
    }

    /** 六亲：以自身地支五行为基准，看目标宫地支五行 */
    private fun sixKin(selfBranch: String, target: String): String {
        val base = WUXING_OF_BRANCH[selfBranch] ?: return ""
        val t = WUXING_OF_BRANCH[target] ?: return ""
        return when {
            base == t -> "兄弟"
            SHENG[base] == t -> "子孙"
            KE[base] == t -> "妻财"
            SHENG[t] == base -> "父母"
            KE[t] == base -> "官鬼"
            else -> "兄弟"
        }
    }

    /** 地支落宫：时支所属的阳支组/阴支组，从时宫顺次落六宫 */
    private fun assignBranches(hourPalace: Int, hourBranch: String): Map<Int, String> {
        var group = listOf("子", "寅", "辰", "午", "申", "戌")
        if (hourBranch !in group) group = listOf("丑", "卯", "巳", "未", "酉", "亥")
        val idx = group.indexOf(hourBranch).coerceAtLeast(0)
        val map = linkedMapOf<Int, String>()
        for (i in 0 until 6) map[((hourPalace - 1 + i) % 6) + 1] = group[(idx + i) % 6]
        return map
    }

    /** 五星落宫：从日宫起 木火土金水天空 */
    private fun assignStars(dayPalace: Int): Map<Int, String> {
        val stars = listOf("木星", "火星", "土星", "金星", "水星", "天空")
        val map = linkedMapOf<Int, String>()
        for (i in 0 until 6) map[((dayPalace - 1 + i) % 6) + 1] = stars[i]
        return map
    }

    /** 天干落宫：取日柱所在旬的十组干支，建立 地支→天干 映射 */
    private fun assignStems(dayGanZhi: String, branches: Map<Int, String>): Map<Int, String> {
        val idx = GANZHI60.indexOf(dayGanZhi)
        if (idx < 0) return branches.mapValues { "" }
        val xunStart = (idx / 10) * 10
        val branchToStem = (0 until 10).associate { i ->
            val gz = GANZHI60[xunStart + i]
            gz[1].toString() to gz[0].toString()
        }
        return branches.mapValues { branchToStem[it.value] ?: "" }
    }

    /** 旬空（按日柱）*/
    private fun kongWang(ganzhi: String): String {
        val idx = GANZHI60.indexOf(ganzhi)
        if (idx < 0) return ""
        val shou = GANZHI60[(idx / 10) * 10]
        return mapOf("甲子" to "戌亥", "甲戌" to "申酉", "甲申" to "午未",
            "甲午" to "辰巳", "甲辰" to "寅卯", "甲寅" to "子丑")[shou] ?: ""
    }

    /** 时辰序数：子=1,丑=2 ... 由地支决定 */
    private fun shiChenIndex(hour: Int, lunarTimeZhi: String): Int {
        val i = DIZHI.indexOf(lunarTimeZhi)
        return if (i >= 0) i + 1 else ((hour + 1) / 2)
    }

    /**
     * 排盘主入口（道家流派：大安起月，月上起日，日上起时）
     */
    fun cast(dt: LocalDateTime): Chart {
        val lun = LunarCalendar.fromDateTime(dt)
        val lunarMonth = abs(lun.month)
        val lunarDay = lun.day
        val timeZhi = lun.hourZhi
        val hourIdx = shiChenIndex(dt.hour, timeZhi)

        // 道家起宫：月宫→日宫→时宫
        val monthPalace = advance(1, lunarMonth)
        val dayPalace = advance(monthPalace, lunarDay)
        val hourPalace = advance(dayPalace, hourIdx)

        val branches = assignBranches(hourPalace, timeZhi)
        val stars = assignStars(dayPalace)
        val stems = assignStems(lun.dayGanZhi, branches)
        val selfBranch = branches[hourPalace] ?: ""

        val palaces = PALACES.map { seed ->
            val br = branches[seed.id] ?: ""
            val tags = mutableListOf<String>()
            if (seed.id == monthPalace) tags.add("月")
            if (seed.id == dayPalace) tags.add("日")
            if (seed.id == hourPalace) tags.add("时")
            PalaceInfo(
                id = seed.id, code = seed.code, name = seed.name, summary = seed.summary,
                earthlyBranch = br,
                heavenlyStem = stems[seed.id] ?: "",
                element = WUXING_OF_BRANCH[br] ?: seed.element,
                fiveStar = stars[seed.id] ?: "",
                sixGod = sixGodByBranch(br),
                sixKin = if (seed.id == hourPalace) "自身" else sixKin(selfBranch, br),
                tags = tags, isResult = seed.id == hourPalace
            )
        }

        val result = PALACES.first { it.id == hourPalace }
        val solarText = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 " +
            String.format("%02d:%02d", dt.hour, dt.minute)
        val lunarMonthCn = LunarCalendar.cnNumber(lunarMonth)
        val lunarDayCn = LunarCalendar.cnNumber(lunarDay)
        val lunarText = "农历${lunarMonthCn}月${lunarDayCn}日"
        val kwFour = listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi).map { kongWang(it) }

        return Chart(
            monthPalace = monthPalace, dayPalace = dayPalace, hourPalace = hourPalace,
            resultName = result.name, resultSummary = result.summary,
            palaces = palaces,
            solarDisplay = solarText, lunarDisplay = lunarText,
            ganzhiDisplay = "${lun.yearGanZhi}年 ${lun.monthGanZhi}月 ${lun.dayGanZhi}日 ${lun.hourGanZhi}时",
            shichenDisplay = "${timeZhi}时",
            siZhuDisplay = "年柱${lun.yearGanZhi} 月柱${lun.monthGanZhi} 日柱${lun.dayGanZhi} 时柱${lun.hourGanZhi}",
            kongWang = "旬空" + kongWang(lun.dayGanZhi),
            kongWangFour = kwFour,
            yearGanZhi = lun.yearGanZhi, monthGanZhi = lun.monthGanZhi,
            dayGanZhi = lun.dayGanZhi, hourGanZhi = lun.hourGanZhi
        )
    }

    /** 生成完整排盘文本（供复制/问AI） */
    fun faceText(c: Chart): String = buildString {
        appendLine("【道家小六壬 · 六宫排盘】")
        appendLine("起卦：${c.solarDisplay}（${c.lunarDisplay} ${c.shichenDisplay}）")
        appendLine("四柱：年${c.yearGanZhi} 月${c.monthGanZhi} 日${c.dayGanZhi} 时${c.hourGanZhi}")
        appendLine("空亡：年${c.kongWangFour.getOrElse(0) { "" }} 月${c.kongWangFour.getOrElse(1) { "" }} 日${c.kongWangFour.getOrElse(2) { "" }} 时${c.kongWangFour.getOrElse(3) { "" }}")
        appendLine("落宫：月→${palaceName(c.monthPalace)}  日→${palaceName(c.dayPalace)}  时→${palaceName(c.hourPalace)}")
        appendLine("时宫结果：${c.resultName} —— ${c.resultSummary}")
        appendLine()
        appendLine("【六宫盘面】")
        for (id in GRID_ORDER) {
            val p = c.palaces.first { it.id == id }
            val tg = listOfNotNull(
                p.tags.joinToString("") .ifEmpty { null },
                p.heavenlyStem + p.earthlyBranch, p.sixKin, p.sixGod, p.fiveStar
            ).joinToString(" ")
            appendLine("${p.name}（${p.element}）：$tg")
        }
        appendLine()
        appendLine("【断语】")
        for (id in GRID_ORDER) {
            val k = KNOWLEDGE[id] ?: continue
            appendLine("${palaceName(id)}：${k.song}")
        }
    }

    fun palaceName(id: Int): String = PALACES.firstOrNull { it.id == id }?.name ?: ""
}

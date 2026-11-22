package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 九星（玄空飞星）—— 1:1 移植自吉时雨 ninestar.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 年/月/日/时飞星 + 九宫飞泊。真太阳时可选（按经度校正）。
 */
object NineStar {
    val GONG_NAME = arrayOf("中", "乾", "兑", "艮", "离", "坎", "坤", "震", "巽")
    val START_NUMBER = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val START_CODES = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
    val START_NAMES = arrayOf("一白", "二黑", "三碧", "四绿", "五黄", "六白", "七赤", "八白", "九紫")
    private val YUEJIAN = arrayOf("寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥", "子", "丑")
    private val DIZHI = LunarCalendar.DIZHI
    private val JIAZI = arrayOf(
        "甲子", "乙丑", "丙寅", "丁卯", "戊辰", "己巳", "庚午", "辛未", "壬申", "癸酉",
        "甲戌", "乙亥", "丙子", "丁丑", "戊寅", "己卯", "庚辰", "辛巳", "壬午", "癸未",
        "甲申", "乙酉", "丙戌", "丁亥", "戊子", "己丑", "庚寅", "辛卯", "壬辰", "癸巳",
        "甲午", "乙未", "丙申", "丁酉", "戊戌", "己亥", "庚子", "辛丑", "壬寅", "癸卯",
        "甲辰", "乙巳", "丙午", "丁未", "戊申", "己酉", "庚戌", "辛亥", "壬子", "癸丑",
        "甲寅", "乙卯", "丙辰", "丁巳", "戊午", "己未", "庚申", "辛酉", "壬戌", "癸亥")

    val HELPER = mapOf(
        "一白" to "贪狼星。吉星，五行属水。得令主官升、名气、功名与财运；失令为桃花劫，破财损家。",
        "二黑" to "巨门星。凶星，五行属土，代表病符。失令为极大凶星，主疾病绝症、破财横祸，与五黄并列最凶。",
        "三碧" to "禄存星。凶星，五行属木，代表是非。得令利律师、法官；失令主是非官非、破财招刑。",
        "四绿" to "文曲星。吉星，五行属木。得令主文化艺术、才华、文思敏捷；失令为桃花劫，招酒色之祸。",
        "五黄" to "廉贞星。凶星，五行属土。得令位极尊崇；失令称五黄煞，主死亡绝症、血光之灾、家破人亡。",
        "六白" to "武曲星。吉星，五行属金，偏财星。与一白、八白合称三大财星。得令丁财两旺，失令倾家荡产。",
        "七赤" to "破军星。凶星，五行属金。当运利口才工作者、通讯传播；退运主口舌是非、刀光剑影、火险肺疾。",
        "八白" to "左辅星。吉星，五行属土。得令为太白财星，主功名富贵、田宅科发，为九星中第一吉星。",
        "九紫" to "右弼星。吉星，五行属火。当令为喜庆星、爱情星，主桃花人缘、天乙贵人；失令主损丁破财、火灾。")

    data class StarInfo(val index: Int, val name: String, val code: String)

    /** 年飞星 */
    fun getYearStar(year: Int): Int {
        val last2 = Math.floorMod(year, 100)
        fun digitSum(n: Int): Int = n.toString().sumOf { it - '0' }
        val idx: Int
        if (year < 2000) {
            var s = digitSum(last2)
            if (s > 10) s = digitSum(s)
            var i = Math.floorMod(10 - s, 9)
            if (i == 0) i = 9
            idx = i
        } else {
            var s = digitSum(last2)
            if (s > 9) s = digitSum(s)
            var i = Math.floorMod(9 - s, 9)
            if (i == 0) i = 9
            idx = i
        }
        return START_NUMBER[idx - 1]
    }

    /** 月飞星：年支分组 + 农历月 */
    fun getMonthStar(yearZhi: String, lunarMonth: Int): Int {
        val start = when (yearZhi) {
            "子", "午", "卯", "酉" -> 8
            "寅", "申", "巳", "亥" -> 2
            "辰", "戌", "丑", "未" -> 5
            else -> 8
        }
        val starIndex = Math.floorMod(start - lunarMonth, 9)
        return START_NUMBER[starIndex]
    }

    /** 时飞星：season 1=夏至后(逆) 2=冬至后(顺) */
    fun getHourStar(season: Int, dayZhi: String, hourZhi: String): Int {
        val isReversed = season == 1
        val group = when (dayZhi) {
            "子", "午", "卯", "酉" -> 0
            "辰", "戌", "丑", "未" -> 1
            else -> 2
        }
        val startStar = when (group) {
            0 -> if (isReversed) 9 else 1
            1 -> if (isReversed) 6 else 4
            else -> if (isReversed) 3 else 7
        }
        val startIndex = START_NUMBER.indexOf(startStar)
        val offset = DIZHI.indexOf(hourZhi)
        val starIndex = if (isReversed) Math.floorMod(startIndex - offset, 9) else Math.floorMod(startIndex + offset, 9)
        return START_NUMBER[starIndex]
    }

    private fun jiaziDay(date: LocalDate): LocalDate {
        val gz = LunarCalendar.dayGanZhi(date)
        val zhi = gz[1].toString()
        val distance = Math.floorMod(0 - DIZHI.indexOf(zhi), 12)
        return date.plusDays(distance.toLong())
    }

    private fun ganzhiNumber(given: Int, riZhu: String, direction: Int): Int {
        val distance = JIAZI.indexOf(riZhu)
        return if (direction == 1) (given + distance - 1) % 9 + 1
        else Math.floorMod(given - distance - 1, 9) + 1
    }

    /** 日飞星：依节气区间取起始数并顺/逆飞 */
    fun getDayStar(dt: LocalDateTime, riZhu: String): Int {
        val y = dt.year
        val dongzhi0 = SolarTerm.termTime(y - 1, "冬至")?.toLocalDate() ?: return 1
        val yushui = SolarTerm.termTime(y, "雨水")?.toLocalDate() ?: return 1
        val guyu = SolarTerm.termTime(y, "谷雨")?.toLocalDate() ?: return 1
        val xiazhi = SolarTerm.termTime(y, "夏至")?.toLocalDate() ?: return 1
        val chushu = SolarTerm.termTime(y, "处暑")?.toLocalDate() ?: return 1
        val shuangjiang = SolarTerm.termTime(y, "霜降")?.toLocalDate() ?: return 1
        val dongzhi1 = SolarTerm.termTime(y, "冬至")?.toLocalDate() ?: return 1

        val a = jiaziDay(dongzhi0); val b = jiaziDay(yushui); val c = jiaziDay(guyu)
        val d = jiaziDay(xiazhi); val e = jiaziDay(chushu); val f = jiaziDay(shuangjiang); val g = jiaziDay(dongzhi1)
        val day = dt.toLocalDate()
        return when {
            day.isAfter(a) && day.isBefore(b) -> ganzhiNumber(1, riZhu, 1)
            day.isAfter(b) && day.isBefore(c) -> ganzhiNumber(7, riZhu, 1)
            day.isAfter(c) && day.isBefore(d) -> ganzhiNumber(4, riZhu, 1)
            day.isAfter(d) && day.isBefore(e) -> ganzhiNumber(9, riZhu, -1)
            day.isAfter(e) && day.isBefore(f) -> ganzhiNumber(3, riZhu, -1)
            day.isAfter(f) && day.isBefore(g) -> ganzhiNumber(6, riZhu, -1)
            else -> ganzhiNumber(1, riZhu, 1)
        }
    }

    /** 分宫飞星：返回 宫名->星数 */
    fun assignStars(center: Int, clockwise: Boolean): Map<String, Int> {
        val result = linkedMapOf<String, Int>()
        for (i in GONG_NAME.indices) {
            val star = if (clockwise) (center + i - 1) % 9 + 1 else Math.floorMod(center - i - 1, 9) + 1
            result[GONG_NAME[i]] = star
        }
        return result
    }

    private fun info(star: Int) = StarInfo(star, START_NAMES[star - 1], START_CODES[star - 1])

    /**
     * 排盘。longitude 非空时按其做真太阳时经度校正(每度4分钟)；null 则用钟表时。
     */
    fun cast(dtInput: LocalDateTime, longitude: Double? = null): CastResult {
        val dt = if (longitude != null) dtInput.plusMinutes(((longitude - 120.0) * 4.0).toLong()) else dtInput
        val lun = LunarCalendar.fromDateTime(dt)
        val yearGanZhi = lun.yearGanZhi; val yearZhi = yearGanZhi[1].toString()
        val monthZhi = lun.monthGanZhi[1].toString()
        val riZhu = lun.dayGanZhi
        val hourZhi = lun.hourZhi

        val xiazhi = SolarTerm.termTime(dt.year, "夏至")
        val dongzhi = SolarTerm.termTime(dt.year, "冬至")
        val lichun = SolarTerm.termTime(dt.year, "立春")
        val isXiazhi = xiazhi != null && dongzhi != null && dt.isAfter(xiazhi) && dt.isBefore(dongzhi)
        val isLichun = lichun != null && dt.isAfter(lichun)

        val yearStar = getYearStar(dt.year - if (isLichun) 0 else 1)
        val lunarMonth = YUEJIAN.indexOf(monthZhi) + 1
        val monthStar = getMonthStar(yearZhi, lunarMonth)
        val dayStar = getDayStar(dt, riZhu)
        val hourStar = getHourStar(if (isXiazhi) 1 else 2, riZhu[1].toString(), hourZhi)

        val ys = assignStars(yearStar, true)
        val ms = assignStars(monthStar, true)
        val ds = assignStars(dayStar, !isXiazhi)
        val hs = assignStars(hourStar, !isXiazhi)

        // 视觉洛书九宫（上南下北）
        val visual = arrayOf(
            arrayOf("巽", "离", "坤"),
            arrayOf("震", "中", "兑"),
            arrayOf("艮", "坎", "乾"))
        val table = visual.map { row ->
            row.map { g -> "${g}宫\n年${START_CODES[ys[g]!! - 1]} 月${START_CODES[ms[g]!! - 1]} 日${START_CODES[ds[g]!! - 1]} 时${START_CODES[hs[g]!! - 1]}" }
        }

        val gongRows = GONG_NAME.map { g ->
            val y = info(ys[g]!!); val m = info(ms[g]!!); val d = info(ds[g]!!); val h = info(hs[g]!!)
            "${g}宫" to "年:${y.name} 月:${m.name} 日:${d.name} 时:${h.name}"
        }

        val dateText = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时${dt.minute}分" +
            "(农历${lun.month}月${lun.day}日)" + if (longitude != null) "(真太阳)" else ""
        val jieqi = SolarTerm.rangeAt(dt)?.let { "${it.from.name} ~ ${it.to.name}" } ?: ""

        val secs = mutableListOf<CastSection>()
        secs.add(CastSection("基本信息", rows = listOf(
            "时间" to dateText,
            "四柱" to "$yearGanZhi ${lun.monthGanZhi} $riZhu ${lun.hourGanZhi}",
            "节气" to jieqi,
            "阴阳" to if (isXiazhi) "夏至后(阳遁)" else "冬至后(阴遁)"
        ), highlighted = true))
        secs.add(CastSection("中宫飞星", rows = listOf(
            "年星" to START_NAMES[yearStar - 1], "月星" to START_NAMES[monthStar - 1],
            "日星" to START_NAMES[dayStar - 1], "时星" to START_NAMES[hourStar - 1])))
        secs.add(CastSection("九宫飞泊(洛书图)", table = table))
        secs.add(CastSection("各宫飞星", rows = gongRows))
        val helperLines = START_NAMES.mapIndexed { i, n -> "【$n】${HELPER[n] ?: ""}" }
        secs.add(CastSection("九星含义", paragraphs = helperLines))

        return CastResult("九 星 飞 泊", subtitle = dateText, sections = secs)
    }

    /** 仿原版详情页所需的结构化飞星数据 */
    data class PanData(
        val dateText: String, val jieqi: String, val yearYun: String,
        val lunarText: String,                   // 农历中文，如「十月十四」
        val siZhu: List<String>,
        val gongStars: Map<String, IntArray>,   // 宫名 -> [年,月,日,时] 星数
        val yearStar: Int, val monthStar: Int, val dayStar: Int, val hourStar: Int,
        val isXiazhi: Boolean
    )

    private val CN_NUM = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    private fun cnMonth(m: Int): String = when (m) {
        1 -> "正"; 11 -> "冬"; 12 -> "腊"; 10 -> "十"
        else -> CN_NUM[m]
    } + "月"
    private fun cnDay(d: Int): String = when {
        d <= 10 -> "初" + CN_NUM[d]
        d < 20 -> "十" + CN_NUM[d - 10]
        d == 20 -> "二十"
        d < 30 -> "廿" + CN_NUM[d - 20]
        else -> "三十"
    }

    /** 三元九运：1~9 运，返回如「下元八运」 */
    fun yearYun(year: Int): String {
        var yun = Math.floorDiv(year - 1864, 20) + 1
        while (yun > 9) yun -= 9
        while (yun < 1) yun += 9
        val yuan = when ((yun - 1) / 3) { 0 -> "上元"; 1 -> "中元"; else -> "下元" }
        val cn = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九")[yun - 1]
        return "$yuan${cn}运"
    }

    /** 结构化排盘（供专用详情页用） */
    fun panData(dtInput: LocalDateTime, longitude: Double? = null): PanData {
        val dt = if (longitude != null) dtInput.plusMinutes(((longitude - 120.0) * 4.0).toLong()) else dtInput
        val lun = LunarCalendar.fromDateTime(dt)
        val yearGanZhi = lun.yearGanZhi; val yearZhi = yearGanZhi[1].toString()
        val monthZhi = lun.monthGanZhi[1].toString()
        val riZhu = lun.dayGanZhi
        val hourZhi = lun.hourZhi

        val xiazhi = SolarTerm.termTime(dt.year, "夏至")
        val dongzhi = SolarTerm.termTime(dt.year, "冬至")
        val lichun = SolarTerm.termTime(dt.year, "立春")
        val isXiazhi = xiazhi != null && dongzhi != null && dt.isAfter(xiazhi) && dt.isBefore(dongzhi)
        val isLichun = lichun != null && dt.isAfter(lichun)

        val yearStar = getYearStar(dt.year - if (isLichun) 0 else 1)
        val lunarMonth = YUEJIAN.indexOf(monthZhi) + 1
        val monthStar = getMonthStar(yearZhi, lunarMonth)
        val dayStar = getDayStar(dt, riZhu)
        val hourStar = getHourStar(if (isXiazhi) 1 else 2, riZhu[1].toString(), hourZhi)

        val ys = assignStars(yearStar, true)
        val ms = assignStars(monthStar, true)
        val ds = assignStars(dayStar, !isXiazhi)
        val hs = assignStars(hourStar, !isXiazhi)
        val gongStars = linkedMapOf<String, IntArray>()
        for (g in GONG_NAME) gongStars[g] = intArrayOf(ys[g]!!, ms[g]!!, ds[g]!!, hs[g]!!)

        val dateText = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时${dt.minute}分"
        val jieqi = SolarTerm.rangeAt(dt)?.let { "${it.from.name} ~ ${it.to.name}" } ?: ""
        val lunarText = cnMonth(lun.month) + cnDay(lun.day) + if (lun.isLeap) "(闰)" else ""
        return PanData(dateText, jieqi, yearYun(dt.year), lunarText,
            listOf(yearGanZhi, lun.monthGanZhi, riZhu, lun.hourGanZhi),
            gongStars, yearStar, monthStar, dayStar, hourStar, isXiazhi)
    }
}

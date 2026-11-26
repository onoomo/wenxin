package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDate

/**
 * 紫微运限选择器数据源 —— 对照 mingyu (Brhiza/mingyu) 的
 * packages/core/src/ziwei/fortune-options.ts + fortune-timeline.ts + iztro/decadal.ts。
 *
 * 四级联动：大限 → 流年 → 流月 → 流日。
 * 关键：虚岁分界按农历年（1岁=出生日，其余=农历正月初一），而非公历直移；
 *       流年干支=农历年干支，流月干支=五虎遁，流日干支=日干支(2000-01-07 甲子日基准)。
 */
object ZiweiFortune {

    /** 童限/大限选项：label=童限/大限，age 区间，dateStr=按农历年位移的起始公历日 */
    data class Period(
        val kind: String,        // "童限" | "大限"
        val label: String,       // 展示标题
        val startAge: Int,
        val endAge: Int,
        val dateStr: String,     // yyyy-MM-dd 起始日
        val palaceName: String = "",
        val zhi: String = "",    // 该运限对应宫位地支：大限=本命宫；童限=对应流年地支（连线焦点）
        val gan: String = ""     // 该运限天干：大限=宫干；童限=流年干（四化飞星用）
    )

    /** 流年/流月/流日/流时通用选项 */
    data class Option(
        val head: String,        // 流年=年份, 流月=「N月」, 流日=日, 流时=时辰
        val sub: String,         // 流年=「N岁」, 流月/流日=MM/DD, 流时=MM/DD
        val age: Int,            // 虚岁（流年用）
        val dateStr: String,     // 起日
        val ganZhi: String,      // 两字干支
        val endDateStr: String = "",  // 止日（流年/流月区间；流日为空）
        val display: String = "" // 参考图显示文案：流月=正月/二月，流日=初一/初二，流时=子时/丑时；空则用 head
    )

    private val GAN = LunarCalendar.TIANGAN
    private val ZHI = LunarCalendar.DIZHI
    private val FLOW_MONTH_BRANCHES = listOf("寅","卯","辰","巳","午","未","申","酉","戌","亥","子","丑")
    private val CN_NUM = arrayOf("零","一","二","三","四","五","六","七","八","九","十",
        "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十",
        "廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十")
    private val CN_MONTH = arrayOf("","正","二","三","四","五","六","七","八","九","十","十一","腊")
    private val HOUR_NAMES = arrayOf("子时","丑时","寅时","卯时","辰时","巳时","午时","未时","申时","酉时","戌时","亥时")

    /** 中文日：1→初一 ... 10→初十 11→十一 21→廿一 30→三十（参考图流日行口径） */
    private fun cnDay(d: Int): String = when {
        d <= 0 -> d.toString()
        d <= 10 -> "初${CN_NUM[d]}"
        d < CN_NUM.size -> CN_NUM[d]
        else -> "${d}日"  // 公历31日等越界回退
    }

    private fun fmt(d: LocalDate) = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
    private fun md(dateStr: String): String {
        val p = dateStr.split("-")
        return if (p.size >= 3) "${p[1]}/${p[2]}" else dateStr
    }
    private fun solar(dateStr: String): LocalDate {
        val p = dateStr.split("-")
        return LocalDate.of(p[0].toInt(), p[1].toInt(), p[2].toInt())
    }

    /** 大限行的年龄区间（去重排序），对齐 collectRegularDecadalRanges */
    private fun regularRanges(r: Ziwei.Result): List<Pair<Int, Int>> =
        r.palaces.map { it.decadal }.distinct().sortedBy { it.first }

    /**
     * 生成童限 + 大限选项（对齐 buildDecadalTimelineOptions）：
     * - 童限：1 .. (首个大限起始-1)，每年一项，dateStr=虚岁分界日(1岁=出生日，其余=农历正月初一)
     * - 大限：每个 decadal 区间一项，dateStr=起始虚岁的分界日
     */
    fun buildPeriods(r: Ziwei.Result): List<Period> {
        val birth = r.solarDate
        val ranges = regularRanges(r)
        val out = mutableListOf<Period>()
        val firstStart = ranges.firstOrNull()?.first ?: 6
        if (firstStart > 1) {
            for (age in 1 until firstStart) {
                val ds = LunarCalendar.ageBoundaryDate(birth, age)
                // 童限焦点宫 = 该虚岁农历年的流年地支/天干（点童限时连线与飞星随之移动）
                val yearGz = LunarCalendar.yearGanZhiOfSolar(solar(ds))
                out.add(Period("童限", "童限", age, age, ds, "", yearGz.takeLast(1), yearGz.take(1)))
            }
        }
        ranges.forEach { (s, e) ->
            val p = r.palaces.firstOrNull { it.decadal == (s to e) }
            out.add(Period("大限", "大限", s, e, LunarCalendar.ageBoundaryDate(birth, s), p?.name ?: "", p?.zhi ?: "", p?.gan ?: ""))
        }
        return out
    }

    /** 虚岁：目标农历年 - 出生农历年 + 1（对齐 calculateNormalZiweiNominalAge） */
    private fun nominalAgeAt(r: Ziwei.Result, dateStr: String): Int {
        val target = LunarCalendar.fromDateTime(java.time.LocalDateTime.of(solar(dateStr), java.time.LocalTime.NOON)).year
        val birth = LunarCalendar.fromDateTime(java.time.LocalDateTime.of(solar(r.solarDate), java.time.LocalTime.NOON)).year
        return target - birth + 1
    }

    /**
     * 选中童限/大限后生成其覆盖的流年（对齐 buildZiweiFortuneOptions 的 yearOptions）：
     * 每个虚岁一项，dateStr=虚岁分界日，year 取 dateStr 公历年，
     * 干支=该日农历年干支。
     */
    fun buildYears(r: Ziwei.Result, period: Period): List<Option> {
        val out = mutableListOf<Option>()
        for (age in period.startAge..period.endAge) {
            val dateStr = LunarCalendar.ageBoundaryDate(r.solarDate, age)
            val year = dateStr.substringBefore("-").toIntOrNull() ?: continue
            val gz = LunarCalendar.yearGanZhiOfSolar(solar(dateStr))
            out.add(Option("$year", "${age}岁", age, dateStr, gz))
        }
        return out
    }

    /** 五虎遁：农历年干 + 月序(1=寅月) → 月干支 */
    private fun monthGanZhi(yearGanZhi: String, monthNo: Int): String {
        val yIdx = GAN.indexOf(yearGanZhi.firstOrNull().toString()).coerceAtLeast(0)
        val yinGanStart = (yIdx % 5) * 2 + 2   // 甲己→丙寅(2), 乙庚→戊寅(4), 丙辛→庚寅(6), 丁壬→壬寅(8), 戊癸→甲寅(0)
        val gan = GAN[(yinGanStart + monthNo - 1) % 10]
        val zhi = FLOW_MONTH_BRANCHES[(monthNo - 1) % 12]
        return gan + zhi
    }

    /**
     * 选中流年后生成该流年的十二流月（对齐 buildZiweiFlowMonths，normal 口径）：
     * 以该流年农历年的正月初一为界；月序 1..12，dateStr=农历月初一公历，
     * 干支=五虎遁月干支。
     */
    fun buildMonths(r: Ziwei.Result, year: Option): List<Option> {
        val lunarYear = LunarCalendar.fromDateTime(
            java.time.LocalDateTime.of(solar(year.dateStr), java.time.LocalTime.NOON)
        ).year
        val yearGz = LunarCalendar.ganZhi(lunarYear - 4)
        val out = mutableListOf<Option>()
        for (m in 1..12) {
            val first = LunarCalendar.lunarToSolar(lunarYear, m, 1)
            val nextFirst = if (m < 12) LunarCalendar.lunarToSolar(lunarYear, m + 1, 1)
                            else LunarCalendar.lunarToSolar(lunarYear + 1, 1, 1)
            val end = nextFirst.minusDays(1)
            out.add(Option("${m}月", md(fmt(first)), year.age, fmt(first), monthGanZhi(yearGz, m), fmt(end),
                display = "${CN_MONTH[m]}月"))
        }
        return out
    }

    /** 选中流月后生成该月逐日（对齐 dayOptions）：从农历月初一公历日到月末，干支=日干支 */
    fun buildDays(r: Ziwei.Result, month: Option): List<Option> {
        val start = solar(month.dateStr)
        val end = if (month.endDateStr.isNotBlank()) solar(month.endDateStr)
                  else start.withDayOfMonth(start.lengthOfMonth())
        val out = mutableListOf<Option>()
        var date = start
        var lunarDaySeq = 1  // 起点=农历初一，逐日递增即农历日序（参考图流日初一/初二...）
        while (!date.isAfter(end)) {
            val ds = fmt(date)
            out.add(Option("${date.dayOfMonth}", md(ds), month.age, ds, LunarCalendar.dayGanZhi(date),
                display = cnDay(lunarDaySeq)))
            date = date.plusDays(1)
            lunarDaySeq++
        }
        return out
    }

    /**
     * 选中流日后生成该日十二流时（参考文墨天机底部「流时」行）：
     * 子时…亥时，干支以日干起时（甲己日子时=甲子，乙庚=丙子…戊癸=壬子），
     * 与 LunarCalendar.fromDateTime 的时干支口径一致。
     */
    fun buildHours(r: Ziwei.Result, day: Option): List<Option> {
        val dayGz = LunarCalendar.dayGanZhi(solar(day.dateStr))
        val dayGanIdx = GAN.indexOf(dayGz.firstOrNull().toString()).coerceAtLeast(0)
        val ziGanStart = (dayGanIdx % 5) * 2  // 甲己→甲子(0), 乙庚→丙子(2), 丙辛→戊子(4), 丁壬→庚子(6), 戊癸→壬子(8)
        val out = mutableListOf<Option>()
        for (i in 0..11) {
            // 时支：子(0)..亥(11)；时干：子时起 ziGanStart，逐时 +1
            val gan = GAN[(ziGanStart + i) % 10]
            val zhi = ZHI[i]
            out.add(Option(HOUR_NAMES[i], day.sub, day.age, day.dateStr, gan + zhi,
                display = HOUR_NAMES[i]))
        }
        return out
    }
}

package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 老黄历引擎 —— 1:1 移植自 jishiyu 的 lunar.js（Lunar 类黄历部分）。
 * 数据表见 AlmanacData.kt（由原版 LunarUtil 导出）。
 */
object LunarAlmanac {

    data class AlmanacDay(
        val solarText: String,        // 公历 2026年11月21日 星期六
        val lunarText: String,        // 农历 十月十三
        val baziText: String,         // 丙午年 己亥月 己亥日 己巳时
        val yearGz: String, val monthGz: String, val dayGz: String, val timeGz: String,
        val jieQi: String,            // 节气（若当日为节气）
        val yi: List<String>,         // 宜
        val ji: List<String>,         // 忌
        val chongSha: String,         // 冲蛇 煞西
        val naYin: String,            // 纳音
        val zhiShen: String,          // 值神：天德
        val zhiShenType: String,      // 黄道/黑道
        val zhiXing: String,          // 建星：建
        val xiu: String,              // 二十八宿
        val xiuLuck: String,          // 宿吉凶
        val animal: String,           // 宿兽
        val caiShen: String,          // 财神
        val xiShen: String,           // 喜神
        val fuShen: String,           // 福神
        val yangGui: String,          // 阳贵
        val taiShen: String,          // 太岁
        val jiShen: List<String>,     // 吉神
        val xiongSha: List<String>,   // 凶煞
        val pengZu: String,           // 彭祖百忌
        val dayZhi: String,
        val dayGanIndex: Int,
        val dayZhiIndex: Int,
        val monthZhiIndex: Int,
        // —— 以下为日历页面(万年历/老黄历)展示所需 ——
        val solarYear: Int, val solarMonth: Int, val solarDay: Int,
        val lunarMonth: Int, val lunarDay: Int, val isLeap: Boolean,
        val weekCn: String,
        val festivals: List<String>,   // 节日(农历/公历/周/节气/道教), 已按优先级排序
        val taiShenText: String        // 今日胎神(完整描述)
    )

    private fun hex(n: Int): String = n.toString(16).uppercase().padStart(2, '0')
    private fun jiaZiIndex(gz: String): Int = AlmanacData.JIA_ZI.indexOf(gz)

    /** 时辰地支（沿用项目工具）：23-1 子 */
    private fun hourZhiIndex(hour: Int): Int = ((hour + 1) / 2) % 12

    /**
     * 节气月干支（原版 getMonthInGanZhi）：以节气（立春/惊蛰/...）为月界。
     * 月支由当前节气区间决定：冬至后为子月、小寒后为丑月……按十二节（月首）推算。
     * 月干用五虎遁：甲己丙寅、乙庚戊寅、丙辛庚寅、丁壬壬寅、戊癸甲寅。
     */
    fun solarMonthGanzhi(dt: LocalDateTime): Pair<String, Int> {
        val zhiSeq = arrayOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
        // 十二"节"（月首）及其进入的地支月：小寒→丑, 立春→寅, 惊蛰→卯, 清明→辰...
        val jie = arrayOf(
            "小寒" to 1, "立春" to 2, "惊蛰" to 3, "清明" to 4, "立夏" to 5, "芒种" to 6,
            "小暑" to 7, "立秋" to 8, "白露" to 9, "寒露" to 10, "立冬" to 11, "大雪" to 0
        )
        // 找 <= dt 当天的最近一个"节"，取其月支。
        // 原版 lunar.js getMonthInGanZhi 用「日」精度（toYmd 比较）：交接当天整天算新月，不看具体时刻。
        var best: LocalDate? = null
        var monthZhiIdx = 1
        val today = dt.toLocalDate()
        for (yy in (dt.year - 1)..(dt.year + 1)) {
            for ((name, zhi) in jie) {
                val t = SolarTerm.termTime(yy, name) ?: continue
                val td = t.toLocalDate()
                if (!td.isAfter(today) && (best == null || td.isAfter(best))) {
                    best = td; monthZhiIdx = zhi
                }
            }
        }
        // 节气年以立春为界（日精度）：立春当天起属新年，立春前属上一年
        val lichun = SolarTerm.termTime(dt.year, "立春")
        val year = if (lichun != null && dt.toLocalDate().isBefore(lichun.toLocalDate())) dt.year - 1 else dt.year
        val yearGanIdx = ((year - 4) % 10 + 10) % 10
        // 五虎遁: 寅月天干 = (yearGanIdx%5)*2+2
        val yinGanStart = (yearGanIdx % 5) * 2 + 2
        // 月支索引: 子=0 -> 对应 monthGan = yinGanStart + (monthZhiIdx - 2) (寅=2 起)
        var diff = monthZhiIdx - 2
        if (diff < 0) diff += 12
        val monthGanIdx = (yinGanStart + diff) % 10
        val monthGan = LunarCalendar.TIANGAN[monthGanIdx]
        val monthZhi = zhiSeq[monthZhiIdx]
        return Pair(monthGan + monthZhi, monthZhiIdx)
    }

    /** 宜：移植 LunarUtil.getDayYi */
    private fun getDayYi(monthGz: String, dayGz: String): List<String> {
        val day = hex(jiaZiIndex(dayGz))
        val month = hex(jiaZiIndex(monthGz))
        var right = AlmanacData.DAY_YI_JI
        var index = right.indexOf("$day=")
        while (index > -1) {
            right = right.substring(index + 3)
            var left = right
            val eq = left.indexOf('=')
            if (eq > -1) left = left.substring(0, eq - 2)
            val ci = left.indexOf(':')
            if (ci < 0) break
            val months = left.substring(0, ci)
            var matched = false
            var i = 0
            while (i < months.length) {
                if (months.substring(i, i + 2) == month) { matched = true; break }
                i += 2
            }
            if (matched) {
                var ys = left.substring(ci + 1)
                val comma = ys.indexOf(',')
                if (comma > -1) ys = ys.substring(0, comma)
                val res = mutableListOf<String>()
                i = 0
                while (i < ys.length) {
                    res.add(AlmanacData.YI_JI[ys.substring(i, i + 2).toInt(16)])
                    i += 2
                }
                if (res.isNotEmpty()) return res
                break
            }
            index = right.indexOf("$day=")
        }
        return listOf(AlmanacData.SHEN_SHA[0])
    }

    /** 忌：移植 LunarUtil.getDayJi */
    private fun getDayJi(monthGz: String, dayGz: String): List<String> {
        val day = hex(jiaZiIndex(dayGz))
        val month = hex(jiaZiIndex(monthGz))
        var right = AlmanacData.DAY_YI_JI
        var index = right.indexOf("$day=")
        while (index > -1) {
            right = right.substring(index + 3)
            var left = right
            val eq = left.indexOf('=')
            if (eq > -1) left = left.substring(0, eq - 2)
            val ci = left.indexOf(':')
            if (ci < 0) break
            val months = left.substring(0, ci)
            var matched = false
            var i = 0
            while (i < months.length) {
                if (months.substring(i, i + 2) == month) { matched = true; break }
                i += 2
            }
            if (matched) {
                val js = left.substring(left.indexOf(',') + 1)
                val res = mutableListOf<String>()
                i = 0
                while (i < js.length) {
                    res.add(AlmanacData.YI_JI[js.substring(i, i + 2).toInt(16)])
                    i += 2
                }
                if (res.isNotEmpty()) return res
                break
            }
            index = right.indexOf("$day=")
        }
        return listOf(AlmanacData.SHEN_SHA[0])
    }

    /** 吉神：移植 LunarUtil.getDayJiShen */
    private fun getDayJiShen(lunarMonth: Int, dayGz: String): List<String> = dayShenSha(lunarMonth, dayGz, false)
    /** 凶煞：移植 LunarUtil.getDayXiongSha */
    private fun getDayXiongSha(lunarMonth: Int, dayGz: String): List<String> = dayShenSha(lunarMonth, dayGz, true)

    private fun dayShenSha(lunarMonth: Int, dayGz: String, xiong: Boolean): List<String> {
        val day = hex(jiaZiIndex(dayGz))
        val month = kotlin.math.abs(lunarMonth).toString(16).uppercase()
        val index = AlmanacData.DAY_SHEN_SHA.indexOf("$month$day=")
        if (index > -1) {
            var left = AlmanacData.DAY_SHEN_SHA.substring(index + 4)
            val eq = left.indexOf('=')
            if (eq > -1) left = left.substring(0, eq - 3)
            var codes = if (xiong) left.substring(left.indexOf(',') + 1) else left.substring(0, left.indexOf(','))
            val res = mutableListOf<String>()
            var i = 0
            while (i < codes.length) {
                res.add(AlmanacData.SHEN_SHA[codes.substring(i, i + 2).toInt(16)])
                i += 2
            }
            if (res.isNotEmpty()) return res
        }
        return listOf(AlmanacData.SHEN_SHA[0])
    }

    /** 排一天的黄历 */
    fun paipan(dt: LocalDateTime): AlmanacDay {
        val lunar = LunarCalendar.fromDateTime(dt)
        val dayGz = lunar.dayGanZhi
        val yearGz = lunar.yearGanZhi
        val timeGz = lunar.hourGanZhi
        val (monthGz, monthZhiIdx) = solarMonthGanzhi(dt)

        val dayGanIdx = LunarCalendar.TIANGAN.indexOf(dayGz[0].toString())
        val dayZhiIdx = LunarCalendar.DIZHI.indexOf(dayGz[1].toString())
        val dayZhi = dayGz[1].toString()

        // 节气：当天中午所在节气区间, 若当天恰为节气日则显示
        var jieQi = ""
        val range = SolarTerm.rangeAt(dt.withHour(12))
        if (range != null) {
            val fd = range.from.time.toLocalDate()
            if (fd == dt.toLocalDate()) jieQi = range.from.name
        }

        // 星期
        val weekCn = when (dt.dayOfWeek.value) { 1->"一";2->"二";3->"三";4->"四";5->"五";6->"六";7->"日"; else->"" }
        val solarText = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 星期$weekCn"
        val lunarText = (if (lunar.isLeap) "闰" else "") + AlmanacData.MONTH[lunar.month] + "月" + AlmanacData.DAY[lunar.day]
        val baziText = "${yearGz}年 ${monthGz}月 ${dayGz}日 ${timeGz}时"

        // 值神
        val zhiShenOffset = AlmanacData.ZHI_TIAN_SHEN_OFFSET[AlmanacData.ZHI[monthZhiIdx + 1]] ?: 0
        val zhiShen = AlmanacData.TIAN_SHEN[(dayZhiIdx + zhiShenOffset) % 12 + 1]
        val zhiShenType = AlmanacData.TIAN_SHEN_TYPE[zhiShen] ?: ""

        // 建星
        var zxOffset = dayZhiIdx - monthZhiIdx
        if (zxOffset < 0) zxOffset += 12
        val zhiXing = AlmanacData.ZHI_XING[zxOffset + 1]

        // 二十八宿: XIU[dayZhi + weekIndex]  (week: 周日=0)
        val weekIdx = dt.dayOfWeek.value % 7
        val xiu = AlmanacData.XIU[dayZhi + weekIdx] ?: ""
        val xiuLuck = AlmanacData.XIU_LUCK[xiu] ?: ""
        val animal = AlmanacData.ANIMAL[xiu] ?: ""

        // 纳音 / 冲煞
        val naYin = AlmanacData.NAYIN[dayGz] ?: ""
        val chongZhi = AlmanacData.CHONG[dayZhiIdx]
        val chongShengXiao = AlmanacData.SHENGXIAO[LunarCalendar.DIZHI.indexOf(chongZhi) + 1]
        val sha = AlmanacData.SHA[dayZhi] ?: ""
        val chongSha = "冲$chongShengXiao 煞$sha"

        // 方位
        fun desc(v: String?) = v?.let { AlmanacData.POSITION_DESC[it] } ?: ""
        val xiShen = desc(AlmanacData.POSITION_XI[dayGanIdx + 1])
        val yangGui = desc(AlmanacData.POSITION_YANG_GUI[dayGanIdx + 1])
        val fuShen = desc(AlmanacData.POSITION_FU[dayGanIdx + 1])
        val caiShen = desc(AlmanacData.POSITION_CAI[dayGanIdx + 1])
        val taiShen = AlmanacData.POSITION_TAI_DAY[jiaZiIndex(dayGz).toString()] ?: ""

        // 彭祖百忌
        val pengZu = (AlmanacData.PENGZU_GAN[dayGanIdx + 1] ?: "") + "\n" + (AlmanacData.PENGZU_ZHI[dayZhiIdx + 1] ?: "")

        // 节日
        val festivals = buildFestivals(dt, lunar, jieQi)

        // 今日胎神完整描述
        val taiShenText = AlmanacData.POSITION_TAI_DAY[jiaZiIndex(dayGz).toString()] ?: taiShen

        return AlmanacDay(
            solarText, lunarText, baziText, yearGz, monthGz, dayGz, timeGz,
            jieQi, getDayYi(monthGz, dayGz), getDayJi(monthGz, dayGz),
            chongSha, naYin, zhiShen, zhiShenType, zhiXing, xiu, xiuLuck, animal,
            caiShen, xiShen, fuShen, yangGui, taiShen,
            getDayJiShen(lunar.month, dayGz), getDayXiongSha(lunar.month, dayGz),
            pengZu, dayZhi, dayGanIdx, dayZhiIdx, monthZhiIdx,
            dt.year, dt.monthValue, dt.dayOfMonth,
            lunar.month, lunar.day, lunar.isLeap, weekCn,
            festivals, taiShenText
        )
    }

    // ============ 日历页面(万年历/老黄历)所需 ============

    /** 时辰格：天干+地支+黄道黑道吉凶 */
    data class HourCell(val gan: String, val zhi: String, val ganZhi: String, val luck: String)

    /** 某日的十二时辰格(子→亥) */
    fun hourCells(dayGan: String, dayZhi: String): List<HourCell> {
        val dayGanIdx = LunarCalendar.TIANGAN.indexOf(dayGan)
        val hourZhiList = LunarCalendar.DIZHI
        val offset = AlmanacData.ZHI_TIAN_SHEN_OFFSET[dayZhi] ?: 0
        return (0..11).map { zi ->
            val gan = LunarCalendar.TIANGAN[((dayGanIdx % 5) * 2 + zi) % 10]
            val zhi = hourZhiList[zi]
            // 天神序 = (时支序 + 日支偏移) % 12, TIAN_SHEN 数组下标从 1 开始(下标0为空)
            val shenIdx = (zi + offset) % 12 + 1
            val shen = AlmanacData.TIAN_SHEN[shenIdx]
            val type = AlmanacData.TIAN_SHEN_TYPE[shen] ?: ""
            val luck = AlmanacData.TIAN_SHEN_TYPE_LUCK[type] ?: ""
            HourCell(gan, zhi, gan + zhi, luck)
        }
    }

    /** 某时辰的宜忌 */
    fun hourYiJi(dayGz: String, hourGz: String): Pair<List<String>, List<String>> {
        val day = hex(jiaZiIndex(dayGz))
        val time = hex(jiaZiIndex(hourGz))
        var left = ""
        val index = AlmanacData.TIME_YI_JI.indexOf("$day$time=")
        if (index > -1) {
            left = AlmanacData.TIME_YI_JI.substring(index + 5)
            val eq = left.indexOf('=')
            if (eq > -1) left = left.substring(0, eq - 4)
        }
        val def = AlmanacData.YI_JI[0]
        fun parse(seg: String): List<String> {
            if (seg.isEmpty()) return listOf(def)
            val res = mutableListOf<String>()
            var i = 0
            while (i + 1 < seg.length) { res.add(AlmanacData.YI_JI[seg.substring(i, i + 2).toInt(16)]); i += 2 }
            return if (res.isEmpty()) listOf(def) else res
        }
        val comma = left.indexOf(',')
        val ys = if (comma > -1) left.substring(0, comma) else left
        val js = if (comma > -1) left.substring(comma + 1) else ""
        return Pair(parse(ys), parse(js))
    }

    /** 时辰时间范围, 如 子=23:00-00:59 */
    fun hourRange(zi: Int): String {
        val start = (zi * 2 - 1 + 24) % 24
        val end = (start + 1) % 24
        return "%02d:00-%02d:59".format(start, end)
    }

    /** 构建节日列表(按显示优先级): 农历节日/公历节日/周节日/节气/道教 */
    private fun buildFestivals(dt: LocalDateTime, lunar: LunarCalendar.LunarDate, jieQi: String): List<String> {
        val res = mutableListOf<String>()
        // 节气优先
        if (jieQi.isNotEmpty()) res.add(jieQi)
        // 农历节日(闰月不算)
        if (!lunar.isLeap) AlmanacExtra.LUNAR_FESTIVAL["${lunar.month}-${lunar.day}"]?.let { res.add(it) }
        // 公历节日
        AlmanacExtra.SOLAR_FESTIVAL["${dt.monthValue}-${dt.dayOfMonth}"]?.let { res.add(it) }
        // 周节日: 月-第几周-星期几 (周日=0)
        val week = dt.dayOfWeek.value % 7
        val idx = (dt.dayOfMonth - 1) / 7 + 1
        AlmanacExtra.WEEK_FESTIVAL["${dt.monthValue}-$idx-$week"]?.let { res.add(it) }
        // 道教节日
        res.addAll(taoFestivals(dt, lunar))
        return res.distinct()
    }

    /** 道教节日(暗戊/明戊/天赦/三会/三元/五腊/八节/八会/常规节日) */
    fun taoFestivals(dt: LocalDateTime, lunar: LunarCalendar.LunarDate): List<String> {
        val res = mutableListOf<String>()
        val md = "${lunar.month}-${lunar.day}"
        AlmanacExtra.TAO_FEST[md]?.let { if (it.isNotEmpty()) res.add(it) }
        if (lunar.month in 1..12 && lunar.dayGanZhi[1].toString() == AlmanacExtra.AN_WU[lunar.month - 1]) res.add("暗戊日")
        if (lunar.dayGanZhi[0] == '戊') res.add("明戊日")
        if (md in AlmanacExtra.SAN_HUI) res.add("三会日")
        if (md in AlmanacExtra.SAN_YUAN) res.add("三元日")
        if (md in AlmanacExtra.WU_LA) res.add("五腊日")
        AlmanacExtra.BA_HUI[lunar.dayGanZhi]?.let { res.add(it) }
        // 天赦日
        val mz = lunar.monthGanZhi[1].toString()
        val dgz = lunar.dayGanZhi
        val ts = when {
            mz in listOf("寅","卯","辰") && dgz == "戊寅" -> true
            mz in listOf("巳","午","未") && dgz == "甲午" -> true
            mz in listOf("申","酉","戌") && dgz == "戊申" -> true
            mz in listOf("亥","子","丑") && dgz == "甲子" -> true
            else -> false
        }
        if (ts) res.add("天赦日")
        return res.distinct()
    }

    /** 时辰详情: 冲煞 + 四方 + 宜忌 */
    data class HourDetail(
        val ganZhi: String, val range: String, val luck: String,
        val chongSha: String, val caiShen: String, val xiShen: String,
        val fuShen: String, val yangGui: String,
        val yi: List<String>, val ji: List<String>
    )

    /** 某时辰完整详情 */
    fun hourDetail(dayGz: String, zi: Int): HourDetail {
        val cells = hourCells(dayGz[0].toString(), dayGz[1].toString())
        val cell = cells[zi]
        val gan = cell.gan
        val zhi = cell.zhi
        val ganIdx = LunarCalendar.TIANGAN.indexOf(gan)
        // 冲煞
        val chongZhi = AlmanacData.CHONG[zi]
        val chongIdx = LunarCalendar.DIZHI.indexOf(chongZhi)
        val chongSx = AlmanacData.SHENGXIAO[chongIdx + 1]
        val sha = AlmanacData.SHA[zhi] ?: ""
        val chongSha = "冲$chongSx 煞$sha"
        fun desc(v: String?) = v?.let { AlmanacData.POSITION_DESC[it] } ?: ""
        val cai = desc(AlmanacData.POSITION_CAI[ganIdx + 1])
        val xi = desc(AlmanacData.POSITION_XI[ganIdx + 1])
        val fu = desc(AlmanacData.POSITION_FU_2[ganIdx + 1])
        val yang = desc(AlmanacData.POSITION_YANG_GUI[ganIdx + 1])
        val yj = hourYiJi(dayGz, cell.ganZhi)
        return HourDetail(cell.ganZhi, hourRange(zi), cell.luck, chongSha, cai, xi, fu, yang, yj.first, yj.second)
    }

    /** 万年历月历格子的节日标签: 仅节气/农历传统节日/公历节日, 不含道教斋日(那些只在详情显示) */
    fun festivalShort(date: java.time.LocalDate): String {
        val dt = date.atTime(12, 0)
        val lunar = LunarCalendar.fromDateTime(dt)
        var jq = ""
        val range = SolarTerm.rangeAt(dt)
        if (range != null && range.from.time.toLocalDate() == date) jq = range.from.name
        if (jq.isNotEmpty()) return jq
        if (!lunar.isLeap) AlmanacExtra.LUNAR_FESTIVAL["${lunar.month}-${lunar.day}"]?.let { return it }
        AlmanacExtra.SOLAR_FESTIVAL["${dt.monthValue}-${dt.dayOfMonth}"]?.let { return it }
        val week = dt.dayOfWeek.value % 7
        val idx = (dt.dayOfMonth - 1) / 7 + 1
        AlmanacExtra.WEEK_FESTIVAL["${dt.monthValue}-$idx-$week"]?.let { return it }
        return ""
    }
}

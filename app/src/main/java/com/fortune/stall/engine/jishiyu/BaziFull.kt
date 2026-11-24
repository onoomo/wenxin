package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 八字完整命盘扩展 —— 1:1 移植自吉时雨 bazi.js / baziutils.js / lunar.js (AGPL-3.0)。
 * 为 BaziChart.Chart 补充"基本 / 命盘 / 详盘"三页所需数据：
 * 真太阳时、五行旺衰统计、天干地支留意、起运、大运及流年。
 */
object BaziFull {

    data class LiuYi(val tiangan: List<String>, val dizhi: List<String>)

    data class QiYun(
        val startYear: Int, val startMonth: Int, val startDay: Int, val startHour: Int,
        val jieName: String, val days: Int, val jiaoyunGan1: String, val jiaoyunGan2: String,
        val startSolarYear: Int = 0   // 起运时刻所在公历年（跨年后），第一步大运起始年
    )

    data class LiuNian(
        val ganzhi: String, val gan: String, val zhi: String,
        val ganWx: String, val zhiWx: String, val ganSs: String, val zhiSs: String,
        val year: Int, val age: Int
    )

    data class DaYun(
        val ganzhi: String, val gan: String, val zhi: String,
        val ganWx: String, val zhiWx: String, val ganSs: String, val zhiSs: String,
        val startYear: Int, val startAge: Int, val liunian: List<LiuNian>
    )

    data class Data(
        val nongliFull: String, val riyuan: String, val mingGuaDir: String,
        val jieqiFull: String, val birthArea: String,
        val taiyuanNayin: String, val taixiNayin: String, val minggongNayin: String, val shengongNayin: String,
        val wuXingQty: Map<String, Int>,          // 含藏干的五行数量
        val wangShuai: List<String>,              // [旺,相,休,囚,死] 对应五行
        val liuYi: LiuYi,
        val qiYun: QiYun,
        val dayun: List<DaYun>,
        val chengguDetail: String
    )

    private val GAN_WX = JishiyuBase.GAN_WUXING
    private val ZHI_WX = JishiyuBase.ZHI_WUXING
    private val GAN = LunarCalendar.TIANGAN
    private val ZHI = LunarCalendar.DIZHI

    // 十神全称 -> 简写 (shishenJc)
    private val SS_SHORT = mapOf(
        "比肩" to "比", "劫财" to "劫", "食神" to "食", "伤官" to "伤", "偏财" to "才",
        "正财" to "财", "七杀" to "杀", "正官" to "官", "偏印" to "枭", "正印" to "印",
        "日主" to "日元", "元男" to "日元", "元女" to "日元"
    )
    private fun ssShort(full: String) = SS_SHORT[full] ?: full

    // 六十四甲子序号
    private fun jzIndex(gz: String): Int {
        val i = JishiyuBase.jiaziIndex(gz)
        return if (i < 0) 0 else i
    }
    private fun jzOf(index: Int): String {
        val i = ((index % 60) + 60) % 60
        return GAN[i % 10] + ZHI[i % 12]
    }

    private fun isYangGan(g: String) = g in setOf("甲", "丙", "戊", "庚", "壬")

    // 年中文 "二〇二六"
    private fun yearCn(y: Int): String {
        val d = "〇一二三四五六七八九"
        return y.toString().map { d[it - '0'] }.joinToString("")
    }

    /** 当前节气：与出生日期同一天的节气(display格式)；否则"上个节之后,下个节之前" */
    private fun currentJieQi(dt: LocalDateTime): String {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        for (yy in (dt.year - 1)..(dt.year + 1)) for (i in 0..23) {
            val t = SolarTerm.termTime(yy, i) ?: continue
            if (t.year == dt.year && t.monthValue == dt.monthValue && t.dayOfMonth == dt.dayOfMonth)
                return SolarTerm.NAMES[i] + "(" + t.format(fmt) + ")"
        }
        val range = SolarTerm.rangeAt(dt)
        return if (range != null) "${range.from.display}之后, ${range.to.display}之前" else ""
    }

    /**
     * 生成扩展数据。dt 为排盘输入的公历时刻，chart 为 BaziChart.build 的结果。
     */
    fun build(dt: LocalDateTime, isMan: Boolean, chart: BaziChart.Chart): Data {
        val gz = chart.pillars.map { it.ganZhi }
        val dayGan = gz[2][0].toString()
        val monthZhi = gz[1][1].toString()
        val yearGan = gz[0][0].toString()

        // ---- 基本信息 ----
        val nongliFull = yearCn(dt.year) + "年" + chart.lunar.replace("日", "") + " " + ZHI[(dt.hour + 1) / 2 % 12] + "时"
        val riyuan = if (isMan) "元男" else "元女"
        val dong4xi4 = dong4xi4(chart.mingGua.replace("卦", ""))
        val jieqiFull = currentJieQi(dt)

        // 胎元/胎息/命宫/身宫纳音
        val taiyuanNayin = JishiyuBase.queryNayin(chart.taiYuan)
        val taixiNayin = JishiyuBase.queryNayin(chart.taiXi)
        val minggongNayin = JishiyuBase.queryNayin(chart.mingGong.replace("宫", ""))
        val shengongNayin = JishiyuBase.queryNayin(chart.shenGong.replace("宫", ""))

        // ---- 五行统计(含藏干)：原版口径 = 4天干 + 4地支本气 + 地支全部藏干 ----
        // 例：丙午 己亥 癸卯 壬戌 = 4天干 + 4地支本气 + 8藏干 = 16
        // (木3 火4 土4 金1 水4)，与原版基本信息页一致。
        val qty = mutableMapOf("金" to 0, "水" to 0, "木" to 0, "火" to 0, "土" to 0)
        for (p in chart.pillars) {
            val gw = GAN_WX[p.gan] ?: "木"
            qty[gw] = (qty[gw] ?: 0) + 1
            val zw = ZHI_WX[p.zhi] ?: "木"
            qty[zw] = (qty[zw] ?: 0) + 1
            for (c in p.cangGan) {
                val wx = GAN_WX[c.toString()]
                if (wx != null) qty[wx] = (qty[wx] ?: 0) + 1
            }
        }

        // ---- 五行旺衰(旺相休囚死) ----
        val wangShuai = WANG_SHUAI[ZHI_WX[monthZhi]] ?: listOf("木", "火", "土", "金", "水")

        // ---- 天干地支留意 ----
        val gans = chart.pillars.map { it.gan }
        val zhis = chart.pillars.map { it.zhi }
        val liuYi = LiuYi(
            tiangan = tiangan5he(gans) + tiangan4cong(gans) + tianganKe(gans),
            dizhi = dizhi6he(zhis) + dizhi4cong(zhis) + dizhiBan3he(zhis) + dizhiGong3he(zhis) +
                dizhiAnhe(zhis) + dizhiZhixing(zhis) + dizhi6hai(zhis) + dizhi6po(zhis) +
                dizhi3hui(zhis) + dizhi3he(zhis) + dizhi3xing(zhis) + dizhi2xing(zhis) + dizhiDuhe(gans)
        )

        // ---- 起运 ----
        val qiyun = buildQiYun(dt, isMan, yearGan, gz)

        // ---- 大运 + 流年 ----
        val dayun = buildDaYun(dt, isMan, yearGan, monthZhi, dayGan, qiyun)

        return Data(
            nongliFull, riyuan, dong4xi4, jieqiFull, "北京市",
            taiyuanNayin, taixiNayin, minggongNayin, shengongNayin,
            qty, wangShuai, liuYi, qiyun, dayun, chart.chenggu.detail
        )
    }

    // ---------- 起运（sect=2，按分钟差换算） ----------
    private val JIE_NAMES = arrayOf("小寒", "立春", "惊蛰", "清明", "立夏", "芒种", "小暑", "立秋", "白露", "寒露", "立冬", "大雪")

    private fun buildQiYun(dt: LocalDateTime, isMan: Boolean, yearGan: String, gz: List<String>): QiYun {
        val forward = (isYangGan(yearGan) && isMan) || (!isYangGan(yearGan) && !isMan)
        // 找 dt 之前/之后最近的"节"
        var prev: LocalDateTime? = null
        var next: LocalDateTime? = null
        var prevName = ""
        for (yy in (dt.year - 1)..(dt.year + 1)) for (name in JIE_NAMES) {
            val t = SolarTerm.termTime(yy, name) ?: continue
            if (!t.isAfter(dt) && (prev == null || t.isAfter(prev))) { prev = t; prevName = name }
            if (t.isAfter(dt) && (next == null || t.isBefore(next))) { next = t }
        }
        // 起算区间
        val start = if (forward) dt else (prev ?: dt)
        val end = if (forward) (next ?: dt.plusDays(15)) else dt
        val minutes = ChronoUnit.MINUTES.between(start, end)
        var year = (minutes / 4320).toInt()
        var rest = minutes - year * 4320
        var month = (rest / 360).toInt(); rest -= month * 360
        var day = (rest / 12).toInt(); rest -= day * 12
        var hour = (rest * 2).toInt()

        // 起运时刻点 = 出生 + 起运年月日时
        var startSolar = dt.plusYears(year.toLong()).plusMonths(month.toLong())
            .plusDays(day.toLong()).plusHours(hour.toLong())
        // 起运时刻所在年份（可能跨年：2026+4年11月 = 2031），即第一步大运起始年
        val firstDayunStartYear = startSolar.year
        // 交运节名：起运时刻之前最近的节；days = 与该节的天数差
        var jieName = prevName
        var jieTime = prev
        for (yy in (startSolar.year - 1)..(startSolar.year + 1)) for (name in JIE_NAMES) {
            val t = SolarTerm.termTime(yy, name) ?: continue
            if (!t.isAfter(startSolar) && (jieTime == null || t.isAfter(jieTime))) { jieTime = t; jieName = name }
        }
        val days = if (jieTime != null) ChronoUnit.DAYS.between(jieTime, startSolar).toInt() else 0

        // 交运干 = 第一步大运所含流年的第 1、6 位天干（流年 = 起运年起的六十甲子）
        val gan1 = jzOf(firstDayunStartYear - 4)[0].toString()
        val gan2 = jzOf(firstDayunStartYear - 4 + 5)[0].toString()

        return QiYun(year, month, day, hour, jieName, days, gan1, gan2, firstDayunStartYear)
    }

    // ---------- 大运（月柱 ± 序），每步 10 流年 ----------
    private fun buildDaYun(dt: LocalDateTime, isMan: Boolean, yearGan: String, monthZhi: String, dayGan: String, qiyun: QiYun): List<DaYun> {
        val monthGz = gzOfMonth(dt)
        val mIdx = jzIndex(monthGz)
        val firstStartYear = if (qiyun.startSolarYear > 0) qiyun.startSolarYear else dt.year + qiyun.startYear
        val isForward = inferForward(dt, isMan, yearGan)

        val list = mutableListOf<DaYun>()
        for (i in 1..10) {
            val dyGz = jzOf(mIdx + (if (isForward) i else -i))
            val dyGan = dyGz[0].toString(); val dyZhi = dyGz[1].toString()
            val startYear = firstStartYear + (i - 1) * 10
            val startAge = startYear - dt.year + 1
            val liunian = mutableListOf<LiuNian>()
            for (j in 0 until 10) {
                val ly = startYear + j
                val lnGz = jzOf(ly - 4) // 公历年干支(立春界) = (year-4)六十甲子
                val lg = lnGz[0].toString(); val lz = lnGz[1].toString()
                val cg = JishiyuBase.CANGGAN[lz]?.firstOrNull()?.toString() ?: lz
                liunian.add(
                    LiuNian(
                        lnGz, lg, lz,
                        GAN_WX[lg] ?: "", ZHI_WX[lz] ?: "",
                        ssShort(BaziChart.shiShen(dayGan, lg)),
                        ssShort(BaziChart.shiShen(dayGan, cg)),
                        ly, startAge + j
                    )
                )
            }
            list.add(
                DaYun(
                    dyGz, dyGan, dyZhi, GAN_WX[dyGan] ?: "", ZHI_WX[dyZhi] ?: "",
                    ssShort(BaziChart.shiShen(dayGan, dyGan)),
                    ssShort(BaziChart.shiShen(dayGan, JishiyuBase.CANGGAN[dyZhi]?.firstOrNull().toString())),
                    startYear, startAge, liunian
                )
            )
        }
        return list
    }

    private fun inferForward(dt: LocalDateTime, man: Boolean, yearGan: String) =
        (isYangGan(yearGan) && man) || (!isYangGan(yearGan) && !man)

    private fun gzOfMonth(dt: LocalDateTime): String {
        // 复用 BaziChart.fourPillars 取月柱
        return BaziChart.fourPillars(dt)[1]
    }

    // ---------- 天干留意 ----------
    private val HE5 = arrayOf("甲己", "乙庚", "丙辛", "丁壬", "戊癸")
    private val HE5_HUA = arrayOf("合化土", "合化金", "合化水", "合化木", "合化火")
    private val CONG4 = arrayOf("甲庚", "乙辛", "丙壬", "丁癸")
    private val WX_KE = arrayOf("金木", "木土", "土水", "水火", "火金")

    private fun tiangan5he(list: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in list) for (b in list) for (p in listOf(a + b, b + a)) {
            val i = HE5.indexOf(p)
            if (i >= 0) { val s = HE5[i] + HE5_HUA[i]; if (!out.contains(s)) out.add(s) }
        }
        return out
    }
    private fun tiangan4cong(list: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in list) for (b in list) for (p in listOf(a + b, b + a)) {
            val i = CONG4.indexOf(p)
            if (i >= 0) { val s = CONG4[i] + "相冲"; if (!out.contains(s)) out.add(s) }
        }
        return out
    }
    private fun tianganKe(list: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in list) for (b in list) {
            val w1 = GAN_WX[a] ?: continue; val w2 = GAN_WX[b] ?: continue
            if (WX_KE.contains(w1 + w2)) { val s = a + "克" + b; if (!out.contains(s)) out.add(s) }
            if (WX_KE.contains(w2 + w1)) { val s = b + "克" + a; if (!out.contains(s)) out.add(s) }
        }
        return out
    }

    // ---------- 地支留意 ----------
    private fun pairs(list: List<String>, table: Array<String>, suffix: Array<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in list) for (b in list) for (p in listOf(a + b, b + a)) {
            val i = table.indexOf(p)
            if (i >= 0) { val s = table[i] + suffix[i]; if (!out.contains(s)) out.add(s) }
        }
        return out
    }
    private fun dizhi6he(l: List<String>) = pairs(l, arrayOf("子丑", "寅亥", "卯戌", "辰酉", "巳申", "午未"), arrayOf("合化土", "合化木", "合化火", "合化金", "合化水", "合化火土"))
    private fun dizhi4cong(l: List<String>) = pairs(l, arrayOf("寅申", "巳亥", "辰戌", "丑未", "子午", "卯酉"), Array(6) { "相冲" })
    private fun dizhiBan3he(l: List<String>) = pairs(l, arrayOf("申子", "子辰", "亥卯", "卯未", "寅午", "午戌", "巳酉", "酉丑"), arrayOf("半合水", "半合水", "半合木", "半合木", "半合火", "半合火", "半合金", "半合金"))
    private fun dizhiGong3he(l: List<String>) = pairs(l, arrayOf("申辰", "亥未", "寅戌", "巳丑"), arrayOf("拱合水", "拱合木", "拱合火", "拱合金"))
    private fun dizhiAnhe(l: List<String>) = pairs(l, arrayOf("寅丑", "亥午", "卯申", "巳酉", "子巳", "寅午"), Array(6) { "暗合" })
    private fun dizhi6hai(l: List<String>) = pairs(l, arrayOf("寅巳", "卯辰", "丑午", "子未", "酉戌", "申亥"), Array(6) { "相害" })
    private fun dizhi6po(l: List<String>) = pairs(l, arrayOf("子酉", "寅亥", "卯午", "辰丑", "巳申", "未戌"), Array(6) { "相破" })

    /** 三元组遍历：匹配 6 个一组的表，输出 表首项+后缀 */
    private fun triple(l: List<String>, table: Array<String>, suffix: Array<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in l) for (b in l) for (c in l) {
            val p = a + b + c
            val idx = table.indexOf(p)
            if (idx >= 0) {
                val base = idx / 6
                val s = table[base * 6] + suffix[base]
                if (!out.contains(s)) out.add(s)
            }
        }
        return out
    }
    private val HUI3 = arrayOf("寅卯辰","寅辰卯","卯辰寅","卯寅辰","辰寅卯","辰卯寅",
        "巳午未","巳未午","午巳未","午未巳","未巳午","未午巳",
        "申酉戌","申戌酉","酉申戌","酉戌申","戌申酉","戌酉申",
        "亥子丑","亥丑子","子亥丑","子丑亥","丑亥子","丑子亥")
    private val HUI3_JU = arrayOf("会东方木","会南方火","会西方金","会北方水")
    private val HE3 = arrayOf("亥卯未","亥未卯","卯亥未","卯未亥","未亥卯","未卯亥",
        "寅午戌","寅戌午","午寅戌","午戌寅","戌寅午","戌午寅",
        "巳酉丑","巳丑酉","酉巳丑","酉丑巳","丑巳酉","丑酉巳",
        "申子辰","申辰子","子申辰","子辰申","辰申子","辰子申")
    private val HE3_JU = arrayOf("三合木","三合火","三合金","三合水")
    private val XING3 = arrayOf("丑未戌","丑戌未","未丑戌","未戌丑","戌丑未","戌未丑",
        "寅巳申","寅申巳","巳寅申","巳申寅","申寅巳","申巳寅")
    private val XING2 = arrayOf("子卯","卯子")
    private val DUHE = arrayOf("甲己乙","甲乙己","己甲乙","己乙甲","乙甲己","乙己甲",
        "庚乙辛","庚辛乙","乙庚辛","乙辛庚","辛庚乙","辛乙庚",
        "丙辛丁","丙丁辛","辛丙丁","辛丁丙","丁丙辛","丁辛丙",
        "壬丁癸","壬癸丁","丁壬癸","丁癸壬","癸壬丁","癸丁壬",
        "戊癸己","戊己癸","癸戊己","癸己戊","己戊癸","己癸戊")
    private fun dizhi3hui(l: List<String>) = triple(l, HUI3, HUI3_JU)
    private fun dizhi3he(l: List<String>) = triple(l, HE3, HE3_JU)
    private fun dizhi3xing(l: List<String>) = triple(l, XING3, arrayOf("三刑", "三刑"))
    private fun dizhi2xing(l: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in l) for (b in l) if (XING2.contains(a + b)) { val s = "子卯相刑"; if (!out.contains(s)) out.add(s) }
        return out
    }
    private fun dizhiDuhe(gans: List<String>) = triple(gans, DUHE, Array(5) { "妒合" })
    private fun dizhiZhixing(l: List<String>): List<String> {
        val out = mutableListOf<String>()
        val table = arrayOf("辰辰", "午午", "酉酉", "亥亥")
        for (a in l.indices) for (b in l.indices) {
            if (a == b) continue
            val p = l[a] + l[b]; val i = table.indexOf(p)
            if (i >= 0) { val s = table[i] + "自刑"; if (!out.contains(s)) out.add(s) }
        }
        return out
    }

    private val WANG_SHUAI = mapOf(
        "金" to listOf("金", "水", "土", "火", "木"),
        "水" to listOf("水", "木", "金", "土", "火"),
        "木" to listOf("木", "火", "水", "金", "土"),
        "火" to listOf("火", "土", "木", "水", "金"),
        "土" to listOf("土", "金", "火", "木", "水")
    )

    private fun dong4xi4(gua: String): String = when (gua) {
        "坎", "离", "震", "巽" -> "东四命"
        else -> "西四命"
    }
}

package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime

/**
 * 八字排盘命盘结构化引擎 —— 依据 ji.js.cn 八字排盘页字段组装。
 * 复用 JishiyuBase 公共表 / LunarCalendar 四柱 / Shensha 神煞 / Chenggu 称骨 / BaziAnalyzer 旺衰用神。
 */
object BaziChart {
    data class Pillar(
        val name: String, val ganZhi: String,
        val shiShen: String,          // 天干十神
        val gan: String, val zhi: String,
        val cangGan: String,          // 藏干
        val cangShen: String,         // 藏干十神(用·分隔)
        val deShi: String,            // 地势(日主在此支的长生)
        val ziZuo: String,            // 自坐(本柱天干坐本柱地支的长生)
        val kongWang: String,         // 空亡
        val naYin: String,            // 纳音
        val shenSha: String           // 神煞
    )

    data class Chart(
        val solar: String, val lunar: String, val baziText: String,
        val shengXiao: String, val gender: String,
        val yearGan: String,
        val pillars: List<Pillar>,
        val wuXing: List<Pair<String, Int>>,   // 五行统计(含天干+地支)
        val taiYuan: String, val taiXi: String,
        val mingGong: String, val shenGong: String, val mingGua: String,
        val analysis: BaziAnalyzer.Analysis,
        val chenggu: Chenggu.ChengguResult,
        val jieQi: String
    )

    private val YANG_GAN = setOf("甲", "丙", "戊", "庚", "壬")
    private val WX = JishiyuBase.GAN_WUXING
    private val SHENG: Map<String, String> = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val KE: Map<String, String> = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")

    /**
     * 八字四柱（1:1 lunar.js EightChar 默认 getYear/getMonth/getDay/getTime）：
     * - 年柱以「立春」精确时刻为界；
     * - 月柱以十二「节」精确时刻为界，月干五虎遁；
     * - 日柱用公历日干支；
     * - 时柱五鼠遁，晚子时(23时)用次日日干起时干。
     */
    fun fourPillars(dt: LocalDateTime): List<String> {
        val gan = LunarCalendar.TIANGAN; val zhi = LunarCalendar.DIZHI
        // 年柱：立春时刻为界
        val lichun = SolarTerm.termTime(dt.year, "立春")
        val gzYear = if (lichun != null && dt.isBefore(lichun)) dt.year - 1 else dt.year
        val yearIdx = gzYear - 4
        val yearGz = LunarCalendar.ganZhi(yearIdx)
        val yearGanIdx = ((yearIdx % 10) + 10) % 10

        // 月柱：找 <= dt 的最近一个「节」(精确时刻)，定月支
        val jie = arrayOf(
            "小寒" to 1, "立春" to 2, "惊蛰" to 3, "清明" to 4, "立夏" to 5, "芒种" to 6,
            "小暑" to 7, "立秋" to 8, "白露" to 9, "寒露" to 10, "立冬" to 11, "大雪" to 0
        )
        var best: LocalDateTime? = null
        var monthZhiIdx = 1
        for (yy in (dt.year - 1)..(dt.year + 1)) for ((name, zi) in jie) {
            val t = SolarTerm.termTime(yy, name) ?: continue
            if (!t.isAfter(dt) && (best == null || t.isAfter(best))) { best = t; monthZhiIdx = zi }
        }
        // 五虎遁：寅月干 = (yearGan%5)*2+2
        val yinGanStart = (yearGanIdx % 5) * 2 + 2
        var diff = monthZhiIdx - 2; if (diff < 0) diff += 12
        val monthGanIdx = (yinGanStart + diff) % 10
        val monthGz = gan[monthGanIdx] + zhi[monthZhiIdx]

        // 日柱
        val dayGz = LunarCalendar.fromDateTime(dt).dayGanZhi
        val dayGanIdx = gan.indexOf(dayGz[0].toString())

        // 时柱：晚子时(23)用次日日干
        val hourZhiIdx = ((dt.hour + 1) / 2) % 12
        val stemForHour = if (dt.hour == 23) (dayGanIdx + 1) % 10 else dayGanIdx
        val hourStart = (stemForHour % 5) * 2
        val hourGz = gan[(hourStart + hourZhiIdx) % 10] + zhi[hourZhiIdx]

        return listOf(yearGz, monthGz, dayGz, hourGz)
    }

    /** 十神: 以日主 dayGan 为我, 判 other 天干相对十神 */
    fun shiShen(dayGan: String, other: String): String {
        if (dayGan == other) return "比肩"
        val dw = WX[dayGan] ?: return ""
        val ow = WX[other] ?: return ""
        val sameYinYang = (dayGan in YANG_GAN) == (other in YANG_GAN)
        return when {
            ow == dw -> if (sameYinYang) "比肩" else "劫财"
            SHENG[ow] == dw -> if (sameYinYang) "偏印" else "正印"   // 生我
            SHENG[dw] == ow -> if (sameYinYang) "食神" else "伤官"   // 我生
            KE[dw] == ow -> if (sameYinYang) "偏财" else "正财"      // 我克
            KE[ow] == dw -> if (sameYinYang) "七杀" else "正官"      // 克我
            else -> ""
        }
    }

    fun build(dt: LocalDateTime, isMan: Boolean): Chart {
        val lun = LunarCalendar.fromDateTime(dt)
        val gz = fourPillars(dt)
        val dayGan = gz[2][0].toString()
        val shenshaList = Shensha.castFourPillars(gz, isMan)
        val ssMap = shenshaList.associate { it.first to it.second }

        val names = listOf("年柱", "月柱", "日柱", "时柱")
        val pillars = gz.mapIndexed { i, pillar ->
            val gan = pillar[0].toString(); val zhi = pillar[1].toString()
            val cang = JishiyuBase.CANGGAN[zhi] ?: emptyArray()
            val cangShen = cang.joinToString("·") { shiShen(dayGan, it) }
            Pillar(
                name = names[i], ganZhi = pillar,
                shiShen = shiShen(dayGan, gan),
                gan = gan, zhi = zhi,
                cangGan = cang.joinToString(""),
                cangShen = cangShen,
                deShi = JishiyuBase.queryShengwang(dayGan, zhi),
                ziZuo = JishiyuBase.queryShengwang(gan, zhi),
                kongWang = JishiyuBase.queryKongwang(pillar),
                naYin = JishiyuBase.queryNayin(pillar),
                shenSha = ssMap[names[i]] ?: "无"
            )
        }

        // 五行统计(天干按干五行, 地支按支五行)
        val counts = mutableMapOf("木" to 0, "火" to 0, "土" to 0, "金" to 0, "水" to 0)
        for (p in pillars) {
            val gw = WX[p.gan] ?: "木"
            counts[gw] = (counts[gw] ?: 0) + 1
            val zw = JishiyuBase.ZHI_WUXING[p.zhi] ?: "木"
            counts[zw] = (counts[zw] ?: 0) + 1
        }
        val wxList = listOf("木", "火", "土", "金", "水").map { it to (counts[it] ?: 0) }

        // 胎元: 月干进一, 月支进三
        val mGan = gz[1][0].toString(); val mZhi = gz[1][1].toString()
        val mGanIdx = LunarCalendar.TIANGAN.indexOf(mGan)
        val mZhiIdx = LunarCalendar.DIZHI.indexOf(mZhi)
        val taiYuan = LunarCalendar.TIANGAN[(mGanIdx + 1) % 10] + LunarCalendar.DIZHI[(mZhiIdx + 3) % 12]
        // 胎息: 日干支(简化取日柱纳音对应, 用日支三合前一位) —— 用日干+日支三位
        val dGanIdx = LunarCalendar.TIANGAN.indexOf(dayGan)
        val dZhiIdx = LunarCalendar.DIZHI.indexOf(gz[2][1].toString())
        val taiXi = LunarCalendar.TIANGAN[(dGanIdx + 6) % 10] + LunarCalendar.DIZHI[(dZhiIdx + 6) % 12]

        // 命宫/身宫: 月支序 + 时支序(寅=1..丑=12)
        fun zhiSeq(z: String): Int { val i = LunarCalendar.DIZHI.indexOf(z); return ((i - 2 + 12) % 12) + 1 }
        val sum = zhiSeq(mZhi) + zhiSeq(gz[3][1].toString())
        fun numToZhi(n: Int) = LunarCalendar.DIZHI[((n - 1 + 2) % 12 + 12) % 12]
        val mingNum = if (sum <= 14) 14 - sum else 26 - sum
        val shenNum = if (sum <= 14) sum + 2 else sum - 12  // 身宫与命宫相隔
        val mingGong = numToZhi(mingNum) + "宫"
        val shenGong = numToZhi(shenNum) + "宫"

        // 命卦(九星): 男(100-年后两位)%9, 女(年后两位-4)%9; 5男寄坤女寄艮
        val yy = dt.year % 100
        val guaNum = if (isMan) ((100 - yy) % 9) else ((yy - 4 + 9) % 9)
        val n = if (guaNum == 0) 9 else guaNum
        val guaNames = mapOf(1 to "坎", 2 to "坤", 3 to "震", 4 to "巽", 5 to if (isMan) "坤" else "艮", 6 to "乾", 7 to "兑", 8 to "艮", 9 to "离")
        val mingGua = (guaNames[n] ?: "坤") + "卦"

        val analysis = BaziAnalyzer.analyze(gz)
        val chenggu = Chenggu.cast(dt, isMan)
        val range = SolarTerm.rangeAt(dt)
        val jieQi = range?.from?.name ?: ""

        val solarText = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时${dt.minute}分"
        val lunarText = (if (lun.isLeap) "闰" else "") + LunarCalendar.cnNumber(lun.month) + "月" + LunarCalendar.cnNumber(lun.day) + "日"

        return Chart(
            solar = solarText, lunar = lunarText, baziText = gz.joinToString("  "),
            shengXiao = lun.zodiac, gender = if (isMan) "男" else "女",
            yearGan = gz[0][0].toString(),
            pillars = pillars, wuXing = wxList,
            taiYuan = taiYuan, taiXi = taiXi, mingGong = mingGong, shenGong = shenGong, mingGua = mingGua,
            analysis = analysis, chenggu = chenggu, jieQi = jieQi
        )
    }
}

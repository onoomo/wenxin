package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 八字详盘"逐级下钻"数据源 —— 依据 ji.js.cn view_bazi.js 的大运/流年/流月/流日展开。
 * 按需生成：流年 -> 12 流月（五虎遁） -> 该月流日（真实公历日干支）。
 */
object BaziCascade {

    /** 一列下钻格子（干支 + 十神 + 表头两行） */
    data class Cell(
        val ganzhi: String, val gan: String, val zhi: String,
        val ganWx: String, val zhiWx: String,
        val ganSs: String, val zhiSs: String,
        val head: String, val sub: String
    )

    private val GAN = LunarCalendar.TIANGAN
    private val ZHI = LunarCalendar.DIZHI
    private val GAN_WX = JishiyuBase.GAN_WUXING
    private val ZHI_WX = JishiyuBase.ZHI_WUXING

    // 十神简写（对齐原版：偏印=枭、七杀=杀；详盘格子空间小用简称）
    private val SS_SHORT = mapOf(
        "比肩" to "比", "劫财" to "劫", "食神" to "食", "伤官" to "伤", "偏财" to "才",
        "正财" to "财", "七杀" to "杀", "正官" to "官", "偏印" to "枭", "正印" to "印"
    )
    private fun ss(full: String) = SS_SHORT[full] ?: full
    private fun ssOf(dayGan: String, g: String) = ss(BaziChart.shiShen(dayGan, g))

    /** 十二个"节"（八字月首）在 SolarTerm.NAMES 中的索引，0=立春...11=小寒 */
    private val JIE_IDX = intArrayOf(3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 1)
    private val JIE_NAME = arrayOf("立春", "惊蛰", "清明", "立夏", "芒种", "小暑", "立秋", "白露", "寒露", "立冬", "大雪", "小寒")
    /** 五虎遁：寅月起，正月地支索引 2(寅) 顺排 */
    private val YUE_ZHI = intArrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 1)

    /** 五虎遁：年干 -> 正月(寅)天干索引。甲己丙(2) 乙庚戊(4) 丙辛庚(6) 丁壬壬(8) 戊癸甲(0) */
    private fun yinGanStart(yearGan: String): Int {
        val gi = GAN.indexOf(yearGan)
        return intArrayOf(2, 4, 6, 8, 0)[(((gi % 5) + 5) % 5)]
    }

    private fun jzOf(index: Int): String {
        val i = ((index % 60) + 60) % 60
        return GAN[i % 10] + ZHI[i % 12]
    }

    /**
     * 小运阶段（出生~起运前，通常 1-5 岁）的流年，供点击「小运」后展开。
     * 起运年 = 首步大运起始年；出生年 = 起运年 - 起运年数；逐年干支按公历年推。
     */
    fun xiaoyunLiunian(qiYun: BaziFull.QiYun, dayun: List<BaziFull.DaYun>, dayGan: String): List<BaziFull.LiuNian> {
        if (dayun.isEmpty()) return emptyList()
        val birthYear = dayun[0].startYear - qiYun.startYear
        val n = qiYun.startYear.coerceIn(1, 10)
        val out = mutableListOf<BaziFull.LiuNian>()
        for (j in 0 until n) {
            val year = birthYear + j
            val gz = jzOf(year - 4)
            val gan = gz[0].toString(); val zhi = gz[1].toString()
            val cang = JishiyuBase.CANGGAN[zhi]?.firstOrNull()?.toString() ?: zhi
            out.add(
                BaziFull.LiuNian(gz, gan, zhi, GAN_WX[gan] ?: "", ZHI_WX[zhi] ?: "",
                    ssOf(dayGan, gan), ssOf(dayGan, cang), year, j + 1)
            )
        }
        return out
    }

    /** 某流年第 m 个流月(0=寅/立春, 11=丑/小寒, 12=次年立春)对应"节"的公历日期 */
    private fun jieDate(year: Int, m: Int): LocalDate? {
        return when {
            m in 0..10 -> SolarTerm.termTime(year, JIE_IDX[m])?.toLocalDate()
            m == 11 -> SolarTerm.termTime(year + 1, JIE_IDX[11])?.toLocalDate() // 小寒在次年 1 月
            m == 12 -> SolarTerm.termTime(year + 1, JIE_IDX[0])?.toLocalDate()  // 次年立春
            else -> null
        }
    }

    /**
     * 流年 -> 12 流月（五虎遁定干，节气定公历日期）。
     */
    fun liuYue(ln: BaziFull.LiuNian, dayGan: String): List<Cell> {
        val start = yinGanStart(ln.gan)
        return (0..11).map { m ->
            val gan = GAN[(start + m) % 10]
            val zhi = ZHI[YUE_ZHI[m]]
            val d = jieDate(ln.year, m)
            val head = JIE_NAME[m]
            val sub = if (d != null) "${d.monthValue}/${d.dayOfMonth}" else ""
            val cang = JishiyuBase.CANGGAN[zhi]?.firstOrNull()?.toString() ?: zhi
            Cell(gan + zhi, gan, zhi, GAN_WX[gan] ?: "", ZHI_WX[zhi] ?: "",
                ssOf(dayGan, gan), ssOf(dayGan, cang), head, sub)
        }
    }

    /**
     * 流年 + 流月序号 -> 该月流日（自本月"节"起至次月"节"前一天；每行一个公历日，真实日干支）。
     */
    fun liuRi(ln: BaziFull.LiuNian, monthIndex: Int, dayGan: String): List<Cell> {
        val start = jieDate(ln.year, monthIndex) ?: return emptyList()
        val end = jieDate(ln.year, monthIndex + 1) ?: start.plusMonths(1)
        val days = ChronoUnit.DAYS.between(start, end).toInt().coerceIn(28, 32)
        val out = mutableListOf<Cell>()
        for (k in 0 until days) {
            val date = start.plusDays(k.toLong())
            val gz = LunarCalendar.dayGanZhi(date)
            val gan = gz[0].toString(); val zhi = gz[1].toString()
            val lun = LunarCalendar.fromDateTime(date.atStartOfDay())
            val cang = JishiyuBase.CANGGAN[zhi]?.firstOrNull()?.toString() ?: zhi
            out.add(
                Cell(gz, gan, zhi, GAN_WX[gan] ?: "", ZHI_WX[zhi] ?: "",
                    ssOf(dayGan, gan), ssOf(dayGan, cang),
                    "${date.dayOfMonth}", LunarCalendar.cnNumber(lun.day))
            )
        }
        return out
    }
}

package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar

/**
 * 八字详盘「串宫压运」十二神 + 「胎命身」三柱算法。
 * 1:1 移植自 jishiyu：
 *  - js/baziutils.js  GODS_12 / GAN2ZHI / getGod / getGanzhi12Gods
 *  - js/view_ui/view_bazi.js  getTaisuiZhi / show4Zhu12Gods / showTaiyuanCol 等
 *  - js/3rd/lunar/lunar.js  EightChar.getTaiYuan / getMingGong / getShenGong
 */
object BaziChuangong {

    /** 串宫十二神固定循环（起点=太岁） */
    val GODS_12 = listOf("太岁", "青龙", "丧门", "六合", "官符", "小耗", "大耗", "朱雀", "白虎", "贵神", "吊客", "病符")

    /** 天干寄宫（日干起太岁用，原版 GAN2ZHI） */
    val GAN2ZHI = mapOf(
        "甲" to "寅", "乙" to "卯", "丙" to "巳", "丁" to "午", "戊" to "戌",
        "己" to "丑", "庚" to "申", "辛" to "酉", "壬" to "亥", "癸" to "子"
    )

    private val ZHI = LunarCalendar.DIZHI                       // 子丑寅卯...（0-based）
    private val GAN = LunarCalendar.TIANGAN                     // 甲乙丙丁...（0-based）
    /**  lunar.js MONTH_ZHI：以寅为正月起（0-based：寅=0） */
    private val MONTH_ZHI = listOf("寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥", "子", "丑")

    /** 某地支相对太岁支的十二神 */
    fun getGod(branch: String, taiSuiBranch: String): String {
        val i1 = ZHI.indexOf(taiSuiBranch)
        val i2 = ZHI.indexOf(branch)
        if (i1 < 0 || i2 < 0) return "-"
        val offset = ((i2 - i1) % 12 + 12) % 12
        return GODS_12[offset]
    }

    data class Gods12(val gan: String, val zhi: String)

    /** 计算一个干支的串宫十二神：[天干神, 地支神]（天干先寄宫再起神） */
    fun gods12(ganzhi: String, taisuiZhi: String): Gods12 {
        if (ganzhi.length < 2) return Gods12("-", "-")
        val gan = ganzhi[0].toString(); val zhi = ganzhi[1].toString()
        val stemBranch = GAN2ZHI[gan] ?: return Gods12("-", getGod(zhi, taisuiZhi))
        return Gods12(getGod(stemBranch, taisuiZhi), getGod(zhi, taisuiZhi))
    }

    /** 太岁支：1=流年起太岁(流年支)，2=大运起太岁(大运支)，3=日干起太岁(日干寄宫) */
    fun taisuiZhi(type: Int, liunianZhi: String, dayunZhi: String, dayGan: String): String = when (type) {
        1 -> liunianZhi
        2 -> dayunZhi
        else -> GAN2ZHI[dayGan] ?: liunianZhi
    }

    data class SanGong(val taiyuan: String, val minggong: String, val shengong: String)

    /**
     * 胎元/命宫/身宫完整干支（1:1 lunar.js EightChar）。
     * @param yearGan 年柱天干（精确，以立春为界）
     * @param monthGan 月柱天干  @param monthZhi 月柱地支
     * @param timeZhi 时柱地支
     */
    fun sanGong(yearGan: String, monthGan: String, monthZhi: String, timeZhi: String): SanGong {
        // 胎元：月干进一，月支进三
        val taiyuan = GAN[((GAN.indexOf(monthGan) + 1) % 10 + 10) % 10] +
                ZHI[((ZHI.indexOf(monthZhi) + 3) % 12 + 12) % 12]

        // lunar.js index(name, MONTH_ZHI, 0) 返回 1-based 位置（数组第0位为空串）
        val monthZhiIdx1 = MONTH_ZHI.indexOf(monthZhi) + 1          // 亥 -> 10
        val timeZhiIdx1InMonth = MONTH_ZHI.indexOf(timeZhi) + 1     // 辰 -> 3
        val yearGanIdx0 = ((GAN.indexOf(yearGan) % 10) + 10) % 10  // 丙 -> 2

        // 命宫：offset=(月支序+时支序)；>=14 取 26-offset 否则 14-offset
        var mgOffset = monthZhiIdx1 + timeZhiIdx1InMonth
        mgOffset = (if (mgOffset >= 14) 26 else 14) - mgOffset
        var mgGanIdx1 = (yearGanIdx0 + 1) * 2 + mgOffset
        while (mgGanIdx1 > 10) mgGanIdx1 -= 10
        while (mgGanIdx1 < 1) mgGanIdx1 += 10
        val minggong = GAN[(mgGanIdx1 - 1) % 10] + MONTH_ZHI[((mgOffset - 1) % 12 + 12) % 12]

        // 身宫：时支序用 ZHI(子=1)；offset>12 减 12
        val timeZhiIdx1 = ZHI.indexOf(timeZhi) + 1                  // 辰 -> 5
        var sgOffset = monthZhiIdx1 + timeZhiIdx1
        if (sgOffset > 12) sgOffset -= 12
        var sgGanIdx1 = (yearGanIdx0 + 1) * 2 + sgOffset
        while (sgGanIdx1 > 10) sgGanIdx1 -= 10
        while (sgGanIdx1 < 1) sgGanIdx1 += 10
        val shengong = GAN[(sgGanIdx1 - 1) % 10] + MONTH_ZHI[((sgOffset - 1) % 12 + 12) % 12]

        return SanGong(taiyuan, minggong, shengong)
    }
}

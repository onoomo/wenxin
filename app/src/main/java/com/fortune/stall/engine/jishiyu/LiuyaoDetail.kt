package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 六爻详细排盘数据组装器：把引擎结果 + 农历/节气/空亡 + 世爻解读
 * 组织成「详细结果页」所需的全部字段（对齐参考项目 view_6yao 结果页）。
 */
object LiuyaoDetail {

    data class YaoRow(
        val liuShen: String,      // 六神
        val liuQin: String,       // 六亲(父/兄/子/财/官/孙)
        val gan: String,          // 天干
        val zhi: String,          // 地支
        val wuXing: String,       // 地支五行
        val yao: Int,             // 1阳 0阴
        val moving: Boolean,      // 是否动爻
        val oldYao: Boolean,      // true=老阳/老阴
        val shi: Boolean,
        val ying: Boolean,
        val fushen: String
    )

    data class Data(
        val ming: String,
        val timeText: String,
        val method: String,
        val jieQi: String,
        val ganZhi: String,
        val kongWang: String,
        val shenSha: String,      // 神煞一行(可能很长)
        val guaLine: String,      // 本卦名(宫·别名)
        val hasBian: Boolean,
        val rows: List<YaoRow>,   // 上爻→初爻
        val shiLiuQin: String,    // 世爻六亲
        val explainLines: List<String>
    )

    /** 六十甲子序号 */
    private fun gzIndex(gz: String): Int {
        if (gz.length < 2) return -1
        val g = LunarCalendar.TIANGAN.indexOf(gz[0].toString())
        val z = LunarCalendar.DIZHI.indexOf(gz[1].toString())
        if (g < 0 || z < 0) return -1
        for (n in 0..59) if (n % 10 == g && n % 12 == z) return n
        return -1
    }

    /** 某柱干支的旬空(两个地支) */
    private fun kongwangOf(gz: String): String {
        val n = gzIndex(gz)
        if (n < 0) return ""
        val shou = n - n % 10              // 旬首序号
        val z0 = shou % 12
        val a = LunarCalendar.DIZHI[(z0 + 10) % 12]
        val b = LunarCalendar.DIZHI[(z0 + 11) % 12]
        return a + b
    }

    fun build(dt: LocalDateTime, yaoList: List<String>, ming: String, method: String): Data {
        val r = LiuYaoJishiyu.cast(dt, yaoList)
        val lun = LunarCalendar.fromDateTime(dt)

        // 时间: yyyy年M月d日 H时m分(农历X月Y日 辰时)
        val lunarMonth = (if (lun.isLeap) "闰" else "") + lun.month + "月"
        val timeText = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时${dt.minute}分" +
                "($lunarMonth${lun.day}日 ${lun.hourZhi}时)"

        // 节气区间
        val jieQi = SolarTerm.rangeAt(dt)?.let { "${it.from.display}→${it.to.display}" } ?: ""

        // 四柱干支 + 空亡
        val siZhu = listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
        val ganZhi = "${siZhu[0]}年 ${siZhu[1]}月 ${siZhu[2]}日 ${siZhu[3]}时"
        val kongWang = siZhu.joinToString(" ") { kongwangOf(it) }

        // 神煞: 名称+地支
        val shenSha = r.shenSha.joinToString("  ") { it.name + it.zhi }

        // 本卦名(宫 · 别名)
        val guaLine = r.baseName + "（${r.baseGong}宫" +
                (if (r.alias.isNotEmpty()) " · ${r.alias}" else "") + "）"

        // 六爻行: 上爻(索引5) → 初爻(索引0)
        val rows = mutableListOf<YaoRow>()
        for (k in 5 downTo 0) {
            val y = r.yaoInfo[k]
            rows.add(
                YaoRow(
                    liuShen = r.liuShen.getOrElse(k) { "" },
                    liuQin = y.liuQin,
                    gan = y.gan,
                    zhi = y.zhi,
                    wuXing = LunarCalendar.ZHI_WUXING[y.zhi] ?: "",
                    yao = y.yao,
                    moving = y.type.contains("o") || y.type.contains("x"),
                    oldYao = y.type.contains("o") || y.type.contains("x"),
                    shi = y.isShi,
                    ying = y.isYing,
                    fushen = y.fushen
                )
            )
        }

        // 世爻六亲 → 解读
        val shiYao = r.yaoInfo.firstOrNull { it.isShi }
        val shiLiuQin = shiYao?.liuQin ?: ""
        val explain = LiuyaoInfo.of(shiLiuQin)
        val explainLines = if (explain.isEmpty()) emptyList()
                           else explain.split("\n").filter { it.isNotEmpty() }

        return Data(
            ming = ming, timeText = timeText, method = method, jieQi = jieQi,
            ganZhi = ganZhi, kongWang = kongWang, shenSha = shenSha,
            guaLine = guaLine, hasBian = r.hasBian, rows = rows,
            shiLiuQin = shiLiuQin, explainLines = explainLines
        )
    }
}

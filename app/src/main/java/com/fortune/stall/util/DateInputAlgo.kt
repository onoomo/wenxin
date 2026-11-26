package com.fortune.stall.util

import com.fortune.stall.engine.jishiyu.BaziChart
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * 起卦页「日期输入」三种口径 → 统一公历时刻（供紫微排盘）。
 * - 公历：年/月/日 + 精准时分
 * - 农历：农历年/月/日 + 精准时分，走 LunarCalendar.lunarToSolar
 * - 四柱：年柱/月柱/日柱/时柱干支，反推匹配的公历时刻
 */
object DateInputAlgo {
    data class Parsed(val dateTime: LocalDateTime? = null, val error: String? = null)

    private fun gzIndex(s: String): Int {
        if (s.length < 2) return -1
        val g = LunarCalendar.TIANGAN.indexOf(s[0].toString())
        val z = LunarCalendar.DIZHI.indexOf(s[1].toString())
        if (g < 0 || z < 0) return -1
        for (i in 0 until 60) if (i % 10 == g && i % 12 == z) return i
        return -1
    }

    private fun yearGz(y: Int): String = LunarCalendar.ganZhi(((y - 4) % 60 + 60) % 60)

    fun parseSolar(y: Int, m: Int, d: Int, hh: Int, mm: Int): Parsed = try {
        Parsed(LocalDateTime.of(y, m, d, hh, mm))
    } catch (e: Exception) {
        Parsed(error = "公历日期无效：${e.message ?: "请检查年月日"}")
    }

    fun parseLunar(y: Int, m: Int, d: Int, hh: Int, mm: Int): Parsed = try {
        Parsed(LunarCalendar.lunarToSolar(y, m, d).atTime(hh, mm))
    } catch (e: Exception) {
        Parsed(error = "农历日期无效：${e.message ?: "请检查年月日"}")
    }

    /**
     * 四柱反推公历：先用年柱锁定候选公历年（六十甲子，取离今年最近的若干年），
     * 再以日柱筛出候选日，最后逐时辰核对四柱完全一致。
     */
    fun parseFourPillars(yearGz: String, monthGz: String, dayGz: String, hourGz: String): Parsed {
        val yi = gzIndex(yearGz); val mi = gzIndex(monthGz); val di = gzIndex(dayGz); val hi = gzIndex(hourGz)
        if (yi < 0 || mi < 0 || di < 0 || hi < 0) {
            return Parsed(error = "请填完整四柱干支，如 丙午 乙未 甲辰 庚午")
        }
        val nowYear = LocalDateTime.now().year
        val years = (1900..2100).filter { yearGz(it) == yearGz }
            .sortedBy { abs(it - nowYear) }.take(4)
        for (y in years) {
            val start = LocalDate.of(y, 1, 1)
            for (offset in 0..370) {
                val date = start.plusDays(offset.toLong())
                if (date.year != y) continue
                if (LunarCalendar.dayGanZhi(date) != dayGz) continue
                for (h in 0..23) {
                    val dt = date.atTime(h, 0)
                    val fp = BaziChart.fourPillars(dt)
                    if (fp[0] == yearGz && fp[1] == monthGz && fp[2] == dayGz && fp[3] == hourGz) {
                        return Parsed(dt)
                    }
                }
            }
        }
        return Parsed(error = "未找到匹配的公历日期，请核对四柱是否同一人")
    }
}

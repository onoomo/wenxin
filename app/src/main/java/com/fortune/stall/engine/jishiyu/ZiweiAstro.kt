package com.fortune.stall.engine.jishiyu

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * 紫微斗数 —— 真太阳时 / 夏令时统一校正。
 * 1:1 借鉴 vvdevpro/ziwei-ds 的 lib/ziwei/{true-solar-time,dst-cn,clock-adjust}.ts，按通行排盘口径：
 *   真太阳时 = 钟表时间 + 经度修正 + 均时差；经度修正 =(经度 −120°)×4 分钟（东八区中央经线 120°）。
 *   均时差采用 NOAA fractional-year 近似式，1900–2026 年足以稳定判断两小时时辰边界。
 *   中国夏令时只判定提示（表内纯按日期、起止都含）；主流口径从不自动减 1 小时。
 */
object ZiweiAstro {

    data class SolarTime(
        val valid: Boolean,
        val corrected: Boolean,
        val longitudeCorrectionMinutes: Double,
        val equationOfTimeMinutes: Double,
        val totalCorrectionMinutes: Double,
        val hour: Int,
        val minute: Int,
        val second: Int,
        val branch: Int,          // 时辰地支绝对索引 子=0..亥=11
        val dayOffset: Int,       // 相对输入日的天数偏移
        val normalizedMinutes: Double,
        val inChinaDST: Boolean   // 该出生日期是否落在夏令时区间
    ) {
        val timeIndex: Int
            get() = if (branch == 0 && normalizedMinutes >= 1380) 12
                    else if (branch == 0) 0
                    else branch
        val clock: String get() = "%02d:%02d".format(hour, minute)
        val summary: String get() = buildString {
            if (corrected) {
                append("真太阳时 $clock")
                val lon = longitudeCorrectionMinutes.roundToInt()
                val eot = equationOfTimeMinutes.roundToInt()
                append("（经度${if (lon >= 0) "+" else ""}${lon}分")
                append(" 均时差${if (eot >= 0) "+" else ""}${eot}分）")
            }
            if (inChinaDST) append(" · 出生于夏令时期间")
        }
    }

    private const val SECONDS_PER_DAY = 86400
    private fun mod(v: Int, base: Int) = ((v % base) + base) % base
    private fun isLeap(y: Int) = y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)

    private fun dayOfYear(y: Int, m: Int, d: Int): Int {
        val start = LocalDate.of(y, 1, 1)
        return ChronoUnit.DAYS.between(start, LocalDate.of(y, m, d)).toInt() + 1
    }

    /** NOAA 均时差（分钟）；正值表示真太阳时快于平太阳时。 */
    fun equationOfTimeMinutes(year: Int, month: Int, day: Int, clockHour: Int = 12, clockMinute: Int = 0): Double {
        if (month !in 1..12 || day !in 1..31) return 0.0
        val daysInYear = if (isLeap(year)) 366 else 365
        val fractionalHour = clockHour + clockMinute / 60.0
        val gamma = (2 * Math.PI / daysInYear) * (dayOfYear(year, month, day) - 1 + (fractionalHour - 12) / 24.0)
        return 229.18 * (
            0.000075
                + 0.001868 * cos(gamma)
                - 0.032077 * sin(gamma)
                - 0.014615 * cos(2 * gamma)
                - 0.040849 * sin(2 * gamma)
        )
    }

    /** 中国大陆夏令时表 [年,起月,起日,止月,止日]，起止都含。通行口径 XLS_CN 逐字照抄。 */
    private val XLS_CN = arrayOf(
        intArrayOf(1935,5,1,9,30), intArrayOf(1936,5,1,9,30), intArrayOf(1937,5,1,9,30),
        intArrayOf(1938,5,1,9,30), intArrayOf(1939,5,1,9,30), intArrayOf(1940,5,1,9,30),
        intArrayOf(1941,5,1,9,30), intArrayOf(1942,5,1,9,30), intArrayOf(1943,5,1,9,30),
        intArrayOf(1944,5,1,9,30), intArrayOf(1945,5,1,9,30), intArrayOf(1946,5,1,9,30),
        intArrayOf(1947,5,1,9,30), intArrayOf(1948,5,1,9,30), intArrayOf(1949,5,1,9,30),
        intArrayOf(1950,5,1,9,30), intArrayOf(1951,5,1,9,30), intArrayOf(1952,3,1,10,31),
        intArrayOf(1953,4,1,10,31), intArrayOf(1954,4,1,10,31), intArrayOf(1955,5,1,9,30),
        intArrayOf(1956,5,1,9,30), intArrayOf(1957,4,1,9,30), intArrayOf(1958,4,1,9,30),
        intArrayOf(1959,4,1,9,30), intArrayOf(1960,6,1,9,30), intArrayOf(1961,6,1,9,30),
        intArrayOf(1974,4,1,10,31), intArrayOf(1975,4,1,10,31), intArrayOf(1979,7,1,9,30),
        intArrayOf(1986,5,4,9,14), intArrayOf(1987,4,12,9,13), intArrayOf(1988,4,10,9,11),
        intArrayOf(1989,4,16,9,17), intArrayOf(1990,4,15,9,16), intArrayOf(1991,4,14,9,15)
    )

    /** 是否处于中国大陆夏令时期间（纯按日期，不看小时，逻辑逐字对齐通行口径）。 */
    fun isChinaDST(year: Int, month: Int, day: Int): Boolean {
        for (x in XLS_CN) {
            if (year != x[0]) continue
            val m1 = x[1]; val d1 = x[2]; val m2 = x[3]; val d2 = x[4]
            if (month > m1 && month < m2) return true
            if (month == m1) { if (day >= d1) return true }
            else if (month == m2) { if (day <= d2) return true }
        }
        return false
    }

    /** 真太阳时下的时辰地支绝对索引(子=0)，23:00–01:00 归子。 */
    private fun branchFromMinutes(normalizedMinutes: Double): Int =
        if (normalizedMinutes >= 1380 || normalizedMinutes < 60) 0
        else floor((normalizedMinutes - 60) / 120).toInt() + 1

    /**
     * 计算真太阳时。longitude 为空则不校正（保留钟表时），仅返回夏令时判定。
     * @param useSummerTimeOffset 用户确认输入的钟表读数是夏令时读数时=true → 先减 60 分钟（默认 false，只提示不自动减）
     */
    fun calculate(dt: LocalDateTime, longitude: Double?, useSummerTimeOffset: Boolean = false): SolarTime {
        val inDst = isChinaDST(dt.year, dt.monthValue, dt.dayOfMonth)
        val dstShift = if (useSummerTimeOffset && inDst) -60 else 0
        val corrected = longitude != null && longitude.isFinite()
        val meridian = 120.0
        val lonCorr = if (corrected) (longitude!! - meridian) * 4.0 else 0.0
        val eot = if (corrected)
            equationOfTimeMinutes(dt.year, dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute)
        else 0.0
        val total = lonCorr + eot + dstShift
        val rawSeconds = ((dt.hour * 60 + dt.minute) * 60 + dt.second + total * 60).roundToInt()
        val dayOffset = Math.floorDiv(rawSeconds, SECONDS_PER_DAY)
        val secondOfDay = mod(rawSeconds, SECONDS_PER_DAY)
        val hour = secondOfDay / 3600
        val minute = (secondOfDay % 3600) / 60
        val second = secondOfDay % 60
        val normalized = secondOfDay / 60.0
        return SolarTime(
            valid = true, corrected = corrected || dstShift != 0,
            longitudeCorrectionMinutes = lonCorr, equationOfTimeMinutes = eot,
            totalCorrectionMinutes = total, hour = hour, minute = minute, second = second,
            branch = branchFromMinutes(normalized), dayOffset = dayOffset,
            normalizedMinutes = normalized, inChinaDST = inDst
        )
    }
}

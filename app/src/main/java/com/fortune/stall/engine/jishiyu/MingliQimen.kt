package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 命理奇门（终身盘）—— 依据 ji.js.cn 命理奇门页。
 * 四柱用八字节气规则；终身阴盘奇门局；起运/大运/流年 1:1 lunar.js getYun/getDaYun/getLiuNian。
 */
object MingliQimen {
    data class YunStep(
        val label: String,
        val ganZhi: String,
        val startAge: Int,
        val years: String
    )

    data class Result(
        val name: String, val gender: String, val shengXiao: String,
        val solar: String, val lunar: String,
        val siZhu: List<String>,
        val qimen: Qimen.Result,
        val startAge: Int, val startYear: Int,
        val shunXing: Boolean,
        val daYun: List<YunStep>,
        val liuNian: List<YunStep>
    )

    private val YANG_GAN = setOf("甲", "丙", "戊", "庚", "壬")
    private val JIE_NAMES = arrayOf("小寒", "立春", "惊蛰", "清明", "立夏", "芒种", "小暑", "立秋", "白露", "寒露", "立冬", "大雪")

    /**
     * 起运岁数（lunar.js getYun sect=1）：
     * 顺行=生日→下一节，逆行=上一节→生日；按 3天=1岁 / 1天=4月 / 时辰折算。
     */
    private fun qiYun(dt: LocalDateTime, forward: Boolean): Pair<Int, Int> {
        fun zhiIdx(t: LocalDateTime): Int = if (t.hour == 23) 11 else LunarCalendar.hourToZhi(t.hour).let { LunarCalendar.DIZHI.indexOf(it) }
        // 找上一节、下一节（精确时刻）
        var prev: LocalDateTime? = null
        var next: LocalDateTime? = null
        for (yy in (dt.year - 1)..(dt.year + 1)) for (nm in JIE_NAMES) {
            val t = SolarTerm.termTime(yy, nm) ?: continue
            if (!t.isAfter(dt) && (prev == null || t.isAfter(prev))) prev = t
            if (t.isAfter(dt) && (next == null || t.isBefore(next))) next = t
        }
        val start = if (forward) dt else (prev ?: dt)
        val end = if (forward) (next ?: dt) else dt
        var hourDiff = zhiIdx(end) - zhiIdx(start)
        var dayDiff = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()).toInt()
        if (hourDiff < 0) { hourDiff += 12; dayDiff-- }
        val monthDiff = (hourDiff * 10) / 30
        val months = dayDiff * 4 + monthDiff
        val year = months / 12
        return Pair(year, months - year * 12)
    }

    fun paipan(name: String, isMan: Boolean, dt: LocalDateTime, jushu: Int = 0,
               genderShengXiao: String = ""): Result {
        val lun = LunarCalendar.fromDateTime(dt)
        val siZhu = BaziChart.fourPillars(dt)
        val qm = Qimen.paipan(dt, isMan, jushu)

        // 年干阴阳按立春界；顺逆：阳男阴女顺、阴男阳女逆
        val yearGan = siZhu[0][0].toString()
        val yang = yearGan in YANG_GAN
        val shun = (yang && isMan) || (!yang && !isMan)
        val (startAge, _) = qiYun(dt, shun)
        val startYearFull = dt.year + startAge
        val monthGz = siZhu[1]  // 八字节气月柱（Exact）

        fun gzAt(step: Int): String {
            val jz = JishiyuBase.JIAZHI
            var offset = jz.indexOf(monthGz)
            offset += if (shun) step else -step
            offset = ((offset % 60) + 60) % 60
            return jz[offset]
        }

        val daYun = (1..10).map { i ->
            val age = startAge + (i - 1) * 10 + 1
            val sy = startYearFull + (i - 1) * 10
            YunStep("第${i}运", gzAt(i), age, "${sy}年起")
        }
        val liuNian = (0 until 10).map { i ->
            val year = dt.year + i
            val gz = LunarCalendar.ganZhi(year - 4)
            val age = year - dt.year + 1
            YunStep("流年", gz, age, "${year}年")
        }

        return Result(
            name = name.ifBlank { "命主" }, gender = if (isMan) "男" else "女",
            shengXiao = genderShengXiao.ifBlank { lun.zodiac },
            solar = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时",
            lunar = (if (lun.isLeap) "闰" else "") + lun.month + "月" + lun.day + "日",
            siZhu = siZhu, qimen = qm, startAge = startAge, startYear = startYearFull,
            shunXing = shun, daYun = daYun, liuNian = liuNian
        )
    }
}

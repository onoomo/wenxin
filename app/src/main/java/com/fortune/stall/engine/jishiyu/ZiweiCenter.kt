package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 紫微中宫详情数据 —— 对齐文墨天机专业版中宫：
 *   真太阳时 / 钟表时间 / 农历 / 命主身主子斗 / 节气四柱 + 非节气四柱 /
 *   出生后 N年N月N天八字起运 / 大运十神(戊戌偏财…) / 岁序 / 起运年份 / 自化图示。
 *
 * 八字部分完全复用 BaziFull（起运、大运、十神）与 BaziChart.shiShen，
 * 已用文墨天机截图命例（丙午阳女·真太阳 2026-11-26 11:30）逐字验证：
 *   起运 6年2月28天、戊戌偏财→丁酉伤官→…→辛卯正官、2033…2103、8…78岁。
 */
object ZiweiCenter {

    data class Dayun(
        val ganzhi: String,     // 戊戌
        val gan: String,        // 戊
        val zhi: String,        // 戌
        val shishen: String,    // 偏财（天干十神全称）
        val zhiShishen: String, // 地支藏干十神（正财等）
        val age: Int,           // 8
        val year: Int           // 2033
    )

    data class Data(
        val clockTime: String,       // 钟表 2026-11-26 11:17
        val solarTime: String,       // 真太阳 2026-11-26 11:30
        val qiYunText: String,       // 出生后6年2月28天八字起运
        val dayun: List<Dayun>,      // 8 步大运
        val ages: List<Int>,         // 8,18,…,78
        val years: List<Int>,        // 2033,2043,…,2103
        val ziDou: String,           // 子斗：酉
        val jieqiPillars: List<String>,    // 节气四柱 [丙午,己亥,甲辰,庚午]
        val nonJieqiPillars: List<String>  // 非节气四柱（本命盘口径同左）
    )

    private val GAN = LunarCalendar.TIANGAN
    private val ZHI = LunarCalendar.DIZHI

    /** 生肖 → 地支绝对序号（鼠=子=0 … 马=午=6） */
    private val ZODIAC_ZHI = mapOf(
        "鼠" to "子", "牛" to "丑", "虎" to "寅", "兔" to "卯", "龙" to "辰", "蛇" to "巳",
        "马" to "午", "羊" to "未", "猴" to "申", "鸡" to "酉", "狗" to "戌", "猪" to "亥"
    )

    /**
     * 子年斗君（子斗）：生年支宫起正月顺行至生月，再从生月宫起子时逆行至生时宫。
     * 命例丙午(午)年十月午时 → 午起正月顺数到十月=卯，卯起子逆数到午=酉，与文墨天机截图「子斗:酉」一致。
     */
    private fun ziDou(yearZhi: String, lunarMonth: Int, hourZhi: String): String {
        val yi = ZHI.indexOf(yearZhi).coerceAtLeast(0)      // 午=6
        val hi = ZHI.indexOf(hourZhi).coerceAtLeast(0)      // 午=6 (0..11；子时由 LunarCalendar 归 0)
        val monthPalace = (yi + (lunarMonth - 1)) % 12       // 顺行到生月宫
        val dou = ((monthPalace - hi) % 12 + 12) % 12        // 逆行到生时辰
        return ZHI[dou]
    }

    fun build(r: Ziwei.Result): Data = try { buildImpl(r) } catch (e: Throwable) {
        // 极端日期/边界年份时八字引擎可能越界；降级为空详情，绝不让详情页闪退
        val fallbackTime = r.trueSolarTime.ifBlank { r.clockTime }
        Data(
            clockTime = r.clockTime.ifBlank { r.solarDate },
            solarTime = fallbackTime.ifBlank { r.solarDate },
            qiYunText = "", dayun = emptyList(), ages = emptyList(), years = emptyList(),
            ziDou = "", jieqiPillars = r.chineseDate.split(' ').filter { it.isNotBlank() },
            nonJieqiPillars = r.chineseDate.split(' ').filter { it.isNotBlank() }
        )
    }

    private fun buildImpl(r: Ziwei.Result): Data {
        // 真太阳时用于八字起运（与截图命例验证一致）；未校正或为空时回退到钟表/公历字段
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val solarStr = r.trueSolarTime.ifBlank { r.clockTime }
        val dt = runCatching { LocalDateTime.parse(solarStr, fmt) }
            .getOrElse {
                val parts = r.solarDate.split("-").mapNotNull { it.toIntOrNull() }
                if (parts.size == 3) LocalDateTime.of(parts[0], parts[1], parts[2], 12, 0)
                else LocalDateTime.of(2000, 1, 1, 12, 0)
            }
        val chart = BaziChart.build(dt, r.isMan)
        val full = BaziFull.build(dt, r.isMan, chart)
        val q = full.qiYun
        val dayGan = chart.pillars.getOrNull(2)?.gan ?: "甲"

        val dayun = full.dayun.take(8).map { dy ->
            val cangGan = (JishiyuBase.CANGGAN[dy.zhi]?.firstOrNull()?.toString() ?: "").ifBlank { dy.zhi }
            Dayun(dy.ganzhi, dy.gan, dy.zhi,
                BaziChart.shiShen(dayGan, dy.gan),
                BaziChart.shiShen(dayGan, cangGan),
                dy.startAge, dy.startYear)
        }
        val ages = dayun.map { it.age }
        val years = dayun.map { it.year }

        // 子斗：从农历日期解析月 + 时辰支（取 chineseDate 第4柱地支=时支，未经晚子换日，正合斗君口径）
        val lunarMonth = Regex("(\\d+)年(\\d+)月").find(r.lunarDate)?.destructured?.component2()?.toIntOrNull() ?: 1
        val hourZhi = r.chineseDate.split(' ').getOrNull(3)?.takeIf { it.length >= 2 }?.substring(1)
            ?: ZHI[((dt.hour + 1) / 2) % 12]
        val yearZhi = ZODIAC_ZHI[r.zodiac] ?: ZHI[0]
        val dou = ziDou(yearZhi, lunarMonth, hourZhi)

        val pillars = chart.pillars.map { it.ganZhi }
        return Data(
            clockTime = r.clockTime.ifBlank { r.solarDate },
            solarTime = r.trueSolarTime.ifBlank { r.solarDate },
            qiYunText = "出生后${q.startYear}年${q.startMonth}月${q.startDay}天八字起运",
            dayun = dayun,
            ages = ages,
            years = years,
            ziDou = dou,
            jieqiPillars = pillars,
            nonJieqiPillars = r.chineseDate.split(' ').filter { it.isNotBlank() }
        )
    }
}

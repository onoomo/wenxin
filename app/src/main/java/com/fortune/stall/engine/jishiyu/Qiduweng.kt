package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 戚都翁「未卜先知」时间起课排盘 —— 1:1 移植自 ji.js.cn 原版 js/qiduweng.js
 *
 * 规则：
 * 1. 建寅地支序：寅卯辰巳午未申酉戌亥子丑
 * 2. 农历一月建寅，从月支起(含)顺数农历日数 → 日支
 * 3. 从日支起按「水火木金土」循环顺布十二支，到时支得五星
 * 4. 时支+本气为「体」，五星为「用」，月建五行为月令，取旺相休囚死与体用关系
 */
object Qiduweng {
    val DIZHI_ORDER = arrayOf("寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥", "子", "丑")
    val WUXING_STARS = arrayOf("水", "火", "木", "金", "土")
    val STAR_ALIAS = mapOf("水" to "辰星", "火" to "荧惑", "木" to "岁星", "金" to "太白", "土" to "镇星")
    val DIZHI_WUXING = mapOf(
        "子" to "水", "亥" to "水", "寅" to "木", "卯" to "木", "巳" to "火", "午" to "火",
        "申" to "金", "酉" to "金", "辰" to "土", "戌" to "土", "丑" to "土", "未" to "土"
    )
    private val WUXING_SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val WUXING_KE = mapOf("木" to "土", "火" to "金", "土" to "水", "金" to "木", "水" to "火")
    private val STATE_DESC = mapOf(
        "旺" to "当令得时，气势最盛", "相" to "得令所生，气势次旺", "休" to "生助月令，自身气泄",
        "囚" to "克制月令，反受其制", "死" to "被月令所克，气势最衰"
    )

    data class PlateItem(
        val zhi: String, val wuxing: String, val star: String, val starName: String, val starAlias: String,
        val isMonth: Boolean, val isDay: Boolean, val isHour: Boolean, val isTi: Boolean
    )
    data class Result(
        val date: String, val jieqi: String, val ju: String,
        val siZhu: List<String>, val xunKong: List<String>,
        val lunarMonth: Int, val lunarDay: Int, val isLeap: Boolean, val monthCn: String, val dayCn: String,
        val monthZhi: String, val monthZhiWx: String,
        val dayZhi: String, val dayZhiWx: String, val dayCount: Int,
        val hourZhi: String, val hourZhiWx: String, val hourStar: String, val hourStarAlias: String, val hourSteps: Int,
        val yueLingZhi: String, val yueLingWx: String,
        val tiZhi: String, val tiWx: String, val tiState: String,
        val yongXing: String, val yongState: String,
        val relation: String, val relationDesc: String, val summary: String,
        val plate: List<PlateItem>, val process: List<String>
    )

    private fun zhiIndex(z: String) = DIZHI_ORDER.indexOf(z)
    private fun countZhi(start: String, n: Int): String {
        val idx = zhiIndex(start)
        return DIZHI_ORDER[(idx + n - 1) % 12]
    }
    private fun stepsFromTo(from: String, to: String): Int = (zhiIndex(to) - zhiIndex(from) + 12) % 12 + 1

    private fun wangXiang(wx: String, yueLing: String): String = when {
        wx == yueLing -> "旺"
        WUXING_SHENG[yueLing] == wx -> "相"
        WUXING_SHENG[wx] == yueLing -> "休"
        WUXING_KE[wx] == yueLing -> "囚"
        else -> "死"
    }

    private fun tiYong(tiWx: String, yongWx: String): Pair<String, String> = when {
        tiWx == yongWx -> "比和" to "体用同类，力量相当"
        WUXING_SHENG[tiWx] == yongWx -> "体生用" to "体生气泄于用"
        WUXING_SHENG[yongWx] == tiWx -> "用生体" to "用生体，用助体"
        WUXING_KE[tiWx] == yongWx -> "体克用" to "体克用，体制服用"
        else -> "用克体" to "用克体，体受用制"
    }

    private val CN_NUM = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    private fun monthChinese(m: Int) = when (m) {
        1 -> "正"; 11 -> "冬"; 12 -> "腊"; else -> CN_NUM.getOrNull(m) ?: "$m"
    }
    private fun dayChinese(d: Int) = when {
        d == 10 -> "初十"; d == 20 -> "二十"; d == 30 -> "三十"
        d < 10 -> "初" + CN_NUM[d]
        d < 20 -> "十" + CN_NUM[d - 10]
        else -> "廿" + CN_NUM[d - 20]
    }

    private val JIAZHI = List(60) { LunarCalendar.gan(it % 10) + LunarCalendar.zhi(it % 12) }

    private fun kongwang(gz: String): String {
        val n = JIAZHI.indexOf(gz)
        if (n < 0) return ""
        val shou = n - n % 10          // 旬首序号
        val z0 = shou % 12
        return "" + LunarCalendar.zhi((z0 + 10) % 12) + LunarCalendar.zhi((z0 + 11) % 12)
    }

    fun paipan(dt: LocalDateTime): Result {
        val lun = LunarCalendar.fromDateTime(dt)
        val lm = lun.month
        val ld = lun.day
        val leap = lun.isLeap   // 闰月标志在 isLeap 字段（month 恒为正数，非 lunar.js 的负月份约定）
        val hourZhi = lun.hourZhi
        val monthCn = monthChinese(lm)
        val dayCn = dayChinese(ld)

        val monthZhi = DIZHI_ORDER[lm - 1]
        val dayZhi = countZhi(monthZhi, ld)

        // 从日支起安五星，顺布十二支
        val plate = mutableListOf<PlateItem>()
        val starMap = mutableMapOf<String, String>()
        val startIdx = zhiIndex(dayZhi)
        for (i in 0 until 12) {
            val z = DIZHI_ORDER[(startIdx + i) % 12]
            val star = WUXING_STARS[i % 5]
            starMap[z] = star
            plate.add(PlateItem(
                zhi = z, wuxing = DIZHI_WUXING[z] ?: "", star = star, starName = star + "星",
                starAlias = STAR_ALIAS[star] ?: "",
                isMonth = z == monthZhi, isDay = z == dayZhi, isHour = z == hourZhi, isTi = z == hourZhi
            ))
        }
        val hourStar = starMap[hourZhi] ?: ""
        val hourSteps = stepsFromTo(dayZhi, hourZhi)

        val tiZhi = hourZhi
        val tiWx = DIZHI_WUXING[tiZhi] ?: ""
        val yongWx = hourStar
        val yueLingZhi = monthZhi
        val yueLingWx = DIZHI_WUXING[yueLingZhi] ?: ""
        val tiState = wangXiang(tiWx, yueLingWx)
        val yongState = wangXiang(yongWx, yueLingWx)
        val (relation, relationDesc) = tiYong(tiWx, yongWx)

        val lunarText = "农历${monthCn}月${dayCn} ${hourZhi}时"
        val juStr = "时间起课 - ${monthCn}月${dayCn}日 ${hourZhi}时"
        val df = DateTimeFormatter.ofPattern("yyyy年M月d日 H时m分")
        val dateStr = dt.format(df) + "(${monthCn}月${dayCn} ${hourZhi}时)"
        val range = SolarTerm.rangeAt(dt)
        val jieqi = range?.let { "${it.from.display} ~ ${it.to.display}" } ?: ""

        // 四柱月柱用节气月（原版 getMonthInGanZhi），与农历月近似不同
        val solarMonthGz = LunarAlmanac.solarMonthGanzhi(dt).first
        // 晚子时(23点) lunar.js 以次日日干起时柱（时柱换日，日柱仍当日）
        val hourGz = if (dt.hour == 23) {
            val nextDayGz = LunarCalendar.dayGanZhi(dt.toLocalDate().plusDays(1))
            val ganIdx = ("甲乙丙丁戊己庚辛壬癸".indexOf(nextDayGz[0]) % 5) * 2  // 五鼠遁：子时天干
            LunarCalendar.gan(ganIdx) + "子"
        } else lun.hourGanZhi
        val siZhu = listOf(lun.yearGanZhi, solarMonthGz, lun.dayGanZhi, hourGz)

        val sum = "体${tiZhi}${tiWx}${tiState}，用${yongWx}星${yongState}，$relation"
        val process = listOf(
            lunarText,
            "月：${monthCn}月对应地支「$monthZhi」",
            "日：从「$monthZhi」起顺数$ld，定位日地支「$dayZhi」",
            "时：从「$dayZhi」起按水、火、木、金、土循环，顺数至「$hourZhi」时，得「${hourStar}星」",
            "体：${tiZhi}${tiWx}（时辰），用：${yongWx}星（${STAR_ALIAS[yongWx]}）",
            "月令：${yueLingZhi}${yueLingWx}月，体${tiState}、用${yongState}"
        )

        return Result(
            date = dateStr, jieqi = jieqi, ju = juStr,
            siZhu = siZhu,
            xunKong = siZhu.map { kongwang(it) },
            lunarMonth = lm, lunarDay = ld, isLeap = leap, monthCn = monthCn, dayCn = dayCn,
            monthZhi = monthZhi, monthZhiWx = yueLingWx,
            dayZhi = dayZhi, dayZhiWx = DIZHI_WUXING[dayZhi] ?: "", dayCount = ld,
            hourZhi = hourZhi, hourZhiWx = tiWx, hourStar = hourStar,
            hourStarAlias = STAR_ALIAS[hourStar] ?: "", hourSteps = hourSteps,
            yueLingZhi = yueLingZhi, yueLingWx = yueLingWx,
            tiZhi = tiZhi, tiWx = tiWx, tiState = tiState,
            yongXing = yongWx, yongState = yongState,
            relation = relation, relationDesc = relationDesc, summary = sum,
            plate = plate, process = process
        )
    }
}

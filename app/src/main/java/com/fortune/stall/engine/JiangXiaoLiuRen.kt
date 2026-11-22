package com.fortune.stall.engine

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 江氏小六壬排盘引擎 —— 完整移植反编译 APK「江氏小六壬 1.0」(uni-app liuren.js 模块 919) 的算法。
 *
 * 与推算版不同：本版严格按 APK 的四张查表定六亲/六神/五星/主事，并支持四种起课方式：
 * - 时刻起课：时辰定 A 宫，刻（时辰内每 10 分钟一刻，12 刻配 12 地支）定 B 宫/身宫
 * - 日时起课：农历日起 A 宫，时辰定 B 宫
 * - 数字起课：报数定 A 宫，时辰定 B 宫
 * - 随机起课：随机数字定 A 宫，随机时辰定 B 宫
 *
 * 六宫顺序: 大安 留连 速喜 赤口 小吉 空亡 (index 0..5)
 * 屏幕排布: [1,2,3,0,5,4] => 留连 速喜 赤口 / 大安 空亡 小吉
 * 农历日由本地 LunarCalendar 提供（数据覆盖 1900-2100，比 APK 自带表更全）。
 */
object JiangXiaoLiuRen {
    /** 六宫顺序（APK 中写作「留恋」，本地沿用已认可的「留连」） */
    val PALACES = arrayOf("大安", "留连", "速喜", "赤口", "小吉", "空亡")
    /** 屏幕排布：留连 速喜 赤口 / 大安 空亡 小吉 */
    val DISPLAY_ORDER = intArrayOf(1, 2, 3, 0, 5, 4)

    private val DIZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val ZHI_IDX = DIZHI.withIndex().associate { it.value to it.index }
    /** APK 的 g 表：小时 → 时辰地支（23/0→子, 1/2→丑 …） */
    private val HOUR_ZHI = linkedMapOf(
        23 to "子", 0 to "子", 1 to "丑", 2 to "丑", 3 to "寅", 4 to "寅",
        5 to "卯", 6 to "卯", 7 to "辰", 8 to "辰", 9 to "巳", 10 to "巳",
        11 to "午", 12 to "午", 13 to "未", 14 to "未", 15 to "申", 16 to "申",
        17 to "酉", 18 to "酉", 19 to "戌", 20 to "戌", 21 to "亥", 22 to "亥"
    )

    /** f 表：身宫地支 → (宫地支 → 六亲)，APK utils/liuren.js f */
    private val F_LIUQIN = mapOf(
        "子" to mapOf("子" to "身", "寅" to "子孙", "辰" to "兄弟", "午" to "妻财", "申" to "父母", "戌" to "官鬼", "丑" to "兄弟", "卯" to "子孙", "巳" to "妻财", "未" to "兄弟", "酉" to "父母", "亥" to "身"),
        "丑" to mapOf("丑" to "身", "卯" to "官鬼", "巳" to "父母", "未" to "兄弟", "酉" to "子孙", "亥" to "妻财", "子" to "妻财", "寅" to "官鬼", "辰" to "兄弟", "午" to "父母", "申" to "子孙", "戌" to "官鬼"),
        "寅" to mapOf("寅" to "身", "辰" to "兄弟", "午" to "子孙", "申" to "官鬼", "戌" to "妻财", "子" to "父母", "丑" to "妻财", "卯" to "身", "巳" to "子孙", "未" to "兄弟", "酉" to "官鬼", "亥" to "父母"),
        "卯" to mapOf("卯" to "身", "巳" to "子孙", "未" to "兄弟", "酉" to "官鬼", "亥" to "父母", "丑" to "妻财", "子" to "父母", "寅" to "身", "辰" to "兄弟", "午" to "子孙", "申" to "官鬼", "戌" to "妻财"),
        "辰" to mapOf("辰" to "身", "午" to "父母", "申" to "子孙", "戌" to "兄弟", "子" to "妻财", "寅" to "官鬼", "丑" to "兄弟", "卯" to "官鬼", "巳" to "父母", "未" to "兄弟", "酉" to "子孙", "亥" to "妻财"),
        "巳" to mapOf("巳" to "身", "未" to "兄弟", "酉" to "妻财", "亥" to "官鬼", "丑" to "子孙", "卯" to "父母", "子" to "官鬼", "寅" to "妻财", "辰" to "父母", "午" to "身", "申" to "妻财", "戌" to "子孙"),
        "午" to mapOf("午" to "身", "申" to "妻财", "戌" to "兄弟", "子" to "官鬼", "寅" to "父母", "辰" to "子孙", "丑" to "子孙", "卯" to "父母", "巳" to "身", "未" to "兄弟", "酉" to "妻财", "亥" to "官鬼"),
        "未" to mapOf("未" to "身", "酉" to "子孙", "亥" to "妻财", "丑" to "兄弟", "卯" to "官鬼", "巳" to "父母", "子" to "妻财", "寅" to "官鬼", "辰" to "父母", "午" to "兄弟", "申" to "子孙", "戌" to "兄弟"),
        "申" to mapOf("申" to "身", "戌" to "兄弟", "子" to "子孙", "寅" to "妻财", "辰" to "父母", "午" to "官鬼", "丑" to "兄弟", "卯" to "妻财", "巳" to "官鬼", "未" to "父母", "酉" to "身", "亥" to "子孙"),
        "酉" to mapOf("酉" to "身", "亥" to "子孙", "丑" to "兄弟", "卯" to "妻财", "巳" to "官鬼", "未" to "父母", "子" to "子孙", "寅" to "妻财", "辰" to "父母", "午" to "官鬼", "申" to "身", "戌" to "兄弟"),
        "戌" to mapOf("戌" to "身", "子" to "妻财", "寅" to "官鬼", "辰" to "兄弟", "午" to "父母", "申" to "子孙", "丑" to "兄弟", "卯" to "官鬼", "巳" to "父母", "未" to "兄弟", "酉" to "子孙", "亥" to "妻财"),
        "亥" to mapOf("亥" to "身", "丑" to "兄弟", "卯" to "子孙", "巳" to "妻财", "未" to "官鬼", "酉" to "父母", "子" to "身", "寅" to "子孙", "辰" to "兄弟", "午" to "妻财", "申" to "父母", "戌" to "官鬼")
    )

    /** h 表：身宫地支 → (宫名 → 身宫主事六亲)，APK utils/liuren.js h */
    private val H_ZHUSHI = mapOf(
        "子" to mapOf("大安" to "子孙", "留连" to "兄弟", "速喜" to "妻财", "赤口" to "父母", "小吉" to "子孙", "空亡" to "官鬼"),
        "亥" to mapOf("大安" to "子孙", "留连" to "兄弟", "速喜" to "妻财", "赤口" to "父母", "小吉" to "子孙", "空亡" to "官鬼"),
        "寅" to mapOf("大安" to "兄弟", "留连" to "父母", "速喜" to "子孙", "赤口" to "官鬼", "小吉" to "兄弟", "空亡" to "妻财"),
        "卯" to mapOf("大安" to "兄弟", "留连" to "父母", "速喜" to "子孙", "赤口" to "官鬼", "小吉" to "兄弟", "空亡" to "妻财"),
        "巳" to mapOf("大安" to "父母", "留连" to "官鬼", "速喜" to "兄弟", "赤口" to "妻财", "小吉" to "父母", "空亡" to "子孙"),
        "午" to mapOf("大安" to "父母", "留连" to "官鬼", "速喜" to "兄弟", "赤口" to "妻财", "小吉" to "父母", "空亡" to "子孙"),
        "申" to mapOf("大安" to "妻财", "留连" to "子孙", "速喜" to "官鬼", "赤口" to "兄弟", "小吉" to "妻财", "空亡" to "父母"),
        "酉" to mapOf("大安" to "妻财", "留连" to "子孙", "速喜" to "官鬼", "赤口" to "兄弟", "小吉" to "妻财", "空亡" to "父母"),
        "辰" to mapOf("大安" to "官鬼", "留连" to "妻财", "速喜" to "父母", "赤口" to "子孙", "小吉" to "官鬼", "空亡" to "兄弟"),
        "未" to mapOf("大安" to "官鬼", "留连" to "妻财", "速喜" to "父母", "赤口" to "子孙", "小吉" to "官鬼", "空亡" to "兄弟"),
        "戌" to mapOf("大安" to "官鬼", "留连" to "妻财", "速喜" to "父母", "赤口" to "子孙", "小吉" to "官鬼", "空亡" to "兄弟"),
        "丑" to mapOf("大安" to "官鬼", "留连" to "妻财", "速喜" to "父母", "赤口" to "子孙", "小吉" to "官鬼", "空亡" to "兄弟")
    )

    /** d 表：身宫地支 → (宫名 → 六神)，APK utils/liuren.js d */
    private val D_LIUSHEN = mapOf(
        "子" to mapOf("大安" to "青龙", "留连" to "朱雀", "速喜" to "勾陈", "赤口" to "白虎", "小吉" to "玄武", "空亡" to "腾蛇"),
        "午" to mapOf("大安" to "青龙", "留连" to "朱雀", "速喜" to "勾陈", "赤口" to "白虎", "小吉" to "玄武", "空亡" to "腾蛇"),
        "丑" to mapOf("留连" to "青龙", "速喜" to "朱雀", "赤口" to "勾陈", "小吉" to "白虎", "空亡" to "玄武", "大安" to "腾蛇"),
        "未" to mapOf("留连" to "青龙", "速喜" to "朱雀", "赤口" to "勾陈", "小吉" to "白虎", "空亡" to "玄武", "大安" to "腾蛇"),
        "寅" to mapOf("速喜" to "青龙", "赤口" to "朱雀", "小吉" to "勾陈", "空亡" to "白虎", "大安" to "玄武", "留连" to "腾蛇"),
        "申" to mapOf("速喜" to "青龙", "赤口" to "朱雀", "小吉" to "勾陈", "空亡" to "白虎", "大安" to "玄武", "留连" to "腾蛇"),
        "卯" to mapOf("赤口" to "青龙", "小吉" to "朱雀", "空亡" to "勾陈", "大安" to "白虎", "留连" to "玄武", "速喜" to "腾蛇"),
        "酉" to mapOf("赤口" to "青龙", "小吉" to "朱雀", "空亡" to "勾陈", "大安" to "白虎", "留连" to "玄武", "速喜" to "腾蛇"),
        "辰" to mapOf("小吉" to "青龙", "空亡" to "朱雀", "大安" to "勾陈", "留连" to "白虎", "速喜" to "玄武", "赤口" to "腾蛇"),
        "戌" to mapOf("小吉" to "青龙", "空亡" to "朱雀", "大安" to "勾陈", "留连" to "白虎", "速喜" to "玄武", "赤口" to "腾蛇"),
        "巳" to mapOf("空亡" to "青龙", "大安" to "朱雀", "留连" to "勾陈", "速喜" to "白虎", "赤口" to "玄武", "小吉" to "腾蛇"),
        "亥" to mapOf("空亡" to "青龙", "大安" to "朱雀", "留连" to "勾陈", "速喜" to "白虎", "赤口" to "玄武", "小吉" to "腾蛇")
    )

    /** x 表：A 宫 → (宫名 → 五星)，APK utils/liuren.js x（A 宫固定木星） */
    private val X_WUXING = mapOf(
        "大安" to mapOf("大安" to "木星", "留连" to "火星", "速喜" to "土星", "赤口" to "金星", "小吉" to "水星", "空亡" to "天空"),
        "留连" to mapOf("留连" to "木星", "速喜" to "火星", "赤口" to "土星", "小吉" to "金星", "空亡" to "水星", "大安" to "天空"),
        "速喜" to mapOf("速喜" to "木星", "赤口" to "火星", "小吉" to "土星", "空亡" to "金星", "大安" to "水星", "留连" to "天空"),
        "赤口" to mapOf("赤口" to "木星", "小吉" to "火星", "空亡" to "土星", "大安" to "金星", "留连" to "水星", "速喜" to "天空"),
        "小吉" to mapOf("小吉" to "木星", "空亡" to "火星", "大安" to "土星", "留连" to "金星", "速喜" to "水星", "赤口" to "天空"),
        "空亡" to mapOf("空亡" to "木星", "大安" to "火星", "留连" to "土星", "速喜" to "金星", "赤口" to "水星", "小吉" to "天空")
    )

    /** 宫五行（仅作展示用，APK 排盘不依赖它定六亲） */
    private val PALACE_WUXING = arrayOf("木", "水", "火", "金", "木", "土")

    /** 起课方式 */
    enum class Method(val label: String) {
        RISHI("日时起课"), SHIKE("时刻起课"), SHUZI("数字起课"), SUJI("随机起课")
    }

    /** 排盘类型（六神排法）：活六神=江氏官方时六神；日六神=旧法/天六神（六爻顺序） */
    enum class LiuShenMode(val label: String) {
        HUO("活六神(官方)"), RI("日六神(旧)")
    }

    /** 日六神：地支 → 日宫六神（寅卯青龙 巳午朱雀 申酉白虎 亥子玄武 辰丑勾陈 未戌腾蛇） */
    private val RI_SHEN_BY_ZHI = mapOf(
        "寅" to "青龙", "卯" to "青龙", "巳" to "朱雀", "午" to "朱雀",
        "申" to "白虎", "酉" to "白虎", "亥" to "玄武", "子" to "玄武",
        "辰" to "勾陈", "丑" to "勾陈", "未" to "腾蛇", "戌" to "腾蛇"
    )
    /** 日六神排列顺序（六爻六神顺序）：青龙→朱雀→勾陈→腾蛇→白虎→玄武 */
    private val RI_SHEN_ORDER = arrayOf("青龙", "朱雀", "勾陈", "腾蛇", "白虎", "玄武")

    data class PalaceCell(
        val palaceIndex: Int,
        val palaceName: String,
        val palaceWuxing: String,
        val wuXing: String,      // 五星（九星）：木星/火星/土星/金星/水星/天空
        val liuShen: String,     // 六神
        val zhi: String,         // 地支
        val liuQin: String,      // 六亲（身宫显示「身」）
        val originalLiuQin: String = "", // 身宫原始六亲（APK originalLiuqin）
        val zhushiRen: String = "",      // 身宫主事六亲（APK zhushiren）
        val isDay: Boolean,      // A 宫（日宫/时宫/数字宫…）
        val isTime: Boolean,     // B 宫（时宫/刻宫）
        val isBody: Boolean,     // 身宫，与 B 宫同宫
        val riShiText: String = "" // 宫格底部标记：按 APK getRiShiText 随起课方式变化
    )

    data class Chart(
        val cells: List<PalaceCell>,
        val dayIndex: Int,           // A 宫索引
        val bodyIndex: Int,          // B 宫（身宫）索引
        val timeZhi: String,         // 身宫地支
        val solarText: String,
        val ganZhiText: String,
        val lunarText: String,
        val methodText: String,
        val faceText: String,
        val yearGz: String, val monthGz: String, val dayGz: String, val hourGz: String,
        val yearKong: String, val monthKong: String, val dayKong: String, val hourKong: String,
        val lunarMonthDay: String,
        val solarDisplay: String,
        val qiMethod: String,
        val keZhi: String = "",          // 时刻起课的刻支
        val randomNumber: Int = 0,       // 随机起课的随机数字
        val randomShiZhi: String = "",   // 随机起课的随机时支
        val courseInfoText: String = ""  // APK formattedCourseInfo 的完整显示文本
    )

    private val XUN_KONG = arrayOf("戌亥", "申酉", "午未", "辰巳", "寅卯", "子丑")
    fun kongOf(ganZhi: String): String {
        for (i in 0 until 60) if (LunarCalendar.ganZhi(i) == ganZhi) return XUN_KONG[i / 10]
        return ""
    }

    /** APK v(hour)：小时 → 时辰地支 */
    private fun hourZhi(hour: Int): String =
        if (hour >= 23 || hour < 1) "子" else (HOUR_ZHI[hour] ?: "子")

    /** APK T() 数字起课用月份数组：十一月写作「十一」 */
    private val LUNAR_MONTH_NUM = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "腊")
    /** APK calculateDisplayLunarInfo() 日时起课用月份数组：十一月写作「冬」 */
    private val LUNAR_MONTH_RISHI = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val LUNAR_DAY = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    /** 农历「X月X」文本；带前缀「农历」，月份数组按起课方式区分 */
    private fun lunarDayText(month: Int, day: Int, forRiShi: Boolean): String {
        val mm = if (forRiShi) LUNAR_MONTH_RISHI else LUNAR_MONTH_NUM
        val m = if (month in 1..12) mm[month - 1] else month.toString()
        val d = if (day in 1..30) LUNAR_DAY[day - 1] else day.toString()
        return "${m}月${d}"
    }
    /** 公历格式化成 APK 风格：2026年11月20日18时43分（月日时分补零） */
    private fun apkDateText(dt: LocalDateTime): String =
        String.format("%04d年%02d月%02d日%02d时%02d分", dt.year, dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute)

    /** APK m(hour,minute) getKeZhi：时辰内每 10 分钟一刻，12 刻配 12 地支 */
    fun keZhi(hour: Int, minute: Int): String {
        val zhi = hourZhi(hour)
        val minutesInto: Int = if (zhi == "子") {
            if (hour >= 23) 60 * (hour - 23) + minute else 60 * (hour + 1) + minute
        } else {
            val startHour = HOUR_ZHI.entries.first { it.value == zhi }.key
            60 * (hour - startHour) + minute
        }
        val ke = (minutesInto / 10) % 12
        return DIZHI[ke]
    }

    /**
     * APK y() 排盘：A 宫名/索引、B 宫名/索引、身宫地支 → 六宫完整数据。
     * 严格按 APK 九步流程：排地支 → 六亲 → 主事 → 六神 → 五星。
     */
    private fun arrange(
        aName: String, aIdx: Int, bName: String, bIdx: Int, shenZhi: String,
        method: Method = Method.RISHI, numberText: String = "",
        liuShenMode: LiuShenMode = LiuShenMode.HUO
    ): List<PalaceCell> {
        // APK getRiShiText：宫格底部标记随起课方式变化
        fun riShiTextOf(p: Int): String {
            val isRi = p == aIdx   // isRiGong = A 宫
            val isShi = p == bIdx  // isShiGong = B 宫
            return when (method) {
                Method.SHIKE -> when {
                    isRi && isShi -> "时刻"
                    isRi -> "时"
                    isShi -> "刻"
                    else -> ""
                }
                Method.RISHI -> when {
                    isRi && isShi -> "日时"
                    isRi -> "日"
                    isShi -> "时"
                    else -> ""
                }
                Method.SHUZI, Method.SUJI -> {
                    var e = ""
                    if (isShi) e += "时"
                    if (isRi) {
                        val s = if (method == Method.SHUZI) {
                            val a = numberText.toIntOrNull() ?: 0
                            val r = a % 6
                            if (r == 0) "6" else r.toString()
                        } else numberText // 随机起课：随机数字
                        if (s.isNotEmpty()) e = if (e.isNotEmpty()) "$e $s" else s
                    }
                    e
                }
            }
        }
        // 地支：身支属阳支组(子寅辰午申戌)则整组顺排，否则阴支组顺排；从身宫 B 起
        val isYang = shenZhi in setOf("子", "寅", "辰", "午", "申", "戌")
        val group = if (isYang) arrayOf("子", "寅", "辰", "午", "申", "戌")
                    else arrayOf("丑", "卯", "巳", "未", "酉", "亥")
        val v = group.indexOf(shenZhi).coerceAtLeast(0)
        val zhiByPalace = Array(6) { "" }
        for (m in 0 until 6) {
            val palace = (bIdx + m) % 6
            zhiByPalace[palace] = group[(v + m) % 6]
        }

        val fMap = F_LIUQIN[shenZhi] ?: emptyMap()
        val hMap = H_ZHUSHI[shenZhi] ?: emptyMap()
        val dMap = D_LIUSHEN[shenZhi] ?: emptyMap()
        val xMap = X_WUXING[aName] ?: emptyMap()
        // 日六神（旧/天六神）：从日宫(A宫)地支定起始六神，按六爻顺序顺时针（宫位 index 递增）排
        val riShenByPalace = Array(6) { "" }
        if (liuShenMode == LiuShenMode.RI) {
            val dayZhi = zhiByPalace[aIdx]
            val start = RI_SHEN_ORDER.indexOf(RI_SHEN_BY_ZHI[dayZhi]).coerceAtLeast(0)
            for (p in 0 until 6) riShenByPalace[p] = RI_SHEN_ORDER[(start + p - aIdx + 6) % 6]
        }

        return (0 until 6).map { p ->
            val zhi = zhiByPalace[p]
            val lq = fMap[zhi] ?: ""
            val isBody = p == bIdx
            PalaceCell(
                palaceIndex = p,
                palaceName = PALACES[p],
                palaceWuxing = PALACE_WUXING[p],
                wuXing = xMap[PALACES[p]] ?: "",
                liuShen = if (liuShenMode == LiuShenMode.RI) riShenByPalace[p] else (dMap[PALACES[p]] ?: ""),
                zhi = zhi,
                liuQin = if (isBody) "身" else lq,
                originalLiuQin = if (isBody) lq else "",
                zhushiRen = hMap[PALACES[p]] ?: "",
                isDay = p == aIdx,
                isTime = p == bIdx,
                isBody = isBody,
                riShiText = riShiTextOf(p)
            )
        }
    }

    /**
     * 核心排盘（完全按 APK 四种起课算法）。
     * @param method 起课方式
     * @param dt 起卦时刻
     * @param number 数字起课的报数；随机起课内部随机生成
     */
    fun chart(method: Method, dt: LocalDateTime, number: Int? = null,
              liuShenMode: LiuShenMode = LiuShenMode.HUO): Chart {
        val lunar = LunarCalendar.fromDateTime(dt)
        val hour = dt.hour
        val minute = dt.minute
        val lunarDay = lunar.day

        val aIdx: Int
        val aName: String
        val bIdx: Int
        val bName: String
        val shenZhi: String
        val methodText: String
        var keZhiText = ""
        var rndNumber = 0
        var rndZhi = ""
        var courseInfoText = ""

        when (method) {
            Method.SHIKE -> {
                // $(hour,minute)：A=时辰宫，B=刻宫，身=刻支
                val shi = hourZhi(hour)
                val shiOrd = (ZHI_IDX[shi] ?: 0) + 1
                aIdx = (shiOrd - 1) % 6
                aName = PALACES[aIdx]
                val ke = keZhi(hour, minute)
                keZhiText = ke
                val keOrd = (ZHI_IDX[ke] ?: 0) + 1
                bIdx = (aIdx + keOrd - 1) % 6
                bName = PALACES[bIdx]
                shenZhi = ke
                methodText = "时刻起课 · ${shi}时${ke}刻 → A宫 $aName / B宫 $bName"
                // APK ll=D：lunar = "${时支}时${刻支}刻"；显示 "时刻起课：{lunar}-{dateTime}"
                courseInfoText = "时刻起课：${shi}时${ke}刻-${apkDateText(dt)}"
            }
            Method.RISHI -> {
                // C(lunarDay,hour,minute)：A=农历日宫，B=时辰宫，身=时支
                aIdx = (lunarDay - 1) % 6
                aName = PALACES[aIdx]
                val shi = hourZhi(hour)
                val shiOrd = (ZHI_IDX[shi] ?: 0) + 1
                bIdx = (aIdx + shiOrd - 1) % 6
                bName = PALACES[bIdx]
                shenZhi = shi
                methodText = "日时起课 · 农历${lunarDay}日 → A宫 $aName / B宫 $bName"
                // APK：dateTime（农历月日冬月 + 时支时）包在括号里
                courseInfoText = "日时起课：${apkDateText(dt)}（农历${lunarDayText(lunar.month, lunar.day, true)} ${shi}时）"
            }
            Method.SHUZI -> {
                // w(number,hour,minute)：A=数字宫，B=时辰宫，身=时支
                val n = number ?: 1
                val rem = n % 6
                val numOrd = if (rem == 0) 6 else rem
                aIdx = (numOrd - 1) % 6
                aName = PALACES[aIdx]
                val shi = hourZhi(hour)
                val shiOrd = (ZHI_IDX[shi] ?: 0) + 1
                bIdx = (aIdx + shiOrd - 1) % 6
                bName = PALACES[bIdx]
                shenZhi = shi
                methodText = "数字起课 · 报数 $n → A宫 $aName / B宫 $bName"
                // APK：{报数}-{dateTime}（农历月日 时支时，无「农历」前缀）
                courseInfoText = "数字起课：$n-${apkDateText(dt)}（${lunarDayText(lunar.month, lunar.day, false)} ${shi}时）"
            }
            Method.SUJI -> {
                // b()：随机数字定 A 宫，随机时支定 B 宫，身=随机时支
                val t = Random.nextInt(1, 7)          // 1..6
                val zhiIdx = Random.nextInt(0, 12)    // 0..11
                rndNumber = t
                val zhi = DIZHI[zhiIdx]
                rndZhi = zhi
                aIdx = (t - 1) % 6
                aName = PALACES[aIdx]
                val zhiOrd = zhiIdx + 1
                bIdx = (aIdx + zhiOrd - 1) % 6
                bName = PALACES[bIdx]
                shenZhi = zhi
                methodText = "随机起课 · 数字 $t / 时辰 $zhi → A宫 $aName / B宫 $bName"
                // APK：dateTime（数字{数}，{时支}时）
                courseInfoText = "随机起课：${apkDateText(dt)}（数字${t}，${zhi}时）"
            }
        }

        val riShiNumber = if (method == Method.SUJI) rndNumber.toString() else (number?.toString() ?: "")
        val cells = arrange(aName, aIdx, bName, bIdx, shenZhi, method, riShiNumber, liuShenMode)

        val solarText = dt.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss"))
        val ganZhiText = "${lunar.yearGanZhi}年 ${lunar.monthGanZhi}月 ${lunar.dayGanZhi}日 ${lunar.hourGanZhi}时"
        val lunarText = "农历${lunar.year}年${lunar.month}月${lunar.day}日 ${lunar.hourZhi}时" + if (lunar.isLeap) "(闰)" else ""
        val solarDisplay = dt.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH时mm分"))
        val lunarMonthDay = "${LunarCalendar.cnNumber(lunar.month)}月${LunarCalendar.cnNumber(lunar.day)}"

        return Chart(
            cells = cells,
            dayIndex = aIdx,
            bodyIndex = bIdx,
            timeZhi = shenZhi,
            solarText = solarText,
            ganZhiText = ganZhiText,
            lunarText = lunarText,
            methodText = methodText,
            faceText = "",
            yearGz = lunar.yearGanZhi, monthGz = lunar.monthGanZhi,
            dayGz = lunar.dayGanZhi, hourGz = lunar.hourGanZhi,
            yearKong = kongOf(lunar.yearGanZhi), monthKong = kongOf(lunar.monthGanZhi),
            dayKong = kongOf(lunar.dayGanZhi), hourKong = kongOf(lunar.hourGanZhi),
            lunarMonthDay = lunarMonthDay,
            solarDisplay = solarDisplay,
            qiMethod = method.label,
            keZhi = keZhiText,
            randomNumber = rndNumber,
            randomShiZhi = rndZhi,
            courseInfoText = courseInfoText
        )
    }

    /** 兼容旧调用：根据 number 是否为 null 走数字/日时 */
    fun chart(dt: LocalDateTime, number: Int? = null): Chart =
        chart(if (number != null && number >= 1) Method.SHUZI else Method.RISHI, dt, number)

    /** 生成可复制的纯文本卦面：完整包含「信息表」与「六宫格」两个框 */
    fun renderFace(chart: Chart, question: String, qiText: String = "—"): String {
        val L = mutableListOf<String>()
        // ——— 第一个框：信息表 ———
        L.add("【江氏小六壬 · 六宫排盘】")
        L.add("事项：${if (question.isBlank()) "输入预测事项" else question}")
        L.add("起课：${chart.courseInfoText}")
        L.add("节气：$qiText")
        L.add("四柱：")
        L.add("      年柱   月柱   日柱   时柱")
        L.add("干支  ${chart.yearGz}  ${chart.monthGz}  ${chart.dayGz}  ${chart.hourGz}")
        L.add("空亡  ${chart.yearKong}  ${chart.monthKong}  ${chart.dayKong}  ${chart.hourKong}")
        L.add("")
        // ——— 第二个框：六宫格（按屏幕两行三列，每宫含九星/六神/地支/六亲/宫名/标记）———
        L.add("【六宫格】")
        val rows = listOf(DISPLAY_ORDER.copyOfRange(0, 3).toList(), DISPLAY_ORDER.copyOfRange(3, 6).toList())
        for (row in rows) {
            val lineCells = row.map { idx ->
                val c = chart.cells[idx]
                val mark = buildString { if (c.isDay) append("日"); if (c.isTime) append("时") }
                val qin = if (c.isBody) "身 ${c.zhushiRen}" else c.liuQin
                "[${c.palaceName}${if (mark.isNotEmpty()) "·$mark" else ""}] ${c.wuXing} ${c.liuShen} ${c.zhi} $qin"
            }
            L.add(lineCells.joinToString("   |   "))
        }
        L.add("")
        L.add("A宫=${chart.cells[chart.dayIndex].palaceName}  B宫(身)=${chart.cells[chart.bodyIndex].palaceName}  身支=${chart.timeZhi}")
        return L.joinToString("\n")
    }

    fun cast(method: Method, dt: LocalDateTime, number: Int? = null, question: String = "", qiText: String = "—",
             liuShenMode: LiuShenMode = LiuShenMode.HUO): Pair<Chart, String> {
        val chart = chart(method, dt, number, liuShenMode)
        return chart to renderFace(chart, question, qiText)
    }

    /** 兼容旧调用 */
    fun cast(dt: LocalDateTime, number: Int? = null, question: String = ""): Pair<Chart, String> =
        cast(if (number != null && number >= 1) Method.SHUZI else Method.RISHI, dt, number, question)
}

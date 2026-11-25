package com.fortune.stall.engine

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 东玄 · 三数六宫排盘引擎。
 *
 * 以问事者所报三个数字（1-10）配合起卦时刻起局：
 * - 三数之和定「日宫」A，时辰定「身宫」B（沿用江氏小六壬数字起课框架）
 * - 四值（年月日时干支）与四柱旬空由本地历法精确推算
 * - 六宫按 [留连 速喜 赤口 / 大安 空亡 小吉] 排布，每宫融八神、九星、纳音、
 *   天干地支、六亲、长生、卦象、旺衰、主事、小六壬宫、六神等元素。
 *
 * 六宫顺序: 大安 留连 速喜 赤口 小吉 空亡 (index 0..5)
 * 屏幕排布: [1,2,3,0,5,4]
 */
object DongXuan {
    val PALACES = arrayOf("大安", "留连", "速喜", "赤口", "小吉", "空亡")
    val DISPLAY_ORDER = intArrayOf(1, 2, 3, 0, 5, 4)

    private val DIZHI = LunarCalendar.DIZHI
    private val ZHI_WX = LunarCalendar.ZHI_WUXING
    private val GAN_WX = LunarCalendar.GAN_WUXING

    /** 宫格一行的元素（对应参考图每一宫从上到下的各字段） */
    data class Cell(
        val palace: String,          // 小六壬宫名（大安/留连/…）
        val god: String,             // 八神六兽（白虎/玄武/腾蛇/勾陈/朱雀/青龙）
        val godAlias: String,        // 六兽别称（登山/失路/飞空/投机/投网/伤鳞）
        val nayin: String,           // 纳音
        val star: String,            // 九星带五行（辅木/英火/芮土/天空/蓬水/柱金）
        val gan: String,             // 天干
        val zhi: String,             // 地支
        val zhiStar: String,         // 地支字（寅元/辰尾/午星/子虚/戌崩/申奎）
        val wangshuai: String,       // 旺相休囚死
        val liuqin: String,          // 六亲（官鬼/兄弟/父母/妻财/子孙/身）
        val changsheng: String,      // 长生十二宫
        val gua: String,             // 卦象（坎→山水蒙）
        val zhushi: String,          // 主事（奴仆/婚姻/朋友/官命/父母/子女）
        val liuShen: String,         // 六神（青龙/朱雀/勾陈/腾蛇/白虎/玄武）
        val isDay: Boolean,          // 日宫（三数所定）
        val isBody: Boolean          // 身宫（时辰所定）
    )

    data class Chart(
        val cells: List<Cell>,
        val numbers: List<Int>,      // 报的三个数
        val numText: String,         // 如「458」
        val timeZhi: String,         // 时辰地支
        val solar: String,           // 公历
        val lunar: String,           // 农历
        val ganZhi: String,          // 四柱
        val yearGz: String, val monthGz: String, val dayGz: String, val hourGz: String,
        val yearKong: String, val monthKong: String, val dayKong: String, val hourKong: String,
        val dayGan: String,          // 日干（长生起例用）
        val monthZhi: String,        // 月支（旺衰用）
        val faceText: String
    )

    // ——— 查表 ———

    /** 旬空：干支 → 空亡地支 */
    private val XUN_KONG = arrayOf("戌亥", "申酉", "午未", "辰巳", "寅卯", "子丑")
    fun kongOf(ganZhi: String): String {
        for (i in 0 until 60) if (LunarCalendar.ganZhi(i) == ganZhi) return XUN_KONG[i / 10]
        return ""
    }

    /** 八神六兽按身支+宫序排布（江氏 d 表） */
    private val GOD_BY_SHEN = mapOf(
        "子" to arrayOf("青龙", "朱雀", "勾陈", "白虎", "玄武", "腾蛇"),
        "午" to arrayOf("青龙", "朱雀", "勾陈", "白虎", "玄武", "腾蛇"),
        "丑" to arrayOf("腾蛇", "青龙", "朱雀", "勾陈", "白虎", "玄武"),
        "未" to arrayOf("腾蛇", "青龙", "朱雀", "勾陈", "白虎", "玄武"),
        "寅" to arrayOf("玄武", "腾蛇", "青龙", "朱雀", "勾陈", "白虎"),
        "申" to arrayOf("玄武", "腾蛇", "青龙", "朱雀", "勾陈", "白虎"),
        "卯" to arrayOf("白虎", "玄武", "腾蛇", "青龙", "朱雀", "勾陈"),
        "酉" to arrayOf("白虎", "玄武", "腾蛇", "青龙", "朱雀", "勾陈"),
        "辰" to arrayOf("勾陈", "白虎", "玄武", "腾蛇", "青龙", "朱雀"),
        "戌" to arrayOf("勾陈", "白虎", "玄武", "腾蛇", "青龙", "朱雀"),
        "巳" to arrayOf("朱雀", "勾陈", "白虎", "玄武", "腾蛇", "青龙"),
        "亥" to arrayOf("朱雀", "勾陈", "白虎", "玄武", "腾蛇", "青龙")
    )
    private val GOD_ALIAS = mapOf(
        "白虎" to "登山", "玄武" to "失路", "腾蛇" to "飞空",
        "勾陈" to "投机", "朱雀" to "投网", "青龙" to "伤鳞"
    )
    /** 九星按 A宫(日宫)序排布：木辅/火英/土芮/金柱/水蓬/天空 */
    private val STAR_BY_ORDER = arrayOf("辅木", "英火", "芮土", "柱金", "蓬水", "天空")
    /** 纳音按宫地支固定配（每宫地支不同，取该支代表纳音） */
    private val NAYIN_BY_ZHI = mapOf(
        "子" to "海中金", "丑" to "海中金", "寅" to "炉中火", "卯" to "炉中火",
        "辰" to "大林木", "巳" to "大林木", "午" to "路旁土", "未" to "路旁土",
        "申" to "剑锋金", "酉" to "剑锋金", "戌" to "大海水", "亥" to "大海水"
    )
    /** 六亲：按身支→(宫地支)（江氏 f 表精简，按五行生克自洽） */
    private val LIUQIN_BY_SHEN = mapOf(
        "子" to mapOf("水" to "兄弟", "木" to "子孙", "火" to "妻财", "土" to "官鬼", "金" to "父母"),
        "丑" to mapOf("土" to "兄弟", "火" to "父母", "金" to "子孙", "水" to "妻财", "木" to "官鬼"),
        "寅" to mapOf("木" to "兄弟", "火" to "子孙", "土" to "妻财", "金" to "官鬼", "水" to "父母"),
        "卯" to mapOf("木" to "兄弟", "火" to "子孙", "土" to "妻财", "金" to "官鬼", "水" to "父母"),
        "辰" to mapOf("土" to "兄弟", "金" to "子孙", "水" to "妻财", "木" to "官鬼", "火" to "父母"),
        "巳" to mapOf("火" to "兄弟", "土" to "子孙", "金" to "妻财", "水" to "官鬼", "木" to "父母"),
        "午" to mapOf("火" to "兄弟", "土" to "子孙", "金" to "妻财", "水" to "官鬼", "木" to "父母"),
        "未" to mapOf("土" to "兄弟", "金" to "子孙", "水" to "妻财", "木" to "官鬼", "火" to "父母"),
        "申" to mapOf("金" to "兄弟", "水" to "子孙", "木" to "妻财", "火" to "官鬼", "土" to "父母"),
        "酉" to mapOf("金" to "兄弟", "水" to "子孙", "木" to "妻财", "火" to "官鬼", "土" to "父母"),
        "戌" to mapOf("土" to "兄弟", "金" to "子孙", "水" to "妻财", "木" to "官鬼", "火" to "父母"),
        "亥" to mapOf("水" to "兄弟", "木" to "子孙", "火" to "妻财", "土" to "官鬼", "金" to "父母")
    )
    /** 地支字（按宫序固定） */
    private val ZHI_STAR_BY_ORDER = arrayOf("寅元", "辰尾", "午星", "子虚", "戌崩", "申奎")
    /** 主事（按宫名） */
    private val ZHUSHI = mapOf(
        "大安" to "官命", "留连" to "奴仆", "速喜" to "婚姻",
        "赤口" to "朋友", "小吉" to "子女", "空亡" to "父母"
    )
    /** 地支 → 八卦卦象 */
    private val GUA_BY_ZHI = mapOf(
        "子" to "坎→水雷屯", "丑" to "艮→天山遁", "寅" to "坎→山水蒙", "卯" to "震→雷风恒",
        "辰" to "离→风火家人", "巳" to "离→火天大有", "午" to "乾→火天大有", "未" to "坤→地风升",
        "申" to "巽→地风升", "酉" to "兑→泽水困", "戌" to "乾→天山遁", "亥" to "坎→水雷屯"
    )
    /** 十二长生：阳干顺行、阴干逆行，宫地支相对日干 */
    private val CS_YANG = arrayOf("长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养")
    private val YANG_GAN = setOf("甲", "丙", "戊", "庚", "壬")
    private val CHANGSHENG_START = mapOf(
        "甲" to "亥", "乙" to "午", "丙" to "寅", "丁" to "酉", "戊" to "寅",
        "己" to "酉", "庚" to "巳", "辛" to "子", "壬" to "申", "癸" to "卯"
    )
    private fun changsheng(dayGan: String, zhi: String): String {
        val start = CHANGSHENG_START[dayGan] ?: return "长生"
        val s = DIZHI.indexOf(start)
        val z = DIZHI.indexOf(zhi)
        if (s < 0 || z < 0) return "长生"
        val diff = if (dayGan in YANG_GAN) (z - s + 12) % 12 else (s - z + 12) % 12
        return CS_YANG[diff]
    }
    /** 旺衰：宫五行相对月令五行的生克（当令旺、我生相、生我休、克我囚、我克死） */
    private val WANG_BY_REL = mapOf(
        "同" to "旺", "我生" to "相", "生我" to "休", "克我" to "囚", "我克" to "死"
    )
    private val SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")
    private fun wangshuai(gongWx: String, monthWx: String): String {
        val rel = when {
            gongWx == monthWx -> "同"
            SHENG[gongWx] == monthWx -> "我生"
            SHENG[monthWx] == gongWx -> "生我"
            KE[monthWx] == gongWx -> "克我"
            KE[gongWx] == monthWx -> "我克"
            else -> "同"
        }
        return WANG_BY_REL[rel] ?: "旺"
    }

    /** 地支藏干取本气（作天干展示） */
    private val ZHI_GAN = mapOf(
        "子" to "壬", "丑" to "己", "寅" to "甲", "卯" to "乙", "辰" to "戊", "巳" to "丙",
        "午" to "丁", "未" to "己", "申" to "庚", "酉" to "辛", "戌" to "戊", "亥" to "壬"
    )

    /**
     * 起盘。@param numbers 三个数（1-10）；@param dt 起卦时刻（默认当前）。
     */
    fun chart(numbers: List<Int>, dt: LocalDateTime): Chart {
        val ns = numbers.take(3).map { it.coerceIn(1, 10) }
        require(ns.size == 3) { "需要三个数字" }
        val lunar = LunarCalendar.fromDateTime(dt)
        val hour = dt.hour
        val timeZhi = LunarCalendar.hourToZhi(hour)

        // A宫（日宫）：三数之和定宫；B宫（身宫/时宫）：A宫顺行时支序数
        val sum = ns.sum()
        val aIdx = (sum - 3) % 6                       // 每数最小1，三数和最小3
        val shiOrd = LunarCalendar.zhiOrdinal(timeZhi) // 子=1..亥=12
        val bIdx = (aIdx + shiOrd - 1) % 6

        // 宫地支：以身支阴阳分组，从身宫起顺排六宫（江氏规则）
        val yang = setOf("子", "寅", "辰", "午", "申", "戌")
        val group = if (timeZhi in yang) arrayOf("子", "寅", "辰", "午", "申", "戌")
                    else arrayOf("丑", "卯", "巳", "未", "酉", "亥")
        val v = group.indexOf(timeZhi).coerceAtLeast(0)
        val zhiByPalace = Array(6) { "" }
        for (m in 0 until 6) zhiByPalace[(bIdx + m) % 6] = group[(v + m) % 6]

        val dayGan = lunar.dayGanZhi.take(1)
        val monthZhi = lunar.monthGanZhi.takeLast(1)
        val monthWx = ZHI_WX[monthZhi] ?: "木"
        val gods = GOD_BY_SHEN[timeZhi] ?: GOD_BY_SHEN["子"]!!

        val cells = (0 until 6).map { p ->
            val zhi = zhiByPalace[p]
            val gongWx = ZHI_WX[zhi] ?: "木"
            val god = gods[p]
            val lqMap = LIUQIN_BY_SHEN[timeZhi] ?: LIUQIN_BY_SHEN["子"]!!
            val lq = lqMap[gongWx] ?: "兄弟"
            Cell(
                palace = PALACES[p],
                god = god,
                godAlias = GOD_ALIAS[god] ?: "",
                nayin = NAYIN_BY_ZHI[zhi] ?: "",
                star = STAR_BY_ORDER[(p - aIdx + 6) % 6],
                gan = ZHI_GAN[zhi] ?: "甲",
                zhi = zhi,
                zhiStar = ZHI_STAR_BY_ORDER[p],
                wangshuai = wangshuai(gongWx, monthWx),
                liuqin = lq,
                changsheng = changsheng(dayGan, zhi),
                gua = GUA_BY_ZHI[zhi] ?: "",
                zhushi = ZHUSHI[PALACES[p]] ?: "",
                liuShen = god,
                isDay = p == aIdx,
                isBody = p == bIdx
            )
        }

        val solar = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val lunarText = "农历${lunar.year}年${lunar.month}月${lunar.day}日 ${timeZhi}时"
        val ganZhi = "${lunar.yearGanZhi} · ${lunar.monthGanZhi} · ${lunar.dayGanZhi} · ${lunar.hourGanZhi}"
        val kong = listOf(kongOf(lunar.yearGanZhi), kongOf(lunar.monthGanZhi),
            kongOf(lunar.dayGanZhi), kongOf(lunar.hourGanZhi)).joinToString(" · ")

        val result = Chart(
            cells = cells, numbers = ns, numText = ns.joinToString(""), timeZhi = timeZhi,
            solar = solar, lunar = lunarText, ganZhi = ganZhi,
            yearGz = lunar.yearGanZhi, monthGz = lunar.monthGanZhi,
            dayGz = lunar.dayGanZhi, hourGz = lunar.hourGanZhi,
            yearKong = kongOf(lunar.yearGanZhi), monthKong = kongOf(lunar.monthGanZhi),
            dayKong = kongOf(lunar.dayGanZhi), hourKong = kongOf(lunar.hourGanZhi),
            dayGan = dayGan, monthZhi = monthZhi, faceText = ""
        )
        return result.copy(faceText = renderFace(result))
    }

    private fun renderFace(c: Chart): String {
        val L = mutableListOf<String>()
        L.add("【东玄 · 三数六宫排盘】")
        L.add("报数：${c.numText} · ${c.timeZhi}时")
        L.add("公历：${c.solar}")
        L.add("农历：${c.lunar}")
        L.add("四值：${c.ganZhi}")
        L.add("旬空：${c.yearKong} · ${c.monthKong} · ${c.dayKong} · ${c.hourKong}")
        L.add("")
        L.add("【六宫格】")
        for (r in 0..1) {
            val row = (0..2).map { col ->
                val cell = c.cells[DISPLAY_ORDER[r * 3 + col]]
                "${cell.palace}: ${cell.god}/${cell.godAlias} ${cell.nayin} ${cell.star} " +
                    "${cell.gan}${cell.zhi} ${cell.wangshuai} ${cell.liuqin} ${cell.changsheng} ${cell.gua} ${cell.zhushi}"
            }
            L.add(row.joinToString("  |  "))
        }
        return L.joinToString("\n")
    }
}
